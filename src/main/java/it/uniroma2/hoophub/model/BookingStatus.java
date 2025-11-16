package it.uniroma2.hoophub.model;

/**
 * Status of a booking
 */
public enum BookingStatus {
    PENDING("Pending"),      // In attesa di conferma
    CONFIRMED("Confirmed"),  // Confermata dal manager
    REJECTED("Rejected"),    // Rifiutata dal manager
    CANCELLED("Cancelled");  // Cancellata dal fan

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
