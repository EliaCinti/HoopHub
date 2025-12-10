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
     *
     * @return
     */
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

                        // Verifica Password (Raw vs Hash)
                        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
                            logger.log(Level.INFO, "User validated: {0}", credentials.getUsername());
                            return UserType.valueOf(typeString); // RITORNA IL TIPO
                        }
                    }
                    // Se siamo qui, o utente non trovato o password errata
                    throw new DAOException(ERR_INVALID_CREDENTIALS);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error validating user", e);
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
    public void saveUser(User user) throws DAOException {
        validateUserInput(user); // Aggiorna la validazione per accettare User

        if (isUsernameTaken(user.getUsername())) {
            throw new DAOException(ERR_USERNAME_EXISTS);
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_USER)) {

                // NON facciamo più l'hash qui. Lo prendiamo dal Model.
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPasswordHash()); // Getter dal model
                stmt.setString(3, user.getFullName());
                stmt.setString(4, user.getGender());
                stmt.setString(5, user.getUserType().toString()); // O come gestisci l'enum

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // Cache Put: Ora è facilissimo, abbiamo già l'oggetto User!
                    putInCache(user, user.getUsername());

                    logger.log(Level.INFO, "User saved: {0}", user.getUsername());
                    notifyObservers(DaoOperation.INSERT, "User", user.getUsername(), user);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error saving user", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Nota sulla Cache:</strong> Questo metodo NON usa la cache.
     * Motivo: L'interfaccia richiede un array di String contenente anche l'hash della password.
     * L'oggetto User in cache (Domain Model) NON contiene la password per sicurezza.
     * Pertanto, dobbiamo sempre interrogare il DB per ottenere i dati grezzi completi.
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
                    return new String[0];
                }
            }
        } catch (SQLException e) {
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
    public void updateUser(User user) throws DAOException {
        // Validation: controlliamo solo che l'utente esista/sia valido
        validateUserInput(user);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_USER)) {
                // Si presuppone che il controller abbia già fatto user.setFullName(...)
                stmt.setString(1, user.getFullName());
                stmt.setString(2, user.getGender());
                stmt.setString(3, user.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // === CACHE WRITE-THROUGH (La tua richiesta) ===
                    // Poiché 'user' è l'oggetto aggiornato (passato dal controller),
                    // possiamo salvarlo direttamente in cache.
                    // Nota: Non serve più rimuoverlo (Eviction), perché ora siamo sicuri che sia corretto.
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
                    // === CACHE REMOVE ===
                    removeFromCache(user.getClass(), user.getUsername());

                    logger.log(Level.INFO, "User deleted: {0}", user.getUsername());
                    notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error deleting user", e);
        }
    }

    // ========== PRIVATE VALIDATION METHODS ==========

    /**
     * Validates credentials input to prevent null pointer exceptions.
     */
    private void validateCredentialsInput(Credentials credentials) {
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
