package it.uniroma2.hoophub;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.sync.InitialSyncManager;
import it.uniroma2.hoophub.utilities.FontLoader;
import it.uniroma2.hoophub.utilities.NavigatorSingleton;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Main class serves as the entry point for the MindHarbor application.
 * <p>
 * This class extends {@link javafx.application.Application} to create a JavaFX application
 * that provides a user interface for managing psychological appointments and patient-psychologist
 * interactions. It handles application initialization, persistence type configuration,
 * and proper resource cleanup on shutdown.
 * </p>
 * <p>
 * The application supports multiple persistence types (MySQL and CSV) with automatic
 * fallback mechanisms and can be configured via command-line arguments.
 * </p>
 */
public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Starts the primary stage of the application and sets the initial scene.
     * <p>
     * This method initializes the {@link NavigatorSingleton} and navigates to the
     * start screen of the application, which serves as the entry point for user interaction.
     * </p>
     *
     * @param primaryStage The primary stage for this application, onto which
     *                     the application scene can be set. The primary stage is
     *                     configured and displayed by this method.
     * @throws IOException If there is an error loading the FXML file for the start screen.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load custom fonts before loading FXML
        FontLoader.loadFonts();
        NavigatorSingleton navigator = NavigatorSingleton.getInstance(primaryStage);
        navigator.gotoPage("/it/uniroma2/mindharbor/fxml/Login.fxml");
    }

    /**
     * Called when the application is stopping.
     * <p>
     * This method ensures proper cleanup of resources, including closing the database
     * connection if MySQL persistence is being used. It performs graceful shutdown
     * to prevent resource leaks.
     * </p>
     */
    @Override
    public void stop() throws Exception {
        if (DaoFactoryFacade.getInstance().getPersistenceType() == PersistenceType.MYSQL) {
            try {
                ConnectionFactory.closeConnection();
                logger.info("Database connection closed successfully");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error closing database connection", e);
            }
        }

        logger.info("Application shutdown complete");
        super.stop();
    }

    /**
     * Main method to configure and launch the MindHarbor application.
     * <p>
     * This method processes command-line arguments to determine persistence and interface types,
     * configures the DAO factory, tests database connectivity with automatic fallback to CSV
     * if MySQL is unavailable, performs initial data synchronization, and launches the
     * appropriate user interface.
     * </p>
     * <p>
     * <strong>Persistence Logic:</strong>
     * <ul>
     * <li>If MySQL is specified, tests the database connection</li>
     * <li>If the connection fails, automatically falls back to CSV persistence</li>
     * <li>Performs initial sync between persistence types if needed</li>
     * </ul>
     * </p>
     *
     * @param args Command-line arguments to configure the application:
     *             <ul>
     *             <li><strong>args[0]</strong> (optional): Persistence type.
     *                 Values: "mysql" or "csv". Default: "mysql"</li>
     *             <li><strong>args[1]</strong> (optional): Interface type.
     *                 Values: "gui" or "cli". Default: "gui".
     *                 Note: CLI interface is not yet implemented</li>
     *             </ul>
     */
    public static void main(String[] args) {
        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();

        String persistenceType = args.length > 0 ? args[0].toLowerCase() : "mysql";
        String interfaceType = args.length > 1 ? args[1].toLowerCase() : "gui";
        PersistenceType primaryPersistenceType;

        logger.log(Level.INFO,
                "Starting HoopHub with persistence: {0}, interface: {1}",
                new Object[]{persistenceType, interfaceType});

        if ("mysql".equals(persistenceType)) {
            daoFactoryFacade.setPersistenceType(PersistenceType.MYSQL);
            try {
                boolean connectionOk = ConnectionFactory.testConnection();
                if (!connectionOk) {
                    logger.log(Level.WARNING, "Database connection test failed. Switching to CSV persistence.");
                    primaryPersistenceType = PersistenceType.CSV;
                } else {
                    logger.log(Level.INFO, "Database connection test successful");
                    primaryPersistenceType = PersistenceType.MYSQL;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error testing database connection", e);
                logger.info("Switching to CSV persistence due to connection error");
                primaryPersistenceType = PersistenceType.CSV;
            }
        } else {
            logger.log(Level.INFO, "Using CSV persistence as specified");
            primaryPersistenceType = PersistenceType.CSV;
        }

        InitialSyncManager initialSyncManager = new InitialSyncManager();
        initialSyncManager.performInitialSync(primaryPersistenceType);

        daoFactoryFacade.setPersistenceType(primaryPersistenceType);

        if ("gui".equals(interfaceType)) {
            logger.log(Level.INFO, "Launching GUI interface");
            launch(args);
        } else {
            logger.info("Command-line interface requested, but not yet implemented");
            // @TODO Placeholder for CLI logic
        }
    }
}