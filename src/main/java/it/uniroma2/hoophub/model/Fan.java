package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.BookingNotAllowedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Fan user.
 *
 * <p>Extends {@link User} with fan-specific attributes and booking management.
 * State modification restricted to public business operations (BCE principles).</p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> with inheritance from User.Builder.</p>
 *
 * <h3>Business Rules</h3>
 * <ul>
 *   <li>Fan must be at least 16 years old</li>
 *   <li>Maximum 1 booking per day</li>
 *   <li>Cannot book the same game twice</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class Fan extends User {

    private final TeamNBA favTeam;
    private final LocalDate birthday;
    /** Booking list initialized at construction, never reassigned. */
    private final List<Booking> bookingList;

    private Fan(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
        this.bookingList = builder.bookingList != null ?
                new ArrayList<>(builder.bookingList) : new ArrayList<>();
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Adds a booking with business rule validation.
     *
     * @param booking the booking to add
     * @throws BookingNotAllowedException if business rules violated
     * @throws IllegalArgumentException   if booking is null or not associated with this fan
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


    // ========================================================================
    // PUBLIC QUERIES
    // ========================================================================

    @Override
    public UserType getUserType() {
        return UserType.FAN;
    }

    // ========================================================================
    // PUBLIC GETTERS
    // ========================================================================

    public TeamNBA getFavTeam() {
        return favTeam;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for Fan with fluent API.
     */
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
    }

    // ========================================================================
    // VALIDATION HELPERS
    // ========================================================================

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

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

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