package it.uniroma2.hoophub.patterns.observer;

/**
 * Defines the contract for observable Data Access Objects in the Observer pattern implementation.
 * <p>
 * This interface enables DAO implementations to notify interested observers about
 * data changes (insertions, updates, deletions). It's a key component in the
 * cross-persistence synchronization system that keeps CSV and MySQL data stores
 * in sync automatically.
 * </p>
 */
public interface ObservableDao {

    /**
     * Registers an observer to receive notifications about data changes.
     *
     * @param observer The observer to register for notifications
     */
    void addObserver(DaoObserver observer);

    /**
     * Unregisters an observer from receiving notifications.
     *
     * @param observer The observer to remove from notifications
     */
    void removeObserver(DaoObserver observer);

    /**
     * Notifies all registered observers about a data operation.
     *
     * @param operation The type of operation performed (INSERT, UPDATE, DELETE)
     * @param entityType The type of entity that was modified
     * @param entityId The unique identifier of the affected entity
     * @param entity The entity object (for INSERT/UPDATE) or null (for DELETE)
     */
    void notifyObservers(DaoOperation operation, String entityType, String entityId, Object entity);
}
