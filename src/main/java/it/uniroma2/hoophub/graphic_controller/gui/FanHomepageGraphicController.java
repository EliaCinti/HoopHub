package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.NotificationController;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the Fan dashboard.
 *
 * <p>Provides navigation to booking and seat management features.
 * Updates notification indicators on menu cards based on unread notifications.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanHomepageGraphicController {

    @FXML
    private Button bookSeatButton;
    @FXML
    private Button manageSeatsButton;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final NotificationController notificationController = new NotificationController();
    private static final Logger logger = Logger.getLogger(FanHomepageGraphicController.class.getName());

    private static final String NOTIFICATION_STYLE_CLASS = "has-notification";

    /**
     * Initializes the controller and updates notification indicators.
     */
    public void initialize() {
        updateNotificationIndicators();
    }

    /**
     * Updates CSS style class on cards based on unread booking notifications.
     */
    private void updateNotificationIndicators() {
        boolean hasBookingNotifications =
                notificationController.hasUnreadNotificationsOfType(NotificationType.BOOKING_APPROVED) ||
                        notificationController.hasUnreadNotificationsOfType(NotificationType.BOOKING_REJECTED) ||
                        notificationController.hasUnreadNotificationsOfType(NotificationType.BOOKING_CANCELLED);

        if (hasBookingNotifications) {
            manageSeatsButton.getStyleClass().add(NOTIFICATION_STYLE_CLASS);
        } else {
            manageSeatsButton.getStyleClass().remove(NOTIFICATION_STYLE_CLASS);
        }
    }

    /**
     * Handles "Book Game Seat" button click.
     */
    @FXML
    private void onBookSeatClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/book_seat.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load book seat page", e);
        }
    }

    /**
     * Handles "Manage My Seats" button click.
     */
    @FXML
    private void onManageSeatsClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/manage_seats.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load manage seats page", e);
        }
    }

    private void closeCurrentWindow() {
        Stage stage = (Stage) bookSeatButton.getScene().getWindow();
        stage.close();
    }
}