package it.uniroma2.hoophub.beans;

/**
 * Bean for venue manager registration and data transfer.
 * Extends UserBean to inherit common user fields (no duplication!).
 */
public class VenueManagerBean extends UserBean {
    private String companyName;
    private String phoneNumber;

    protected VenueManagerBean(Builder builder) {
        super(builder);
        this.companyName = builder.companyName;
        this.phoneNumber = builder.phoneNumber;
    }

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

        public VenueManagerBean build() {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Company name cannot be null or empty");
            }

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }
            return new VenueManagerBean(this);
        }
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
