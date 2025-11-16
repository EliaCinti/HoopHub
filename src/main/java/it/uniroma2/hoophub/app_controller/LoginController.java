package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.model.UserType;

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
     * <strong>Bean Pattern:</strong> This method accepts a CredentialsBean (input) and returns
     * a UserBean (output), ensuring the boundary layer never accesses business logic from Model objects.
     * Internally, the controller works with Model objects and converts them to Beans for the boundary.
     * </p>
     *
     * @param credentials The credentials provided by the user, containing username, password, and user type.
     * @return A UserBean containing user data without business logic, for boundary layer use.
     * @throws DAOException         If there is an issue with data access, such as invalid user type or database errors.
     * @throws UserSessionException If the user is already logged in elsewhere, preventing a new session start.
     */
    public UserBean login(CredentialsBean credentials) throws DAOException, UserSessionException {

        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();
        UserDao userDao = daoFactoryFacade.getUserDao();
        userDao.validateUser(credentials);

        User user = retrieveUserByType(credentials, daoFactoryFacade);

        if (user == null) {
            // inconsistenza in persistenza
            throw new DAOException("CRITICAL: User validated but not found in specific table. Inconsistency!");
        }

        // Store the Model in session (internal to controller)
        storeUserSession(user);

        // Convert Model → Bean for boundary layer
        return convertUserToBean(user);
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
        SessionManager.INSTANCE.login(user);
    }

    /**
     * Converts a User Model object to a UserBean for boundary layer consumption.
     * <p>
     * This method extracts only the necessary data from the Model and packages it
     * into a Bean, preventing the boundary layer from accessing business logic methods.
     * </p>
     *
     * @param user The User Model object to convert
     * @return A UserBean containing only data (no business logic)
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
