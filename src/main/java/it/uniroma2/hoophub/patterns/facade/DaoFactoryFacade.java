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
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 * @version 1.0
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
     * @return current persistence type (MYSQL or CSV)
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
     * Gets UserDao with sync observer configured.
     *
     * @return cached or new UserDao instance
     */
    public UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDaoFactory().getUserDao(this.persistenceType);
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) userDao).addObserver(syncObserver);
        }
        return userDao;
    }

    /**
     * Gets FanDao with sync observer configured.
     *
     * @return cached or new FanDao instance
     */
    public FanDao getFanDao() {
        if (fanDao == null) {
            fanDao = new FanDaoFactory().getFanDao(this.persistenceType);
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) fanDao).addObserver(syncObserver);
        }
        return fanDao;
    }

    /**
     * Gets VenueManagerDao with sync observer configured.
     *
     * @return cached or new VenueManagerDao instance
     */
    public VenueManagerDao getVenueManagerDao() {
        if (venueManagerDao == null) {
            venueManagerDao = new VenueManagerDaoFactory().getVenueManagerDao(this.persistenceType);
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) venueManagerDao).addObserver(syncObserver);
        }
        return venueManagerDao;
    }

    /**
     * Gets VenueDao with sync observer configured.
     *
     * @return cached or new VenueDao instance
     */
    public VenueDao getVenueDao() {
        if (venueDao == null) {
            venueDao = new VenueDaoFactory().getVenueDao(this.persistenceType);
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) venueDao).addObserver(syncObserver);
        }
        return venueDao;
    }

    /**
     * Gets BookingDao with both sync and notification observers.
     *
     * <p>BookingDao has two observers:
     * <ul>
     *   <li>CrossPersistenceSyncObserver for MySQL ↔ CSV sync</li>
     *   <li>NotificationBookingObserver for automatic notifications</li>
     * </ul>
     * </p>
     *
     * @return cached or new BookingDao instance
     */
    public BookingDao getBookingDao() {
        if (bookingDao == null) {
            bookingDao = new BookingDaoFactory().getBookingDao(this.persistenceType);

            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) bookingDao).addObserver(syncObserver);

            DaoObserver notificationObserver = observerFactory.getNotificationBookingObserver();
            ((ObservableDao) bookingDao).addObserver(notificationObserver);
        }
        return bookingDao;
    }

    /**
     * Gets NotificationDao with sync observer configured.
     *
     * @return cached or new NotificationDao instance
     */
    public NotificationDao getNotificationDao() {
        if (notificationDao == null) {
            notificationDao = new NotificationDaoFactory().getNotificationDao(this.persistenceType);
            DaoObserver syncObserver = getSyncObserver();
            notificationDao.addObserver(syncObserver);
        }
        return notificationDao;
    }

    /**
     * Gets appropriate sync observer based on persistence type.
     */
    private DaoObserver getSyncObserver() {
        if (this.persistenceType == PersistenceType.MYSQL) {
            return observerFactory.getMySqlToCsvObserver();
        } else {
            return observerFactory.getCsvToMySqlObserver();
        }
    }
}