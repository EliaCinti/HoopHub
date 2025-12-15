package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.LoginController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.enums.UserType;

import java.io.Serial;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the login use case.
 * Parallel to {@link it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController} for GUI.
 * Use POLYMORPHISM to navigate to the appropriate home screen based on user type.
 */
public class CliLoginGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliLoginGraphicController.class.getName());

    // Constants for commands and limits
    private static final String EXIT_COMMAND = "exit";
    private static final String QUIT_COMMAND = "quit";
    private static final String SIGNUP_COMMAND = "signup";

    // Message constants
    private static final String TITLE = "HOOPHUB - LOGIN";
    private static final String USERNAME_PROMPT = "Username: ";
    private static final String PASSWORD_PROMPT = "Password: ";
    private static final String EMPTY_USERNAME_MSG = "Username cannot be empty";
    private static final String EMPTY_PASSWORD_MSG = "Password cannot be empty";
    private static final String LOGIN_CANCELLED_MSG = "Login cancelled";
    private static final String LOGIN_SUCCESS_MSG = "Login successful! Welcome, %s";
    private static final String USER_TYPE_MSG = "User type: %s";
    private static final String LOGIN_FAILED_MSG = "Login failed: %s";
    private static final String RETRY_MSG = "Please try again or type 'exit' to quit";
    private static final String USER_ALREADY_LOGGED_MSG = "This user is already logged in";
    private static final String LOGOUT_FIRST_MSG = "Please logout first or try another account";
    private static final String LOADING_DASHBOARD_MSG = "Loading %s dashboard...";
    private static final String DASHBOARD_NOT_IMPLEMENTED_MSG = "Note: Dashboard graphic controllers not yet implemented";
    private static final String LOGGING_OUT_MSG = "Logging out...";
    private static final String LOGOUT_SUCCESS_MSG = "Logged out successfully";
    private static final String LOGOUT_ERROR_MSG = "Error during logout: %s";
    private static final String SIGNUP_OPTION_MSG = "Don't have an account? Type 'signup' to create one";
    private static final String EXIT_OPTION_MSG = "Type 'exit' or 'quit' to close the application";

    private final LoginController loginController;

    public CliLoginGraphicController() {
        this.loginController = new LoginController();
    }

    private boolean userRequestedExit = false;

    /**
     * Executes the complete login use case.
     * Keeps showing login screen until successful login or user exits.
     */
    @Override
    public void execute() {
        while (!userRequestedExit) {
            Optional<UserBean> loggedUser = performLogin();
            if (loggedUser.isPresent()) {
                navigateToHomepage(loggedUser.get());
                break; // Exit loop after successful login and homepage navigation
            }
            // If empty and user didn't request exit, continue loop (e.g., came back from signup)
        }
    }

    /**
     * Performs the login operation with improved input validation.
     * <p>
     * <strong>Bean Pattern:</strong> Returns UserBean (not Model) to prevent CLI boundary
     * from accessing business logic methods.
     * </p>
     * <p>
     * <strong>Rate Limiting:</strong> No attempt limit at UI level. Rate limiting is handled
     * entirely by the LoginController (application layer), which enforces global rate limits
     * after 3 failed attempts. This ensures consistent rate limiting across all UIs.
     * </p>
     *
     * @return Optional containing the authenticated UserBean, or empty if login is canceled or rate limited
     */
    private Optional<UserBean> performLogin() {
        clearScreen();
        printTitle(TITLE);
        printInfo(SIGNUP_OPTION_MSG);
        printInfo(EXIT_OPTION_MSG);
        printNewLine();

        while (true) {
            printNewLine();

            Optional<String> username = readUsername();
            if (username.isEmpty()) {
                return Optional.empty(); // User cancelled
            }

            Optional<String> password = readPassword();
            if (password.isEmpty()) {
                continue; // Retry with new username
            }

            try {
                Optional<UserBean> loginResult = attemptLogin(username.get(), password.get());
                if (loginResult.isPresent()) {
                    return loginResult;
                }
            } catch (RateLimitException e) {
                // Rate limit is active - exit immediately and return to main loop
                // User can only exit or wait for the rate limit to expire
                return Optional.empty();
            }

            // Login failed but no rate limit - allow retry
            printInfo(RETRY_MSG);
            printNewLine();
        }
    }

    /**
     * Reads and validates username input.
     *
     * @return Optional containing the username, or empty if user wants to exit
     */
    private Optional<String> readUsername() {
        String username = readInput(USERNAME_PROMPT);

        if (isExitCommand(username)) {
            printInfo(LOGIN_CANCELLED_MSG);
            userRequestedExit = true; // Set flag to exit main loop
            return Optional.empty();
        }

        if (SIGNUP_COMMAND.equalsIgnoreCase(username)) {
            navigateToSignUp();
            return Optional.empty();
        }

        if (username.isEmpty()) {
            printWarning(EMPTY_USERNAME_MSG);
            return Optional.empty();
        }

        return Optional.of(username);
    }

    /**
     * Reads and validates password input.
     *
     * @return Optional containing the password, or empty if validation fails
     */
    private Optional<String> readPassword() {
        String password = readInput(PASSWORD_PROMPT);

        if (password.isEmpty()) {
            printWarning(EMPTY_PASSWORD_MSG);
            return Optional.empty();
        }

        return Optional.of(password);
    }

    /**
     * Attempts to authenticate user with given credentials.
     *
     * @param username The username
     * @param password The password
     * @return Optional containing the authenticated UserBean, or empty if authentication fails
     * @throws RateLimitException if rate limiting is active (wrapped in RuntimeException for flow control)
     */
    private Optional<UserBean> attemptLogin(String username, String password) {
        try {
            CredentialsBean credentials = buildCredentials(username, password);
            UserBean loggedUser = loginController.login(credentials);

            displayLoginSuccess(loggedUser);
            logSuccessfulLogin(username, loggedUser);

            return Optional.of(loggedUser);

        } catch (DAOException e) {
            boolean isRateLimited = handleDAOException(username, e);
            if (isRateLimited) {
                // Throw unchecked exception to break out of attempt loop
                throw new RateLimitException();
            }
        } catch (UserSessionException e) {
            handleSessionException(username, e);
        }

        return Optional.empty();
    }

    /**
     * Unchecked exception used internally to signal rate limiting.
     * This breaks out of the login attempt loop when the rate limit is active.
     */
    private static class RateLimitException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Builds credentials bean from username and password.
     */
    private CredentialsBean buildCredentials(String username, String password) {
        return new CredentialsBean.Builder<>()
                .username(username)
                .password(password)
                // .type(null) // Non settiamo il tipo qui, è null al login
                .build();
    }

    /**
     * Displays success message after login.
     */
    private void displayLoginSuccess(UserBean userBean) {
        printNewLine();
        printSuccess(String.format(LOGIN_SUCCESS_MSG, userBean.getFullName()));
        printInfo(String.format(USER_TYPE_MSG, userBean.getType()));
        printNewLine();
    }

    /**
     * Logs successful login attempt.
     */
    private void logSuccessfulLogin(String username, UserBean userBean) {
        LOGGER.log(Level.INFO, "User logged in via CLI: {0} ({1})",
                new Object[]{username, userBean.getType()});
    }

    /**
     * Handles DAO exceptions during login.
     *
     * @param username The username that failed
     * @param e The exception
     * @return true if this is a rate limiting exception (user must wait), false otherwise
     */
    private boolean handleDAOException(String username, DAOException e) {
        LOGGER.log(Level.FINE, "Login failed for user: {0} - {1}", new Object[]{username, e.getMessage()});

        // Check if this is a rate limiting error
        boolean isRateLimited = e.getMessage() != null && e.getMessage().contains("Too many failed login attempts");

        if (isRateLimited) {
            printNewLine();
            printError(e.getMessage());
            printWarning("You cannot attempt login until the waiting period expires.");
            printInfo("You can only type 'exit' or 'quit' to close the application, or wait.");
            printNewLine();
        } else {
            printError(String.format(LOGIN_FAILED_MSG, e.getMessage()));
        }

        return isRateLimited;
    }

    /**
     * Handles session exceptions during login.
     */
    private void handleSessionException(String username, UserSessionException e) {
        LOGGER.log(Level.INFO, e, () -> "User already logged in: " + username);        printError(USER_ALREADY_LOGGED_MSG);
        printInfo(LOGOUT_FIRST_MSG);
        printNewLine();
    }

    /**
     * Navigates to the appropriate homepage based on user type.
     * <p>
     * <strong>Bean Pattern:</strong> This method works with UserBean (data only),
     * accessing only the 'type' field without any business logic methods.
     * </p>
     *
     * @param userBean The authenticated user data (Bean, not Model)
     */
    private void navigateToHomepage(UserBean userBean) {
        printNewLine();
        printInfo(String.format(LOADING_DASHBOARD_MSG, userBean.getType()));

        try {
            // FIX: Rimosso UserType.valueOf(...). userBean.getType() è già un Enum!
            UserType userType = userBean.getType();

            // Navigate based on user type
            if (userType == UserType.FAN) {
                navigateToFanHomepage();
            } else if (userType == UserType.VENUE_MANAGER) {
                navigateToVenueManagerHomepage();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during post-login navigation", e);
            printError("An unexpected error occurred during navigation");
            performLogout();
        }
    }

    /**
     * Navigates to Fan homepage.
     * TODO: Implement CliFanHomeGraphicController
     */
    private void navigateToFanHomepage() {
        printWarning(DASHBOARD_NOT_IMPLEMENTED_MSG);
        printInfo(LOGGING_OUT_MSG);
        printNewLine();
        performLogout();
    }

    /**
     * Navigates to VenueManager homepage.
     * TODO: Implement CliVenueManagerHomeGraphicController
     */
    private void navigateToVenueManagerHomepage() {
        printWarning(DASHBOARD_NOT_IMPLEMENTED_MSG);
        printInfo(LOGGING_OUT_MSG);
        printNewLine();
        performLogout();
    }

    /**
     * Performs logout operation with proper error handling.
     */
    private void performLogout() {
        try {
            SessionManager.INSTANCE.logout();
            printSuccess(LOGOUT_SUCCESS_MSG);
            printNewLine();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Critical error during logout", e);
            printWarning(String.format(LOGOUT_ERROR_MSG, e.getMessage()));
        }
    }

    /**
     * Navigates to the sign-up screen.
     */
    private void navigateToSignUp() {
        clearScreen();
        CliSignUpGraphicController signUpController = new CliSignUpGraphicController();
        signUpController.execute();
    }

    /**
     * Checks if input is an exit command.
     *
     * @param input The user input
     * @return true if input is an exit command, false otherwise
     */
    private boolean isExitCommand(String input) {
        return EXIT_COMMAND.equalsIgnoreCase(input) ||
                QUIT_COMMAND.equalsIgnoreCase(input);
    }
}