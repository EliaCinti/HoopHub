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
 * Entry point for the JavaFX GUI application.
 *
 * <p>Extends {@link Application} to integrate with JavaFX lifecycle.
 * Handles font loading, navigator initialization, and resource cleanup.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CliApplication
 */
public class GuiApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(GuiApplication.class.getName());
    private static final String INITIAL_FXML = "/it/uniroma2/hoophub/fxml/login.fxml";

    /**
     * Starts the JavaFX application.
     *
     * <p>Loads custom fonts, initializes the navigator singleton,
     * and displays the login screen.</p>
     *
     * @param primaryStage the primary stage provided by JavaFX
     * @throws IOException if FXML loading fails
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        FontLoader.loadFonts();

        NavigatorSingleton navigator = NavigatorSingleton.getInstance(primaryStage);
        navigator.gotoPage(INITIAL_FXML);
    }

    /**
     * Called by JavaFX when the application is stopping.
     *
     * @throws Exception if cleanup fails
     */
    @Override
    public void stop() throws Exception {
        cleanup();
        super.stop();
    }

    /**
     * Performs cleanup: closes database connection if MySQL.
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