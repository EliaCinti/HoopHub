package it.uniroma2.hoophub.enums;

/**
 * Enumeration representing the different types of users in the HoopHub system.
 * <p>
 * This enum defines the two primary user roles supported by the application:
 * fans who seek to book basketball venues and venue managers who provide
 * and manage basketball courts and facilities.
 * </p>
 * <p>
 * The enum is used throughout the application for:
 * <ul>
 *   <li>User registration and authentication</li>
 *   <li>Role-based access control</li>
 *   <li>UI navigation and feature availability</li>
 *   <li>Database persistence and data validation</li>
 * </ul>
 * </p>
 *
 * @see it.uniroma2.hoophub.model.User Base user class
 * @see it.uniroma2.hoophub.model.Fan Fan-specific implementation
 * @see it.uniroma2.hoophub.model.VenueManager VenueManager-specific implementation
 */
public enum UserType {

    /**
     * Represents a fan user who can book basketball venues and access fan-specific features.
     * <p>
     * Fans have the following capabilities:
     * <ul>
     *   <li>Search and browse available basketball venues</li>
     *   <li>Book time slots at basketball courts</li>
     *   <li>View their booking history and upcoming reservations</li>
     *   <li>Receive notifications for confirmed bookings</li>
     *   <li>Manage and cancel their reservations</li>
     * </ul>
     * </p>
     */
    FAN("FAN"),

    /**
     * Represents a venue manager user who can manage basketball venues and handle bookings.
     * <p>
     * Venue managers have the following capabilities:
     * <ul>
     *   <li>Manage their basketball venues and courts</li>
     *   <li>Review and confirm booking requests</li>
     *   <li>Set venue availability and pricing</li>
     *   <li>View schedules and booking information</li>
     *   <li>Access booking history for all their venues</li>
     * </ul>
     * </p>
     */
    VENUE_MANAGER("VENUE_MANAGER");

    /**
     * The string representation of the user type used for persistence and validation.
     */
    private final String type;

    /**
     * Creates a UserType enum constant with the specified string value.
     *
     * @param type The string representation of the user type
     */
    UserType(String type) {
        this.type = type;
    }

    /**
     * Returns the string representation of this user type.
     * <p>
     * This value is used for database persistence, form validation,
     * and string-based comparisons throughout the application.
     * </p>
     *
     * @return The string representation of the user type ("FAN" or "VENUE_MANAGER")
     */
    public String getType() {
        return type;
    }
}
