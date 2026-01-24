package it.uniroma2.hoophub.patterns.observer;

/**
 * Subject interface for the <b>Observer pattern (GoF)</b>.
 *
 * <p>Enables DAO implementations to notify observers about data changes.
 * Key component in cross-persistence synchronization that keeps CSV
 * and MySQL in sync automatically.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see DaoObserver
 */
public interface ObservableDao {

    /**
     * Registers an observer for notifications.
     *
     * @param observer the observer to register
     */
    void addObserver(DaoObserver observer);

    /**
     * Unregisters an observer.
     *
     * @param observer the observer to remove
     */
    void removeObserver(DaoObserver observer);

    /**
     * Notifies all observers about a data operation.
     *
     * @param operation  operation type (INSERT, UPDATE, DELETE)
     * @param entityType type of modified entity
     * @param entityId   identifier of affected entity
     * @param entity     entity object (null for DELETE)
     */
    void notifyObservers(DaoOperation operation, String entityType, String entityId, Object entity);
}