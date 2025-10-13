package it.uniroma2.hoophub.beans;

/**
 * Bean for user credentials (login).
 */
public class CredentialsBean {
    private final String username;
    private final String password;
    private String type;

    protected CredentialsBean(Builder<?> builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.type = builder.type;
    }

    public static class Builder<T extends Builder<T>> {
        protected String username;
        protected String password;
        protected String type;

        public T username(String username) {
            this.username = username;
            return self();
        }

        public T password(String password) {
            this.password = password;
            return self();
        }

        public T type(String type) {
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
