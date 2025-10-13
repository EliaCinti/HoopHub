package it.uniroma2.hoophub.patterns.observer;

/**
 * Enumeration of database operations that can be observed in the DAO layer.
 * <p>
 * This enum defines the three fundamental CRUD operations that trigger
 * observer notifications in the cross-persistence synchronization system.
 * Each operation type may carry different data and require different
 * handling by observers.
 * </p>
 */
public enum DaoOperation {
    /**
     * Represents an entity insertion operation.
     * Triggered when new entities are created in the persistence layer.
     */
    INSERT,

    /**
     * Represents an entity update operation.
     * Triggered when existing entities are modified in the persistence layer.
     */
    UPDATE,

    /**
     * Represents an entity deletion operation.
     * Triggered when entities are removed from the persistence layer.
     */
    DELETE
}