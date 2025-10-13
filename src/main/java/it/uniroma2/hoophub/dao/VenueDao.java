package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import java.util.List;

/**
 * Data Access Object interface for Venue entities.
 * <p>
 * This interface defines the contract for all operations related to venue data persistence.
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * </p>
 */
public interface VenueDao {

    /**
     * Saves a new venue in the persistence layer.
     *
     * @param venue The {@link VenueBean} object containing the venue's details
     * @throws DAOException If an error occurs while saving the venue
     */
    void saveVenue(VenueBean venue) throws DAOException;

    /**
     * Retrieves a venue by its unique identifier.
     *
     * @param venueId The ID of the venue to retrieve
     * @return The {@link Venue} with the specified ID, or null if not found
     * @throws DAOException If an error occurs while accessing the data storage
     */
    Venue retrieveVenue(int venueId) throws DAOException;

    /**
     * Retrieves all venues from the persistence layer.
     *
     * @return A list of {@link Venue} objects representing all venues in the system
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Venue> retrieveAllVenues() throws DAOException;

    /**
     * Retrieves all venues managed by a specific venue manager.
     *
     * @param venueManagerUsername The username of the venue manager
     * @return A list of {@link Venue} objects managed by the venue manager
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Venue> retrieveVenuesByManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves all venues in a specific city.
     *
     * @param city The city to filter by
     * @return A list of {@link Venue} objects in the specified city
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Venue> retrieveVenuesByCity(String city) throws DAOException;

    /**
     * Updates an existing venue's details in the persistence layer.
     *
     * @param venue The {@link Venue} object containing updated information
     * @throws DAOException If an error occurs while updating or if the venue doesn't exist
     */
    void updateVenue(Venue venue) throws DAOException;

    /**
     * Deletes a venue from the persistence layer.
     *
     * @param venueId The ID of the venue to delete
     * @throws DAOException If an error occurs during deletion or if the venue doesn't exist
     */
    void deleteVenue(int venueId) throws DAOException;

    /**
     * Checks if a venue with the given ID exists.
     *
     * @param venueId The ID to check
     * @return true if the venue exists, false otherwise
     * @throws DAOException If an error occurs while accessing the data storage
     */
    boolean venueExists(int venueId) throws DAOException;

    /**
     * Generates the next available venue ID.
     *
     * @return The next available venue ID
     * @throws DAOException If an error occurs while accessing the data storage
     */
    int getNextVenueId() throws DAOException;
}
