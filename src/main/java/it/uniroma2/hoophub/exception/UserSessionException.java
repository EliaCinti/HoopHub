package it.uniroma2.hoophub.exception;

import java.io.Serial;

/**
 * Exception thrown when user session operations fail or are invalid.
 * <p>
 * This exception is used throughout the HoopHub application to indicate
 * problems with user session management, such as attempting to perform
 * operations when no user is logged in, trying to log in when a user is
 * already authenticated, or session-related validation failures.
 * </p>
 * <p>
 * Common scenarios that trigger this exception:
 * <ul>
 *   <li>Attempting to access user-specific functionality without being logged in</li>
 *   <li>Trying to log in when a user session already exists</li>
 *   <li>Session validation failures during sensitive operations</li>
 *   <li>Concurrent session conflicts in multi-user scenarios</li>
 * </ul>
 * </p>
 * <p>
 * This exception is typically caught by controllers and UI components to
 * provide appropriate user feedback and redirect to authentication screens
 * when necessary.
 * </p>
 *
 * @see it.uniroma2.hoophub.session.SessionManager for session management
 */
public class UserSessionException extends Exception {

    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Constructs a new UserSessionException with a default error message.
     * <p>
     * This constructor creates an exception with a standard message indicating
     * that no user has been defined or logged into the current session.
     * It's typically used when operations require an authenticated user
     * but no session exists.
     * </p>
     */
    public UserSessionException() {
        super("The user hasn't been defined yet");
    }

    /**
     * Constructs a new UserSessionException with the specified detail message.
     * <p>
     * This constructor allows for custom error messages that can provide
     * more specific information about the session-related problem that occurred.
     * The message is useful for debugging and providing contextual feedback
     * to users or developers.
     * </p>
     *
     * @param message the detail message explaining the specific session issue
     */
    public UserSessionException(String message) {
        super(message);
    }
}
