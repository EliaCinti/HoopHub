package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import java.util.List;

/**
 * Data Access Object interface for Venue entities.
 * <p>
 * This interface defines the contract for all operations related to venue data persistence.
 * It follows object-oriented principles by accepting domain objects (VenueManager) instead
 * of primitive identifiers where appropriate, promoting loose coupling and maintainability.
 * </p>
 * <p>
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * The DAO handles conversion between Bean objects (for persistence) and Model objects (for
 * business logic), including the reconstruction of relationships with VenueManager and Bookings.
 * </p>
 *
 * @see Venue Domain model representing a venue
 * @see VenueBean Bean for data transfer
 */
public interface VenueDao {

    /**
     * Saves a new venue in the persistence layer.
     * <p>
     * This method uses {@link VenueBean} as input because it represents data coming from
     * the UI layer (venue creation form). The bean contains the venue manager's username
     * as a string, which is appropriate for data transfer.
     * </p>
     * <p>
     * The implementation will:
     * <ol>
     *   <li>Validate that the venue manager exists</li>
     *   <li>Generate a new venue ID if needed</li>
     *   <li>Persist the venue data</li>
     *   <li>Notify observers for cross-persistence synchronization</li>
     * </ol>
     * </p>
     *
     * @param venueBean The bean containing the venue's details from the UI, must not be null
     * @throws DAOException If an error occurs while saving the venue, such as:
     *                      <ul>
     *                        <li>Invalid venue manager reference</li>
     *                        <li>Database connection failure</li>
     *                        <li>Constraint violation</li>
     *                      </ul>
     * @throws IllegalArgumentException If venueBean is null or contains invalid data
     */
    void saveVenue(VenueBean venueBean) throws DAOException;

    /**
     * Retrieves a venue by its unique identifier.
     * <p>
     * This method is an exception to the "pass objects" rule because it represents
     * an <strong>initial lookup</strong> - we use the ID to obtain the Venue object
     * when we don't have it yet (e.g., when selecting a venue from a dropdown).
     * </p>
     * <p>
     * The returned Venue object will have:
     * <ul>
     *   <li>Fully populated basic data (name, address, capacity, etc.)</li>
     *   <li>A reference to the managing VenueManager</li>
     *   <li>An empty bookings map (bookings are loaded lazily when needed)</li>
     * </ul>
     * </p>
     *
     * @param venueId The ID of the venue to retrieve, must be positive
     * @return The {@link Venue} with the specified ID, or {@code null} if not found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueId is not positive
     */
    Venue retrieveVenue(int venueId) throws DAOException;

    /**
     * Retrieves all venues from the persistence layer.
     * <p>
     * Each returned Venue object will have fully populated data including VenueManager
     * reference. This method may be expensive for large datasets and should be used
     * judiciously. Consider pagination for production systems with many venues.
     * </p>
     *
     * @return A list of all venues in the system, empty list if no venues exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Venue> retrieveAllVenues() throws DAOException;

    /**
     * Retrieves all venues managed by a specific venue manager.
     * <p>
     * This method demonstrates <strong>Information Hiding</strong> by accepting the
     * VenueManager domain object. The DAO internally extracts the username (primary key)
     * to query venues. If the primary key mechanism changes (e.g., from username to UUID),
     * only the DAO implementation needs to change, not the client code.
     * </p>
     *
     * @param venueManager The venue manager whose venues should be retrieved, must not be null
     * @return A list of venues managed by the venue manager, empty list if none found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venueManager is null
     */
    List<Venue> retrieveVenuesByManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves all venues in a specific city.
     * <p>
     * Note: This method uses a primitive (String) rather than an object because
     * the city is a <strong>query parameter</strong>, not a domain entity. This is
     * an acceptable exception to the "pass objects" rule.
     * </p>
     *
     * @param city The city to filter by, must not be null or empty
     * @return A list of venues in the specified city, empty list if none found
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If city is null or empty
     */
    List<Venue> retrieveVenuesByCity(String city) throws DAOException;

    /**
     * Updates an existing venue's details in the persistence layer.
     * <p>
     * Uses the Venue object to extract the identifier internally, following the
     * Information Hiding principle. The entire venue object is passed, allowing
     * the DAO to update all modifiable fields (name, address, capacity, etc.).
     * </p>
     * <p>
     * <strong>Note:</strong> The venue's ID and managing VenueManager should not
     * change after creation. If manager transfer is needed, implement a separate
     * method with appropriate security checks.
     * </p>
     *
     * @param venue The venue object containing updated information, must not be null
     * @throws DAOException If an error occurs while updating or if the venue doesn't exist
     * @throws IllegalArgumentException If venue is null
     */
    void updateVenue(Venue venue) throws DAOException;

    /**
     * Deletes a venue from the persistence layer.
     * <p>
     * Uses the Venue object to extract the identifier internally. This operation
     * should be used carefully as it may have cascading effects on related entities
     * (bookings).
     * </p>
     * <p>
     * <strong>Implementation Note:</strong> Consider the following:
     * <ul>
     *   <li>Prevent deletion if bookings exist for this venue</li>
     *   <li>Cascade deletion of associated bookings (with caution)</li>
     *   <li>Implement soft deletion (mark as inactive) instead</li>
     * </ul>
     * </p>
     *
     * @param venue The venue to delete, must not be null
     * @throws DAOException If an error occurs during deletion, such as:
     *                      <ul>
     *                        <li>Venue doesn't exist</li>
     *                        <li>Foreign key constraint violation (has bookings)</li>
     *                        <li>Database connection failure</li>
     *                      </ul>
     * @throws IllegalArgumentException If venue is null
     */
    void deleteVenue(Venue venue) throws DAOException;

    /**
     * Checks if a venue exists in the persistence system.
     * <p>
     * Uses the Venue object to extract the identifier internally. Useful for
     * validation before performing operations that require the venue to exist.
     * </p>
     *
     * @param venue The venue to check, must not be null
     * @return {@code true} if the venue exists, {@code false} otherwise
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If venue is null
     */
    boolean venueExists(Venue venue) throws DAOException;

    /**
     * Generates the next available venue ID.
     * <p>
     * This method is used when creating new venues to obtain a unique identifier.
     * The implementation depends on the persistence mechanism:
     * <ul>
     *   <li><strong>MySQL:</strong> May use AUTO_INCREMENT or sequence</li>
     *   <li><strong>CSV:</strong> Calculates max existing ID + 1</li>
     * </ul>
     * </p>
     *
     * @return The next available venue ID, guaranteed to be positive and unique
     * @throws DAOException If there is an error accessing the data storage
     */
    int getNextVenueId() throws DAOException;
}