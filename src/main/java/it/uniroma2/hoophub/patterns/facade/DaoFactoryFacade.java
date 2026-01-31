package it.uniroma2.hoophub.patterns.facade;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.patterns.factory.BookingDaoFactory;
import it.uniroma2.hoophub.patterns.factory.FanDaoFactory;
import it.uniroma2.hoophub.patterns.factory.NotificationDaoFactory;
import it.uniroma2.hoophub.patterns.factory.ObserverFactory;
import it.uniroma2.hoophub.patterns.factory.UserDaoFactory;
import it.uniroma2.hoophub.patterns.factory.VenueDaoFactory;
import it.uniroma2.hoophub.patterns.factory.VenueManagerDaoFactory;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

/**
 * Unified interface to all DAO factories implementing <b>Facade pattern (GoF)</b>
 * combined with <b>Singleton pattern (GoF)</b>.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Single point of access to all DAO factories</li>
 *   <li>Hides the complexity of DAO creation and observer management</li>
 *   <li>Manages cross-persistence synchronization via Observer pattern</li>
 *   <li>Lazy initialization with caching of DAO instances</li>
 * </ul>
 * </p>
 *
 * <p>Synchronization strategy:
 * <ul>
 *   <li>MySQL as primary → syncs changes to CSV</li>
 *   <li>CSV as primary → syncs changes to MySQL</li>
 *   <li>IN_MEMORY → no synchronization (standalone mode)</li>
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
@SuppressWarnings("java:S6548")
public class DaoFactoryFacade {

    private static DaoFactoryFacade instance;

    private PersistenceType persistenceType;
    private UserDao userDao;
    private FanDao fanDao;
    private VenueManagerDao venueManagerDao;
    private VenueDao venueDao;
    private BookingDao bookingDao;
    private NotificationDao notificationDao;

    private final ObserverFactory observerFactory = ObserverFactory.getInstance();

    private DaoFactoryFacade() {
        /* Singleton */
    }

    /**
     * Returns the singleton instance.
     *
     * @return the DaoFactoryFacade singleton
     */
    public static synchronized DaoFactoryFacade getInstance() {
        if (instance == null) {
            instance = new DaoFactoryFacade();
        }
        return instance;
    }

    /**
     * Gets the current persistence type.
     *
     * @return current persistence type (MYSQL, CSV, or IN_MEMORY)
     */
    public PersistenceType getPersistenceType() {
        return persistenceType;
    }

    /**
     * Sets persistence type and clears DAO cache if type changes.
     *
     * @param persistenceType the new persistence type
     */
    public void setPersistenceType(PersistenceType persistenceType) {
        if (this.persistenceType != persistenceType) {
            this.persistenceType = persistenceType;
            this.userDao = null;
            this.fanDao = null;
            this.venueManagerDao = null;
            this.venueDao = null;
            this.bookingDao = null;
            this.notificationDao = null;
        }
    }

    /**
     * Gets UserDao with sync observer configured (if applicable).
     *
     * @return cached or new UserDao instance
     */
    public UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDaoFactory().getUserDao(this.persistenceType);
            attachSyncObserverIfNeeded((ObservableDao) userDao);
        }
        return userDao;
    }

    /**
     * Gets FanDao with sync observer configured (if applicable).
     *
     * @return cached or new FanDao instance
     */
    public FanDao getFanDao() {
        if (fanDao == null) {
            fanDao = new FanDaoFactory().getFanDao(this.persistenceType);
            attachSyncObserverIfNeeded((ObservableDao) fanDao);
        }
        return fanDao;
    }

    /**
     * Gets VenueManagerDao with sync observer configured (if applicable).
     *
     * @return cached or new VenueManagerDao instance
     */
    public VenueManagerDao getVenueManagerDao() {
        if (venueManagerDao == null) {
            venueManagerDao = new VenueManagerDaoFactory().getVenueManagerDao(this.persistenceType);
            attachSyncObserverIfNeeded((ObservableDao) venueManagerDao);
        }
        return venueManagerDao;
    }

    /**
     * Gets VenueDao with sync observer configured (if applicable).
     *
     * @return cached or new VenueDao instance
     */
    public VenueDao getVenueDao() {
        if (venueDao == null) {
            venueDao = new VenueDaoFactory().getVenueDao(this.persistenceType);
            attachSyncObserverIfNeeded((ObservableDao) venueDao);
        }
        return venueDao;
    }

    /**
     * Gets BookingDao with both sync and notification observers (if applicable).
     *
     * <p>BookingDao has two observers:
     * <ul>
     *   <li>CrossPersistenceSyncObserver for MySQL ↔ CSV sync (not for IN_MEMORY)</li>
     *   <li>NotificationBookingObserver for automatic notifications (all types)</li>
     * </ul>
     * </p>
     *
     * @return cached or new BookingDao instance
     */
    public BookingDao getBookingDao() {
        if (bookingDao == null) {
            bookingDao = new BookingDaoFactory().getBookingDao(this.persistenceType);

            // Sync observer only for MYSQL and CSV
            attachSyncObserverIfNeeded((ObservableDao) bookingDao);

            // Notification observer for ALL persistence types (including IN_MEMORY)
            DaoObserver notificationObserver = observerFactory.getNotificationBookingObserver();
            ((ObservableDao) bookingDao).addObserver(notificationObserver);
        }
        return bookingDao;
    }

    /**
     * Gets NotificationDao with sync observer configured (if applicable).
     *
     * @return cached or new NotificationDao instance
     */
    public NotificationDao getNotificationDao() {
        if (notificationDao == null) {
            notificationDao = new NotificationDaoFactory().getNotificationDao(this.persistenceType);
            attachSyncObserverIfNeeded(notificationDao);
        }
        return notificationDao;
    }

    /**
     * Attaches sync observer only for MYSQL and CSV persistence types.
     * IN_MEMORY does not need synchronization.
     *
     * @param dao the observable DAO to attach observer to
     */
    private void attachSyncObserverIfNeeded(ObservableDao dao) {
        if (this.persistenceType != PersistenceType.IN_MEMORY) {
            DaoObserver syncObserver = getSyncObserver();
            if (syncObserver != null) {
                dao.addObserver(syncObserver);
            }
        }
    }

    /**
     * Gets appropriate sync observer based on persistence type.
     *
     * @return sync observer for MYSQL/CSV, null for IN_MEMORY
     */
    private DaoObserver getSyncObserver() {
        return switch (this.persistenceType) {
            case MYSQL -> observerFactory.getMySqlToCsvObserver();
            case CSV -> observerFactory.getCsvToMySqlObserver();
            case IN_MEMORY -> null;
        };
    }
}