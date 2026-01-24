package it.uniroma2.hoophub.enums;

/**
 * Status lifecycle for bookings in HoopHub.
 *
 * <p>A booking transitions through states:
 * {@code PENDING} → {@code CONFIRMED}/{@code REJECTED} → {@code CANCELLED} (optional).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public enum BookingStatus {

    /** Awaiting VenueManager approval. Initial state for new bookings. */
    PENDING("Pending"),

    /** Approved by VenueManager. Fan can attend the event. */
    CONFIRMED("Confirmed"),

    /** Rejected by VenueManager. Booking request denied. */
    REJECTED("Rejected"),

    /** Canceled by Fan or VenueManager after confirmation. */
    CANCELLED("Cancelled");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the UI-friendly display name.
     *
     * @return human-readable status name
     */
    public String getDisplayName() {
        return displayName;
    }
}