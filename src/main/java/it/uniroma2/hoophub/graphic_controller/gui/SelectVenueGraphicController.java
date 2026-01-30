package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.BookGameSeatController;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.VenueType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI controller for Step 2 of Book Game Seat: Select Venue.
 *
 * <p>Displays venues that broadcast the selected game and allows filtering.
 * Uses the same {@link BookGameSeatController} as the CLI version.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class SelectVenueGraphicController {

    private static final Logger LOGGER = Logger.getLogger(SelectVenueGraphicController.class.getName());

    // Page title
    private static final String PAGE_TITLE = "Select a Venue";

    // Messages
    private static final String NO_VENUES_MSG = "No venues available for this game";
    private static final String VENUES_COUNT_MSG = "%d venues found";
    private static final String LOAD_ERROR_MSG = "Failed to load venues";
    private static final String NAV_ERROR_MSG = "Navigation error";
    private static final String SEATS_AVAILABLE_MSG = "%d seats available";
    private static final String NO_SEATS_MSG = "FULL - Waitlist only";

    // Filter options
    private static final String ALL_CITIES = "All Cities";
    private static final String ALL_TYPES = "All Types";

    // Date formatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    // FXML Components
    @FXML private Button backButton;
    @FXML private Label msgLabel;
    @FXML private Label gameInfoLabel;
    @FXML private ComboBox<String> cityFilter;
    @FXML private ComboBox<String> typeFilter;
    @FXML private CheckBox availableOnlyFilter;
    @FXML private VBox venuesList;
    @FXML private Label infoLabel;

    // Dependencies
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private BookGameSeatController bookGameSeatController;

    // State
    private NbaGameBean selectedGame;
    private List<VenueBean> currentVenues;

    public SelectVenueGraphicController() {
        // empty constructor
    }

    @FXML
    public void initialize() {
        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        setupFilters();
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes the controller with the selected game.
     * Called by SelectGameGraphicController after navigation.
     *
     * @param game The selected game
     */
    public void initWithGame(NbaGameBean game, BookGameSeatController bookGameSeatController) {
        this.selectedGame = game;
        this.bookGameSeatController = bookGameSeatController;
        updateGameInfoLabel();
        loadCityFilter();
        loadVenues();
    }

    /**
     * Updates the game info label.
     */
    private void updateGameInfoLabel() {
        if (selectedGame != null) {
            String gameInfo = String.format("%s @ %s • %s",
                    selectedGame.getAwayTeam().getDisplayName(),
                    selectedGame.getHomeTeam().getDisplayName(),
                    selectedGame.getDate().format(DATE_FORMATTER));
            gameInfoLabel.setText(gameInfo);
        }
    }

    /**
     * Sets up filter components.
     */
    private void setupFilters() {
        // Type filter
        typeFilter.getItems().add(ALL_TYPES);
        for (VenueType type : VenueType.values()) {
            typeFilter.getItems().add(type.getDisplayName());
        }
        typeFilter.setValue(ALL_TYPES);

        // Filter change listeners
        cityFilter.setOnAction(e -> applyFilters());
        typeFilter.setOnAction(e -> applyFilters());
        availableOnlyFilter.setOnAction(e -> applyFilters());
    }

    /**
     * Loads available cities for the filter dropdown.
     */
    private void loadCityFilter() {
        try {
            List<String> cities = bookGameSeatController.getAvailableCitiesForGame(selectedGame);
            cityFilter.getItems().clear();
            cityFilter.getItems().add(ALL_CITIES);
            cityFilter.getItems().addAll(cities);
            cityFilter.setValue(ALL_CITIES);
        } catch (DAOException e) {
            LOGGER.log(Level.WARNING, "Error loading cities for filter", e);
            cityFilter.getItems().clear();
            cityFilter.getItems().add(ALL_CITIES);
            cityFilter.setValue(ALL_CITIES);
        }
    }

    // ==================== DATA LOADING ====================

    /**
     * Loads venues based on current filters.
     */
    private void loadVenues() {
        if (selectedGame == null) {
            return;
        }

        try {
            String city = getSelectedCity();
            String type = getSelectedType();
            boolean onlyAvailable = availableOnlyFilter.isSelected();

            currentVenues = bookGameSeatController.getVenuesForGame(
                    selectedGame, city, type, onlyAvailable);

            if (currentVenues.isEmpty()) {
                showEmptyState();
            } else {
                displayVenues();
                infoLabel.setText(String.format(VENUES_COUNT_MSG, currentVenues.size()));
            }
        } catch (DAOException e) {
            LOGGER.log(Level.SEVERE, "Error loading venues", e);
            UIHelper.showErrorThenTitle(msgLabel, LOAD_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Gets selected city from filter (null if "All Cities").
     */
    private String getSelectedCity() {
        String selected = cityFilter.getValue();
        return ALL_CITIES.equals(selected) ? null : selected;
    }

    /**
     * Gets selected type from filter (null if "All Types").
     */
    private String getSelectedType() {
        String selected = typeFilter.getValue();
        if (ALL_TYPES.equals(selected)) {
            return null;
        }
        // Convert display name back to enum name
        for (VenueType type : VenueType.values()) {
            if (type.getDisplayName().equals(selected)) {
                return type.name();
            }
        }
        return null;
    }

    /**
     * Applies filters and reloads venues.
     */
    private void applyFilters() {
        loadVenues();
    }

    /**
     * Shows empty state when no venues available.
     */
    private void showEmptyState() {
        venuesList.getChildren().clear();

        Label emptyLabel = new Label(NO_VENUES_MSG);
        emptyLabel.getStyleClass().add("empty-state-label");

        venuesList.getChildren().add(emptyLabel);
        infoLabel.setText("");
    }

    /**
     * Displays the list of venues as cards.
     */
    private void displayVenues() {
        venuesList.getChildren().clear();

        for (VenueBean venue : currentVenues) {
            VBox venueCard = createVenueCard(venue);
            venuesList.getChildren().add(venueCard);
        }
    }

    // ==================== CARD CREATION ====================

    /**
     * Creates a clickable card for a venue.
     */
    private VBox createVenueCard(VenueBean venue) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("list-item-card", "clickable-card");
        card.setPadding(new Insets(16));
        card.setCursor(Cursor.HAND);

        int availableSeats = bookGameSeatController.getAvailableSeats(venue.getId(), selectedGame);

        // Header row (name + availability)
        HBox headerRow = createVenueHeaderRow(venue, availableSeats);

        // Details row
        HBox detailsRow = createVenueDetailsRow(venue);

        // Address row
        Label addressLabel = new Label(venue.getAddress() + ", " + venue.getCity());
        addressLabel.getStyleClass().add("card-detail");

        card.getChildren().addAll(headerRow, detailsRow, addressLabel);

        // Click handler
        card.setOnMouseClicked(e -> onVenueSelected(venue, availableSeats));

        return card;
    }

    /**
     * Creates the venue header row (name + seats availability).
     */
    private HBox createVenueHeaderRow(VenueBean venue, int availableSeats) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Venue name
        Label nameLabel = new Label(venue.getName());
        nameLabel.getStyleClass().add("card-title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Availability badge
        Label availabilityLabel;
        if (availableSeats > 0) {
            availabilityLabel = new Label(String.format(SEATS_AVAILABLE_MSG, availableSeats));
            availabilityLabel.getStyleClass().addAll("badge", "badge-success");
        } else {
            availabilityLabel = new Label(NO_SEATS_MSG);
            availabilityLabel.getStyleClass().addAll("badge", "badge-warning");
        }

        row.getChildren().addAll(nameLabel, spacer, availabilityLabel);
        return row;
    }

    /**
     * Creates the venue details row (type + teams).
     */
    private HBox createVenueDetailsRow(VenueBean venue) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Type
        Label typeLabel = new Label(venue.getType().getDisplayName());
        typeLabel.getStyleClass().add("card-subtitle");

        // Teams
        Label teamsLabel = new Label(VenueBean.formatTeamsForDisplay(venue.getAssociatedTeams()));
        teamsLabel.getStyleClass().add("card-detail");

        row.getChildren().addAll(typeLabel, teamsLabel);
        return row;
    }

    // ==================== NAVIGATION ====================

    /**
     * Handles venue selection - navigates to booking summary.
     */
    private void onVenueSelected(VenueBean venue, int availableSeats) {
        LOGGER.log(Level.INFO, "Venue selected: {0}", venue.getName());

        try {
            BookingSummaryGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/booking_summary.fxml",
                    BookingSummaryGraphicController.class
            );
            // Passa game, venue, seats E il controller applicativo
            controller.initWithData(selectedGame, venue, availableSeats, bookGameSeatController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating to booking summary", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/select_game.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating back", e);
        }
    }

    /**
     * Closes the current stage.
     */
    private void closeCurrentStage() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
}