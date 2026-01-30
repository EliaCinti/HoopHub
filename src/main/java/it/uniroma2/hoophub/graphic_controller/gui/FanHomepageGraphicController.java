package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ManageSeatsController;
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
 * JavaFX graphic controller for the Fan dashboard.
 *
 * <p>Provides navigation to game booking and seat management features.
 * Shows notification badge on Manage Seats button when unread notifications exist.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanHomepageGraphicController {

    private static final Logger LOGGER = Logger.getLogger(FanHomepageGraphicController.class.getName());

    private static final String NOTIFICATION_STYLE = "card-with-notification";

    @FXML
    private Button bookSeatButton;

    @FXML
    private Button manageSeatsButton;

    @FXML
    private Label notificationBadge;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final ManageSeatsController manageSeatsController;

    public FanHomepageGraphicController() {
        this.manageSeatsController = new ManageSeatsController();
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
            int unreadCount = manageSeatsController.getUnreadNotificationsCount();
            UIHelper.updateNotificationBadge(notificationBadge, manageSeatsButton, unreadCount, NOTIFICATION_STYLE);
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
            UIHelper.hideNotificationBadge(notificationBadge, manageSeatsButton, NOTIFICATION_STYLE);
        }
    }

    @FXML
    private void onBookSeatClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/select_game.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load select game page", e);
        }
    }

    @FXML
    private void onManageSeatsClick() {
        try {
            ManageSeatsGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/manage_seats.fxml",
                    ManageSeatsGraphicController.class
            );
            controller.initWithController(manageSeatsController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load manage seats page", e);
        }
    }

    private void closeCurrentStage() {
        Stage stage = (Stage) bookSeatButton.getScene().getWindow();
        stage.close();
    }
}