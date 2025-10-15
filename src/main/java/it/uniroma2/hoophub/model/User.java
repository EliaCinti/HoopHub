package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.UserType;

import java.util.Objects;

/**
 * Base class for users with common validation logic.
 * This is an abstract domain entity representing any user in the system.
 */
public abstract class User {
    private String username;
    private String fullName;
    private String gender;

    /**
     * Protected constructor - only accessible via Builder or subclasses.
     */
    protected User(Builder<?> builder) {
        this.username = builder.username;
        this.fullName = builder.fullName;
        this.gender = builder.gender;
    }

    // ========== METODI ASTRATTI (da implementare nelle sottoclassi) ==========

    /**
     * Returns the user type (FAN or VENUE_MANAGER).
     * Used for: role-based UI navigation, access control.
     */
    public abstract UserType getUserType();

    /**
     * Builder for creating User instances with validation.
     */
    public static class Builder<T extends Builder<T>> {
        protected String username;
        protected String fullName;
        protected String gender;

        public T username(String username) {
            this.username = username;
            return self();
        }

        public T fullName(String fullName) {
            this.fullName = fullName;
            return self();
        }

        public T gender(String gender) {
            this.gender = gender;
            return self();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        /**
         * Template method for validation.
         * Subclasses can override to add more validation.
         * PROTECTED - allows subclass builders to extend validation.
         */
        protected void validate() {
            validateUsername();
            validateFullName();
            validateGender();
        }

        /**
         * Validates username field.
         * PROTECTED - allows subclass builders to reuse or override.
         */
        protected void validateUsername() {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            if (username.length() < 3) {
                throw new IllegalArgumentException("Username must be at least 3 characters");
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
            }
        }

        /**
         * Validates full name field.
         * PROTECTED - allows subclass builders to reuse or override.
         */
        protected void validateFullName() {
            if (fullName == null || fullName.trim().isEmpty()) {
                throw new IllegalArgumentException("Full name cannot be null or empty");
            }
        }

        /**
         * Validates gender field.
         * PROTECTED - allows subclass builders to reuse or override.
         */
        protected void validateGender() {
            if (gender == null || gender.trim().isEmpty()) {
                throw new IllegalArgumentException("Gender cannot be null or empty");
            }
        }
    }

    // ========== STANDARD GETTERS - Always PUBLIC ==========

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    // ========== SETTERS - PUBLIC but use with caution ==========

    /**
     * Sets username.
     * WARNING: Username is typically immutable (primary key).
     * Consider removing this setter or making it package-private.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets full name.
     * Used by: UserController when user updates profile.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Sets gender.
     * Used by: UserController when user updates profile.
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    // ========== UTILITY METHODS ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", gender='" + gender + '\'' +
                '}';
    }
}
