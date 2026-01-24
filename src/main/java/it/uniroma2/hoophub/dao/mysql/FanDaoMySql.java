package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link FanDao}.
 *
 * <p>Delegates common user data to {@link UserDao} and manages Fan-specific
 * data (favorite team, birthday) in the {@code fans} table.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanDaoMySql extends AbstractMySqlDao implements FanDao {

    private final UserDao userDao;

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

    private static final String ERR_NULL_FAN = "Fan cannot be null";
    private static final String ERR_FAN_NOT_FOUND = "Fan not found";

    public FanDaoMySql(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void saveFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            userDao.saveUser(fan);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_FAN)) {
                stmt.setString(1, fan.getUsername());
                stmt.setString(2, fan.getFavTeam().name());
                stmt.setDate(3, Date.valueOf(fan.getBirthday()));

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(fan, fan.getUsername());

                    logger.log(Level.INFO, "Fan saved successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.INSERT, "Fan", fan.getUsername(), fan);
                } else {
                    conn.rollback();
                    throw new DAOException("Failed to insert fan-specific data");
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public Fan retrieveFan(String username) throws DAOException {
        validateUsernameInput(username);

        Fan cachedFan = getFromCache(Fan.class, username);
        if (cachedFan != null) {
            return cachedFan;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_FAN)) {
                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Fan fan = mapResultSetToFan(rs);
                        putInCache(fan, username);
                        return fan;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving fan", e);
        }
    }

    @Override
    public List<Fan> retrieveAllFans() throws DAOException {
        List<Fan> fans = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_FANS);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String username = rs.getString("username");
                    Fan cachedFan = getFromCache(Fan.class, username);

                    if (cachedFan != null) {
                        fans.add(cachedFan);
                    } else {
                        Fan fan = mapResultSetToFan(rs);
                        putInCache(fan, username);
                        fans.add(fan);
                    }
                }
                return fans;
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all fans", e);
        }
    }

    @Override
    public void updateFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            userDao.updateUser(fan);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_FAN)) {
                stmt.setString(1, fan.getFavTeam().name());
                stmt.setDate(2, Date.valueOf(fan.getBirthday()));
                stmt.setString(3, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(fan, fan.getUsername());

                    logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void deleteFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_FAN)) {
                stmt.setString(1, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    userDao.deleteUser(fan);

                    conn.commit();
                    removeFromCache(Fan.class, fan.getUsername());

                    logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    // ========== PRIVATE HELPERS ==========

    private Fan mapResultSetToFan(ResultSet rs) throws SQLException, DAOException {
        String username = rs.getString("username");
        String teamString = rs.getString("fav_team");

        TeamNBA team = TeamNBA.robustValueOf(teamString);
        if (team == null) {
            throw new DAOException("Invalid team for fan " + username + ": " + teamString);
        }

        String key = "Fan:" + username;
        if (DaoLoadingContext.isLoading(key)) {
            return new Fan.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .favTeam(team)
                    .birthday(rs.getDate("birthday").toLocalDate())
                    .bookingList(new ArrayList<>())
                    .build();
        }

        DaoLoadingContext.startLoading(key);
        try {
            return new Fan.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .favTeam(team)
                    .birthday(rs.getDate("birthday").toLocalDate())
                    .bookingList(new ArrayList<>())
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    private void validateFanInput(Fan fan) {
        if (fan == null) {
            throw new IllegalArgumentException(ERR_NULL_FAN);
        }
    }
}