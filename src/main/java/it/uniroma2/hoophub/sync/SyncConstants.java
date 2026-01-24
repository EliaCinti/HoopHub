package it.uniroma2.hoophub.sync;

/**
 * Constants used by synchronization components.
 *
 * <p>Centralizes entity type identifiers used by observers
 * to route sync operations to the correct DAOs.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class SyncConstants {

    private SyncConstants() {
        // Utility class - prevent instantiation
    }

    // Logging message fragments
    public static final String FROM = " from ";
    public static final String PRIMARY_SOURCE = ". Primary source ";
    public static final String TAKES_PRECEDENCE = " takes precedence.";

    // Entity type identifiers (must match notifyObservers calls in DAOs)
    public static final String USER = "User";
    public static final String FAN = "Fan";
    public static final String VENUE_MANAGER = "VenueManager";
    public static final String VENUE = "Venue";
    public static final String BOOKING = "Booking";
}