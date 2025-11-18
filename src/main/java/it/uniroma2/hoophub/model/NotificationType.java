package it.uniroma2.hoophub.model;

/**
 * Enumeration of notification types in the HoopHub system.
 * <p>
 * Each notification type represents a specific event that requires
 * user attention. The system uses these types to categorize and
 * format notifications appropriately for different user types.
 * </p>
 *
 * @author Elia Cinti
 */
public enum NotificationType {
    /**
     * Notification sent to VenueManager when a new booking request is submitted.
     * <p>
     * Triggered when: A Fan creates a new booking request for a venue.
     * Recipients: VenueManager who owns the venue.
     * </p>
     */
    BOOKING_REQUESTED("Booking Requested"),

    /**
     * Notification sent to Fan when their booking is approved by the VenueManager.
     * <p>
     * Triggered when: VenueManager accepts a pending booking request.
     * Recipients: Fan who made the booking.
     * </p>
     */
    BOOKING_APPROVED("Booking Approved"),

    /**
     * Notification sent to Fan when their booking is rejected by the VenueManager.
     * <p>
     * Triggered when: VenueManager rejects a pending booking request.
     * Recipients: Fan who made the booking.
     * </p>
     */
    BOOKING_REJECTED("Booking Rejected"),

    /**
     * Notification sent to Fan when their confirmed booking is cancelled.
     * <p>
     * Triggered when: Fan or VenueManager cancels a confirmed booking.
     * Recipients: Both Fan and VenueManager.
     * </p>
     */
    BOOKING_CANCELLED("Booking Cancelled"),

    /**
     * Generic system notification for important announcements.
     * <p>
     * Triggered when: System administrators need to notify users.
     * Recipients: All users or specific user groups.
     * </p>
     */
    SYSTEM_NOTIFICATION("System Notification");

    private final String displayName;

    /**
     * Constructor for NotificationType enum.
     *
     * @param displayName Human-readable name for UI display
     */
    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this notification type.
     *
     * @return Display name suitable for UI presentation
     */
    public String getDisplayName() {
        return displayName;
    }
}
