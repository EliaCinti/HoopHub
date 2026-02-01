package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.enums.UserType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to resolve user avatar images based on user type and gender.
 *
 * <p>Avatars are not persisted in the database - they are derived at runtime
 * based on the user's type (FAN/VENUE_MANAGER) and gender.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class UserAvatarResolver {

    private static final Logger LOGGER = Logger.getLogger(UserAvatarResolver.class.getName());

    private static final String IMAGES_BASE_PATH;

    // Avatar filenames
    private static final String FAN_MALE = "fan_male.png";
    private static final String FAN_FEMALE = "fan_female.png";
    private static final String MANAGER_MALE = "manager_male.png";
    private static final String MANAGER_FEMALE = "manager_female.png";
    private static final String USER_DEFAULT_AVATAR = "user_default_avatar.png";

    // Gender identifier
    private static final String FEMALE = "Female";

    static {
        String loadedPath = null;
        try (InputStream input = UserAvatarResolver.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                loadedPath = prop.getProperty("images.avatar.base.path");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading config.properties", ex);
        }

        if (loadedPath == null || loadedPath.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Configuration 'images.avatar.base.path' not found in config.properties. Check your configuration."
            );
        }

        IMAGES_BASE_PATH = loadedPath;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private UserAvatarResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the full resource path to the avatar image for a given user type and gender.
     *
     * @param userType The user type (FAN or VENUE_MANAGER)
     * @param gender   The user's gender ("Male" or "Female")
     * @return The full resource path to the avatar image
     */
    public static String getAvatarPath(UserType userType, String gender) {
        String filename = resolveAvatarFilename(userType, gender);
        return IMAGES_BASE_PATH + filename;
    }

    /**
     * Resolves the avatar filename based on user type and gender.
     *
     * @param userType The user type
     * @param gender   The gender
     * @return The appropriate avatar filename
     */
    private static String resolveAvatarFilename(UserType userType, String gender) {
        if (userType == null || gender == null || gender.equalsIgnoreCase("Other")) {
            return USER_DEFAULT_AVATAR;
        }

        boolean isFemale = FEMALE.equalsIgnoreCase(gender);

        return switch (userType) {
            case FAN -> isFemale ? FAN_FEMALE : FAN_MALE;
            case VENUE_MANAGER -> isFemale ? MANAGER_FEMALE : MANAGER_MALE;
        };
    }
}