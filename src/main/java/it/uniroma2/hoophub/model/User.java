package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.UserType;

import java.util.Objects;

/**
 * Base abstract class for users.
 * This class encapsulates the state and behavior common to all users.
 * State modification is restricted to public business operations.
 * @author Elia Cinti
 */
public abstract class User {
    private final String username; // Immutable Primary Key
    private String fullName;
    private String gender;
    private final String passwordHash;

    /**
     * Protected constructor for use by subclasses and the Builder.
     * The Initial state (including immutable PK) is set directly here.
     */
    protected User(Builder<?> builder) {
        // Immutable fields like PK are set directly
        this.username = builder.username;

        // Initial state for mutable fields is set directly
        this.fullName = builder.fullName;
        this.gender = builder.gender;
        this.passwordHash  = builder.passwordHash;
    }

    // ========================================================================
    // PUBLIC ABSTRACT OPERATIONS (Business Logic)
    // ========================================================================

    /**
     * Returns the specific user type (FAN or VENUE_MANAGER).
     * @return The UserType enum value.
     */
    public abstract UserType getUserType();

    /**
     * Public business operation to update the user's profile information.
     * This is the ONLY way external classes can modify the user's state
     * after construction.
     *
     * @param newFullName The user's new full name.
     * @param newGender   The user's new gender.
     * @throws IllegalArgumentException if validation for new data fails.
     */
    public void updateProfileDetails(String newFullName, String newGender) {
        // 1. Validate the new data
        validateFullName(newFullName);
        validateGender(newGender);

        // 2. Mutate the state using private setters
        this.setFullName(newFullName);
        this.setGender(newGender);
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
    // ========================================================================

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public String getPasswordHash() {return passwordHash;}

    // ========================================================================
    // PRIVATE/PROTECTED SETTERS (Internal State Mutation)
    // ========================================================================

    /**
     * Private setter for full name.
     * Only called by updateProfileDetails().
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Private setter for gender.
     * Only called by updateProfileDetails().
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    // ========================================================================
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

    /**
     * Builder for creating User instances with validation.
     */
    public static class Builder<T extends Builder<T>> {
        protected String username;
        protected String fullName;
        protected String gender;
        protected String passwordHash;

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
        public T password(String passwordHash) {
            this.passwordHash = passwordHash;
            return self();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        protected void validate() {
            validateUsername(username);
            validateFullName(fullName);
            validateGender(gender);
        }
    }

    // ========================================================================
    // PRIVATE VALIDATION HELPERS (Internal Logic)
    // ========================================================================

    protected static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (!username.matches("^[\\p{Alnum}_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
    }

    protected static void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
    }

    protected static void validateGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
    }

// ========================================================================
// UTILITY METHODS (equals, hashCode, toString)
// ========================================================================

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
