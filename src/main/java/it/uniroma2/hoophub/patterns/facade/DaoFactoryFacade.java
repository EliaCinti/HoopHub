package it.uniroma2.hoophub.patterns.facade;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.patterns.factory.CsvDaoFactory;
import it.uniroma2.hoophub.patterns.factory.DaoAbstractFactory;
import it.uniroma2.hoophub.patterns.factory.InMemoryDaoFactory;
import it.uniroma2.hoophub.patterns.factory.MySqlDaoFactory;
import it.uniroma2.hoophub.patterns.factory.ObserverFactory;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

/**
 * Unified interface implementing <b>Facade pattern (GoF)</b> combined with <b>Singleton pattern (GoF)</b>.
 * * <p>Uses an internal <b>Abstract Factory (GoF)</b> strategy to switch between persistence families.</p>
 *
 * @author Elia Cinti
 * @version 2.0 (Refactored to Abstract Factory)
 */
@SuppressWarnings("java:S6548")
public class DaoFactoryFacade {

    private static DaoFactoryFacade instance;

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
}