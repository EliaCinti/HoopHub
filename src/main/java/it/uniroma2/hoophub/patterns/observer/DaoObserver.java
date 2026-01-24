package it.uniroma2.hoophub.patterns.observer;

/**
 * Observer interface for the <b>Observer pattern (GoF)</b>.
 *
 * <p>Allows objects to receive notifications when data changes occur
 * in observable DAOs. Used primarily by cross-persistence synchronization
 * and notification systems.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see ObservableDao
 */
public interface DaoObserver {

    /**
     * Called after entity insertion.
     *
     * @param entityType type of entity (e.g., "Fan", "Booking")
     * @param entityId   unique identifier
     * @param entity     the inserted entity object
     */
    void onAfterInsert(String entityType, String entityId, Object entity);

    /**
     * Called after entity update.
     *
     * @param entityType type of entity
     * @param entityId   unique identifier
     * @param entity     the updated entity object
     */
    void onAfterUpdate(String entityType, String entityId, Object entity);

    /**
     * Called after entity deletion.
     *
     * @param entityType type of entity
     * @param entityId   unique identifier of deleted entity
     */
    void onAfterDelete(String entityType, String entityId);
}