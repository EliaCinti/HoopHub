package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.FanBooking;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.BookingCardHelper;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI controller for Manage Seats use case (Fan side).
 *
 * <p>Displays bookings for the current Fan, allowing cancellation
 * of pending or confirmed bookings within the deadline.</p>
 *
 * <p>Depends on {@link FanBooking} interface (ISP compliance).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ManageSeatsGraphicController {

    private static final Logger LOGGER = Logger.getLogger(ManageSeatsGraphicController.class.getName());

    // Page title
    private static final String PAGE_TITLE = "My Bookings";

    // Messages
    private static final String BOOKINGS_COUNT_MSG = "Showing %d booking(s)";
    private static final String LOAD_ERROR_MSG = "Failed to load bookings";
    private static final String NAV_ERROR_MSG = "Navigation error";
    private static final String CANCEL_SUCCESS_MSG = "Booking cancelled successfully!";
    private static final String CANCEL_ERROR_MSG = "Failed to cancel booking";

    // Filter options
    private static final String ALL_STATUSES = "All Statuses";

    // Cancellation deadline
    private static final int CANCELLATION_DEADLINE_DAYS = 2;

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

    // ISP: dipende dall'interfaccia
    private FanBooking fanBookingController;

    // State
    private List<BookingBean> currentBookings;

    @FXML
    public void initialize() {
        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        setupFilters();
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes the controller with the FanBooking interface.
     * Called by FanHomepageGraphicController after navigation.
     *
     * @param controller The FanBooking interface instance
     */
    public void initWithController(FanBooking controller) {
        this.fanBookingController = controller;
        markNotificationsAsRead();
        loadBookings();
    }

    private void setupFilters() {
        statusFilter.getItems().add(ALL_STATUSES);
        for (BookingStatus status : BookingStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.setOnAction(e -> loadBookings());
    }

    private void markNotificationsAsRead() {
        try {
            fanBookingController.markFanNotificationsAsRead();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error marking notifications as read", e);
        }
    }

    // ==================== DATA LOADING ====================

    private void loadBookings() {
        if (fanBookingController == null) {
            return;
        }

        try {
            BookingStatus statusFilterValue = getSelectedStatus();
            currentBookings = fanBookingController.getMyBookings(statusFilterValue);

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

    private BookingStatus getSelectedStatus() {
        String selected = statusFilter.getValue();
        if (ALL_STATUSES.equals(selected)) {
            return null;
        }
        return BookingStatus.valueOf(selected);
    }

    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        bookingsScrollPane.setVisible(false);
        bookingsScrollPane.setManaged(false);
        infoLabel.setText("");
    }

    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        bookingsScrollPane.setVisible(true);
        bookingsScrollPane.setManaged(true);
    }

    private void displayBookings() {
        bookingsList.getChildren().clear();

        for (BookingBean booking : currentBookings) {
            VBox bookingCard = createBookingCard(booking);
            bookingsList.getChildren().add(bookingCard);
        }
    }

    // ==================== CARD CREATION ====================

    private VBox createBookingCard(BookingBean booking) {
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-card");
        card.setPadding(new Insets(16));

        HBox headerRow = createHeaderRow(booking);
        HBox detailsRow = BookingCardHelper.createDetailsRow(booking);

        card.getChildren().addAll(headerRow, detailsRow);

        if (canBeCancelled(booking)) {
            HBox actionsRow = createActionsRow(booking);
            card.getChildren().add(actionsRow);
        }

        return card;
    }

    private HBox createHeaderRow(BookingBean booking) {
        if (canBeCancelled(booking)) {
            Label cancellableBadge = BookingCardHelper.createCancellableBadge();
            return BookingCardHelper.createHeaderRowWithExtra(booking, cancellableBadge);
        } else {
            return BookingCardHelper.createHeaderRowBase(booking);
        }
    }

    private HBox createActionsRow(BookingBean booking) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 0, 0, 0));

        Button cancelBtn = new Button("✗ Cancel Booking");
        cancelBtn.getStyleClass().add("btn-danger-small");
        cancelBtn.setOnAction(e -> onCancelClick(booking));

        row.getChildren().add(cancelBtn);
        return row;
    }

    // ==================== BUSINESS LOGIC ====================

    private boolean canBeCancelled(BookingBean booking) {
        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            return false;
        }

        LocalDate deadline = booking.getGameDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        return !LocalDate.now().isAfter(deadline);
    }

    // ==================== ACTIONS ====================

    private void onCancelClick(BookingBean booking) {
        try {
            fanBookingController.cancelBooking(booking.getId());
            UIHelper.showSuccessThenTitle(msgLabel, CANCEL_SUCCESS_MSG, PAGE_TITLE);
            loadBookings();
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error cancelling booking", e);
            UIHelper.showErrorThenTitle(msgLabel, CANCEL_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/fan_homepage.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating back", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, PAGE_TITLE);
        }
    }

    private void closeCurrentStage() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
}