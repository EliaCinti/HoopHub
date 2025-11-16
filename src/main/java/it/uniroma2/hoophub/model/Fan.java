package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.exception.BookingNotAllowedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Fan entity.
 * State modification is restricted to public business operations.
 *
 * @author Elia Cinti
 */
public class Fan extends User {
    private TeamNBA favTeam;
    private LocalDate birthday;
    private final List<Booking> bookingList;

    /**
     * Private constructor for use by the Builder.
     * Sets initial state directly.
     */
    private Fan(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
        // The list is initialized directly. It can not be re-assigned later.
        this.bookingList = builder.bookingList != null ?
                new ArrayList<>(builder.bookingList) : new ArrayList<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Public business operation to update the Fan's profile.
     * This extends the base User operation with Fan-specific fields.
     *
     * @param newFullName   The user's new full name.
     * @param newGender     The user's new gender.
     * @param newFavTeam    The fan's new favorite NBA team.
     * @param newBirthday   The fan's new birthday.
     * @throws IllegalArgumentException if validation for new data fails.
     */
    public void updateFanProfile(String newFullName, String newGender, TeamNBA newFavTeam, LocalDate newBirthday) {
        // 1. Call the parent's business operation
        super.updateProfileDetails(newFullName, newGender);

        // 2. Validate Fan-specific data
        validateFavTeam(newFavTeam);
        validateBirthday(newBirthday);

        // 3. Mutate Fan-specific state using private setters
        this.setFavTeam(newFavTeam);
        this.setBirthday(newBirthday);
    }

    /**
     * Adds a booking with validation.
     * Called by: VenueManager.confirmBooking(), BookingController
     *
     * @param booking The booking to add.
     * @throws BookingNotAllowedException if booking rules are violated.
     */
    public void addBooking(Booking booking) throws BookingNotAllowedException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if (!booking.getFan().equals(this)) {
            throw new BookingNotAllowedException("Booking must be associated with this fan");
        }
        if (hasBookingForGame(booking.getGameDate(), booking.getHomeTeam(), booking.getAwayTeam())) {
            throw new BookingNotAllowedException("You already have a booking for this game");
        }
        if (countBookingsOnDate(booking.getGameDate()) >= 1) {
            throw new BookingNotAllowedException("Cannot book more than 1 event on the same day");
        }
        bookingList.add(booking);
    }

    /**
     * Cancels a booking by ID.
     * Called by: FanController when user clicks "Cancel booking"
     *
     * @param bookingId The ID of the booking to cancel.
     * @return true if cancellation was successful, false otherwise.
     */
    public boolean cancelBooking(int bookingId) {
        Booking booking = findBookingById(bookingId);
        if (booking == null || isPastBooking(booking)) {
            return false;
        }
        booking.cancel();
        return true;
    }

    /**
     * Marks all unread confirmed bookings as notified.
     */
    public void markAllBookingsAsNotified() {
        bookingList.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && !b.isNotified())
                .forEach(Booking::markAsNotified);
    }

    // ========================================================================
    // PUBLIC QUERIES (Read-Only Access & Business Questions)
    // ========================================================================

    @Override
    public UserType getUserType() {
        return UserType.FAN;
    }

    /**
     * Gets all upcoming bookings (future dates only).
     */
    public List<Booking> getUpcomingBookings() {
        return filterBookingsByDateCondition(b -> b.getGameDate().isAfter(LocalDate.now()));
    }

    /**
     * Gets all past bookings (for history view).
     */
    public List<Booking> getPastBookings() {
        return filterBookingsByDateCondition(b -> b.getGameDate().isBefore(LocalDate.now()));
    }

    /**
     * Gets bookings where fan's favorite team is playing.
     */
    public List<Booking> getFavoriteTeamBookings() {
        return bookingList.stream()
                .filter(b -> b.isFavoriteTeamPlaying(favTeam))
                .collect(Collectors.toList());
    }

    /**
     * Checks if fan has unnotified confirmed bookings.
     */
    public boolean hasUnnotifiedBookings() {
        return bookingList.stream()
                .anyMatch(b -> !b.isNotified() && b.getStatus() == BookingStatus.CONFIRMED);
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
    // ========================================================================

    public TeamNBA getFavTeam() {
        return favTeam;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    /**
     * Returns an UNMODIFIABLE view of the booking list to prevent external modification.
     */
    public List<Booking> getBookingList() {
        return Collections.unmodifiableList(bookingList);
    }

    // ========================================================================
    // PRIVATE SETTERS (Internal State Mutation)
    // ========================================================================

    /**
     * Private setter for favorite team.
     * Only called by updateFanProfile().
     */
    private void setFavTeam(TeamNBA favTeam) {
        this.favTeam = favTeam;
    }

    /**
     * Private setter for birthday.
     * Only called by updateFanProfile().
     */
    private void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    // ========================================================================
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

    public static class Builder extends User.Builder<Builder> {
        private TeamNBA favTeam;
        private LocalDate birthday;
        private List<Booking> bookingList;

        public Builder favTeam(TeamNBA favTeam) {
            this.favTeam = favTeam;
            return this;
        }

        public Builder birthday(LocalDate birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder bookingList(List<Booking> bookingList) {
            this.bookingList = bookingList;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Fan build() {
            validate();
            return new Fan(this);
        }

        @Override
        protected void validate() {
            super.validate();
            validateFavTeam(favTeam);
            validateBirthday(birthday);
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION & HELPER METHODS (Internal Logic)
    // ========================================================================

    private static void validateFavTeam(TeamNBA favTeam) {
        if (favTeam == null) {
            throw new IllegalArgumentException("Favorite team cannot be null");
        }
    }

    private static void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday cannot be null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future");
        }
        if (birthday.isAfter(LocalDate.now().minusYears(16))) {
            throw new IllegalArgumentException("Fan must be at least 16 years old");
        }
    }

    private boolean hasBookingForGame(LocalDate date, TeamNBA homeTeam, TeamNBA awayTeam) {
        return bookingList.stream()
                .anyMatch(b -> b.getGameDate().equals(date) &&
                        b.getHomeTeam() == homeTeam &&
                        b.getAwayTeam() == awayTeam);
    }

    private int countBookingsOnDate(LocalDate date) {
        return (int) bookingList.stream()
                .filter(b -> b.getGameDate().equals(date))
                .count();
    }

    private Booking findBookingById(int bookingId) {
        return bookingList.stream()
                .filter(b -> b.getId() == bookingId)
                .findFirst()
                .orElse(null);
    }

    private boolean isPastBooking(Booking booking) {
        return booking.getGameDate().isBefore(LocalDate.now());
    }

    private List<Booking> filterBookingsByDateCondition(java.util.function.Predicate<Booking> condition) {
        return bookingList.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // UTILITY METHODS (equals, hashCode, toString, isDataEquivalent)
    // ========================================================================

    public boolean isDataEquivalent(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fan fan = (Fan) o;
        return getUsername().equals(fan.getUsername()) &&
                getFullName().equals(fan.getFullName()) &&
                getGender().equals(fan.getGender()) &&
                getFavTeam().equals(fan.getFavTeam()) &&
                getBirthday().equals(fan.getBirthday());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fan fan)
            return fan.getUsername().equals(this.getUsername());
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Fan{" +
                "username='" + getUsername() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", favTeam='" + favTeam + '\'' +
                ", bookingsCount=" + bookingList.size() +
                '}';
    }
}
