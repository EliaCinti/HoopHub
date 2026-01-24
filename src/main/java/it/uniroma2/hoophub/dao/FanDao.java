package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import java.util.List;

/**
 * DAO interface for Fan entity persistence.
 *
 * <p>Handles Fan-specific CRUD operations. Works with {@link UserDao} for common
 * user data. Bookings are loaded lazily via {@link BookingDao}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Fan
 */
public interface FanDao {

    /**
     * Saves a new fan. Password is hashed before storage.
     *
     * @param fan the fan model to save
     * @throws DAOException if username exists or save fails
     */
    void saveFan(Fan fan) throws DAOException;

    /**
     * Retrieves a fan by username.
     *
     * @param username the fan's username
     * @return the fan, or null if not found
     * @throws DAOException if retrieval fails
     */
    Fan retrieveFan(String username) throws DAOException;

    /**
     * Retrieves all fans.
     *
     * @return list of all fans, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Fan> retrieveAllFans() throws DAOException;

    /**
     * Updates a fan's details (name, gender, team, birthday).
     *
     * @param fan the fan with updated data
     * @throws DAOException if a fan doesn't exist or update fails
     */
    void updateFan(Fan fan) throws DAOException;

    /**
     * Deletes a fan. May cascade to bookings.
     *
     * @param fan the fan to delete
     * @throws DAOException if deletion fails or has FK constraints
     */
    void deleteFan(Fan fan) throws DAOException;
}