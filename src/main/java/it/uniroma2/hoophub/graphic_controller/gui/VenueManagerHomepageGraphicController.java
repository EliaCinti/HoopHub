package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the VenueManager dashboard.
 *
 * <p>Provides navigation to venue management and booking approval features.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerHomepageGraphicController {

    @FXML
    private Button manageVenuesButton;

    @FXML
    private Button viewBookingsButton;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(VenueManagerHomepageGraphicController.class.getName());

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        // Homepage-specific initialization if needed
    }

    /**
     * Handles "Manage Venues" button click.
     */
    @FXML
    private void onManageVenuesClick() {
        // TODO: Navigate to manage venues page
        logger.info("Manage Venues clicked");
    }

    /**
     * Handles "View Bookings" button click.
     */
    @FXML
    private void onViewBookingsClick() {
        // TODO: Navigate to view bookings page
        logger.info("View Bookings clicked");
    }
}