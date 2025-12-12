package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * Bean for transferring fan data between layers.
 * <p>
 * Contains specific fields for Fan users (Favorite Team and Birthday).
 * Extends {@link UserBean} to inherit common user properties.
 * Provides static methods for parsing and validating fan-specific data.
 * </p>
 */
public class FanBean extends UserBean {
    private TeamNBA favTeam;
    private final LocalDate birthday;

    // Minimum age requirement (e.g., 16 years old)
    private static final int MIN_AGE = 16;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder instance containing the data.
     */
    private FanBean(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
    }

    // ========================================================================
    // STATIC VALIDATION & PARSING METHODS
    // ========================================================================

    /**
     * Performs syntactic validation of the team input string.
     * Ensures the string corresponds to a valid NBA team using flexible matching.
     *
     * @param teamInput The user input string for the team.
     * @throws IllegalArgumentException if the team name is empty or not found.
     */
    public static void validateTeamSyntax(String teamInput) {
        if (teamInput == null || teamInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }

        // FIX: Usa robustValueOf per usare la stessa logica di parsing del resto dell'app
        if (TeamNBA.robustValueOf(teamInput) == null) {
            throw new IllegalArgumentException("Team not found. Please try the full name (e.g. 'Chicago Bulls') or abbreviation (e.g. 'CHI')");
        }
    }

    /**
     * Parses and validates the birthday string.
     * Encapsulates both format parsing (String to LocalDate) and business rule validation.
     * This is the entry point for text-based interfaces (CLI/GUI text fields).
     *
     * @param dateStr The date string to parse (expected format: YYYY-MM-DD).
     * @return The parsed and validated LocalDate.
     * @throws IllegalArgumentException if the format is invalid or business rules are violated.
     */
    public static LocalDate parseBirthday(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Birthday cannot be empty");
        }

        LocalDate date;
        try {
            // 1. Syntactic Parsing (Format & Calendar validity)
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Translate technical parsing error into a user-friendly message
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD and ensure the date exists.");
        }

        // 2. Business Rule Validation
        validateBirthday(date);

        return date;
    }

    /**
     * Validates the birthday date against business rules.
     * Checks if the date is null in the future, or if the user meets the minimum age requirement.
     *
     * @param birthday The date to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    public static void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday cannot be null");
        }

        LocalDate now = LocalDate.now();

        if (birthday.isAfter(now)) {
            throw new IllegalArgumentException("Birthday cannot be in the future");
        }

        // Check age limit
        if (Period.between(birthday, now).getYears() < MIN_AGE) {
            throw new IllegalArgumentException("You must be at least " + MIN_AGE + " years old to register");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder class for constructing FanBean instances.
     * Extends UserBean.Builder to inherit common user fields.
     */
    public static class Builder extends UserBean.Builder<Builder> {
        private TeamNBA favTeam;
        private LocalDate birthday;

        public Builder() {
            super();
        }

        /**
         * Sets the favorite team.
         * Note: The input here is now a type-safe TeamNBA object.
         *
         * @param favTeam The favorite NBA team.
         * @return The builder instance.
         */
        public Builder favTeam(TeamNBA favTeam) {
            this.favTeam = favTeam;
            return this;
        }

        /**
         * Sets the birthday with validation.
         *
         * @param birthday The birthday date.
         * @return The builder instance.
         * @throws IllegalArgumentException if the date is invalid according to business rules.
         */
        public Builder birthday(LocalDate birthday) {
            validateBirthday(birthday);
            this.birthday = birthday;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FanBean build() {
            return new FanBean(this);
        }
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    public TeamNBA getFavTeam() {
        return favTeam;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
}