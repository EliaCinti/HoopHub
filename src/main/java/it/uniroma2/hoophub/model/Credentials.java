package it.uniroma2.hoophub.model;

/**
 * Model class representing authentication credentials.
 * Contains the raw data needed to verify a user's identity.
 */
public class Credentials {
    private final String username;
    private final String password; // Password in chiaro (raw)

    private Credentials(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // Builder Pattern
    public static class Builder {
        private String username;
        private String password;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Credentials build() {
            // Validazione base
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }
            return new Credentials(this);
        }
    }
}
