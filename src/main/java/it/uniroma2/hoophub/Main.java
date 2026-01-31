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
 *
 * <p>This class serves as the application bootstrap, orchestrating the entire startup sequence
 * through the following phases:</p>
 * <ol>
 *     <li><b>Configuration parsing:</b> Reads command-line arguments to determine persistence
 *         and interface types</li>
 *     <li><b>Persistence setup:</b> Configures the data access layer with automatic fallback
 *         mechanism (MySQL → CSV) in case of connection failures</li>
 *     <li><b>Data synchronization:</b> Performs initial sync between persistence layers to
 *         ensure data consistency (skipped for IN_MEMORY)</li>
 *     <li><b>Interface launch:</b> Starts either the JavaFX GUI or CLI interface based on
 *         user preference</li>
 * </ol>
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
 * <p><b>Design considerations:</b></p>
 * <ul>
 *     <li>Implements a <i>fail-safe</i> approach: if MySQL connection fails, the application
 *         gracefully degrades to CSV persistence</li>
 *     <li>IN_MEMORY mode requires no external dependencies (no DB, no files)</li>
 *     <li>Designed as a utility class with private constructor to prevent instantiation</li>
 *     <li>Uses {@link java.util.logging.Logger} for consistent application logging</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.1
 * @see DaoFactoryFacade
 * @see InitialSyncManager
 * @see GuiApplication
 * @see CliApplication
 */
public class Main {

    /** Logger instance for this class. */
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /** Default persistence type used when no argument is provided. */
    private static final String DEFAULT_PERSISTENCE = "mysql";

    /** Default interface type used when no argument is provided. */
    private static final String DEFAULT_INTERFACE = "gui";

    /** Configuration constant for MySQL persistence type. */
    private static final String PERSISTENCE_MYSQL = "mysql";

    /** Configuration constant for CSV persistence type. */
    private static final String PERSISTENCE_CSV = "csv";

    /** Configuration constant for IN_MEMORY persistence type. */
    private static final String PERSISTENCE_INMEMORY = "inmemory";

    /** Configuration constant for GUI interface type. */
    private static final String INTERFACE_GUI = "gui";

    /** Configuration constant for CLI interface type. */
    private static final String INTERFACE_CLI = "cli";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class follows the utility class pattern and should not be instantiated.
     * All functionality is provided through the static {@link #main(String[])} method.</p>
     *
     * @throws UnsupportedOperationException always, as this class cannot be instantiated
     */
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Main method and entry point for the HoopHub application.
     *
     * <p>Parses command-line arguments, configures the persistence layer with automatic
     * fallback support, performs initial data synchronization (if applicable), and launches the
     * appropriate user interface.</p>
     *
     * <p><b>Execution flow:</b></p>
     * <ol>
     *     <li>Parse and validate command-line arguments (with defaults)</li>
     *     <li>Configure persistence type via {@link #configurePersistence(String)}</li>
     *     <li>Synchronize data via {@link #performInitialSync(PersistenceType)} (skipped for IN_MEMORY)</li>
     *     <li>Launch interface via {@link #launchInterface(String, String[])}</li>
     * </ol>
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

        // Configure persistence
        PersistenceType primaryPersistenceType = configurePersistence(persistenceType);

        // Perform initial synchronization (only for MYSQL and CSV)
        performInitialSync(primaryPersistenceType);

        // Launch appropriate interface
        launchInterface(interfaceType, args);
    }

    /**
     * Configures and validates the persistence layer based on the requested type.
     *
     * <p>This method implements a <b>fail-safe mechanism</b>: when MySQL persistence is
     * requested, it first tests the database connection. If the connection test fails
     * (either returning {@code false} or throwing an exception), the method automatically
     * falls back to CSV persistence to ensure application availability.</p>
     *
     * <p><b>Behavior by persistence type:</b></p>
     * <ul>
     *     <li><b>mysql:</b> Tests connection via {@link ConnectionFactory#testConnection()}.
     *         Falls back to CSV on failure.</li>
     *     <li><b>csv:</b> Directly returns CSV persistence type without additional checks.</li>
     *     <li><b>inmemory:</b> Returns IN_MEMORY persistence type (no external dependencies).</li>
     *     <li><b>unknown:</b> Logs a warning and defaults to MySQL (with connection test).</li>
     * </ul>
     *
     * @param persistenceType the requested persistence type as a lowercase string
     *                        ({@code "mysql"}, {@code "csv"}, or {@code "inmemory"})
     * @return the actual {@link PersistenceType} to be used, which may differ from the
     *         requested type if MySQL connection fails
     * @see ConnectionFactory#testConnection()
     * @see DaoFactoryFacade#setPersistenceType(PersistenceType)
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
                // Ricorsione per gestire il default come se fosse MySQL
                return configurePersistence(PERSISTENCE_MYSQL);
        }
    }

    /**
     * Performs initial data synchronization between persistence layers.
     *
     * <p>This method ensures data consistency by synchronizing records from the secondary
     * persistence layer to the primary one. After synchronization, it updates the
     * {@link DaoFactoryFacade} to use the specified primary persistence type for all
     * subsequent data access operations.</p>
     *
     * <p><b>Note:</b> Synchronization is skipped for IN_MEMORY persistence type,
     * as it operates in standalone mode without external data sources.</p>
     *
     * @param primaryPersistenceType the primary {@link PersistenceType} that will be
     *                               used as the target for synchronization and for
     *                               all subsequent DAO operations
     * @see InitialSyncManager#performInitialSync(PersistenceType)
     */
    private static void performInitialSync(PersistenceType primaryPersistenceType) {
        // Skip sync for IN_MEMORY - it starts fresh with no data
        if (primaryPersistenceType == PersistenceType.IN_MEMORY) {
            LOGGER.info("Skipping initial sync for IN_MEMORY persistence (standalone mode)");
            DaoFactoryFacade.getInstance().setPersistenceType(primaryPersistenceType);
            return;
        }

        InitialSyncManager initialSyncManager = new InitialSyncManager();
        initialSyncManager.performInitialSync(primaryPersistenceType);

        DaoFactoryFacade.getInstance().setPersistenceType(primaryPersistenceType);
    }

    /**
     * Launches the appropriate user interface based on the specified type.
     *
     * <p>Supports two interface modes:</p>
     * <ul>
     *     <li><b>GUI ({@code "gui"}):</b> Launches the JavaFX-based graphical interface
     *         via {@link Application#launch(Class, String...)} with {@link GuiApplication}</li>
     *     <li><b>CLI ({@code "cli"}):</b> Starts the command-line interface via
     *         {@link CliApplication#start()}</li>
     * </ul>
     *
     * <p>If an unrecognized interface type is provided, the method logs a warning and
     * defaults to the GUI interface.</p>
     *
     * <p><b>Note:</b> When launching the GUI, the original command-line arguments are
     * passed to JavaFX as required by the {@link Application#launch(Class, String...)}
     * method signature.</p>
     *
     * @param interfaceType the interface type to launch ({@code "gui"} or {@code "cli"})
     * @param args          the original command-line arguments, passed to JavaFX when
     *                      launching the GUI
     * @see GuiApplication
     * @see CliApplication
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