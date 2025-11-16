package it.uniroma2.hoophub.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a Booking entity.
 * This class encapsulates all state and business logic for a booking.
 * It perfectly follows BCE principles: state is private and can ONLY be modified
 * through explicit public business operations (confirm, reject, cancel).
 * It has NO public setters.
 *
 * @author Elia Cinti
 */
public class Booking {
    // All fields are final except for status and notified,
    // which are managed by internal business logic.
    private final int id;
    private final LocalDate gameDate;
    private final LocalTime gameTime;
    private final TeamNBA homeTeam;
    private final TeamNBA awayTeam;
    private final Venue venue;
    private final Fan fan;

    private BookingStatus status;
    private boolean notified;

    /**
     * Private constructor for use by the Builder.
     * Validates all incoming data.
     */
    private Booking(Builder builder) {
        // Validation is centralized
        validateBookingData(builder.id, builder.gameDate, builder.gameTime,
                builder.homeTeam, builder.awayTeam);

        if (builder.venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }
        if (builder.fan == null) {
            throw new IllegalArgumentException("Fan cannot be null");
        }

        this.id = builder.id;
        this.gameDate = builder.gameDate;
        this.gameTime = builder.gameTime;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.venue = builder.venue;
        this.fan = builder.fan;
        this.status = builder.status;
        this.notified = builder.notified;
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS (State Transitions)
    // ========================================================================

    /**
     * Confirms the booking.
     * Business Rule: Can only confirm PENDING bookings.
     * This operation also resets the notification flag.
     *
     * @throws IllegalStateException if booking is not PENDING.
     */
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only confirm PENDING bookings");
        }
        this.status = BookingStatus.CONFIRMED;
        this.notified = false; // Reset notification status
    }

    /**
     * Rejects the booking.
     * Business Rule: Can only reject PENDING bookings.
     *
     * @throws IllegalStateException if booking is not PENDING.
     */
    public void reject() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only reject PENDING bookings");
        }
        this.status = BookingStatus.REJECTED;
    }

    /**
     * Cancels the booking.
     * Business Rule: Can only cancel CONFIRMED or PENDING bookings.
     * Business Rule: Cannot cancel bookings for past dates.
     *
     * @throws IllegalStateException if booking is not cancellable or is in the past.
     */
    public void cancel() {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Can only cancel CONFIRMED or PENDING bookings");
        }
        if (gameDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel bookings for past dates");
        }
        this.status = BookingStatus.CANCELLED;
    }

    /**
     * Marks booking as notified.
     * This is the only way to change the 'notified' flag.
     */
    public void markAsNotified() {
        this.notified = true;
    }

    // ========================================================================
    // PUBLIC QUERIES (Read-Only Access & Business Questions)
    // ========================================================================

    /**
     * Gets the game matchup as formatted string.
     *
     * @return A string like "Home Team vs Away Team".
     */
    public String getMatchup() {
        return homeTeam.getDisplayName() + " vs " + awayTeam.getDisplayName();
    }

    /**
     * Checks if a specific team is playing in this game.
     *
     * @param team The NBA team to check.
     * @return true if the team is playing, false otherwise.
     */
    public boolean isFavoriteTeamPlaying(TeamNBA team) {
        if (team == null) {
            return false;
        }
        return homeTeam == team || awayTeam == team;
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
    // ========================================================================

    public int getId() {
        return id;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public LocalTime getGameTime() {
        return gameTime;
    }

    public TeamNBA getHomeTeam() {
        return homeTeam;
    }

    public TeamNBA getAwayTeam() {
        return awayTeam;
    }

    public Venue getVenue() {
        return venue;
    }

    public Fan getFan() {
        return fan;
    }

    public int getVenueId() {
        return venue.getId();
    }

    public String getFanUsername() {
        return fan.getUsername();
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean isNotified() {
        return notified;
    }

    // ========================================================================
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

    /**
     * Builder for Booking with fluent API.
     */
    public static class Builder {
        // Required parameters
        private final int id;
        private final LocalDate gameDate;
        private final LocalTime gameTime;
        private final TeamNBA homeTeam;
        private final TeamNBA awayTeam;
        private final Venue venue;
        private final Fan fan;

        // Optional parameters with defaults
        private BookingStatus status = BookingStatus.PENDING;
        private boolean notified = false;

        /**
         * Constructor with required parameters only.
         */
        public Builder(int id, LocalDate gameDate, LocalTime gameTime,
                       TeamNBA homeTeam, TeamNBA awayTeam, Venue venue, Fan fan) {
            this.id = id;
            this.gameDate = gameDate;
            this.gameTime = gameTime;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.venue = venue;
            this.fan = fan;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder notified(boolean notified) {
            this.notified = notified;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION HELPERS (Internal Logic)
    // ========================================================================

    /**
     * Centralized validation logic for booking data.
     */
    private void validateBookingData(int id, LocalDate gameDate, LocalTime gameTime,
                                     TeamNBA homeTeam, TeamNBA awayTeam) {
        if (id < 0) {
            throw new IllegalArgumentException("Booking ID cannot be negative");
        }
        if (gameDate == null) {
            throw new IllegalArgumentException("Game date cannot be null");
        }
        if (gameTime == null) {
            throw new IllegalArgumentException("Game time cannot be null");
        }
        if (homeTeam == null) {
            throw new IllegalArgumentException("Home team cannot be null");
        }
        if (awayTeam == null) {
            throw new IllegalArgumentException("Away team cannot be null");
        }
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException("Home team and away team cannot be the same");
        }
        // Skip past date validation during initial sync (SyncContext prevents observer loops already)
        if (!it.uniroma2.hoophub.sync.SyncContext.isSyncing() && gameDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book for past dates");
        }
    }

    // ========================================================================
    // UTILITY METHODS (equals, hashCode, toString, isDataEquivalent)
    // ========================================================================

    public boolean isDataEquivalent(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return getId() == booking.getId() &&
                getVenueId() == booking.getVenueId() &&
                isNotified() == booking.isNotified() &&
                getGameDate().equals(booking.getGameDate()) &&
                getGameTime().equals(booking.getGameTime()) &&
                getHomeTeam().equals(booking.getHomeTeam()) &&
                getAwayTeam().equals(booking.getAwayTeam()) &&
                getFanUsername().equals(booking.getFanUsername()) &&
                getStatus() == booking.getStatus();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Booking booking)
            // Equality is based on the immutable primary key (id)
            return this.id == booking.getId();
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", matchup='" + getMatchup() + '\'' +
                ", date=" + gameDate +
                ", venue='" + venue.getName() + '\'' +
                ", fan='" + fan.getUsername() + '\'' +
                ", status=" + status +
                '}';
    }
}