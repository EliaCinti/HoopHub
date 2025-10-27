package it.uniroma2.hoophub.utilities;

/**
 * Enum representing the type of notification.
 */
public enum NotificationType {
    BOOKING_REQUESTED(
            "Booking Requested",
            "You have received a new booking request"
    ),
    BOOKING_APPROVED(
            "Booking Approved",
            "Your booking request has been approved"
    ),
    BOOKING_REJECTED(
            "Booking Rejected",
            "Your booking request has been rejected"
    ),
    BOOKING_CANCELLED(
            "Booking Cancelled",
            "A booking has been cancelled"
    );

    private final String displayName;
    private final String defaultMessage;

    NotificationType(String displayName, String defaultMessage) {
        this.displayName = displayName;
        this.defaultMessage = defaultMessage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Checks if this notification type is for VenueManager.
     *
     * @return true if notification is for VenueManager
     */
    public boolean isForVenueManager() {
        return this == BOOKING_REQUESTED || this == BOOKING_CANCELLED;
    }

    /**
     * Checks if this notification type is for Fan.
     *
     * @return true if notification is for Fan
     */
    public boolean isForFan() {
        return this == BOOKING_APPROVED || this == BOOKING_REJECTED;
    }
}
