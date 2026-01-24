package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.TeamNBA;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * Bean for Fan user data transfer.
 *
 * <p>Extends {@link UserBean} with fan-specific fields: favorite team and birthday.
 * Provides validation for team selection and age requirements (minimum 16 years).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanBean extends UserBean {

    private TeamNBA favTeam;
    private final LocalDate birthday;
    private static final int MIN_AGE = 16;

    private FanBean(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
    }

    // ========================================================================
    // STATIC VALIDATION & PARSING METHODS
    // ========================================================================

    /**
     * Validates team input using flexible matching (full name or abbreviation).
     *
     * @param teamInput the team string to validate
     * @throws IllegalArgumentException if team not found
     */
    public static void validateTeamSyntax(String teamInput) {
        if (teamInput == null || teamInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }
        if (TeamNBA.robustValueOf(teamInput) == null) {
            throw new IllegalArgumentException("Team not found. Please try the full name (e.g. 'Chicago Bulls') or abbreviation (e.g. 'CHI')");
        }
    }

    /**
     * Parses and validates birthday string (format: YYYY-MM-DD).
     *
     * @param dateStr the date string to parse
     * @return parsed and validated LocalDate
     * @throws IllegalArgumentException if format invalid or age requirements not met
     */
    public static LocalDate parseBirthday(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Birthday cannot be empty");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD and ensure the date exists.");
        }

        validateBirthday(date);
        return date;
    }

    /**
     * Validates birthday against business rules (not future, minimum age).
     *
     * @param birthday the date to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday cannot be null");
        }

        LocalDate now = LocalDate.now();

        if (birthday.isAfter(now)) {
            throw new IllegalArgumentException("Birthday cannot be in the future");
        }

        if (Period.between(birthday, now).getYears() < MIN_AGE) {
            throw new IllegalArgumentException("You must be at least " + MIN_AGE + " years old to register");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for FanBean, extends {@link UserBean.Builder}.
     */
    public static class Builder extends UserBean.Builder<Builder> {
        private TeamNBA favTeam;
        private LocalDate birthday;

        public Builder() {
            super();
        }

        public Builder favTeam(TeamNBA favTeam) {
            this.favTeam = favTeam;
            return this;
        }

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

    // Getters
    public TeamNBA getFavTeam() { return favTeam; }
    public LocalDate getBirthday() { return birthday; }
}