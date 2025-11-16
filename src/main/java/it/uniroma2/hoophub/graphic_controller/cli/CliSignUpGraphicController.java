package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.TeamNBA;
import it.uniroma2.hoophub.model.UserType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the sign-up use case.
 * <p>
 * Handles new user registration for Fans. After successful registration,
 * the user is automatically logged in and navigated to their homepage.
 * </p>
 */
public class CliSignUpGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliSignUpGraphicController.class.getName());

    // Constants for commands
    private static final String BACK_COMMAND = "back";
    private static final String CANCEL_COMMAND = "cancel";

    // Message constants
    private static final String TITLE = "HOOPHUB - SIGN UP";
    private static final String SUBTITLE = "Create your Fan account";
    private static final String USERNAME_PROMPT = "Username (min 3 characters): ";
    private static final String PASSWORD_PROMPT = "Password (min 6 characters): ";
    private static final String CONFIRM_PASSWORD_PROMPT = "Confirm password: ";
    private static final String FULL_NAME_PROMPT = "Full name (e.g., John Doe): ";
    private static final String GENDER_PROMPT = "Gender (M/F/Other): ";
    private static final String FAVORITE_TEAM_PROMPT = "Favorite NBA team (e.g., Los Angeles Lakers): ";
    private static final String BIRTHDAY_PROMPT = "Birthday (YYYY-MM-DD): ";

    private static final String EMPTY_INPUT_MSG = "Input cannot be empty";
    private static final String INVALID_USERNAME_MSG = "Username must be at least 3 characters";
    private static final String INVALID_PASSWORD_MSG = "Password must be at least 6 characters";
    private static final String PASSWORD_MISMATCH_MSG = "Passwords do not match";
    private static final String INVALID_GENDER_MSG = "Please enter M, F, or Other";
    private static final String INVALID_TEAM_MSG = "Team not found. Please enter a valid NBA team name";
    private static final String INVALID_DATE_MSG = "Invalid date format. Please use YYYY-MM-DD";
    private static final String SIGNUP_CANCELLED_MSG = "Sign up cancelled";
    private static final String SIGNUP_SUCCESS_MSG = "Account created successfully! Welcome, %s";
    private static final String SIGNUP_FAILED_MSG = "Sign up failed: %s";
    private static final String LOGIN_OPTION_MSG = "Already have an account? Type 'back' to return to login";
    private static final String CANCEL_INFO_MSG = "Type 'cancel' at any time to abort registration";
    private static final String LOADING_DASHBOARD_MSG = "Loading your dashboard...";

    private final SignUpController signUpController;
    private final CliLoginGraphicController loginController;

    public CliSignUpGraphicController() {
        this.signUpController = SignUpController.getInstance();
        this.loginController = new CliLoginGraphicController();
    }

    /**
     * Executes the complete sign-up use case.
     * If the user types 'back', returns to login screen.
     */
    @Override
    public void execute() {
        Optional<UserBean> registeredUser = performSignUp();

        if (registeredUser.isPresent()) {
            navigateToHomepage(registeredUser.get());
        }
        // If empty, either cancelled or chose to go back to login
    }

    /**
     * Performs the sign-up operation with input validation.
     *
     * @return Optional containing the registered UserBean, or empty if cancelled
     */
    private Optional<UserBean> performSignUp() {
        printTitle(TITLE);
        printInfo(SUBTITLE);
        printNewLine();
        printInfo(LOGIN_OPTION_MSG);
        printInfo(CANCEL_INFO_MSG);
        printNewLine();

        // Collect user input
        Optional<String> username = readUsername();
        if (username.isEmpty()) return handleBackOrCancel();

        Optional<String> password = readPassword();
        if (password.isEmpty()) return handleBackOrCancel();

        Optional<String> fullName = readFullName();
        if (fullName.isEmpty()) return handleBackOrCancel();

        Optional<String> gender = readGender();
        if (gender.isEmpty()) return handleBackOrCancel();

        Optional<TeamNBA> favTeam = readFavoriteTeam();
        if (favTeam.isEmpty()) return handleBackOrCancel();

        Optional<LocalDate> birthday = readBirthday();
        if (birthday.isEmpty()) return handleBackOrCancel();

        // Build FanBean and attempt registration
        return attemptSignUp(username.get(), password.get(), fullName.get(),
                gender.get(), favTeam.get(), birthday.get());
    }

    /**
     * Handles the back/cancel command by returning to login.
     */
    private Optional<UserBean> handleBackOrCancel() {
        printInfo(SIGNUP_CANCELLED_MSG);
        printNewLine();
        loginController.execute();
        return Optional.empty();
    }

    /**
     * Reads and validates username input.
     */
    private Optional<String> readUsername() {
        while (true) {
            String username = readInput(USERNAME_PROMPT);

            if (isBackOrCancelCommand(username)) {
                return Optional.empty();
            }

            if (username.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            if (username.length() < 3) {
                printWarning(INVALID_USERNAME_MSG);
                continue;
            }

            return Optional.of(username);
        }
    }

    /**
     * Reads and validates password input.
     */
    private Optional<String> readPassword() {
        while (true) {
            String password = readInput(PASSWORD_PROMPT);

            if (isBackOrCancelCommand(password)) {
                return Optional.empty();
            }

            if (password.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            if (password.length() < 6) {
                printWarning(INVALID_PASSWORD_MSG);
                continue;
            }

            // Confirm password
            String confirmPassword = readInput(CONFIRM_PASSWORD_PROMPT);

            if (isBackOrCancelCommand(confirmPassword)) {
                return Optional.empty();
            }

            if (!password.equals(confirmPassword)) {
                printWarning(PASSWORD_MISMATCH_MSG);
                continue;
            }

            return Optional.of(password);
        }
    }

    /**
     * Reads and validates full name input.
     */
    private Optional<String> readFullName() {
        while (true) {
            String fullName = readInput(FULL_NAME_PROMPT);

            if (isBackOrCancelCommand(fullName)) {
                return Optional.empty();
            }

            if (fullName.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            return Optional.of(fullName);
        }
    }

    /**
     * Reads and validates gender input.
     */
    private Optional<String> readGender() {
        while (true) {
            String gender = readInput(GENDER_PROMPT);

            if (isBackOrCancelCommand(gender)) {
                return Optional.empty();
            }

            if (gender.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            // Normalize gender input
            String normalizedGender = normalizeGender(gender);
            if (normalizedGender == null) {
                printWarning(INVALID_GENDER_MSG);
                continue;
            }

            return Optional.of(normalizedGender);
        }
    }

    /**
     * Normalizes gender input to standard format.
     */
    private String normalizeGender(String gender) {
        String lower = gender.toLowerCase();
        if ("m".equals(lower) || "male".equals(lower)) {
            return "Male";
        } else if ("f".equals(lower) || "female".equals(lower)) {
            return "Female";
        } else if ("other".equals(lower)) {
            return "Other";
        }
        return null;
    }

    /**
     * Reads and validates favorite team input.
     */
    private Optional<TeamNBA> readFavoriteTeam() {
        while (true) {
            String teamInput = readInput(FAVORITE_TEAM_PROMPT);

            if (isBackOrCancelCommand(teamInput)) {
                return Optional.empty();
            }

            if (teamInput.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            // Try to find team by display name
            TeamNBA team = TeamNBA.fromDisplayName(teamInput);
            if (team != null) {
                return Optional.of(team);
            }

            printWarning(INVALID_TEAM_MSG);
            printInfo("Available teams: Lakers, Warriors, Celtics, Heat, Bulls, etc.");
        }
    }

    /**
     * Reads and validates birthday input.
     */
    private Optional<LocalDate> readBirthday() {
        while (true) {
            String birthdayInput = readInput(BIRTHDAY_PROMPT);

            if (isBackOrCancelCommand(birthdayInput)) {
                return Optional.empty();
            }

            if (birthdayInput.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            try {
                LocalDate birthday = LocalDate.parse(birthdayInput);
                return Optional.of(birthday);
            } catch (DateTimeParseException e) {
                printWarning(INVALID_DATE_MSG);
            }
        }
    }

    /**
     * Attempts to register the user with the collected data.
     */
    private Optional<UserBean> attemptSignUp(String username, String password, String fullName,
                                             String gender, TeamNBA favTeam, LocalDate birthday) {
        try {
            FanBean fanBean = new FanBean.Builder()
                    .username(username)
                    .password(password)
                    .fullName(fullName)
                    .gender(gender)
                    .type(UserType.FAN.toString())
                    .favTeam(favTeam)
                    .birthday(birthday)
                    .build();

            // Register and auto-login
            UserBean registeredUser = signUpController.signUpFan(fanBean, true);

            displaySignUpSuccess(registeredUser);
            logSuccessfulSignUp(username);

            return Optional.of(registeredUser);

        } catch (DAOException e) {
            handleDAOException(username, e);
        } catch (UserSessionException e) {
            handleSessionException(username, e);
        } catch (IllegalArgumentException e) {
            printError(SIGNUP_FAILED_MSG.formatted(e.getMessage()));
        }

        return Optional.empty();
    }

    /**
     * Displays success message after sign-up.
     */
    private void displaySignUpSuccess(UserBean userBean) {
        printNewLine();
        printSuccess(String.format(SIGNUP_SUCCESS_MSG, userBean.getFullName()));
        printNewLine();
    }

    /**
     * Logs successful sign-up attempt.
     */
    private void logSuccessfulSignUp(String username) {
        LOGGER.log(Level.INFO, "New user registered via CLI: {0}", username);
    }

    /**
     * Handles DAO exceptions during sign-up.
     */
    private void handleDAOException(String username, DAOException e) {
        LOGGER.log(Level.WARNING, "Sign up failed for user: " + username, e);
        printError(String.format(SIGNUP_FAILED_MSG, e.getMessage()));
        printNewLine();
    }

    /**
     * Handles session exceptions during sign-up.
     */
    private void handleSessionException(String username, UserSessionException e) {
        LOGGER.log(Level.WARNING, "Session error during sign up: " + username, e);
        printError(String.format(SIGNUP_FAILED_MSG, e.getMessage()));
        printNewLine();
    }

    /**
     * Navigates to the appropriate homepage after successful registration.
     */
    private void navigateToHomepage(UserBean userBean) {
        printInfo(LOADING_DASHBOARD_MSG);
        printNewLine();

        // For now, just logout as dashboards are not implemented
        printWarning("Dashboard not yet implemented. You have been logged out.");
        printInfo("Please use the login option to access your account.");
        printNewLine();

        try {
            it.uniroma2.hoophub.session.SessionManager.INSTANCE.logout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during logout after signup", e);
        }

        // Return to login
        loginController.execute();
    }

    /**
     * Checks if input is a back or cancel command.
     */
    private boolean isBackOrCancelCommand(String input) {
        return BACK_COMMAND.equalsIgnoreCase(input) ||
                CANCEL_COMMAND.equalsIgnoreCase(input);
    }
}
