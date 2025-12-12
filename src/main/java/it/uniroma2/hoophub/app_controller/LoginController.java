package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Credentials;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.model.VenueManager;
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
     * Esegue il login completo.
     * @return UserBean popolato con i dati dell'utente loggato (per la GUI).
     */
    public UserBean login(CredentialsBean bean) throws DAOException, UserSessionException {
        // 1. Rate Limiting Check
        checkRateLimit();

        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        UserDao userDao = daoFactory.getUserDao();

        try {
            // 2. Bean -> Model (Credentials)
            Credentials credentials = new Credentials.Builder()
                    .username(bean.getUsername())
                    .password(bean.getPassword())
                    .build();

            // 3. Validazione (Model-First) -> Restituisce UserType
            UserType userType = userDao.validateUser(credentials);

            // 4. Caricamento Profilo Completo
            User user = null;
            if (userType == UserType.FAN) {
                user = daoFactory.getFanDao().retrieveFan(credentials.getUsername());
            } else if (userType == UserType.VENUE_MANAGER) {
                user = daoFactory.getVenueManagerDao().retrieveVenueManager(credentials.getUsername());
            }

            if (user == null) {
                throw new DAOException("User profile corrupted or not found: " + credentials.getUsername());
            }

            // 5. Successo: Reset tentativi e Login in Sessione
            resetFailedAttempts();
            storeUserSession(user);

            // 6. Restituzione Bean alla GUI (così la GUI non deve chiamare SessionManager)
            return convertUserToBean(user);

        } catch (DAOException | IllegalArgumentException e) {
            // Fallimento: Registra tentativo e rilancia
            recordFailedAttempt();
            throw e;
        }
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
     * Converte il Model in Bean specifico (FanBean o VenueManagerBean).
     * Questo permette alla GUI di avere tutti i dati specifici (es. Squadra del cuore)
     * senza dover conoscere il Model.
     */
    private UserBean convertUserToBean(User user) {
        return switch (user) {
            case Fan fan -> new FanBean.Builder()
                    .username(fan.getUsername())
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .type(UserType.FAN)
                    .favTeam(fan.getFavTeam())
                    .birthday(fan.getBirthday())
                    .build();

            case VenueManager vm -> new VenueManagerBean.Builder()
                    .username(vm.getUsername())
                    .fullName(vm.getFullName())
                    .gender(vm.getGender())
                    .type(UserType.VENUE_MANAGER)
                    .companyName(vm.getCompanyName())
                    .phoneNumber(vm.getPhoneNumber())
                    .build();

            case null -> throw new IllegalArgumentException("User cannot be null");
            default -> throw new IllegalStateException("Unexpected value: " + user);
        };
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
}
