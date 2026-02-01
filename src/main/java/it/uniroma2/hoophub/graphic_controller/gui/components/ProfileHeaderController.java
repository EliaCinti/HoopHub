package it.uniroma2.hoophub.graphic_controller.gui.components;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.session.SessionManager;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import it.uniroma2.hoophub.utilities.UserAvatarResolver;
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
 * Avatar is dynamically resolved based on user type and gender using
 * {@link UserAvatarResolver}.</p>
 *
 * <p>Designed to be included via {@code <fx:include>} in homepage FXML files.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class ProfileHeaderController {

    @FXML
    private MenuButton profileMenuButton;
    @FXML
    private Circle avatarCircle;

    private static final Logger LOGGER = Logger.getLogger(ProfileHeaderController.class.getName());

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
            profileMenuButton.setText(displayName != null ? displayName : "User");
        } else {
            profileMenuButton.setText("Guest");
        }
    }

    /**
     * Loads avatar image into the circle shape using ImagePattern.
     * Avatar is resolved dynamically based on user type and gender.
     */
    private void loadAvatarImage() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();

        if (currentUser == null) {
            setFallbackAvatar();
            return;
        }

        try {
            String avatarPath = UserAvatarResolver.getAvatarPath(
                    currentUser.getType(),
                    currentUser.getGender()
            );

            InputStream imageStream = getClass().getResourceAsStream(avatarPath);
            if (imageStream != null) {
                avatarCircle.setFill(new ImagePattern(new Image(imageStream)));
                LOGGER.log(Level.FINE, "Avatar loaded: {0}", avatarPath);
            } else {
                LOGGER.log(Level.WARNING, "Avatar image not found at: {0}", avatarPath);                setFallbackAvatar();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load avatar image", e);
            setFallbackAvatar();
        }
    }

    /**
     * Sets a fallback style when avatar image cannot be loaded.
     */
    private void setFallbackAvatar() {
        avatarCircle.setStyle("-fx-fill: linear-gradient(to bottom, #667eea, #764ba2);");
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
            LOGGER.log(Level.SEVERE, "Unable to load login page", e);
        }
    }
}