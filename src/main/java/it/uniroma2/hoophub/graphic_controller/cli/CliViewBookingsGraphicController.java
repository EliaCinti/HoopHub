package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.ViewBookingsController;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the View Bookings use case (VenueManager side).
 *
 * <p>Extends {@link AbstractCliBookingListController} implementing the
 * Template Method pattern with VenueManager-specific actions (approve/reject).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliViewBookingsGraphicController extends AbstractCliBookingListController {

    private static final Logger LOGGER = Logger.getLogger(CliViewBookingsGraphicController.class.getName());

    // Titles
    private static final String TITLE = "HOOPHUB - VIEW BOOKINGS";
    private static final String TITLE_MANAGE = "HOOPHUB - MANAGE BOOKING";
    private static final String BOOKINGS_HEADER = "=== BOOKINGS ===";

    // Menu Options (specific)
    private static final String OPTION_APPROVE = "A";
    private static final String OPTION_REJECT = "R";

    // Messages
    private static final String NO_BOOKINGS_MSG = "No bookings found.";
    private static final String UNREAD_NOTIFICATIONS_MSG = "You have %d new booking notification(s)!";
    private static final String ONLY_PENDING_MSG = "Only PENDING bookings can be managed.";
    private static final String APPROVE_SUCCESS_MSG = "Booking APPROVED successfully!";
    private static final String REJECT_SUCCESS_MSG = "Booking REJECTED.";
    private static final String FAN_NOTIFIED_MSG = "The fan will be notified.";
    private static final String APPROVE_CANCELLED_MSG = "Approval cancelled.";
    private static final String REJECT_CANCELLED_MSG = "Rejection cancelled.";
    private static final String APPROVE_ERROR_MSG = "Failed to approve booking";
    private static final String REJECT_ERROR_MSG = "Failed to reject booking";

    // Prompts
    private static final String BOOKING_SELECT_PROMPT = "Enter booking number to manage (or B to go back): ";
    private static final String ACTION_PROMPT = "Select action [A/R/B]: ";
    private static final String APPROVE_CONFIRM_PROMPT = "Are you sure you want to APPROVE this booking? (y/n): ";
    private static final String REJECT_CONFIRM_PROMPT = "Are you sure you want to REJECT this booking? (y/n): ";

    private final ViewBookingsController viewBookingsController;

    public CliViewBookingsGraphicController() {
        this.viewBookingsController = new ViewBookingsController();
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected String getBookingsHeader() {
        return BOOKINGS_HEADER;
    }

    @Override
    protected String getEmptyMessage() {
        return NO_BOOKINGS_MSG;
    }

    @Override
    protected String getUnreadNotificationsMessage() {
        return UNREAD_NOTIFICATIONS_MSG;
    }

    @Override
    protected int fetchUnreadNotificationsCount() {
        try {
            return viewBookingsController.getUnreadNotificationsCount();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
            return -1;
        }
    }

    @Override
    protected void markAllNotificationsAsRead() {
        try {
            viewBookingsController.markNotificationsAsRead();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error marking notifications as read", e);
        }
    }

    @Override
    protected void loadBookings(BookingStatus statusFilter) {
        try {
            currentBookings = viewBookingsController.getBookingsForMyVenues(statusFilter);
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.SEVERE, "Error loading bookings", e);
            printError(LOAD_ERROR_MSG + ": " + e.getMessage());
            currentBookings = new ArrayList<>();
        }
    }

    @Override
    protected void displayBookingItem(int number, BookingBean booking) {
        String statusBadge = getStatusBadge(booking.getStatus());

        printNewLine();
        print(String.format("  %2d. %s %s", number, formatMatchup(booking), statusBadge));
        print(String.format("      %s", formatDateTime(booking)));
        print(String.format("      Venue: %s", booking.getVenueName()));
        print(String.format("      Fan: %s", booking.getFanUsername()));
    }

    @Override
    protected void promptBookingSelection() {
        printNewLine();
        String input = readInput(BOOKING_SELECT_PROMPT).toUpperCase();

        if (OPTION_BACK.equals(input)) {
            return;
        }

        try {
            int bookingNumber = Integer.parseInt(input);
            if (bookingNumber < 1 || bookingNumber > currentBookings.size()) {
                printWarning(INVALID_NUMBER_MSG);
                pauseBeforeContinue();
                return;
            }

            BookingBean selected = currentBookings.get(bookingNumber - 1);

            if (selected.getStatus() != BookingStatus.PENDING) {
                printWarning(ONLY_PENDING_MSG);
                pauseBeforeContinue();
                return;
            }

            manageBooking(selected);

        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            pauseBeforeContinue();
        }
    }

    // ==================== SPECIFIC METHODS ====================

    /**
     * Shows management options for a pending booking.
     */
    private void manageBooking(BookingBean booking) {
        clearScreen();
        printTitle(TITLE_MANAGE);

        displayBookingDetails(booking);
        displayManageOptions();

        String action = readInput(ACTION_PROMPT).toUpperCase();

        switch (action) {
            case OPTION_APPROVE -> approveBooking(booking);
            case OPTION_REJECT -> rejectBooking(booking);
            case OPTION_BACK -> { /* Return to list */ }
            default -> {
                printWarning(INVALID_OPTION_MSG);
                pauseBeforeContinue();
            }
        }
    }

    /**
     * Displays detailed booking information (VenueManager version with fan info).
     */
    private void displayBookingDetails(BookingBean booking) {
        displayBookingDetailsBase(booking);
        print("  Fan:      " + booking.getFanUsername());
        printNewLine();
        printWarning("  Status:   " + booking.getStatus().name());
        printSeparator();
    }

    /**
     * Displays management action options.
     */
    private void displayManageOptions() {
        printNewLine();
        print("[A] Approve Booking");
        print("[R] Reject Booking");
        print("[B] Back to List");
        printNewLine();
    }

    /**
     * Handles booking approval.
     */
    private void approveBooking(BookingBean booking) {
        printNewLine();
        String confirm = readInput(APPROVE_CONFIRM_PROMPT);

        if (!confirm.equalsIgnoreCase("y")) {
            printInfo(APPROVE_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            viewBookingsController.approveBooking(booking.getId());
            printNewLine();
            printSuccess(APPROVE_SUCCESS_MSG);
            printInfo(FAN_NOTIFIED_MSG);
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error approving booking", e);
            printError(APPROVE_ERROR_MSG + ": " + e.getMessage());
        }

        pauseBeforeContinue();
    }

    /**
     * Handles booking rejection.
     */
    private void rejectBooking(BookingBean booking) {
        printNewLine();
        String confirm = readInput(REJECT_CONFIRM_PROMPT);

        if (!confirm.equalsIgnoreCase("y")) {
            printInfo(REJECT_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            viewBookingsController.rejectBooking(booking.getId());
            printNewLine();
            printSuccess(REJECT_SUCCESS_MSG);
            printInfo(FAN_NOTIFIED_MSG);
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error rejecting booking", e);
            printError(REJECT_ERROR_MSG + ": " + e.getMessage());
        }

        pauseBeforeContinue();
    }
}