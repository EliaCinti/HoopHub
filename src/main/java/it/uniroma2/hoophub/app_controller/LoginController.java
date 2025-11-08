package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.UserType;

/**
 * LoginController manages the authentication process for users trying to log in to the MindHarbor application.
 * This class interacts with various data access objects (DAOs)
 * to validate user credentials and retrieve user information.
 * <p>
 * Uses polymorphism to handle different user types (Fan, VenueManager) uniformly.
 * </p>
 * <p>
 * Session management is handled through a dedicated SessionManager instance,
 * following dependency injection principles for better testability.
 * </p>
 */
public class LoginController extends AbstractController {

    private final SessionManager sessionManager;

    /**
     * Constructs a new LoginController with default SessionManager.
     * <p>
     * This constructor creates a new SessionManager instance for handling
     * user sessions. For testing purposes, use {@link #LoginController(SessionManager)}
     * to inject a mock SessionManager.
     * </p>
     */
    public LoginController() {
        this.sessionManager = new SessionManager();
    }

    /**
     * Constructs a new LoginController with a custom SessionManager.
     * <p>
     * This constructor allows dependency injection of a SessionManager instance,
     * which is useful for testing with mock objects.
     * </p>
     *
     * @param sessionManager The SessionManager instance to use for session handling
     */
    public LoginController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Attempts to log in a user using the provided credentials.
     * It determines the user type, validates the credentials, retrieves the corresponding user data,
     * and initiates a session for the user if authentication is successful.
     *
     * @param credentials The credentials provided by the user, containing username, password, and user type.
     * @return A User object representing the logged-in user, or null if authentication fails.
     * @throws DAOException         If there is an issue with data access, such as invalid user type or database errors.
     * @throws UserSessionException If the user is already logged in elsewhere, preventing a new session start.
     */
    public User login(CredentialsBean credentials) throws DAOException, UserSessionException {

        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();
        UserDao userDao = daoFactoryFacade.getUserDao();
        userDao.validateUser(credentials);

        User user = retrieveUserByType(credentials, daoFactoryFacade);

        if (user == null) {
            // inconsistenza in persistenza
            throw new DAOException("CRITICAL: User validated but not found in specific table. Inconsistency!");
        }
        storeUserSession(user);
        return user;
    }

    /**
     * Factory method that retrieves the appropriate user type based on credentials.
     * Encapsulates the type-checking logic.
     *
     * @param credentials The user credentials containing the user type
     * @param factory The DAO factory for data access
     * @return The concrete User instance (Fan or VenueManager), or null
     * @throws DAOException If there is an error retrieving user data
     */
    private User retrieveUserByType(CredentialsBean credentials, DaoFactoryFacade factory)
            throws DAOException {

        String type = credentials.getType();
        if (type == null) {
            return null;
        }

        if (type.equalsIgnoreCase(String.valueOf(UserType.FAN))) {
            return factory.getFanDao().retrieveFan(credentials.getUsername());
        } else if (type.equalsIgnoreCase(String.valueOf(UserType.VENUE_MANAGER))) {
            return factory.getVenueManagerDao().retrieveVenueManager(credentials.getUsername());
        }

        return null;
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
        sessionManager.login(user);
    }
}
