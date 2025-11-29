package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.exception.BookingNotAllowedException;
import it.uniroma2.hoophub.exception.VenueCapacityExceededException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a VenueManager entity.
 * This class encapsulates a Manager's state and business logic for managing venues
 * and handling booking requests.
 * State modification is restricted to public business operations, as per BCE principles.
 *
 * @author Elia Cinti
 */
public class VenueManager extends User {
    private String companyName;
    private String phoneNumber;
    private final List<Venue> managedVenues; // List is initialized at construction and never re-assigned

    /**
     * Private constructor for use by the Builder.
     * Sets initial state directly.
     */
    private VenueManager(Builder builder) {
        super(builder);
        this.companyName = builder.companyName;
        this.phoneNumber = builder.phoneNumber;
        // The list is initialized directly. It can not be re-assigned later.
        this.managedVenues = builder.managedVenues != null ?
                new ArrayList<>(builder.managedVenues) : new ArrayList<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Public business operation to update the Manager's profile.
     * This extends the base User operation with Manager-specific fields.
     * This is the ONLY way to modify companyName and phoneNumber after creation.
     *
     * @param newFullName     The user's new full name.
     * @param newGender       The user's new gender.
     * @param newCompanyName  The manager's new company name.
     * @param newPhoneNumber  The manager's new phone number.
     * @throws IllegalArgumentException if validation for new data fails.
     */
    public void updateManagerProfile(String newFullName, String newGender, String newCompanyName, String newPhoneNumber) {
        // 1. Call the parent's business operation to update common fields
        super.updateProfileDetails(newFullName, newGender);

        // 2. Validate Manager-specific data
        validateCompanyName(newCompanyName);
        validatePhoneNumber(newPhoneNumber);

        // 3. Mutate Manager-specific state using private setters
        this.setCompanyName(newCompanyName);
        this.setPhoneNumber(newPhoneNumber);
    }

    /**
     * Confirms a booking, performing all business logic checks.
     * This operation coordinates state changes across multiple entities
     * (Booking, Fan, Venue).
     *
     * @param booking The booking to confirm.
     * @param fan     The fan associated with the booking.
     * @param venue   The venue for the booking.
     * @throws BookingNotAllowedException       if venue is not managed or booking is in the past.
     * @throws VenueCapacityExceededException   if the venue is full.
     */
    public void confirmBooking(Booking booking, Fan fan, Venue venue)
            throws BookingNotAllowedException {

        // 1. Validate business rules
        validateVenueOwnership(venue);
        validateBookingDate(booking);

        // 2. Check venue capacity
        if (!venue.hasAvailableCapacity(booking.getGameDate())) {
            throw new VenueCapacityExceededException(
                    venue.getName(),
                    1, // Each booking is for 1 person
                    venue.getRemainingCapacity(booking.getGameDate())
            );
        }

        // 3. Coordinate state changes in other entities
        fan.addBooking(booking);
        venue.addBooking(booking);
        booking.confirm();
    }

    /**
     * Rejects a booking request.
     * Delegates the state change to the Booking entity.
     *
     * @param booking The booking to reject.
     * @param venue   The venue associated with the booking.
     * @throws BookingNotAllowedException if the venue is not managed by this manager.
     */
    public void rejectBooking(Booking booking, Venue venue) throws BookingNotAllowedException {
        validateVenueOwnership(venue);
        booking.reject();
    }

    /**
     * Adds a venue to this manager's portfolio.
     * This is the ONLY way to add to the managedVenues list after construction.
     *
     * @param venue The venue to add.
     * @throws IllegalArgumentException if the venue is not assigned to this manager.
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
    // PUBLIC QUERIES (Read-Only Access & Business Questions)
    // ========================================================================

    @Override
    public UserType getUserType() {
        return UserType.VENUE_MANAGER;
    }

    /**
     * Gets all pending bookings across all managed venues.
     * Used by: VenueManagerController for "Pending Requests" dashboard
     */
    public List<Booking> getAllPendingBookings() {
        return collectBookingsByStatus(BookingStatus.PENDING);
    }

    /**
     * Gets venues by type (for filtered views).
     * Called by: VenueManagerController for type-based filtering
     */
    public List<Venue> getVenuesByType(VenueType type) {
        return managedVenues.stream()
                .filter(v -> v.getType() == type)
                .toList();
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
    // ========================================================================

    public String getCompanyName() {
        return companyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns an UNMODIFIABLE view of managed venues.
     * Prevents external modification of internal state.
     */
    public List<Venue> getManagedVenues() {
        return Collections.unmodifiableList(managedVenues);
    }

    // ========================================================================
    // PRIVATE SETTERS (Internal State Mutation)
    // ========================================================================

    /**
     * Private setter for company name.
     * Only called by updateManagerProfile().
     */
    private void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * Private setter for phone number.
     * Only called by updateManagerProfile().
     */
    private void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // ========================================================================
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

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

        /**
         * Validation logic called by the Builder at construction time.
         */
        @Override
        protected void validate() {
            super.validate();
            // Use static validation helpers
            validateCompanyName(companyName);
            validatePhoneNumber(phoneNumber);
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION & HELPER METHODS (Internal Logic)
    // ========================================================================

    /**
     * Validates company name. Static to be used by Builder and updateManagerProfile.
     */
    private static void validateCompanyName(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be null or empty");
        }
    }

    /**
     * Validates phone number. Static to be used by Builder and updateManagerProfile.
     */
    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        if (!phoneNumber.matches("^\\+?[0-9\\s-]{10,20}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    /**
     * Validates that the venue is managed by this manager.
     */
    private void validateVenueOwnership(Venue venue) throws BookingNotAllowedException {
        if (!managedVenues.contains(venue)) {
            throw new BookingNotAllowedException("This venue is not managed by this manager");
        }
    }

    /**
     * Validates that booking date is not in the past.
     */
    private void validateBookingDate(Booking booking) throws BookingNotAllowedException {
        if (booking.getGameDate().isBefore(LocalDate.now())) {
            throw new BookingNotAllowedException("Cannot confirm bookings for past dates");
        }
    }

    /**
     * Checks if venue is assigned to this manager.
     */
    private boolean isVenueAssignedToManager(Venue venue) {
        return venue.getVenueManager().equals(this);
    }

    /**
     * Collects all bookings with a specific status across all venues.
     */
    private List<Booking> collectBookingsByStatus(BookingStatus status) {
        return managedVenues.stream()
                .flatMap(venue -> venue.getAllBookings().stream())
                .filter(booking -> booking.getStatus() == status)
                .toList();
    }

    // ========================================================================
    // UTILITY METHODS (equals, hashCode, isDataEquivalent)
    // ========================================================================

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

    @Override
    public String toString() {
        return "VenueManager{" +
                "username='" + getUsername() + '\'' +
                ", companyName='" + companyName + '\'' +
                ", managedVenuesCount=" + managedVenues.size() +
                '}';
    }
}