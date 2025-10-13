package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import java.util.List;

/**
 * Data Access Object interface for Fan entities.
 * <p>
 * This interface defines the contract for all operations related to fan data persistence.
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * </p>
 */
public interface FanDao {

    /**
     * Saves a new fan in the persistence layer.
     * <p>
     * If a fan with the same username already exists, a {@link DAOException} is thrown.
     * </p>
     *
     * @param fan The {@link FanBean} object containing the fan's details
     * @throws DAOException If an error occurs while saving the fan or if the fan already exists
     */
    void saveFan(FanBean fan) throws DAOException;

    /**
     * Retrieves fan details from the persistence layer based on the username.
     *
     * @param username The username of the fan to retrieve
     * @return A {@link Fan} object if found, otherwise {@code null}
     * @throws DAOException If an error occurs while accessing the data storage
     */
    Fan retrieveFan(String username) throws DAOException;

    /**
     * Retrieves all fans from the persistence layer.
     *
     * @return A list of {@link Fan} objects representing all fans in the system
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Fan> retrieveAllFans() throws DAOException;

    /**
     * Updates an existing fan's details in the persistence layer.
     * <p>
     * This method updates both the user's general information and the fan's specific details.
     * </p>
     *
     * @param fan The {@link Fan} object containing the updated fan-specific details
     * @param user The {@link UserBean} object containing the updated general user details
     * @throws DAOException If the fan does not exist, or if an error occurs while updating the data
     */
    void updateFan(Fan fan, UserBean user) throws DAOException;

    /**
     * Deletes a fan from the persistence layer.
     *
     * @param username The username of the fan to delete
     * @throws DAOException If an error occurs while deleting the fan
     */
    void deleteFan(String username) throws DAOException;
}
