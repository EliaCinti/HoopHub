package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of the UserDao interface.
 * <p>
 * This class provides data access operations for User entities stored in CSV files.
 * It extends {@link AbstractCsvDao} to leverage common functionality like file initialization,
 * ID generation, and validation, eliminating code duplication across DAO implementations.
 * </p>
 * <p>
 * <strong>CSV File Structure (users.csv):</strong>
 * <pre>
 * username,password_hash,full_name,gender,user_type
 * john_doe,$2a$12$...,John Doe,Male,FAN
 * jane_smith,$2a$12$...,Jane Smith,Female,VENUE_MANAGER
 * </pre>
 * </p>
 * <p>
 * <strong>Security:</strong> Passwords are hashed using BCrypt (via {@link PasswordUtils})
 * before being written to the CSV file. The DAO never stores or logs plain-text passwords.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> All public methods are synchronized to prevent concurrent
 * modification issues when multiple threads access the CSV file.
 * </p>
 * <p>
 * <strong>Observer Pattern:</strong> This DAO extends {@link AbstractCsvDao} (which extends
 * {@link it.uniroma2.hoophub.dao.AbstractObservableDao}), so it notifies observers after
 * successful INSERT, UPDATE, and DELETE operations for cross-persistence synchronization.
 * </p>
 *
 * @see UserDao Interface defining the contract
 * @see AbstractCsvDao Base class providing common CSV functionality
 * @see PasswordUtils Utility for secure password hashing and verification
 * @see CsvUtilities Low-level CSV file operations
 */
public class UserDaoCsv extends AbstractCsvDao implements UserDao {

    // ========== CSV CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "users.csv";
    private static final String[] CSV_HEADER = {"username", "password_hash", "full_name", "gender", "user_type"};

    // ========== COLUMN INDICES ==========

    private static final int COL_USERNAME = 0;
    private static final int COL_PASSWORD_HASH = 1;
    private static final int COL_FULL_NAME = 2;
    private static final int COL_GENDER = 3;
    private static final int COL_USER_TYPE = 4;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructs a new UserDaoCsv and initializes the CSV file.
     * <p>
     * The parent constructor ({@link AbstractCsvDao}) handles:
     * <ul>
     *   <li>Creating the File object</li>
     *   <li>Creating parent directories if needed</li>
     *   <li>Initializing the CSV file with headers if it doesn't exist</li>
     *   <li>Setting up the logger</li>
     * </ul>
     * </p>
     */
    public UserDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC METHODS (UserDao Interface Implementation) ==========

    /**
     * {@inheritDoc}
     * <p>
     * This method performs authentication by:
     * <ol>
     *   <li>Looking up the user by username in the CSV file</li>
     *   <li>Verifying the password using BCrypt (constant-time comparison)</li>
     *   <li>If valid, setting the user type in the CredentialsBean</li>
     *   <li>If invalid, throwing a DAOException</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Security Note:</strong> This method uses {@link PasswordUtils#checkPassword}
     * which performs constant-time comparison to prevent timing attacks.
     * </p>
     */
    @Override
    public synchronized void validateUser(CredentialsBean credentials) throws DAOException {
        validateNotNull(credentials, "Credentials");
        validateNotNullOrEmpty(credentials.getUsername(), "Username");
        validateNotNullOrEmpty(credentials.getPassword(), "Password");

        String[] userRow = findRowByValue(COL_USERNAME, credentials.getUsername());

        if (userRow == null) {
            logger.log(Level.WARNING, "User not found: {0}", credentials.getUsername());
            throw new DAOException("Invalid username or password");
        }

        String storedHash = userRow[COL_PASSWORD_HASH];

        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
            credentials.setType(userRow[COL_USER_TYPE]);
            logger.log(Level.INFO, "User validated successfully: {0}", credentials.getUsername());
            return;
        }

        logger.log(Level.WARNING, "Invalid password for user: {0}", credentials.getUsername());
        throw new DAOException("Invalid username or password");
    }

    /**
     * {@inheritDoc}
     * <p>
     * The password is hashed using BCrypt before being written to the CSV file.
     * After successful insertion, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public synchronized void saveUser(UserBean userBean) throws DAOException {
        validateNotNull(userBean, "UserBean");
        validateNotNullOrEmpty(userBean.getUsername(), "Username");
        validateNotNullOrEmpty(userBean.getPassword(), "Password");
        validateNotNullOrEmpty(userBean.getFullName(), "Full name");
        validateNotNullOrEmpty(userBean.getGender(), "Gender");
        validateNotNullOrEmpty(userBean.getType(), "User type");

        logger.log(Level.INFO, ">>> SAVE USER: checking if username exists: {0}", userBean.getUsername());
        boolean taken = isUsernameTaken(userBean.getUsername());
        logger.log(Level.INFO, ">>> SAVE USER: isUsernameTaken() returned: {0}", taken);

        if (taken) {
            logger.log(Level.SEVERE, ">>> SAVE USER: THROWING EXCEPTION - Username already exists: {0}", userBean.getUsername());
            throw new DAOException("Username already exists: " + userBean.getUsername());
        }

        logger.log(Level.INFO, ">>> SAVE USER: proceeding with save for: {0}", userBean.getUsername());

        String hashedPassword = PasswordUtils.hashPassword(userBean.getPassword());

        String[] newRow = {
                userBean.getUsername(),
                hashedPassword,
                userBean.getFullName(),
                userBean.getGender(),
                userBean.getType()
        };

        logger.log(Level.INFO, ">>> SAVE USER: about to write to users.csv");
        CsvUtilities.writeFile(csvFile, newRow);
        logger.log(Level.INFO, ">>> SAVE USER: writeFile completed");

        logger.log(Level.INFO, "User saved successfully: {0}", userBean.getUsername());
        notifyObservers(DaoOperation.INSERT, "User", userBean.getUsername(), userBean);
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
    public synchronized String[] retrieveUser(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");
        return findRowByValue(COL_USERNAME, username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isUsernameTaken(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");
        return findRowByValue(COL_USERNAME, username) != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful update, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public synchronized void updateUser(User user, UserBean userBean) throws DAOException {
        validateNotNull(user, "User");
        validateNotNull(userBean, "UserBean");
        validateNotNullOrEmpty(userBean.getFullName(), "Full name");
        validateNotNullOrEmpty(userBean.getGender(), "Gender");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, update matching row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(user.getUsername())) {
                // Update only full_name and gender (not password or type)
                row[COL_FULL_NAME] = userBean.getFullName();
                row[COL_GENDER] = userBean.getGender();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "User", "update", user.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "User updated successfully: {0}", user.getUsername());
        notifyObservers(DaoOperation.UPDATE, "User", user.getUsername(), user);
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful deletion, observers are notified for cross-persistence sync.
     * Note: This should typically be called after deleting type-specific data (Fan/VenueManager).
     * </p>
     */
    @Override
    public synchronized void deleteUser(User user) throws DAOException {
        validateNotNull(user, "User");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, find and remove matching row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(user.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "User", "deletion", user.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "User deleted successfully: {0}", user.getUsername());
        notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
    }
}
