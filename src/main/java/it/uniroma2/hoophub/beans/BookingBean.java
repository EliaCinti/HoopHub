package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.utilities.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bean for booking data transfer.
 * Used when Fans create booking requests via UI.
 */
public class BookingBean {
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

    private BookingBean(Builder builder) {
        this.id = builder.id;
        this.gameDate = builder.gameDate;
        this.gameTime = builder.gameTime;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.venueId = builder.venueId;
        this.fanUsername = builder.fanUsername;
        this.seatsRequested = builder.seatsRequested;
        this.status = builder.status;
        this.notified = builder.notified;
    }

    public static class Builder {
        private int id;
        private LocalDate gameDate;
        private LocalTime gameTime;
        private String homeTeam;
        private String awayTeam;
        private int venueId;
        private String fanUsername;
        private int seatsRequested;
        private BookingStatus status = BookingStatus.PENDING;
        private boolean notified = false;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder gameDate(LocalDate gameDate) {
            this.gameDate = gameDate;
            return this;
        }

        public Builder gameTime(LocalTime gameTime) {
            this.gameTime = gameTime;
            return this;
        }

        public Builder homeTeam(String homeTeam) {
            this.homeTeam = homeTeam;
            return this;
        }

        public Builder awayTeam(String awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public Builder venueId(int venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder fanUsername(String fanUsername) {
            this.fanUsername = fanUsername;
            return this;
        }

        public Builder seatsRequested(int seatsRequested) {
            this.seatsRequested = seatsRequested;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder notified(boolean notified) {
            this.notified = notified;
            return this;
        }

        public BookingBean build() {
            return new BookingBean(this);
        }
    }

    // Getters
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

    // Setters
    public void setId(int id) { this.id = id; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }
    public void setGameTime(LocalTime gameTime) { this.gameTime = gameTime; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    public void setVenueId(int venueId) { this.venueId = venueId; }
    public void setFanUsername(String fanUsername) { this.fanUsername = fanUsername; }
    public void setSeatsRequested(int seats) { this.seatsRequested = seats; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
