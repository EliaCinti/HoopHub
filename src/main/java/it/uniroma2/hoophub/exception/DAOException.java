package it.uniroma2.hoophub.exception;

/**
 * Exception thrown when data access operations fail in the DAO layer.
 * <p>
 * This exception serves as a comprehensive wrapper for all database and
 * persistence-related errors in the HoopHub application. It provides
 * a consistent way to handle failures across different persistence
 * mechanisms (CSV files and MySQL database) and DAO implementations.
 * </p>
 * <p>
 * Common scenarios that trigger this exception:
 * <ul>
 *   <li>Database connection failures or timeouts</li>
 *   <li>SQL syntax errors or constraint violations</li>
 *   <li>File I/O errors when working with CSV persistence</li>
 *   <li>Data validation failures during persistence operations</li>
 *   <li>Entity not found errors during retrieval operations</li>
 *   <li>Duplicate key violations during insertion operations</li>
 *   <li>Cross-persistence synchronization failures</li>
 * </ul>
 * </p>
 * <p>
 * This exception supports both simple error messages and chained exceptions,
 * allowing the preservation of the original error context when wrapping
 * lower-level exceptions from JDBC, file I/O, or CSV parsing libraries.
 * </p>
 *
 * @see it.uniroma2.hoophub.dao DAO interfaces and implementations
 * @see it.uniroma2.hoophub.sync.CrossPersistenceSyncObserver for synchronization errors
 * @see it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade for DAO creation errors
 */
public class DAOException extends Exception {

    /**
     * Constructs a new DAOException with no detail message.
     * <p>
     * This constructor creates a basic exception without any specific
     * error information. It's typically used in scenarios where the
     * error context is self-evident or will be provided through other means.
     * </p>
     */
    public DAOException() {
        super();
    }

    /**
     * Constructs a new DAOException with the specified detail message and cause.
     * <p>
     * This constructor is the most comprehensive option, allowing both a
     * descriptive error message and the preservation of the original exception
     * that caused the failure. This is particularly useful when wrapping
     * lower-level exceptions from database drivers, file I/O operations,
     * or CSV parsing libraries.
     * </p>
     *
     * @param message the detail message explaining what went wrong
     * @param cause the underlying exception that caused this DAO operation to fail
     */
    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DAOException with the specified detail message.
     * <p>
     * This constructor provides a way to create exceptions with descriptive
     * error messages that can help developers and users understand what
     * went wrong during the data access operation. The message should be
     * clear and actionable when possible.
     * </p>
     *
     * @param message the detail message explaining the specific data access issue
     */
    public DAOException(String message) {
        super(message);
    }
}

