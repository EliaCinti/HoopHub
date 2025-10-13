package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.utilities.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Booking entities.
 * <p>
 * This interface defines the contract for all operations related to booking data persistence.
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * </p>
 */
public interface BookingDao {

    /**
     * Saves a new booking in the persistence system.
     * <p>
     * This method stores a new booking and associates it with the specified fan.
     * </p>
     *
     * @param booking The booking to be saved
     * @param fanUsername The username of the fan this booking belongs to
     * @throws DAOException If there is an error during the save operation
     */
    void saveBooking(Booking booking, String fanUsername) throws DAOException;

    /**
     * Retrieves a booking by its unique identifier.
     *
     * @param bookingId The ID of the booking to retrieve
     * @return The booking with the specified ID, or null if not found
     * @throws DAOException If there is an error accessing the data storage
     */
    Booking retrieveBooking(int bookingId) throws DAOException;

    /**
     * Retrieves all bookings from the persistence layer.
     *
     * @return A list of {@link Booking} objects representing all bookings in the system
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Booking> retrieveAllBookings() throws DAOException;

    /**
     * Retrieves all bookings for a specific fan.
     *
     * @param fanUsername The username of the fan
     * @return A list of bookings for the fan, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException;

    /**
     * Retrieves all bookings for a specific venue.
     *
     * @param venueId The ID of the venue
     * @return A list of bookings for the venue, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException;

    /**
     * Retrieves all bookings for venues managed by a specific venue manager.
     *
     * @param venueManagerUsername The username of the venue manager
     * @return A list of bookings, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves all bookings scheduled for a specific date.
     *
     * @param date The date for which bookings should be retrieved
     * @return A list of bookings on the specified date, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException;

    /**
     * Retrieves all bookings with a specific status for a fan.
     *
     * @param fanUsername The username of the fan
     * @param status The booking status to filter by
     * @return A list of bookings with the specified status, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException;

    /**
     * Retrieves all unnotified bookings for a specific fan.
     *
     * @param fanUsername The username of the fan
     * @return A list of unnotified bookings, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     */
    List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException;

    /**
     * Updates an existing booking in the persistence system.
     *
     * @param booking The booking with updated information
     * @throws DAOException If there is an error updating or if the booking doesn't exist
     */
    void updateBooking(Booking booking) throws DAOException;

    /**
     * Updates the notification status of a specific booking.
     *
     * @param bookingId The ID of the booking to update
     * @param notified The new notification status
     * @throws DAOException If there is an error updating or if the booking doesn't exist
     */
    void updateBookingNotificationStatus(int bookingId, boolean notified) throws DAOException;

    /**
     * Updates the notification status for a list of bookings in a batch operation.
     *
     * @param bookings The list of bookings to update
     * @throws DAOException If there is an error updating any of the bookings
     */
    void updateBookingsNotificationStatus(List<Booking> bookings) throws DAOException;

    /**
     * Deletes a booking from the persistence system.
     *
     * @param bookingId The ID of the booking to delete
     * @throws DAOException If there is an error during deletion or if the booking doesn't exist
     */
    void deleteBooking(int bookingId) throws DAOException;

    /**
     * Checks if a booking with the given ID exists.
     *
     * @param bookingId The ID to check
     * @return true if the booking exists, false otherwise
     * @throws DAOException If there is an error accessing the data storage
     */
    boolean bookingExists(int bookingId) throws DAOException;

    /**
     * Generates the next available booking ID.
     *
     * @return The next available booking ID
     * @throws DAOException If there is an error accessing the data storage
     */
    int getNextBookingId() throws DAOException;
}