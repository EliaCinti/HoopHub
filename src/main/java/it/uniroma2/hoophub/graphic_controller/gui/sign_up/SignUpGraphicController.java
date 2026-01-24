package it.uniroma2.hoophub.graphic_controller.gui.sign_up;

import it.uniroma2.hoophub.app_controller.SignUpController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.SignUpDataSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for sign-up step 1: basic user data.
 *
 * <p>Collects full name, gender, username, and password. Validation is
 * delegated to Bean classes; username availability is checked via
 * {@link SignUpController}. Data is stored in {@link SignUpDataSingleton}
 * for use in subsequent steps.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see SignUpChoiceGraphicController
 */
public class SignUpGraphicController {

    @FXML
    private Label msgLabel;
    @FXML
    private TextField fullName;
    @FXML
    private ComboBox<String> gender;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private PasswordField confirmPassword;
    @FXML
    private Button continueButton;

    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private final SignUpDataSingleton dataSingleton = SignUpDataSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(SignUpGraphicController.class.getName());

    private static final String PAGE_TITLE = "Create your account";
    private static final String EMPTY_FIELDS_MSG = "Please fill in all fields";
    private static final String PASSWORD_MISMATCH_MSG = "Passwords do not match";
    private static final String NAV_ERROR_MSG = "Error loading page";
    private static final String USERNAME_TAKEN_MSG = "Username already taken. Please choose another.";
    private static final String USERNAME_CHECK_ERROR_MSG = "Unable to verify username. Please try again.";

    private final SignUpController signUpController = new SignUpController();

    /**
     * Initializes the controller and clears previous sign-up data.
     */
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);

        UIHelper.showTitle(msgLabel, PAGE_TITLE);

        dataSingleton.clearUserData();
    }

    /**
     * Handles Continue button click. Validates all fields and navigates to user type selection.
     */
    @FXML
    private void onContinueClick() {
        String fullNameText = fullName.getText().trim();
        String genderText = gender.getValue();
        String usernameText = username.getText().trim();
        String passwordText = password.getText();
        String confirmPasswordText = confirmPassword.getText();

        if (fullNameText.isEmpty() || genderText == null || genderText.isEmpty() ||
                usernameText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
            UIHelper.showError(msgLabel, EMPTY_FIELDS_MSG);
            return;
        }

        try {
            UserBean.validateFullNameSyntax(fullNameText);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return;
        }

        try {
            UserBean.validateGenderSyntax(genderText);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return;
        }

        try {
            CredentialsBean.validateUsernameSyntax(usernameText);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return;
        }

        try {
            if (signUpController.isUsernameTaken(usernameText)) {
                UIHelper.showError(msgLabel, USERNAME_TAKEN_MSG);
                return;
            }
        } catch (UserSessionException e) {
            logger.log(Level.WARNING, "Error checking username availability", e);
            UIHelper.showError(msgLabel, USERNAME_CHECK_ERROR_MSG);
            return;
        }

        try {
            CredentialsBean.validatePasswordSyntax(passwordText);
        } catch (IllegalArgumentException e) {
            UIHelper.showError(msgLabel, e.getMessage());
            return;
        }

        if (!passwordText.equals(confirmPasswordText)) {
            UIHelper.showError(msgLabel, PASSWORD_MISMATCH_MSG);
            return;
        }

        storeDataAndNavigate(fullNameText, genderText, usernameText, passwordText);
    }

    /**
     * Stores validated data and navigates to user type selection.
     */
    private void storeDataAndNavigate(String fullNameText, String genderText,
                                      String usernameText, String passwordText) {
        try {
            dataSingleton.setFullName(fullNameText);
            dataSingleton.setGender(genderText);
            dataSingleton.setUsername(usernameText);
            dataSingleton.setPassword(passwordText);

            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/sign_up_choice.fxml");

            Stage currentStage = (Stage) continueButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up choice page", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }

    /**
     * Handles Sign-In hyperlink click. Clears data and navigates to log in.
     */
    @FXML
    private void onSignInClick() {
        try {
            dataSingleton.clearUserData();

            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/Login.fxml");

            Stage stage = (Stage) continueButton.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load login page", e);
            UIHelper.showError(msgLabel, NAV_ERROR_MSG);
        }
    }
}