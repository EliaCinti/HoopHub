package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.BookGameSeatController;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
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
 * GUI controller for Step 1 of Book Game Seat: Select Game.
 *
 * <p>Displays upcoming NBA games and allows the fan to select one.
 * Uses the same {@link BookGameSeatController} as the CLI version.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class SelectGameGraphicController {

    private static final Logger LOGGER = Logger.getLogger(SelectGameGraphicController.class.getName());

    // Page title
    private static final String PAGE_TITLE = "Select a Game";

    // Messages
    private static final String NO_GAMES_MSG = "No upcoming games available";
    private static final String GAMES_COUNT_MSG = "Showing %d games for the next 2 weeks";
    private static final String LOAD_ERROR_MSG = "Failed to load games";
    private static final String NAV_ERROR_MSG = "Navigation error";

    // Date/Time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // FXML Components
    @FXML private Button backButton;
    @FXML private Label msgLabel;
    @FXML private VBox gamesList;
    @FXML private Label infoLabel;

    // Dependencies
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final BookGameSeatController bookGameSeatController;

    // State
    private List<NbaGameBean> currentGames;

    public SelectGameGraphicController() {
        this.bookGameSeatController = new BookGameSeatController();
    }

    @FXML
    public void initialize() {
        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        loadGames();
    }

    // ==================== DATA LOADING ====================

    /**
     * Loads upcoming games from the controller.
     */
    private void loadGames() {
        try {
            currentGames = bookGameSeatController.getUpcomingGames();

            if (currentGames.isEmpty()) {
                showEmptyState();
            } else {
                displayGames();
                infoLabel.setText(String.format(GAMES_COUNT_MSG, currentGames.size()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading games", e);
            UIHelper.showErrorThenTitle(msgLabel, LOAD_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Shows empty state when no games available.
     */
    private void showEmptyState() {
        gamesList.getChildren().clear();

        Label emptyLabel = new Label(NO_GAMES_MSG);
        emptyLabel.getStyleClass().add("empty-state-label");

        gamesList.getChildren().add(emptyLabel);
        infoLabel.setText("");
    }

    /**
     * Displays the list of games as cards.
     */
    private void displayGames() {
        gamesList.getChildren().clear();

        for (NbaGameBean game : currentGames) {
            VBox gameCard = createGameCard(game);
            gamesList.getChildren().add(gameCard);
        }
    }

    // ==================== CARD CREATION ====================

    /**
     * Creates a clickable card for a game.
     */
    private VBox createGameCard(NbaGameBean game) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("list-item-card", "clickable-card");
        card.setPadding(new Insets(16));
        card.setCursor(Cursor.HAND);

        // Matchup row
        HBox matchupRow = createMatchupRow(game);

        // Date/Time row
        HBox dateTimeRow = createDateTimeRow(game);

        card.getChildren().addAll(matchupRow, dateTimeRow);

        // Click handler
        card.setOnMouseClicked(e -> onGameSelected(game));

        return card;
    }

    /**
     * Creates the matchup display row (Away @ Home).
     */
    private HBox createMatchupRow(NbaGameBean game) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Away team
        Label awayLabel = new Label(game.getAwayTeam().getDisplayName());
        awayLabel.getStyleClass().add("card-title");

        // @ symbol
        Label atLabel = new Label("@");
        atLabel.getStyleClass().add("card-subtitle");

        // Home team
        Label homeLabel = new Label(game.getHomeTeam().getDisplayName());
        homeLabel.getStyleClass().add("card-title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Arrow indicator
        Label arrowLabel = new Label("→");
        arrowLabel.getStyleClass().add("card-arrow");

        row.getChildren().addAll(awayLabel, atLabel, homeLabel, spacer, arrowLabel);
        return row;
    }

    /**
     * Creates the date/time display row.
     */
    private HBox createDateTimeRow(NbaGameBean game) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Date
        Label dateLabel = new Label(game.getDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("card-detail");

        // Time
        Label timeLabel = new Label(game.getTime().format(TIME_FORMATTER));
        timeLabel.getStyleClass().add("card-detail");

        row.getChildren().addAll(dateLabel, timeLabel);
        return row;
    }

    // ==================== NAVIGATION ====================

    /**
     * Handles game selection - navigates to venue selection.
     */
    private void onGameSelected(NbaGameBean game) {
        LOGGER.log(Level.INFO, "Game selected: {0} @ {1}",
                new Object[]{game.getAwayTeam(), game.getHomeTeam()});

        try {
            SelectVenueGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/select_venue.fxml",
                    SelectVenueGraphicController.class
            );
            // Passa sia il game che il controller applicativo
            controller.initWithGame(game, bookGameSeatController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating to venue selection", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/fan_homepage.fxml");
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