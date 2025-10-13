package it.uniroma2.hoophub.patterns.facade;

/**
 * Enumeration representing the available persistence mechanisms in MindHarbor.
 * <p>
 * This enum defines the supported data storage strategies that the application
 * can use. The system supports dual persistence with automatic synchronization
 * between different storage types, allowing for flexible deployment scenarios.
 * </p>
 * <p>
 * Persistence characteristics:
 * <ul>
 *   <li><strong>MYSQL</strong>: Full relational database with ACID properties, optimal for production</li>
 *   <li><strong>CSV</strong>: Simple file-based storage, ideal for development and testing</li>
 * </ul>
 * </p>
 * <p>
 * The application can operate with either persistence type as primary, while
 * maintaining automatic synchronization with the secondary type through the
 * Observer pattern implementation.
 * </p>
 *
 * @see it.uniroma2.hoophub.patterns.factory Factory classes for DAO creation
 * @see it.uniroma2.hoophub.sync.CrossPersistenceSyncObserver for synchronization
 * @see DaoFactoryFacade for persistence type management
 */
public enum PersistenceType {
    /**
     * MySQL database persistence.
     * <p>
     * Provides full relational database capabilities with ACID transactions,
     * referential integrity, and optimized query performance. Recommended
     * for production deployments where data consistency and performance
     * are critical.
     * </p>
     */
    MYSQL,

    /**
     * CSV file-based persistence.
     * <p>
     * Provides simple file-based storage using CSV format. Ideal for
     * development, testing, and scenarios where database setup is not
     * feasible. Data is stored in human-readable format for easy inspection.
     * </p>
     */
    CSV
}
