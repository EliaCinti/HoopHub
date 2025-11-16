package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.VenueType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a Venue entity.
 * This class encapsulates a Venue's state and business logic.
 * State modification is restricted to public business operations, as per BCE principles.
 *
 * @author Elia Cinti
 */
public class Venue {
    private final int id; // Immutable Primary Key
    private String name;
    private VenueType type;
    private String address;
    private String city;
    private int maxCapacity;

    private VenueManager venueManager;
    private final Set<TeamNBA> associatedTeams;
    private final Map<LocalDate, List<Booking>> bookingsByDate;

    /**
     * Private constructor for use by the Builder.
     * Sets initial state.
     */
    private Venue(Builder builder) {
        // Immutable PK is set DIRECTLY. There is no setId() method.
        this.id = builder.id;

        // Initial state for mutable fields is set DIRECTLY
        this.name = builder.name;
        this.type = builder.type;
        this.address = builder.address;
        this.city = builder.city;
        this.maxCapacity = builder.maxCapacity;

        // We call the private setter for manager to reuse validation
        this.setVenueManager(builder.venueManager);

        // Initialize associated teams set (mutable set)
        this.associatedTeams = new HashSet<>();

        // The map is initialized directly. There is no setBookingsByDate() method.
        this.bookingsByDate = new HashMap<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Public business operation to update the Venue's core details.
     * This is the ONLY way to modify these fields after construction.
     *
     * @param newName        The new name for the venue.
     * @param newType        The new type for the venue.
     * @param newAddress     The new street address.
     * @param newCity        The new city.
     * @param newMaxCapacity The new maximum capacity.
     * @throws IllegalArgumentException if validation for new data fails.
     */
    public void updateVenueDetails(String newName, VenueType newType, String newAddress, String newCity, int newMaxCapacity) {
        // 1. Validate the new data
        validateDetails(newName, newType, newAddress, newCity, newMaxCapacity);

        // 2. Mutate the state using private setters
        this.setName(newName);
        this.setType(newType);
        this.setAddress(newAddress);
        this.setCity(newCity);
        this.setMaxCapacity(newMaxCapacity);
    }

    /**
     * Public business operation to assign a new manager to this venue.
     *
     * @param newManager The VenueManager entity to assign.
     * @throws IllegalArgumentException if the manager is null.
     */
    public void assignNewManager(VenueManager newManager) {
        // Delegate to private setter that contains validation
        this.setVenueManager(newManager);
    }

    /**
     * Adds a booking to this venue's internal tracking.
     * This is the only way to add a booking to the venue's list.
     *
     * @param booking The booking to add.
     * @throws IllegalArgumentException if booking is null or not for this venue.
     */
    public void addBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if (!booking.getVenue().equals(this)) {
            throw new IllegalArgumentException("Booking must be associated with this venue");
        }
        LocalDate gameDate = booking.getGameDate();
        bookingsByDate.computeIfAbsent(gameDate, k -> new ArrayList<>()).add(booking);
    }

    /**
     * Adds a team to this venue's associated teams list.
     * FAN_CLUB venues can only have one team associated.
     *
     * @param team The NBA team to associate with this venue
     * @throws IllegalArgumentException if team is null
     * @throws IllegalStateException if trying to add more than one team to a FAN_CLUB venue
     */
    public void addTeam(TeamNBA team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        if (this.type == VenueType.FAN_CLUB && !associatedTeams.isEmpty()) {
            throw new IllegalStateException("FAN_CLUB venues can only have one associated team");
        }
        associatedTeams.add(team);
    }

    /**
     * Removes a team from this venue's associated teams list.
     *
     * @param team The NBA team to remove from this venue
     * @throws IllegalArgumentException if team is null
     * @return true if the team was removed, false if it wasn't associated
     */
    public boolean removeTeam(TeamNBA team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        return associatedTeams.remove(team);
    }

    /**
     * Checks if a team is associated with this venue.
     *
     * @param team The team to check
     * @return true if the team is associated with this venue, false otherwise
     */
    public boolean isTeamAssociated(TeamNBA team) {
        if (team == null) {
            return false;
        }
        return associatedTeams.contains(team);
    }

    // ========================================================================
    // PUBLIC QUERIES (Read-Only Access & Business Questions)
    // ========================================================================

    /**
     * Checks if there's available capacity for a new booking on the specified date.
     *
     * @param gameDate The date to check availability for
     * @return true if there's at least one spot available, false otherwise
     */
    public boolean hasAvailableCapacity(LocalDate gameDate) {
        int confirmedBookings = countConfirmedBookings(gameDate);
        return confirmedBookings < maxCapacity;
    }

