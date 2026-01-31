package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link VenueDao}.
 *
 * <p>Stores Venue data and team associations in RAM via {@link InMemoryDataStore}.
 * Mirrors the pattern used by MySQL implementation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueDaoInMemory extends AbstractObservableDao implements VenueDao {

    private static final Logger LOGGER = Logger.getLogger(VenueDaoInMemory.class.getName());

    private static final String VENUE = "Venue";
    private static final String ERR_NULL_VENUE = "Venue cannot be null";
    private static final String ERR_INVALID_ID = "Venue ID must be positive";
    private static final String ERR_VENUE_NOT_FOUND = "Venue not found";

    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public VenueDaoInMemory() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public Venue saveVenue(Venue venue) {
        validateVenueInput(venue);

        // Generate ID if not set
        int venueId = venue.getId();
        if (venueId <= 0) {
            venueId = dataStore.getNextVenueId();
        }

        // Save venue
        Venue venueToStore = rebuildVenueWithId(venue, venueId);
        dataStore.saveVenue(venueToStore);

        // Save team associations
        for (TeamNBA team : venue.getAssociatedTeams()) {
            dataStore.saveVenueTeam(venueId, team);
        }

        cache.put(generateCacheKey(venueId), venueToStore);

        LOGGER.log(Level.INFO, "Venue saved with ID: {0}", venueId);
        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(venueId), venueToStore);

        return venueToStore;
    }

    @Override
    public Venue retrieveVenue(int venueId) {
        validateIdInput(venueId);

        // Check cache first
        Venue cached = (Venue) cache.get(generateCacheKey(venueId));
        if (cached != null) {
            return cached;
        }

        Venue venue = dataStore.getVenue(venueId);
        if (venue != null) {
            // Load associated teams
            Set<TeamNBA> teams = dataStore.getVenueTeams(venueId);
            venue = rebuildVenueWithTeams(venue, teams);
            cache.put(generateCacheKey(venueId), venue);
        }

        return venue;
    }

    @Override
    public List<Venue> retrieveAllVenues() {
        List<Venue> result = new ArrayList<>();

        for (Integer venueId : dataStore.getAllVenues().keySet()) {
            Venue venue = retrieveVenue(venueId);
            if (venue != null) {
                result.add(venue);
            }
        }

        return result;
    }

    @Override
    public List<Venue> retrieveVenuesByManager(String venueManagerUsername) {
        validateUsernameInput(venueManagerUsername);

        List<Venue> result = new ArrayList<>();

        for (Venue venue : retrieveAllVenues()) {
            if (venue.getVenueManager().getUsername().equals(venueManagerUsername)) {
                result.add(venue);
            }
        }

        return result;
    }

    @Override
    public List<Venue> retrieveVenuesByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }

        List<Venue> result = new ArrayList<>();

        for (Venue venue : retrieveAllVenues()) {
            if (venue.getCity().equalsIgnoreCase(city)) {
                result.add(venue);
            }
        }

        return result;
    }

    @Override
    public void updateVenue(Venue venue) throws DAOException {
        validateVenueInput(venue);
        int venueId = venue.getId();

        if (!dataStore.venueExists(venueId)) {
            throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueId);
        }

        dataStore.saveVenue(venue);

        // Update team associations
        dataStore.deleteAllVenueTeams(venueId);
        for (TeamNBA team : venue.getAssociatedTeams()) {
            dataStore.saveVenueTeam(venueId, team);
        }

        cache.put(generateCacheKey(venueId), venue);

        LOGGER.log(Level.INFO, "Venue updated: {0}", venueId);
        notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venueId), venue);
    }

    @Override
    public void deleteVenue(Venue venue) throws DAOException {
        validateVenueInput(venue);
        int venueId = venue.getId();

        if (!dataStore.venueExists(venueId)) {
            throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueId);
        }

        dataStore.deleteVenue(venueId);
        cache.remove(generateCacheKey(venueId));

        LOGGER.log(Level.INFO, "Venue deleted: {0}", venueId);
        notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(venueId), null);
    }

    @Override
    public boolean venueExists(int venueId) {
        return dataStore.venueExists(venueId);
    }

    @Override
    public int getNextVenueId() {
        return dataStore.getNextVenueId();
    }

    @Override
    public void saveVenueTeam(Venue venue, TeamNBA team) {
        validateVenueInput(venue);
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        dataStore.saveVenueTeam(venue.getId(), team);
    }

    @Override
    public void deleteVenueTeam(Venue venue, TeamNBA team) {
        validateVenueInput(venue);
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        dataStore.deleteVenueTeam(venue.getId(), team);
    }

    @Override
    public Set<TeamNBA> retrieveVenueTeams(int venueId) {
        validateIdInput(venueId);
        return new HashSet<>(dataStore.getVenueTeams(venueId));
    }

    @Override
    public void deleteAllVenueTeams(Venue venue) {
        validateVenueInput(venue);
        dataStore.deleteAllVenueTeams(venue.getId());
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(int venueId) {
        return "Venue:" + venueId;
    }

    private void validateVenueInput(Venue venue) {
        if (venue == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE);
        }
    }

    private void validateIdInput(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }

    /**
     * Rebuilds venue with new ID (venues may be immutable).
     */
    private Venue rebuildVenueWithId(Venue original, int newId) {
        return new Venue.Builder()
                .id(newId)
                .name(original.getName())
                .type(original.getType())
                .address(original.getAddress())
                .city(original.getCity())
                .maxCapacity(original.getMaxCapacity())
                .venueManager(original.getVenueManager())
                .teams(original.getAssociatedTeams())
                .build();
    }

    /**
     * Rebuilds venue with loaded teams.
     */
    private Venue rebuildVenueWithTeams(Venue original, Set<TeamNBA> teams) {
        return new Venue.Builder()
                .id(original.getId())
                .name(original.getName())
                .type(original.getType())
                .address(original.getAddress())
                .city(original.getCity())
                .maxCapacity(original.getMaxCapacity())
                .venueManager(original.getVenueManager())
                .teams(teams)
                .build();
    }
}