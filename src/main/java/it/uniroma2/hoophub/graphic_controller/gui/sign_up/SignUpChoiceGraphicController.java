package it.uniroma2.hoophub.graphic_controller.gui.sign_up;

import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.SignUpDataSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for sign-up step 2: user type selection.
 *
 * <p>Allows user to choose between Fan and VenueManager account types.
 * Stores selection in {@link SignUpDataSingleton} and navigates to the
 * appropriate role-specific registration form.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see SignUpFanGraphicController
 * @see SignUpVenueManagerGraphicController
 */
public class SignUpChoiceGraphicController {

    @FXML
    private Label msgLabel;
    @FXML
    private ToggleButton fanToggle;
    @FXML
    private ToggleButton venueManagerToggle;
    @FXML
    private ToggleGroup userTypeGroup;
    @FXML
    private Button continueButton;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final SignUpDataSingleton dataSingleton = SignUpDataSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(SignUpChoiceGraphicController.class.getName());

    private static final String PAGE_TITLE = "Select your account type";
    private static final String NO_SELECTION_MSG = "Please select a user type";
    private static final String NAV_ERROR_MSG = "Error loading page";

    /**
     * Initializes the controller. Verifies basic data from previous step.
     */
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);

        UIHelper.showTitle(msgLabel, PAGE_TITLE);

        if (!dataSingleton.hasBasicData()) {
            logger.warning("SignUpChoice accessed without basic data - redirecting to SignUp");
            navigateToSignUp();
            return;
        }

        if (dataSingleton.hasUserType()) {
            if (dataSingleton.getUserType() == UserType.FAN) {
                fanToggle.setSelected(true);
            } else if (dataSingleton.getUserType() == UserType.VENUE_MANAGER) {
                venueManagerToggle.setSelected(true);
            }
        }
    }

    /**
     * Handles Continue button click. Validates selection and navigates to role-specific form.
     */
    @FXML
    private void onContinueClick() {
        ToggleButton selectedToggle = (ToggleButton) userTypeGroup.getSelectedToggle();

        if (selectedToggle == null) {
            UIHelper.showError(msgLabel, NO_SELECTION_MSG);
            return;
        }

        UserType selectedType;
        String targetFxml;

        if (selectedToggle == fanToggle) {
            selectedType = UserType.FAN;
            targetFxml = "/it/uniroma2/hoophub/fxml/sign_up_fan.fxml";
        } else {
            selectedType = UserType.VENUE_MANAGER;
            targetFxml = "/it/uniroma2/hoophub/fxml/sign_up_venue_manager.fxml";
        }

        dataSingleton.setUserType(selectedType);

        try {
            navigatorSingleton.gotoPage(targetFxml);
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load registration form", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    /**
     * Handles Sign-In hyperlink click. Clears data and navigates to log in.
     */
    @FXML
    private void onSignInClick() {
        dataSingleton.clearUserData();
        navigateToLogin();
    }

    private void navigateToSignUp() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/SignUp.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up page", e);
        }
    }

    private void navigateToLogin() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/Login.fxml");
            closeCurrentWindow();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load login page", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    private void closeCurrentWindow() {
        Stage stage = (Stage) continueButton.getScene().getWindow();
        stage.close();
    }
}