package it.uniroma2.hoophub.model;

/**
 * Types of venues where NBA games can be watched
 */
public enum VenueType {
    PUB("Pub"),
    BAR("Bar"),
    SPORTS_BAR("Sports Bar"),
    FAN_CLUB("Fan Club"),
    RESTAURANT("Restaurant"),
    LOUNGE("Lounge"),
    PRIVATE_CLUB("Private Club");

    private final String displayName;

    VenueType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
