package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.VenueType;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for venue data transfer.
 *
 * <p>Used when VenueManagers create or modify venues via UI.
 * Includes validation for name, address, city, capacity, and team associations.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueBean {

    private int id;
    private String name;
    private VenueType type;
    private String address;
    private String city;
    private int maxCapacity;
    private String venueManagerUsername;
    private Set<TeamNBA> associatedTeams;

    private VenueBean(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.address = builder.address;
        this.city = builder.city;
        this.maxCapacity = builder.maxCapacity;
        this.venueManagerUsername = builder.venueManagerUsername;
        this.associatedTeams = builder.associatedTeams != null
                ? new HashSet<>(builder.associatedTeams)
                : new HashSet<>();
    }

    // ========================================================================
    // STATIC VALIDATION METHODS
    // ========================================================================

    /**
     * Validates venue name (3-100 characters).
     */
    public static void validateNameSyntax(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name cannot be empty");
        }
        if (name.trim().length() < 3) {
            throw new IllegalArgumentException("Venue name must be at least 3 characters");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Venue name cannot exceed 100 characters");
        }
    }

    /**
     * Validates address (not empty).
     */
    public static void validateAddressSyntax(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }
    }

    /**
     * Validates city (not empty).
     */
    public static void validateCitySyntax(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
    }

    /**
     * Validates capacity against venue type limits.
     */
    public static void validateCapacity(int capacity, VenueType type) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (type != null && capacity > type.getMaxCapacityLimit()) {
            throw new IllegalArgumentException(String.format(
                    "Capacity %d exceeds limit for %s (max: %d)",
                    capacity, type.getDisplayName(), type.getMaxCapacityLimit()));
        }
    }

    /**
     * Validates team associations (at least one required, Fan Club allows only one).
     */
    public static void validateTeams(Set<TeamNBA> teams, VenueType type) {
        if (teams == null || teams.isEmpty()) {
            throw new IllegalArgumentException("At least one team must be selected");
        }
        if (type == VenueType.FAN_CLUB && teams.size() > 1) {
            throw new IllegalArgumentException("Fan Club venues can only have one team");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for VenueBean with validation on build.
     */
    public static class Builder {
        private int id;
        private String name;
        private VenueType type;
        private String address;
        private String city;
        private int maxCapacity;
        private String venueManagerUsername;
        private Set<TeamNBA> associatedTeams = new HashSet<>();

        public Builder id(int id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(VenueType type) { this.type = type; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder maxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; return this; }
        public Builder venueManagerUsername(String username) { this.venueManagerUsername = username; return this; }

        public Builder associatedTeams(Set<TeamNBA> teams) {
            this.associatedTeams = teams != null ? new HashSet<>(teams) : new HashSet<>();
            return this;
        }

        public Builder addTeam(TeamNBA team) {
            if (team != null) this.associatedTeams.add(team);
            return this;
        }

        public VenueBean build() {
            validateNameSyntax(name);
            validateAddressSyntax(address);
            validateCitySyntax(city);
            validateCapacity(maxCapacity, type);
            validateTeams(associatedTeams, type);
            return new VenueBean(this);
        }
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public VenueType getType() { return type; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public int getMaxCapacity() { return maxCapacity; }
    public String getVenueManagerUsername() { return venueManagerUsername; }
    public Set<TeamNBA> getAssociatedTeams() { return new HashSet<>(associatedTeams); }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(VenueType type) { this.type = type; }
    public void setAddress(String address) { this.address = address; }
    public void setCity(String city) { this.city = city; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setVenueManagerUsername(String username) { this.venueManagerUsername = username; }
    public void setAssociatedTeams(Set<TeamNBA> teams) {
        this.associatedTeams = teams != null ? new HashSet<>(teams) : new HashSet<>();
    }
}