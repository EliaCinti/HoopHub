package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;

import java.sql.Connection;
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
}
