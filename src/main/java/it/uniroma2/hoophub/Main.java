package it.uniroma2.hoophub;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.sync.InitialSyncManager;
import it.uniroma2.hoophub.launcher.CliApplication;
import it.uniroma2.hoophub.launcher.GuiApplication;
import javafx.application.Application;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the HoopHub application.
 * Orchestrates application startup by:
 * - Configuring persistence type (MySQL/CSV)
 * - Testing database connectivity with automatic fallback
 * - Performing initial data synchronization
 * - Launching the appropriate interface (GUI/CLI)
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // Default configuration
    private static final String DEFAULT_PERSISTENCE = "mysql";
    private static final String DEFAULT_INTERFACE = "gui";

    // Configuration values
    private static final String PERSISTENCE_MYSQL = "mysql";
    private static final String PERSISTENCE_CSV = "csv";
    private static final String INTERFACE_GUI = "gui";
    private static final String INTERFACE_CLI = "cli";

    /**
     * Private constructor to prevent instantiation.
     */
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Main method to configure and launch the HoopHub application.
     *
     * @param args Command-line arguments:
     *             args[0] (optional): Persistence type - "mysql" or "csv" (default: "mysql")
     *             args[1] (optional): Interface type - "gui" or "cli" (default: "gui")
     */
    public static void main(String[] args) {
        // Parse command-line arguments
        String persistenceType = args.length > 0 ? args[0].toLowerCase() : DEFAULT_PERSISTENCE;
        String interfaceType = args.length > 1 ? args[1].toLowerCase() : DEFAULT_INTERFACE;

        LOGGER.log(Level.INFO,
                "Starting HoopHub with persistence: {0}, interface: {1}",
                new Object[]{persistenceType, interfaceType});

        // Configure persistence
        PersistenceType primaryPersistenceType = configurePersistence(persistenceType);

        // Perform initial synchronization
        performInitialSync(primaryPersistenceType);

        // Launch appropriate interface
        launchInterface(interfaceType, args);
    }

    /**
     * Configures and tests the persistence type.
     * Automatically falls back to CSV if MySQL connection fails.
     *
     * @param persistenceType The requested persistence type
     * @return The actual persistence type to use (may differ due to fallback)
     */
    private static PersistenceType configurePersistence(String persistenceType) {
        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();

        if (PERSISTENCE_MYSQL.equals(persistenceType)) {
            daoFactoryFacade.setPersistenceType(PersistenceType.MYSQL);

            try {
                boolean connectionOk = ConnectionFactory.testConnection();

                if (!connectionOk) {
                    LOGGER.warning("Database connection test failed. Switching to CSV persistence.");
                    return PersistenceType.CSV;
                }

                LOGGER.info("Database connection test successful");
                return PersistenceType.MYSQL;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error testing database connection", e);
                LOGGER.info("Switching to CSV persistence due to connection error");
                return PersistenceType.CSV;
            }
        } else if (PERSISTENCE_CSV.equals(persistenceType)) {
            LOGGER.info("Using CSV persistence as specified");
            return PersistenceType.CSV;
        } else {
            LOGGER.warning("Unknown persistence type: " + persistenceType + ". Defaulting to MySql.");
            return PersistenceType.MYSQL;
        }
    }

    /**
     * Performs initial data synchronization between persistence types.
     *
     * @param primaryPersistenceType The primary persistence type to sync to
     */
    private static void performInitialSync(PersistenceType primaryPersistenceType) {
        InitialSyncManager initialSyncManager = new InitialSyncManager();
        initialSyncManager.performInitialSync(primaryPersistenceType);

        DaoFactoryFacade.getInstance().setPersistenceType(primaryPersistenceType);
    }

    /**
     * Launches the appropriate user interface (GUI or CLI).
     *
     * @param interfaceType The interface type to launch
     * @param args Original command-line arguments (needed for JavaFX launch)
     */
    private static void launchInterface(String interfaceType, String[] args) {
        if (INTERFACE_GUI.equals(interfaceType)) {
            LOGGER.info("Launching GUI interface");
            Application.launch(GuiApplication.class, args);
        } else if (INTERFACE_CLI.equals(interfaceType)) {
            LOGGER.info("Launching CLI interface");
            new CliApplication().start();
        } else {
            LOGGER.warning("Unknown interface type: " + interfaceType + ". Defaulting to GUI.");
            Application.launch(GuiApplication.class, args);
        }
    }
}
