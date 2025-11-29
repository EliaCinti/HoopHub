package it.uniroma2.hoophub.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents an NBA Game event.
 * <p>
 * This entity represents a specific game scheduled in the NBA calendar.
 * Instances of this class are typically created from data retrieved from
 * an external NBA Schedule API.
 * </p>
 * <p>
 * This class is immutable. Its state is set at construction time via the Builder
 * and cannot be modified afterwards, ensuring consistency when displaying schedules.
 * </p>
 *
 * @author Elia Cinti
 */
public class NbaGame {

    private final String gameId; // External API ID might be alphanumeric
    private final TeamNBA homeTeam;
    private final TeamNBA awayTeam;
    private final LocalDate date;
    private final LocalTime time;
    private final String arenaName; // The actual NBA arena (e.g., "Madison Square Garden")

    /**
     * Private constructor for use by the Builder.
     * Validates all incoming data.
     */
    private NbaGame(Builder builder) {
        validateGameData(builder.gameId, builder.homeTeam, builder.awayTeam, builder.date, builder.time);

        this.gameId = builder.gameId;
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.date = builder.date;
        this.time = builder.time;
        this.arenaName = builder.arenaName;
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS & QUERIES
    // ========================================================================

    /**
     * Gets the game matchup as a formatted string.
     *
     * @return A string like "Los Angeles Lakers vs Boston Celtics".
     */
    public String getMatchup() {
        return homeTeam.getDisplayName() + " vs " + awayTeam.getDisplayName();
    }

    /**
     * Checks if a specific team is playing in this game.
     *
     * @param team The NBA team to check.
     * @return true if the team is either home or away, false otherwise.
     */
    public boolean isTeamPlaying(TeamNBA team) {
        if (team == null) {
            return false;
        }
        return homeTeam == team || awayTeam == team;
    }

    /**
     * Checks if the game is in the past relative to the current date.
     *
     * @return true if the game date is before today.
     */
    public boolean isPastGame() {
        return date.isBefore(LocalDate.now());
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
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

        public NbaGame build() {
            return new NbaGame(this);
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION HELPERS
    // ========================================================================

    private void validateGameData(String gameId, TeamNBA homeTeam, TeamNBA awayTeam, LocalDate date, LocalTime time) {
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
    // UTILITY METHODS (equals, hashCode, toString)
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
                ", arena='" + arenaName + '\'' +
                '}';
    }
}
