package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for NBA game data transfer between layers.
 *
 * <p>Pure data carrier used by the boundary layer to display game information.
 * Contains no business logic.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NbaGameBean {

    private String gameId;
    private TeamNBA homeTeam;
    private TeamNBA awayTeam;
    private LocalDate date;
    private LocalTime time;

    private NbaGameBean(Builder builder) {
        this.gameId = builder.gameId;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.date = builder.date;
        this.time = builder.time;
    }

    /**
     * Builder for constructing NbaGameBean instances.
     */
    public static class Builder {
        private String gameId;
        private TeamNBA homeTeam;
        private TeamNBA awayTeam;
        private LocalDate date;
        private LocalTime time;

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

        public NbaGameBean build() {
            return new NbaGameBean(this);
        }
    }

    // Getters
    public String getGameId() { return gameId; }
    public TeamNBA getHomeTeam() { return homeTeam; }
    public TeamNBA getAwayTeam() { return awayTeam; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }

    // Setters
    public void setGameId(String gameId) { this.gameId = gameId; }
    public void setHomeTeam(TeamNBA homeTeam) { this.homeTeam = homeTeam; }
    public void setAwayTeam(TeamNBA awayTeam) { this.awayTeam = awayTeam; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
}