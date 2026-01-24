package it.uniroma2.hoophub.beans;

import java.util.regex.Pattern;

/**
 * Bean for VenueManager user data transfer.
 *
 * <p>Extends {@link UserBean} with manager-specific fields: company name and phone number.
 * Provides validation for both fields.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerBean extends UserBean {

    private String companyName;
    private String phoneNumber;

    private static final int COMPANY_NAME_MIN_LENGTH = 2;
    private static final int COMPANY_NAME_MAX_LENGTH = 100;
    private static final int PHONE_NUMBER_MIN_LENGTH = 6;
    private static final int PHONE_NUMBER_MAX_LENGTH = 20;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]+$");

    protected VenueManagerBean(Builder builder) {
        super(builder);
        this.companyName = builder.companyName;
        this.phoneNumber = builder.phoneNumber;
    }

    // ========================================================================
    // STATIC VALIDATION METHODS
    // ========================================================================

    /**
     * Validates company name syntax (2-100 characters).
     *
     * @param companyName the company name to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateCompanyNameSyntax(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be empty");
        }
        String trimmed = companyName.trim();
        if (trimmed.length() < COMPANY_NAME_MIN_LENGTH) {
            throw new IllegalArgumentException("Company name must be at least " + COMPANY_NAME_MIN_LENGTH + " characters");
        }
        if (trimmed.length() > COMPANY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Company name cannot exceed " + COMPANY_NAME_MAX_LENGTH + " characters");
        }
    }

    /**
     * Validates phone number syntax (6-20 characters, valid phone characters only).
     *
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validatePhoneNumberSyntax(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        String trimmed = phoneNumber.trim();
        if (trimmed.length() < PHONE_NUMBER_MIN_LENGTH) {
            throw new IllegalArgumentException("Phone number must be at least " + PHONE_NUMBER_MIN_LENGTH + " characters");
        }
        if (trimmed.length() > PHONE_NUMBER_MAX_LENGTH) {
            throw new IllegalArgumentException("Phone number cannot exceed " + PHONE_NUMBER_MAX_LENGTH + " characters");
        }
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Phone number contains invalid characters");
        }
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for VenueManagerBean, extends {@link UserBean.Builder}.
     */
    public static class Builder extends UserBean.Builder<Builder> {
        private String companyName;
        private String phoneNumber;

        public Builder() {
            super();
        }

        public Builder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public VenueManagerBean build() {
            validateCompanyNameSyntax(companyName);
            validatePhoneNumberSyntax(phoneNumber);
            return new VenueManagerBean(this);
        }
    }

    // Getters & Setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}