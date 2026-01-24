package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.NotificationController;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.session.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for CLI homepage controllers.
 *
 * <p>Implements the <b>Template Method pattern (GoF)</b>: defines the skeleton
 * of the homepage flow (display, input handling, logout) while letting subclasses
 * define specific menu options and handlers via abstract methods.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CliFanHomepageGraphicController
 * @see CliVenueManagerHomepageGraphicController
 */
public abstract class AbstractCliHomepageController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(AbstractCliHomepageController.class.getName());

    protected static final String LOGOUT_COMMAND = "logout";
    protected static final String EXIT_COMMAND = "exit";

    protected static final String WELCOME_MSG = "Welcome back, %s!";
    protected static final String UNREAD_NOTIFICATIONS_MSG = "You have %d unread notification(s)";
    protected static final String MENU_PROMPT = "Select an option (1-%d) or type 'logout': ";
    protected static final String INVALID_OPTION_MSG = "Invalid option. Please try again.";
    protected static final String FEATURE_NOT_IMPLEMENTED_MSG = "This feature is not yet implemented.";
    protected static final String LOGOUT_SUCCESS_MSG = "Logged out successfully. Goodbye!";
    protected static final String LOGOUT_ERROR_MSG = "Error during logout: %s";
    protected static final String NO_USER_ERROR_MSG = "No user logged in. Returning to login...";

    protected final NotificationController notificationController;
    protected UserBean currentUser;
    protected boolean shouldExit = false;

    protected AbstractCliHomepageController() {
        this.notificationController = new NotificationController();
    }

    // ==================== TEMPLATE METHOD ====================

    /**
     * Template method: executes the homepage flow.
     * Subclasses should NOT override this method.
     */
    @Override
    public void execute() {
        currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            printError(NO_USER_ERROR_MSG);
            return;
        }

        while (!shouldExit) {
            displayHomepage();
            handleUserInput();
        }
    }

    private void displayHomepage() {
        clearScreen();
        printTitle(getTitle());

        String displayName = getDisplayName();
        printSuccess(String.format(WELCOME_MSG, displayName));

        displayAdditionalInfo();

        printNewLine();

        int unreadCount = notificationController.getUnreadNotificationCount();
        if (unreadCount > 0) {
            printWarning(String.format(UNREAD_NOTIFICATIONS_MSG, unreadCount));
            printNewLine();
        }

        printMenu("What would you like to do?", getMenuOptions());
    }

    private void handleUserInput() {
        String input = readInput(String.format(MENU_PROMPT, getMenuOptions().length));

        if (LOGOUT_COMMAND.equalsIgnoreCase(input) || EXIT_COMMAND.equalsIgnoreCase(input)) {
            performLogout();
            return;
        }

        try {
            int option = Integer.parseInt(input);
            processMenuOption(option);
        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            pauseBeforeContinue();
        }
    }

    // ==================== ABSTRACT METHODS ====================

    /**
     * Returns the homepage title.
     *
     * @return title string for the dashboard
     */
    protected abstract String getTitle();

    /**
     * Returns the menu options array.
     *
     * @return array of menu option strings
     */
    protected abstract String[] getMenuOptions();

    /**
     * Processes the selected menu option.
     *
     * @param option selected option (1-based index)
     */
    protected abstract void processMenuOption(int option);

    // ==================== HOOK METHODS ====================

    /**
     * Hook for displaying additional user info. Override to customize.
     */
    protected void displayAdditionalInfo() {
        // Default: no additional info
    }

    // ==================== HELPER METHODS ====================

    /**
     * Gets the display name (full name or username).
     *
     * @return user's display name
     */
    protected String getDisplayName() {
        return currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                ? currentUser.getFullName()
                : currentUser.getUsername();
    }

    /** Performs logout and exits the homepage loop. */
    protected void performLogout() {
        try {
            SessionManager.INSTANCE.logout();
            printNewLine();
            printSuccess(LOGOUT_SUCCESS_MSG);
            printNewLine();
            shouldExit = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during logout", e);
            printError(String.format(LOGOUT_ERROR_MSG, e.getMessage()));
        }
    }

    /** Pauses until user presses Enter. */
    protected void pauseBeforeContinue() {
        printNewLine();
        readInput("Press Enter to continue...");
    }

    /** Shows "feature not implemented" message. */
    protected void showNotImplemented() {
        printNewLine();
        printInfo(FEATURE_NOT_IMPLEMENTED_MSG);
        pauseBeforeContinue();
    }
}