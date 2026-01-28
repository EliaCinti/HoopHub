package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.app_controller.ManageVenuesController;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.VenueType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CLI graphic controller for the Manage Venues use case.
 *
 * <p>Provides text-based CRUD operations for venues using the same
 * {@link ManageVenuesController} as the GUI version, demonstrating
 * that different graphic controllers can share the same application logic.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see it.uniroma2.hoophub.graphic_controller.gui.ManageVenuesGraphicController
 */
public class CliManageVenuesGraphicController extends CliGraphicController {

    private static final Logger LOGGER = Logger.getLogger(CliManageVenuesGraphicController.class.getName());

    // Page Title
    private static final String TITLE = "HOOPHUB - MANAGE VENUES";

    // Menu Options
    private static final String OPTION_ADD = "A";
    private static final String OPTION_EDIT = "E";
    private static final String OPTION_DELETE = "D";
    private static final String OPTION_BACK = "B";

    // Messages
    private static final String NO_VENUES_MSG = "You haven't added any venues yet.";
    private static final String VENUES_HEADER = "=== YOUR VENUES ===";
    private static final String MENU_PROMPT = "Select option [A/E/D/B]: ";
    private static final String VENUE_NUMBER_PROMPT = "Enter venue number: ";
    private static final String INVALID_OPTION_MSG = "Invalid option. Please try again.";
    private static final String INVALID_NUMBER_MSG = "Invalid venue number. Please try again.";
    private static final String LOAD_ERROR_MSG = "Failed to load venues";
    private static final String DELETE_CONFIRM_MSG = "Are you sure you want to delete '%s'? (y/n): ";
    private static final String DELETE_SUCCESS_MSG = "Venue deleted successfully!";
    private static final String DELETE_CANCELLED_MSG = "Delete cancelled.";
    private static final String DELETE_ERROR_MSG = "Failed to delete venue";
    private static final String CREATE_SUCCESS_MSG = "Venue created successfully!";
    private static final String UPDATE_SUCCESS_MSG = "Venue updated successfully!";
    private static final String OPERATION_CANCELLED_MSG = "Operation cancelled.";

    // Form Labels
    private static final String FORM_NAME_PROMPT = "Enter venue name: ";
    private static final String FORM_TYPE_PROMPT = "Select venue type (1-%d): ";
    private static final String FORM_ADDRESS_PROMPT = "Enter address: ";
    private static final String FORM_CITY_PROMPT = "Enter city: ";
    private static final String FORM_CAPACITY_PROMPT = "Enter capacity (max %d): ";
    private static final String FORM_TEAMS_PROMPT = "Enter team numbers (comma-separated, e.g. 1,3,5): ";
    private static final String FORM_CONFIRM_PROMPT = "Confirm? (y/n): ";

    private final ManageVenuesController manageVenuesController;
    private List<VenueBean> currentVenues;
    private boolean shouldExit = false;

    public CliManageVenuesGraphicController() {
        this.manageVenuesController = new ManageVenuesController();
        this.currentVenues = new ArrayList<>();
    }

