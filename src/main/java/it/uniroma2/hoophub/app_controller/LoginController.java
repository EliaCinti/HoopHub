package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.UserType;

/**
 * LoginController manages the authentication process for users trying to log in to the MindHarbor application.
 * This class interacts with various data access objects (DAOs)
 * to validate user credentials and retrieve user information.
 */
public class LoginController extends AbstractController {

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

        if (credentials.getType() != null) {
            if (credentials.getType().equalsIgnoreCase(String.valueOf(UserType.FAN))) {
                // Handle login for a fan
                FanDao fanDao = daoFactoryFacade.getFanDao();
                Fan fan = fanDao.retrieveFan(credentials.getUsername());
                storeUserSession(fan);
                return fan;
            } else {
                // Handle login for a venueManager
                VenueManagerDao venueManagerDao = daoFactoryFacade.getVenueManagerDao();
                VenueManager venueManager = venueManagerDao.retrieveVenueManager(credentials.getUsername());
                storeUserSession(venueManager);
                return venueManager;
            }
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
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.login(user);
    }
}
