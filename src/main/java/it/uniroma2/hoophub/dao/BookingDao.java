package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.utilities.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Booking entities.
 * <p>
 * This interface defines the contract for all operations related to booking data persistence.
 * It follows the <strong>Information Hiding</strong> principle by accepting domain objects
 * (Fan, Venue) instead of primitive identifiers, reducing coupling between layers.
 * </p>
 * <p>
 * Implementations will provide specific logic for different storage mechanisms (e.g., CSV, MySQL).
 * The DAO is responsible for converting between Bean objects (for persistence) and Model objects
 * (for business logic), hiding these implementation details from clients.
 * </p>
 *
 * @see Booking Domain model representing a booking
 * @see it.uniroma2.hoophub.beans.BookingBean Bean for data transfer
 */
public interface BookingDao {

    /**
     * Saves a new booking in the persistence system.
     * <p>
     * The booking object already contains references to Fan and Venue, which are used
     * internally by the DAO to extract the necessary identifiers for persistence.
     * This follows the <strong>Tell, Don't Ask</strong> principle - the client tells
     * the DAO to save a booking, without needing to know how the DAO extracts identifiers.
     * </p>
     *
     * @param booking The booking to be saved, must contain valid Fan and Venue references
     * @throws DAOException If there is an error during the save operation, such as:
     *                      <ul>
     *                        <li>Database connection failure</li>
     *                        <li>Constraint violation (e.g., duplicate booking)</li>
     *                        <li>Invalid foreign key references</li>
     *                      </ul>
     * @throws IllegalArgumentException If booking is null
     */
    void saveBooking(Booking booking) throws DAOException;

    /**
     * Retrieves a booking by its unique identifier.
     * <p>
     * This method is an exception to the "pass objects" rule because it represents
     * an <strong>initial lookup</strong> - we use the ID to obtain the Booking object
     * when we don't have it yet.
     * </p>
     * <p>
     * The returned Booking object will have fully populated references to Fan and Venue,
     * allowing the client to navigate relationships without additional DAO calls.
     * </p>
     *
     * @param bookingId The ID of the booking to retrieve, must be positive
     * @return The booking with the specified ID, or {@code null} if not found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If bookingId is negative
     */
    Booking retrieveBooking(int bookingId) throws DAOException;

    /**
     * Retrieves all bookings from the persistence layer.
     * <p>
     * Each returned Booking object will have fully populated Fan and Venue references.
     * This method may be expensive for large datasets and should be used judiciously.
     * </p>
     *
     * @return A list of all bookings in the system, empty list if no bookings exist
     * @throws DAOException If an error occurs while accessing the data storage
     */
    List<Booking> retrieveAllBookings() throws DAOException;

    /**
     * Retrieves all bookings for a specific fan.
     * <p>
     * This method demonstrates <strong>Information Hiding</strong> - the DAO internally
     * extracts the fan's identifier from the Fan object, hiding this detail from clients.
     * If the fan's primary key changes in the future (e.g., from username to UUID),
     * only the DAO implementation needs to change, not the client code.
     * </p>
     *
     * @param fan The fan whose bookings should be retrieved, must not be null
     * @return A list of bookings for the fan, ordered by date descending, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fan is null
     */
    List<Booking> retrieveBookingsByFan(Fan fan) throws DAOException;

    /**
     * Retrieves all bookings for a specific venue.
     * <p>
     * Uses the Venue object to extract the identifier internally, following
     * the Information Hiding principle.
     * </p>
     *
     * @param venue The venue whose bookings should be retrieved, must not be null
     * @return A list of bookings for the venue, ordered by date descending, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If venue is null
     */
    List<Booking> retrieveBookingsByVenue(Venue venue) throws DAOException;

