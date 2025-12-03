package it.uniroma2.hoophub.beans;

/**
 * Bean for user credentials (login).
 * <p>
 * This class encapsulates the username, password, and user type.
 * It serves as a base class for {@link UserBean} and provides static validation methods
 * to enforce syntactic rules for credentials.
 * </p>
 */
public class CredentialsBean {
    private final String username;
    private final String password;
    private String type;

    /**
     * Protected constructor for use by the Builder.
     *
     * @param builder The builder instance containing the data.
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
     * Performs syntactic validation of the username.
     * Rule: Must not be null or empty, and must be at least 3 characters long.
     *
     * @param username The username string to validate.
     * @throws IllegalArgumentException if the username is invalid.
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
     * Performs syntactic validation of the password.
     * Rule: Must not be null or empty, and must be at least 6 characters long.
     *
     * @param password The password string to validate.
     * @throws IllegalArgumentException if the password is invalid.
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
     * Builder class for constructing CredentialsBean instances.
     * <p>
     * Uses generic type {@code T} to allow subclassing (fluent interface inheritance).
     * </p>
     *
     * @param <T> The type of the builder subclass.
     */
    public static class Builder<T extends Builder<T>> {
        protected String username;
        protected String password;
        protected String type;

        /**
         * Sets the username with validation.
         *
         * @param username The username to set.
         * @return The builder instance.
         * @throws IllegalArgumentException if the username is invalid.
         */
        public T username(String username) {
            validateUsernameSyntax(username);
            this.username = username;
            return self();
        }

        /**
         * Sets the password with validation.
         *
         * @param password The password to set.
         * @return The builder instance.
         * @throws IllegalArgumentException if the password is invalid.
         */
        public T password(String password) {
            validatePasswordSyntax(password);
            this.password = password;
            return self();
        }

        /**
         * Sets the user type.
         *
         * @param type The user type string (e.g., "FAN", "VENUE_MANAGER").
         * @return The builder instance.
         */
        public T type(String type) {
            this.type = type;
            return self();
        }

        /**
         * Returns the builder instance (essential for fluent inheritance).
         *
         * @return The builder instance cast to type {@code T}.
         */
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        /**
         * Builds the CredentialsBean instance.
         *
         * @return A new CredentialsBean.
         */
        public CredentialsBean build() {
            return new CredentialsBean(this);
        }
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
