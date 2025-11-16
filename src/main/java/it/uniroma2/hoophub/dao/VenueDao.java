package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object interface for Venue entities.
 * <p>
 * This interface follows DAO design best practices with Bean-based write operations
 * and primitive-parameter queries to prevent circular dependencies.
 * </p>
 *
 * @see Venue Domain model representing a venue
 * @see VenueBean DTO for data transfer
 */
public interface VenueDao {

    /**
     * Saves a new venue in the persistence layer.
     *
     * @param venueBean The bean containing the venue's details, must not be null
     * @throws DAOException If an error occurs while saving the venue
     * @throws IllegalArgumentException If venueBean is null or contains invalid data
     */
    void saveVenue(VenueBean venueBean) throws DAOException;

    /**
     * Retrieves a venue by its unique identifier.
     *
     * @param venueId The ID of the venue to retrieve, must be positive
     * @return The {@link Venue} with the specified ID, or {@code null} if not found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueId is not positive
     */
    Venue retrieveVenue(int venueId) throws DAOException;

    /**
     * Retrieves all venues from the persistence layer.
     *
     * @return A list of all venues in the system, empty list if no venues exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Venue> retrieveAllVenues() throws DAOException;

    /**
     * Retrieves all venues managed by a specific venue manager.
     *
     * @param venueManagerUsername The username of the venue manager
     * @return A list of venues managed by the venue manager, empty list if none found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueManagerUsername is null or empty
     */
    List<Venue> retrieveVenuesByManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves all venues in a specific city.
     *
     * @param city The city to filter by, must not be null or empty
     * @return A list of venues in the specified city, empty list if none found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If city is null or empty
     */
    List<Venue> retrieveVenuesByCity(String city) throws DAOException;

    /**
     * Updates an existing venue's details in the persistence layer.
     *
     * @param venueBean The bean containing updated venue information, must not be null
     * @throws DAOException If an error occurs while updating or if the venue doesn't exist
     * @throws IllegalArgumentException If venueBean is null
     */
    void updateVenue(VenueBean venueBean) throws DAOException;

    /**
     * Deletes a venue from the persistence layer.
     *
     * @param venueId The ID of the venue to delete, must be positive
     * @throws DAOException If an error occurs during deletion or if the venue doesn't exist
     * @throws IllegalArgumentException If venueId is not positive
     */
    void deleteVenue(int venueId) throws DAOException;

    /**
     * Checks if a venue exists in the persistence system.
     *
     * @param venueId The ID of the venue to check, must be positive
     * @return {@code true} if the venue exists, {@code false} otherwise
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueId is not positive
     */
    boolean venueExists(int venueId) throws DAOException;

    /**
     * Generates the next available venue ID.
     *
     * @return The next available venue ID, guaranteed to be positive and unique
     * @throws DAOException If there is an error accessing the data storage
     */
    int getNextVenueId() throws DAOException;

    /**
     * Associates a team with a venue.
     *
     * @param venueId The ID of the venue
     * @param team The NBA team to associate
     * @throws DAOException If an error occurs during the operation
     * @throws IllegalArgumentException If venueId is not positive or team is null
     */
    void saveVenueTeam(int venueId, TeamNBA team) throws DAOException;

    /**
     * Removes a team association from a venue.
     *
     * @param venueId The ID of the venue
     * @param team The NBA team to disassociate
     * @throws DAOException If an error occurs during the operation
     * @throws IllegalArgumentException If venueId is not positive or team is null
     */
    void deleteVenueTeam(int venueId, TeamNBA team) throws DAOException;

    /**
     * Retrieves all teams associated with a venue.
     *
     * @param venueId The ID of the venue
     * @return A set of NBA teams associated with the venue, empty set if none
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueId is not positive
     */
    Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException;

    /**
     * Removes all team associations from a venue.
     *
     * @param venueId The ID of the venue
     * @throws DAOException If an error occurs during the operation
     * @throws IllegalArgumentException If venueId is not positive
     */
    void deleteAllVenueTeams(int venueId) throws DAOException;
}
