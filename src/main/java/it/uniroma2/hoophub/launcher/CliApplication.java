package it.uniroma2.hoophub.launcher;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.graphic_controller.cli.CliGraphicController;
import it.uniroma2.hoophub.graphic_controller.cli.CliMainMenuGraphicController;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the CLI application.
 * Equivalent to JavaFX Application class - handles ONLY initialization and cleanup.
 * It Does NOT contain presentation logic (that belongs to graphic controllers).
 */
public class CliApplication {

    private static final Logger LOGGER = Logger.getLogger(CliApplication.class.getName());

    /**
     * Starts the CLI application.
     * Initializes resources, launches the first graphic controller, and handles cleanup.
     */
    public void start() {
        try {
            // Launch first graphic controller (main menu)
            CliMainMenuGraphicController mainMenuController = new CliMainMenuGraphicController();
            mainMenuController.execute();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in CLI application", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Performs cleanup operations.
     * Closes resources and database connections.
     */
    private void cleanup() {
        // Close CLI scanner
        CliGraphicController.closeScanner();

        // Close database connection if using MySQL
        if (DaoFactoryFacade.getInstance().getPersistenceType() == PersistenceType.MYSQL) {
            try {
                ConnectionFactory.closeConnection();
                LOGGER.info("Database connection closed successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection", e);
            }
        }

        LOGGER.info("CLI application shutdown complete");
    }
}