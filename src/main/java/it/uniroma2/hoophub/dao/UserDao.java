package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;

/**
 * Data Access Object interface for User entities.
 * <p>
 * This interface defines the contract for all operations related to user data persistence.
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * </p>
 */
public interface UserDao {

    /**
     * Validates a user by checking if the provided username and password match
     * an existing record in the persistence layer.
     * <p>
     * If a match is found, the user's type is retrieved and set in the
     * {@link CredentialsBean}. Otherwise, the {@code type} remains {@code null}.
     * </p>
     *
     * @param credentials The {@link CredentialsBean} containing the username and password to validate
     * @throws DAOException If an error occurs while accessing the data storage
     */
    void validateUser(CredentialsBean credentials) throws DAOException;

    /**
     * Saves a new user in the persistence layer.
     * <p>
     * If a user with the same username already exists, a {@link DAOException} is thrown.
     * </p>
     *
     * @param user The {@link UserBean} object containing the user's details
     * @throws DAOException If an error occurs while saving the user or if the user already exists
     */
    void saveUser(UserBean user) throws DAOException;

    /**
     * Retrieves user details from the persistence layer based on the username.
     *
     * @param username The username of the user to retrieve
     * @return A {@code String[]} containing the user's details if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     */
    String[] retrieveUser(String username) throws DAOException;

    /**
     * Checks if a username is already in use in the system.
     *
     * @param username The username to check
     * @return true if the username is already taken, false otherwise
     * @throws DAOException If an error occurs while accessing the data
     */
    boolean isUsernameTaken(String username) throws DAOException;

    /**
     * Updates an existing user's details in the persistence layer.
     *
     * @param user The {@link UserBean} object containing the updated details
     * @throws DAOException If an error occurs while updating the user
     */
    void updateUser(UserBean user) throws DAOException;

    /**
     * Deletes a user from the persistence layer.
     *
     * @param username The username of the user to delete
     * @throws DAOException If an error occurs while deleting the user
     */
    void deleteUser(String username) throws DAOException;
}