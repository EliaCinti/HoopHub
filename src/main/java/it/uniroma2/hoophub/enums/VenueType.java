package it.uniroma2.hoophub.enums;

/**
 * Types of venues where NBA games can be watched.
 *
 * <p>Each type has a display name and maximum capacity limit
 * enforced during venue creation/update.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see it.uniroma2.hoophub.beans.VenueBean
 */
public enum VenueType {

    /** Traditional pub. Max capacity: 200. */
    PUB("Pub", 200),

    /** Small bar venue. Max capacity: 100. */
    BAR("Bar", 100),

    /** Dedicated sports bar with multiple screens. Max capacity: 500. */
    SPORTS_BAR("Sports Bar", 500),

    /** NBA team fan club venue. Max capacity: 150. */
    FAN_CLUB("Fan Club", 150),

    /** Restaurant with game viewing. Max capacity: 300. */
    RESTAURANT("Restaurant", 300),

    /** Lounge-style venue. Max capacity: 100. */
    LOUNGE("Lounge", 100),

    /** Members-only private club. Max capacity: 200. */
    PRIVATE_CLUB("Private Club", 200);

    private final String displayName;
    private final int maxCapacityLimit;

    VenueType(String displayName, int maxCapacityLimit) {
        this.displayName = displayName;
        this.maxCapacityLimit = maxCapacityLimit;
    }

    /**
     * Returns the UI-friendly display name.
     *
     * @return human-readable venue type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the maximum allowed capacity for this venue type.
     *
     * @return capacity limit enforced by {@link it.uniroma2.hoophub.beans.VenueBean}
     */
    public int getMaxCapacityLimit() {
        return maxCapacityLimit;
    }
}