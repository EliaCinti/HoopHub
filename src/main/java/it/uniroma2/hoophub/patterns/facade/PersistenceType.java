package it.uniroma2.hoophub.patterns.facade;

/**
 * Enumeration of available persistence mechanisms.
 *
 * <p>Supports dual persistence with automatic synchronization
 * between storage types via the Observer pattern.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see DaoFactoryFacade
 */
public enum PersistenceType {
    /**
     * MySQL database persistence with ACID transactions.
     * Recommended for production deployments.
     */
    MYSQL,

    /**
     * CSV file-based persistence.
     * Ideal for development and testing.
     */
    CSV
}