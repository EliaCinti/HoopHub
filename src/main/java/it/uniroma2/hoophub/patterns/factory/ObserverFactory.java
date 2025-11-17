package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.NotificationBookingObserver;
import it.uniroma2.hoophub.sync.CrossPersistenceSyncObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating Observer instances.
 * <p>
 * This factory implements both the Singleton and Factory patterns to provide
 * a centralized, type-safe way to create and manage DAO observers. It ensures
 * that observer instances are created consistently and can be reused across
 * the application.
 * </p>
 * <p>
 * <strong>Pattern Combination: Singleton + Abstract Factory</strong>
 * As required by ISPW course guidelines, this class combines:
 * <ul>
 *   <li><strong>Singleton</strong>: Single instance with synchronized access</li>
 *   <li><strong>Abstract Factory</strong>: Creates families of related observers</li>
 *   <li><strong>Polymorphic Solution</strong>: Switch/case for observer type selection</li>
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 */
public class ObserverFactory {

    /**
     * Singleton instance (static as required by ISPW guidelines).
     */
    private static ObserverFactory instance;

    /**
     * Cached observers for reuse (avoid creating duplicates).
     */
    private CrossPersistenceSyncObserver mysqlToCsvObserver;
    private CrossPersistenceSyncObserver csvToMysqlObserver;
    private NotificationBookingObserver notificationBookingObserver;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private ObserverFactory() {
        /* Singleton - no public instantiation */
    }

    /**
     * Returns the singleton instance of ObserverFactory.
     * <p>
     * <strong>ISPW Requirement:</strong> Method is synchronized and static.
     * This ensures thread-safe lazy initialization of the singleton instance.
     * </p>
     *
     * @return The singleton ObserverFactory instance
     */
    public static synchronized ObserverFactory getInstance() {
        if (instance == null) {
            instance = new ObserverFactory();
        }
        return instance;
    }

    /**
     * Gets the CrossPersistenceSyncObserver for MySQL→CSV synchronization.
     * <p>
     * This observer is cached and reused to avoid creating multiple instances.
     * </p>
     *
     * @return CrossPersistenceSyncObserver configured for MySQL as source
     */
    public CrossPersistenceSyncObserver getMySqlToCsvObserver() {
        if (mysqlToCsvObserver == null) {
            mysqlToCsvObserver = new CrossPersistenceSyncObserver(PersistenceType.MYSQL);
        }
        return mysqlToCsvObserver;
    }

    /**
     * Gets the CrossPersistenceSyncObserver for CSV→MySQL synchronization.
     * <p>
     * This observer is cached and reused to avoid creating multiple instances.
     * </p>
     *
     * @return CrossPersistenceSyncObserver configured for CSV as source
     */
    public CrossPersistenceSyncObserver getCsvToMySqlObserver() {
        if (csvToMysqlObserver == null) {
            csvToMysqlObserver = new CrossPersistenceSyncObserver(PersistenceType.CSV);
        }
        return csvToMysqlObserver;
    }

    /**
     * Gets the NotificationBookingObserver for automatic notification generation.
     * <p>
     * This observer listens for Booking changes and creates notifications.
     * It is cached and reused to avoid creating multiple instances.
     * </p>
     *
     * @return NotificationBookingObserver instance
     */
    public NotificationBookingObserver getNotificationBookingObserver() {
        if (notificationBookingObserver == null) {
            notificationBookingObserver = new NotificationBookingObserver();
        }
        return notificationBookingObserver;
    }

    /**
     * Polymorphic factory method to get observer by type using switch/case.
     * <p>
     * <strong>ISPW Requirement:</strong> Polymorphic solution with switch/case.
     * This method demonstrates type-safe observer selection at runtime.
     * </p>
     *
     * @param observerType The type of observer to create
     * @param persistenceType The persistence type (for sync observers)
     * @return The appropriate DaoObserver instance
     */
    public DaoObserver getObserver(ObserverType observerType, PersistenceType persistenceType) {
        return switch (observerType) {
            case CROSS_PERSISTENCE_SYNC -> getSyncObserver(persistenceType);
            case NOTIFICATION_BOOKING -> getNotificationBookingObserver();
        };
    }

    /**
     * Gets all observers that should be registered on a DAO.
     * <p>
     * This is a convenience method that returns both sync and notification observers.
     * </p>
     *
     * @param currentPersistenceType The current persistence type
     * @return List of all observers to register
     */
    public List<DaoObserver> getAllObservers(PersistenceType currentPersistenceType) {
        List<DaoObserver> observers = new ArrayList<>();
        
        // Add sync observer (opposite persistence type)
        observers.add(getSyncObserver(currentPersistenceType));
        
        // Add notification observer
        observers.add(getNotificationBookingObserver());
        
        return observers;
    }

    /**
     * Helper method to get the appropriate sync observer based on persistence type.
     */
    private CrossPersistenceSyncObserver getSyncObserver(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case MYSQL -> getMySqlToCsvObserver();
            case CSV -> getCsvToMySqlObserver();
        };
    }

    /**
     * Enumeration of observer types for polymorphic selection.
     */
    public enum ObserverType {
        /**
         * Observer for cross-persistence synchronization (MySQL ↔ CSV).
         */
        CROSS_PERSISTENCE_SYNC,

        /**
         * Observer for automatic notification generation on booking changes.
         */
        NOTIFICATION_BOOKING
    }
}
