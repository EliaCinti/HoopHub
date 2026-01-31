package it.uniroma2.hoophub.patterns.facade;

/**
 * Enumeration of available persistence mechanisms.
 *
 * <p>Supports multiple persistence strategies:</p>
 * <ul>
 *   <li><b>MYSQL:</b> Production database with ACID transactions</li>
 *   <li><b>CSV:</b> File-based persistence for development/testing</li>
 *   <li><b>IN_MEMORY:</b> RAM-only persistence for demos (no sync, data lost on exit)</li>
 * </ul>
 *
 * <p>MYSQL and CSV support automatic synchronization via Observer pattern.
 * IN_MEMORY is standalone with no synchronization.</p>
 *
 * @author Elia Cinti
 * @version 1.1
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
    CSV,

    /**
     * In-memory RAM persistence.
     * Data exists only during application runtime.
     * No external dependencies (no DB, no files).
     * Demonstrates Dependency Inversion Principle.
     */
    IN_MEMORY
}