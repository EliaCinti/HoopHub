package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.TeamNBA;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Immutable domain entity representing an NBA game from external API.
 *
 * <p>Represents a scheduled game in the NBA calendar. Instances are typically
 * created from data retrieved via {@link it.uniroma2.hoophub.patterns.adapter.NbaApiAdapter}.</p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> for construction. Fully immutable after construction.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NbaGame {

    /** External API game ID (may be alphanumeric). */
    private final String gameId;
    private final TeamNBA homeTeam;
    private final TeamNBA awayTeam;
    private final LocalDate date;
    private final LocalTime time;

    private NbaGame(Builder builder) {
        validateGameData(builder.gameId, builder.homeTeam, builder.awayTeam, builder.date, builder.time);

        this.gameId = builder.gameId;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.date = builder.date;
        this.time = builder.time;
    }

    // ========================================================================
    // PUBLIC QUERIES
    // ========================================================================

    /**
     * Gets formatted game matchup.
     *
     * @return "Los Angeles Lakers vs Boston Celtics"
     */
    public String getMatchup() {
        return homeTeam.getDisplayName() + " vs " + awayTeam.getDisplayName();
    }

    // ========================================================================
    // PUBLIC GETTERS
    // ========================================================================

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

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for NbaGame with fluent API.
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

        public NbaGame build() {
            return new NbaGame(this);
        }
    }

    // ========================================================================
    // VALIDATION HELPERS
    // ========================================================================

    private void validateGameData(String gameId, TeamNBA homeTeam, TeamNBA awayTeam,
                                  LocalDate date, LocalTime time) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty");
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
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NbaGame nbaGame = (NbaGame) o;
        return Objects.equals(gameId, nbaGame.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId);
    }

    @Override
    public String toString() {
        return "NbaGame{" +
                "id='" + gameId + '\'' +
                ", matchup='" + getMatchup() + '\'' +
                ", date=" + date +
                ", time=" + time +
                '}';
    }
}