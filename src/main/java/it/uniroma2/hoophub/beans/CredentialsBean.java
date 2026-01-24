package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.UserType;

/**
 * Base bean for user credentials (login).
 *
 * <p>Encapsulates username, password, and user type. Serves as base class for
 * {@link UserBean}. Provides static validation methods for credential syntax.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CredentialsBean {

    private final String username;
    private final String password;
    private UserType type;

    /**
     * Protected constructor for Builder pattern.
     *
     * @param builder the builder instance
     */
    protected CredentialsBean(Builder<?> builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.type = builder.type;
    }

    // ========================================================================
    // STATIC VALIDATION METHODS
    // ========================================================================

    /**
     * Validates username syntax (not null/empty, min 3 characters).
     *
     * @param username the username to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateUsernameSyntax(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
    }

    /**
     * Validates password syntax (not null/empty, min 6 characters).
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validatePasswordSyntax(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Generic builder supporting fluent inheritance for subclasses.
     *
     * @param <T> the builder subclass type
     */
    public static class Builder<T extends Builder<T>> {
        protected String username;
        protected String password;
        protected UserType type;

        public T username(String username) {
            validateUsernameSyntax(username);
            this.username = username;
            return self();
        }

        public T password(String password) {
            validatePasswordSyntax(password);
            this.password = password;
            return self();
        }

        public T type(UserType type) {
            this.type = type;
            return self();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public CredentialsBean build() {
            return new CredentialsBean(this);
        }
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserType getType() { return type; }
    public void setType(UserType type) { this.type = type; }
}