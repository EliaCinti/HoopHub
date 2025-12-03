package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Booking entities.
 * <p>
 * This interface defines the contract for all operations related to booking data persistence.
 * It follows the <strong>DAO Design Best Practices</strong>:
 * <ul>
 *   <li><strong>Write operations (save/update):</strong> Accept {@link BookingBean} (DTO) to reduce coupling</li>
 *   <li><strong>Delete operations:</strong> Accept primitive ID to avoid unnecessary object construction</li>
 *   <li><strong>Read operations:</strong> Return {@link Booking} (model) with business logic</li>
 *   <li><strong>Query operations:</strong> Accept primitive parameters (String, int) to prevent circular dependencies</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Design Rationale:</strong>
 * Previous design accepted entity objects (Fan, Venue) in query methods, which created circular
 * dependencies between DAOs (BookingDao → FanDao → BookingDao). The new design uses primitive
 * identifiers, breaking these cycles while maintaining clean separation of concerns.
 * </p>
 *
 * @see Booking Domain model representing a booking
 * @see BookingBean DTO for data transfer
 */
public interface BookingDao {

    /**
     * Saves a new booking in the persistence system.
     *
     * @param bookingBean The booking data to be saved, must not be null
     * @throws DAOException If there is an error during the save operation
     * @throws IllegalArgumentException If bookingBean is null or contains invalid data
     */
    void saveBooking(BookingBean bookingBean) throws DAOException;

    /**
     * Retrieves a booking by its unique identifier.
     *
     * @param bookingId The ID of the booking to retrieve, must be positive
     * @return The booking with the specified ID, or {@code null} if not found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If bookingId is not positive
     */
    Booking retrieveBooking(int bookingId) throws DAOException;

    /**
     * Retrieves all bookings from the persistence layer.
     *
     * @return A list of all bookings in the system, empty list if no bookings exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Booking> retrieveAllBookings() throws DAOException;

    /**
     * Retrieves all bookings for a specific fan.
     *
     * @param fanUsername The username of the fan whose bookings should be retrieved
     * @return A list of bookings for the fan, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fanUsername is null or empty
     */
    List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException;

    /**
     * Retrieves all bookings for a specific venue.
     *
     * @param venueId The ID of the venue whose bookings should be retrieved
     * @return A list of bookings for the venue, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If venueId is not positive
     */
    List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException;

    /**
     * Retrieves all bookings for venues managed by a specific venue manager.
     *
     * @param venueManagerUsername The username of the venue manager
     * @return A list of bookings for all managed venues, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If venueManagerUsername is null or empty
     */
    List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException;

    /**
     * Retrieves all bookings scheduled for a specific date.
     *
     * @param date The date for which bookings should be retrieved
     * @return A list of bookings on the specified date, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If date is null
     */
    List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException;

    /**
     * Retrieves all bookings with a specific status for a fan.
     *
     * @param fanUsername The username of the fan
     * @param status The booking status to filter by
     * @return A list of bookings with the specified status, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fanUsername or status is null
     */
    List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException;

    /**
     * Retrieves all unnotified bookings for a specific fan.
     *
     * @param fanUsername The username of the fan
     * @return A list of unnotified bookings, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fanUsername is null or empty
     */
    List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException;

    /**
     * Updates an existing booking in the persistence system.
     *
     * @param bookingBean The booking data with updated information
     * @throws DAOException If there is an error updating or if the booking doesn't exist
     * @throws IllegalArgumentException If bookingBean is null or has invalid ID
     */
    void updateBooking(BookingBean bookingBean) throws DAOException;

    /**
     * Deletes a booking from the persistence system.
     *
     * @param bookingId The ID of the booking to delete
     * @throws DAOException If there is an error during deletion or if the booking doesn't exist
     * @throws IllegalArgumentException If bookingId is not positive
     */
    void deleteBooking(int bookingId) throws DAOException;

    /**
     * Checks if a booking with the specified ID exists in the persistence system.
     *
     * @param bookingId The ID of the booking to check
     * @return {@code true} if the booking exists, {@code false} otherwise
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If bookingId is not positive
     */
    boolean bookingExists(int bookingId) throws DAOException;

    /**
     * Generates the next available booking ID.
     *
     * @return The next available booking ID, guaranteed to be positive and unique
     * @throws DAOException If there is an error accessing the data storage
     */
    int getNextBookingId() throws DAOException;
}
