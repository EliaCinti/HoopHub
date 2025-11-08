package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.LoginController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.utilities.CliView;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginCliController manages the CLI login interface.
 * <p>
 * This is a graphic controller for the Command Line Interface (CLI),
 * parallel to {@link it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController}
 * for GUI. It follows the MVC pattern by:
 * <ul>
 *   <li><strong>Model:</strong> User, CredentialsBean (domain objects)</li>
 *   <li><strong>View:</strong> CliView (handles formatted console I/O)</li>
 *   <li><strong>Controller:</strong> This class + LoginController (application controller)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Separation of Concerns:</strong>
 * <ul>
 *   <li>LoginController - Application logic (authentication, session management)</li>
 *   <li>LoginCliController - Presentation logic (CLI-specific input/output)</li>
 *   <li>CliView - View utility (formatted console output)</li>
 * </ul>
 * </p>
 *
 * @see LoginController
 * @see CliView
 * @see it.uniroma2.hoophub.graphic_controller.gui.LoginGraphicController
 */
public class LoginCliController {

    private final CliView view;
    private final LoginController loginController;
    private static final Logger logger = Logger.getLogger(LoginCliController.class.getName());

    /**
     * Constructs a new LoginCliController with the specified view.
     *
     * @param view The CliView instance for formatted console I/O
     */
    public LoginCliController(CliView view) {
        this.view = view;
        this.loginController = new LoginController();
    }

    /**
     * Displays the login interface and handles user interaction.
     * <p>
     * This method:
     * <ol>
     *   <li>Shows the login screen</li>
     *   <li>Prompts for username and password</li>
     *   <li>Delegates authentication to LoginController</li>
     *   <li>Returns the authenticated User or null if login fails/cancelled</li>
     * </ol>
     * </p>
     *
     * @return The authenticated User object, or null if login fails or is cancelled
     */
    public User showLogin() {
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
}
