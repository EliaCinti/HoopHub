package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;

import java.util.List;

/**
 * DAO interface for VenueManager entity persistence.
 *
 * <p>Handles VenueManager-specific CRUD operations. Works with {@link UserDao}
 * for common user data. Venues are loaded lazily via {@link #getVenues(VenueManager)}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see VenueManager
 */
public interface VenueManagerDao {

    /**
     * Saves a new venue manager. Password is hashed before storage.
     *
     * @param venueManager the venue manager to save
     * @throws DAOException if username exists or save fails
     */
    void saveVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves a venue manager by username.
     *
     * @param username the manager's username
     * @return the venue manager, or null if not found
     * @throws DAOException if retrieval fails
     */
    VenueManager retrieveVenueManager(String username) throws DAOException;

    /**
     * Retrieves all venue managers.
     *
     * @return list of all venue managers
     * @throws DAOException if retrieval fails
     */
    List<VenueManager> retrieveAllVenueManagers() throws DAOException;

    /**
     * Updates a venue manager's details.
     *
     * @param venueManager the manager with updated data
     * @throws DAOException if update fails
     */
    void updateVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Deletes a venue manager. May fail if it has active venues.
     *
     * @param venueManager the manager to delete
     * @throws DAOException if deletion fails or has FK constraints
     */
    void deleteVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves all venues managed by a venue manager.
     *
     * @param venueManager the venue manager
     * @return list of managed venues
     * @throws DAOException if retrieval fails
     */
    List<Venue> getVenues(VenueManager venueManager) throws DAOException;
}