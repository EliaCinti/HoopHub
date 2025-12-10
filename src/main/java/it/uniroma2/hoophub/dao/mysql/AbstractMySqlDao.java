package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.UserBean;
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
 * <p>
 * Provides common functionality including:
 * <ul>
 * <li><strong>Centralized Caching:</strong> Delegates to {@link GlobalCache}.</li>
 * <li>Transaction management</li>
 * <li>Input validation</li>
 * </ul>
 * </p>
 */
public abstract class AbstractMySqlDao extends AbstractObservableDao {

    protected final Logger logger;

    // NOTA: La mappa locale 'cache' è stata rimossa in favore di GlobalCache

    protected static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    protected static final String ERR_NULL_USER_BEAN = "UserBean cannot be null";
    protected static final String ERR_INVALID_ID = "ID must be positive";

    protected AbstractMySqlDao() {
        this.logger = Logger.getLogger(this.getClass().getName());
    }

    // =================================================================================
    // CACHE MANAGEMENT METHODS (Delegated to GlobalCache)
    // =================================================================================

    /**
     * Genera una chiave univoca per la cache globale (es. "Booking:10").
     */
    protected String generateCacheKey(Class<?> clazz, Object id) {
        if (id == null) return null;
        return clazz.getSimpleName() + ":" + id;
    }

    /**
     * Retrieves an entity from the Global Cache safely using Generics.
     *
     * @param clazz The class type of the object (e.g., Booking.class)
     * @param key The unique identifier (ID or Username)
     * @return The entity if found and matches the type, null otherwise
     */
    protected <T> T getFromCache(Class<T> clazz, Object key) {
        String cacheKey = generateCacheKey(clazz, key);
        Object cachedObject = GlobalCache.getInstance().get(cacheKey);

        // Il cast è sicuro perché controllato dalla firma del metodo
        return cachedObject != null ? clazz.cast(cachedObject) : null;
    }

    /**
     * Stores an entity in the Global Cache.
     * The key is automatically generated based on the entity's class and the ID.
     *
     * @param entity The entity to cache
     * @param key The unique identifier for the entity
     */
    protected void putInCache(Object entity, Object key) {
        if (entity != null && key != null) {
            String cacheKey = generateCacheKey(entity.getClass(), key);
            GlobalCache.getInstance().put(cacheKey, entity);
        }
    }

    /**
     * Removes an entity from the Global Cache.
     *
     * @param clazz The class type of the object to remove
     * @param key The unique identifier
     */
    protected void removeFromCache(Class<?> clazz, Object key) {
        if (key != null) {
            String cacheKey = generateCacheKey(clazz, key);
            GlobalCache.getInstance().remove(cacheKey);
        }
    }

    /**
     * Clears the entire Global Cache.
     * Useful for testing or session reset.
     */
    public void clearCache() {
        GlobalCache.getInstance().clearAll();
    }

    // =================================================================================
    // TRANSACTION & VALIDATION METHODS (INVARIATI)
    // =================================================================================

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

    protected void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error resetting auto-commit", ex);
            }
        }
    }

    protected void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }

    protected void validateUserBeanInput(UserBean userBean) {
        if (userBean == null) {
            throw new IllegalArgumentException(ERR_NULL_USER_BEAN);
        }
    }

    protected void validateIdInput(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    @FunctionalInterface
    protected interface PreparedStatementProvider {
        PreparedStatement create(Connection conn) throws SQLException;
    }

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

    @FunctionalInterface
    protected interface TransactionOperation<T> {
        T execute(Connection connection) throws SQLException, DAOException;
    }

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

    @SuppressWarnings("SameParameterValue")
    protected void executeInTransactionVoid(TransactionOperation<Void> operation, String errorMessage) throws DAOException {
        executeInTransaction(operation, errorMessage);
    }
}
