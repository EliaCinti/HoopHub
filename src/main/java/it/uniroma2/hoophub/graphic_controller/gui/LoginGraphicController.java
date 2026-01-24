package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.LoginController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.graphic_controller.cli.CliLoginGraphicController;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import it.uniroma2.hoophub.enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX graphic controller for the login use case.
 *
 * <p>Handles user authentication and navigation to appropriate homepage
 * based on user type. Works with UserBean (data only) to maintain
 * boundary/controller separation.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CliLoginGraphicController
 */
public class LoginGraphicController {

    @FXML
    private Label msgLabel;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;

    private final LoginController loginController = new LoginController();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(LoginGraphicController.class.getName());

    /**
     * Initializes the controller with welcome message.
     */
    public void initialize() {
        msgLabel.setManaged(true);
        msgLabel.setVisible(true);
        msgLabel.setMinHeight(40);
        msgLabel.setOpacity(0.0);

        UIHelper.showTitle(msgLabel, "Welcome to HoopHub");
    }

    /**
     * Handles login button click. Validates input, authenticates, and navigates.
     */
    @FXML
    private void onLoginClick() {
        String usernameText = username.getText();
        String passwordText = password.getText();

        if (usernameText.isEmpty() || passwordText.isEmpty()) {
            UIHelper.showError(msgLabel, "Please enter your username and password");
            return;
        }

        try {
            CredentialsBean credentialsBean = new CredentialsBean.Builder<>()
                    .username(usernameText)
                    .password(passwordText)
                    .build();

            UserBean userBean = loginController.login(credentialsBean);

            navigateToHomepage(userBean);
        } catch (DAOException e) {
            logger.log(Level.SEVERE, e, () -> "Error while logging in: " + usernameText);
            UIHelper.showError(msgLabel, "Login failed: " + e.getMessage());
        } catch (UserSessionException e) {
            logger.log(Level.INFO, e, () -> "User already logged in: " + usernameText);
            UIHelper.showError(msgLabel, "User already logged in");
        }
    }

    /**
     * Navigates to Fan or VenueManager homepage based on user type.
     *
     * @param userBean authenticated user data
     */
    private void navigateToHomepage(UserBean userBean) {
        try {
            UserType userType = userBean.getType();

            if (userType == UserType.FAN) {
                navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/fan_homepage.fxml");
            } else if (userType == UserType.VENUE_MANAGER) {
                navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/venue_manager_homepage.fxml");
            }

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load homepage", e);
            UIHelper.showError(msgLabel, "Error loading homepage");
        }
    }

    /**
     * Handles sign-up hyperlink click. Navigates to registration.
     */
    @FXML
    private void onSignUpClick() {
        try {
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/sign_up.fxml");

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up page", e);
            UIHelper.showError(msgLabel, "Error loading sign up page");
        }
    }

    /**
     * Handles Google Sign-In button click. Feature not yet implemented.
     */
    @FXML
    private void onGoogleSignInClick() {
        UIHelper.showMessage(msgLabel, "Feature Unavailable, Google Sign In is coming soon!");
    }
}