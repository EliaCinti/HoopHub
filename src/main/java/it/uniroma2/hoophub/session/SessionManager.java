package it.uniroma2.hoophub.session;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.exception.UserSessionException;

/**
 * The {@code SessionManager} enum is a thread-safe singleton that manages the current user's session.
 * <p>
 * It provides methods to log in and log out the user, as well as to query the current session status.
 * This allows different parts of the application to access the logged-in user's information
 * without having to pass the user object between controllers.
 * </p>
 * <p>
 * <strong>Singleton Pattern Justification:</strong>
 * The Singleton pattern is REQUIRED here because the application needs exactly one shared session
 * across all controllers and UI components. Multiple instances would create separate, isolated
 * sessions, breaking the fundamental requirement of having a single, application-wide user session.
 * </p>
 * <p>
 * This implementation uses the Enum Singleton pattern (Effective Java, Joshua Bloch) which provides:
 * <ul>
 *   <li>Thread safety without explicit synchronization</li>
 *   <li>Protection against serialization attacks</li>
 *   <li>Protection against reflection attacks</li>
 *   <li>Guaranteed single instance</li>
 * </ul>
 * </p>
 */
@SuppressWarnings("java:S6548") // Singleton is required for shared application-wide session state
public enum SessionManager {
    INSTANCE;

    private UserBean currentUser;

    public void login(UserBean user) throws UserSessionException {
        if (currentUser != null) {
            throw new UserSessionException("User already logged in.");
        }
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
        GlobalCache.getInstance().clearAll();
    }

    public UserBean getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}