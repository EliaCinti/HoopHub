package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.TeamNBA;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain entity representing a booking for an NBA game viewing.
 *
 * <p>Encapsulates booking state and business logic following BCE principles.
 * All fields are final except status and notified flag, which are managed
 * through explicit business operations.</p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> for construction.</p>
 *
 * <h3>State Machine</h3>
 * <pre>
 * PENDING → CONFIRMED (via confirm())
 * PENDING → REJECTED (via reject())
 * PENDING → CANCELLED (via cancel())
 * CONFIRMED → CANCELLED (via cancel())
 * </pre>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class Booking {

    /** Immutable booking data. */
    private final int id;
    private final LocalDate gameDate;
    private final LocalTime gameTime;
    private final TeamNBA homeTeam;
    private final TeamNBA awayTeam;
    private final Venue venue;
    private final Fan fan;
    private final LocalDateTime createdAt;

    /** Mutable state managed by business operations. */
    private BookingStatus status;
    private boolean notified;

    private Booking(Builder builder) {
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
        this.createdAt = builder.createdAt;
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS (State Transitions)
    // ========================================================================

    /**
     * Confirms the booking (PENDING → CONFIRMED).
     *
     * <p>Resets notification flag so fan receives confirmation alert.</p>
     *
     * @throws IllegalStateException if booking is not PENDING
     */
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only confirm PENDING bookings");
        }
        this.status = BookingStatus.CONFIRMED;
        this.notified = false;
    }

    /**
     * Rejects the booking (PENDING → REJECTED).
     *
     * @throws IllegalStateException if booking is not PENDING
     */
    public void reject() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only reject PENDING bookings");
        }
        this.status = BookingStatus.REJECTED;
    }

    /**
     * Cancels the booking (PENDING|CONFIRMED → CANCELLED).
     *
     * @throws IllegalStateException if not cancellable or game date is past
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
     * Marks booking as notified (sets flag to true).
     */
    public void markAsNotified() {
        this.notified = true;
    }

    // ========================================================================
    // PUBLIC QUERIES
    // ========================================================================

    /**
     * Gets formatted game matchup.
     *
     * @return "Home Team vs Away Team"
     */
    public String getMatchup() {
        return homeTeam.getDisplayName() + " vs " + awayTeam.getDisplayName();
    }

    /**
     * Checks if a specific team is playing in this game.
     *
     * @param team the team to check
     * @return true if team is home or away
     */
    public boolean isFavoriteTeamPlaying(TeamNBA team) {
        if (team == null) {
            return false;
        }
        return homeTeam == team || awayTeam == team;
    }

    // ========================================================================
    // PUBLIC GETTERS
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for Booking with required parameters in constructor.
     */
    public static class Builder {
        private final int id;
        private final LocalDate gameDate;
        private final LocalTime gameTime;
        private final TeamNBA homeTeam;
        private final TeamNBA awayTeam;
        private final Venue venue;
        private final Fan fan;

        private BookingStatus status = BookingStatus.PENDING;
        private boolean notified = false;
        private LocalDateTime createdAt = LocalDateTime.now();

        /**
         * Creates builder with all required parameters.
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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }

    // ========================================================================
    // VALIDATION HELPERS
    // ========================================================================

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
        // Skip past date validation during sync to allow historical data
        if (!it.uniroma2.hoophub.sync.SyncContext.isSyncing() && gameDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book for past dates");
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Checks data equivalence (all fields).
     * Used by sync to detect conflicts.
     *
     * @param o object to compare
     * @return true if all data fields match
     */
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
                getStatus() == booking.getStatus() &&
                ((getCreatedAt() == null && booking.getCreatedAt() == null) ||
                        (getCreatedAt() != null && getCreatedAt().equals(booking.getCreatedAt())));
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Booking booking)
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
                ", createdAt=" + createdAt +
                '}';
    }
}