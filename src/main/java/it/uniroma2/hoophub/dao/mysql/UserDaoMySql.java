package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link UserDao}.
 *
 * <p>Handles common user data (username, password, full name, gender, type).
 * Used by {@link FanDaoMySql} and {@link VenueManagerDaoMySql} for base user operations.
 * Passwords are verified using BCrypt.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class UserDaoMySql extends AbstractMySqlDao implements UserDao {

    private static final String SQL_VALIDATE_USER =
            "SELECT username, password_hash, user_type FROM users WHERE username = ?";

    private static final String SQL_INSERT_USER =
            "INSERT INTO users (username, password_hash, full_name, gender, user_type) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_USER =
            "SELECT username, password_hash, full_name, gender, user_type FROM users WHERE username = ?";

    private static final String SQL_CHECK_USERNAME =
            "SELECT COUNT(*) FROM users WHERE username = ?";

    private static final String SQL_UPDATE_USER =
            "UPDATE users SET full_name = ?, gender = ? WHERE username = ?";

    private static final String SQL_DELETE_USER =
            "DELETE FROM users WHERE username = ?";

    private static final String ERR_NULL_CREDENTIALS = "Credentials cannot be null";
    private static final String ERR_NULL_USER = "User cannot be null";
    private static final String ERR_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String ERR_USERNAME_EXISTS = "Username already exists";

    @Override
    public UserType validateUser(Credentials credentials) throws DAOException {
        validateCredentialsInput(credentials);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_VALIDATE_USER)) {

                stmt.setString(1, credentials.getUsername());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String typeString = rs.getString("user_type");

                        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
                            logger.log(Level.INFO, "User validated: {0}", credentials.getUsername());
                            return UserType.valueOf(typeString);
                        }
                    }
                    throw new DAOException(ERR_INVALID_CREDENTIALS);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error validating user", e);
        }
    }

    @Override
    public void saveUser(User user) throws DAOException {
        validateUserInput(user);

        if (isUsernameTaken(user.getUsername())) {
            throw new DAOException(ERR_USERNAME_EXISTS);
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_USER)) {

                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPasswordHash());
                stmt.setString(3, user.getFullName());
                stmt.setString(4, user.getGender());
                stmt.setString(5, user.getUserType().toString());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    putInCache(user, user.getUsername());

                    logger.log(Level.INFO, "User saved: {0}", user.getUsername());
                    notifyObservers(DaoOperation.INSERT, "User", user.getUsername(), user);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error saving user", e);
        }
    }

    @Override
    public String[] retrieveUser(String username) throws DAOException {
        validateUsernameInput(username);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_USER)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new String[]{
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getString("full_name"),
                                rs.getString("gender"),
                                rs.getString("user_type")
                        };
                    }
                    return new String[0];
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving user", e);
        }
    }

    @Override
    public boolean isUsernameTaken(String username) throws DAOException {
        validateUsernameInput(username);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_USERNAME)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error checking username availability", e);
        }
    }

    @Override
    public void updateUser(User user) throws DAOException {
        validateUserInput(user);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_USER)) {
                stmt.setString(1, user.getFullName());
                stmt.setString(2, user.getGender());
                stmt.setString(3, user.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    putInCache(user, user.getUsername());

                    logger.log(Level.INFO, "User updated: {0}", user.getUsername());
                    notifyObservers(DaoOperation.UPDATE, "User", user.getUsername(), user);
                } else {
                    throw new DAOException("User not found for update: " + user.getUsername());
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error updating user", e);
        }
    }

    @Override
    public void deleteUser(User user) throws DAOException {
        validateUserInput(user);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_USER)) {
                stmt.setString(1, user.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    removeFromCache(user.getClass(), user.getUsername());

                    logger.log(Level.INFO, "User deleted: {0}", user.getUsername());
                    notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error deleting user", e);
        }
    }

    // ========== PRIVATE VALIDATION ==========

    private void validateCredentialsInput(Credentials credentials) {
        if (credentials == null || credentials.getUsername() == null ||
                credentials.getUsername().trim().isEmpty() ||
                credentials.getPassword() == null || credentials.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_CREDENTIALS);
        }
    }

    private void validateUserInput(User user) {
        if (user == null) {
            throw new IllegalArgumentException(ERR_NULL_USER);
        }
    }
}