package it.uniroma2.hoophub.beans;

/**
 * Bean for general user data.
 * Contains common fields shared by FanBean and VenueManagerBean.
 * This class eliminates code duplication.
 */
public class UserBean extends CredentialsBean {
    private String fullName;
    private String gender;

    protected UserBean(Builder<?> builder) {
        super(builder);
        this.fullName = builder.fullName;
        this.gender = builder.gender;
    }

    public static class Builder<T extends Builder<T>> extends CredentialsBean.Builder<T> {
        private String fullName;
        private String gender;

        public Builder() {
            super();
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
        @Override
        protected T self() {
            return (T) this;
        }

        public UserBean build() {
            return new UserBean(this);
        }
    }

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