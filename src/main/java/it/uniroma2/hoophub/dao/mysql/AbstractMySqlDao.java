package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.exception.DAOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for MySQL DAO implementations.
 * <p>
 * This class provides common functionality shared across MySQL DAO implementations,
 * reducing code duplication and ensuring consistent error handling and validation.
 * </p>
 * <p>
 * Common functionality includes:
 * <ul>
 *   <li>Transaction management (rollback, auto-commit reset)</li>
 *   <li>Input validation (username, UserBean)</li>
 *   <li>Logger instance for subclasses</li>
 * </ul>
 * </p>
 * <p>
 * This class extends {@link AbstractObservableDao} to support the Observer pattern
 * for data synchronization between different persistence types.
 * </p>
 *
 * @see AbstractObservableDao
 */
public abstract class AbstractMySqlDao extends AbstractObservableDao {

    /**
     * Logger instance that subclasses can use for logging.
     * Initialized with the subclass's actual class name.
     */
    protected final Logger logger;

    /**
     * Common error messages.
     */
    protected static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    protected static final String ERR_NULL_USER_BEAN = "UserBean cannot be null";
    protected static final String ERR_INVALID_ID = "ID must be positive";

    /**
     * Constructs a new AbstractMySqlDao.
     * <p>
     * Initializes the logger with the actual subclass's class name,
     * ensuring proper logging context.
     * </p>
     */
    protected AbstractMySqlDao() {
        this.logger = Logger.getLogger(this.getClass().getName());
    }

    // ========== TRANSACTION MANAGEMENT ==========

    /**
     * Safely rolls back a transaction, logging any errors that occur.
     * <p>
     * This method should be called in catch blocks when a transaction fails.
     * It handles null connections gracefully and logs any SQLException that
     * occurs during rollback.
     * </p>
     *
     * @param conn The database connection to rollback (can be null)
     */
    protected void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                logger.info("Transaction rolled back");
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error rolling back transaction", ex);
            }
        }
    }

    /**
     * Safely resets auto-commit mode to true, logging any errors that occur.
     * <p>
     * This method should be called in finally blocks after manual transaction
     * management. It ensures the connection is returned to auto-commit mode
     * for subsequent operations.
     * </p>
     *
     * @param conn The database connection to reset (can be null)
     */
    protected void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error resetting auto-commit", ex);
            }
        }
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validates that a username is not null or empty.
     * <p>
     * This validation is commonly used in retrieve, update, and delete operations
     * that require a username as input.
     * </p>
     *
     * @param username The username to validate
     * @throws IllegalArgumentException if username is null or empty
     */
    protected void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }

    /**
     * Validates that a UserBean is not null.
     * <p>
     * This validation is commonly used in update operations that require
     * a UserBean containing updated user data.
     * </p>
     *
     * @param userBean The UserBean to validate
     * @throws IllegalArgumentException if userBean is null
     */
    protected void validateUserBeanInput(UserBean userBean) {
        if (userBean == null) {
            throw new IllegalArgumentException(ERR_NULL_USER_BEAN);
        }
    }

    /**
     * Validates that an ID (int) is positive.
     * <p>
     * This validation is commonly used in retrieve, update, and delete operations
     * that require a positive integer ID as input (booking ID, venue ID, etc.).
     * </p>
     *
     * @param id The ID to validate
     * @throws IllegalArgumentException if ID is zero or negative
     */
    protected void validateIdInput(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    /**
     * Validates that an ID (Long) is not null and positive.
     * <p>
     * This validation is commonly used in retrieve, update, and delete operations
     * that require a positive Long ID as input (notification ID, user ID, etc.).
     * </p>
     *
     * @param id The ID to validate
     * @throws IllegalArgumentException if ID is null or not positive
     */
    protected void validateIdInput(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    /**
     * Functional interface to create a PreparedStatement safely.
     */
    @FunctionalInterface
    protected interface PreparedStatementProvider {
        PreparedStatement create(Connection conn) throws SQLException;
    }

    /**
     * Checks if an entity exists by its ID using a provider to create the statement.
     * Removes SQL Injection warnings by delegating statement creation to the concrete class.
     *
     * @param provider The provider that creates the PreparedStatement (e.g., via lambda)
     * @param id The ID to check
     * @return true if the count is greater than 0, false otherwise
     * @throws DAOException If a database error occurs
     */
    protected boolean existsById(PreparedStatementProvider provider, int id) throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            // Qui chiamiamo il provider invece di fare prepareStatement(string)
            try (PreparedStatement stmt = provider.create(conn)) {
                stmt.setInt(1, id); // Impostiamo l'ID comune a tutti
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error checking existence by ID", e);
        }
    }

    // ========== TRANSACTION HELPERS ==========

    /**
     * Functional interface for database operations that need to execute within a transaction.
     * <p>
     * This interface allows for cleaner transaction management using lambda expressions
     * or method references, eliminating boilerplate transaction code.
     * </p>
     *
     * @param <T> The type of result returned by the operation
     */
    @FunctionalInterface
    protected interface TransactionOperation<T> {
        /**
         * Executes the database operation using the provided connection.
         *
         * @param connection The database connection within the transaction
         * @return The result of the operation
         * @throws SQLException If a database error occurs
         * @throws DAOException If a DAO-specific error occurs
         */
        T execute(Connection connection) throws SQLException, DAOException;
    }

    /**
     * Executes a database operation within a transaction.
     * The return value is generic to support both void and returning operations.
     * Suppressed warning because some callers (like void ops) intentionally ignore the result.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected <T> T executeInTransaction(TransactionOperation<T> operation, String errorMessage) throws DAOException {
        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            T result = operation.execute(conn);

            conn.commit();
            return result;

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException(errorMessage, e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * Executes a database operation within a transaction (void return).
     * <p>
     * Convenience method for operations that don't return a value.
     * </p>
     *
     * @param operation The operation to execute
     * @param errorMessage Error message to use if operation fails
     * @throws DAOException If the operation fails
     */
    @SuppressWarnings("SameParameterValue")
    protected void executeInTransactionVoid(TransactionOperation<Void> operation, String errorMessage) throws DAOException {
        executeInTransaction(operation, errorMessage);
    }
}
