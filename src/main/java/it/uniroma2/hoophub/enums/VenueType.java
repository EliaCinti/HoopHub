package it.uniroma2.hoophub.enums;

/**
 * Types of venues where NBA games can be watched
 */
public enum VenueType {
    PUB("Pub", 200),
    BAR("Bar", 100),
    SPORTS_BAR("Sports Bar", 500),
    FAN_CLUB("Fan Club", 150),
    RESTAURANT("Restaurant", 300),
    LOUNGE("Lounge", 100),
    PRIVATE_CLUB("Private Club", 200);

    private final String displayName;
    private final int maxCapacityLimit;

    VenueType(String displayName, int maxCapacityLimit) {
        this.displayName = displayName;
        this.maxCapacityLimit = maxCapacityLimit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxCapacityLimit() {
        return maxCapacityLimit;
    }
}
