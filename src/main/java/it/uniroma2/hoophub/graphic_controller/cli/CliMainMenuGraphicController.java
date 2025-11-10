package it.uniroma2.hoophub.graphic_controller.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the main menu.
 * Handles the welcome screen with Login/SignUp/Exit options.
 * Parallel to MainGraphicController (GUI).
 */
public class CliMainMenuGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliMainMenuGraphicController.class.getName());

    // ========== Menu constants ==========
    private static final String APP_NAME = "🏀 HOOPHUB 🏀";
    private static final String WELCOME_MSG = "Welcome to HoopHub - Basketball Venue Booking System";
    private static final String MENU_TITLE = "MAIN MENU";
    private static final String OPTION_LOGIN_TEXT = "Login";
    private static final String OPTION_SIGNUP_TEXT = "Sign Up";
    private static final String OPTION_EXIT_TEXT = "Exit";
    private static final String SELECT_PROMPT = "\nSelect an option: ";
    private static final String INVALID_OPTION_MSG = "Invalid option. Please select 1, 2, or 3.";
    private static final String GOODBYE_MSG = "Thank you for using HoopHub!";
    private static final String GOODBYE_SUCCESS_MSG = "Goodbye!";

    // ========== Menu option values ==========
    private static final String OPTION_LOGIN = "1";
    private static final String OPTION_SIGNUP = "2";
    private static final String OPTION_EXIT = "3";

    // ==========   ==========
    private final CliLoginGraphicController loginController;
    // TODO: private final CliSignUpGraphicController signUpController;

    public CliMainMenuGraphicController() {
        this.loginController = new CliLoginGraphicController();
        // TODO: this.signUpController = new CliSignUpGraphicController();
    }

    /**
     * Executes the main menu graphic controller.
     * Shows welcome banner and runs the menu loop.
     */
    @Override
    public void execute() {
        showWelcome();
        runMenuLoop();
    }

    /**
     * Displays the welcome banner and initial messages.
     * This is presentation logic, so it belongs HERE in the graphic controller.
     */
    private void showWelcome() {
        printNewLine();
        print("    _   _  ___   ___  ____  _   _ _   _ ____ ");
        print("   | | | |/ _ \\ / _ \\|  _ \\| | | | | | | __ )");
        print("   | |_| | | | | | | | |_) | |_| | | | |  _ \\");
        print("   |  _  | |_| | |_| |  __/|  _  | |_| | |_) |");
        print("   |_| |_|\\___/ \\___/|_|   |_| |_|\\___/|____/");
        printNewLine();
        printInfo(WELCOME_MSG);
        printSeparator();
    }

    /**
     * Runs the main menu loop until the user exits.
     */
    private void runMenuLoop() {
        boolean running = true;

        while (running) {
            displayMenu();
            String choice = readInput(SELECT_PROMPT);

            switch (choice) {
                case OPTION_LOGIN:
                    handleLogin();
                    break;
                case OPTION_SIGNUP:
                    handleSignUp();
                    break;
                case OPTION_EXIT:
                    running = false;
                    showGoodbye();
                    break;
                default:
                    printWarning(INVALID_OPTION_MSG);
                    printNewLine();
            }
        }
    }

    /**
     * Displays the main menu.
     */
    private void displayMenu() {
        printMenu(MENU_TITLE,
                OPTION_LOGIN_TEXT,
                OPTION_SIGNUP_TEXT,
                OPTION_EXIT_TEXT);
    }

    /**
     * Handles login by delegating to CliLoginGraphicController.
     */
    private void handleLogin() {
        try {
            loginController.execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during login flow", e);
            printError("An error occurred during login. Please try again.");
            printNewLine();
        }
    }

    /**
     * Handles sign up by delegating to CliSignUpGraphicController.
     */
    private void handleSignUp() {
        printWarning("Sign up feature not yet implemented");
        printInfo("Please use the Login option if you already have an account");
        printNewLine();

        // TODO: Implement when CliSignUpGraphicController is ready
        // try {
        //     signUpController.execute();
        // } catch (Exception e) {
        //     LOGGER.log(Level.SEVERE, "Error during sign up flow", e);
        //     printError("An error occurred during sign up. Please try again.");
        //     printNewLine();
        // }
    }

    /**
     * Displays goodbye message when user exits.
     */
    private void showGoodbye() {
        printNewLine();
        printInfo(GOODBYE_MSG);
        printSuccess(GOODBYE_SUCCESS_MSG);
        printNewLine();
    }
}
