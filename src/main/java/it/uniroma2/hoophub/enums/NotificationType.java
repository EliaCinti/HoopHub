package it.uniroma2.hoophub.enums;

/**
 * Types of notifications in HoopHub.
 *
 * <p>Each type represents a booking-related or system event
 * requiring user attention.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public enum NotificationType {

    /** Sent to VenueManager when a Fan submits a new booking request. */
    BOOKING_REQUESTED("Booking Requested"),

    /** Sent to Fan when VenueManager approves their booking. */
    BOOKING_APPROVED("Booking Approved"),

    /** Sent to Fan when VenueManager rejects their booking. */
    BOOKING_REJECTED("Booking Rejected"),

    /** Sent to Fan/VenueManager when a confirmed booking is canceled. */
    BOOKING_CANCELLED("Booking Cancelled"),

    /** Generic system announcement for all users. */
    SYSTEM_NOTIFICATION("System Notification");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the UI-friendly display name.
     *
     * @return human-readable notification type
     */
    public String getDisplayName() {
        return displayName;
    }
}
