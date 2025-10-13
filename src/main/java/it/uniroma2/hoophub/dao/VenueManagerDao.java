package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;

import java.util.List;

/**
 * Data Access Object interface for VenueManager entities.
 * <p>
 * This interface defines the contract for all operations related to venue manager data persistence.
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * </p>
 */
public interface VenueManagerDao {

    /**
     * Saves a new venue manager in the persistence layer.
     * <p>
     * If a venue manager with the same username already exists, a {@link DAOException} is thrown.
     * </p>
     *
     * @param venueManager The {@link VenueManagerBean} object containing the venue manager's details
     * @throws DAOException If an error occurs while saving or if the venue manager already exists
     */
    void saveVenueManager(VenueManagerBean venueManager) throws DAOException;

    /**
     * Retrieves venue manager details from the persistence layer based on the username.
     *
     * @param username The username of the venue manager to retrieve
     * @return A {@link VenueManager} object if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     */
    VenueManager retrieveVenueManager(String username) throws DAOException;

    /**
     * Retrieves all venue managers from the persistence layer.
     *
     * @return A list of {@link VenueManager} objects representing all venue managers in the system
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<VenueManager> retrieveAllVenueManagers() throws DAOException;

    /**
     * Updates an existing venue manager's details in the persistence layer.
     *
     * @param venueManager The {@link VenueManager} object containing the updated details
     * @param user The {@link UserBean} object containing the updated general user details
     * @throws DAOException If an error occurs while updating
     */
    void updateVenueManager(VenueManager venueManager, UserBean user) throws DAOException;

    /**
     * Deletes a venue manager from the persistence layer.
     *
     * @param username The username of the venue manager to delete
     * @throws DAOException If an error occurs while deleting
     */
    void deleteVenueManager(String username) throws DAOException;

    /**
     * Retrieves all venues managed by a specific venue manager.
     *
     * @param venueManager The {@link VenueManager} whose venues should be retrieved
     * @return A list of {@link Venue} objects managed by the venue manager
     * @throws DAOException If an error occurs while accessing the data
     */
    List<Venue> getVenues(VenueManager venueManager) throws DAOException;
}