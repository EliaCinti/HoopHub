package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link FanDao}.
 *
 * <p>Delegates common user data to {@link UserDao} and manages Fan-specific
 * data in the in-memory store. Mirrors the pattern used by MySQL implementation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanDaoInMemory extends AbstractObservableDao implements FanDao {

    private static final Logger LOGGER = Logger.getLogger(FanDaoInMemory.class.getName());

    private static final String ERR_NULL_FAN = "Fan cannot be null";
    private static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    private static final String ERR_FAN_NOT_FOUND = "Fan not found";

    private final UserDao userDao;
    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public FanDaoInMemory(UserDao userDao) {
        this.userDao = userDao;
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public void saveFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        // Delegate to UserDao for base user data (like MySQL does)
        userDao.saveUser(fan);

        // Save Fan-specific data
        dataStore.saveFan(fan);
        cache.put(generateCacheKey(fan.getUsername()), fan);

        LOGGER.log(Level.INFO, "Fan saved successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.INSERT, "Fan", fan.getUsername(), fan);
    }

    @Override
    public Fan retrieveFan(String username) {
        validateUsernameInput(username);

        // Check cache first
        Fan cachedFan = (Fan) cache.get(generateCacheKey(username));
        if (cachedFan != null) {
            return cachedFan;
        }

        Fan fan = dataStore.getFan(username);
        if (fan != null) {
            cache.put(generateCacheKey(username), fan);
        }

        return fan;
    }

    @Override
    public List<Fan> retrieveAllFans() {
        List<Fan> result = new ArrayList<>();

        for (Fan fan : dataStore.getAllFans().values()) {
            String username = fan.getUsername();
            Fan cachedFan = (Fan) cache.get(generateCacheKey(username));

            if (cachedFan != null) {
                result.add(cachedFan);
            } else {
                cache.put(generateCacheKey(username), fan);
                result.add(fan);
            }
        }

        return result;
    }

    @Override
    public void updateFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        if (dataStore.getFan(fan.getUsername()) == null) {
            throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
        }

        // Delegate to UserDao for base user data
        userDao.updateUser(fan);

        // Update Fan-specific data
        dataStore.saveFan(fan);
        cache.put(generateCacheKey(fan.getUsername()), fan);

        LOGGER.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
    }

    @Override
    public void deleteFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        if (dataStore.getFan(fan.getUsername()) == null) {
            throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
        }

        // Delete Fan-specific data first
        dataStore.deleteFan(fan.getUsername());

        // Delegate to UserDao for base user data
        userDao.deleteUser(fan);

        cache.remove(generateCacheKey(fan.getUsername()));

        LOGGER.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(String username) {
        return "Fan:" + username;
    }

    private void validateFanInput(Fan fan) {
        if (fan == null) {
            throw new IllegalArgumentException(ERR_NULL_FAN);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }
}