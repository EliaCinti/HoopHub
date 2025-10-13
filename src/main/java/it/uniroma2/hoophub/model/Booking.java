package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a booking with business logic for status management and validation.
 */
public class Booking {
    private int id;
    private LocalDate gameDate;
    private LocalTime gameTime;
    private String homeTeam;
    private String awayTeam;
    private int venueId;
    private String fanUsername;
    private int seatsRequested;
    private BookingStatus status;
    private boolean notified;

    public Booking(int id, LocalDate gameDate, LocalTime gameTime, String homeTeam,
                   String awayTeam, int venueId, String fanUsername, int seatsRequested,
                   BookingStatus status, boolean notified) {
        validateBookingData(id, gameDate, gameTime, homeTeam, awayTeam, seatsRequested);
        this.id = id;
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.venueId = venueId;
        this.fanUsername = fanUsername;
        this.seatsRequested = seatsRequested;
        this.status = status;
        this.notified = notified;
    }

    public Booking(int id, LocalDate gameDate, LocalTime gameTime, String homeTeam,
                   String awayTeam, int venueId, String fanUsername, int seatsRequested) {
        this(id, gameDate, gameTime, homeTeam, awayTeam, venueId, fanUsername,
                seatsRequested, BookingStatus.PENDING, false);
    }

    // ========== PUBLIC API - State Transitions ==========

    /**
     * Confirms the booking (PENDING → CONFIRMED).
     * Called by: VenueManager.confirmBooking()
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
     * Called by: VenueManager.rejectBooking()
     */
    public void reject() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Can only reject PENDING bookings");
        }
        this.status = BookingStatus.REJECTED;
    }

    /**
     * Cancels the booking (PENDING/CONFIRMED → CANCELLED).
     * Called by: Fan.cancelBooking()
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
     * Called by: Fan.markAllBookingsAsNotified(), NotificationService
     */
    public void markAsNotified() {
        this.notified = true;
    }

    // ========== PUBLIC API - Display/Formatting ==========

    /**
     * Gets the game matchup as formatted string.
     * Called by: UI for display
     */
    public String getMatchup() {
        return homeTeam + " vs " + awayTeam;
    }

    /**
     * Checks if a specific team is playing in this game.
     * Called by: Fan.getFavoriteTeamBookings()
     */
    public boolean isFavoriteTeamPlaying(String teamName) {
        return homeTeam.equalsIgnoreCase(teamName) || awayTeam.equalsIgnoreCase(teamName);
    }

    // ========== PRIVATE - Implementation Details ==========

    /**
     * Validates booking data during construction.
     */
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

    public int getId() { return id; }
    public LocalDate getGameDate() { return gameDate; }
    public LocalTime getGameTime() { return gameTime; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public int getVenueId() { return venueId; }
    public String getFanUsername() { return fanUsername; }
    public int getSeatsRequested() { return seatsRequested; }
    public BookingStatus getStatus() { return status; }
    public boolean isNotified() { return notified; }

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
                ", status=" + status +
                '}';
    }
}