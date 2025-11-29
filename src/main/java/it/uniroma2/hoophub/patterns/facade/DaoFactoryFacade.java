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
 * Facade that provides a unified interface to all DAO factories and manages cross-persistence synchronization.
 * <p>
 * This class implements both the Singleton and Facade patterns to:
 * <ul>
 *   <li>Provide a single point of access to all DAO factories</li>
 *   <li>Hide the complexity of DAO creation and observer management</li>
 *   <li>Ensure consistent persistence type configuration across the application</li>
 *   <li>Manage automatic cross-persistence synchronization observers</li>
 * </ul>
 * </p>
 * <p>
 * The facade automatically configures the appropriate synchronization observers
 * based on the current persistence type:
 * <ul>
 *   <li>When using <strong>MySQL</strong> as primary: syncs changes to CSV</li>
 *   <li>When using <strong>CSV</strong> as primary: syncs changes to MySQL</li>
 * </ul>
 * </p>
 * <p>
 * DAOs are cached after first creation and cleared when persistence type changes,
 * ensuring optimal performance while maintaining consistency.
 * </p>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is required for this Facade
public class DaoFactoryFacade {
    private static DaoFactoryFacade instance;

    private PersistenceType persistenceType;
    private UserDao userDao;
    private FanDao fanDao;
    private VenueManagerDao venueManagerDao;
    private VenueDao venueDao;
    private BookingDao bookingDao;
    private NotificationDao notificationDao;

    // Observer factory for creating and managing observers (Singleton + Factory pattern)
    private final ObserverFactory observerFactory = ObserverFactory.getInstance();

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private DaoFactoryFacade() {
        /* no instance */
    }

    /**
     * Returns the singleton instance of the DAO factory facade.
     * <p>
     * This method ensures that only one instance of the facade exists
     * throughout the application lifecycle, providing consistent access
     * to DAO factories and synchronization configuration.
     * </p>
     *
     * @return The singleton DaoFactoryFacade instance
     */
    public static synchronized DaoFactoryFacade getInstance() {
        if (instance == null) {
            instance = new DaoFactoryFacade();
        }
        return instance;
    }

    /**
     * Gets the currently configured persistence type.
     *
     * @return The current persistence type (MYSQL or CSV)
     */
    public PersistenceType getPersistenceType() {
        return persistenceType;
    }

    /**
     * Sets the persistence type and clears the DAO cache if the type changes.
     * <p>
     * When the persistence type changes, all cached DAO instances are cleared
     * to ensure that subsequent requests create DAOs of the correct type with
     * appropriate observer configurations.
     * </p>
     *
     * @param persistenceType The new persistence type to use
     */
    public void setPersistenceType(PersistenceType persistenceType) {
        if (this.persistenceType != persistenceType) {
            this.persistenceType = persistenceType;
            // Clear DAO cache when type changes
            this.userDao = null;
            this.fanDao = null;
            this.venueManagerDao = null;
            this.venueDao = null;
            this.bookingDao = null;
            this.notificationDao = null;
        }
    }

    /**
     * Gets a UserDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no UserDao
     * exists, it creates one using the appropriate factory and configures
     * the cross-persistence synchronization observer.
     * </p>
     *
     * @return A UserDao instance with synchronization capabilities
     */
    public UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDaoFactory().getUserDao(this.persistenceType);
            // Connect the appropriate sync observer using ObserverFactory
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) userDao).addObserver(syncObserver);
        }
        return userDao;
    }

    /**
     * Gets a FanDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no FanDao
     * exists, it creates one using the appropriate factory and configures
     * the cross-persistence synchronization observer.
     * </p>
     *
     * @return A FanDao instance with synchronization capabilities
     */
    public FanDao getFanDao() {
        if (fanDao == null) {
            fanDao = new FanDaoFactory().getFanDao(this.persistenceType);
            // Connect the appropriate sync observer using ObserverFactory
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) fanDao).addObserver(syncObserver);
        }
        return fanDao;
    }

    /**
     * Gets a VenueManagerDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no VenueManagerDao
     * exists, it creates one using the appropriate factory and configures
     * the cross-persistence synchronization observer.
     * </p>
     *
     * @return A VenueManagerDao instance with synchronization capabilities
     */
    public VenueManagerDao getVenueManagerDao() {
        if (venueManagerDao == null) {
            venueManagerDao = new VenueManagerDaoFactory().getVenueManagerDao(this.persistenceType);
            // Connect the appropriate sync observer using ObserverFactory
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) venueManagerDao).addObserver(syncObserver);
        }
        return venueManagerDao;
    }

    /**
     * Gets a VenueDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no VenueDao
     * exists, it creates one using the appropriate factory and configures
     * the cross-persistence synchronization observer.
     * </p>
     *
     * @return A VenueDao instance with synchronization capabilities
     */
    public VenueDao getVenueDao() {
        if (venueDao == null) {
            venueDao = new VenueDaoFactory().getVenueDao(this.persistenceType);
            // Connect the appropriate sync observer using ObserverFactory
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) venueDao).addObserver(syncObserver);
        }
        return venueDao;
    }

    /**
     * Gets a BookingDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no BookingDao
     * exists, it creates one using the appropriate factory and configures
     * BOTH the cross-persistence synchronization observer AND the notification observer.
     * </p>
     * <p>
     * <strong>Observer Pattern:</strong> BookingDao has TWO observers:
     * <ul>
     *   <li>CrossPersistenceSyncObserver - for MySQL ↔ CSV synchronization</li>
     *   <li>NotificationBookingObserver - for automatic notification creation</li>
     * </ul>
     * </p>
     *
     * @return A BookingDao instance with synchronization and notification capabilities
     */
    public BookingDao getBookingDao() {
        if (bookingDao == null) {
            bookingDao = new BookingDaoFactory().getBookingDao(this.persistenceType);

            // Observer 1: Sync observer for cross-persistence synchronization
            DaoObserver syncObserver = getSyncObserver();
            ((ObservableDao) bookingDao).addObserver(syncObserver);

            // Observer 2: Notification observer for automatic notification generation
            DaoObserver notificationObserver = observerFactory.getNotificationBookingObserver();
            ((ObservableDao) bookingDao).addObserver(notificationObserver);
        }
        return bookingDao;
    }

    /**
     * Gets a NotificationDao instance configured for the current persistence type.
     * <p>
     * This method implements lazy initialization and caching. If no NotificationDao
     * exists, it creates one using the appropriate factory and configures
     * the cross-persistence synchronization observer.
     * </p>
     *
     * @return A NotificationDao instance with synchronization capabilities
     */
    public NotificationDao getNotificationDao() {
        if (notificationDao == null) {
            notificationDao = new NotificationDaoFactory().getNotificationDao(this.persistenceType);
            // Connect the appropriate sync observer using ObserverFactory
            DaoObserver syncObserver = getSyncObserver();
            notificationDao.addObserver(syncObserver);
        }
        return notificationDao;
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Gets the appropriate sync observer based on current persistence type.
     * <p>
     * <strong>Polymorphic Solution:</strong> This method uses the ObserverFactory
     * to get the correct CrossPersistenceSyncObserver based on the persistence type.
     * </p>
     *
     * @return The appropriate sync observer (MySQL→CSV or CSV→MySQL)
     */
    private DaoObserver getSyncObserver() {
        if (this.persistenceType == PersistenceType.MYSQL) {
            return observerFactory.getMySqlToCsvObserver();
        } else {
            return observerFactory.getCsvToMySqlObserver();
        }
    }
}