    @Override
    public void execute() {
        shouldExit = false;

        while (!shouldExit) {
            displayVenueList();
            handleMenuInput();
        }
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * Displays the list of venues and menu options.
     */
    private void displayVenueList() {
        clearScreen();
        printTitle(TITLE);

        loadVenues();

        printNewLine();
        print(VENUES_HEADER);
        printSeparator();

        if (currentVenues.isEmpty()) {
            printInfo(NO_VENUES_MSG);
        } else {
            for (int i = 0; i < currentVenues.size(); i++) {
                displayVenueItem(i + 1, currentVenues.get(i));
            }
        }

        printSeparator();
        printNewLine();
        displayMenuOptions();
        printNewLine();
    }

    /**
     * Displays a single venue item.
     */
    private void displayVenueItem(int number, VenueBean venue) {
        printNewLine();
        print(String.format("  %d. %s", number, venue.getName()));
        print(String.format("     %s • %s • Capacity: %d • %s",
                venue.getType().getDisplayName(),
                venue.getCity(),
                venue.getMaxCapacity(),
                VenueBean.formatTeamsForDisplay(venue.getAssociatedTeams())));
    }

    /**
     * Displays the menu options.
     */
    private void displayMenuOptions() {
        print("[A] Add New Venue");
        print("[E] Edit Venue");
        print("[D] Delete Venue");
        print("[B] Back to Homepage");
    }

    // ==================== DATA LOADING ====================

    /**
     * Loads venues from the application controller.
     */
    private void loadVenues() {
        try {
            currentVenues = manageVenuesController.getMyVenues();
        } catch (UserSessionException | DAOException e) {
            LOGGER.log(Level.SEVERE, "Error loading venues", e);
            printError(LOAD_ERROR_MSG + ": " + e.getMessage());
            currentVenues = new ArrayList<>();
        }
    }

    // ==================== INPUT HANDLING ====================

    /**
     * Handles the main menu input.
     */
    private void handleMenuInput() {
        String input = readInput(MENU_PROMPT).toUpperCase();

        switch (input) {
            case OPTION_ADD -> onAddVenueSelected();
            case OPTION_EDIT -> onEditVenueSelected();
            case OPTION_DELETE -> onDeleteVenueSelected();
            case OPTION_BACK -> shouldExit = true;
            default -> {
                printWarning(INVALID_OPTION_MSG);
                pauseBeforeContinue();
            }
        }
    }

    // ==================== ADD VENUE ====================

    /**
     * Handles the Add Venue flow.
     */
    private void onAddVenueSelected() {
        clearScreen();
        printTitle("ADD NEW VENUE");

        VenueBean newVenue = collectVenueData(null);
        if (newVenue == null) {
            printInfo(OPERATION_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            manageVenuesController.createVenue(newVenue);
            printNewLine();
            printSuccess(CREATE_SUCCESS_MSG);
        } catch (UserSessionException | DAOException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error creating venue", e);
            printError(e.getMessage());
        }

        pauseBeforeContinue();
    }

    // ==================== EDIT VENUE ====================

    /**
     * Handles the Edit Venue flow.
     */
    private void onEditVenueSelected() {
        if (currentVenues.isEmpty()) {
            printWarning(NO_VENUES_MSG);
            pauseBeforeContinue();
            return;
        }

        VenueBean selectedVenue = selectVenue();
        if (selectedVenue == null) {
            return;
        }

        clearScreen();
        printTitle("EDIT VENUE: " + selectedVenue.getName());

        VenueBean updatedVenue = collectVenueData(selectedVenue);
        if (updatedVenue == null) {
            printInfo(OPERATION_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            updatedVenue.setId(selectedVenue.getId());
            manageVenuesController.updateVenue(updatedVenue);
            printNewLine();
            printSuccess(UPDATE_SUCCESS_MSG);
        } catch (UserSessionException | DAOException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error updating venue", e);
            printError(e.getMessage());
        }

        pauseBeforeContinue();
    }

    // ==================== DELETE VENUE ====================

    /**
     * Handles the Delete Venue flow.
     */
    private void onDeleteVenueSelected() {
        if (currentVenues.isEmpty()) {
            printWarning(NO_VENUES_MSG);
            pauseBeforeContinue();
            return;
        }

        VenueBean selectedVenue = selectVenue();
        if (selectedVenue == null) {
            return;
        }

        printNewLine();
        String confirm = readInput(String.format(DELETE_CONFIRM_MSG, selectedVenue.getName()));

        if (!confirm.equalsIgnoreCase("y")) {
            printInfo(DELETE_CANCELLED_MSG);
            pauseBeforeContinue();
            return;
        }

        try {
            manageVenuesController.deleteVenue(selectedVenue.getId());
            printNewLine();
            printSuccess(DELETE_SUCCESS_MSG);
        } catch (UserSessionException | DAOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting venue", e);
            printError(DELETE_ERROR_MSG + ": " + e.getMessage());
        }

        pauseBeforeContinue();
    }

    // ==================== VENUE SELECTION ====================

    /**
     * Prompts user to select a venue by number.
     *
     * @return selected VenueBean or null if canceled
     */
    private VenueBean selectVenue() {
        printNewLine();
        String input = readInput(VENUE_NUMBER_PROMPT);

        if (input.equalsIgnoreCase(OPTION_BACK)) {
            return null;
        }

        try {
            int venueNumber = Integer.parseInt(input);
            if (venueNumber < 1 || venueNumber > currentVenues.size()) {
                printWarning(INVALID_NUMBER_MSG);
                pauseBeforeContinue();
                return null;
            }
            return currentVenues.get(venueNumber - 1);
        } catch (NumberFormatException e) {
            printWarning(INVALID_NUMBER_MSG);
            pauseBeforeContinue();
            return null;
        }
    }

    // ==================== FORM DATA COLLECTION ====================

    /**
     * Collects venue data from user input.
     *
     * @param existingVenue existing venue for edit mode (null for add mode)
     * @return VenueBean with collected data, or null if canceled
     */
    private VenueBean collectVenueData(VenueBean existingVenue) {
        boolean isEditMode = existingVenue != null;

        // Name
        String name = collectStringField(FORM_NAME_PROMPT,
                isEditMode ? existingVenue.getName() : null,
                VenueBean::validateNameSyntax);
        if (name == null) return null;

        // Type
        VenueType type = collectVenueType(isEditMode ? existingVenue.getType() : null);
        if (type == null) return null;

        // Address
        String address = collectStringField(FORM_ADDRESS_PROMPT,
                isEditMode ? existingVenue.getAddress() : null,
                VenueBean::validateAddressSyntax);
        if (address == null) return null;

        // City
        String city = collectStringField(FORM_CITY_PROMPT,
                isEditMode ? existingVenue.getCity() : null,
                VenueBean::validateCitySyntax);
        if (city == null) return null;

        // Capacity
        Integer capacity = collectCapacity(type, isEditMode ? existingVenue.getMaxCapacity() : null);
        if (capacity == null) return null;

        // Teams
        Set<TeamNBA> teams = collectTeams(type, isEditMode ? existingVenue.getAssociatedTeams() : null);
        if (teams == null) return null;

        // Confirmation
        printNewLine();
        displayVenueSummary(name, type, address, city, capacity, teams);
        String confirm = readInput(FORM_CONFIRM_PROMPT);

        if (!confirm.equalsIgnoreCase("y")) {
            return null;
        }

        return new VenueBean.Builder()
                .name(name)
                .type(type)
                .address(address)
                .city(city)
                .maxCapacity(capacity)
                .associatedTeams(teams)
                .build();
    }

    // ==================== GENERIC STRING FIELD COLLECTION ====================

    /**
     * Collects a validated string field with optional default value.
     *
     * <p>This method eliminates duplication across name, address, and city collection
     * by accepting a validation function as parameter.</p>
     *
     * @param prompt       The prompt to display
     * @param defaultValue The current value (for edit mode), or null
     * @param validator    Validation method reference (e.g., VenueBean::validateNameSyntax)
     * @return The validated input, defaultValue if input empty, or null if canceled
     */
    private String collectStringField(String prompt, String defaultValue, StringValidator validator) {
        printNewLine();
        if (defaultValue != null) {
            printInfo("Current: " + defaultValue);
        }

        String input = readInput(prompt);

        if (input.isEmpty() && defaultValue != null) {
            return defaultValue;
        }
        if (input.isEmpty() || input.equalsIgnoreCase(OPTION_BACK)) {
            return null;
        }

        try {
            validator.validate(input);
            return input;
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
            return collectStringField(prompt, defaultValue, validator);
        }
    }

    /**
     * Functional interface for string validation.
     * Accepts a string and throws IllegalArgumentException if invalid.
     */
    @FunctionalInterface
    private interface StringValidator {
        void validate(String value) throws IllegalArgumentException;
    }

    // ==================== SPECIFIC FIELD COLLECTION ====================

    /**
     * Collects venue type.
     */
    private VenueType collectVenueType(VenueType defaultValue) {
        printNewLine();
        print("Venue Types:");
        VenueType[] types = VenueType.values();
        for (int i = 0; i < types.length; i++) {
            String marker = (defaultValue == types[i]) ? " (current)" : "";
            print(String.format("  %d. %s (max capacity: %d)%s",
                    i + 1, types[i].getDisplayName(), types[i].getMaxCapacityLimit(), marker));
        }

        printNewLine();
        String input = readInput(String.format(FORM_TYPE_PROMPT, types.length));

        if (input.isEmpty() && defaultValue != null) {
            return defaultValue;
        }
        if (input.equalsIgnoreCase(OPTION_BACK)) {
            return null;
        }

        try {
            int typeIndex = Integer.parseInt(input) - 1;
            if (typeIndex < 0 || typeIndex >= types.length) {
                printWarning(INVALID_OPTION_MSG);
                return collectVenueType(defaultValue);
            }
            return types[typeIndex];
        } catch (NumberFormatException e) {
            printWarning(INVALID_OPTION_MSG);
            return collectVenueType(defaultValue);
        }
    }

    /**
     * Collects venue capacity.
     */
    private Integer collectCapacity(VenueType type, Integer defaultValue) {
        printNewLine();
        if (defaultValue != null) {
            printInfo("Current: " + defaultValue);
        }
        String input = readInput(String.format(FORM_CAPACITY_PROMPT, type.getMaxCapacityLimit()));

        if (input.isEmpty() && defaultValue != null) {
            return defaultValue;
        }
        if (input.equalsIgnoreCase(OPTION_BACK)) {
            return null;
        }

        try {
            int capacity = Integer.parseInt(input);
            VenueBean.validateCapacity(capacity, type);
            return capacity;
        } catch (NumberFormatException e) {
            printWarning("Please enter a valid number.");
            return collectCapacity(type, defaultValue);
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
            return collectCapacity(type, defaultValue);
        }
    }

    /**
     * Collects associated teams.
     */
    private Set<TeamNBA> collectTeams(VenueType type, Set<TeamNBA> defaultValue) {
        printNewLine();
        print("Available Teams:");
        TeamNBA[] allTeams = TeamNBA.values();

        for (int i = 0; i < allTeams.length; i++) {
            String marker = (defaultValue != null && defaultValue.contains(allTeams[i])) ? " ✓" : "";
            print(String.format("  %2d. %s%s", i + 1, allTeams[i].getDisplayName(), marker));
        }

        printNewLine();
        if (type == VenueType.FAN_CLUB) {
            printWarning("Fan Club venues can only have ONE team.");
        }
        printInfo("Enter numbers separated by commas (e.g., 1,5,10)");
        if (defaultValue != null && !defaultValue.isEmpty()) {
            printInfo("Press Enter to keep current selection");
        }

        String input = readInput(FORM_TEAMS_PROMPT);

        if (input.isEmpty() && defaultValue != null && !defaultValue.isEmpty()) {
            return defaultValue;
        }
        if (input.equalsIgnoreCase(OPTION_BACK)) {
            return Collections.emptySet();
        }

        Set<TeamNBA> selectedTeams = parseTeamSelection(input, allTeams);

        try {
            VenueBean.validateTeams(selectedTeams, type);
            return selectedTeams;
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
            return collectTeams(type, defaultValue);
        }
    }

    /**
     * Parses team selection from comma-separated input.
     */
    private Set<TeamNBA> parseTeamSelection(String input, TeamNBA[] allTeams) {
        Set<TeamNBA> selected = new HashSet<>();

        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                int index = Integer.parseInt(part.trim()) - 1;
                if (index >= 0 && index < allTeams.length) {
                    selected.add(allTeams[index]);
                }
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }

        return selected;
    }

    /**
     * Displays a summary of the venue before confirmation.
     */
    private void displayVenueSummary(String name, VenueType type, String address,
                                     String city, int capacity, Set<TeamNBA> teams) {
        printSeparator();
        print("VENUE SUMMARY:");
        print("  Name:     " + name);
        print("  Type:     " + type.getDisplayName());
        print("  Address:  " + address);
        print("  City:     " + city);
        print("  Capacity: " + capacity);
        print("  Teams:    " + teams.stream()
                .map(TeamNBA::getDisplayName)
                .collect(Collectors.joining(", ")));
        printSeparator();
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