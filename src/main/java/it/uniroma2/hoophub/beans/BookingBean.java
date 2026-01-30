package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for booking data transfer between UI and controller layers.
 *
 * <p>Each booking represents a single fan's reservation for a game viewing event.
 * Status defaults to {@link BookingStatus#PENDING} on creation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingBean {

    private int id;
    private LocalDate gameDate;
    private LocalTime gameTime;
    private TeamNBA homeTeam;
    private TeamNBA awayTeam;
    private int venueId;
    private String venueName;
    private String fanUsername;
    private BookingStatus status;
    private boolean notified;
    private LocalDateTime createdAt;

    private BookingBean(Builder builder) {
        this.id = builder.id;
        this.gameDate = builder.gameDate;
        this.gameTime = builder.gameTime;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.venueId = builder.venueId;
        this.venueName = builder.venueName;
        this.fanUsername = builder.fanUsername;
        this.status = builder.status;
        this.notified = builder.notified;
        this.createdAt = builder.createdAt;
    }

    /**
     * Builder for constructing BookingBean instances.
     */
    public static class Builder {
        private int id;
        private LocalDate gameDate;
        private LocalTime gameTime;
        private TeamNBA homeTeam;
        private TeamNBA awayTeam;
        private int venueId;
        private String venueName;
        private String fanUsername;
        private BookingStatus status = BookingStatus.PENDING;
        private boolean notified = false;
        private LocalDateTime createdAt;

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

        public Builder homeTeam(TeamNBA homeTeam) {
            this.homeTeam = homeTeam;
            return this;
        }

        public Builder awayTeam(TeamNBA awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public Builder venueId(int venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder venueName(String venueName) {
            this.venueName = venueName;
            return this;
        }

        public Builder fanUsername(String fanUsername) {
            this.fanUsername = fanUsername;
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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BookingBean build() {
            return new BookingBean(this);
        }
    }

    // ==================== GETTERS ====================

    public int getId() { return id; }
    public LocalDate getGameDate() { return gameDate; }
    public LocalTime getGameTime() { return gameTime; }
    public TeamNBA getHomeTeam() { return homeTeam; }
    public TeamNBA getAwayTeam() { return awayTeam; }
    public int getVenueId() { return venueId; }
    public String getVenueName() { return venueName; }
    public String getFanUsername() { return fanUsername; }
    public BookingStatus getStatus() { return status; }
    public boolean isNotified() { return notified; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ==================== SETTERS ====================

    public void setId(int id) { this.id = id; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }
    public void setGameTime(LocalTime gameTime) { this.gameTime = gameTime; }
    public void setHomeTeam(TeamNBA homeTeam) { this.homeTeam = homeTeam; }
    public void setAwayTeam(TeamNBA awayTeam) { this.awayTeam = awayTeam; }
    public void setVenueId(int venueId) { this.venueId = venueId; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public void setFanUsername(String fanUsername) { this.fanUsername = fanUsername; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}