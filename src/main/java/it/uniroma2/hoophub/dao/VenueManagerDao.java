package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;

import java.util.List;

/**
 * Data Access Object interface for VenueManager entities.
 * <p>
 * This interface defines the contract for all operations related to venue manager data persistence.
 * It follows object-oriented principles by accepting domain objects where appropriate,
 * reducing coupling between layers and improving maintainability.
 * </p>
 * <p>
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * The DAO handles conversion between Bean objects (for data transfer) and Model objects
 * (for business logic), including the reconstruction of relationships with managed Venues.
 * </p>
 *
 * @see VenueManager Domain model representing a venue manager
 * @see VenueManagerBean Bean for data transfer
 */
public interface VenueManagerDao {

    /**
     * Saves a new venue manager in the persistence layer.
     * <p>
     * This method uses {@link VenueManagerBean} as input because it represents data coming
     * from the UI layer (registration form). The bean contains flat data without object
     * relationships, which is appropriate for the initial creation operation.
     * </p>
     * <p>
     * The implementation will:
     * <ol>
     *   <li>Validate that the username is not already taken</li>
     *   <li>Hash the password for security</li>
     *   <li>Validate phone number format</li>
     *   <li>Persist the venue manager data</li>
     *   <li>Notify observers for cross-persistence synchronization</li>
     * </ol>
     * </p>
     *
     * @param venueManager@throws DAOException If an error occurs while saving, such as:
     *                            <ul>
     *                              <li>Username already exists (duplicate key)</li>
     *                              <li>Database connection failure</li>
     *                              <li>Constraint violation</li>
     *                            </ul>
     * @throws IllegalArgumentException If venueManagerBean is null or contains invalid data
     */
    void saveVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves venue manager details from the persistence layer based on username.
     * <p>
     * This method is an exception to the "pass objects" rule because it represents
     * an <strong>initial lookup</strong> - we use the username to obtain the VenueManager
     * object when we don't have it yet (e.g., during login or user search).
     * </p>
     * <p>
     * The returned VenueManager object will have:
     * <ul>
     *   <li>Fully populated basic data (username, full name, company, phone, etc.)</li>
     *   <li>An empty venues list (venues are loaded lazily via {@link #getVenues(VenueManager)})</li>
     * </ul>
     * </p>
     *
     * @param username The username of the venue manager to retrieve, must not be null or empty
     * @return A {@link VenueManager} object if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If username is null or empty
     */
    VenueManager retrieveVenueManager(String username) throws DAOException;

    /**
     * Retrieves all venue managers from the persistence layer.
     * <p>
     * Each returned VenueManager object will have basic data populated (without venues list).
     * This method may be expensive for large datasets and should be used judiciously.
     * Consider pagination for production systems with many venue managers.
     * </p>
     *
     * @return A list of all venue managers in the system, empty list if no venue managers exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<VenueManager> retrieveAllVenueManagers() throws DAOException;

    /**
     * Updates an existing venue manager's details in the persistence layer.
     * <p>
     * This method demonstrates <strong>Information Hiding</strong> by accepting the
     * VenueManager domain object. The DAO internally extracts the username (primary key)
     * to locate the record to update. This approach means that if the primary key
     * mechanism changes, only the DAO implementation needs to change.
     * </p>
     * <p>
     * The method updates both:
     * <ul>
     *   <li>General user information (from UserBean): full name, gender</li>
     *   <li>VenueManager-specific information (from VenueManager model): company name, phone number</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Note:</strong> Password updates should go through a separate,
     * security-focused method with proper authentication checks.
     * </p>
     *
     * @param venueManager The venue manager object containing updated manager-specific details, must not be null
     * @throws DAOException             If an error occurs while updating the venue manager
     * @throws IllegalArgumentException If venueManager or userBean is null
     */
    void updateVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Deletes a venue manager from the persistence layer.
     * <p>
     * Uses the VenueManager object to extract the identifier internally, following
     * the Information Hiding principle. This operation should be used carefully as
     * it may have cascading effects on related entities (managed venues and their bookings).
     * </p>
     * <p>
     * <strong>Implementation Note:</strong> Consider the following:
     * <ul>
     *   <li>Prevent deletion if the manager has active venues</li>
     *   <li>Reassign venues to another manager before deletion</li>
     *   <li>Implement soft deletion (mark as inactive)</li>
     * </ul>
     * </p>
     *
     * @param venueManager The venue manager to delete, must not be null
     * @throws DAOException If an error occurs while deleting, such as:
     *                      <ul>
     *                        <li>Venue manager doesn't exist</li>
     *                        <li>Foreign key constraint violation (has venues)</li>
     *                        <li>Database connection failure</li>
     *                      </ul>
     * @throws IllegalArgumentException If venueManager is null
     */
    void deleteVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves all venues managed by a specific venue manager.
     * <p>
     * This method uses the VenueManager object to extract the identifier internally,
     * demonstrating the Information Hiding principle. It's designed to be called when
     * the venue manager's complete profile with venues is needed.
     * </p>
     * <p>
     * This method is particularly useful for:
     * <ul>
     *   <li>Displaying a manager's dashboard with all their venues</li>
     *   <li>Populating the VenueManager's managedVenues list lazily</li>
     *   <li>Generating reports on a manager's portfolio</li>
     * </ul>
     * </p>
     *
     * @param venueManager The venue manager, whose venues should be retrieved, must not be null
     * @return A list of venues managed by the venue manager, empty list if none found
     * @throws DAOException If an error occurs while accessing the data
     * @throws IllegalArgumentException If venueManager is null
     */
    List<Venue> getVenues(VenueManager venueManager) throws DAOException;
}