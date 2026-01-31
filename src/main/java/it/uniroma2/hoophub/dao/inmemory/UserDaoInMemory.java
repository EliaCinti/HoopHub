package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.PasswordUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link UserDao}.
 *
 * <p>Stores user data in RAM via {@link InMemoryDataStore}.
 * Used by {@link FanDaoInMemory} and {@link VenueManagerDaoInMemory}
 * for base user operations (delegation pattern like MySQL implementation).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class UserDaoInMemory extends AbstractObservableDao implements UserDao {

    private static final Logger LOGGER = Logger.getLogger(UserDaoInMemory.class.getName());

    private static final String ERR_NULL_CREDENTIALS = "Credentials cannot be null";
    private static final String ERR_NULL_USER = "User cannot be null";
    private static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    private static final String ERR_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String ERR_USERNAME_EXISTS = "Username already exists";

    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public UserDaoInMemory() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public UserType validateUser(Credentials credentials) throws DAOException {
        validateCredentialsInput(credentials);

        User user = dataStore.getUser(credentials.getUsername());
        if (user == null) {
            throw new DAOException(ERR_INVALID_CREDENTIALS);
        }

        if (PasswordUtils.checkPassword(credentials.getPassword(), user.getPasswordHash())) {
            LOGGER.log(Level.INFO, "User validated: {0}", credentials.getUsername());
            return user.getUserType();
        }

        throw new DAOException(ERR_INVALID_CREDENTIALS);
    }

    @Override
    public void saveUser(User user) throws DAOException {
        validateUserInput(user);

        if (isUsernameTaken(user.getUsername())) {
            throw new DAOException(ERR_USERNAME_EXISTS);
        }

        dataStore.saveUser(user);
        cache.put(generateCacheKey(user.getUsername()), user);

        LOGGER.log(Level.INFO, "User saved: {0}", user.getUsername());
        notifyObservers(DaoOperation.INSERT, "User", user.getUsername(), user);
    }

    @Override
    public String[] retrieveUser(String username) {
        validateUsernameInput(username);

        User user = dataStore.getUser(username);
        if (user == null) {
            return new String[0];
        }

        return new String[]{
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getGender(),
                user.getUserType().toString()
        };
    }

    @Override
    public boolean isUsernameTaken(String username) {
        validateUsernameInput(username);
        return dataStore.userExists(username);
    }

    @Override
    public void updateUser(User user) throws DAOException {
        validateUserInput(user);

        if (!dataStore.userExists(user.getUsername())) {
            throw new DAOException("User not found for update: " + user.getUsername());
        }

        dataStore.saveUser(user);
        cache.put(generateCacheKey(user.getUsername()), user);

        LOGGER.log(Level.INFO, "User updated: {0}", user.getUsername());
        notifyObservers(DaoOperation.UPDATE, "User", user.getUsername(), user);
    }

    @Override
    public void deleteUser(User user) throws DAOException {
        validateUserInput(user);

        dataStore.deleteUser(user.getUsername());
        cache.remove(generateCacheKey(user.getUsername()));

        LOGGER.log(Level.INFO, "User deleted: {0}", user.getUsername());
        notifyObservers(DaoOperation.DELETE, "User", user.getUsername(), null);
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(String username) {
        return "User:" + username;
    }

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

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }
}