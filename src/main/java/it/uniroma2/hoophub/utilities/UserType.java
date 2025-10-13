package it.uniroma2.hoophub.utilities;

/**
 * Enumeration representing the different types of users in the MindHarbor system.
 * <p>
 * This enum defines the two primary user roles supported by the application:
 * patients who seek psychological consultation and psychologists who provide
 * professional services. Each user type has different capabilities and access
 * levels within the system.
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
     * Represents a patient user who can book appointments and access patient-specific features.
     * <p>
     * Patients have the following capabilities:
     * <ul>
     *   <li>Book appointments with their assigned psychologist</li>
     *   <li>View their appointment history and schedule</li>
     *   <li>Receive notifications for confirmed appointments</li>
     *   <li>Maintain a personal diary</li>
     * </ul>
     * </p>
     */
    FAN("FAN"),

    /**
     * Represents a psychologist user who can manage patients and confirm appointments.
     * <p>
     * Psychologists have the following capabilities:
     * <ul>
     *   <li>Manage their assigned patients</li>
     *   <li>Review and confirm appointment requests</li>
     *   <li>View their schedule and patient information</li>
     *   <li>Access appointment history for all their patients</li>
     * </ul>
     * </p>
     */
    VENUE_MANAGER("VENUE_MANAGER"),;

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
     * @return The string representation of the user type ("PATIENT" or "PSYCHOLOGIST")
     */
    public String getType() {
        return type;
    }
}
