package it.uniroma2.hoophub.utilities;

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
 *
 * @SuppressWarnings("java:S6548") Singleton pattern is justified for this multi-step registration workflow
 */
public class SignUpDataSingleton {
    private static SignUpDataSingleton instance;
    private String username;
    private String password;
    private String name;
    private String surname;
    private String gender;
    private String userType;

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

    /**
     * Stores user data for the registration process.
     *
     * @param username The user's username
     * @param password The user's password
     * @param name The user's first name
     * @param surname The user's last name
     * @param gender The user's gender
     * @param userType The user type (FAN or VENUE_MANAGER)
     */
    public void setUserData(String username, String password, String name, String surname, String gender, String userType) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.userType = userType;
    }

    /**
     * Clears user data after registration has been completed or cancelled.
     */
    public void clearUserData() {
        this.username = null;
        this.password = null;
        this.name = null;
        this.surname = null;
        this.gender = null;
        this.userType = null;
    }

    /**
     * Gets the user's username.
     *
     * @return The user's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the user's password.
     *
     * @return The user's password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the user's first name.
     *
     * @return The user's first name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the user's last name.
     *
     * @return The user's last name
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Gets the user's gender.
     *
     * @return The user's gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * Gets the user type.
     *
     * @return The user type (FAN or VENUE_MANAGER)
     */
    public String getUserType() {
        return userType;
    }
}

