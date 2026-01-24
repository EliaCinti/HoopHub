package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.UserType;

import java.util.Objects;

/**
 * Abstract base class for all user entities in HoopHub.
 *
 * <p>Implements <b>BCE (Boundary-Control-Entity)</b> principles:
 * <ul>
 *   <li>State is private with no public setters</li>
 *   <li>State modification only through explicit business operations</li>
 *   <li>Immutable primary key (username)</li>
 * </ul>
 * </p>
 *
 * <p>Uses <b>Builder pattern (GoF)</b> with generic self-referencing type
 * for proper inheritance support in subclass builders.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Fan
 * @see VenueManager
 */
public abstract class User {

    /** Immutable primary key. */
    private final String username;
    private String fullName;
    private String gender;
    /** Stored as hash, never plain text. */
    private final String passwordHash;

    /**
     * Protected constructor for subclass builders.
     *
     * @param builder the builder containing validated data
     */
    protected User(Builder<?> builder) {
        this.username = builder.username;
        this.fullName = builder.fullName;
        this.gender = builder.gender;
        this.passwordHash = builder.passwordHash;
    }

    // ========================================================================
    // PUBLIC ABSTRACT OPERATIONS
    // ========================================================================

    /**
     * Returns the specific user type.
     *
     * @return FAN or VENUE_MANAGER
     */
    public abstract UserType getUserType();

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Updates user profile information.
     *
     * <p>This is the ONLY way to modify user state after construction,
     * enforcing BCE principles.</p>
     *
     * @param newFullName new full name
     * @param newGender   new gender
     * @throws IllegalArgumentException if validation fails
     */
    public void updateProfileDetails(String newFullName, String newGender) {
        validateFullName(newFullName);
        validateGender(newGender);

        this.setFullName(newFullName);
        this.setGender(newGender);
    }

    // ========================================================================
    // PUBLIC GETTERS
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

    public String getPasswordHash() {
        return passwordHash;
    }

    // ========================================================================
    // PRIVATE SETTERS
    // ========================================================================

    /** Called only by updateProfileDetails(). */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** Called only by updateProfileDetails(). */
    public void setGender(String gender) {
        this.gender = gender;
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Generic builder supporting inheritance via self-referencing type parameter.
     *
     * @param <T> concrete builder type for fluent method chaining
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
    // VALIDATION HELPERS
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
    // UTILITY METHODS
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