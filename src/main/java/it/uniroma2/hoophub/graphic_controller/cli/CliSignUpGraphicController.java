package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
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
 * Handles new user registration for both Fans and Venue Managers.
 * The user first provides common credentials (username, password),
 * then selects their account type, and finally provides role-specific information.
 * After successful registration, the user is automatically logged in.
 * </p>
 */
public class CliSignUpGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliSignUpGraphicController.class.getName());

    // Constants for commands
    private static final String BACK_COMMAND = "back";
    private static final String CANCEL_COMMAND = "cancel";

    // Message constants
    private static final String TITLE = "HOOPHUB - SIGN UP";
    private static final String SUBTITLE = "Create your account";
    private static final String USERNAME_PROMPT = "Username (min 3 characters): ";
    private static final String PASSWORD_PROMPT = "Password (min 6 characters): ";
    private static final String CONFIRM_PASSWORD_PROMPT = "Confirm password: ";

    // User type selection
    private static final String USER_TYPE_PROMPT = "Select account type:\n  1) Fan\n  2) Venue Manager\nYour choice: ";
    private static final String INVALID_CHOICE_MSG = "Invalid choice. Please enter 1 or 2";

    // Fan-specific prompts
    private static final String FULL_NAME_PROMPT = "Full name (e.g., John Doe): ";
    private static final String GENDER_PROMPT = "Gender (M/F/Other): ";
    private static final String FAVORITE_TEAM_PROMPT = "Favorite NBA team (e.g., Los Angeles Lakers): ";
    private static final String BIRTHDAY_PROMPT = "Birthday (YYYY-MM-DD): ";

    // VenueManager-specific prompts
    private static final String COMPANY_NAME_PROMPT = "Company name: ";
    private static final String PHONE_NUMBER_PROMPT = "Phone number: ";

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

    public CliSignUpGraphicController() {
        this.signUpController = SignUpController.getInstance();
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
     * Flow: collect common credentials → select user type → collect type-specific info
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

        // Phase 1: Collect common credentials
        Optional<String> username = readUsername();
        if (username.isEmpty()) return handleBackOrCancel();

        Optional<String> password = readPassword();
        if (password.isEmpty()) return handleBackOrCancel();

        printNewLine();

        // Phase 2: Select user type
        Optional<Integer> userType = readUserType();
        if (userType.isEmpty()) return handleBackOrCancel();

        printNewLine();

        // Phase 3: Collect type-specific information and register
        if (userType.get() == 1) {
            return performFanSignUp(username.get(), password.get());
        } else {
            return performVenueManagerSignUp(username.get(), password.get());
        }
    }

    /**
     * Performs Fan-specific signup: collects Fan information and registers.
     *
     * @param username The username (already collected)
     * @param password The password (already collected)
     * @return Optional containing the registered UserBean, or empty if cancelled
     */
    private Optional<UserBean> performFanSignUp(String username, String password) {
        printInfo("Complete your Fan profile:");
        printNewLine();

        Optional<String> fullName = readFullName();
        if (fullName.isEmpty()) return handleBackOrCancel();

        Optional<String> gender = readGender();
        if (gender.isEmpty()) return handleBackOrCancel();

        Optional<TeamNBA> favTeam = readFavoriteTeam();
        if (favTeam.isEmpty()) return handleBackOrCancel();

        Optional<LocalDate> birthday = readBirthday();
        if (birthday.isEmpty()) return handleBackOrCancel();

        return attemptFanSignUp(username, password, fullName.get(),
                gender.get(), favTeam.get(), birthday.get());
    }

    /**
     * Performs VenueManager-specific signup: collects VenueManager information and registers.
     *
     * @param username The username (already collected)
     * @param password The password (already collected)
     * @return Optional containing the registered UserBean, or empty if cancelled
     */
    private Optional<UserBean> performVenueManagerSignUp(String username, String password) {
        printInfo("Complete your Venue Manager profile:");
        printNewLine();

        Optional<String> companyName = readCompanyName();
        if (companyName.isEmpty()) return handleBackOrCancel();

        Optional<String> phoneNumber = readPhoneNumber();
        if (phoneNumber.isEmpty()) return handleBackOrCancel();

        return attemptVenueManagerSignUp(username, password, companyName.get(), phoneNumber.get());
    }

    /**
     * Handles the back/cancel command by returning to login.
     * Simply returns empty to exit signup and return to caller (login screen).
     */
    private Optional<UserBean> handleBackOrCancel() {
        printInfo(SIGNUP_CANCELLED_MSG);
        printNewLine();
        return Optional.empty();
    }

    /**
     * Reads and validates user type selection (1=Fan, 2=VenueManager).
     *
     * @return Optional containing 1 for Fan or 2 for VenueManager, or empty if cancelled
     */
    private Optional<Integer> readUserType() {
        while (true) {
            String input = readInput(USER_TYPE_PROMPT);

            if (isBackOrCancelCommand(input)) {
                return Optional.empty();
            }

            if (input.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            if ("1".equals(input)) {
                return Optional.of(1);
            } else if ("2".equals(input)) {
                return Optional.of(2);
            } else {
                printWarning(INVALID_CHOICE_MSG);
            }
        }
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
     * Reads and validates company name input (VenueManager).
     */
    private Optional<String> readCompanyName() {
        while (true) {
            String companyName = readInput(COMPANY_NAME_PROMPT);

            if (isBackOrCancelCommand(companyName)) {
                return Optional.empty();
            }

            if (companyName.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            return Optional.of(companyName);
        }
    }

    /**
     * Reads and validates phone number input (VenueManager).
     */
    private Optional<String> readPhoneNumber() {
        while (true) {
            String phoneNumber = readInput(PHONE_NUMBER_PROMPT);

            if (isBackOrCancelCommand(phoneNumber)) {
                return Optional.empty();
            }

            if (phoneNumber.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            return Optional.of(phoneNumber);
        }
    }

    /**
     * Attempts to register a Fan with the collected data.
     */
    private Optional<UserBean> attemptFanSignUp(String username, String password, String fullName,
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

            // Register and auto-login using polymorphic signUp method
            UserBean registeredUser = signUpController.signUp(fanBean, true);

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
     * Attempts to register a VenueManager with the collected data.
     */
    private Optional<UserBean> attemptVenueManagerSignUp(String username, String password,
                                                         String companyName, String phoneNumber) {
        try {
            VenueManagerBean venueManagerBean = new VenueManagerBean.Builder()
                    .username(username)
                    .password(password)
                    .type(UserType.VENUE_MANAGER.toString())
                    .companyName(companyName)
                    .phoneNumber(phoneNumber)
                    .build();

            // Register and auto-login using polymorphic signUp method
            UserBean registeredUser = signUpController.signUp(venueManagerBean, true);

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
        String displayName = userBean.getFullName() != null && !userBean.getFullName().isEmpty()
                ? userBean.getFullName()
                : userBean.getUsername();
        printSuccess(String.format(SIGNUP_SUCCESS_MSG, displayName));
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
     * Since dashboards are not yet implemented, logs out the user and returns to login.
     */
    private void navigateToHomepage(UserBean userBean) {
        printInfo(LOADING_DASHBOARD_MSG);
        printNewLine();

        // For now, just logout as dashboards are not implemented
        printWarning("Dashboard not yet implemented. You have been logged out.");
        printInfo("Please use the login option to access your account.");
        printNewLine();
        printInfo("Press Enter to continue...");
        readInput("");

        try {
            it.uniroma2.hoophub.session.SessionManager.INSTANCE.logout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during logout after signup", e);
        }

        // Clear screen before returning to login
        clearScreen();

        // Exit signup - caller (login screen) will show again
    }

    /**
     * Checks if input is a back or cancel command.
     */
    private boolean isBackOrCancelCommand(String input) {
        return BACK_COMMAND.equalsIgnoreCase(input) ||
                CANCEL_COMMAND.equalsIgnoreCase(input);
    }
}
