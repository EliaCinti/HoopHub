package it.uniroma2.hoophub.launcher;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.utilities.FontLoader;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the GUI (JavaFX) application.
 * Handles JavaFX initialization, scene setup, and cleanup.
 * Parallel to CliApplication for CLI.
 */
public class GuiApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(GuiApplication.class.getName());
    private static final String INITIAL_FXML = "/it/uniroma2/hoophub/fxml/Login.fxml"; // Da cambiare con il FXML del MainMenu

    /**
     * Starts the JavaFX application.
     * Loads fonts, initializes navigator, and displays the first scene.
     *
     * @param primaryStage The primary stage for this application
     * @throws IOException If there is an error loading the FXML file
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load custom fonts before loading FXML
        FontLoader.loadFonts();

        // Initialize navigator and navigate to the first screen
        NavigatorSingleton navigator = NavigatorSingleton.getInstance(primaryStage);
        navigator.gotoPage(INITIAL_FXML);
    }

    /**
     * Called when the application is stopping.
     * Ensures proper cleanup of resources, including database connections.
     */
    @Override
    public void stop() throws Exception {
        cleanup();
        super.stop();
    }

    /**
     * Performs cleanup operations.
     * Closes database connection if using MySQL.
     */
    private void cleanup() {
        if (DaoFactoryFacade.getInstance().getPersistenceType() == PersistenceType.MYSQL) {
            try {
                ConnectionFactory.closeConnection();
                LOGGER.info("Database connection closed successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection", e);
            }
        }

        LOGGER.info("GUI application shutdown complete");
    }
}
