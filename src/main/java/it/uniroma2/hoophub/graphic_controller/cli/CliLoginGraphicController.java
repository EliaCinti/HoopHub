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
 *
 * <p>Handles user authentication and navigation to the appropriate homepage.
 * Uses polymorphism to route to Fan or VenueManager dashboard based on user type.
 * Rate limiting is delegated entirely to {@link LoginController}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController
 */
public class CliLoginGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliLoginGraphicController.class.getName());

    private static final String EXIT_COMMAND = "exit";
    private static final String QUIT_COMMAND = "quit";
    private static final String SIGNUP_COMMAND = "signup";

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
    private static final String LOGOUT_SUCCESS_MSG = "Logged out successfully";
    private static final String LOGOUT_ERROR_MSG = "Error during logout: %s";
    private static final String SIGNUP_OPTION_MSG = "Don't have an account? Type 'signup' to create one";
    private static final String EXIT_OPTION_MSG = "Type 'exit' or 'quit' to close the application";

    private final LoginController loginController;
    private boolean userRequestedExit = false;

    public CliLoginGraphicController() {
        this.loginController = new LoginController();
    }

    /**
     * Executes the login flow until successful or user exits.
     */
    @Override
    public void execute() {
        while (!userRequestedExit) {
            Optional<UserBean> loggedUser = performLogin();
            if (loggedUser.isPresent()) {
                navigateToHomepage(loggedUser.get());
                break;
            }
        }
    }

    /**
     * Performs login with input validation.
     *
     * @return authenticated UserBean or empty if canceled/rate-limited
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
                return Optional.empty();
            }

            Optional<String> password = readPassword();
            if (password.isEmpty()) {
                continue;
            }

            try {
                Optional<UserBean> loginResult = attemptLogin(username.get(), password.get());
                if (loginResult.isPresent()) {
                    return loginResult;
                }
            } catch (RateLimitException e) {
                return Optional.empty();
            }

            printInfo(RETRY_MSG);
            printNewLine();
        }
    }

    private Optional<String> readUsername() {
        String username = readInput(USERNAME_PROMPT);

        if (isExitCommand(username)) {
            printInfo(LOGIN_CANCELLED_MSG);
            userRequestedExit = true;
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

    private Optional<String> readPassword() {
        String password = readInput(PASSWORD_PROMPT);

        if (password.isEmpty()) {
            printWarning(EMPTY_PASSWORD_MSG);
            return Optional.empty();
        }

        return Optional.of(password);
    }

    /**
     * Attempts authentication with given credentials.
     *
     * @throws RateLimitException if rate limiting is active
     */
    private Optional<UserBean> attemptLogin(String username, String password) {
        try {
            CredentialsBean credentials = buildCredentials(username, password);
            UserBean loggedUser = loginController.login(credentials);

            displayLoginSuccess(loggedUser);
            return Optional.of(loggedUser);

        } catch (DAOException e) {
            boolean isRateLimited = handleDAOException(username, e);
            if (isRateLimited) {
                throw new RateLimitException();
            }
        } catch (UserSessionException e) {
            handleSessionException(username, e);
        }

        return Optional.empty();
    }

    /** Internal exception for rate limit flow control. */
    private static class RateLimitException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private CredentialsBean buildCredentials(String username, String password) {
        return new CredentialsBean.Builder<>()
                .username(username)
                .password(password)
                .build();
    }

    private void displayLoginSuccess(UserBean userBean) {
        printNewLine();
        printSuccess(String.format(LOGIN_SUCCESS_MSG, userBean.getFullName()));
        printInfo(String.format(USER_TYPE_MSG, userBean.getType()));
        printNewLine();
    }

    private boolean handleDAOException(String username, DAOException e) {
        LOGGER.log(Level.FINE, "Login failed for user: {0} - {1}", new Object[]{username, e.getMessage()});

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

    private void handleSessionException(String username, UserSessionException e) {
        LOGGER.log(Level.INFO, e, () -> "User already logged in: " + username);
        printError(USER_ALREADY_LOGGED_MSG);
        printInfo(LOGOUT_FIRST_MSG);
        printNewLine();
    }

    /**
     * Navigates to appropriate homepage based on user type.
     */
    private void navigateToHomepage(UserBean userBean) {
        printNewLine();
        printInfo(String.format(LOADING_DASHBOARD_MSG, userBean.getType()));

        try {
            UserType userType = userBean.getType();

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

    private void navigateToFanHomepage() {
        CliFanHomepageGraphicController fanHomepage = new CliFanHomepageGraphicController();
        fanHomepage.execute();
    }

    private void navigateToVenueManagerHomepage() {
        CliVenueManagerHomepageGraphicController vmHomepage = new CliVenueManagerHomepageGraphicController();
        vmHomepage.execute();
    }

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

    private void navigateToSignUp() {
        clearScreen();
        CliSignUpGraphicController signUpController = new CliSignUpGraphicController();
        signUpController.execute();
    }

    private boolean isExitCommand(String input) {
        return EXIT_COMMAND.equalsIgnoreCase(input) ||
                QUIT_COMMAND.equalsIgnoreCase(input);
    }
}