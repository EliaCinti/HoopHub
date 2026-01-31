package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link VenueManagerDao}.
 *
 * <p>Delegates common user data to {@link UserDao} and manages VenueManager-specific
 * data in the in-memory store. Mirrors the pattern used by MySQL implementation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerDaoInMemory extends AbstractObservableDao implements VenueManagerDao {

    private static final Logger LOGGER = Logger.getLogger(VenueManagerDaoInMemory.class.getName());

    private static final String VENUE_MANAGER = "VenueManager";
    private static final String ERR_NULL_VENUE_MANAGER = "VenueManager cannot be null";
    private static final String ERR_NULL_USERNAME = "Username cannot be null or empty";
    private static final String ERR_VENUE_MANAGER_NOT_FOUND = "VenueManager not found";

    private final UserDao userDao;
    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public VenueManagerDaoInMemory(UserDao userDao) {
        this.userDao = userDao;
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public void saveVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        // Delegate to UserDao for base user data (like MySQL does)
        userDao.saveUser(venueManager);

        // Save VenueManager-specific data
        dataStore.saveVenueManager(venueManager);
        cache.put(generateCacheKey(venueManager.getUsername()), venueManager);

        LOGGER.log(Level.INFO, "VenueManager saved: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.INSERT, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    @Override
    public VenueManager retrieveVenueManager(String username) throws DAOException {
        validateUsernameInput(username);

        // Check cache first
        VenueManager cached = (VenueManager) cache.get(generateCacheKey(username));
        if (cached != null) {
            return cached;
        }

        VenueManager vm = dataStore.getVenueManager(username);
        if (vm != null) {
            cache.put(generateCacheKey(username), vm);
        }

        return vm;
    }

    @Override
    public List<VenueManager> retrieveAllVenueManagers() {
        List<VenueManager> result = new ArrayList<>();

        for (VenueManager vm : dataStore.getAllVenueManagers().values()) {
            String username = vm.getUsername();
            VenueManager cached = (VenueManager) cache.get(generateCacheKey(username));

            if (cached != null) {
                result.add(cached);
            } else {
                cache.put(generateCacheKey(username), vm);
                result.add(vm);
            }
        }

        return result;
    }

    @Override
    public void updateVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        if (dataStore.getVenueManager(venueManager.getUsername()) == null) {
            throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND + ": " + venueManager.getUsername());
        }

        // Delegate to UserDao for base user data
        userDao.updateUser(venueManager);

        // Update VenueManager-specific data
        dataStore.saveVenueManager(venueManager);
        cache.put(generateCacheKey(venueManager.getUsername()), venueManager);

        LOGGER.log(Level.INFO, "VenueManager updated: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.UPDATE, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    @Override
    public void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        if (dataStore.getVenueManager(venueManager.getUsername()) == null) {
            throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND + ": " + venueManager.getUsername());
        }

        // Delete VenueManager-specific data first
        dataStore.deleteVenueManager(venueManager.getUsername());

        // Delegate to UserDao for base user data
        userDao.deleteUser(venueManager);

        cache.remove(generateCacheKey(venueManager.getUsername()));

        LOGGER.log(Level.INFO, "VenueManager deleted: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.DELETE, VENUE_MANAGER, venueManager.getUsername(), null);
    }

    @Override
    public List<Venue> getVenues(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);
        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();
        return venueDao.retrieveVenuesByManager(venueManager.getUsername());
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(String username) {
        return "VenueManager:" + username;
    }

    private void validateVenueManagerInput(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE_MANAGER);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }
}