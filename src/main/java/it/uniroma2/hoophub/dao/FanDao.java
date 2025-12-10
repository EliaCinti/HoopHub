package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import java.util.List;

/**
 * Data Access Object interface for Fan entities.
 * <p>
 * This interface defines the contract for all operations related to fan data persistence.
 * It follows object-oriented principles by accepting domain objects where appropriate,
 * reducing coupling and improving maintainability.
 * </p>
 * <p>
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * The DAO handles conversion between Bean objects (for data transfer) and Model objects
 * (for business logic), including the reconstruction of relationships with other entities
 * like Bookings.
 * </p>
 *
 * @see Fan Domain model representing a fan
 * @see FanBean Bean for data transfer
 */
public interface FanDao {

    /**
     * Saves a new fan in the persistence layer.
     * <p>
     * This method uses {@link FanBean} as input because it represents data coming from
     * the UI layer (registration form). The bean contains flat data without object
     * relationships, which is appropriate for the initial creation operation.
     * </p>
     * <p>
     * The implementation will:
     * <ol>
     *   <li>Validate that the username is not already taken</li>
     *   <li>Hash the password for security</li>
     *   <li>Persist the fan data</li>
     *   <li>Notify observers for cross-persistence synchronization</li>
     * </ol>
     * </p>
     *
     * @param fan@throws DAOException If an error occurs while saving the fan, such as:
     *                   <ul>
     *                     <li>Username already exists (duplicate key)</li>
     *                     <li>Database connection failure</li>
     *                     <li>Constraint violation</li>
     *                   </ul>
     * @throws IllegalArgumentException If fanBean is null or contains invalid data
     */
    void saveFan(Fan fan) throws DAOException;

    /**
     * Retrieves fan details from the persistence layer based on username.
     * <p>
     * This method is an exception to the "pass objects" rule because it represents
     * an <strong>initial lookup</strong> - we use the username to obtain the Fan object
     * when we don't have it yet (e.g., during login or user search).
     * </p>
     * <p>
     * The returned Fan object will have fully populated data, including an empty
     * booking list (bookings are loaded lazily via {@link BookingDao} when needed).
     * </p>
     *
     * @param username The username of the fan to retrieve, must not be null or empty
     * @return A {@link Fan} object if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If username is null or empty
     */
    Fan retrieveFan(String username) throws DAOException;

    /**
     * Retrieves all fans from the persistence layer.
     * <p>
     * Each returned Fan object will have basic data populated (without bookings).
     * This method may be expensive for large datasets and should be used judiciously.
     * Consider pagination for production systems with many fans.
     * </p>
     *
     * @return A list of all fans in the system, empty list if no fans exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Fan> retrieveAllFans() throws DAOException;

    /**
     * Updates an existing fan's details in the persistence layer.
     * <p>
     * This method demonstrates <strong>Information Hiding</strong> by accepting the
     * Fan domain object. The DAO internally extracts the username (primary key) to
     * locate the record to update. This approach means that if the primary key
     * mechanism changes, only the DAO implementation needs to change.
     * </p>
     * <p>
     * The method updates both:
     * <ul>
     *   <li>General user information (from UserBean): full name, gender</li>
     *   <li>Fan-specific information (from Fan model): favorite team, birthday</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Note:</strong> Password updates should go through a separate,
     * security-focused method with proper authentication checks.
     * </p>
     *
     * @param fan The fan object containing updated fan-specific details, must not be null
     * @throws DAOException             If the fan does not exist, or if an error occurs while updating the data
     * @throws IllegalArgumentException If fan or userBean is null
     */
    void updateFan(Fan fan) throws DAOException;

    /**
     * Deletes a fan from the persistence layer.
     * <p>
     * Uses the Fan object to extract the identifier internally, following the
     * Information Hiding principle. This operation should be used carefully as
     * it may have cascading effects on related entities (bookings).
     * </p>
     * <p>
     * <strong>Implementation Note:</strong> Consider the following:
     * <ul>
     *   <li>Cascade deletion of associated bookings, or</li>
     *   <li>Prevent deletion if bookings exist, or</li>
     *   <li>Implement soft deletion (mark as inactive)</li>
     * </ul>
     * </p>
     *
     * @param fan The fan to delete, must not be null
     * @throws DAOException If an error occurs while deleting the fan, such as:
     *                      <ul>
     *                        <li>Fan doesn't exist</li>
     *                        <li>Foreign key constraint violation (has bookings)</li>
     *                        <li>Database connection failure</li>
     *                      </ul>
     * @throws IllegalArgumentException If fan is null
     */
    void deleteFan(Fan fan) throws DAOException;
}