package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;

import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the sign-up use case.
 * <p>
 * Handles new user registration for both Fans and Venue Managers.
 * The flow collects common User data first (delegating validation to Beans),
 * then specialized data based on the selected role.
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

    // Common User prompts
    private static final String FULL_NAME_PROMPT = "Full name (e.g., John Doe): ";
    private static final String GENDER_PROMPT = "Gender (M/F/Other): ";

    // Fan-specific prompts
    private static final String FAVORITE_TEAM_PROMPT = "Favorite NBA team (e.g., Los Angeles Lakers): ";
    private static final String BIRTHDAY_PROMPT = "Birthday (YYYY-MM-DD): ";

    // VenueManager-specific prompts
    private static final String COMPANY_NAME_PROMPT = "Company name: ";
    private static final String PHONE_NUMBER_PROMPT = "Phone number: ";

    private static final String EMPTY_INPUT_MSG = "Input cannot be empty";
    private static final String PASSWORD_MISMATCH_MSG = "Passwords do not match";
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
     */
    @Override
    public void execute() {
        performSignUp().ifPresent(this::navigateToHomepage);
    }

    /**
     * Performs the sign-up operation.
     * LOGIC REFACTORED: Collects all COMMON User data first, then User Type, then Specifics.
     * Validation is delegated to Bean classes.
     */
    private Optional<UserBean> performSignUp() {
        printTitle(TITLE);
        printInfo(SUBTITLE);
        printNewLine();
        printInfo(LOGIN_OPTION_MSG);
        printInfo(CANCEL_INFO_MSG);
        printNewLine();

        // 1. Collect Common User Credentials & Info
        Optional<String> username = readUsername();
        if (username.isEmpty()) return handleBackOrCancel();

        Optional<String> password = readPassword();
        if (password.isEmpty()) return handleBackOrCancel();

        // Collect Name and Gender immediately (Common to User)
        Optional<String> fullName = readFullName();
        if (fullName.isEmpty()) return handleBackOrCancel();

        Optional<String> gender = readGender();
        if (gender.isEmpty()) return handleBackOrCancel();

        printNewLine();

        // 2. Select User Type
        Optional<Integer> userType = readUserType();
        if (userType.isEmpty()) return handleBackOrCancel();

        printNewLine();

        // 3. Collect Type-Specific Info and Register
        if (userType.get() == 1) {
            return performFanSignUp(username.get(), password.get(), fullName.get(), gender.get());
        } else {
            return performVenueManagerSignUp(username.get(), password.get(), fullName.get(), gender.get());
        }
    }

    /**
     * Performs Fan-specific signup: collects only Fan-specific info.
     */
    private Optional<UserBean> performFanSignUp(String username, String password, String fullName, String gender) {
        printInfo("Complete your Fan profile:");
        printNewLine();

        Optional<TeamNBA> favTeam = readFavoriteTeam();
        if (favTeam.isEmpty()) return handleBackOrCancel();

        Optional<LocalDate> birthday = readBirthday();
        if (birthday.isEmpty()) return handleBackOrCancel();

        return attemptFanSignUp(username, password, fullName, gender, favTeam.get(), birthday.get());
    }

    /**
     * Performs VenueManager-specific signup: collects only VenueManager-specific info.
     */
    private Optional<UserBean> performVenueManagerSignUp(String username, String password, String fullName, String gender) {
        printInfo("Complete your Venue Manager profile:");
        printNewLine();

        Optional<String> companyName = readCompanyName();
        if (companyName.isEmpty()) return handleBackOrCancel();

        Optional<String> phoneNumber = readPhoneNumber();
        if (phoneNumber.isEmpty()) return handleBackOrCancel();

        return attemptVenueManagerSignUp(username, password, fullName, gender, companyName.get(), phoneNumber.get());
    }

    private Optional<UserBean> handleBackOrCancel() {
        printInfo(SIGNUP_CANCELLED_MSG);
        printNewLine();
        return Optional.empty();
    }

    private Optional<Integer> readUserType() {
        while (true) {
            String input = readInput(USER_TYPE_PROMPT);

            if (isBackOrCancelCommand(input)) return Optional.empty();
            if (input.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
                continue;
            }

            if ("1".equals(input)) return Optional.of(1);
            if ("2".equals(input)) return Optional.of(2);
            printWarning(INVALID_CHOICE_MSG);
        }
    }

    /**
     * Reads and validates username input.
     * Uses CredentialsBean logic to validate syntax.
     */
    private Optional<String> readUsername() {
        while (true) {
            String username = readInput(USERNAME_PROMPT);

            if (isBackOrCancelCommand(username)) {
                return Optional.empty();
            }

            try {
                // Delegate syntax validation to the Bean
                CredentialsBean.validateUsernameSyntax(username);
                return Optional.of(username);
            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }
    }

    /**
     * Reads and validates password input.
     * Uses CredentialsBean logic to validate syntax.
     */
    private Optional<String> readPassword() {
        while (true) {
            String password = readInput(PASSWORD_PROMPT);

            if (isBackOrCancelCommand(password)) {
                return Optional.empty();
            }

            try {
                // Delegate syntax validation to the Bean (length/empty check)
                CredentialsBean.validatePasswordSyntax(password);

                // Password confirmation logic remains here (UI flow logic)
                String confirmPassword = readInput(CONFIRM_PASSWORD_PROMPT);
                if (isBackOrCancelCommand(confirmPassword)) return Optional.empty();

                if (!password.equals(confirmPassword)) {
                    printWarning(PASSWORD_MISMATCH_MSG);
                } else {
                    return Optional.of(password);
                }

            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }
    }

    /**
     * Reads and validates full name input.
     * Uses UserBean logic to validate syntax.
     */
    private Optional<String> readFullName() {
        while (true) {
            String fullName = readInput(FULL_NAME_PROMPT);

            if (isBackOrCancelCommand(fullName)) {
                return Optional.empty();
            }

            try {
                // Delegate syntax validation to the Bean
                UserBean.validateFullNameSyntax(fullName);
                return Optional.of(fullName);
            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }
    }

    /**
     * Reads and validates gender input.
     * Uses UserBean logic to validate syntax.
     */
    private Optional<String> readGender() {
        while (true) {
            String genderInput = readInput(GENDER_PROMPT);

            if (isBackOrCancelCommand(genderInput)) {
                return Optional.empty();
            }
            try {
                // 1. Syntactic Validation (Delegated to Bean)
                UserBean.validateGenderSyntax(genderInput);
                return Optional.of(genderInput);

            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }

    }

    private Optional<TeamNBA> readFavoriteTeam() {
        while (true) {
            String teamInput = readInput(FAVORITE_TEAM_PROMPT + " (or type 'list' to see all)");

            if (isBackOrCancelCommand(teamInput)) {
                return Optional.empty();
            }

            if (teamInput.equalsIgnoreCase("list")) {
                printAvailableTeams();
                continue;
            }

            try {
                // 1. Validate Syntax (String level check)
                FanBean.validateTeamSyntax(teamInput);

                // 2. Resolve (Model level lookup)
                // Siccome la validazione usa fromInput, siamo sicuri che qui non tornerà null
                TeamNBA team = TeamNBA.fromInput(teamInput);
                return Optional.of(team);

            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }
    }

    /**
     * Prints the list of available NBA teams.
     */
    private void printAvailableTeams() {
        printNewLine();
        printInfo("--- AVAILABLE NBA TEAMS ---");
        for (TeamNBA team : TeamNBA.values()) {
            // Using String.format instead of System.out.printf to leverage the protected print() method
            // Format: "LAL   - Los Angeles Lakers"
            String line = String.format("%-5s - %s", team.getAbbreviation(), team.getDisplayName());
            print(line);
        }
        printNewLine();
    }

    /**
     * Reads and validates birthday input.
     * Delegates parsing and validation entirely to the Bean.
     */
    private Optional<LocalDate> readBirthday() {
        while (true) {
            String birthdayInput = readInput(BIRTHDAY_PROMPT);

            if (isBackOrCancelCommand(birthdayInput)) {
                return Optional.empty();
            }

            try {
                LocalDate date = FanBean.parseBirthday(birthdayInput);
                return Optional.of(date);

            } catch (IllegalArgumentException e) {
                printWarning(e.getMessage());
            }
        }
    }

    private Optional<String> readCompanyName() {
        return readGenericNonEmptyInput(COMPANY_NAME_PROMPT);
    }

    private Optional<String> readPhoneNumber() {
        return readGenericNonEmptyInput(PHONE_NUMBER_PROMPT);
    }

    // --- REGISTRATION LOGIC ---

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

    private Optional<UserBean> attemptVenueManagerSignUp(String username, String password, String fullName,
                                                         String gender, String companyName, String phoneNumber) {
        try {
            VenueManagerBean venueManagerBean = new VenueManagerBean.Builder()
                    .username(username)
                    .password(password)
                    .fullName(fullName)
                    .gender(gender)
                    .type(UserType.VENUE_MANAGER.toString())
                    .companyName(companyName)
                    .phoneNumber(phoneNumber)
                    .build();

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

    private void displaySignUpSuccess(UserBean userBean) {
        printNewLine();
        String displayName = userBean.getFullName() != null && !userBean.getFullName().isEmpty()
                ? userBean.getFullName()
                : userBean.getUsername();
        printSuccess(String.format(SIGNUP_SUCCESS_MSG, displayName));
        printNewLine();
    }

    private void logSuccessfulSignUp(String username) {
        LOGGER.log(Level.INFO, "New user registered via CLI: {0}", username);
    }

    private void handleDAOException(String username, DAOException e) {
        LOGGER.log(Level.WARNING, e, () -> "Sign up failed for user: " + username);
        printError(String.format(SIGNUP_FAILED_MSG, e.getMessage()));
        printNewLine();
    }

    private void handleSessionException(String username, UserSessionException e) {
        LOGGER.log(Level.WARNING, e, () -> "Session error during sign up: " + username);
        printError(String.format(SIGNUP_FAILED_MSG, e.getMessage()));
        printNewLine();
    }

    private void navigateToHomepage(UserBean userBean) {
        printInfo(LOADING_DASHBOARD_MSG);
        printNewLine();
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
        clearScreen();
    }

    private boolean isBackOrCancelCommand(String input) {
        return BACK_COMMAND.equalsIgnoreCase(input) || CANCEL_COMMAND.equalsIgnoreCase(input);
    }

    private Optional<String> readGenericNonEmptyInput(String prompt) {
        while (true) {
            String input = readInput(prompt);
            if (isBackOrCancelCommand(input)) return Optional.empty();
            if (input.isEmpty()) {
                printWarning(EMPTY_INPUT_MSG);
            } else {
                return Optional.of(input);
            }
        }
    }
}