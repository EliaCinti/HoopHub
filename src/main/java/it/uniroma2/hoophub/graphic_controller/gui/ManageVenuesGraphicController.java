package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ManageVenuesController;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the Manage Venues screen.
 * Displays a list of venues owned by the current VenueManager with CRUD operations.
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ManageVenuesGraphicController {

    private static final Logger logger = Logger.getLogger(ManageVenuesGraphicController.class.getName());

    // Page Title
    private static final String PAGE_TITLE = "My Venues";

    // Error Messages
    private static final String LOAD_ERROR_MSG = "Failed to load venues";
    private static final String DELETE_ERROR_MSG = "Failed to delete venue";
    private static final String NAV_ERROR_MSG = "Error loading page";

    // Success Messages
    private static final String DELETE_SUCCESS_MSG = "Venue deleted successfully!";

    // FXML Components
    @FXML private Label msgLabel;
    @FXML private ScrollPane venuesScrollPane;
    @FXML private VBox venuesContainer;
    @FXML private VBox emptyState;
    @FXML private Button addVenueButton;

    private final ManageVenuesController manageVenuesController = new ManageVenuesController();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();

    /**
     * Initializes the controller and loads venues.
     */
    @FXML
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);
        msgLabel.setOpacity(0.0);

        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        loadVenues();
    }

    /**
     * Loads and displays all venues for the current venue manager.
     */
    private void loadVenues() {
        try {
            List<VenueBean> venues = manageVenuesController.getMyVenues();
            venuesContainer.getChildren().clear();

            if (venues.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                for (VenueBean venue : venues) {
                    HBox venueCard = createVenueCard(venue);
                    venuesContainer.getChildren().add(venueCard);
                }
            }
        } catch (UserSessionException | DAOException e) {
            logger.log(Level.SEVERE, "Error loading venues", e);
            UIHelper.showErrorThenTitle(msgLabel, LOAD_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Creates a styled card for a single venue.
     */
    private HBox createVenueCard(VenueBean venue) {
        HBox card = new HBox(16);
        card.getStyleClass().add("venue-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));

        VBox infoBox = createVenueInfoBox(venue);
        HBox actionsBox = createVenueActionsBox(venue);

        card.getChildren().addAll(infoBox, actionsBox);
        return card;
    }

    /**
     * Creates the info section of a venue card.
     */
    private VBox createVenueInfoBox(VenueBean venue) {
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(venue.getName());
        nameLabel.getStyleClass().add("venue-card-name");

        Label detailsLabel = new Label(String.format("%s • %s",
                venue.getType().getDisplayName(), venue.getCity()));
        detailsLabel.getStyleClass().add("venue-card-details");

        String teamsText = formatTeamsText(venue.getAssociatedTeams());
        Label statsLabel = new Label(String.format("Capacity: %d • %s",
                venue.getMaxCapacity(), teamsText));
        statsLabel.getStyleClass().add("venue-card-stats");

        infoBox.getChildren().addAll(nameLabel, detailsLabel, statsLabel);
        return infoBox;
    }

    /**
     * Creates the action buttons section of a venue card.
     */
    private HBox createVenueActionsBox(VenueBean venue) {
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("btn-secondary-small");
        editButton.setOnAction(e -> onEditVenue(venue));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("btn-danger-small");
        deleteButton.setOnAction(e -> onDeleteVenue(venue));

        actionsBox.getChildren().addAll(editButton, deleteButton);
        return actionsBox;
    }

    /**
     * Formats the teams text for display.
     */
    private String formatTeamsText(Set<TeamNBA> teams) {
        return VenueBean.formatTeamsForDisplay(teams);
    }

    /**
     * Shows the empty state when no venues exist.
     */
    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        venuesScrollPane.setVisible(false);
        venuesScrollPane.setManaged(false);
    }

    /**
     * Hides the empty state when venues exist.
     */
    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        venuesScrollPane.setVisible(true);
        venuesScrollPane.setManaged(true);
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Handles back button click - returns to homepage.
     */
    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/venue_manager_homepage.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load homepage", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, PAGE_TITLE);
        }
    }

    /**
     * Handles add venue button click - navigates to add form.
     */
    @FXML
    private void onAddVenueClick() {
        try {
            AddEditVenueGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/add_edit_venue.fxml",
                    AddEditVenueGraphicController.class
            );
            // Passa il controller applicativo
            controller.initAddMode(manageVenuesController);
            closeCurrentStage();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load add venue page", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, PAGE_TITLE);
        }
    }

    /**
     * Handles edit button click on a venue card.
     */
    private void onEditVenue(VenueBean venue) {
        try {
            AddEditVenueGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/add_edit_venue.fxml",
                    AddEditVenueGraphicController.class
            );
            // Passa venue E il controller applicativo
            controller.initEditMode(venue, manageVenuesController);
            closeCurrentStage();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load edit venue page", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, PAGE_TITLE);
        }
    }

    /**
     * Handles delete button click on a venue card.
     */
    private void onDeleteVenue(VenueBean venue) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Venue");
        confirmDialog.setHeaderText("Delete \"" + venue.getName() + "\"?");
        confirmDialog.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                manageVenuesController.deleteVenue(venue.getId());
                UIHelper.showSuccessThenTitle(msgLabel, DELETE_SUCCESS_MSG, PAGE_TITLE);
                loadVenues();
            } catch (UserSessionException | DAOException e) {
                logger.log(Level.SEVERE, "Error deleting venue", e);
                UIHelper.showErrorThenTitle(msgLabel, DELETE_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
            }
        }
    }

    /**
     * Closes the current stage.
     */
    private void closeCurrentStage() {
        Stage stage = (Stage) addVenueButton.getScene().getWindow();
        stage.close();
    }
}