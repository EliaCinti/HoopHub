package it.uniroma2.hoophub.view;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.graphic_controller.cli.LoginCliController;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.utilities.CliView;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CliApplication is the main View class for the Command Line Interface.
 * <p>
 * This class manages the entire CLI application lifecycle, including:
 * <ul>
 *   <li>Application banner and welcome messages</li>
 *   <li>Main menu navigation</li>
 *   <li>User interaction loop</li>
 *   <li>Resource cleanup</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Architecture:</strong> This is the CLI equivalent of the JavaFX Application class.
 * It serves as the top-level View component for the CLI interface, following MVC pattern:
 * <ul>
 *   <li><strong>View:</strong> CliApplication + CliView (presentation layer)</li>
 *   <li><strong>Controller:</strong> LoginCliController (graphic controller for CLI)</li>
 *   <li><strong>Application Controller:</strong> LoginController (business logic)</li>
 *   <li><strong>Model:</strong> DAOs, entities, business objects</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Controller Lifecycle:</strong> Controllers are instantiated ONCE and reused
 * throughout the application lifecycle (no new controller instances per operation).
 * Each controller manages its entire use case from start to finish.
 * </p>
 *
 * @see CliView
 * @see LoginCliController
 */
public class CliApplication {

    private final CliView view;
    private final LoginCliController loginController;
    private static final Logger logger = Logger.getLogger(CliApplication.class.getName());

    /**
     * Constructs a new CliApplication.
     * <p>
     * Initializes the CliView for formatted console I/O and creates
     * controller instances (ONE instance per controller, reused throughout lifecycle).
     * </p>
     */
    public CliApplication() {
        this.view = new CliView();
        // Create controller instances ONCE - they will be reused
        this.loginController = new LoginCliController(view);
    }

    /**
     * Starts the CLI application.
     * <p>
     * This method:
     * <ol>
     *   <li>Displays the application banner and welcome message</li>
     *   <li>Enters the main application loop</li>
     *   <li>Handles user menu selections</li>
     *   <li>Manages resource cleanup on exit</li>
     * </ol>
     * </p>
     * <p>
     * This is the entry point called by Main when CLI mode is selected.
     * It's analogous to the JavaFX Application.start() method for GUI.
     * </p>
     */
    public void start() {
        try {
            showWelcome();
            runMainLoop();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in CLI application", e);
            view.showError("An unexpected error occurred: " + e.getMessage());
            view.showInfo("Please restart the application");
        } finally {
            cleanup();
        }
    }

    /**
     * Displays the welcome banner and initial messages.
     */
    private void showWelcome() {
        view.showBanner("🏀 HOOPHUB 🏀");
        view.showInfo("Welcome to HoopHub - Basketball Venue Booking System");
        view.showSeparator();
    }

    /**
     * Runs the main application loop.
     * <p>
     * Continuously displays the main menu and processes user input
     * until the user chooses to exit.
     * </p>
     */
    private void runMainLoop() {
        boolean running = true;

        while (running) {
            view.showMenu("MAIN MENU",
                    "Login",
                    "Exit");

            String choice = view.readInput("\nSelect an option: ");

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    running = false;
                    showGoodbye();
                    break;
                default:
                    view.showWarning("Invalid option. Please select 1 or 2.");
                    view.newLine();
            }
        }
    }

    /**
     * Handles the login flow.
     * <p>
     * Delegates to the LoginCliController instance (reused, not recreated).
     * The controller manages the entire login use case from start to finish,
     * including post-login navigation.
     * </p>
     */
    private void handleLogin() {
        // Use the SAME controller instance (no new)
        loginController.execute();
    }

    /**
     * Displays goodbye message.
     */
    private void showGoodbye() {
        view.newLine();
        view.showInfo("Thank you for using HoopHub!");
        view.showSuccess("Goodbye!");
        view.newLine();
    }

    /**
     * Performs cleanup operations.
     * <p>
     * Closes the CliView scanner and database connection if using MySQL.
     * This method is called in the finally block to ensure proper resource cleanup.
     * </p>
     */
    private void cleanup() {
        // Close CLI view resources
        view.close();

        // Close database connection if using MySQL
        if (DaoFactoryFacade.getInstance().getPersistenceType() == PersistenceType.MYSQL) {
            try {
                ConnectionFactory.closeConnection();
                logger.info("Database connection closed successfully");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error closing database connection", e);
            }
        }

        logger.info("CLI application shutdown complete");
    }
}
