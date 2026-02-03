package it.uniroma2.hoophub;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.sync.CrossPersistenceSyncObserver;
import it.uniroma2.hoophub.sync.InitialSyncManager;
import it.uniroma2.hoophub.launcher.CliApplication;
import it.uniroma2.hoophub.launcher.GuiApplication;
import javafx.application.Application;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the HoopHub application.
 *
 * <p>This class serves as the application bootstrap, orchestrating the entire startup sequence
 * through the following phases:</p>
 * <ol>
 *     <li><b>Configuration parsing:</b> Reads command-line arguments to determine persistence
 *         and interface types</li>
 *     <li><b>Persistence setup:</b> Configures the data access layer with automatic fallback
 *         mechanism (MySQL → CSV) in case of connection failures</li>
 *     <li><b>Data synchronization:</b> Performs initial sync (MySQL → CSV) to ensure
 *         data consistency.
 *         MySQL is always the Master.</li>
 *     <li><b>Real-time sync:</b> Registers observers for bidirectional sync during runtime</li>
 *     <li><b>Interface launch:</b> Starts either the JavaFX GUI or CLI interface</li>
 * </ol>
 *
 * <h3>Synchronization Strategy</h3>
 * <p>HoopHub uses a <b>Master-Slave</b> approach:</p>
 * <ul>
 *     <li><b>MySQL</b> is always the Master (source of truth)</li>
 *     <li><b>CSV</b> is the Slave (local cache/backup)</li>
 *     <li>At startup: MySQL → CSV (wipe and repopulate)</li>
 *     <li>During runtime: Bidirectional sync with UPSERT to handle conflicts</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * java it.uniroma2.hoophub.Main [persistence] [interface]
 *
 * Examples:
 *   java Main                  # MySQL + GUI (defaults)
 *   java Main mysql gui        # MySQL + GUI
 *   java Main csv cli          # CSV + CLI
 *   java Main mysql cli        # MySQL + CLI
 *   java Main inmemory gui     # IN_MEMORY + GUI (demo mode)
 *   java Main inmemory cli     # IN_MEMORY + CLI (demo mode)
 * }</pre>
 *
 * @author Elia Cinti
 * @version 1.2
 * @see DaoFactoryFacade
 * @see InitialSyncManager
 * @see CrossPersistenceSyncObserver
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String DEFAULT_PERSISTENCE = "mysql";
    private static final String DEFAULT_INTERFACE = "gui";

    private static final String PERSISTENCE_MYSQL = "mysql";
    private static final String PERSISTENCE_CSV = "csv";
    private static final String PERSISTENCE_INMEMORY = "inmemory";

    private static final String INTERFACE_GUI = "gui";
    private static final String INTERFACE_CLI = "cli";

    /**
     * Private constructor to prevent instantiation.
     */
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Main method and entry point for the HoopHub application.
     *
     * @param args command-line arguments where:
     *             <ul>
     *                 <li>{@code args[0]} (optional): Persistence type - {@code "mysql"}, {@code "csv"},
     *                     or {@code "inmemory"}. Defaults to {@code "mysql"}</li>
     *                 <li>{@code args[1]} (optional): Interface type - {@code "gui"} or
     *                     {@code "cli"}. Defaults to {@code "gui"}</li>
     *             </ul>
     */
    public static void main(String[] args) {
        // Parse command-line arguments
        String persistenceType = args.length > 0 ? args[0].toLowerCase() : DEFAULT_PERSISTENCE;
        String interfaceType = args.length > 1 ? args[1].toLowerCase() : DEFAULT_INTERFACE;

        LOGGER.log(Level.INFO,
                "Starting HoopHub with persistence: {0}, interface: {1}",
                new Object[]{persistenceType, interfaceType});

        // Step 1: Configure persistence (with fallback)
        PersistenceType primaryPersistenceType = configurePersistence(persistenceType);

        // Step 2: Perform initial synchronization (MySQL → CSV)
        performInitialSync(primaryPersistenceType);

        // Step 3: Start real-time synchronization observers
        startRealTimeSync(primaryPersistenceType);

        // Step 4: Launch appropriate interface
        launchInterface(interfaceType, args);
    }

    /**
     * Configures and validates the persistence layer based on the requested type.
     *
     * <p>Implements a <b>fail-safe mechanism</b>: when MySQL persistence is
     * requested, it first tests the database connection.
     * If the connection fails, it
     * automatically falls back to CSV persistence.</p>
     *
     * @param persistenceType the requested persistence type as a lowercase string
     * @return the actual {@link PersistenceType} to be used
     */
    private static PersistenceType configurePersistence(String persistenceType) {
        DaoFactoryFacade daoFactoryFacade = DaoFactoryFacade.getInstance();

        switch (persistenceType) {
            case PERSISTENCE_MYSQL:
                daoFactoryFacade.setPersistenceType(PersistenceType.MYSQL);
                try {
                    boolean connectionOk = ConnectionFactory.testConnection();

                    if (!connectionOk) {
                        LOGGER.warning("Database connection test failed. Switching to CSV persistence.");
                        daoFactoryFacade.setPersistenceType(PersistenceType.CSV);
                        return PersistenceType.CSV;
                    }

                    LOGGER.info("Database connection test successful");
                    return PersistenceType.MYSQL;

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error testing database connection", e);
                    LOGGER.info("Switching to CSV persistence due to connection error");
                    daoFactoryFacade.setPersistenceType(PersistenceType.CSV);
                    return PersistenceType.CSV;
                }

            case PERSISTENCE_CSV:
                LOGGER.info("Using CSV persistence as specified");
                daoFactoryFacade.setPersistenceType(PersistenceType.CSV);
                return PersistenceType.CSV;

            case PERSISTENCE_INMEMORY:
                LOGGER.info("Using IN_MEMORY persistence (demo mode - data will not persist)");
                daoFactoryFacade.setPersistenceType(PersistenceType.IN_MEMORY);
                return PersistenceType.IN_MEMORY;

            default:
                LOGGER.log(Level.WARNING,
                        () -> "Unknown persistence type: " + persistenceType + ". Defaulting to MySQL.");
                return configurePersistence(PERSISTENCE_MYSQL);
        }
    }

    /**
     * Performs initial data synchronization using Master-Slave strategy.
     *
     * <p><b>Strategy:</b> MySQL is always the Master.
     * At startup, CSV files are
     * wiped and repopulated from MySQL to ensure data consistency and ID alignment.</p>
     *
     * <p>Skipped for IN_MEMORY persistence (standalone demo mode).</p>
     *
     * @param primaryPersistenceType the primary persistence type selected by user
     */
    private static void performInitialSync(PersistenceType primaryPersistenceType) {
        InitialSyncManager initialSyncManager = new InitialSyncManager();
        initialSyncManager.performInitialSync(primaryPersistenceType);

        // Ensure we're using the selected persistence type after sync
        DaoFactoryFacade.getInstance().setPersistenceType(primaryPersistenceType);
    }

    /**
     * Registers real-time synchronization observers on all DAOs.
     *
     * <p>The {@link CrossPersistenceSyncObserver} implements the <b>Observer pattern</b>
     * to automatically propagate data changes from one persistence layer to another
     * during application runtime.</p>
     *
     * <p><b>Sync Direction:</b></p>
     * <ul>
     *     <li>If primary is MYSQL: Changes sync MySQL → CSV</li>
     *     <li>If primary is CSV: Changes sync CSV → MySQL</li>
     * </ul>
     *
     * <p><b>Note:</b> DAOs use UPSERT semantics to handle potential ID conflicts
     * gracefully (insert if new, update if exists).</p>
     *
     * @param currentType the current primary persistence type
     */
    private static void startRealTimeSync(PersistenceType currentType) {
        // No sync needed for IN_MEMORY (standalone mode)
        if (currentType == PersistenceType.IN_MEMORY) {
            LOGGER.info("IN_MEMORY mode: Real-time sync disabled (standalone demo mode)");
            return;
        }

        DaoFactoryFacade dao = DaoFactoryFacade.getInstance();

        // Create observer that syncs FROM current type TO the opposite type
        CrossPersistenceSyncObserver syncObserver = new CrossPersistenceSyncObserver(currentType);

        try {
            // Register observer on all DAOs
            registerObserver(dao.getUserDao(), syncObserver, "UserDao");
            registerObserver(dao.getFanDao(), syncObserver, "FanDao");
            registerObserver(dao.getVenueManagerDao(), syncObserver, "VenueManagerDao");
            registerObserver(dao.getVenueDao(), syncObserver, "VenueDao");
            registerObserver(dao.getBookingDao(), syncObserver, "BookingDao");
            registerObserver(dao.getNotificationDao(), syncObserver, "NotificationDao");

            String direction = currentType == PersistenceType.MYSQL ? "MySQL → CSV" : "CSV → MySQL";
            LOGGER.log(Level.INFO, ">>> Real-Time Sync ACTIVATED ({0})", direction);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not attach Sync Observer: {0}", e.getMessage());
        }
    }

    /**
     * Helper method to safely register an observer on a DAO.
     *
     * @param daoObject the DAO instance (might not be AbstractObservableDao)
     * @param observer  the observer to register
     * @param daoName   name for logging purposes
     */
    private static void registerObserver(Object daoObject, CrossPersistenceSyncObserver observer, String daoName) {
        if (daoObject instanceof AbstractObservableDao abstractObservableDao) {
            abstractObservableDao.addObserver(observer);
            LOGGER.log(Level.FINE, "Registered sync observer on {0}", daoName);
        } else {
            LOGGER.log(Level.WARNING, "{0} is not observable, sync not available", daoName);
        }
    }

    /**
     * Launches the appropriate user interface based on the specified type.
     *
     * @param interfaceType the interface type to launch ({@code "gui"} or {@code "cli"})
     * @param args          the original command-line arguments, passed to JavaFX
     */
    private static void launchInterface(String interfaceType, String[] args) {
        if (INTERFACE_GUI.equals(interfaceType)) {
            LOGGER.info("Launching GUI interface");
            Application.launch(GuiApplication.class, args);
        } else if (INTERFACE_CLI.equals(interfaceType)) {
            LOGGER.info("Launching CLI interface");
            new CliApplication().start();
        } else {
            LOGGER.log(Level.WARNING,
                    () -> "Unknown interface type: " + interfaceType + ". Defaulting to GUI.");
            Application.launch(GuiApplication.class, args);
        }
    }
}