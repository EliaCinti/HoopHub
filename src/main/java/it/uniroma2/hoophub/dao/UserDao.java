package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.User;

/**
 * DAO interface for base User entity persistence.
 *
 * <p>Handles common user operations (authentication, CRUD) for all user types.
 * Works with {@link FanDao} and {@link VenueManagerDao} for type-specific data.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see User
 */
public interface UserDao {

    /**
     * Validates user credentials during login.
     *
     * @param credentials username and password to validate
     * @return the user type if valid, null if invalid
     * @throws DAOException if validation fails
     */
    UserType validateUser(Credentials credentials) throws DAOException;

    /**
     * Saves a new user with hashed password.
     *
     * @param user the user model to save
     * @throws DAOException if username exists or save fails
     */
    void saveUser(User user) throws DAOException;

    /**
     * Retrieves raw user data by username.
     *
     * @param username the username to look up
     * @return String array [username, hashedPassword, fullName, gender, type], or null
     * @throws DAOException if retrieval fails
     */
    String[] retrieveUser(String username) throws DAOException;

    /**
     * Checks if a username is already taken.
     *
     * @param username the username to check
     * @return true if taken, false if available
     * @throws DAOException if check fails
     */
    boolean isUsernameTaken(String username) throws DAOException;

    /**
     * Updates common user fields (fullName, gender).
     *
     * @param user the user with updated data
     * @throws DAOException if update fails
     */
    void updateUser(User user) throws DAOException;

    /**
     * Deletes a user. Call after deleting type-specific data.
     *
     * @param user the user to delete
     * @throws DAOException if deletion fails
     */
    void deleteUser(User user) throws DAOException;
}