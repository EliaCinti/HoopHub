package it.uniroma2.hoophub.dao.utility_dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;

/**
 * Helper class to eliminate code duplication between BookingDaoCsv and BookingDaoMySql.
 * Centralizes the logic for retrieving related Fan and Venue entities.
 */
public class BookingDaoHelper {

    private BookingDaoHelper() {
        // Utility class
    }

    /**
     * Simple container to hold the retrieved dependencies.
     * Converted to Java Record for conciseness and immutability.
     */
    public record BookingDependencies(Fan fan, Venue venue) {
    }

    /**
     * Loads Fan and Venue entities by their IDs using the DAO Factory.
     * Throws exception if either entity is not found.
     *
     * @param fanUsername The username of the fan
     * @param venueId     The ID of the venue
     * @return A container with the populated Fan and Venue objects
     * @throws DAOException If retrieval fails or entities are not found
     */
    public static BookingDependencies loadDependencies(String fanUsername, int venueId) throws DAOException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();

        // Recuperiamo i dati
        Fan fan = daoFactory.getFanDao().retrieveFan(fanUsername);
        Venue venue = daoFactory.getVenueDao().retrieveVenue(venueId);

        // Validiamo
        if (fan == null) {
            throw new DAOException("Fan not found for booking: " + fanUsername);
        }
        if (venue == null) {
            throw new DAOException("Venue not found for booking: " + venueId);
        }

        // Ritorniamo il record
        return new BookingDependencies(fan, venue);
    }
}