    /**
     * Gets remaining capacity for a specific date.
     *
     * @param gameDate The date to check
     * @return Number of available spots
     */
    public int getRemainingCapacity(LocalDate gameDate) {
        int confirmedBookings = countConfirmedBookings(gameDate);
        return maxCapacity - confirmedBookings;
    }

    /**
     * Gets a COPY of the bookings list for a specific date.
     *
     * @param date The date to check.
     * @return A new list containing the bookings for that date.
     */
    public List<Booking> getBookingsByDate(LocalDate date) {
        // Returns a copy, protecting the internal map's list
        return new ArrayList<>(bookingsByDate.getOrDefault(date, new ArrayList<>()));
    }

    /**
     * Gets a new list of all bookings for this venue (all dates).
     *
     * @return A new list containing all bookings.
     */
    public List<Booking> getAllBookings() {
        // Returns a new list, protecting the internal map
        return bookingsByDate.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Gets total number of bookings (all statuses, all dates).
     */
    public int getTotalBookingsCount() {
        return countAllBookings();
    }

    /**
     * Gets venue's full address as formatted string.
     */
    public String getFullAddress() {
        return formatAddress(address, city);
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
    // ========================================================================

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VenueType getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public VenueManager getVenueManager() {
        return venueManager;
    }

    public String getVenueManagerUsername() {
        return venueManager.getUsername();
    }

    /**
     * Gets an unmodifiable view of the teams associated with this venue.
     *
     * @return An unmodifiable set of associated NBA teams
     */
    public Set<TeamNBA> getAssociatedTeams() {
        return Collections.unmodifiableSet(associatedTeams);
    }

    // ========================================================================
    // PRIVATE SETTERS (Internal State Mutation)
    // ========================================================================

    /**
     * Private setter for name.
     * Only called by updateVenueDetails().
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * Private setter for type.
     * Only called by updateVenueDetails().
     */
    private void setType(VenueType type) {
        this.type = type;
    }

    /**
     * Private setter for address.
     * Only called by updateVenueDetails().
     */
    private void setAddress(String address) {
        this.address = address;
    }

    /**
     * Private setter for city.
     * Only called by updateVenueDetails().
     */
    private void setCity(String city) {
        this.city = city;
    }

    /**
     * Private setter for max capacity.
     * Only called by updateVenueDetails().
     */
    private void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Private setter for venue manager.
     * Called by assignNewManager() AND constructor to reuse validation.
     */
    private void setVenueManager(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException("Venue manager cannot be null");
        }
        this.venueManager = venueManager;
    }

    // ========================================================================
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

    public static class Builder {
        private int id;
        private String name;
        private VenueType type;
        private String address;
        private String city;
        private int maxCapacity;
        private VenueManager venueManager;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(VenueType type) {
            this.type = type;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder maxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public Builder venueManager(VenueManager venueManager) {
            this.venueManager = venueManager;
            return this;
        }

        public Venue build() {
            validate();
            return new Venue(this);
        }

        /**
         * Validation logic called by the Builder at construction time.
         */
        private void validate() {
            // Use the static validation helper
            validateDetails(name, type, address, city, maxCapacity);
            // Specific validation for manager (must be present at build time)
            if (venueManager == null) {
                throw new IllegalArgumentException("Venue manager cannot be null");
            }
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION & HELPER METHODS (Internal Logic)
    // ========================================================================

    /**
     * Validates core venue details.
     * Static to be reusable by the Builder and updateVenueDetails.
     */
    private static void validateDetails(String name, VenueType type, String address, String city, int maxCapacity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Venue type cannot be null");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Max capacity must be greater than 0");
        }
        if (maxCapacity > 10000) {
            throw new IllegalArgumentException("Max capacity cannot exceed 10000");
        }
    }

    /**
     * Counts confirmed bookings for a specific date.
     */
    private int countConfirmedBookings(LocalDate gameDate) {
        return (int) getBookingsByDate(gameDate).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }

    /**
     * Counts all bookings across all dates.
     */
    private int countAllBookings() {
        return bookingsByDate.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Formats the address string.
     */
    private String formatAddress(String streetAddress, String cityName) {
        return streetAddress + ", " + cityName;
    }

    // ========================================================================
    // UTILITY METHODS (equals, hashCode, toString)
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venue venue = (Venue) o;
        // Equality is based on the immutable primary key (id)
        return id == venue.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Venue{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", city='" + city + '\'' +
                ", maxCapacity=" + maxCapacity +
                ", manager='" + venueManager.getUsername() + '\'' +
                '}';
    }
}