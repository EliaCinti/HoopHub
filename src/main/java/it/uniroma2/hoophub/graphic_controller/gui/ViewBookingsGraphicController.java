package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ViewBookingsController;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
 * GUI controller for View Bookings use case (VenueManager side).
 *
 * <p>Displays booking requests for venues owned by the current VenueManager,
 * allowing approval or rejection of pending bookings.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ViewBookingsGraphicController {

    private static final Logger LOGGER = Logger.getLogger(ViewBookingsGraphicController.class.getName());

    // Page title
    private static final String PAGE_TITLE = "View Bookings";

    // Messages
    private static final String BOOKINGS_COUNT_MSG = "Showing %d booking(s)";
    private static final String LOAD_ERROR_MSG = "Failed to load bookings";
    private static final String NAV_ERROR_MSG = "Navigation error";
    private static final String APPROVE_SUCCESS_MSG = "Booking approved successfully!";
    private static final String REJECT_SUCCESS_MSG = "Booking rejected";
    private static final String APPROVE_ERROR_MSG = "Failed to approve booking";
    private static final String REJECT_ERROR_MSG = "Failed to reject booking";

    // Filter options
    private static final String ALL_STATUSES = "All Statuses";

    // Style classes
    private static final String STYLE_CARD_DETAIL = "card-detail";

    // Date/Time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // FXML Components
    @FXML private Button backButton;
    @FXML private Label msgLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ScrollPane bookingsScrollPane;
    @FXML private VBox bookingsList;
    @FXML private VBox emptyState;
    @FXML private Label infoLabel;

    // Dependencies
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private ViewBookingsController viewBookingsController;

    // State
    private List<BookingBean> currentBookings;

    @FXML
    public void initialize() {
        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        setupFilters();
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes the controller with the application controller instance.
     * Called by VenueManagerHomepageGraphicController after navigation.
     *
     * @param appController The application controller instance
     */
    public void initWithController(ViewBookingsController appController) {
        this.viewBookingsController = appController;
        markNotificationsAsRead();
        loadBookings();
    }

    /**
     * Sets up the status filter dropdown.
     */
    private void setupFilters() {
        statusFilter.getItems().add(ALL_STATUSES);
        for (BookingStatus status : BookingStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.setOnAction(e -> loadBookings());
    }

    /**
     * Marks notifications as read when entering the screen.
     */
    private void markNotificationsAsRead() {
        try {
            viewBookingsController.markNotificationsAsRead();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error marking notifications as read", e);
        }
    }

    // ==================== DATA LOADING ====================

    /**
     * Loads bookings based on selected filter.
     */
    private void loadBookings() {
        if (viewBookingsController == null) {
            return;
        }

        try {
            BookingStatus statusFilterValue = getSelectedStatus();
            currentBookings = viewBookingsController.getBookingsForMyVenues(statusFilterValue);

            if (currentBookings.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                displayBookings();
                infoLabel.setText(String.format(BOOKINGS_COUNT_MSG, currentBookings.size()));
            }
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.SEVERE, "Error loading bookings", e);
            UIHelper.showErrorThenTitle(msgLabel, LOAD_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Gets the selected status filter (null for all).
     */
    private BookingStatus getSelectedStatus() {
        String selected = statusFilter.getValue();
        if (ALL_STATUSES.equals(selected)) {
            return null;
        }
        return BookingStatus.valueOf(selected);
    }

    /**
     * Shows empty state.
     */
    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        bookingsScrollPane.setVisible(false);
        bookingsScrollPane.setManaged(false);
        infoLabel.setText("");
    }

    /**
     * Hides empty state.
     */
    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        bookingsScrollPane.setVisible(true);
        bookingsScrollPane.setManaged(true);
    }

    /**
     * Displays bookings as cards.
     */
    private void displayBookings() {
        bookingsList.getChildren().clear();

        for (BookingBean booking : currentBookings) {
            VBox bookingCard = createBookingCard(booking);
            bookingsList.getChildren().add(bookingCard);
        }
    }

    // ==================== CARD CREATION ====================

    /**
     * Creates a booking card.
     */
    private VBox createBookingCard(BookingBean booking) {
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-card");
        card.setPadding(new Insets(16));

        // Header row: matchup + status badge
        HBox headerRow = createHeaderRow(booking);

        // Details row: date, time, venue
        HBox detailsRow = createDetailsRow(booking);

        // Fan row
        HBox fanRow = createFanRow(booking);

        card.getChildren().addAll(headerRow, detailsRow, fanRow);

        // Actions row (only for PENDING)
        if (booking.getStatus() == BookingStatus.PENDING) {
            HBox actionsRow = createActionsRow(booking);
            card.getChildren().add(actionsRow);
        }

        return card;
    }

    /**
     * Creates the header row with matchup and status badge.
     */
    private HBox createHeaderRow(BookingBean booking) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Matchup
        String matchup = booking.getAwayTeam().getDisplayName() + " @ " +
                booking.getHomeTeam().getDisplayName();
        Label matchupLabel = new Label(matchup);
        matchupLabel.getStyleClass().add("card-title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        Label statusBadge = createStatusBadge(booking.getStatus());

        row.getChildren().addAll(matchupLabel, spacer, statusBadge);
        return row;
    }

    /**
     * Creates a status badge label.
     */
    private Label createStatusBadge(BookingStatus status) {
        Label badge = new Label(status.name());
        badge.getStyleClass().add("badge");

        switch (status) {
            case PENDING -> badge.getStyleClass().add("badge-warning");
            case CONFIRMED -> badge.getStyleClass().add("badge-success");
            case REJECTED -> badge.getStyleClass().add("badge-error");
            case CANCELLED -> badge.getStyleClass().add("badge-muted");
        }

        return badge;
    }

    /**
     * Creates the details row with date, time, venue.
     */
    private HBox createDetailsRow(BookingBean booking) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Date
        Label dateLabel = new Label(booking.getGameDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        // Time
        Label timeLabel = new Label(booking.getGameTime().format(TIME_FORMATTER));
        timeLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        // Venue
        Label venueLabel = new Label("📍 " + booking.getVenueName());
        venueLabel.getStyleClass().add(STYLE_CARD_DETAIL);

        row.getChildren().addAll(dateLabel, timeLabel, venueLabel);
        return row;
    }

    /**
     * Creates the fan info row.
     */
    private HBox createFanRow(BookingBean booking) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label fanLabel = new Label("👤 " + booking.getFanUsername());
        fanLabel.getStyleClass().add("card-subtitle");

        row.getChildren().add(fanLabel);
        return row;
    }

    /**
     * Creates action buttons for pending bookings.
     */
    private HBox createActionsRow(BookingBean booking) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 0, 0, 0));

        // Approve button
        Button approveBtn = new Button("✓ Approve");
        approveBtn.getStyleClass().add("btn-success-small");
        approveBtn.setOnAction(e -> onApproveClick(booking));

        // Reject button
        Button rejectBtn = new Button("✗ Reject");
        rejectBtn.getStyleClass().add("btn-danger-small");
        rejectBtn.setOnAction(e -> onRejectClick(booking));

        row.getChildren().addAll(approveBtn, rejectBtn);
        return row;
    }

    // ==================== ACTIONS ====================

    /**
     * Handles approve button click.
     */
    private void onApproveClick(BookingBean booking) {
        try {
            viewBookingsController.approveBooking(booking.getId());
            UIHelper.showSuccessThenTitle(msgLabel, APPROVE_SUCCESS_MSG, PAGE_TITLE);
            loadBookings(); // Refresh list
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error approving booking", e);
            UIHelper.showErrorThenTitle(msgLabel, APPROVE_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    /**
     * Handles reject button click.
     */
    private void onRejectClick(BookingBean booking) {
        try {
            viewBookingsController.rejectBooking(booking.getId());
            UIHelper.showSuccessThenTitle(msgLabel, REJECT_SUCCESS_MSG, PAGE_TITLE);
            loadBookings(); // Refresh list
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error rejecting booking", e);
            UIHelper.showErrorThenTitle(msgLabel, REJECT_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/venue_manager_homepage.fxml");
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
