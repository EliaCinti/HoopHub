package it.uniroma2.hoophub.graphic_controller.gui.sign_up;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.SignUpDataSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * JavaFX graphic controller for sign-up step 3 (Fan): role-specific data.
 *
 * <p>Collects favorite NBA team and birthday with autocomplete support.
 * Completes registration via {@link SignUpController} and navigates to Fan homepage.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class SignUpFanGraphicController {

    @FXML
    private Label msgLabel;
    @FXML
    private TextField teamField;
    @FXML
    private ListView<TeamNBA> teamSuggestions;
    @FXML
    private TextField dayField;
    @FXML
    private TextField monthField;
    @FXML
    private TextField yearField;
    @FXML
    private ListView<Integer> daySuggestions;
    @FXML
    private ListView<String> monthSuggestions;
    @FXML
    private ListView<Integer> yearSuggestions;
    @FXML
    private Button signUpButton;

    private final SignUpController signUpController = new SignUpController();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final SignUpDataSingleton dataSingleton = SignUpDataSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(SignUpFanGraphicController.class.getName());

    private TeamNBA selectedTeam;
    private Integer selectedDay;
    private String selectedMonth;
    private Integer selectedYear;

    private final List<TeamNBA> allTeams = Arrays.asList(TeamNBA.values());
    private final List<Integer> allDays = IntStream.rangeClosed(1, 31).boxed().toList();
    private final List<String> allMonths = Arrays.asList(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );
    private final List<Integer> allYears;

    private static final String PAGE_TITLE = "Complete your Fan profile";
    private static final String EMPTY_FIELDS_MSG = "Please fill in all fields";
    private static final String INVALID_TEAM_MSG = "Please select a valid NBA team";
    private static final String INVALID_DATE_MSG = "Please select a valid date";
    private static final String SIGNUP_FAILED_MSG = "Sign up failed: ";
    private static final String NAV_ERROR_MSG = "Error loading page";

    public SignUpFanGraphicController() {
        int currentYear = LocalDate.now().getYear();
        allYears = IntStream.iterate(currentYear - 13, i -> i >= 1930, i -> i - 1).boxed().toList();
    }

    /**
     * Initializes the controller and sets up autocomplete fields.
     */
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);

        UIHelper.showTitle(msgLabel, PAGE_TITLE);

        if (!dataSingleton.hasBasicData() || !dataSingleton.hasUserType()) {
            logger.warning("SignUpFan accessed without required data - redirecting to SignUp");
            navigateToSignUp();
            return;
        }

        setupTeamAutocomplete();
        setupDayAutocomplete();
        setupMonthAutocomplete();
        setupYearAutocomplete();
    }

    // ==================== TEAM AUTOCOMPLETE ====================

    private void setupTeamAutocomplete() {
        teamSuggestions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TeamNBA team, boolean empty) {
                super.updateItem(team, empty);
                setText(empty || team == null ? null : team.getDisplayName() + " (" + team.getAbbreviation() + ")");
            }
        });

        teamField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                hideSuggestions(teamSuggestions);
                selectedTeam = null;
                return;
            }

            List<TeamNBA> filtered = filterTeams(newVal.trim());
            if (filtered.isEmpty()) {
                hideSuggestions(teamSuggestions);
            } else {
                showSuggestions(teamSuggestions, FXCollections.observableArrayList(filtered));
            }
        });

        teamSuggestions.setOnMouseClicked(e -> {
            TeamNBA selected = teamSuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedTeam = selected;
                teamField.setText(selected.getDisplayName());
                hideSuggestions(teamSuggestions);
            }
        });

        setupFocusLostHandler(teamField, teamSuggestions);
        hideSuggestions(teamSuggestions);
    }

    private List<TeamNBA> filterTeams(String input) {
        String lowerInput = input.toLowerCase();
        return allTeams.stream()
                .filter(team ->
                        team.getDisplayName().toLowerCase().contains(lowerInput) ||
                                team.getAbbreviation().toLowerCase().contains(lowerInput) ||
                                team.name().toLowerCase().contains(lowerInput))
                .limit(6)
                .toList();
    }

    // ==================== DATE AUTOCOMPLETE ====================

    private void setupDayAutocomplete() {
        setupIntegerListView(daySuggestions);
        setupAutocompleteField(dayField, daySuggestions, allDays, this::filterDays,
                selected -> { selectedDay = selected; dayField.setText(String.valueOf(selected)); });
    }

    private List<Integer> filterDays(String input) {
        return allDays.stream().filter(day -> String.valueOf(day).contains(input)).toList();
    }

    private void setupMonthAutocomplete() {
        setupStringListView(monthSuggestions);
        setupAutocompleteField(monthField, monthSuggestions, allMonths, this::filterMonths,
                selected -> { selectedMonth = selected; monthField.setText(selected); });
    }

    private List<String> filterMonths(String input) {
        String lowerInput = input.toLowerCase();
        return allMonths.stream().filter(month -> month.toLowerCase().contains(lowerInput)).toList();
    }

    private void setupYearAutocomplete() {
        setupIntegerListView(yearSuggestions);
        List<Integer> defaultYears = allYears.subList(0, Math.min(10, allYears.size()));
        setupAutocompleteFieldWithDefault(yearField, yearSuggestions, defaultYears, this::filterYears,
                selected -> { selectedYear = selected; yearField.setText(String.valueOf(selected)); });
    }

    private List<Integer> filterYears(String input) {
        return allYears.stream().filter(year -> String.valueOf(year).contains(input)).limit(10).toList();
    }

    // ==================== GENERIC AUTOCOMPLETE ====================

    private void setupIntegerListView(ListView<Integer> listView) {
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });
    }

    private void setupStringListView(ListView<String> listView) {
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
    }

    private <T> void setupAutocompleteField(TextField field, ListView<T> listView, List<T> allItems,
                                            Function<String, List<T>> filterFunction, Consumer<T> onSelect) {
        setupAutocompleteFieldWithDefault(field, listView, allItems, filterFunction, onSelect);
    }

    private <T> void setupAutocompleteFieldWithDefault(TextField field, ListView<T> listView, List<T> defaultItems,
                                                       Function<String, List<T>> filterFunction, Consumer<T> onSelect) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                handleFocusChange(field, listView, defaultItems, filterFunction, Boolean.TRUE.equals(isFocused)));

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (field.isFocused()) {
                updateSuggestions(listView, defaultItems, filterFunction, newVal);
            }
        });

        listView.setOnMouseClicked(e -> handleSelection(listView, onSelect));

        hideSuggestions(listView);
    }

    private <T> void handleFocusChange(TextField field, ListView<T> listView, List<T> defaultItems,
                                       Function<String, List<T>> filterFunction, boolean isFocused) {
        if (isFocused) {
            updateSuggestions(listView, defaultItems, filterFunction, field.getText());
        } else {
            javafx.application.Platform.runLater(() -> {
                if (!listView.isFocused()) hideSuggestions(listView);
            });
        }
    }

    private <T> void updateSuggestions(ListView<T> listView, List<T> defaultItems,
                                       Function<String, List<T>> filterFunction, String text) {
        List<T> filtered = (text == null || text.trim().isEmpty()) ? defaultItems : filterFunction.apply(text.trim());
        showSuggestions(listView, FXCollections.observableArrayList(filtered));
    }

    private <T> void handleSelection(ListView<T> listView, Consumer<T> onSelect) {
        T selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onSelect.accept(selected);
            hideSuggestions(listView);
        }
    }

    private <T> void showSuggestions(ListView<T> listView, ObservableList<T> items) {
        listView.setItems(items);
        listView.setVisible(true);
        listView.setManaged(true);
        listView.setPrefHeight(Math.min((double) items.size() * 36 + 12, listView.getMaxHeight()));
    }

    private void hideSuggestions(ListView<?> listView) {
        listView.setVisible(false);
        listView.setManaged(false);
    }

    private void setupFocusLostHandler(TextField field, ListView<?> listView) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (Boolean.FALSE.equals(isFocused)) {
                javafx.application.Platform.runLater(() -> {
                    if (!listView.isFocused()) hideSuggestions(listView);
                });
            }
        });
    }

    // ==================== SIGN UP ====================

    /**
     * Handles Sign-Up button click. Validates and completes registration.
     */
    @FXML
    private void onSignUpClick() {
        String teamText = teamField.getText().trim();
        String dayText = dayField.getText().trim();
        String monthText = monthField.getText().trim();
        String yearText = yearField.getText().trim();

        if (teamText.isEmpty() || dayText.isEmpty() || monthText.isEmpty() || yearText.isEmpty()) {
            UIHelper.showError(msgLabel, EMPTY_FIELDS_MSG);
            return;
        }

        LocalDate birthday = parseBirthday(dayText, monthText, yearText);
        if (birthday == null) return;

        if (!validateAndResolveTeam(teamText)) return;

        attemptSignUp(selectedTeam, birthday);
    }

    private LocalDate parseBirthday(String dayText, String monthText, String yearText) {
        Integer day = parseDay(dayText);
        Integer month = parseMonth(monthText);
        Integer year = parseYear(yearText);

        if (day == null || month == null || year == null) {
            UIHelper.showError(msgLabel, INVALID_DATE_MSG);
            return null;
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            UIHelper.showError(msgLabel, INVALID_DATE_MSG);
            return null;
        }
    }

    private Integer parseDay(String dayText) {
        if (selectedDay != null) return selectedDay;
        try { return Integer.parseInt(dayText); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseMonth(String monthText) {
        if (selectedMonth != null) return allMonths.indexOf(selectedMonth) + 1;
        return allMonths.stream().filter(m -> m.equalsIgnoreCase(monthText))
                .findFirst().map(m -> allMonths.indexOf(m) + 1).orElse(null);
    }

    private Integer parseYear(String yearText) {
        if (selectedYear != null) return selectedYear;
        try { return Integer.parseInt(yearText); } catch (NumberFormatException e) { return null; }
    }

    private boolean validateAndResolveTeam(String teamText) {
        if (selectedTeam == null) selectedTeam = TeamNBA.robustValueOf(teamText);

        if (selectedTeam == null) {
            UIHelper.showError(msgLabel, INVALID_TEAM_MSG);
            return false;
        }

        try {
            FanBean.validateTeamSyntax(teamText);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return false;
        }

        return true;
    }

    private void attemptSignUp(TeamNBA favTeam, LocalDate birthday) {
        try {
            FanBean.validateBirthday(birthday);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return;
        }

        try {
            FanBean fanBean = new FanBean.Builder()
                    .username(dataSingleton.getUsername())
                    .password(dataSingleton.getPassword())
                    .fullName(dataSingleton.getFullName())
                    .gender(dataSingleton.getGender())
                    .type(UserType.FAN)
                    .favTeam(favTeam)
                    .birthday(birthday)
                    .build();

            UserBean registeredUser = signUpController.signUp(fanBean, true);
            dataSingleton.clearUserData();
            logger.info("Fan registered successfully: " + registeredUser.getUsername());
            navigateToHomepage();

        } catch (DAOException e) {
            logger.log(Level.WARNING, "Sign up failed", e);
            UIHelper.showError(msgLabel, SIGNUP_FAILED_MSG + e.getMessage());
        } catch (UserSessionException e) {
            logger.log(Level.WARNING, "Session error during sign up", e);
            UIHelper.showError(msgLabel, SIGNUP_FAILED_MSG + e.getMessage());
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void onBackClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/sign_up_choice.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up choice page", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    @FXML
    private void onSignInClick() {
        dataSingleton.clearUserData();
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/login.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load login page", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    private void navigateToSignUp() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/sign_up.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up page", e);
        }
    }

    private void navigateToHomepage() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/fan_homepage.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load homepage", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    private void closeCurrentWindow() {
        Stage stage = (Stage) signUpButton.getScene().getWindow();
        stage.close();
    }
}