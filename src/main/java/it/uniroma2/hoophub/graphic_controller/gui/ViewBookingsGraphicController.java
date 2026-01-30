package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.VenueManagerBooking;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI controller for View Bookings use case (VenueManager side).
 *
 * <p>Displays booking requests for venues owned by the current VenueManager,
 * allowing approval or rejection of pending bookings.</p>
 *
 * <p>Depends on {@link VenueManagerBooking} interface (ISP compliance).</p>
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
    private VenueManagerBooking vmBookingController;

    // State
    private List<BookingBean> currentBookings;

    @FXML
    public void initialize() {
        UIHelper.showTitle(msgLabel, PAGE_TITLE);
        setupFilters();
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes the controller with the VenueManagerBooking interface.
     * Called by VenueManagerHomepageGraphicController after navigation.
     *
     * @param controller The VenueManagerBooking interface instance
     */
    public void initWithController(VenueManagerBooking controller) {
        this.vmBookingController = controller;
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
            vmBookingController.markVmNotificationsAsRead();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error marking notifications as read", e);
        }
    }

    // ==================== DATA LOADING ====================

    private void loadBookings() {
        if (vmBookingController == null) {
            return;
        }

        try {
            BookingStatus statusFilterValue = getSelectedStatus();
            currentBookings = vmBookingController.getBookingsForMyVenues(statusFilterValue);

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

        HBox headerRow = BookingCardHelper.createHeaderRowBase(booking);
        HBox detailsRow = BookingCardHelper.createDetailsRow(booking);
        HBox fanRow = createFanRow(booking);

        card.getChildren().addAll(headerRow, detailsRow, fanRow);

        if (booking.getStatus() == BookingStatus.PENDING) {
            HBox actionsRow = createActionsRow(booking);
            card.getChildren().add(actionsRow);
        }

        return card;
    }

    private HBox createFanRow(BookingBean booking) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label fanLabel = new Label("👤 " + booking.getFanUsername());
        fanLabel.getStyleClass().add("card-subtitle");

        row.getChildren().add(fanLabel);
        return row;
    }

    private HBox createActionsRow(BookingBean booking) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 0, 0, 0));

        Button approveBtn = new Button("✓ Approve");
        approveBtn.getStyleClass().add("btn-success-small");
        approveBtn.setOnAction(e -> onApproveClick(booking));

        Button rejectBtn = new Button("✗ Reject");
        rejectBtn.getStyleClass().add("btn-danger-small");
        rejectBtn.setOnAction(e -> onRejectClick(booking));

        row.getChildren().addAll(approveBtn, rejectBtn);
        return row;
    }

    // ==================== ACTIONS ====================

    private void onApproveClick(BookingBean booking) {
        try {
            vmBookingController.approveBooking(booking.getId());
            UIHelper.showSuccessThenTitle(msgLabel, APPROVE_SUCCESS_MSG, PAGE_TITLE);
            loadBookings();
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error approving booking", e);
            UIHelper.showErrorThenTitle(msgLabel, APPROVE_ERROR_MSG + ": " + e.getMessage(), PAGE_TITLE);
        }
    }

    private void onRejectClick(BookingBean booking) {
        try {
            vmBookingController.rejectBooking(booking.getId());
            UIHelper.showSuccessThenTitle(msgLabel, REJECT_SUCCESS_MSG, PAGE_TITLE);
            loadBookings();
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

    private void closeCurrentStage() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
}