package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.enums.UserType;

/**
 * SignUpDataSingleton implements the Singleton pattern to share registration data
 * between different graphic controllers during the multistep registration process.
 * <p>
 * This class allows temporarily storing user data entered in the first
 * registration form and making it available to subsequent user-specific forms.
 * </p>
 * <p>
 * <strong>Design Rationale for Singleton Pattern:</strong><br>
 * The Singleton pattern is necessary in this context because:
 * <ul>
 *   <li>Registration is a stateful multi-step process spanning multiple JavaFX controllers</li>
 *   <li>JavaFX creates new controller instances for each FXML view, making instance variables unsuitable</li>
 *   <li>Data must persist across different view transitions during the registration workflow</li>
 *   <li>A single shared state ensures data consistency and prevents concurrent registration conflicts</li>
 *   <li>Alternative solutions (dependency injection, event bus) would add unnecessary complexity
 *       for this simple temporary data sharing use case</li>
 * </ul>
 * The data is cleared immediately after registration completion, keeping the singleton stateless
 * between registration sessions.
 * </p>
 */
@SuppressWarnings("java:S6548")
public class SignUpDataSingleton {

    private static SignUpDataSingleton instance;

    // Common user data (collected in SignUp screen)
    private String username;
    private String password;
    private String fullName;
    private String gender;

    // User type (selected in SignUpChoice screen)
    private UserType userType;

    /**
     * Private constructor to implement the Singleton pattern.
     */
    private SignUpDataSingleton() {
        // Private constructor for Singleton pattern
    }

    /**
     * Returns the singleton instance of {@code SignUpDataSingleton}.
     * If the instance doesn't exist yet, it is created.
     *
     * @return The singleton instance of SignUpDataSingleton.
     */
    public static synchronized SignUpDataSingleton getInstance() {
        if (instance == null) {
            instance = new SignUpDataSingleton();
        }
        return instance;
    }

    // ========= Setters =========

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    // ========= Getters =========

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public UserType getUserType() {
        return userType;
    }

    // ========= Utility Methods =========

    /**
     * Clears all user data after registration has been completed or canceled.
     */
    public void clearUserData() {
        this.username = null;
        this.password = null;
        this.fullName = null;
        this.gender = null;
        this.userType = null;
    }

    /**
     * Checks if basic required data is present.
     *
     * @return true if fullName, username, password and gender are set
     */
    public boolean hasBasicData() {
        return fullName != null && !fullName.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty()
                && gender != null && !gender.isEmpty();
    }

    /**
     * Checks if user type has been selected.
     *
     * @return true if userType is set
     */
    public boolean hasUserType() {
        return userType != null;
    }
}