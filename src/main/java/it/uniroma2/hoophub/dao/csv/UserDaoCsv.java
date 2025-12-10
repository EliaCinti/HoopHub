package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;
import it.uniroma2.hoophub.sync.SyncContext;
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
     *
     * @return
     */
    @Override
    public synchronized UserType validateUser(Credentials credentials) throws DAOException {
        // 1. Validazioni (usiamo i getter del Model)
        validateNotNull(credentials, "Credentials");
        // Nota: validateNotNullOrEmpty accetta stringhe, quindi va bene passare i getter
        validateNotNullOrEmpty(credentials.getUsername(), CsvDaoConstants.USERNAME);
        validateNotNullOrEmpty(credentials.getPassword(), "Password");

        // 2. Ricerca nel CSV
        String[] userRow = findRowByValue(COL_USERNAME, credentials.getUsername());

        // Check if user was not found
        if (userRow == null || userRow.length == 0) {
            logger.log(Level.FINE, "User not found: {0}", credentials.getUsername());
            throw new DAOException("Invalid username or password");
        }

        // Integrity Check
        if (userRow.length < 5) {
            logger.log(Level.SEVERE, "Corrupted user data for: {0}", credentials.getUsername());
            throw new DAOException("Data integrity error.");
        }

        String storedHash = userRow[COL_PASSWORD_HASH];

        // 3. Verifica Password e Ritorno del Tipo
        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
            logger.log(Level.FINE, "User validated successfully: {0}", credentials.getUsername());

            // REFACTORING QUI:
            // Invece di credentials.setType(...), leggiamo la stringa dal CSV
            // e la convertiamo in Enum per restituirla.
            String typeString = userRow[COL_USER_TYPE];
            return UserType.valueOf(typeString);
        }

        logger.log(Level.FINE, "Invalid password for user: {0}", credentials.getUsername());
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
    public synchronized void saveUser(User user) throws DAOException {
        // 1. Validazione sul Model
        validateNotNull(user, "User");
        validateNotNullOrEmpty(user.getUsername(), CsvDaoConstants.USERNAME);
        // Nota: controlliamo che l'hash esista (la password in chiaro non c'è nel model)
        validateNotNullOrEmpty(user.getPasswordHash(), "Password Hash");
        validateNotNullOrEmpty(user.getFullName(), "Full name");
        validateNotNullOrEmpty(user.getGender(), "Gender");
        validateNotNull(user.getUserType(), "User type");

        // 2. Controllo Duplicati
        if (!SyncContext.isSyncing() && isUsernameTaken(user.getUsername())) {
            throw new DAOException("Username already exists: " + user.getUsername());
        }

        // 3. Preparazione Riga CSV (leggendo dal Model)
        // Usiamo direttamente la passwordHash presente nell'oggetto User
        String[] newRow = {
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getGender(),
                user.getUserType().toString() // Salviamo il nome dell'enum (FAN, VENUE_MANAGER)
        };

        // 4. Scrittura su File
        CsvUtilities.writeFile(csvFile, newRow);

        // 5. Notifica Observer (passando il Model)
        notifyObservers(DaoOperation.INSERT, "User", user.getUsername(), user);
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
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        return findRowByValue(COL_USERNAME, username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isUsernameTaken(String username) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        String[] userRow = findRowByValue(COL_USERNAME, username);
        return userRow != null && userRow.length > 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful update, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public synchronized void updateUser(User user) throws DAOException {
        // 1. Validazione sul Model (via la Bean)
        validateNotNull(user, "User");
        validateNotNullOrEmpty(user.getFullName(), "Full name");
        validateNotNullOrEmpty(user.getGender(), "Gender");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, update matching row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(user.getUsername())) {
                // 2. Lettura dati dal MODEL (Refactoring)
                // Aggiorniamo solo i campi modificabili (non password o tipo)
                row[COL_FULL_NAME] = user.getFullName();
                row[COL_GENDER] = user.getGender();
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

        // 3. Notifica Observer col Model
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