    /**
     * Retrieves all bookings for venues managed by a specific venue manager.
     * <p>
     * This method aggregates bookings across all venues managed by the specified
     * venue manager. The DAO internally determines which venues belong to the manager
     * and retrieves their bookings.
     * </p>
     *
     * @param venueManager The venue manager whose bookings should be retrieved, must not be null
     * @return A list of bookings for all managed venues, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If venueManager is null
     */
    List<Booking> retrieveBookingsByVenueManager(VenueManager venueManager) throws DAOException;

    /**
     * Retrieves all bookings scheduled for a specific date.
     * <p>
     * Note: This method uses a primitive (LocalDate) rather than an object because
     * the date is a <strong>query parameter</strong>, not a domain entity. This is
     * an acceptable exception to the "pass objects" rule.
     * </p>
     *
     * @param date The date for which bookings should be retrieved, must not be null
     * @return A list of bookings on the specified date, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If date is null
     */
    List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException;

    /**
     * Retrieves all bookings with a specific status for a fan.
     * <p>
     * This method combines domain object (Fan) and enum (BookingStatus) filtering.
     * Useful for queries like "get all pending bookings for this fan".
     * </p>
     *
     * @param fan The fan whose bookings should be retrieved, must not be null
     * @param status The booking status to filter by, must not be null
     * @return A list of bookings with the specified status, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fan or status is null
     */
    List<Booking> retrieveBookingsByStatus(Fan fan, BookingStatus status) throws DAOException;

    /**
     * Retrieves all unnotified bookings for a specific fan.
     * <p>
     * Used by the notification system to find bookings that have been confirmed
     * but the fan hasn't been notified yet. This supports the notification workflow
     * where fans are alerted about booking status changes.
     * </p>
     *
     * @param fan The fan whose unnotified bookings should be retrieved, must not be null
     * @return A list of unnotified bookings, empty list if none found
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If fan is null
     */
    List<Booking> retrieveUnnotifiedBookings(Fan fan) throws DAOException;

    /**
     * Updates an existing booking in the persistence system.
     * <p>
     * The booking object contains all updated information, including its identifier.
     * The DAO internally extracts the ID to perform the update operation.
     * This method is typically used after state transitions (e.g., PENDING → CONFIRMED)
     * or when notification status changes.
     * </p>
     *
     * @param booking The booking with updated information, must not be null and must have valid ID
     * @throws DAOException If there is an error updating or if the booking doesn't exist
     * @throws IllegalArgumentException If booking is null or has invalid ID
     */
    void updateBooking(Booking booking) throws DAOException;

    /**
     * Deletes a booking from the persistence system.
     * <p>
     * Uses the Booking object to extract the identifier internally. This method
     * should be used carefully as it permanently removes the booking. Consider
     * using status changes (e.g., CANCELLED) instead of deletion for audit trail purposes.
     * </p>
     *
     * @param booking The booking to delete, must not be null
     * @throws DAOException If there is an error during deletion or if the booking doesn't exist
     * @throws IllegalArgumentException If booking is null
     */
    void deleteBooking(Booking booking) throws DAOException;

    /**
     * Checks if a booking exists in the persistence system.
     * <p>
     * Uses the Booking object to extract the identifier internally. Useful for
     * validation before performing operations that require the booking to exist.
     * </p>
     *
     * @param booking The booking to check, must not be null
     * @return {@code true} if the booking exists, {@code false} otherwise
     * @throws DAOException If there is an error accessing the data storage
     * @throws IllegalArgumentException If booking is null
     */
    boolean bookingExists(Booking booking) throws DAOException;

    /**
     * Generates the next available booking ID.
     * <p>
     * This method is used when creating new bookings to obtain a unique identifier.
     * The implementation depends on the persistence mechanism:
     * <ul>
     *   <li><strong>MySQL:</strong> May use AUTO_INCREMENT or sequence</li>
     *   <li><strong>CSV:</strong> Calculates max existing ID + 1</li>
     * </ul>
     * </p>
     *
     * @return The next available booking ID, guaranteed to be positive and unique
     * @throws DAOException If there is an error accessing the data storage
     */
    int getNextBookingId() throws DAOException;
}