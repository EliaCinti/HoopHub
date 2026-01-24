package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO interface for Booking entity persistence.
 *
 * <p>Follows DAO best practices: write operations accept models, read operations return
 * models, queries use primitive parameters to prevent circular dependencies.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Booking
 */
public interface BookingDao {

    /**
     * Saves a new booking.
     *
     * @param booking the booking model to save
     * @return the saved booking with generated ID
     * @throws DAOException if save fails
     */
    Booking saveBooking(Booking booking) throws DAOException;

    /**
     * Retrieves a booking by ID.
     *
     * @param bookingId the booking ID
     * @return the booking, or null if not found
     * @throws DAOException if retrieval fails
     */
    Booking retrieveBooking(int bookingId) throws DAOException;

    /**
     * Retrieves all bookings.
     *
     * @return list of all bookings, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveAllBookings() throws DAOException;

    /**
     * Retrieves all bookings for a specific fan.
     *
     * @param fanUsername the fan's username
     * @return list of bookings, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException;

    /**
     * Retrieves all bookings for a specific venue.
     *
     * @param venueId the venue ID
     * @return list of bookings, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException;

    /**
     * Retrieves all bookings for venues managed by a specific manager.
     *
     * @param venueManagerUsername the manager's username
     * @return list of bookings, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves all bookings for a specific date.
     *
     * @param date the date to filter by
     * @return list of bookings, empty if none
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException;

    /**
     * Retrieves bookings by fan and status.
     *
     * @param fanUsername the fan's username
     * @param status      the booking status filter
     * @return list of matching bookings
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException;

    /**
     * Retrieves unnotified bookings for a fan.
     *
     * @param fanUsername the fan's username
     * @return list of unnotified bookings
     * @throws DAOException if retrieval fails
     */
    List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException;

    /**
     * Updates an existing booking.
     *
     * @param booking the booking with updated data
     * @throws DAOException if update fails or booking doesn't exist
     */
    void updateBooking(Booking booking) throws DAOException;

    /**
     * Deletes a booking.
     *
     * @param booking the booking to delete
     * @throws DAOException if deletion fails
     */
    void deleteBooking(Booking booking) throws DAOException;

    /**
     * Checks if a booking exists.
     *
     * @param bookingId the booking ID
     * @return true if exists
     * @throws DAOException if check fails
     */
    boolean bookingExists(int bookingId) throws DAOException;

    /**
     * Generates the next available booking ID.
     *
     * @return next unique booking ID
     * @throws DAOException if generation fails
     */
    int getNextBookingId() throws DAOException;
}