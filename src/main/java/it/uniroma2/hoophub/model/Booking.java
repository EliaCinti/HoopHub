package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a booking with business logic for status management and validation.
 * Uses Builder pattern to avoid constructor with too many parameters (SonarQube S107).
 */
public class Booking {
    private final int id;
    private final LocalDate gameDate;
    private final LocalTime gameTime;
    private final String homeTeam;
    private final String awayTeam;
    private final Venue venue;
    private final Fan fan;
    private final int seatsRequested;

    private BookingStatus status;
    private boolean notified;

    private Booking(Builder builder) {
        validateBookingData(builder.id, builder.gameDate, builder.gameTime,
                builder.homeTeam, builder.awayTeam, builder.seatsRequested);

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
        this.seatsRequested = builder.seatsRequested;
        this.status = builder.status;
        this.notified = builder.notified;
    }

    /**
     * Builder for Booking with fluent API.
     * Solves SonarQube S107 (too many constructor parameters).
     */
    public static class Builder {
        // Required parameters
        private final int id;
        private final LocalDate gameDate;
        private final LocalTime gameTime;
        private final String homeTeam;
        private final String awayTeam;
        private final Venue venue;
        private final Fan fan;
        private final int seatsRequested;

        // Optional parameters with defaults
        private BookingStatus status = BookingStatus.PENDING;
        private boolean notified = false;

        /**
         * Constructor with required parameters only.
         * Optional parameters have sensible defaults.
         */
        public Builder(int id, LocalDate gameDate, LocalTime gameTime,
                       String homeTeam, String awayTeam, Venue venue,
                       Fan fan, int seatsRequested) {
            this.id = id;
            this.gameDate = gameDate;
            this.gameTime = gameTime;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.venue = venue;
            this.fan = fan;
            this.seatsRequested = seatsRequested;
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

    // ========== PUBLIC API - State Transitions ==========

    /**
     * Confirms the booking (PENDING → CONFIRMED).
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
     */
    public void reject() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only reject PENDING bookings");
        }
        this.status = BookingStatus.REJECTED;
    }

    /**
     * Cancels the booking (PENDING/CONFIRMED → CANCELLED).
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
     */
    public void markAsNotified() {
        this.notified = true;
    }

    // ========== PUBLIC API - Display/Formatting ==========

    /**
     * Gets the game matchup as formatted string.
     */
    public String getMatchup() {
        return homeTeam + " vs " + awayTeam;
    }

    /**
     * Checks if a specific team is playing in this game.
     */
    public boolean isFavoriteTeamPlaying(String teamName) {
        return homeTeam.equalsIgnoreCase(teamName) || awayTeam.equalsIgnoreCase(teamName);
    }

    // ========== PRIVATE - Validation ==========

    private void validateBookingData(int id, LocalDate gameDate, LocalTime gameTime,
                                     String homeTeam, String awayTeam, int seatsRequested) {
        if (id < 0) {
            throw new IllegalArgumentException("Booking ID cannot be negative");
        }
        if (gameDate == null) {
            throw new IllegalArgumentException("Game date cannot be null");
        }
        if (gameTime == null) {
            throw new IllegalArgumentException("Game time cannot be null");
        }
        if (homeTeam == null || homeTeam.trim().isEmpty()) {
            throw new IllegalArgumentException("Home team cannot be null or empty");
        }
        if (awayTeam == null || awayTeam.trim().isEmpty()) {
            throw new IllegalArgumentException("Away team cannot be null or empty");
        }
        if (homeTeam.equalsIgnoreCase(awayTeam)) {
            throw new IllegalArgumentException("Home team and away team cannot be the same");
        }
        if (seatsRequested <= 0) {
            throw new IllegalArgumentException("Seats requested must be greater than 0");
        }
        if (seatsRequested > 50) {
            throw new IllegalArgumentException("Cannot request more than 50 seats");
        }
        if (gameDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book for past dates");
        }
    }

    // ========== GETTERS ==========

    public int getId() {
        return id;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public LocalTime getGameTime() {
        return gameTime;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
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

    public int getSeatsRequested() {
        return seatsRequested;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean isNotified() {
        return notified;
    }

    // ========== NO SETTERS per attributi final ==========
    // Gli attributi final non hanno setter, solo status e notified cambiano via metodi di business logic

    // ========== UTILITY METHODS ==========

    public boolean isDataEquivalent(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return getId() == booking.getId() &&
                getVenueId() == booking.getVenueId() &&
                getSeatsRequested() == booking.getSeatsRequested() &&
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