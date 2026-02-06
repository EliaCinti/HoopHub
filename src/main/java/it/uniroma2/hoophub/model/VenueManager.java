package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.BookingNotAllowedException;
import it.uniroma2.hoophub.exception.VenueCapacityExceededException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain entity representing a Venue Manager user.
 *
 * <p>Extends {@link User} with manager-specific attributes and venue/booking
 * management capabilities. State modification restricted to public business
 * operations (BCE principles).</p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> with inheritance from User.Builder.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Manage portfolio of venues</li>
 *   <li>Confirm/reject booking requests</li>
 *   <li>Coordinate state changes across Booking, Fan, and Venue entities</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManager extends User {

    private final String companyName;
    private final String phoneNumber;
    /** Venue list initialized at construction, never reassigned. */
    private final List<Venue> managedVenues;

    private VenueManager(Builder builder) {
        super(builder);
        this.companyName = builder.companyName;
        this.phoneNumber = builder.phoneNumber;
        this.managedVenues = builder.managedVenues != null ?
                new ArrayList<>(builder.managedVenues) : new ArrayList<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Confirms a booking, coordinating state changes across entities.
     *
     * <p>Performs validation, capacity check, then updates:
     * <ol>
     *   <li>Fan's booking list (via fan.addBooking)</li>
     *   <li>Venue's booking tracking (via venue.addBooking)</li>
     *   <li>Booking status (via booking.confirm)</li>
     * </ol>
     * </p>
     *
     * @param booking the booking to confirm
     * @param fan     the fan requesting the booking
     * @param venue   the venue for the booking
     * @throws BookingNotAllowedException     if venue not managed or date is past
     * @throws VenueCapacityExceededException if the venue is at capacity
     */
    public void confirmBooking(Booking booking, Fan fan, Venue venue)
            throws BookingNotAllowedException {

        validateVenueOwnership(venue);
        validateBookingDate(booking);

        if (!venue.hasAvailableCapacity(booking.getGameDate())) {
            throw new VenueCapacityExceededException(
                    venue.getName(),
                    1,
                    venue.getRemainingCapacity(booking.getGameDate())
            );
        }

        fan.addBooking(booking);
        venue.addBooking(booking);
        booking.confirm();
    }

    /**
     * Adds a venue to this manager's portfolio.
     *
     * @param venue the venue to add
     * @throws IllegalArgumentException if venue not assigned to this manager
     */
    public void addVenue(Venue venue) throws IllegalArgumentException {
        if (!isVenueAssignedToManager(venue)) {
            throw new IllegalArgumentException("Venue must be assigned to this manager");
        }
        if (!managedVenues.contains(venue)) {
            managedVenues.add(venue);
        }
    }

    // ========================================================================
    // PUBLIC QUERIES
    // ========================================================================

    @Override
    public UserType getUserType() {
        return UserType.VENUE_MANAGER;
    }

    // ========================================================================
    // PUBLIC GETTERS
    // ========================================================================

    public String getCompanyName() {
        return companyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns unmodifiable view of managed venues.
     *
     * @return unmodifiable venue list
     */
    public List<Venue> getManagedVenues() {
        return Collections.unmodifiableList(managedVenues);
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for VenueManager with fluent API.
     */
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
            validateCompanyName(companyName);
            validatePhoneNumber(phoneNumber);
        }

        private static void validateCompanyName(String companyName) {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Company name cannot be null or empty");
            }
        }

        private static void validatePhoneNumber(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }
            if (!phoneNumber.matches("^\\+?[0-9\\s-]{10,20}$")) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }
    }

    // ========================================================================
    // VALIDATION HELPERS
    // ========================================================================

    private void validateVenueOwnership(Venue venue) throws BookingNotAllowedException {
        if (!managedVenues.contains(venue)) {
            throw new BookingNotAllowedException("This venue is not managed by this manager");
        }
    }

    private void validateBookingDate(Booking booking) throws BookingNotAllowedException {
        if (booking.getGameDate().isBefore(LocalDate.now())) {
            throw new BookingNotAllowedException("Cannot confirm bookings for past dates");
        }
    }

    private boolean isVenueAssignedToManager(Venue venue) {
        return venue.getVenueManager().equals(this);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

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

    @Override
    public String toString() {
        return "VenueManager{" +
                "username='" + getUsername() + '\'' +
                ", companyName='" + companyName + '\'' +
                ", managedVenuesCount=" + managedVenues.size() +
                '}';
    }
}