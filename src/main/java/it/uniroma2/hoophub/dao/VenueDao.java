package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import java.util.List;
import java.util.Set;

/**
 * DAO interface for Venue entity persistence.
 *
 * <p>Handles venue CRUD and team associations. Uses primitive parameters
 * for queries to prevent circular dependencies.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Venue
 */
public interface VenueDao {

    /**
     * Saves a new venue.
     *
     * @param venue the venue model to save
     * @return saved venue with generated ID
     * @throws DAOException if save fails
     */
    Venue saveVenue(Venue venue) throws DAOException;

    /**
     * Retrieves a venue by ID.
     *
     * @param venueId the venue ID
     * @return the venue, or null if not found
     * @throws DAOException if retrieval fails
     */
    Venue retrieveVenue(int venueId) throws DAOException;

    /**
     * Retrieves all venues.
     *
     * @return list of all venues
     * @throws DAOException if retrieval fails
     */
    List<Venue> retrieveAllVenues() throws DAOException;

    /**
     * Retrieves venues by manager username.
     *
     * @param venueManagerUsername the manager's username
     * @return list of managed venues
     * @throws DAOException if retrieval fails
     */
    List<Venue> retrieveVenuesByManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves venues by city.
     *
     * @param city the city name
     * @return list of venues in that city
     * @throws DAOException if retrieval fails
     */
    List<Venue> retrieveVenuesByCity(String city) throws DAOException;

    /**
     * Updates a venue.
     *
     * @param venue the venue with updated data
     * @throws DAOException if update fails
     */
    void updateVenue(Venue venue) throws DAOException;

    /**
     * Deletes a venue.
     *
     * @param venue the venue to delete
     * @throws DAOException if deletion fails
     */
    void deleteVenue(Venue venue) throws DAOException;

    /**
     * Checks if a venue exists.
     *
     * @param venueId the venue ID
     * @return true if exists
     * @throws DAOException if check fails
     */
    boolean venueExists(int venueId) throws DAOException;

    /**
     * Generates the next available venue ID.
     *
     * @return next unique venue ID
     * @throws DAOException if generation fails
     */
    int getNextVenueId() throws DAOException;

    /**
     * Associates a team with a venue.
     *
     * @param venue the venue
     * @param team  the team to associate
     * @throws DAOException if operation fails
     */
    void saveVenueTeam(Venue venue, TeamNBA team) throws DAOException;

    /**
     * Removes a team association from a venue.
     *
     * @param venue the venue
     * @param team  the team to disassociate
     * @throws DAOException if operation fails
     */
    void deleteVenueTeam(Venue venue, TeamNBA team) throws DAOException;

    /**
     * Retrieves all teams associated with a venue.
     *
     * @param venueId the venue ID
     * @return set of associated teams
     * @throws DAOException if retrieval fails
     */
    Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException;

    /**
     * Removes all team associations from a venue.
     *
     * @param venue the venue
     * @throws DAOException if operation fails
     */
    void deleteAllVenueTeams(Venue venue) throws DAOException;
}