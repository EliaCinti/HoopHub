package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.BookGameSeatController;
import it.uniroma2.hoophub.app_controller.FanBooking;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.VenueType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI graphic controller for the Book Game Seat use case.
 *
 * <p>Provides text-based booking flow for fans: select game → select venue → confirm booking.
 * Depends on {@link FanBooking} interface (ISP compliance).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliBookGameSeatGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliBookGameSeatGraphicController.class.getName());

    // Titles
    private static final String TITLE_SELECT_GAME = "HOOPHUB - SELECT GAME";
    private static final String TITLE_SELECT_VENUE = "HOOPHUB - SELECT VENUE";
    private static final String TITLE_BOOKING_SUMMARY = "HOOPHUB - BOOKING SUMMARY";

    // Messages
    private static final String NO_GAMES_MSG = "No upcoming games available in the next 2 weeks.";
    private static final String NO_VENUES_MSG = "No venues available for this game.";
    private static final String GAMES_HEADER = "=== UPCOMING GAMES ===";
    private static final String VENUES_HEADER = "=== AVAILABLE VENUES ===";
    private static final String INVALID_OPTION_MSG = "Invalid option. Please try again.";
    private static final String INVALID_NUMBER_MSG = "Invalid number. Please try again.";
    private static final String BOOKING_SUCCESS_MSG = "Booking request sent successfully!";
    private static final String BOOKING_PENDING_MSG = "Your booking is now PENDING. The venue manager will review it.";
    private static final String BOOKING_ERROR_MSG = "Failed to create booking";
    private static final String NO_SEATS_MSG = "No seats available at this venue.";
    private static final String WAITLIST_MSG = "Waitlist feature is not yet available.";
    private static final String REDIRECT_MSG = "Redirecting to homepage in %d seconds...";
    private static final String OPERATION_CANCELLED_MSG = "Operation cancelled.";

    // Prompts
    private static final String GAME_SELECT_PROMPT = "Select game number (or B to go back): ";
    private static final String VENUE_SELECT_PROMPT = "Select venue number (or B to go back): ";
    private static final String FILTER_PROMPT = "Apply filters? (y/n): ";
    private static final String CITY_FILTER_PROMPT = "Enter city (or press Enter to skip): ";
    private static final String TYPE_FILTER_PROMPT = "Select venue type (1-%d, or press Enter to skip): ";
    private static final String SEATS_FILTER_PROMPT = "Show only venues with available seats? (y/n): ";
    private static final String CONFIRM_BOOKING_PROMPT = "Confirm booking? (y/n): ";
    private static final String WAITLIST_PROMPT = "Join waitlist? (y/n): ";

    // Formatting
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Redirect delay
    private static final int REDIRECT_DELAY_SECONDS = 5;

    // ISP: dipende dall'interfaccia
    private final FanBooking fanBookingController;
    private List<NbaGameBean> currentGames;
    private List<VenueBean> currentVenues;
    private NbaGameBean selectedGame;

    public CliBookGameSeatGraphicController() {
        // L'implementazione concreta viene istanziata qui
        this.fanBookingController = new BookGameSeatController();
        this.currentGames = new ArrayList<>();
        this.currentVenues = new ArrayList<>();
    }

    @Override
    public void execute() {
        // Step 1: Select Game
        selectedGame = selectGame();
        if (selectedGame == null) {
            return; // User cancelled
        }

        // Step 2: Select Venue
        VenueBean selectedVenue = selectVenue();
        if (selectedVenue == null) {
            return; // User cancelled
        }

        // Step 3: Confirm and Book
        confirmAndBook(selectedVenue);
    }

    // ==================== STEP 1: SELECT GAME ====================

    /**
     * Displays games and handles selection.
     *
     * @return Selected game or null if canceled
     */
    private NbaGameBean selectGame() {
        clearScreen();
        printTitle(TITLE_SELECT_GAME);

        currentGames = fanBookingController.getUpcomingGames();

        if (currentGames.isEmpty()) {
            printWarning(NO_GAMES_MSG);
            pauseBeforeContinue();
            return null;
        }

        displayGameList();

        return promptGameSelection();
    }

    /**
     * Displays the list of upcoming games.
     */
    private void displayGameList() {
        printNewLine();
        print(GAMES_HEADER);
        printSeparator();

        for (int i = 0; i < currentGames.size(); i++) {
            displayGameItem(i + 1, currentGames.get(i));
        }

        printSeparator();
    }

    /**
     * Displays a single game item.
     */
    private void displayGameItem(int number, NbaGameBean game) {
        String dateStr = game.getDate().format(DATE_FORMATTER);
        String timeStr = game.getTime().format(TIME_FORMATTER);
        String matchup = game.getAwayTeam().getDisplayName() + " @ " + game.getHomeTeam().getDisplayName();

        printNewLine();
        print(String.format("  %2d. %s", number, matchup));
        print(String.format("      %s - %s", dateStr, timeStr));
    }

    /**
     * Prompts user to select a game.
     */
    private NbaGameBean promptGameSelection() {
        printNewLine();
        String input = readInput(GAME_SELECT_PROMPT);

        if (input.equalsIgnoreCase("B")) {
            return null;
        }

        try {
            int gameNumber = Integer.parseInt(input);
            if (gameNumber < 1 || gameNumber > currentGames.size()) {
                printWarning(INVALID_NUMBER_MSG);
                pauseBeforeContinue();
                return selectGame(); // Retry
            }
            return currentGames.get(gameNumber - 1);
        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            pauseBeforeContinue();
            return selectGame(); // Retry
        }
    }

    // ==================== STEP 2: SELECT VENUE ====================

    /**
     * Displays venues and handles selection.
     *
     * @return Selected venue or null if cancelled
     */
    private VenueBean selectVenue() {
        clearScreen();
        printTitle(TITLE_SELECT_VENUE);

        // Show selected game info
        printInfo("Selected game: " + formatGameInfo(selectedGame));
        printNewLine();

        // Ask for filters
        VenueFilters filters = askForFilters();

        // Load venues
        loadVenuesWithFilters(filters);

        if (currentVenues.isEmpty()) {
            printWarning(NO_VENUES_MSG);
            printNewLine();
            String retry = readInput("Try again without filters? (y/n): ");
            if (retry.equalsIgnoreCase("y")) {
                return selectVenue();
            }
            return null;
        }

        displayVenueList();

        return promptVenueSelection();
    }

    /**
     * Asks user for venue filters.
     */
    private VenueFilters askForFilters() {
        VenueFilters filters = new VenueFilters();

        String applyFilters = readInput(FILTER_PROMPT);
        if (!applyFilters.equalsIgnoreCase("y")) {
            return filters;
        }

        printNewLine();

        // City filter
        filters.city = readInput(CITY_FILTER_PROMPT);

        // Type filter
        printNewLine();
        print("Venue Types:");
        VenueType[] types = VenueType.values();
        for (int i = 0; i < types.length; i++) {
            print(String.format("  %d. %s", i + 1, types[i].getDisplayName()));
        }
        printNewLine();
        String typeInput = readInput(String.format(TYPE_FILTER_PROMPT, types.length));
        if (!typeInput.isEmpty()) {
            try {
                int typeIndex = Integer.parseInt(typeInput) - 1;
                if (typeIndex >= 0 && typeIndex < types.length) {
                    filters.type = types[typeIndex].name();
                }
            } catch (NumberFormatException e) {
                // Skip filter
            }
        }

        // Seats filter
        printNewLine();
        String seatsFilter = readInput(SEATS_FILTER_PROMPT);
        filters.onlyWithSeats = seatsFilter.equalsIgnoreCase("y");

        return filters;
    }

    /**
     * Loads venues with applied filters.
     */
    private void loadVenuesWithFilters(VenueFilters filters) {
        try {
            currentVenues = fanBookingController.getVenuesForGame(
                    selectedGame,
                    filters.city,
                    filters.type,
                    filters.onlyWithSeats
            );
        } catch (DAOException e) {
            LOGGER.log(Level.SEVERE, "Error loading venues", e);
            printError("Failed to load venues: " + e.getMessage());
            currentVenues = new ArrayList<>();
        }
    }

    /**
     * Displays the list of venues.
     */
    private void displayVenueList() {
        printNewLine();
        print(VENUES_HEADER);
        printSeparator();

        for (int i = 0; i < currentVenues.size(); i++) {
            displayVenueItem(i + 1, currentVenues.get(i));
        }

        printSeparator();
    }

    /**
     * Displays a single venue item with availability.
     */
    private void displayVenueItem(int number, VenueBean venue) {
        int availableSeats = fanBookingController.getAvailableSeats(venue.getId(), selectedGame);
        String availabilityText = availableSeats > 0
                ? availableSeats + " seats available"
                : "FULL - Waitlist only";

        printNewLine();
        print(String.format("  %2d. %s", number, venue.getName()));
        print(String.format("      %s • %s", venue.getType().getDisplayName(), venue.getCity()));
        print(String.format("      %s • %s", venue.getAddress(), availabilityText));
    }

    /**
     * Prompts user to select a venue.
     */
    private VenueBean promptVenueSelection() {
        printNewLine();
        String input = readInput(VENUE_SELECT_PROMPT);

        if (input.equalsIgnoreCase("B")) {
            return selectGame() != null ? selectVenue() : null; // Go back to game selection
        }

        try {
            int venueNumber = Integer.parseInt(input);
            if (venueNumber < 1 || venueNumber > currentVenues.size()) {
                printWarning(INVALID_NUMBER_MSG);
                pauseBeforeContinue();
                return selectVenue(); // Retry
            }
            return currentVenues.get(venueNumber - 1);
        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            pauseBeforeContinue();
            return selectVenue(); // Retry
        }
    }

    // ==================== STEP 3: CONFIRM AND BOOK ====================

    /**
     * Shows booking summary and handles confirmation.
     */
    private void confirmAndBook(VenueBean venue) {
        clearScreen();
        printTitle(TITLE_BOOKING_SUMMARY);

        int availableSeats = fanBookingController.getAvailableSeats(venue.getId(), selectedGame);

        displayBookingSummary(venue, availableSeats);

        if (availableSeats > 0) {
            handleBookingConfirmation(venue);
        } else {
            handleWaitlistOption();
        }
    }

    /**
     * Displays the booking summary.
     */
    private void displayBookingSummary(VenueBean venue, int availableSeats) {
        printSeparator();
        print("BOOKING DETAILS:");
        printNewLine();
        print("  Game:     " + selectedGame.getAwayTeam().getDisplayName() +
                " @ " + selectedGame.getHomeTeam().getDisplayName());
        print("  Date:     " + selectedGame.getDate().format(DATE_FORMATTER));
        print("  Time:     " + selectedGame.getTime().format(TIME_FORMATTER));
        printNewLine();
        print("  Venue:    " + venue.getName());
        print("  Type:     " + venue.getType().getDisplayName());
        print("  Address:  " + venue.getAddress() + ", " + venue.getCity());
        printNewLine();

        if (availableSeats > 0) {
            printSuccess("  Status:   " + availableSeats + " seats available");
        } else {
            printWarning("  Status:   FULL - No seats available");
        }

        printSeparator();
    }

    /**
     * Handles booking confirmation when seats are available.
     */
    private void handleBookingConfirmation(VenueBean venue) {
        printNewLine();
        String confirm = readInput(CONFIRM_BOOKING_PROMPT);

        if (!confirm.equalsIgnoreCase("y")) {
            printInfo(OPERATION_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            // Controlla se ha già prenotato questa partita
            if (fanBookingController.hasAlreadyBooked(selectedGame)) {
                printError("You have already booked this game!");
                pauseBeforeContinue();
                return;
            }

            fanBookingController.createBookingRequest(selectedGame, venue.getId());

            printNewLine();
            printSuccess(BOOKING_SUCCESS_MSG);
            printInfo(BOOKING_PENDING_MSG);

            redirectToHomepage();
        } catch (UserSessionException | DAOException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error creating booking", e);
            printError(BOOKING_ERROR_MSG + ": " + e.getMessage());
            pauseBeforeContinue();
        }
    }

    /**
     * Handles waitlist option when no seats available.
     */
    private void handleWaitlistOption() {
        printNewLine();
        printWarning(NO_SEATS_MSG);
        printNewLine();

        String joinWaitlist = readInput(WAITLIST_PROMPT);

        if (joinWaitlist.equalsIgnoreCase("y")) {
            printNewLine();
            printInfo(WAITLIST_MSG);
            redirectToHomepage();
        } else {
            printInfo(OPERATION_CANCELLED_MSG);
            pauseBeforeContinue();
        }
    }

    /**
     * Redirects to homepage after delay.
     */
    private void redirectToHomepage() {
        printNewLine();
        printInfo(String.format(REDIRECT_MSG, REDIRECT_DELAY_SECONDS));

        try {
            for (int i = REDIRECT_DELAY_SECONDS; i > 0; i--) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simply return - the execute() method will end and return to homepage
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Formats game info for display.
     */
    private String formatGameInfo(NbaGameBean game) {
        return String.format("%s @ %s - %s %s",
                game.getAwayTeam().getDisplayName(),
                game.getHomeTeam().getDisplayName(),
                game.getDate().format(DATE_FORMATTER),
                game.getTime().format(TIME_FORMATTER));
    }

    /**
     * Pauses until user presses Enter.
     */
    private void pauseBeforeContinue() {
        printNewLine();
        readInput("Press Enter to continue...");
    }

    /**
     * Simple holder for venue filters.
     */
    private static class VenueFilters {
        String city = null;
        String type = null;
        boolean onlyWithSeats = false;
    }
}