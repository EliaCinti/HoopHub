package it.uniroma2.hoophub.utilities;

import javafx.scene.text.Font;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FontLoader {

    private static final Logger logger = Logger.getLogger(FontLoader.class.getName());
    private static boolean fontsLoaded = false;

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private FontLoader() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Loads custom fonts for the application.
     * This method is idempotent - it will only load fonts once.
     */
    public static void loadFonts() {
        if (fontsLoaded) {
            logger.fine("Fonts already loaded, skipping reload");
            return;
        }

        try {
            Font promptBold = Font.loadFont(
                    FontLoader.class.getResourceAsStream("/it/uniroma2/hoophub/fonts/Prompt-Bold.ttf"),
                    14
            );

            Font interMedium = Font.loadFont(
                    FontLoader.class.getResourceAsStream("/it/uniroma2/hoophub/fonts/Inter-Medium.ttf"),
                    14
            );

            Font interSemiBold = Font.loadFont(
                    FontLoader.class.getResourceAsStream("/it/uniroma2/hoophub/fonts/Inter-SemiBold.ttf"),
                    14
            );

            if (promptBold != null && interMedium != null && interSemiBold != null) {
                fontsLoaded = true;
                logger.info("Custom fonts loaded successfully");
                logger.fine("Prompt Bold: " + promptBold.getName());
                logger.fine("Inter Medium: " + interMedium.getName());
                logger.fine("Inter SemiBold: " + interSemiBold.getName());
            } else {
                logger.warning("Some fonts could not be loaded, fallback fonts will be used");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading custom fonts", e);
            logger.info("System fallback fonts will be used");
        }
    }
}