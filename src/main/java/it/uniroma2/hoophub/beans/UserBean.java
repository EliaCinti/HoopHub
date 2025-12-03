package it.uniroma2.hoophub.beans;

/**
 * Bean for general user data.
 * <p>
 * Contains common fields shared by FanBean and VenueManagerBean (fullName, gender).
 * Extends {@link CredentialsBean} to eliminate code duplication.
 * Provides static validation methods for user-specific fields.
 * </p>
 */
public class UserBean extends CredentialsBean {
    private String fullName;
    private String gender;

    /**
     * Protected constructor for use by the Builder.
     *
     * @param builder The builder instance containing the data.
     */
    protected UserBean(Builder<?> builder) {
        super(builder);
        this.fullName = builder.fullName;
        this.gender = builder.gender;
    }

    // ========================================================================
    // STATIC VALIDATION METHODS (Public Domain Rules)
    // ========================================================================

    /**
     * Performs syntactic validation of the full name.
     * Rule: Must contain at least two words separated by a space (Firstname Lastname).
     *
     * @param fullName The full name string to validate.
     * @throws IllegalArgumentException if the format is invalid or empty.
     */
    public static void validateFullNameSyntax(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        if (!fullName.contains(" ") || !fullName.trim().contains(" ")) {
            throw new IllegalArgumentException("Please enter both Name and Surname separated by a space");
        }
    }

    /**
     * Performs syntactic validation of the gender.
     * Rule: Must be one of 'Male', 'Female', or 'Other' (case-insensitive) or 'M', 'F'.
     *
     * @param gender The gender string to validate.
     * @throws IllegalArgumentException if the gender is invalid or empty.
     */
    public static void validateGenderSyntax(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be empty");
        }
        String lower = gender.toLowerCase();
        if (!lower.equals("male") && !lower.equals("m") &&
                !lower.equals("female") && !lower.equals("f") &&
                !lower.equals("other")) {
            throw new IllegalArgumentException("Please enter 'Male' (M), 'Female' (F), or 'Other'");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder class for UserBean.
     * Extends CredentialsBean.Builder to inherit username/password methods.
     */
    public static class Builder<T extends Builder<T>> extends CredentialsBean.Builder<T> {
        private String fullName;
        private String gender;

        public Builder() {
            super();
        }

        /**
         * Sets the full name with validation.
         *
         * @param fullName The full name to set.
         * @return The builder instance.
         * @throws IllegalArgumentException if validation fails.
         */
        public T fullName(String fullName) {
            validateFullNameSyntax(fullName);
            this.fullName = fullName;
            return self();
        }

        /**
         * Sets the gender with validation and normalization.
         *
         * @param gender The gender to set.
         * @return The builder instance.
         * @throws IllegalArgumentException if validation fails.
         */
        public T gender(String gender) {
            // 1. Validation (External rule)
            validateGenderSyntax(gender);

            // 2. Normalization (Internal representation logic)
            this.gender = normalizeGenderInternal(gender);

            return self();
        }

        /**
         * Internal helper to normalize gender string.
         */
        private static String normalizeGenderInternal(String gender) {
            if (gender == null) return null;
            String lower = gender.toLowerCase();
            return switch (lower) {
                case "m", "male" -> "Male";
                case "f", "female" -> "Female";
                case "other" -> "Other";
                default -> gender; // Fallback, blocked by validation anyway
            };
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T self() {
            return (T) this;
        }

        @Override
        public UserBean build() {
            return new UserBean(this);
        }
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}