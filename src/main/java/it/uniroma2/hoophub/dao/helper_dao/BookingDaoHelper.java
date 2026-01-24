package it.uniroma2.hoophub.dao.helper_dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;

/**
 * Helper class for loading Booking dependencies.
 *
 * <p>Eliminates code duplication between BookingDaoCsv and BookingDaoMySql
 * by centralizing Fan and Venue retrieval logic.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingDaoHelper {

    private BookingDaoHelper() {
        // Utility class
    }

    /**
     * Immutable container for Booking dependencies.
     *
     * @param fan   the fan who made the booking
     * @param venue the venue for the booking
     */
    public record BookingDependencies(Fan fan, Venue venue) {
    }

    /**
     * Loads Fan and Venue entities required for a Booking.
     *
     * @param fanUsername the fan's username
     * @param venueId     the venue ID
     * @return container with populated Fan and Venue
     * @throws DAOException if retrieval fails or entities not found
     */
    public static BookingDependencies loadDependencies(String fanUsername, int venueId) throws DAOException {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();

        Fan fan = daoFactory.getFanDao().retrieveFan(fanUsername);
        Venue venue = daoFactory.getVenueDao().retrieveVenue(venueId);

        if (fan == null) {
            throw new DAOException("Fan not found for booking: " + fanUsername);
        }
        if (venue == null) {
            throw new DAOException("Venue not found for booking: " + venueId);
        }

        return new BookingDependencies(fan, venue);
    }
}