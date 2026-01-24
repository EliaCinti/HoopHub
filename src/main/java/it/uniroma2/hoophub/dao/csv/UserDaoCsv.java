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
 * CSV implementation of {@link UserDao}.
 *
 * <p>Persists common user data to {@code users.csv}. Handles authentication
 * with BCrypt password verification. Used by {@link FanDaoCsv} and
 * {@link VenueManagerDaoCsv} for common user data.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class UserDaoCsv extends AbstractCsvDao implements UserDao {

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "users.csv";
    private static final String[] CSV_HEADER = {"username", "password_hash", "full_name", "gender", "user_type"};

    public static final int COL_USERNAME = 0;
    public static final int COL_PASSWORD_HASH = 1;
    public static final int COL_FULL_NAME = 2;
    public static final int COL_GENDER = 3;
    public static final int COL_USER_TYPE = 4;

    public UserDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized UserType validateUser(Credentials credentials) throws DAOException {
        validateNotNull(credentials, "Credentials");
        validateNotNullOrEmpty(credentials.getUsername(), CsvDaoConstants.USERNAME);
        validateNotNullOrEmpty(credentials.getPassword(), "Password");

        String[] userRow = findRowByValue(COL_USERNAME, credentials.getUsername());

        if (userRow == null || userRow.length == 0) {
            logger.log(Level.FINE, "User not found: {0}", credentials.getUsername());
            throw new DAOException("Invalid username or password");
        }

        if (userRow.length < 5) {
            logger.log(Level.SEVERE, "Corrupted user data for: {0}", credentials.getUsername());
            throw new DAOException("Data integrity error.");
        }

        String storedHash = userRow[COL_PASSWORD_HASH];

        if (PasswordUtils.checkPassword(credentials.getPassword(), storedHash)) {
            logger.log(Level.FINE, "User validated successfully: {0}", credentials.getUsername());
            return UserType.valueOf(userRow[COL_USER_TYPE]);
        }

        logger.log(Level.FINE, "Invalid password for user: {0}", credentials.getUsername());
        throw new DAOException("Invalid username or password");
    }

    @Override
    public synchronized void saveUser(User user) throws DAOException {
        validateNotNull(user, "User");
        validateNotNullOrEmpty(user.getUsername(), CsvDaoConstants.USERNAME);
        validateNotNullOrEmpty(user.getPasswordHash(), "Password Hash");
        validateNotNullOrEmpty(user.getFullName(), "Full name");
        validateNotNullOrEmpty(user.getGender(), "Gender");
        validateNotNull(user.getUserType(), "User type");

        if (!SyncContext.isSyncing() && isUsernameTaken(user.getUsername())) {
            throw new DAOException("Username already exists: " + user.getUsername());
        }

        String[] newRow = {
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getGender(),
                user.getUserType().toString()
        };

        CsvUtilities.writeFile(csvFile, newRow);
        putInCache(user, user.getUsername());
        notifyObservers(DaoOperation.INSERT, "User", user.getUsername(), user);
    }

    @Override
    public synchronized String[] retrieveUser(String username) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        return findRowByValue(COL_USERNAME, username);
    }

    @Override
    public synchronized boolean isUsernameTaken(String username) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        String[] userRow = findRowByValue(COL_USERNAME, username);
        return userRow != null && userRow.length > 0;
    }

    @Override
    public synchronized void updateUser(User user) throws DAOException {
        validateNotNull(user, "User");
        validateNotNullOrEmpty(user.getFullName(), "Full name");
        validateNotNullOrEmpty(user.getGender(), "Gender");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(user.getUsername())) {
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
        putInCache(user, user.getUsername());
        notifyObservers(DaoOperation.UPDATE, "User", user.getUsername(), user);
    }

    @Override
    public synchronized void deleteUser(User user) throws DAOException {
        validateNotNull(user, "User");

        boolean found = deleteByColumn(COL_USERNAME, user.getUsername());

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "User", "deletion", user.getUsername()));
        }

        logger.log(Level.INFO, "User deleted successfully: {0}", user.getUsername());
        removeFromCache(user.getClass(), user.getUsername());
        notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
    }
}