package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.enums.UserType;

import java.time.Instant;
import java.time.Duration;

/**
 * LoginController manages the authentication process for users trying to log in to the HoopHub application.
 * This class interacts with various data access objects (DAOs)
 * to validate user credentials and retrieve user information.
 * <p>
 * Uses polymorphism to handle different user types (Fan, VenueManager) uniformly.
 * </p>
 * <p>
 * <strong>Singleton Pattern:</strong> This controller is a singleton because it manages application-level
 * use case logic without any UI-specific or session-specific state. All boundary classes should use
 * the same instance via {@link #getInstance()}.
 * </p>
 */
@SuppressWarnings("java:S6548") // Singleton is required
public class LoginController extends AbstractController {

    private static LoginController instance;

    // Rate limiting constants
    private static final int MAX_ATTEMPTS_BEFORE_DELAY = 3;
    private static final int BASE_DELAY_SECONDS = 30;

    // Track failed login attempts globally (not per username)
    private int globalFailedAttempts = 0;
    private Instant lastFailedAttemptTime = null;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private LoginController() {
        // Private constructor
    }

    /**
     * Returns the singleton instance of LoginController.
     * <p>
     * This method ensures that only one instance of the controller exists
     * throughout the application lifecycle.
     * </p>
     *
     * @return The singleton LoginController instance
     */
    public static synchronized LoginController getInstance() {
        if (instance == null) {
            instance = new LoginController();
        }
        return instance;
    }

    /**
     * Attempts to log in a user using the provided credentials.
     * It determines the user type, validates the credentials, retrieves the corresponding user data,
     * and initiates a session for the user if authentication is successful.
     * <p>
     * <strong>Global Rate Limiting:</strong> After 3 failed login attempts (regardless of username),
     * ALL subsequent login attempts are blocked for a waiting period of 30 * (attemptNumber - 3) seconds.
     * This prevents both brute-force attacks and username enumeration attacks.
     * Example: After 3 failed attempts, the 4th attempt requires waiting 30 seconds, the 5th requires
     * 60 seconds, etc. The counter resets after a successful login or when the delay expires.
     * </p>
     * <p>
     * <strong>Bean Pattern:</strong> This method accepts a CredentialsBean (input) and returns
     * a UserBean (output), ensuring the boundary layer never accesses business logic from Model objects.
     * Internally, the controller works with Model objects and converts them to Beans for the boundary.
     * </p>
     *
     * @param credentials The credentials provided by the user, containing username, password, and user type.
     * @return A UserBean containing user data without business logic, for boundary layer use.
     * @throws DAOException         If there is an issue with data access, rate limit exceeded, or validation fails.
     * @throws UserSessionException If the user is already logged in elsewhere, preventing a new session start.
     */
    public UserBean login(CredentialsBean credentials) throws DAOException, UserSessionException {
        // Check rate limiting before attempting login (global check)
        checkRateLimit();

        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();
        UserDao userDao = daoFactoryFacade.getUserDao();

        try {
            userDao.validateUser(credentials);
        } catch (DAOException e) {
            // Login failed - increment global failed attempts counter
            recordFailedAttempt();
            throw e;
        }

        User user = retrieveUserByType(credentials, daoFactoryFacade);

        if (user == null) {
            // inconsistenza in persistenza
            recordFailedAttempt();
            throw new DAOException("CRITICAL: User validated but not found in specific table. Inconsistency!");
        }

        // Login successful - reset global failed attempts counter
        resetFailedAttempts();

        // Store the Model in session (internal to controller)
        storeUserSession(user);

        // Convert Model → Bean for boundary layer
        return convertUserToBean(user);
    }

    /**
     * Factory method that retrieves the appropriate user type based on credentials.
     * <p>
     * <strong>Polymorphism:</strong> Uses switch expression with enum to dispatch to the
     * appropriate DAO retrieval method. This provides type safety and compile-time exhaustiveness
     * checking compared to string-based if-else chains.
     * </p>
     *
     * @param credentials The user credentials containing the user type
     * @param factory The DAO factory for data access
     * @return The concrete User instance (Fan or VenueManager), or null
     * @throws DAOException If there is an error retrieving user data
     */
    private User retrieveUserByType(CredentialsBean credentials, DaoFactoryFacade factory)
            throws DAOException {

        String typeString = credentials.getType();
        if (typeString == null || typeString.isEmpty()) {
            return null;
        }

        // Convert String to UserType enum for type-safe dispatching
        UserType userType;
        try {
            userType = UserType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid user type string
            return null;
        }

        // POLYMORPHISM: Enum-based switch expression dispatches to the appropriate DAO method
        // at runtime based on the UserType enum value. This replaces traditional if-else chains
        // with a type-safe, exhaustive-checked approach that ensures all enum cases are handled.
        // The Java compiler verifies that all UserType enum values have a corresponding case.
        // Each case returns a different User subtype (Fan or VenueManager) polymorphically.
        return switch (userType) {
            case FAN -> factory.getFanDao().retrieveFan(credentials.getUsername());
            case VENUE_MANAGER -> factory.getVenueManagerDao().retrieveVenueManager(credentials.getUsername());
        };
    }

