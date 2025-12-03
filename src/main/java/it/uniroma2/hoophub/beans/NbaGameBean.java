package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bean for transferring NbaGame data between layers.
 * <p>
 * Contains only data fields with no business logic, following the DTO pattern.
 * Used by the Boundary to display game information retrieved from the Controller.
 * </p>
 */
public class NbaGameBean {

    private String gameId;
    private TeamNBA homeTeam;
    private TeamNBA awayTeam;
    private LocalDate date;
    private LocalTime time;
    private String arenaName;

    /**
     * Private constructor used by the Builder.
     */
    private NbaGameBean(Builder builder) {
        this.gameId = builder.gameId;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.date = builder.date;
        this.time = builder.time;
        this.arenaName = builder.arenaName;
    }

    /**
     * Builder class for NbaGameBean.
     */
    public static class Builder {
        private String gameId;
        private TeamNBA homeTeam;
        private TeamNBA awayTeam;
        private LocalDate date;
        private LocalTime time;
        private String arenaName;

        public Builder gameId(String gameId) {
            this.gameId = gameId;
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

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder time(LocalTime time) {
            this.time = time;
            return this;
        }

        public Builder arenaName(String arenaName) {
            this.arenaName = arenaName;
            return this;
        }

        public NbaGameBean build() {
            return new NbaGameBean(this);
        }
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public String getGameId() {
        return gameId;
    }

    public TeamNBA getHomeTeam() {
        return homeTeam;
    }

    public TeamNBA getAwayTeam() {
        return awayTeam;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getArenaName() {
        return arenaName;
    }

    // ========================================================================
    // SETTERS
    // ========================================================================

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setHomeTeam(TeamNBA homeTeam) {
        this.homeTeam = homeTeam;
    }

    public void setAwayTeam(TeamNBA awayTeam) {
        this.awayTeam = awayTeam;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }
}