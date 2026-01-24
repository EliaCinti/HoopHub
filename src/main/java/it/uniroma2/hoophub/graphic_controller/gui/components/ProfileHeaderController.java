package it.uniroma2.hoophub.graphic_controller.gui.components;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reusable JavaFX controller for the profile header component.
 *
 * <p>Displays user avatar and username with a dropdown menu for logout.
 * Designed to be included via {@code <fx:include>} in homepage FXML files.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ProfileHeaderController {

    @FXML
    private MenuButton profileMenuButton;
    @FXML
    private Circle avatarCircle;

    private static final Logger logger = Logger.getLogger(ProfileHeaderController.class.getName());
    private static final String DEFAULT_AVATAR = "/it/uniroma2/hoophub/images/default_avatar.png";

    /**
     * Initializes the header by loading user info and avatar.
     */
    public void initialize() {
        loadUserInfo();
        loadAvatarImage();
    }

    /**
     * Loads current user's username into the menu button.
     */
    private void loadUserInfo() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getUsername();
            profileMenuButton.setText(displayName != null ? displayName : "Fan");
        } else {
            profileMenuButton.setText("Ospite");
        }
    }

    /**
     * Loads avatar image into the circle shape using ImagePattern.
     */
    private void loadAvatarImage() {
        try {
            InputStream imageStream = getClass().getResourceAsStream(DEFAULT_AVATAR);
            if (imageStream != null) {
                avatarCircle.setFill(new ImagePattern(new Image(imageStream)));
            } else {
                logger.warning("Default avatar image not found at: " + DEFAULT_AVATAR);
                avatarCircle.setStyle("-fx-fill: lightgray;");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load avatar image", e);
        }
    }

    /**
     * Handles logout menu item click. Clears session and navigates to log in.
     */
    @FXML
    private void onLogoutClick() {
        try {
            SessionManager.INSTANCE.logout();
            NavigatorSingleton.getInstance().gotoPage("/it/uniroma2/hoophub/fxml/login.fxml");

            Stage currentStage = (Stage) profileMenuButton.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load login page", e);
        }
    }
}