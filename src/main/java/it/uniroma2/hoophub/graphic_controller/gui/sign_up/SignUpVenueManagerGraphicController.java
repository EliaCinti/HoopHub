package it.uniroma2.hoophub.graphic_controller.gui.sign_up;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.SignUpDataSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for sign-up step 3 (VenueManager): role-specific data.
 *
 * <p>Collects company name and phone number. Completes registration via
 * {@link SignUpController} and navigates to VenueManager homepage.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class SignUpVenueManagerGraphicController {

    @FXML
    private Label msgLabel;
    @FXML
    private TextField companyNameField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private Button signUpButton;

    private final SignUpController signUpController = new SignUpController();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final SignUpDataSingleton dataSingleton = SignUpDataSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(SignUpVenueManagerGraphicController.class.getName());

    private static final String PAGE_TITLE = "Complete your Venue profile";
    private static final String EMPTY_FIELDS_MSG = "Please fill in all fields";
    private static final String SIGNUP_FAILED_MSG = "Sign up failed: ";
    private static final String NAV_ERROR_MSG = "Error loading page";

    /**
     * Initializes the controller and verifies required data from previous steps.
     */
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);

        UIHelper.showTitle(msgLabel, PAGE_TITLE);

        if (!dataSingleton.hasBasicData() || !dataSingleton.hasUserType()) {
            logger.warning("SignUpVenueManager accessed without required data - redirecting to SignUp");
            navigateToSignUp();
        }
    }

    /**
     * Handles Sign-Up button click. Validates and completes registration.
     */
    @FXML
    private void onSignUpClick() {
        String companyName = companyNameField.getText().trim();
        String phoneNumber = phoneNumberField.getText().trim();

        if (companyName.isEmpty() || phoneNumber.isEmpty()) {
            UIHelper.showError(msgLabel, EMPTY_FIELDS_MSG);
            return;
        }

        if (!validateInputs(companyName, phoneNumber)) {
            return;
        }

        attemptSignUp(companyName, phoneNumber);
    }

    /**
     * Validates company name and phone number syntax via Bean.
     */
    private boolean validateInputs(String companyName, String phoneNumber) {
        try {
            VenueManagerBean.validateCompanyNameSyntax(companyName);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return false;
        }

        try {
            VenueManagerBean.validatePhoneNumberSyntax(phoneNumber);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Attempts to complete registration via SignUpController.
     */
    private void attemptSignUp(String companyName, String phoneNumber) {
        try {
            VenueManagerBean venueManagerBean = new VenueManagerBean.Builder()
                    .username(dataSingleton.getUsername())
                    .password(dataSingleton.getPassword())
                    .fullName(dataSingleton.getFullName())
                    .gender(dataSingleton.getGender())
                    .type(UserType.VENUE_MANAGER)
                    .companyName(companyName)
                    .phoneNumber(phoneNumber)
                    .build();

            UserBean registeredUser = signUpController.signUp(venueManagerBean, true);
            dataSingleton.clearUserData();
            logger.info("VenueManager registered successfully: " + registeredUser.getUsername());
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
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/venue_manager_homepage.fxml");
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