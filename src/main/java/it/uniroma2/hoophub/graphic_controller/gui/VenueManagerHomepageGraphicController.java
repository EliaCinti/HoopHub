package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ViewBookingsController;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the VenueManager dashboard.
 *
 * <p>Provides navigation to venue management and booking approval features.
 * Shows notification badge on View Bookings button when unread notifications exist.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerHomepageGraphicController {

    private static final Logger LOGGER = Logger.getLogger(VenueManagerHomepageGraphicController.class.getName());

    @FXML
    private Button manageVenuesButton;

    @FXML
    private Button viewBookingsButton;

    @FXML
    private Label notificationBadge;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final ViewBookingsController viewBookingsController;

    public VenueManagerHomepageGraphicController() {
        this.viewBookingsController = new ViewBookingsController();
    }

    /**
     * Initializes the controller and checks for unread notifications.
     */
    @FXML
    public void initialize() {
        checkUnreadNotifications();
    }

    /**
     * Checks for unread notifications and updates the badge.
     */
    private void checkUnreadNotifications() {
        try {
            int unreadCount = viewBookingsController.getUnreadNotificationsCount();

            if (unreadCount > 0) {
                // Show badge with count
                notificationBadge.setText(String.valueOf(unreadCount));
                notificationBadge.setVisible(true);
                notificationBadge.setManaged(true);

                // Add orange border to button
                viewBookingsButton.getStyleClass().add("card-with-notification");
            } else {
                // Hide badge
                notificationBadge.setVisible(false);
                notificationBadge.setManaged(false);

                // Remove orange border
                viewBookingsButton.getStyleClass().remove("card-with-notification");
            }
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
            notificationBadge.setVisible(false);
            notificationBadge.setManaged(false);
        }
    }

    /**
     * Handles "Manage Venues" button click.
     */
    @FXML
    private void onManageVenuesClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/manage_venues.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load manage venues page", e);
        }
    }

    /**
     * Handles "View Bookings" button click.
     */
    @FXML
    private void onViewBookingsClick() {
        try {
            ViewBookingsGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/view_bookings.fxml",
                    ViewBookingsGraphicController.class
            );
            // Pass the application controller instance
            controller.initWithController(viewBookingsController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load view bookings page", e);
        }
    }

    /**
     * Closes the current stage.
     */
    private void closeCurrentStage() {
        Stage stage = (Stage) manageVenuesButton.getScene().getWindow();
        stage.close();
    }
}