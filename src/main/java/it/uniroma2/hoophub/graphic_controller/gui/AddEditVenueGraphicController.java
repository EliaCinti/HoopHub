package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.ManageVenuesController;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.VenueType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the Add/Edit Venue screen.
 * Handles both creating new venues and editing existing ones.
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class AddEditVenueGraphicController {

    private static final Logger logger = Logger.getLogger(AddEditVenueGraphicController.class.getName());

    // Page Titles
    private static final String ADD_TITLE = "Add New Venue";
    private static final String EDIT_TITLE = "Edit Venue";

    // Error Messages
    private static final String EMPTY_FIELDS_MSG = "Please fill in all required fields";
    private static final String SELECT_TYPE_MSG = "Please select a venue type";
    private static final String FAN_CLUB_TEAM_MSG = "Fan Club venues can only have one team";
    private static final String CREATE_ERROR_MSG = "Failed to create venue";
    private static final String UPDATE_ERROR_MSG = "Failed to update venue";
    private static final String NAV_ERROR_MSG = "Error loading page";

    private static final String SELECTED = "selected";


    // FXML Components
    @FXML private Label msgLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<VenueType> typeComboBox;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private Spinner<Integer> capacitySpinner;
    @FXML private Label capacityHintLabel;
    @FXML private TextField teamSearchField;
    @FXML private ListView<TeamNBA> teamsListView;
    @FXML private Label teamsCountLabel;
    @FXML private Button saveButton;

    private final ManageVenuesController manageVenuesController = new ManageVenuesController();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();

    // State
    private boolean isEditMode = false;
    private VenueBean venueToEdit = null;
    private String currentPageTitle = ADD_TITLE;
    private final Set<TeamNBA> selectedTeams = new HashSet<>();
    private FilteredList<TeamNBA> filteredTeams;

    /**
     * Initializes the controller and sets up form components.
     */
    @FXML
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);
        msgLabel.setOpacity(0.0);

        setupVenueTypeComboBox();
        setupCapacitySpinner();
        setupTeamsList();
        setupTeamSearch();
    }

    /**
     * Initializes the form in ADD mode.
     */
    public void initAddMode() {
        isEditMode = false;
        venueToEdit = null;
        currentPageTitle = ADD_TITLE;
        UIHelper.showTitle(msgLabel, ADD_TITLE);
        saveButton.setText("Create Venue");
        clearForm();
    }

    /**
     * Initializes the form in EDIT mode with existing venue data.
     *
     * @param venue The venue to edit
     */
    public void initEditMode(VenueBean venue) {
        isEditMode = true;
        venueToEdit = venue;
        currentPageTitle = EDIT_TITLE;
        UIHelper.showTitle(msgLabel, EDIT_TITLE);
        saveButton.setText("Save Changes");
        populateForm(venue);
    }

    // ==================== SETUP METHODS ====================

    /**
     * Sets up the venue type combo box with all available types.
     */
    private void setupVenueTypeComboBox() {
        typeComboBox.setItems(FXCollections.observableArrayList(VenueType.values()));
        typeComboBox.setCellFactory(listView -> createVenueTypeCell());
        typeComboBox.setConverter(createVenueTypeConverter());
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onVenueTypeChanged(newVal));
    }

    /**
     * Creates a ListCell for VenueType display.
     */
    private ListCell<VenueType> createVenueTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(VenueType type, boolean empty) {
                super.updateItem(type, empty);
                setText(empty || type == null ? null : type.getDisplayName());
            }
        };
    }

    /**
     * Creates a StringConverter for VenueType.
     */
    private StringConverter<VenueType> createVenueTypeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(VenueType type) {
                return type == null ? null : type.getDisplayName();
            }

            @Override
            public VenueType fromString(String string) {
                return Arrays.stream(VenueType.values())
                        .filter(t -> t.getDisplayName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    /**
     * Handles venue type selection change.
     */
    private void onVenueTypeChanged(VenueType newVal) {
        if (newVal != null) {
            updateCapacityHint(newVal);
            updateCapacitySpinnerMax(newVal);
            handleFanClubTeamLimit(newVal);
        }
    }

    /**
     * Sets up the capacity spinner.
     */
    private void setupCapacitySpinner() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 50);
        capacitySpinner.setValueFactory(valueFactory);
    }

    /**
     * Sets up the teams list view.
     */
    private void setupTeamsList() {
        ObservableList<TeamNBA> allTeams = FXCollections.observableArrayList(TeamNBA.values());
        filteredTeams = new FilteredList<>(allTeams, p -> true);
        teamsListView.setItems(filteredTeams);
        teamsListView.setCellFactory(listView -> createTeamCell());
        teamsListView.setOnMouseClicked(event -> onTeamClicked());
    }

    /**
     * Creates a ListCell for TeamNBA display with checkbox style.
     */
    private ListCell<TeamNBA> createTeamCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TeamNBA team, boolean empty) {
                super.updateItem(team, empty);
                if (empty || team == null) {
                    setText(null);
                    getStyleClass().removeAll(SELECTED);
                } else {
                    updateTeamCellContent(this, team);
                }
            }
        };
    }

    /**
     * Updates the content and style of a team cell.
     */
    private void updateTeamCellContent(ListCell<TeamNBA> cell, TeamNBA team) {
        boolean isSelected = selectedTeams.contains(team);
        String checkbox = isSelected ? "☑" : "☐";
        cell.setText(checkbox + "  " + team.getDisplayName());

        if (isSelected && !cell.getStyleClass().contains(SELECTED)) {
            cell.getStyleClass().add(SELECTED);
        } else if (!isSelected) {
            cell.getStyleClass().removeAll(SELECTED);
        }
    }

    /**
     * Handles click on a team in the list.
     */
    private void onTeamClicked() {
        TeamNBA clickedTeam = teamsListView.getSelectionModel().getSelectedItem();
        if (clickedTeam != null) {
            toggleTeamSelection(clickedTeam);
        }
    }

    /**
     * Sets up the team search functionality.
     */
    private void setupTeamSearch() {
        teamSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String searchText = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredTeams.setPredicate(team -> searchText.isEmpty()
                    || team.getDisplayName().toLowerCase().contains(searchText)
                    || team.name().toLowerCase().contains(searchText));
        });
    }

    /**
     * Toggles the selection of a team.
     */
    private void toggleTeamSelection(TeamNBA team) {
        VenueType selectedType = typeComboBox.getValue();

        if (selectedTeams.contains(team)) {
            handleTeamDeselection(team);
        } else {
            handleTeamSelection(team, selectedType);
        }
    }

    /**
     * Handles deselecting a team.
     * Always allows deselection - validation at save time checks for at least one team.
     */
    private void handleTeamDeselection(TeamNBA team) {
        selectedTeams.remove(team);
        teamsListView.refresh();
        updateTeamsCountLabel();
    }

    /**
     * Handles selecting a team.
     * For Fan Club venues, replace the existing team instead of blocking.
     */
    private void handleTeamSelection(TeamNBA team, VenueType selectedType) {
        if (selectedType == VenueType.FAN_CLUB && !selectedTeams.isEmpty()) {
            // Fan Club: replace existing team with new selection
            selectedTeams.clear();
        }
        selectedTeams.add(team);
        teamsListView.refresh();
        updateTeamsCountLabel();
    }

    /**
     * Handles the FAN_CLUB team limit rule.
     */
    private void handleFanClubTeamLimit(VenueType type) {
        if (type == VenueType.FAN_CLUB && selectedTeams.size() > 1) {
            TeamNBA firstTeam = selectedTeams.iterator().next();
            selectedTeams.clear();
            selectedTeams.add(firstTeam);
            teamsListView.refresh();
            updateTeamsCountLabel();
            UIHelper.showMessageThenTitle(msgLabel, FAN_CLUB_TEAM_MSG, currentPageTitle);
        }
    }

    /**
     * Updates the capacity hint label based on venue type.
     */
    private void updateCapacityHint(VenueType type) {
        capacityHintLabel.setText("(max: " + type.getMaxCapacityLimit() + ")");
    }

    /**
     * Updates the capacity spinner max value based on venue type.
     */
    private void updateCapacitySpinnerMax(VenueType type) {
        int currentValue = capacitySpinner.getValue();
        int maxLimit = type.getMaxCapacityLimit();
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxLimit,
                        Math.min(currentValue, maxLimit));
        capacitySpinner.setValueFactory(valueFactory);
    }

    /**
     * Updates the teams count label.
     */
    private void updateTeamsCountLabel() {
        int count = selectedTeams.size();
        teamsCountLabel.setText("(" + count + " selected)");
    }

    // ==================== FORM OPERATIONS ====================

    /**
     * Clears all form fields.
     */
    private void clearForm() {
        nameField.clear();
        typeComboBox.setValue(null);
        addressField.clear();
        cityField.clear();
        capacitySpinner.getValueFactory().setValue(50);
        capacityHintLabel.setText("");
        teamSearchField.clear();
        selectedTeams.clear();
        teamsListView.refresh();
        updateTeamsCountLabel();
    }

    /**
     * Populates the form with venue data for editing.
     */
    private void populateForm(VenueBean venue) {
        nameField.setText(venue.getName());
        typeComboBox.setValue(venue.getType());
        addressField.setText(venue.getAddress());
        cityField.setText(venue.getCity());
        capacitySpinner.getValueFactory().setValue(venue.getMaxCapacity());

        selectedTeams.clear();
        selectedTeams.addAll(venue.getAssociatedTeams());
        teamsListView.refresh();
        updateTeamsCountLabel();

        if (venue.getType() != null) {
            updateCapacityHint(venue.getType());
        }
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Handles back button click.
     */
    @FXML
    private void onBackClick() {
        navigateToManageVenues();
    }

    /**
     * Handles cancel button click.
     */
    @FXML
    private void onCancelClick() {
        navigateToManageVenues();
    }

    /**
     * Handles save button click.
     */
    @FXML
    private void onSaveClick() {
        if (!validateForm()) {
            return;
        }

        try {
            VenueBean venueBean = buildVenueBeanFromForm();

            if (isEditMode) {
                venueBean.setId(venueToEdit.getId());
                manageVenuesController.updateVenue(venueBean);
                logger.info("Venue updated: " + venueBean.getName());
            } else {
                manageVenuesController.createVenue(venueBean);
                logger.info("Venue created: " + venueBean.getName());
            }

            navigateToManageVenues();

        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
        } catch (UserSessionException | DAOException e) {
            String errorMsg = isEditMode ? UPDATE_ERROR_MSG : CREATE_ERROR_MSG;
            logger.log(Level.SEVERE, errorMsg, e);
            UIHelper.showErrorThenTitle(msgLabel, errorMsg + ": " + e.getMessage(), currentPageTitle);
        }
    }

    /**
     * Validates all form fields.
     *
     * @return true if form is valid, false otherwise
     */
    private boolean validateForm() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();

        if (name.isEmpty() || address.isEmpty() || city.isEmpty()) {
            UIHelper.showErrorThenTitle(msgLabel, EMPTY_FIELDS_MSG, currentPageTitle);
            return false;
        }

        return validateName(name)
                && validateType()
                && validateAddress(address)
                && validateCity(city)
                && validateCapacity()
                && validateTeams();
    }

    private boolean validateName(String name) {
        try {
            VenueBean.validateNameSyntax(name);
            return true;
        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
            return false;
        }
    }

    private boolean validateType() {
        if (typeComboBox.getValue() == null) {
            UIHelper.showErrorThenTitle(msgLabel, SELECT_TYPE_MSG, currentPageTitle);
            return false;
        }
        return true;
    }

    private boolean validateAddress(String address) {
        try {
            VenueBean.validateAddressSyntax(address);
            return true;
        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
            return false;
        }
    }

    private boolean validateCity(String city) {
        try {
            VenueBean.validateCitySyntax(city);
            return true;
        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
            return false;
        }
    }

    private boolean validateCapacity() {
        try {
            VenueBean.validateCapacity(capacitySpinner.getValue(), typeComboBox.getValue());
            return true;
        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
            return false;
        }
    }

    private boolean validateTeams() {
        try {
            VenueBean.validateTeams(selectedTeams, typeComboBox.getValue());
            return true;
        } catch (IllegalArgumentException e) {
            UIHelper.showErrorThenTitle(msgLabel, e.getMessage(), currentPageTitle);
            return false;
        }
    }

    /**
     * Builds a VenueBean from the current form values.
     */
    private VenueBean buildVenueBeanFromForm() {
        return new VenueBean.Builder()
                .name(nameField.getText().trim())
                .type(typeComboBox.getValue())
                .address(addressField.getText().trim())
                .city(cityField.getText().trim())
                .maxCapacity(capacitySpinner.getValue())
                .associatedTeams(new HashSet<>(selectedTeams))
                .build();
    }

    /**
     * Navigates back to the Manage Venues screen.
     */
    private void navigateToManageVenues() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/manage_venues.fxml");
            closeCurrentStage();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load manage venues page", e);
            UIHelper.showErrorThenTitle(msgLabel, NAV_ERROR_MSG, currentPageTitle);
        }
    }

    /**
     * Closes the current stage.
     */
    private void closeCurrentStage() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}