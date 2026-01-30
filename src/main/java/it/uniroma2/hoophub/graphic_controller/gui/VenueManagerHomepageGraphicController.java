package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ViewBookingsController;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
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

    private static final String NOTIFICATION_STYLE = "card-with-notification";

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
            UIHelper.updateNotificationBadge(notificationBadge, viewBookingsButton, unreadCount, NOTIFICATION_STYLE);
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
            UIHelper.hideNotificationBadge(notificationBadge, viewBookingsButton, NOTIFICATION_STYLE);
        }
    }

    @FXML
    private void onManageVenuesClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/manage_venues.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load manage venues page", e);
        }
    }

    @FXML
    private void onViewBookingsClick() {
        try {
            ViewBookingsGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/view_bookings.fxml",
                    ViewBookingsGraphicController.class
            );
            controller.initWithController(viewBookingsController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load view bookings page", e);
        }
    }

    private void closeCurrentStage() {
        Stage stage = (Stage) manageVenuesButton.getScene().getWindow();
        stage.close();
    }
}