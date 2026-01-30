package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.BookGameSeatController;
import it.uniroma2.hoophub.app_controller.FanBooking;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the Manage Seats use case (Fan side).
 *
 * <p>Extends {@link AbstractCliBookingListController} implementing the
 * Template Method pattern with Fan-specific actions (cancel).</p>
 *
 * <p>Depends on {@link FanBooking} interface (ISP compliance).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliManageSeatsGraphicController extends AbstractCliBookingListController {

    private static final Logger LOGGER = Logger.getLogger(CliManageSeatsGraphicController.class.getName());

    // Titles
    private static final String TITLE = "HOOPHUB - MY BOOKINGS";
    private static final String TITLE_CANCEL = "HOOPHUB - CANCEL BOOKING";
    private static final String BOOKINGS_HEADER = "=== MY BOOKINGS ===";

    // Messages
    private static final String NO_BOOKINGS_MSG = "You don't have any bookings yet.";
    private static final String UNREAD_NOTIFICATIONS_MSG = "You have %d new notification(s)!";
    private static final String NOT_CANCELLABLE_MSG = "This booking cannot be cancelled.";
    private static final String CANCEL_SUCCESS_MSG = "Booking cancelled successfully!";
    private static final String VENUE_NOTIFIED_MSG = "The venue manager has been notified.";
    private static final String CANCEL_ABORTED_MSG = "Cancellation aborted.";
    private static final String CANCEL_ERROR_MSG = "Failed to cancel booking";

    // Prompts
    private static final String BOOKING_SELECT_PROMPT = "Enter booking number to cancel (or B to go back): ";
    private static final String CANCEL_CONFIRM_PROMPT = "Are you sure you want to CANCEL this booking? (y/n): ";

    // ISP: dipende dall'interfaccia
    private final FanBooking fanBookingController;

    public CliManageSeatsGraphicController() {
        // L'implementazione concreta viene istanziata qui
        this.fanBookingController = new BookGameSeatController();
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
            return fanBookingController.getFanUnreadNotificationsCount();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
            return -1;
        }
    }

    @Override
    protected void markAllNotificationsAsRead() {
        try {
            fanBookingController.markFanNotificationsAsRead();
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error marking notifications as read", e);
        }
    }

    @Override
    protected void loadBookings(BookingStatus statusFilter) {
        try {
            currentBookings = fanBookingController.getMyBookings(statusFilter);
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.SEVERE, "Error loading bookings", e);
            printError(LOAD_ERROR_MSG + ": " + e.getMessage());
            currentBookings = new ArrayList<>();
        }
    }

    @Override
    protected void displayBookingItem(int number, BookingBean booking) {
        String statusBadge = getStatusBadge(booking.getStatus());
        String cancellable = isCancellable(booking) ? " (can cancel)" : "";

        printNewLine();
        print(String.format("  %2d. %s %s%s", number, formatMatchup(booking), statusBadge, cancellable));
        print(String.format("      %s", formatDateTime(booking)));
        print(String.format("      Venue: %s", booking.getVenueName()));
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

            if (!isCancellable(selected)) {
                printWarning(NOT_CANCELLABLE_MSG);
                pauseBeforeContinue();
                return;
            }

            confirmCancellation(selected);

        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            pauseBeforeContinue();
        }
    }

    // ==================== SPECIFIC METHODS ====================

    /**
     * Checks if booking can be canceled (for display purposes).
     */
    private boolean isCancellable(BookingBean booking) {
        try {
            return fanBookingController.canCancelBooking(booking.getId());
        } catch (DAOException | UserSessionException e) {
            return false;
        }
    }

    /**
     * Shows booking details and confirms cancellation.
     */
    private void confirmCancellation(BookingBean booking) {
        clearScreen();
        printTitle(TITLE_CANCEL);

        displayBookingDetails(booking);

        printNewLine();
        String confirm = readInput(CANCEL_CONFIRM_PROMPT);

        if (!confirm.equalsIgnoreCase("y")) {
            printInfo(CANCEL_ABORTED_MSG);
            pauseBeforeContinue();
            return;
        }

        cancelBooking(booking);
    }

    /**
     * Displays detailed booking information (Fan version with status).
     */
    private void displayBookingDetails(BookingBean booking) {
        displayBookingDetailsBase(booking);
        print("  Status:   " + booking.getStatus().name());
        printSeparator();
    }

    /**
     * Cancels the booking.
     */
    private void cancelBooking(BookingBean booking) {
        try {
            fanBookingController.cancelBooking(booking.getId());
            printNewLine();
            printSuccess(CANCEL_SUCCESS_MSG);
            printInfo(VENUE_NOTIFIED_MSG);
        } catch (DAOException | UserSessionException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error cancelling booking", e);
            printError(CANCEL_ERROR_MSG + ": " + e.getMessage());
        }

        pauseBeforeContinue();
    }
}