package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.exception.BookingNotAllowedException;
import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.UserType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a fan with business logic for managing bookings and preferences.
 */
public class Fan extends User {
    private String favTeam;
    private LocalDate birthday;
    private List<Booking> bookingList;

    private Fan(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
        this.bookingList = builder.bookingList != null ?
                new ArrayList<>(builder.bookingList) : new ArrayList<>();
    }

    public static class Builder extends User.Builder<Builder> {
        private String favTeam;
        private LocalDate birthday;
        private List<Booking> bookingList;

        public Builder favTeam(String favTeam) {
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

            if (favTeam == null || favTeam.trim().isEmpty()) {
                throw new IllegalArgumentException("Favorite team cannot be null or empty");
            }

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
    }

    // ========== PUBLIC API - Core Operations ==========

    /**
     * Adds a booking with validation.
     * Called by: VenueManager.confirmBooking(), BookingController
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
     */
    public boolean cancelBooking(int bookingId) {
        Booking booking = findBookingById(bookingId);

        if (booking == null || isPastBooking(booking)) {
            return false;
        }

        booking.cancel();
        return true;
    }

    // ========== PUBLIC API - Queries ==========

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

    // ========== PUBLIC API - Notifications ==========

    /**
     * Checks if fan has unnotified confirmed bookings.
     */
    public boolean hasUnnotifiedBookings() {
        return bookingList.stream()
                .anyMatch(b -> !b.isNotified() && b.getStatus() == BookingStatus.CONFIRMED);
    }

    /**
     * Marks all bookings as notified.
     */
    public void markAllBookingsAsNotified() {
        bookingList.forEach(Booking::markAsNotified);
    }

    // ========== PRIVATE - Implementation Details ==========

    private boolean hasBookingForGame(LocalDate date, String homeTeam, String awayTeam) {
        return bookingList.stream()
                .anyMatch(b -> b.getGameDate().equals(date) &&
                        b.getHomeTeam().equals(homeTeam) &&
                        b.getAwayTeam().equals(awayTeam));
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

    // ========== GETTERS/SETTERS ==========

    public String getFavTeam() {
        return favTeam;
    }

    public void setFavTeam(String favTeam) {
        this.favTeam = favTeam;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    @Override
    public UserType getUserType() {
        return UserType.FAN;
    }

    /**
     * Returns an UNMODIFIABLE view of the booking list.
     */
    public List<Booking> getBookingList() {
        return Collections.unmodifiableList(bookingList);
    }

    public void setBookingList(List<Booking> bookingList) {
        this.bookingList = bookingList != null ? new ArrayList<>(bookingList) : new ArrayList<>();
    }

    // ========== UTILITY METHODS ==========

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