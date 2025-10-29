package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.VenueType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a venue with business logic for capacity management and bookings.
 */
public class Venue {
    private int id;
    private String name;
    private VenueType type;
    private String address;
    private String city;
    private int maxCapacity;

    private VenueManager venueManager;

    private Map<LocalDate, List<Booking>> bookingsByDate = new HashMap<>();

    private Venue(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.address = builder.address;
        this.city = builder.city;
        this.maxCapacity = builder.maxCapacity;
        this.venueManager = builder.venueManager;
    }

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

        private void validate() {
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
            if (venueManager == null) {
                throw new IllegalArgumentException("Venue manager cannot be null");
            }
        }
    }

    // ========== PUBLIC API - Core Operations ==========

    /**
     * Adds a booking to this venue.
     */
    public void addBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        // Verifica che il booking sia associato a questo venue
        if (!booking.getVenue().equals(this)) {
            throw new IllegalArgumentException("Booking must be associated with this venue");
        }

        LocalDate gameDate = booking.getGameDate();
        bookingsByDate.computeIfAbsent(gameDate, k -> new ArrayList<>()).add(booking);
    }

    // ========== PUBLIC API - Capacity Management ==========
    // MODIFIED: Simplified - each booking = 1 person

    /**
     * Checks if there's available capacity for a new booking on the specified date.
     * MODIFIED: Each booking represents ONE person.
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
     * MODIFIED: Simply counts confirmed bookings (each = 1 person).
     *
     * @param gameDate The date to check
     * @return Number of available spots
     */
    public int getRemainingCapacity(LocalDate gameDate) {
        int confirmedBookings = countConfirmedBookings(gameDate);
        return maxCapacity - confirmedBookings;
    }

    // ========== PUBLIC API - Queries ==========

    /**
     * Gets all bookings for a specific date.
     */
    public List<Booking> getBookingsByDate(LocalDate date) {
        return new ArrayList<>(bookingsByDate.getOrDefault(date, new ArrayList<>()));
    }

    /**
     * Gets all bookings for this venue (all dates).
     */
    public List<Booking> getAllBookings() {
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

    // ========== PUBLIC API - Formatting/Display ==========

    /**
     * Gets venue's full address as formatted string.
     */
    public String getFullAddress() {
        return formatAddress(address, city);
    }

    // ========== PRIVATE - Implementation Details ==========

    /**
     * Counts confirmed bookings for a specific date.
     * MODIFIED: Each booking = 1 person, so we just count them.
     */
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

    // ========== GETTERS/SETTERS ==========

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VenueType getType() {
        return type;
    }

    public void setType(VenueType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public VenueManager getVenueManager() {
        return venueManager;
    }

    public void setVenueManager(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException("Venue manager cannot be null");
        }
        this.venueManager = venueManager;
    }

    public String getVenueManagerUsername() {
        return venueManager.getUsername();
    }

    // ========== UTILITY METHODS ==========

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