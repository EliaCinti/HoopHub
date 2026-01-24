package it.uniroma2.hoophub.exception;

import java.time.LocalDate;

/**
 * Exception thrown when a venue's capacity would be exceeded by a booking request.
 * <p>
 * This exception is a specialized type of {@link BookingNotAllowedException} that provides
 * detailed information about capacity constraints. It allows the system to give users
 * specific feedback about why their booking cannot be accommodated, including exactly
 * how many seats were requested versus how many are actually available.
 * </p>
 * <p>
 * This exception is typically thrown by venue managers when attempting to confirm
 * a booking that would exceed the venue's maximum capacity for a given date.
 * </p>
 * <p>
 * The exception includes three key pieces of information:
 * <ul>
 *   <li>The name of the venue that cannot accommodate the request</li>
 *   <li>The number of seats that were requested</li>
 *   <li>The number of seats actually available</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * if (!venue.hasAvailableCapacity(gameDate, seatsRequested)) {
 *     throw new VenueCapacityExceededException(
 *         venue.getName(),
 *         seatsRequested,
 *         venue.getRemainingCapacity(gameDate)
 *     );
 * }
 * </pre>
 * </p>
 *
 * @see BookingNotAllowedException
 * @see it.uniroma2.hoophub.model.Venue#hasAvailableCapacity(LocalDate) (java.time.LocalDate, int)
 * @see it.uniroma2.hoophub.model.VenueManager#confirmBooking(it.uniroma2.hoophub.model.Booking, it.uniroma2.hoophub.model.Fan, it.uniroma2.hoophub.model.Venue)
 */
public class VenueCapacityExceededException extends BookingNotAllowedException {

    private final int requestedSeats;
    private final int availableSeats;
    private final String venueName;

    /**
     * Constructs a new VenueCapacityExceededException with detailed capacity information.
     * <p>
     * Creates an exception with a formatted message that clearly explains the capacity
     * constraint, including the venue name, requested seats, and available seats.
     * The message is automatically generated in a user-friendly format.
     * </p>
     *
     * @param venueName the name of the venue that cannot accommodate the booking
     * @param requestedSeats the number of seats that were requested in the booking
     * @param availableSeats the number of seats actually available at the venue
     * @throws IllegalArgumentException if requestedSeats or availableSeats are negative,
     *         or if venueName is null or empty
     */
    public VenueCapacityExceededException(String venueName, int requestedSeats, int availableSeats) {
        super(String.format(
                "Venue '%s' doesn't have enough capacity. Requested: %d seats, Available: %d seats",
                venueName, requestedSeats, availableSeats
        ));

        if (venueName == null || venueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name cannot be null or empty");
        }
        if (requestedSeats < 0) {
            throw new IllegalArgumentException("Requested seats cannot be negative");
        }
        if (availableSeats < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }

        this.venueName = venueName;
        this.requestedSeats = requestedSeats;
        this.availableSeats = availableSeats;
    }

    /**
     * Returns the number of seats that were requested in the booking.
     *
     * @return the number of requested seats
     */
    public int getRequestedSeats() {
        return requestedSeats;
    }

    /**
     * Returns the number of seats actually available at the venue.
     *
     * @return the number of available seats
     */
    public int getAvailableSeats() {
        return availableSeats;
    }

    /**
     * Returns the name of the venue that cannot accommodate the booking.
     *
     * @return the venue name
     */
    public String getVenueName() {
        return venueName;
    }

    /**
     * Calculates how many seats were over the venue's capacity.
     * <p>
     * This can be useful for suggesting alternative solutions to the user,
     * such as reducing the number of seats or splitting into multiple bookings.
     * </p>
     *
     * @return the difference between requested and available seats (always positive)
     */
    public int getSeatsOverCapacity() {
        return Math.max(0, requestedSeats - availableSeats);
    }

    /**
     * Checks if the venue has any availability at all.
     *
     * @return true if at least one seat is available, false if completely full
     */
    public boolean hasPartialAvailability() {
        return availableSeats > 0;
    }
}

