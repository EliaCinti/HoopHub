package it.uniroma2.hoophub.beans;

/**
 * Bean for general user data transfer.
 *
 * <p>Contains common fields (fullName, gender) shared by {@link FanBean} and
 * {@link VenueManagerBean}. Extends {@link CredentialsBean} to inherit credentials.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class UserBean extends CredentialsBean {

    private String fullName;
    private String gender;

    protected UserBean(Builder<?> builder) {
        super(builder);
        this.fullName = builder.fullName;
        this.gender = builder.gender;
    }

    // ========================================================================
    // STATIC VALIDATION METHODS
    // ========================================================================

    /**
     * Validates full name syntax (must contain first and last name separated by space).
     *
     * @param fullName the full name to validate
     * @throws IllegalArgumentException if validation fails
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
     * Validates gender syntax (must be Male/M, Female/F, or Other).
     *
     * @param gender the gender string to validate
     * @throws IllegalArgumentException if validation fails
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
     * Generic builder extending {@link CredentialsBean.Builder}.
     *
     * @param <T> the builder subclass type
     */
    public static class Builder<T extends Builder<T>> extends CredentialsBean.Builder<T> {
        private String fullName;
        private String gender;

        public Builder() {
            super();
        }

        public T fullName(String fullName) {
            validateFullNameSyntax(fullName);
            this.fullName = fullName;
            return self();
        }

        public T gender(String gender) {
            validateGenderSyntax(gender);
            this.gender = normalizeGenderInternal(gender);
            return self();
        }

        private static String normalizeGenderInternal(String gender) {
            if (gender == null) return null;
            String lower = gender.toLowerCase();
            return switch (lower) {
                case "m", "male" -> "Male";
                case "f", "female" -> "Female";
                case "other" -> "Other";
                default -> gender;
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

    /**
     * Concrete builder for direct UserBean instantiation.
     */
    public static final class ConcreteBuilder extends Builder<ConcreteBuilder> {
        @Override
        protected ConcreteBuilder self() {
            return this;
        }
    }

    /**
     * Factory method to create a concrete builder.
     *
     * @return new ConcreteBuilder instance
     */
    public static ConcreteBuilder builder() {
        return new ConcreteBuilder();
    }

    // Getters & Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}