package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.dao.GlobalCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for MySQL DAO implementations.
 *
 * <p>Extends {@link AbstractObservableDao} to support the <b>Observer pattern (GoF)</b>
 * for cross-persistence synchronization. Provides common functionality: centralized
 * caching via {@link GlobalCache}, transaction management, and input validation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see GlobalCache
 */
public abstract class AbstractMySqlDao extends AbstractObservableDao {

    protected final Logger logger;

    protected static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    protected static final String ERR_INVALID_ID = "ID must be positive";

    protected AbstractMySqlDao() {
        this.logger = Logger.getLogger(this.getClass().getName());
    }

    // ========== CACHE METHODS (Delegated to GlobalCache) ==========

    /**
     * Generates a unique cache key (e.g., "Booking:10").
     */
    protected String generateCacheKey(Class<?> clazz, Object id) {
        if (id == null) return null;
        return clazz.getSimpleName() + ":" + id;
    }

    /**
     * Retrieves entity from cache with type safety.
     *
     * @param clazz entity class
     * @param key   entity identifier
     * @return cached entity or null
     */
    protected <T> T getFromCache(Class<T> clazz, Object key) {
        String cacheKey = generateCacheKey(clazz, key);
        Object cachedObject = GlobalCache.getInstance().get(cacheKey);
        return cachedObject != null ? clazz.cast(cachedObject) : null;
    }

    /**
     * Stores entity in cache.
     *
     * @param entity the entity to cache
     * @param key    entity identifier
     */
    protected void putInCache(Object entity, Object key) {
        if (entity != null && key != null) {
            String cacheKey = generateCacheKey(entity.getClass(), key);
            GlobalCache.getInstance().put(cacheKey, entity);
        }
    }

    /**
     * Removes entity from cache.
     *
     * @param clazz entity class
     * @param key   entity identifier
     */
    protected void removeFromCache(Class<?> clazz, Object key) {
        if (key != null) {
            String cacheKey = generateCacheKey(clazz, key);
            GlobalCache.getInstance().remove(cacheKey);
        }
    }

    /**
     * Clears entire cache. Useful for testing or session reset.
     */
    public void clearCache() {
        GlobalCache.getInstance().clearAll();
    }

    // ========== TRANSACTION METHODS ==========

    /**
     * Rolls back a transaction safely.
     *
     * @param conn the connection to rollback
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
     * Resets auto-commit to true safely.
     *
     * @param conn the connection to reset
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

    protected void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }

    protected void validateIdInput(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    // ========== HELPER INTERFACES & METHODS ==========

    /**
     * Functional interface for creating PreparedStatements.
     */
    @FunctionalInterface
    protected interface PreparedStatementProvider {
        PreparedStatement create(Connection conn) throws SQLException;
    }

    /**
     * Checks if a record exists by ID using supplied statement provider.
     *
     * @param provider creates the COUNT query statement
     * @param id       the ID to check
     * @return true if exists
     */
    @SuppressWarnings("java:S2077")
    protected boolean existsById(PreparedStatementProvider provider, int id) throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = provider.create(conn)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error checking existence by ID", e);
        }
    }

    /**
     * Functional interface for transaction operations.
     *
     * @param <T> return type
     */
    @FunctionalInterface
    protected interface TransactionOperation<T> {
        T execute(Connection connection) throws SQLException, DAOException;
    }

    /**
     * Executes an operation within a transaction with automatic commit/rollback.
     *
     * @param operation    the operation to execute
     * @param errorMessage error message if operation fails
     * @return operation result
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
     * Executes a void operation within a transaction.
     *
     * @param operation    the operation to execute
     * @param errorMessage error message if operation fails
     */
    @SuppressWarnings("SameParameterValue")
    protected void executeInTransactionVoid(TransactionOperation<Void> operation, String errorMessage) throws DAOException {
        executeInTransaction(operation, errorMessage);
    }
}