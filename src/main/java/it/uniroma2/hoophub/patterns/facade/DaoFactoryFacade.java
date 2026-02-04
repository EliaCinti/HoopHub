package it.uniroma2.hoophub.patterns.facade;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.patterns.factory.CsvDaoFactory;
import it.uniroma2.hoophub.patterns.factory.DaoAbstractFactory;
import it.uniroma2.hoophub.patterns.factory.InMemoryDaoFactory;
import it.uniroma2.hoophub.patterns.factory.MySqlDaoFactory;
import it.uniroma2.hoophub.patterns.factory.ObserverFactory;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;
import it.uniroma2.hoophub.sync.SyncContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified interface implementing <b>Facade pattern (GoF)</b> combined with <b>Singleton pattern (GoF)</b>.
 * * <p>Uses an internal <b>Abstract Factory (GoF)</b> strategy to switch between persistence families.</p>
 *
 * @author Elia Cinti
 * @version 2.1 (Added dual-layer bulk sync coordination)
 */
@SuppressWarnings("java:S6548")
public class DaoFactoryFacade {

    private static DaoFactoryFacade instance;

    private static final Logger logger = Logger.getLogger(DaoFactoryFacade.class.getName());

    private PersistenceType persistenceType;
    private DaoAbstractFactory activeFactory;

    private final ObserverFactory observerFactory;

    private UserDao userDao;
    private FanDao fanDao;
    private VenueManagerDao venueManagerDao;
    private VenueDao venueDao;
    private BookingDao bookingDao;
    private NotificationDao notificationDao;

    private DaoFactoryFacade() {
        this.observerFactory = ObserverFactory.getInstance();
        setPersistenceType(PersistenceType.MYSQL);
    }

    public static synchronized DaoFactoryFacade getInstance() {
        if (instance == null) {
            instance = new DaoFactoryFacade();
        }
        return instance;
    }

    /**
     * Sets the persistence type and instantiates the corresponding Abstract Factory family.
     * This resets any cached DAO instances.
     *
     * @param type the desired persistence mechanism
     */
    public void setPersistenceType(PersistenceType type) {
        this.persistenceType = type;

        switch (type) {
            case MYSQL -> this.activeFactory = new MySqlDaoFactory();
            case CSV -> this.activeFactory = new CsvDaoFactory();
            case IN_MEMORY -> this.activeFactory = new InMemoryDaoFactory();
        }

        clearCache();
    }

    private void clearCache() {
        this.userDao = null;
        this.fanDao = null;
        this.venueManagerDao = null;
        this.venueDao = null;
        this.bookingDao = null;
        this.notificationDao = null;
    }

    public PersistenceType getPersistenceType() {
        return persistenceType;
    }

    // =========================================================================
    // DAO Access Methods (Facade Methods)
    // =========================================================================

    public UserDao getUserDao() {
        if (userDao == null) {
            userDao = activeFactory.getUserDao();
        }
        return userDao;
    }

    public FanDao getFanDao() {
        if (fanDao == null) {
            fanDao = activeFactory.getFanDao();
        }
        return fanDao;
    }

    public VenueManagerDao getVenueManagerDao() {
        if (venueManagerDao == null) {
            venueManagerDao = activeFactory.getVenueManagerDao();
        }
        return venueManagerDao;
    }

    public VenueDao getVenueDao() {
        if (venueDao == null) {
            venueDao = activeFactory.getVenueDao();
        }
        return venueDao;
    }

    public BookingDao getBookingDao() {
        if (bookingDao == null) {
            bookingDao = activeFactory.getBookingDao();

            if (bookingDao instanceof ObservableDao observableBookingDao) {

                attachSyncObserverIfNeeded(observableBookingDao);

                DaoObserver notificationObserver = observerFactory.getNotificationBookingObserver();
                observableBookingDao.addObserver(notificationObserver);
            }
        }
        return bookingDao;
    }

    public NotificationDao getNotificationDao() {
        if (notificationDao == null) {
            notificationDao = activeFactory.getNotificationDao();

            attachSyncObserverIfNeeded(notificationDao);
        }
        return notificationDao;
    }

    // =========================================================================
    // Dual-Layer Bulk Operations (bypass observer chain)
    // =========================================================================

    /**
     * Marks all notifications as read on <b>both</b> persistence layers.
     *
     * <p>{@code markAllAsReadForUser} is a bulk operation that bypasses the
     * observer chain (no single entity to pass to {@code onAfterUpdate}).
     * The Facade explicitly propagates to the secondary layer, preserving
     * encapsulation: callers remain unaware of dual-persistence internals.</p>
     *
     * @param username the user whose notifications should be marked as read
     * @param userType the type of user (FAN or VENUE_MANAGER)
     * @throws DAOException if the primary layer write fails
     */
    public void markAllNotificationsAsRead(String username, UserType userType) throws DAOException {
        // Primary layer (active DAO with observers)
        getNotificationDao().markAllAsReadForUser(username, userType);

        // Secondary layer (sync-suppressed, no observers)
        NotificationDao secondaryDao = getSecondaryNotificationDao();
        if (secondaryDao != null) {
            SyncContext.startSync();
            try {
                secondaryDao.markAllAsReadForUser(username, userType);
            } catch (DAOException e) {
                logger.log(Level.WARNING, "Failed to sync mark-as-read to secondary layer", e);
            } finally {
                SyncContext.endSync();
            }
        }
    }

    // =========================================================================
    // Private Helpers for Observer Management (Identici a prima)
    // =========================================================================

    private void attachSyncObserverIfNeeded(ObservableDao dao) {
        if (this.persistenceType != PersistenceType.IN_MEMORY) {
            DaoObserver syncObserver = getSyncObserver();
            if (syncObserver != null) {
                dao.addObserver(syncObserver);
            }
        }
    }

    private DaoObserver getSyncObserver() {
        return switch (this.persistenceType) {
            case MYSQL -> observerFactory.getMySqlToCsvObserver();
            case CSV -> observerFactory.getCsvToMySqlObserver();
            case IN_MEMORY -> null;
        };
    }

    /**
     * Returns a raw NotificationDao for the <b>opposite</b> persistence layer,
     * without any observer attached.
     *
     * <p>Package-private: only the Facade itself uses this for bulk sync
     * operations. External callers use {@link #markAllNotificationsAsRead}.</p>
     *
     * @return the secondary NotificationDao, or {@code null} if IN_MEMORY
     */
    private NotificationDao getSecondaryNotificationDao() {
        return switch (this.persistenceType) {
            case CSV -> new MySqlDaoFactory().getNotificationDao();
            case MYSQL -> new CsvDaoFactory().getNotificationDao();
            case IN_MEMORY -> null;
        };
    }
}