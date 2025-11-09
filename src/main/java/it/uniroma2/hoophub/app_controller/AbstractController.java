package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;

/**
 * AbstractController serves as a base class for controllers within the HoopHub application.
 * This class defines a common framework for session management across different types of controllers.
 */
public abstract class AbstractController {
    /**
     * Abstract method to store user session information.
     * This method must be implemented by subclasses to handle the specifics of user session storage,
     * which is essential for maintaining user state during the application lifecycle.
     *
     * @param user The user whose session needs to be stored.
     * @throws UserSessionException If there is an issue storing the session, such as a duplicate session.
     */
    protected abstract void storeUserSession(User user) throws UserSessionException;
}
