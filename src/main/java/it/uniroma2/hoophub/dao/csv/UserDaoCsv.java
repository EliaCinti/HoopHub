package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV implementation of the UserDao interface.
 * <p>
 * This class provides data access operations for User entities stored in CSV files.
 * It uses {@link CsvUtilities} for file I/O operations and extends {@link AbstractObservableDao}
 * to support the Observer pattern for cross-persistence synchronization.
 * </p>
 * <p>
 * CSV file structure (users.csv):
 * <pre>
 * username,password_hash,full_name,gender,user_type
 * john_doe,$2a$12$...,John Doe,Male,FAN
 * </pre>
 * </p>
 * <p>
 * Note: This implementation uses synchronized methods to prevent concurrent modification
 * issues when multiple threads access the CSV file.
 * </p>
 *
 * @see UserDao
 * @see AbstractObservableDao
 * @see CsvUtilities
 */
public class UserDaoCsv extends AbstractObservableDao implements UserDao {

    private static final Logger logger = Logger.getLogger(UserDaoCsv.class.getName());

    // CSV File configuration
    private static final String CSV_FILE_PATH = "data/users.csv";
    private static final String[] CSV_HEADER = {"username", "password_hash", "full_name", "gender", "user_type"};

    // Column indices for clarity and maintainability
    private static final int COL_USERNAME = 0;
    private static final int COL_PASSWORD_HASH = 1;
    private static final int COL_FULL_NAME = 2;
    private static final int COL_GENDER = 3;
    private static final int COL_USER_TYPE = 4;

    // Error messages
    private static final String ERR_NULL_CREDENTIALS = "Credentials cannot be null";
    private static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    private static final String ERR_NULL_USERBEAN = "UserBean cannot be null";
    private static final String ERR_NULL_USER = "User cannot be null";
    private static final String ERR_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String ERR_USERNAME_EXISTS = "Username already exists";

    private final File csvFile;

    /**
     * Constructs a new UserDaoCsv and initializes the CSV file.
     * Creates the file and directory structure if they don't exist.
     */
    public UserDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        initializeCsvFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void validateUser(CredentialsBean credentials) throws DAOException {
        validateCredentialsInput(credentials);

        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header
        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(credentials.getUsername())) {
                String storedHash = row[COL_PASSWORD_HASH];

                if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
                    credentials.setType(row[COL_USER_TYPE]);
                    logger.log(Level.INFO, "User validated successfully: {0}", credentials.getUsername());
                    return;
                }

                logger.log(Level.WARNING, "Invalid password for user: {0}", credentials.getUsername());
                throw new DAOException(ERR_INVALID_CREDENTIALS);
            }
        }

        logger.log(Level.WARNING, "User not found: {0}", credentials.getUsername());
        throw new DAOException(ERR_INVALID_CREDENTIALS);
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
        validateUserBeanInput(userBean);

        if (isUsernameTaken(userBean.getUsername())) {
            throw new DAOException(ERR_USERNAME_EXISTS);
        }

        String hashedPassword = PasswordUtils.hashPassword(userBean.getPassword());

        String[] newRow = {
                userBean.getUsername(),
                hashedPassword,
                userBean.getFullName(),
                userBean.getGender(),
                userBean.getType()
        };

        CsvUtilities.writeFile(csvFile, newRow);

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
        validateUsernameInput(username);

        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header
        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username)) {
                return row;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isUsernameTaken(String username) throws DAOException {
        validateUsernameInput(username);

        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(username)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful update, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public synchronized void updateUser(User user, UserBean userBean) throws DAOException {
        validateUserInput(user);
        validateUserBeanInput(userBean);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, update matching row
        for (int i = 1; i < data.size(); i++) {
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
            throw new DAOException("User not found for update: " + user.getUsername());
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
        validateUserInput(user);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, find and remove matching row
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(user.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("User not found for deletion: " + user.getUsername());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "User deleted successfully: {0}", user.getUsername());
        notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Initializes the CSV file if it doesn't exist.
     * Creates the directory structure and an empty file with headers.
     */
    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                // Create parent directories if they don't exist
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        logger.warning("Failed to create directories for CSV file");
                    }
                }

                // Create file with header
                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, CSV_HEADER, emptyData);
                logger.info("Initialized CSV file: " + CSV_FILE_PATH);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Failed to initialize CSV file", e);
        }
    }

    // ========== VALIDATION METHODS ==========

    private void validateCredentialsInput(CredentialsBean credentials) {
        if (credentials == null || credentials.getUsername() == null ||
                credentials.getUsername().trim().isEmpty() ||
                credentials.getPassword() == null || credentials.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_CREDENTIALS);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }

    private void validateUserBeanInput(UserBean userBean) {
        if (userBean == null) {
            throw new IllegalArgumentException(ERR_NULL_USERBEAN);
        }
    }

    private void validateUserInput(User user) {
        if (user == null) {
            throw new IllegalArgumentException(ERR_NULL_USER);
        }
    }
}
