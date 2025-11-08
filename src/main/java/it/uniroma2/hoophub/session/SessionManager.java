package it.uniroma2.hoophub.session;

import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;

/**
 * The {@code SessionManager} class manages the current user's session.
 * <p>
 * It provides methods to log in and log out the user, as well as to query the current session status.
 * This allows different parts of the application to access the logged-in user's information
 * without having to pass the user object between controllers.
 * </p>
 * <p>
 * This implementation uses instance-based design instead of Singleton pattern,
 * improving testability and eliminating SonarQube code smells. Each controller
 * that needs session management should maintain its own SessionManager instance.
 * </p>
 */
public class SessionManager {

    private User currentUser;

    /**
     * Logs in a user by setting the current user for the session.
     * <p>
     * Optionally, you can add logic here to check if a user is already logged in
     * and throw a dedicated session exception if needed.
     * </p>
     *
     * @param user The {@link User} to log in.
     */
    public void login(User user) throws UserSessionException {
        if (currentUser != null) {
            throw new UserSessionException("A user is already logged in.");
        }
        this.currentUser = user;
    }

    /**
     * Logs out the current user by clearing the session.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Returns the currently logged-in user.
     *
     * @return The {@link User} currently logged in, or {@code null} if no user is logged in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return {@code true} if a user is logged in; {@code false} otherwise.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
