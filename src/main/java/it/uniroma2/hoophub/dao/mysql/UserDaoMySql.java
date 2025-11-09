package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * MySQL implementation of the UserDao interface.
 * <p>
 * This class provides data access operations for User entities stored in a MySQL database.
 * It extends {@link AbstractMySqlDao} to support the Observer pattern for cross-persistence
 * synchronization. All database operations use prepared statements to prevent SQL injection attacks.
 * </p>
 * <p>
 * The implementation handles:
 * <ul>
 *   <li>User authentication and validation</li>
 *   <li>Secure password hashing using BCrypt</li>
 *   <li>CRUD operations on the users table</li>
 *   <li>Observer notifications for data changes</li>
 * </ul>
 * </p>
 *
 * @see UserDao
 * @see AbstractMySqlDao
 * @see ConnectionFactory
 */
public class UserDaoMySql extends AbstractMySqlDao implements UserDao {

    // SQL Queries - constants to avoid string duplication and magic strings
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

    // Error messages
    private static final String ERR_NULL_CREDENTIALS = "Credentials cannot be null";
    private static final String ERR_NULL_USER = "User cannot be null";
    private static final String ERR_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String ERR_USERNAME_EXISTS = "Username already exists";

    /**
     * {@inheritDoc}
     * <p>
     * This implementation:
     * <ol>
     *   <li>Validates that credentials are not null</li>
     *   <li>Queries the database for the user</li>
     *   <li>Verifies the password using BCrypt</li>
     *   <li>Sets the user type in the credentials if validation succeeds</li>
     * </ol>
     * </p>
     */
    @Override
    public void validateUser(CredentialsBean credentials) throws DAOException {
        validateCredentialsInput(credentials);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_VALIDATE_USER)) {

                stmt.setString(1, credentials.getUsername());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String userType = rs.getString("user_type");

                        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
                            credentials.setType(userType);
                            logger.log(Level.INFO, "User validated successfully: {0}", credentials.getUsername());
                        } else {
                            logger.log(Level.WARNING, "Invalid password for user: {0}", credentials.getUsername());
                            throw new DAOException(ERR_INVALID_CREDENTIALS);
                        }
                    } else {
                        logger.log(Level.WARNING, "User not found: {0}", credentials.getUsername());
                        throw new DAOException(ERR_INVALID_CREDENTIALS);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during user validation", e);
            throw new DAOException("Error validating user credentials", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The password is hashed using BCrypt before being stored in the database.
     * After successful insertion, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public void saveUser(UserBean userBean) throws DAOException {
        validateUserBeanInput(userBean);

        if (isUsernameTaken(userBean.getUsername())) {
            logger.log(Level.WARNING, "Attempt to save user with existing username: {0}", userBean.getUsername());
            throw new DAOException(ERR_USERNAME_EXISTS);
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_USER)) {

                String hashedPassword = PasswordUtils.hashPassword(userBean.getPassword());

                stmt.setString(1, userBean.getUsername());
                stmt.setString(2, hashedPassword);
                stmt.setString(3, userBean.getFullName());
                stmt.setString(4, userBean.getGender());
                stmt.setString(5, userBean.getType());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "User saved successfully: {0}", userBean.getUsername());
                    notifyObservers(DaoOperation.INSERT, "User", userBean.getUsername(), userBean);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during user save", e);
            throw new DAOException("Error saving user", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns user data as a String array in the following order:
     * <ol start="0">
     *   <li>username</li>
     *   <li>password_hash</li>
     *   <li>full_name</li>
     *   <li>gender</li>
     *   <li>user_type</li>
     * </ol>
     * </p>
     */
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
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during user retrieval", e);
            throw new DAOException("Error retrieving user", e);
        }
    }

    /**
     * {@inheritDoc}
     */
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
            logger.log(Level.SEVERE, "Database error during username check", e);
            throw new DAOException("Error checking username availability", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful update, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public void updateUser(User user, UserBean userBean) throws DAOException {
        validateUserInput(user);
        validateUserBeanInput(userBean);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_USER)) {

                stmt.setString(1, userBean.getFullName());
                stmt.setString(2, userBean.getGender());
                stmt.setString(3, user.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "User updated successfully: {0}", user.getUsername());
                    notifyObservers(DaoOperation.UPDATE, "User", user.getUsername(), user);
                } else {
                    logger.log(Level.WARNING, "User not found for update: {0}", user.getUsername());
                    throw new DAOException("User not found for update: " + user.getUsername());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during user update", e);
            throw new DAOException("Error updating user", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful deletion, observers are notified for cross-persistence sync.
     * Note: This should typically be called after deleting type-specific data (Fan/VenueManager).
     * </p>
     */
    @Override
    public void deleteUser(User user) throws DAOException {
        validateUserInput(user);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_USER)) {

                stmt.setString(1, user.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "User deleted successfully: {0}", user.getUsername());
                    notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
                } else {
                    logger.log(Level.WARNING, "User not found for deletion: {0}", user.getUsername());
                    throw new DAOException("User not found for deletion: " + user.getUsername());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during user deletion", e);
            throw new DAOException("Error deleting user", e);
        }
    }

    // ========== PRIVATE VALIDATION METHODS ==========

    /**
     * Validates credentials input to prevent null pointer exceptions.
     */
    private void validateCredentialsInput(CredentialsBean credentials) {
        if (credentials == null || credentials.getUsername() == null ||
                credentials.getUsername().trim().isEmpty() ||
                credentials.getPassword() == null || credentials.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_CREDENTIALS);
        }
    }

    /**
     * Validates User input to prevent null pointer exceptions.
     */
    private void validateUserInput(User user) {
        if (user == null) {
            throw new IllegalArgumentException(ERR_NULL_USER);
        }
    }
}
