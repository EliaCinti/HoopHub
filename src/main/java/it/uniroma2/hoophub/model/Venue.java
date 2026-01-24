package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.VenueType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain entity representing a Venue (sports bar, pub, fan club, etc.).
 *
 * <p>Encapsulates venue state and business logic following BCE principles.
 * State modification restricted to public business operations.</p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> for construction.</p>
 *
 * <h3>Business Rules</h3>
 * <ul>
 *   <li>Must have at least one associated NBA team</li>
 *   <li>FAN_CLUB venues can only have one team</li>
 *   <li>Capacity enforced per VenueType limits</li>
 *   <li>Bookings tracked per date for capacity management</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class Venue {

    /** Immutable primary key. */
    private final int id;
    private String name;
    private VenueType type;
    private String address;
    private String city;
    private int maxCapacity;
    private VenueManager venueManager;
    /** Teams this venue shows games for. */
    private final Set<TeamNBA> associatedTeams;
    /** Bookings organized by game date for capacity tracking. */
    private final Map<LocalDate, List<Booking>> bookingsByDate;

    private Venue(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.address = builder.address;
        this.city = builder.city;
        this.maxCapacity = builder.maxCapacity;
        this.setVenueManager(builder.venueManager);
        this.associatedTeams = new HashSet<>(builder.associatedTeams);
        this.bookingsByDate = new HashMap<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Updates all venue details including associated teams.
     *
     * @param newName        new venue name
     * @param newType        new venue type
     * @param newAddress     new street address
     * @param newCity        new city
     * @param newMaxCapacity new maximum capacity
     * @param newTeams       new set of associated teams
     * @throws IllegalArgumentException if validation fails
     */
    public void updateVenueDetails(String newName, VenueType newType, String newAddress,
                                   String newCity, int newMaxCapacity, Set<TeamNBA> newTeams) {
        validateDetails(newName, newType, newAddress, newCity, newMaxCapacity, newTeams);

        this.setName(newName);
        this.setType(newType);
        this.setAddress(newAddress);
        this.setCity(newCity);
        this.setMaxCapacity(newMaxCapacity);
        this.associatedTeams.clear();
        this.associatedTeams.addAll(newTeams);
    }

    /**
     * Assigns a new manager to this venue.
     *
     * @param newManager the new VenueManager
     * @throws IllegalArgumentException if manager is null
     */
    public void assignNewManager(VenueManager newManager) {
        this.setVenueManager(newManager);
    }

    /**
     * Adds a booking to this venue's tracking.
     *
     * @param booking the booking to add
     * @throws IllegalArgumentException if booking is null or not for this venue
     */
    public void addBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if (booking.getVenueId() != this.id) {
            throw new IllegalArgumentException("Booking must be associated with this venue");
        }
        LocalDate gameDate = booking.getGameDate();
        bookingsByDate.computeIfAbsent(gameDate, k -> new ArrayList<>()).add(booking);
    }

    /**
     * Adds a team to this venue's associated teams.
     *
     * @param team the NBA team to associate
     * @throws IllegalArgumentException if team is null
     * @throws IllegalStateException    if FAN_CLUB already has a team
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
     * Removes a team from this venue's associated teams.
     *
     * @param team the team to remove
     * @return true if removed, false if not associated
     * @throws IllegalArgumentException if team is null
     * @throws IllegalStateException    if trying to remove the last team
     */
    public boolean removeTeam(TeamNBA team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        if (associatedTeams.size() <= 1 && associatedTeams.contains(team)) {
            throw new IllegalStateException("Cannot remove the last team. A venue must have at least one team.");
        }
        return associatedTeams.remove(team);
    }

    /**
     * Checks if a team is associated with this venue.
     *
     * @param team the team to check
     * @return true if team is associated
     */
    public boolean isTeamAssociated(TeamNBA team) {
        if (team == null) {
            return false;
        }
        return associatedTeams.contains(team);
    }

    // ========================================================================
    // PUBLIC QUERIES
    // ========================================================================

    /**
     * Checks if there's available capacity on specified date.
     *
     * @param gameDate the date to check
     * @return true if at least one spot available
     */
    public boolean hasAvailableCapacity(LocalDate gameDate) {
        int confirmedBookings = countConfirmedBookings(gameDate);
        return confirmedBookings < maxCapacity;
    }

    /**
     * Gets remaining capacity for a specific date.
     *
     * @param gameDate the date to check
     * @return number of available spots
     */
    public int getRemainingCapacity(LocalDate gameDate) {
        int confirmedBookings = countConfirmedBookings(gameDate);
        return maxCapacity - confirmedBookings;
    }

    /**
     * Gets a copy of bookings for a specific date.
     *
     * @param date the date to query
     * @return new list of bookings (defensive copy)
     */
    public List<Booking> getBookingsByDate(LocalDate date) {
        return new ArrayList<>(bookingsByDate.getOrDefault(date, new ArrayList<>()));
    }

    /**
     * Gets all bookings across all dates.
     *
     * @return new list of all bookings (defensive copy)
     */
    public List<Booking> getAllBookings() {
        return bookingsByDate.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Gets total booking count.
     *
     * @return number of bookings across all dates
     */
    public int getTotalBookingsCount() {
        return countAllBookings();
    }

    /**
     * Gets formatted full address.
     *
     * @return "address, city"
     */
    public String getFullAddress() {
        return formatAddress(address, city);
    }

    // ========================================================================
    // PUBLIC GETTERS
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
     * Returns unmodifiable view of associated teams.
     *
     * @return unmodifiable team set
     */
    public Set<TeamNBA> getAssociatedTeams() {
        return Collections.unmodifiableSet(associatedTeams);
    }

    // ========================================================================
    // PRIVATE SETTERS
    // ========================================================================

    private void setName(String name) {
        this.name = name;
    }

    private void setType(VenueType type) {
        this.type = type;
    }

    private void setAddress(String address) {
        this.address = address;
    }

    private void setCity(String city) {
        this.city = city;
    }

    private void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    private void setVenueManager(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException("Venue manager cannot be null");
        }
        this.venueManager = venueManager;
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for Venue with fluent API.
     */
    public static class Builder {
        private int id;
        private String name;
        private VenueType type;
        private String address;
        private String city;
        private int maxCapacity;
        private VenueManager venueManager;
        private final Set<TeamNBA> associatedTeams = new HashSet<>();

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

        /**
         * Adds a single team to the builder.
         */
        public Builder addTeam(TeamNBA team) {
            if (team != null) {
                this.associatedTeams.add(team);
            }
            return this;
        }

        /**
         * Adds a collection of teams to the builder.
         */
        public Builder teams(Set<TeamNBA> teams) {
            if (teams != null) {
                this.associatedTeams.addAll(teams);
            }
            return this;
        }

        public Venue build() {
            validate();
            return new Venue(this);
        }

        private void validate() {
            validateDetails(name, type, address, city, maxCapacity, associatedTeams);
            if (venueManager == null) {
                throw new IllegalArgumentException("Venue manager cannot be null");
            }
        }
    }

    // ========================================================================
    // VALIDATION HELPERS
    // ========================================================================

    private static void validateDetails(String name, VenueType type, String address,
                                        String city, int maxCapacity, Set<TeamNBA> associatedTeams) {
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
        if (maxCapacity > type.getMaxCapacityLimit()) {
            throw new IllegalArgumentException(String.format(
                    "Capacity %d exceeds the limit for %s (Max: %d)",
                    maxCapacity, type.getDisplayName(), type.getMaxCapacityLimit()));
        }
        if (associatedTeams.isEmpty()) {
            throw new IllegalArgumentException("A venue must host at least one team.");
        }
        if (type == VenueType.FAN_CLUB && associatedTeams.size() > 1) {
            throw new IllegalStateException("FAN_CLUB venues can only have one associated team");
        }
    }

    private int countConfirmedBookings(LocalDate gameDate) {
        return (int) getBookingsByDate(gameDate).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }

    private int countAllBookings() {
        return bookingsByDate.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    private String formatAddress(String streetAddress, String cityName) {
        return streetAddress + ", " + cityName;
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venue venue = (Venue) o;
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