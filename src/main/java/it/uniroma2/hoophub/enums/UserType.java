package it.uniroma2.hoophub.enums;

/**
 * User roles in HoopHub.
 *
 * <p>Defines the two primary roles:
 * <ul>
 *   <li>{@link #FAN}: Books venues to watch NBA games</li>
 *   <li>{@link #VENUE_MANAGER}: Manages venues and approves bookings</li>
 * </ul>
 * Used for authentication, authorization, and UI routing.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see it.uniroma2.hoophub.model.Fan
 * @see it.uniroma2.hoophub.model.VenueManager
 */
public enum UserType {

    /** Fan user who can search venues and create bookings. */
    FAN("FAN"),

    /** Manager who owns venues and handles booking requests. */
    VENUE_MANAGER("VENUE_MANAGER");

    private final String type;

    UserType(String type) {
        this.type = type;
    }

    /**
     * Returns the string representation for persistence.
     *
     * @return "FAN" or "VENUE_MANAGER"
     */
    public String getType() {
        return type;
    }
}