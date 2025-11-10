package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.utilities.CliView;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the main menu.
 * Handles the welcome screen with Login/SignUp/Exit options.
 * Parallel to MainGraphicController (GUI).
 */
public class CliMainMenuGraphicController {

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
    private final CliView view;
    private final CliLoginGraphicController loginController;
    // TODO: private final CliSignUpGraphicController signUpController;

    public CliMainMenuGraphicController(CliView view) {
        this.view = view;
        this.loginController = new CliLoginGraphicController(view);
        // TODO: this.signUpController = new CliSignUpGraphicController(view);
    }

    /**
     * Executes the main menu graphic controller.
     * Shows welcome banner and runs the menu loop.
     */
    public void execute() {
        showWelcome();
        runMenuLoop();
    }

    /**
     * Displays the welcome banner and initial messages.
     * This is presentation logic, so it belongs HERE in the graphic controller.
     */
    private void showWelcome() {
        view.newLine();
        view.showMessage("    _   _  ___   ___  ____  _   _ _   _ ____ ");
        view.showMessage("   | | | |/ _ \\ / _ \\|  _ \\| | | | | | | __ )");
        view.showMessage("   | |_| | | | | | | | |_) | |_| | | | |  _ \\");
        view.showMessage("   |  _  | |_| | |_| |  __/|  _  | |_| | |_) |");
        view.showMessage("   |_| |_|\\___/ \\___/|_|   |_| |_|\\___/|____/");
        view.newLine();
        view.showInfo(WELCOME_MSG);
        view.showSeparator();
    }

    /**
     * Runs the main menu loop until the user exits.
     */
    private void runMenuLoop() {
        boolean running = true;

        while (running) {
            displayMenu();
            String choice = view.readInput(SELECT_PROMPT);

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
                    view.showWarning(INVALID_OPTION_MSG);
                    view.newLine();
            }
        }
    }

    /**
     * Displays the main menu.
     */
    private void displayMenu() {
        view.showMenu(MENU_TITLE,
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
            view.showError("An error occurred during login. Please try again.");
            view.newLine();
        }
    }

    /**
     * Handles sign up by delegating to CliSignUpGraphicController.
     */
    private void handleSignUp() {
        view.showWarning("Sign up feature not yet implemented");
        view.showInfo("Please use the Login option if you already have an account");
        view.newLine();

        // TODO: Implement when CliSignUpGraphicController is ready
        // try {
        //     signUpController.execute();
        // } catch (Exception e) {
        //     LOGGER.log(Level.SEVERE, "Error during sign up flow", e);
        //     view.showError("An error occurred during sign up. Please try again.");
        //     view.newLine();
        // }
    }

    /**
     * Displays goodbye message when user exits.
     */
    private void showGoodbye() {
        view.newLine();
        view.showInfo(GOODBYE_MSG);
        view.showSuccess(GOODBYE_SUCCESS_MSG);
        view.newLine();
    }
}
