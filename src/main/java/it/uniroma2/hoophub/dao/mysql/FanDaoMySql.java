package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the FanDao interface.
 * <p>
 * This class provides data access operations for Fan entities stored in a MySQL database.
 * It coordinates between the users and fans tables, delegating common user operations
 * to {@link UserDaoMySql} while handling fan-specific data directly.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>users table</strong>: username (PK), password_hash, full_name, gender, user_type</li>
 *   <li><strong>fans table</strong>: username (PK, FK), fav_team, birthday</li>
 * </ul>
 * </p>
 *
 * @see FanDao
 * @see AbstractMySqlDao
 */
public class FanDaoMySql extends AbstractMySqlDao implements FanDao {

    private final UserDao userDao;

    // SQL Queries
    private static final String SQL_INSERT_FAN =
            "INSERT INTO fans (username, fav_team, birthday) VALUES (?, ?, ?)";

    private static final String SQL_SELECT_FAN =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "f.fav_team, f.birthday " +
                    "FROM users u INNER JOIN fans f ON u.username = f.username " +
                    "WHERE u.username = ?";

    private static final String SQL_SELECT_ALL_FANS =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "f.fav_team, f.birthday " +
                    "FROM users u INNER JOIN fans f ON u.username = f.username";

    private static final String SQL_UPDATE_FAN =
            "UPDATE fans SET fav_team = ?, birthday = ? WHERE username = ?";

    private static final String SQL_DELETE_FAN =
            "DELETE FROM fans WHERE username = ?";

    // Error messages
    private static final String ERR_NULL_FAN_BEAN = "FanBean cannot be null";
    private static final String ERR_NULL_FAN = "Fan cannot be null";
    private static final String ERR_FAN_NOT_FOUND = "Fan not found";

    /**
     * Constructs a new FanDaoMySql with a UserDao dependency.
     * <p>
     * The UserDao is used to handle common user operations, following the
     * DRY principle and avoiding code duplication.
     * </p>
     */
    public FanDaoMySql() {
        this.userDao = new UserDaoMySql();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs two operations within a transaction:
     * <ol>
     *   <li>Saves common user data via {@link UserDao#saveUser(UserBean)}</li>
     *   <li>Saves fan-specific data in the fans table</li>
     * </ol>
     * If either operation fails, the entire transaction is rolled back.
     * </p>
     */
    @Override
    public void saveFan(FanBean fanBean) throws DAOException {
        validateFanBeanInput(fanBean);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Save common user data first
            userDao.saveUser(fanBean);

            // Then save fan-specific data
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_FAN)) {
                stmt.setString(1, fanBean.getUsername());
                stmt.setString(2, fanBean.getFavTeam());
                stmt.setDate(3, Date.valueOf(fanBean.getBirthday()));

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    logger.log(Level.INFO, "Fan saved successfully: {0}", fanBean.getUsername());
                    notifyObservers(DaoOperation.INSERT, "Fan", fanBean.getUsername(), fanBean);
                } else {
                    conn.rollback();
                    throw new DAOException("Failed to insert fan-specific data");
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during fan save", e);
            throw new DAOException("Error saving fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves fan data using a JOIN between users and fans tables.
     * The returned Fan object has an empty booking list - bookings should be
     * loaded separately when needed.
     * </p>
     */
    @Override
    public Fan retrieveFan(String username) throws DAOException {
        validateUsernameInput(username);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_FAN)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFan(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during fan retrieval", e);
            throw new DAOException("Error retrieving fan", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Fan> retrieveAllFans() throws DAOException {
        List<Fan> fans = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_FANS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                fans.add(mapResultSetToFan(rs));
            }

            logger.log(Level.INFO, "Retrieved {0} fans", fans.size());
            return fans;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during fans retrieval", e);
            throw new DAOException("Error retrieving all fans", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation updates both:
     * <ul>
     *   <li>Common user data via {@link UserDao#updateUser(it.uniroma2.hoophub.model.User, UserBean)}</li>
     *   <li>Fan-specific data in the fans table</li>
     * </ul>
     * </p>
     */
    @Override
    public void updateFan(Fan fan, UserBean userBean) throws DAOException {
        validateFanInput(fan);
        validateUserBeanInput(userBean);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Update common user data first
            userDao.updateUser(fan, userBean);

            // Then update fan-specific data
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_FAN)) {
                stmt.setString(1, fan.getFavTeam());
                stmt.setDate(2, Date.valueOf(fan.getBirthday()));
                stmt.setString(3, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during fan update", e);
            throw new DAOException("Error updating fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation deletes in the correct order due to foreign key constraints:
     * <ol>
     *   <li>Delete fan-specific data from fans table</li>
     *   <li>Delete common user data via {@link UserDao#deleteUser(it.uniroma2.hoophub.model.User)}</li>
     * </ol>
     * </p>
     */
    @Override
    public void deleteFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Delete fan-specific data first
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_FAN)) {
                stmt.setString(1, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // Then delete common user data
                    userDao.deleteUser(fan);

                    conn.commit();
                    logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during fan deletion", e);
            throw new DAOException("Error deleting fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a ResultSet row to a Fan domain object.
     * <p>
     * The ResultSet must contain all required columns from the JOIN query.
     * </p>
     */
    private Fan mapResultSetToFan(ResultSet rs) throws SQLException {
        return new Fan.Builder()
                .username(rs.getString("username"))
                .fullName(rs.getString("full_name"))
                .gender(rs.getString("gender"))
                .favTeam(rs.getString("fav_team"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .bookingList(new ArrayList<>())  // Empty list - bookings loaded separately
                .build();
    }

    // ========== VALIDATION METHODS ==========

    private void validateFanBeanInput(FanBean fanBean) {
        if (fanBean == null) {
            throw new IllegalArgumentException(ERR_NULL_FAN_BEAN);
        }
    }

    private void validateFanInput(Fan fan) {
        if (fan == null) {
            throw new IllegalArgumentException(ERR_NULL_FAN);
        }
    }
}