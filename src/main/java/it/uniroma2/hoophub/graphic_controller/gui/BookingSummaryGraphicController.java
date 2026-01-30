package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.FanBooking;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI controller for Step 3 of Book Game Seat: Booking Summary.
 *
 * <p>Displays booking summary and handles confirmation or waitlist.
 * Depends on {@link FanBooking} interface (ISP compliance).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingSummaryGraphicController {

    private static final Logger LOGGER = Logger.getLogger(BookingSummaryGraphicController.class.getName());

    // Page titles
    private static final String PAGE_TITLE = "Booking Summary";
    private static final String PAGE_TITLE_WAITLIST = "Join Waitlist";

    // Messages
    private static final String BOOKING_SUCCESS_MSG = "Booking request sent successfully!";
    private static final String BOOKING_PENDING_MSG = "Your booking is now PENDING. The venue manager will review it.";
    private static final String BOOKING_ERROR_MSG = "Failed to create booking";
    private static final String ALREADY_BOOKED_MSG = "You have already booked this game!";
    private static final String WAITLIST_MSG = "Waitlist feature is not yet available.";
    private static final String REDIRECT_MSG = "Redirecting to homepage in %d seconds...";
    private static final String NAV_ERROR_MSG = "Navigation error";
    private static final String SEATS_AVAILABLE_MSG = "%d seats available";
    private static final String NO_SEATS_MSG = "No seats available";

    // Redirect delay
    private static final int REDIRECT_DELAY_SECONDS = 5;

    // Date/Time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // FXML Components
    @FXML private Button backButton;
    @FXML private Label msgLabel;
    @FXML private Label matchupLabel;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Label venueNameLabel;
    @FXML private Label venueTypeLabel;
    @FXML private Label venueAddressLabel;
    @FXML private Label availabilityLabel;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    // Dependencies
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();

    // ISP: dipende dall'interfaccia
    private FanBooking fanBookingController;

    // State
    private NbaGameBean selectedGame;
    private VenueBean selectedVenue;
    private int availableSeats;
    private boolean isWaitlistMode;

    public BookingSummaryGraphicController() {
        // empty constructor
    }

    @FXML
    public void initialize() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes the controller with booking data and FanBooking interface.
     * Called by SelectVenueGraphicController after navigation.
     *
     * @param game           The selected game
     * @param venue          The selected venue
     * @param availableSeats Number of available seats
     * @param controller     The FanBooking interface instance
     */
    public void initWithData(NbaGameBean game, VenueBean venue, int availableSeats,
                             FanBooking controller) {
        this.selectedGame = game;
        this.selectedVenue = venue;
        this.availableSeats = availableSeats;
        this.fanBookingController = controller;
        this.isWaitlistMode = availableSeats <= 0;

        if (isWaitlistMode) {
            UIHelper.showTitle(msgLabel, PAGE_TITLE_WAITLIST);
            confirmButton.setText(PAGE_TITLE_WAITLIST);
        } else {
            UIHelper.showTitle(msgLabel, PAGE_TITLE);
            confirmButton.setText("Confirm Booking");
        }

        displaySummary();
    }

    /**
     * Displays the booking summary.
     */
    private void displaySummary() {
        String matchup = selectedGame.getAwayTeam().getDisplayName() +
                " @ " + selectedGame.getHomeTeam().getDisplayName();
        matchupLabel.setText(matchup);
        dateLabel.setText(selectedGame.getDate().format(DATE_FORMATTER));
        timeLabel.setText(selectedGame.getTime().format(TIME_FORMATTER));

        venueNameLabel.setText(selectedVenue.getName());
        venueTypeLabel.setText(selectedVenue.getType().getDisplayName());
        venueAddressLabel.setText(selectedVenue.getAddress() + ", " + selectedVenue.getCity());

        if (availableSeats > 0) {
            availabilityLabel.setText(String.format(SEATS_AVAILABLE_MSG, availableSeats));
            availabilityLabel.getStyleClass().addAll("badge", "badge-success");
        } else {
            availabilityLabel.setText(NO_SEATS_MSG);
            availabilityLabel.getStyleClass().addAll("badge", "badge-warning");
        }
    }

    // ==================== EVENT HANDLERS ====================

    @FXML
    private void onConfirmClick() {
        if (isWaitlistMode) {
            handleWaitlist();
        } else {
            handleBooking();
        }
    }

    /**
     * Handles booking confirmation.
     */
    private void handleBooking() {
        try {
            if (fanBookingController.hasAlreadyBooked(selectedGame)) {
                UIHelper.showErrorThenTitle(msgLabel, ALREADY_BOOKED_MSG, PAGE_TITLE);
                return;
            }

            fanBookingController.createBookingRequest(selectedGame, selectedVenue.getId());

            showSuccessAndRedirect();

        } catch (UserSessionException | DAOException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error creating booking", e);
            UIHelper.showErrorThenTitle(msgLabel, BOOKING_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Handles waitlist option.
     */
    private void handleWaitlist() {
        UIHelper.showMessage(msgLabel, WAITLIST_MSG);
        showRedirectCountdown();
    }

    /**
     * Shows success message and starts redirect countdown.
     */
    private void showSuccessAndRedirect() {
        confirmButton.setDisable(true);
        cancelButton.setDisable(true);
        backButton.setDisable(true);

        UIHelper.showSuccess(msgLabel, BOOKING_SUCCESS_MSG);

        statusLabel.setText(BOOKING_PENDING_MSG);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        showRedirectCountdown();
    }

    /**
     * Shows redirect countdown and navigates to homepage using JavaFX Timeline.
     */
    private void showRedirectCountdown() {
        confirmButton.setDisable(true);
        cancelButton.setDisable(true);

        if (backButton != null) backButton.setDisable(true);

        updateCountdownOrNavigate(REDIRECT_DELAY_SECONDS);

        Timeline timeline = new Timeline();
        timeline.setCycleCount(REDIRECT_DELAY_SECONDS);

        final int[] timeSeconds = {REDIRECT_DELAY_SECONDS};

        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeSeconds[0]--;
                    updateCountdownOrNavigate(timeSeconds[0]);
                })
        );

        timeline.play();
    }

    /**
     * Updates countdown label or navigates to homepage.
     */
    private void updateCountdownOrNavigate(int secondsLeft) {
        if (secondsLeft > 0) {
            statusLabel.setText(String.format(REDIRECT_MSG, secondsLeft));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        } else {
            navigateToHomepage();
        }
    }

    /**
     * Navigates back to fan homepage.
     */
    private void navigateToHomepage() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/fan_homepage.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating to homepage", e);
        }
    }

    @FXML
    private void onCancelClick() {
        onBackClick();
    }

    @FXML
    private void onBackClick() {
        try {
            SelectVenueGraphicController controller = navigatorSingleton.gotoPage(
                    "/it/uniroma2/hoophub/fxml/select_venue.fxml",
                    SelectVenueGraphicController.class
            );
            controller.initWithGame(selectedGame, fanBookingController);
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating back", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, PAGE_TITLE);
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