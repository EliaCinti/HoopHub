package it.uniroma2.hoophub.exception;

/**
 * Exception thrown when booking operations are not allowed.
 * <p>
 * This exception is used to indicate that a requested booking
 * cannot be completed due to business rule violations or system constraints.
 * It serves as a way to enforce the application's booking policies and
 * prevent invalid booking scenarios.
 * </p>
 * <p>
 * Common scenarios that trigger this exception:
 * <ul>
 *   <li>Attempting to book at a venue that is already at full capacity</li>
 *   <li>Trying to book a duplicate reservation for the same game</li>
 *   <li>Booking at a venue not managed by the specified venue manager</li>
 *   <li>Attempting to confirm bookings for venues not under the manager's control</li>
 *   <li>Scheduling conflicts with existing bookings in the system</li>
 *   <li>Violating booking policies or constraints</li>
 * </ul>
 * </p>
 * <p>
 * This exception is typically caught by controllers and UI components to
 * provide meaningful feedback to users about why their booking request
 * could not be processed, allowing them to make appropriate adjustments.
 * </p>
 * <p>
 * Note: This exception extends {@code Exception} rather than {@code RuntimeException},
 * requiring explicit handling by calling code to ensure that booking failures
 * are properly addressed rather than causing unexpected application termination.
 * </p>
 *
 * @see it.uniroma2.hoophub.model.Fan#addBooking(it.uniroma2.hoophub.model.Booking)
 * @see it.uniroma2.hoophub.model.VenueManager#confirmBooking(it.uniroma2.hoophub.model.Booking, it.uniroma2.hoophub.model.Fan, it.uniroma2.hoophub.model.Venue)
 * @see it.uniroma2.hoophub.model.Booking for booking management
 */
public class BookingNotAllowedException extends Exception {

    /**
     * Constructs a new BookingNotAllowedException with no detail message.
     * <p>
     * This constructor creates a basic exception without any specific
     * error information. The lack of a message indicates that the calling
     * code is expected to handle the exception based on the context in
     * which it was thrown, such as displaying a generic "booking not allowed"
     * message to the user.
     * </p>
     * <p>
     * This design choice keeps the exception lightweight while allowing
     * the UI layer to provide context-appropriate error messages based
     * on the specific operation that failed.
     * </p>
     */
    public BookingNotAllowedException() {
        super();
    }

    /**
     * Constructs a new BookingNotAllowedException with the specified detail message.
     * <p>
     * This constructor allows for custom error messages that provide more specific
     * information about why the booking was not allowed.
     * </p>
     *
     * @param message the detail message explaining why the booking was not allowed
     */
    public BookingNotAllowedException(String message) {
        super(message);
    }

    /**
     * Constructs a new BookingNotAllowedException with the specified detail message and cause.
     * <p>
     * This constructor is useful when wrapping another exception that caused
     * the booking failure.
     * </p>
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BookingNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