    /**
     * Checks if rate limiting is active due to excessive failed login attempts.
     * <p>
     * <strong>Global Rate Limiting:</strong> This applies to ALL login attempts, regardless
     * of username. After 3 failed attempts (even with different usernames), the system
     * enforces a delay. This prevents both brute-force attacks and username enumeration.
     * </p>
     * <p>
     * Rate limiting formula: After 3 failed attempts, subsequent attempts require a delay of
     * 30 * (attemptNumber - 3) seconds.
     * - Attempt 4: 30 seconds delay
     * - Attempt 5: 60 seconds delay
     * - Attempt 6: 90 seconds delay
     * - etc.
     * </p>
     *
     * @throws DAOException If the system is under rate limiting and user must wait
     */
    private synchronized void checkRateLimit() throws DAOException {
        if (globalFailedAttempts < MAX_ATTEMPTS_BEFORE_DELAY) {
            return; // No rate limiting yet (attempts 0, 1, 2 are allowed)
        }

        if (lastFailedAttemptTime == null) {
            return; // No attempts recorded
        }

        // Calculate required delay: 30 * (attemptNumber - 2) seconds
        // When counter = 3 (4th attempt), delay = 30 * 1 = 30 seconds
        // When counter = 4 (5th attempt), delay = 30 * 2 = 60 seconds
        // etc.
        int delaySeconds = BASE_DELAY_SECONDS * (globalFailedAttempts - MAX_ATTEMPTS_BEFORE_DELAY + 1);
        Instant now = Instant.now();
        Duration timeSinceLastAttempt = Duration.between(lastFailedAttemptTime, now);

        if (timeSinceLastAttempt.getSeconds() < delaySeconds) {
            long remainingSeconds = delaySeconds - timeSinceLastAttempt.getSeconds();
            throw new DAOException(String.format(
                    "Too many failed login attempts. Please wait %d seconds before trying again.",
                    remainingSeconds
            ));
        }

        // Delay has expired - allow the login attempt to proceed
        // Do NOT reset counter here - it will only reset on successful login
    }

    /**
     * Records a failed login attempt globally.
     * This increments the global counter regardless of which username was used.
     */
    private synchronized void recordFailedAttempt() {
        globalFailedAttempts++;
        lastFailedAttemptTime = Instant.now();
    }

    /**
     * Resets the global failed login attempts counter after successful login.
     */
    private synchronized void resetFailedAttempts() {
        globalFailedAttempts = 0;
        lastFailedAttemptTime = null;
    }

    /**
     * Stores user session information upon successful login.
     * This method ensures that the user's session is registered in the system,
     * allowing for session management and tracking.
     *
     * @param user The user for whom the session is to be stored.
     * @throws UserSessionException If there is an error in starting a new session, typically if the user is already logged in.
     */
    @Override
    protected void storeUserSession(User user) throws UserSessionException {
        SessionManager.INSTANCE.login(user);
    }

    /**
     * Converts a User Model object to the appropriate UserBean subtype for boundary layer consumption.
     * <p>
     * <strong>Polymorphism:</strong> Uses pattern matching with switch expression to determine
     * the runtime type of the User and create the appropriate Bean subtype (FanBean or VenueManagerBean).
     * This ensures type-specific data is properly transferred to the boundary layer.
     * </p>
     * <p>
     * This method extracts only the necessary data from the Model and packages it
     * into a Bean, preventing the boundary layer from accessing business logic methods.
     * </p>
     *
     * @param user The User Model object to convert (Fan or VenueManager)
     * @return A UserBean subtype containing only data (no business logic)
     */
    private UserBean convertUserToBean(User user) {
        return new UserBean.Builder<>()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .type(user.getUserType().toString())
                .build();
    }
}
