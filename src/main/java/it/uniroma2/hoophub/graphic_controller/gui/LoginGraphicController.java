package it.uniroma2.hoophub.graphic_controller.gui;

import it.uniroma2.hoophub.app_controller.LoginController;
import it.uniroma2.hoophub.beans.CredentialsBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.User;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UIHelper;
import it.uniroma2.hoophub.utilities.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginGraphicController {
    @FXML
    private Label msgLabel;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;

    private final LoginController loginController = LoginController.getInstance();
    private final NavigatorSingleton navigatorSingleton = NavigatorSingleton.getInstance();
    private static final Logger logger = Logger.getLogger(LoginGraphicController.class.getName());

    public void initialize() {
        UIHelper.showMessage(msgLabel, "Welcome to Hoophub");
    }

    /**
     * Handles the login button click event.
     * Validates user input, authenticates credentials, and uses POLYMORPHISM
     * to navigate to the appropriate home screen based on user type.
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

            User loggedUser = loginController.login(credentialsBean);

            navigateToHomepage(loggedUser);
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Error while logging in: " + usernameText, e);
            UIHelper.showError(msgLabel, "Login failed: " + e.getMessage());
        } catch (UserSessionException e) {
            logger.log(Level.INFO, "User already logged in: " + usernameText, e);
            UIHelper.showError(msgLabel, "User already logged in");
        }
    }

    /**
     * Navigates to the appropriate homepage based on user type WITHOUT using instanceof.
     * This method calls the abstract getUserType() method, which is implemented differently
     * by Fan and VenueManager classes. The decision of which implementation to call is made
     * at RUNTIME (late binding).
     *
     * @param user The authenticated user
     */
    private void navigateToHomepage(User user) {
        try {
            UserType userType = user.getUserType();

            // Navigate based on user type
            if (userType == UserType.FAN) {
                navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/FanHomepage.fxml");
            } else if (userType == UserType.VENUE_MANAGER) {
                navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/VenueManagerHomepage.fxml");
            }

            // close the current login window
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();


        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load homepage", e);
            UIHelper.showError(msgLabel, "Error loading homepage");
        }
    }

    @FXML
    private void onSignUpClick() {
        try {
            // Open sign up page FIRST
            navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/SignUp.fxml");

            // Close the login page AFTER
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load sign up page", e);
            UIHelper.showError(msgLabel, "Error loading sign up page");
        }
    }
}
