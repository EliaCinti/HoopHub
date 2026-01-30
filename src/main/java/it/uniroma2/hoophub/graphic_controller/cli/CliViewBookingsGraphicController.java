package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.ViewBookingsController;
import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the View Bookings use case (VenueManager side).
 *
 * <p>Provides text-based booking management: view, approve, and reject booking requests.
 * Uses the same {@link ViewBookingsController} as the GUI version.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliViewBookingsGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliViewBookingsGraphicController.class.getName());

    // Titles
    private static final String TITLE = "HOOPHUB - VIEW BOOKINGS";
    private static final String TITLE_MANAGE = "HOOPHUB - MANAGE BOOKING";

    // Menu Options
    private static final String OPTION_ALL = "1";
    private static final String OPTION_PENDING = "2";
    private static final String OPTION_CONFIRMED = "3";
    private static final String OPTION_REJECTED = "4";
    private static final String OPTION_CANCELLED = "5";
    private static final String OPTION_BACK = "B";
    private static final String OPTION_APPROVE = "A";
    private static final String OPTION_REJECT = "R";

    // Headers
    private static final String BOOKINGS_HEADER = "=== BOOKINGS ===";
    private static final String FILTER_HEADER = "=== FILTER BY STATUS ===";

    // Messages
    private static final String NO_BOOKINGS_MSG = "No bookings found.";
    private static final String UNREAD_NOTIFICATIONS_MSG = "You have %d new booking notification(s)!";
    private static final String INVALID_OPTION_MSG = "Invalid option. Please try again.";
    private static final String INVALID_NUMBER_MSG = "Invalid booking number. Please try again.";
    private static final String LOAD_ERROR_MSG = "Failed to load bookings";
    private static final String ONLY_PENDING_MSG = "Only PENDING bookings can be managed.";
    private static final String APPROVE_SUCCESS_MSG = "Booking APPROVED successfully!";
    private static final String REJECT_SUCCESS_MSG = "Booking REJECTED.";
    private static final String FAN_NOTIFIED_MSG = "The fan will be notified.";
    private static final String APPROVE_CANCELLED_MSG = "Approval cancelled.";
    private static final String REJECT_CANCELLED_MSG = "Rejection cancelled.";
    private static final String APPROVE_ERROR_MSG = "Failed to approve booking";
    private static final String REJECT_ERROR_MSG = "Failed to reject booking";

    // Prompts
    private static final String MENU_PROMPT = "Select option (1-5) or B to go back: ";
    private static final String BOOKING_SELECT_PROMPT = "Enter booking number to manage (or B to go back): ";
    private static final String ACTION_PROMPT = "Select action [A/R/B]: ";
    private static final String APPROVE_CONFIRM_PROMPT = "Are you sure you want to APPROVE this booking? (y/n): ";
    private static final String REJECT_CONFIRM_PROMPT = "Are you sure you want to REJECT this booking? (y/n): ";

    // Formatting
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Status badges
    private static final String BADGE_PENDING = "[PENDING]";
    private static final String BADGE_CONFIRMED = "[CONFIRMED]";
    private static final String BADGE_REJECTED = "[REJECTED]";
    private static final String BADGE_CANCELLED = "[CANCELLED]";

    private final ViewBookingsController viewBookingsController;
    private List<BookingBean> currentBookings;
    private boolean shouldExit = false;

    public CliViewBookingsGraphicController() {
        this.viewBookingsController = new ViewBookingsController();
        this.currentBookings = new ArrayList<>();
    }

    @Override
    public void execute() {
        shouldExit = false;

        // Check and display unread notifications
        checkNotifications();

        while (!shouldExit) {
            displayMainMenu();
            handleMenuInput();
        }
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Checks and displays unread notifications count.
     */
    private void checkNotifications() {
        try {
            int unreadCount = viewBookingsController.getUnreadNotificationsCount();
            if (unreadCount > 0) {
                clearScreen();
                printTitle(TITLE);
                printNewLine();
                printWarning(String.format(UNREAD_NOTIFICATIONS_MSG, unreadCount));
                viewBookingsController.markNotificationsAsRead();
                pauseBeforeContinue();
            }
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.WARNING, "Error checking notifications", e);
        }
    }

    // ==================== MAIN MENU ====================

    /**
     * Displays the main menu with filter options.
     */
    private void displayMainMenu() {
        clearScreen();
        printTitle(TITLE);

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
    private void handleMenuInput() {
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

    // ==================== DISPLAY BOOKINGS ====================

    /**
     * Shows bookings with optional status filter.
     */
    private void showBookings(BookingStatus statusFilter) {
        clearScreen();

        String titleSuffix = statusFilter != null ? " - " + statusFilter.name() : "";
        printTitle(TITLE + titleSuffix);

        loadBookings(statusFilter);

        if (currentBookings.isEmpty()) {
            printNewLine();
            printInfo(NO_BOOKINGS_MSG);
            pauseBeforeContinue();
            return;
        }

        displayBookingList();
        promptBookingSelection();
    }

    /**
     * Loads bookings from the controller.
     */
    private void loadBookings(BookingStatus statusFilter) {
        try {
            currentBookings = viewBookingsController.getBookingsForMyVenues(statusFilter);
        } catch (DAOException | UserSessionException e) {
            LOGGER.log(Level.SEVERE, "Error loading bookings", e);
            printError(LOAD_ERROR_MSG + ": " + e.getMessage());
            currentBookings = new ArrayList<>();
        }
    }

    /**
     * Displays the list of bookings.
     */
    private void displayBookingList() {
        printNewLine();
        print(BOOKINGS_HEADER);
        printSeparator();

        for (int i = 0; i < currentBookings.size(); i++) {
            displayBookingItem(i + 1, currentBookings.get(i));
        }

        printSeparator();
        printNewLine();
        printInfo("Showing " + currentBookings.size() + " booking(s)");
    }

    /**
     * Displays a single booking item.
     */
    private void displayBookingItem(int number, BookingBean booking) {
        String matchup = booking.getAwayTeam().getDisplayName() + " @ " +
                booking.getHomeTeam().getDisplayName();
        String dateTime = booking.getGameDate().format(DATE_FORMATTER) + " - " +
                booking.getGameTime().format(TIME_FORMATTER);
        String statusBadge = getStatusBadge(booking.getStatus());

        printNewLine();
        print(String.format("  %2d. %s %s", number, matchup, statusBadge));
        print(String.format("      %s", dateTime));
        print(String.format("      Venue: %s", booking.getVenueName()));
        print(String.format("      Fan: %s", booking.getFanUsername()));
    }

    /**
     * Gets the status badge text.
     */
    private String getStatusBadge(BookingStatus status) {
        return switch (status) {
            case PENDING -> BADGE_PENDING;
            case CONFIRMED -> BADGE_CONFIRMED;
            case REJECTED -> BADGE_REJECTED;
            case CANCELLED -> BADGE_CANCELLED;
        };
    }

    // ==================== BOOKING SELECTION ====================

    /**
     * Prompts user to select a booking to manage.
     */
    private void promptBookingSelection() {
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

    // ==================== MANAGE BOOKING ====================

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
     * Displays detailed booking information.
     */
    private void displayBookingDetails(BookingBean booking) {
        String matchup = booking.getAwayTeam().getDisplayName() + " @ " +
                booking.getHomeTeam().getDisplayName();

        printSeparator();
        print("BOOKING DETAILS:");
        printNewLine();
        print("  Game:     " + matchup);
        print("  Date:     " + booking.getGameDate().format(DATE_FORMATTER));
        print("  Time:     " + booking.getGameTime().format(TIME_FORMATTER));
        printNewLine();
        print("  Venue:    " + booking.getVenueName());
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

    // ==================== APPROVE/REJECT ====================

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

    // ==================== UTILITY METHODS ====================

    /**
     * Pauses until user presses Enter.
     */
    private void pauseBeforeContinue() {
        printNewLine();
        readInput("Press Enter to continue...");
    }
}
