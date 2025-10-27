package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.exception.BookingNotAllowedException;
import it.uniroma2.hoophub.exception.VenueCapacityExceededException;
import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.UserType;
import it.uniroma2.hoophub.utilities.VenueType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a venue manager with business logic for managing venues and bookings.
 */
public class VenueManager extends User {
    private String companyName;
    private String phoneNumber;
    private List<Venue> managedVenues = new ArrayList<>();

    private VenueManager(Builder builder) {
        super(builder);
        this.companyName = builder.companyName;
        this.phoneNumber = builder.phoneNumber;
        this.managedVenues = builder.managedVenues != null ?
                builder.managedVenues : new ArrayList<>();
    }

    public static class Builder extends User.Builder<Builder> {
        private String companyName;
        private String phoneNumber;
        private List<Venue> managedVenues;

        public Builder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder managedVenues(List<Venue> managedVenues) {
            this.managedVenues = managedVenues;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public VenueManager build() {
            validate();
            return new VenueManager(this);
        }

        @Override
        protected void validate() {
            super.validate();

            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Company name cannot be null or empty");
            }

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }

            if (!phoneNumber.matches("^\\+?[0-9\\s-]{10,20}$")) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }
    }

    // ========== PUBLIC API - Core Operations ==========

    /**
     * Confirms a booking with full validation.
     * Called by: VenueManagerController when manager approves a booking request
     */
    public void confirmBooking(Booking booking, Fan fan, Venue venue)
            throws BookingNotAllowedException, VenueCapacityExceededException {

        validateVenueOwnership(venue);
        validateBookingDate(booking);

        if (!venue.hasAvailableCapacity(booking.getGameDate(), booking.getSeatsRequested())) {
            throw new VenueCapacityExceededException(
                    venue.getName(),
                    booking.getSeatsRequested(),
                    venue.getRemainingCapacity(booking.getGameDate())
            );
        }

        fan.addBooking(booking);
        venue.addBooking(booking);
        booking.confirm();
    }

    /**
     * Rejects a booking request.
     * Called by: VenueManagerController when manager rejects a booking
     */
    public void rejectBooking(Booking booking, Venue venue) throws BookingNotAllowedException {
        validateVenueOwnership(venue);
        booking.reject();
    }

    /**
     * Adds a venue to this manager's portfolio.
     * Called by: VenueManagerController, VenueCreationService
     */
    public void addVenue(Venue venue) throws IllegalArgumentException {
        if (!isVenueAssignedToManager(venue)) {
            throw new IllegalArgumentException("Venue must be assigned to this manager");
        }

        if (!managedVenues.contains(venue)) {
            managedVenues.add(venue);
        }
    }

    // ========== PUBLIC API - Queries (Used by Controllers/UI) ==========

    /**
     * Gets all pending bookings across all managed venues.
     * Called by: VenueManagerController for "Pending Requests" dashboard
     */
    public List<Booking> getAllPendingBookings() {
        return collectBookingsByStatus();
    }

    /**
     * Gets venues by type (for filtered views).
     * Called by: VenueManagerController for type-based filtering
     * NOTE: If filtering is done in Controller, make this PRIVATE.
     */
    public List<Venue> getVenuesByType(VenueType type) {
        return managedVenues.stream()
                .filter(v -> v.getType() == type)
                .collect(Collectors.toList());
    }

    // ========== PRIVATE - Implementation Details (Information Hiding) ==========

    /**
     * Validates that the venue is managed by this manager.
     * PRIVATE - internal validation logic
     */
    private void validateVenueOwnership(Venue venue) throws BookingNotAllowedException {
        if (!managedVenues.contains(venue)) {
            throw new BookingNotAllowedException("This venue is not managed by this manager");
        }
    }

    /**
     * Validates that booking date is not in the past.
     * PRIVATE - internal validation logic
     */
    private void validateBookingDate(Booking booking) throws BookingNotAllowedException {
        if (booking.getGameDate().isBefore(LocalDate.now())) {
            throw new BookingNotAllowedException("Cannot confirm bookings for past dates");
        }
    }

    /**
     * Checks if venue is assigned to this manager.
     * PRIVATE - helper for addVenue validation
     */
    private boolean isVenueAssignedToManager(Venue venue) {
        return venue.getVenueManager().equals(this);
    }

    /**
     * Collects all bookings with a specific status across all venues.
     * PRIVATE - helper for getAllPendingBookings and potential future methods
     */
    private List<Booking> collectBookingsByStatus() {
        return managedVenues.stream()
                .flatMap(venue -> venue.getAllBookings().stream())
                .filter(booking -> booking.getStatus() == BookingStatus.PENDING)
                .collect(Collectors.toList());
    }

    // ========== GETTERS/SETTERS ==========

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public UserType getUserType() {
        return UserType.VENUE_MANAGER;
    }

    /**
     * Returns an UNMODIFIABLE view of managed venues.
     * Prevents external modification of internal state.
     */
    public List<Venue> getManagedVenues() {
        return Collections.unmodifiableList(managedVenues);
    }

    public void setManagedVenues(List<Venue> managedVenues) {
        this.managedVenues = managedVenues != null ?
                new ArrayList<>(managedVenues) : new ArrayList<>();
    }

    // ========== UTILITY METHODS ==========

    public boolean isDataEquivalent(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VenueManager that = (VenueManager) o;
        return getUsername().equals(that.getUsername()) &&
                getFullName().equals(that.getFullName()) &&
                getGender().equals(that.getGender()) &&
                getCompanyName().equals(that.getCompanyName()) &&
                getPhoneNumber().equals(that.getPhoneNumber());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VenueManager manager)
            return manager.getUsername().equals(this.getUsername());
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}