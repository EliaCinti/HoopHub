package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for CLI controllers that display booking lists.
 *
 * <p>Implements the <b>Template Method pattern (GoF)</b>: defines the skeleton
 * of the booking list flow (notifications, menu, filtering) while letting subclasses
 * define specific display and action handlers via abstract methods.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CliViewBookingsGraphicController
 * @see CliManageSeatsGraphicController
 */
public abstract class AbstractCliBookingListController extends CliGraphicController {

    // ==================== SHARED CONSTANTS ====================

    // Menu Options
    protected static final String OPTION_ALL = "1";
    protected static final String OPTION_PENDING = "2";
    protected static final String OPTION_CONFIRMED = "3";
    protected static final String OPTION_REJECTED = "4";
    protected static final String OPTION_CANCELLED = "5";
    protected static final String OPTION_BACK = "B";

    // Headers
    protected static final String FILTER_HEADER = "=== FILTER BY STATUS ===";

    // Messages
    protected static final String INVALID_OPTION_MSG = "Invalid option. Please try again.";
    protected static final String INVALID_NUMBER_MSG = "Invalid booking number. Please try again.";
    protected static final String LOAD_ERROR_MSG = "Failed to load bookings";

    // Prompts
    protected static final String MENU_PROMPT = "Select option (1-5) or B to go back: ";

    // Formatting
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Status badges
    protected static final String BADGE_PENDING = "[PENDING]";
    protected static final String BADGE_CONFIRMED = "[CONFIRMED]";
    protected static final String BADGE_REJECTED = "[REJECTED]";
    protected static final String BADGE_CANCELLED = "[CANCELLED]";

    // ==================== STATE ====================

    protected List<BookingBean> currentBookings = new ArrayList<>();
    protected boolean shouldExit = false;

    // ==================== TEMPLATE METHOD ====================

    /**
     * Template method: executes the booking list flow.
     * Subclasses should NOT override this method.
     */
    @Override
    public void execute() {
        shouldExit = false;
        checkNotifications();

        while (!shouldExit) {
            displayMainMenu();
            handleMenuInput();
        }
    }

    // ==================== COMMON IMPLEMENTATIONS ====================

    /**
     * Checks and displays unread notifications count.
     */
    protected void checkNotifications() {
        int unreadCount = fetchUnreadNotificationsCount();
        if (unreadCount > 0) {
            clearScreen();
            printTitle(getTitle());
            printNewLine();
            printWarning(String.format(getUnreadNotificationsMessage(), unreadCount));
            markAllNotificationsAsRead();
            pauseBeforeContinue();
        }
    }

    /**
     * Displays the main menu with filter options.
     */
    protected void displayMainMenu() {
        clearScreen();
        printTitle(getTitle());

        printNewLine();
        print(FILTER_HEADER);
        printSeparator();
        print("  1. View All Bookings");
        print("  2. View PENDING Bookings");
        print("  3. View CONFIRMED Bookings");
        print("  4. View REJECTED Bookings");
        print("  5. View CANCELLED Bookings");
        print("  B. Back to Homepage");
        printSeparator();
        printNewLine();
    }

    /**
     * Handles main menu input.
     */
    protected void handleMenuInput() {
        String input = readInput(MENU_PROMPT).toUpperCase();

        switch (input) {
            case OPTION_ALL -> showBookings(null);
            case OPTION_PENDING -> showBookings(BookingStatus.PENDING);
            case OPTION_CONFIRMED -> showBookings(BookingStatus.CONFIRMED);
            case OPTION_REJECTED -> showBookings(BookingStatus.REJECTED);
            case OPTION_CANCELLED -> showBookings(BookingStatus.CANCELLED);
            case OPTION_BACK -> shouldExit = true;
            default -> {
                printWarning(INVALID_OPTION_MSG);
                pauseBeforeContinue();
            }
        }
    }

    /**
     * Shows bookings with optional status filter.
     */
    protected void showBookings(BookingStatus statusFilter) {
        clearScreen();

        String titleSuffix = statusFilter != null ? " - " + statusFilter.name() : "";
        printTitle(getTitle() + titleSuffix);

        loadBookings(statusFilter);

        if (currentBookings.isEmpty()) {
            printNewLine();
            printInfo(getEmptyMessage());
            pauseBeforeContinue();
            return;
        }

        displayBookingList();
        promptBookingSelection();
    }

    /**
     * Displays the list of bookings.
     */
    protected void displayBookingList() {
        printNewLine();
        print(getBookingsHeader());
        printSeparator();

        for (int i = 0; i < currentBookings.size(); i++) {
            displayBookingItem(i + 1, currentBookings.get(i));
        }

        printSeparator();
        printNewLine();
        printInfo("Showing " + currentBookings.size() + " booking(s)");
    }

    /**
     * Gets the status badge text.
     */
    protected String getStatusBadge(BookingStatus status) {
        return switch (status) {
            case PENDING -> BADGE_PENDING;
            case CONFIRMED -> BADGE_CONFIRMED;
            case REJECTED -> BADGE_REJECTED;
            case CANCELLED -> BADGE_CANCELLED;
        };
    }

    /**
     * Formats booking matchup for display.
     */
    protected String formatMatchup(BookingBean booking) {
        return booking.getAwayTeam().getDisplayName() + " @ " +
                booking.getHomeTeam().getDisplayName();
    }

    /**
     * Formats date and time for display.
     */
    protected String formatDateTime(BookingBean booking) {
        return booking.getGameDate().format(DATE_FORMATTER) + " - " +
                booking.getGameTime().format(TIME_FORMATTER);
    }

    /**
     * Displays detailed booking information (common structure).
     */
    protected void displayBookingDetailsBase(BookingBean booking) {
        printSeparator();
        print("BOOKING DETAILS:");
        printNewLine();
        print("  Game:     " + formatMatchup(booking));
        print("  Date:     " + booking.getGameDate().format(DATE_FORMATTER));
        print("  Time:     " + booking.getGameTime().format(TIME_FORMATTER));
        printNewLine();
        print("  Venue:    " + booking.getVenueName());
    }

    /**
     * Pauses until user presses Enter.
     */
    protected void pauseBeforeContinue() {
        printNewLine();
        readInput("Press Enter to continue...");
    }

    // ==================== ABSTRACT METHODS ====================

    /**
     * Returns the page title.
     */
    protected abstract String getTitle();

    /**
     * Returns the bookings list header.
     */
    protected abstract String getBookingsHeader();

    /**
     * Returns the message when no bookings found.
     */
    protected abstract String getEmptyMessage();

    /**
     * Returns the unread notifications message format.
     */
    protected abstract String getUnreadNotificationsMessage();

    /**
     * Gets the unread notifications count.
     * @return number of unread notifications, or -1 if error
     */
    protected abstract int fetchUnreadNotificationsCount();

    /**
     * Marks all notifications as read.
     */
    protected abstract void markAllNotificationsAsRead();

    /**
     * Loads bookings with optional status filter.
     */
    protected abstract void loadBookings(BookingStatus statusFilter);

    /**
     * Displays a single booking item.
     */
    protected abstract void displayBookingItem(int number, BookingBean booking);

    /**
     * Prompts user to select a booking for action.
     */
    protected abstract void promptBookingSelection();
}
