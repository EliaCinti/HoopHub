package it.uniroma2.hoophub.patterns.observer;

/**
 * Defines the contract for observers in the DAO Observer pattern implementation.
 * <p>
 * This interface allows objects to receive notifications when data changes
 * occur in observable DAOs. It's primarily used by the cross-persistence
 * synchronization system to maintain data consistency between CSV files
 * and MySQL database.
 * </p>
 */
public interface DaoObserver {

    /**
     * Called after an entity has been successfully inserted into the data store.
     *
     * @param entityType The type of entity that was inserted (e.g., "Fan", "VenueManager")
     * @param entityId The unique identifier of the inserted entity
     * @param entity The complete entity data, typically a Bean object
     */
    void onAfterInsert(String entityType, String entityId, Object entity);

    /**
     * Called after an entity has been successfully updated in the data store.
     *
     * @param entityType The type of entity that was updated
     * @param entityId The unique identifier of the updated entity
     * @param entity The updated entity data, typically a Model object
     */
    void onAfterUpdate(String entityType, String entityId, Object entity);

    /**
     * Called after an entity has been successfully deleted from the data store.
     *
     * @param entityType The type of entity that was deleted
     * @param entityId The unique identifier of the deleted entity
     */
    void onAfterDelete(String entityType, String entityId);
}