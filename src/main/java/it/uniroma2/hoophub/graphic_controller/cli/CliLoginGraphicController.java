package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.LoginController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.CliView;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CliLoginGraphicController is the CLI graphic controller for the login use case.
 * <p>
 * This is a graphic controller for the Command Line Interface (CLI),
 * parallel to {@link it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController}
 * for GUI. It follows the MVC pattern by:
 * <ul>
 *   <li><strong>Model:</strong> User, CredentialsBean (domain objects)</li>
 *   <li><strong>View:</strong> CliView (handles formatted console I/O)</li>
 *   <li><strong>Graphic Controller:</strong> This class (CLI presentation logic)</li>
 *   <li><strong>Application Controller:</strong> LoginController (business logic)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Naming Convention:</strong>
 * <ul>
 *   <li>GUI Graphic Controller: LoginGraphicController</li>
 *   <li>CLI Graphic Controller: CliLoginGraphicController (this class)</li>
 *   <li>Application Controller: LoginController (shared by both GUI and CLI)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Controller Lifecycle:</strong> This controller is instantiated ONCE in CliApplication
 * and reused for all login operations. It manages the entire login use case from start to finish,
 * including post-login navigation and eventual logout.
 * </p>
 *
 * @see LoginController
 * @see CliView
 * @see it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController
 */
public class CliLoginGraphicController {

    private final CliView view;
    private final LoginController loginController;
    private static final Logger logger = Logger.getLogger(CliLoginGraphicController.class.getName());

    /**
     * Constructs a new CliLoginGraphicController with the specified view.
     * <p>
     * This constructor is called ONCE by CliApplication. The same instance
     * is reused for all login operations (no new instances created per login).
     * </p>
     *
     * @param view The CliView instance for formatted console I/O
     */
    public CliLoginGraphicController(CliView view) {
        this.view = view;
        this.loginController = new LoginController();
    }

    /**
     * Executes the complete login use case.
     * <p>
     * This method manages the entire login flow:
     * <ol>
     *   <li>Shows the login screen</li>
     *   <li>Prompts for username and password</li>
     *   <li>Delegates authentication to LoginController</li>
     *   <li>Handles post-login navigation (dashboard loading)</li>
     *   <li>Manages logout when dashboard is not yet implemented</li>
     * </ol>
     * </p>
     * <p>
     * This is the main entry point for the login use case, called by CliApplication.
     * </p>
     */
    public void execute() {
        User loggedUser = performLogin();

        if (loggedUser != null) {
            handlePostLogin(loggedUser);
        }
    }

    /**
     * Performs the login operation with user input loop.
     *
     * @return The authenticated User object, or null if login is cancelled
     */
    private User performLogin() {
        view.showTitle("HOOPHUB - LOGIN");

        while (true) {
            // Read credentials from user
            view.newLine();
            String username = view.readInput("Username: ");

            if (username.isEmpty()) {
                view.showWarning("Username cannot be empty");
                continue;
            }

            // Check for exit command
            if (username.equalsIgnoreCase("exit") || username.equalsIgnoreCase("quit")) {
                view.showInfo("Login cancelled");
                return null;
            }

            String password = view.readPassword("Password: ");

            if (password.isEmpty()) {
                view.showWarning("Password cannot be empty");
                continue;
            }

            // Attempt login
            try {
                CredentialsBean credentials = new CredentialsBean.Builder<>()
                        .username(username)
                        .password(password)
                        .build();

                User loggedUser = loginController.login(credentials);

                // Login successful
                view.newLine();
                view.showSuccess("Login successful! Welcome, " + loggedUser.getFullName());
                view.showInfo("User type: " + loggedUser.getUserType());
                view.newLine();

                logger.log(Level.INFO, "User logged in via CLI: {0} ({1})",
                    new Object[]{username, loggedUser.getUserType()});

                return loggedUser;

            } catch (DAOException e) {
                logger.log(Level.WARNING, "Login failed for user: " + username, e);
                view.showError("Login failed: " + e.getMessage());
                view.showInfo("Please try again or type 'exit' to quit");
                view.newLine();

            } catch (UserSessionException e) {
                logger.log(Level.INFO, "User already logged in: " + username, e);
                view.showError("This user is already logged in");
                view.showInfo("Please logout first or try another account");
                view.newLine();
            }
        }
    }

    /**
     * Handles post-login navigation and actions.
     * <p>
     * This method determines what happens after successful login based on user type.
     * Currently shows a placeholder message and logs out the user since
     * dashboard graphic controllers are not yet implemented.
     * </p>
     * <p>
     * In a complete implementation, this would delegate to CliFanHomeGraphicController
     * or CliVenueManagerHomeGraphicController based on user type.
     * </p>
     *
     * @param user The authenticated user
     */
    private void handlePostLogin(User user) {
        view.newLine();
        view.showInfo("Loading " + user.getUserType() + " dashboard...");
        view.showWarning("Note: Dashboard graphic controllers not yet implemented");

        // TODO: Implement navigation to dashboard graphic controllers
        // switch (user.getUserType()) {
        //     case FAN:
        //         cliFanHomeGraphicController.execute();
        //         break;
        //     case VENUE_MANAGER:
        //         cliVenueManagerHomeGraphicController.execute();
        //         break;
        // }

        // For now, just logout
        view.showInfo("Logging out...");
        view.newLine();

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
