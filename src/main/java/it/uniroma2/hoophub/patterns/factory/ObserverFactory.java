package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.NotificationBookingObserver;
import it.uniroma2.hoophub.sync.CrossPersistenceSyncObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Observer instances combining <b>Singleton pattern (GoF)</b>
 * with <b>Abstract Factory pattern (GoF)</b>.
 *
 * <p>Provides centralized, type-safe creation and caching of DAO observers.
 * Uses polymorphic switch/case for observer type selection.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
@SuppressWarnings("java:S6548")
public class ObserverFactory {

    private static ObserverFactory instance;

    private CrossPersistenceSyncObserver mysqlToCsvObserver;
    private CrossPersistenceSyncObserver csvToMysqlObserver;
    private NotificationBookingObserver notificationBookingObserver;

    private ObserverFactory() {
        /* Singleton */
    }

    /**
     * Returns the singleton instance.
     *
     * @return the ObserverFactory singleton
     */
    public static synchronized ObserverFactory getInstance() {
        if (instance == null) {
            instance = new ObserverFactory();
        }
        return instance;
    }

    /**
     * Gets cached MySQL→CSV sync observer.
     *
     * @return CrossPersistenceSyncObserver for MySQL source
     */
    public CrossPersistenceSyncObserver getMySqlToCsvObserver() {
        if (mysqlToCsvObserver == null) {
            mysqlToCsvObserver = new CrossPersistenceSyncObserver(PersistenceType.MYSQL);
        }
        return mysqlToCsvObserver;
    }

    /**
     * Gets cached CSV→MySQL sync observer.
     *
     * @return CrossPersistenceSyncObserver for CSV source
     */
    public CrossPersistenceSyncObserver getCsvToMySqlObserver() {
        if (csvToMysqlObserver == null) {
            csvToMysqlObserver = new CrossPersistenceSyncObserver(PersistenceType.CSV);
        }
        return csvToMysqlObserver;
    }

    /**
     * Gets cached notification observer for bookings.
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
     * Polymorphic factory method using switch/case.
     *
     * @param observerType    type of observer to create
     * @param persistenceType persistence type (for sync observers)
     * @return appropriate DaoObserver instance
     */
    public DaoObserver getObserver(ObserverType observerType, PersistenceType persistenceType) {
        return switch (observerType) {
            case CROSS_PERSISTENCE_SYNC -> getSyncObserver(persistenceType);
            case NOTIFICATION_BOOKING -> getNotificationBookingObserver();
        };
    }

    /**
     * Gets all observers for a DAO (sync + notification).
     *
     * @param currentPersistenceType current persistence type
     * @return list of observers to register
     */
    public List<DaoObserver> getAllObservers(PersistenceType currentPersistenceType) {
        List<DaoObserver> observers = new ArrayList<>();
        observers.add(getSyncObserver(currentPersistenceType));
        observers.add(getNotificationBookingObserver());
        return observers;
    }

    private CrossPersistenceSyncObserver getSyncObserver(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case MYSQL -> getMySqlToCsvObserver();
            case CSV -> getCsvToMySqlObserver();
        };
    }

    /**
     * Observer types for polymorphic selection.
     */
    public enum ObserverType {
        /** Cross-persistence synchronization observer. */
        CROSS_PERSISTENCE_SYNC,
        /** Automatic notification generation observer. */
        NOTIFICATION_BOOKING
    }
}