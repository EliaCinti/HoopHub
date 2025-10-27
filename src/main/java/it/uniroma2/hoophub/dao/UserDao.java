package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.User;

/**
 * Data Access Object interface for User entities.
 * <p>
 * This interface defines the contract for all operations related to base user data persistence.
 * It handles common user operations that apply to all user types (Fan and VenueManager),
 * including authentication and basic CRUD operations.
 * </p>
 * <p>
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * This DAO works in conjunction with specialized DAOs (FanDao, VenueManagerDao) to provide
 * a complete user management system with proper separation of concerns.
 * </p>
 *
 * @see User Base domain model for all user types
 * @see UserBean Base bean for data transfer
 * @see FanDao DAO for Fan-specific operations
 * @see VenueManagerDao DAO for VenueManager-specific operations
 */
public interface UserDao {

    /**
     * Validates a user by checking if the provided username and password match
     * an existing record in the persistence layer.
     * <p>
     * This method is used during the login process. It performs the following steps:
     * <ol>
     *   <li>Looks up the user by username</li>
     *   <li>Verifies the password (using secure hash comparison)</li>
     *   <li>If valid, retrieves and sets the user type in the CredentialsBean</li>
     *   <li>If invalid, leaves the type as {@code null}</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Security Note:</strong> This method should use constant-time comparison
     * for password verification to prevent timing attacks. The actual password should
     * never be returned, only the validation result.
     * </p>
     * <p>
     * After successful validation, the controller can use the type field to determine
     * which specialized DAO (FanDao or VenueManagerDao) to call for retrieving the
     * complete user object.
     * </p>
     *
     * @param credentials The credentials bean containing username and password to validate,
     *                    must not be null. The type field will be populated if validation succeeds.
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If credentials is null or contains null/empty username or password
     */
    void validateUser(CredentialsBean credentials) throws DAOException;

    /**
     * Saves a new user in the persistence layer.
     * <p>
     * This method uses {@link UserBean} as input because it represents data coming from
     * the UI layer during the registration process. This is typically called by specialized
     * DAOs (FanDao, VenueManagerDao) as part of their save operations to persist the
     * common user data before saving type-specific data.
     * </p>
     * <p>
     * The implementation will:
     * <ol>
     *   <li>Validate that the username is not already taken</li>
     *   <li>Hash the password using a secure algorithm (e.g., BCrypt)</li>
     *   <li>Persist the user data in the users table</li>
     *   <li>Notify observers for cross-persistence synchronization</li>
     * </ol>
     * </p>
     *
     * @param userBean The bean containing the user's details from the UI, must not be null
     * @throws DAOException If an error occurs while saving the user, such as:
     *                      <ul>
     *                        <li>Username already exists (duplicate key)</li>
     *                        <li>Database connection failure</li>
     *                        <li>Constraint violation</li>
     *                      </ul>
     * @throws IllegalArgumentException If userBean is null or contains invalid data
     */
    void saveUser(UserBean userBean) throws DAOException;

    /**
     * Retrieves user details from the persistence layer based on username.
     * <p>
     * This method returns the raw data as a String array rather than a User object
     * because User is abstract and cannot be instantiated directly. The specialized
     * DAOs (FanDao, VenueManagerDao) use this method to retrieve common user data
     * and combine it with type-specific data to construct complete domain objects.
     * </p>
     * <p>
     * This method is an exception to the "pass objects" rule because it represents
     * an <strong>initial lookup</strong> and returns primitive data that will be used
     * to construct domain objects.
     * </p>
     * <p>
     * <strong>Array Structure:</strong> The returned array typically contains:
     * <ol start="0">
     *   <li>username (String)</li>
     *   <li>hashedPassword (String)</li>
     *   <li>fullName (String)</li>
     *   <li>gender (String)</li>
     *   <li>type (String - "FAN" or "VENUE_MANAGER")</li>
     * </ol>
     * </p>
     *
     * @param username The username of the user to retrieve, must not be null or empty
     * @return A String array containing user details if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     * @throws IllegalArgumentException If username is null or empty
     */
    String[] retrieveUser(String username) throws DAOException;

    /**
     * Checks if a username is already in use in the system.
     * <p>
     * This method is used during registration to prevent duplicate usernames.
     * It performs a simple existence check without retrieving full user data,
     * making it more efficient than {@link #retrieveUser(String)} for validation purposes.
     * </p>
     *
     * @param username The username to check, must not be null or empty
     * @return {@code true} if the username is already taken, {@code false} otherwise
     * @throws DAOException If an error occurs while accessing the data
     * @throws IllegalArgumentException If username is null or empty
     */
    boolean isUsernameTaken(String username) throws DAOException;

    /**
     * Updates an existing user's common details in the persistence layer.
     * <p>
     * This method demonstrates <strong>Information Hiding</strong> by accepting both
     * the User domain object and a UserBean with updated data. The DAO internally
     * extracts the username (primary key) from the User object to locate the record
     * to update. This approach means that if the primary key mechanism changes,
     * only the DAO implementation needs to change.
     * </p>
     * <p>
     * This method updates common user fields (full name, gender) but not type-specific
     * fields (favorite team, company name, etc.). For complete user updates, specialized
     * DAOs should be used.
     * </p>
     * <p>
     * <strong>Note:</strong> Password updates should go through a separate,
     * security-focused method with proper authentication and validation.
     * </p>
     *
     * @param user The user object to identify which record to update, must not be null
     * @param userBean The bean containing updated user details, must not be null
     * @throws DAOException If an error occurs while updating the user
     * @throws IllegalArgumentException If user or userBean is null
     */
    void updateUser(User user, UserBean userBean) throws DAOException;

    /**
     * Deletes a user from the persistence layer.
     * <p>
     * Uses the User object to extract the identifier internally, following the
     * Information Hiding principle. This is typically called by specialized DAOs
     * (FanDao, VenueManagerDao) as part of their delete operations to remove the
     * common user data after removing type-specific data.
     * </p>
     * <p>
     * <strong>Implementation Note:</strong> This operation should be coordinated
     * with specialized DAO deletions to maintain referential integrity. Consider:
     * <ul>
     *   <li>Deleting type-specific data first (fan or venue manager record)</li>
     *   <li>Then deleting the common user record</li>
     *   <li>Using database transactions to ensure atomicity</li>
     * </ul>
     * </p>
     *
     * @param user The user to delete, must not be null
     * @throws DAOException If an error occurs while deleting the user, such as:
     *                      <ul>
     *                        <li>User doesn't exist</li>
     *                        <li>Foreign key constraint violation</li>
     *                        <li>Database connection failure</li>
     *                      </ul>
     * @throws IllegalArgumentException If user is null
     */
    void deleteUser(User user) throws DAOException;
}