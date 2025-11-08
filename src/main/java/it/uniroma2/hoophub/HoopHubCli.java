package it.uniroma2.hoophub;

import it.uniroma2.hoophub.graphic_controller.cli.LoginCliController;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.CliView;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HoopHubCli is the main entry point for the HoopHub Command Line Interface.
 * <p>
 * This class initializes the CLI environment and manages the main application loop.
 * It follows the MVC pattern by delegating to graphic controllers (CLI) which in turn
 * use application controllers for business logic.
 * </p>
 * <p>
 * <strong>Architecture:</strong>
 * <ul>
 *   <li>Main → LoginCliController (graphic controller)</li>
 *   <li>LoginCliController → LoginController (application controller)</li>
 *   <li>LoginController → DAOs and SessionManager (model/business logic)</li>
 * </ul>
 * </p>
 *
 * @see LoginCliController
 * @see it.uniroma2.hoophub.app_controller.LoginController
 * @see CliView
 */
public class HoopHubCli {

    private static final Logger logger = Logger.getLogger(HoopHubCli.class.getName());

    /**
     * Main method - entry point for the CLI application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        CliView view = new CliView();

        try {
            // Show application banner
            view.showBanner("🏀 HOOPHUB 🏀");
            view.showInfo("Welcome to HoopHub - Basketball Venue Booking System");
            view.showSeparator();

            // Main application loop
            boolean running = true;

            while (running) {
                // Show main menu
                view.showMenu("MAIN MENU",
                        "Login",
                        "Exit");

                String choice = view.readInput("\nSelect an option: ");

                switch (choice) {
                    case "1":
                        handleLogin(view);
                        break;
                    case "2":
                        running = false;
                        view.newLine();
                        view.showInfo("Thank you for using HoopHub!");
                        view.showSuccess("Goodbye!");
                        view.newLine();
                        break;
                    default:
                        view.showWarning("Invalid option. Please select 1 or 2.");
                        view.newLine();
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in CLI application", e);
            view.showError("An unexpected error occurred: " + e.getMessage());
            view.showInfo("Please restart the application");
        } finally {
            view.close();
        }
    }

    /**
     * Handles the login flow.
     *
     * @param view The CliView instance
     */
    private static void handleLogin(CliView view) {
        LoginCliController loginController = new LoginCliController(view);

        User loggedUser = loginController.showLogin();

        if (loggedUser != null) {
            // Login successful - show next screen based on user type
            String nextController = loginController.getNextController(loggedUser);

            view.newLine();
            view.showInfo("Loading " + loggedUser.getUserType() + " dashboard...");
            view.showWarning("Note: Dashboard controllers not yet implemented");
            view.showInfo("Next controller: " + nextController);
            view.newLine();

            // TODO: Implement Fan and VenueManager home controllers
            // For now, just logout after showing the message
            view.showInfo("Logging out...");

            try {
                SessionManager.INSTANCE.logout();
                view.showSuccess("Logged out successfully");
                view.newLine();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during logout", e);
                view.showWarning("Error during logout: " + e.getMessage());
            }
        }
    }
}
