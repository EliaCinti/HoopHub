package it.uniroma2.hoophub.launcher;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.graphic_controller.cli.CliGraphicController;
import it.uniroma2.hoophub.graphic_controller.cli.CliLoginGraphicController;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the CLI application.
 *
 * <p>Equivalent to {@link GuiApplication} for JavaFX. Handles only initialization
 * and cleanup; presentation logic belongs to graphic controllers.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see GuiApplication
 */
public class CliApplication {

    private static final Logger LOGGER = Logger.getLogger(CliApplication.class.getName());

    /**
     * Starts the CLI application.
     *
     * <p>Launches the login controller and ensures cleanup on exit.</p>
     */
    public void start() {
        try {
            CliLoginGraphicController loginController = new CliLoginGraphicController();
            loginController.execute();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in CLI application", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Performs cleanup: closes scanner and database connection if MySQL.
     */
    private void cleanup() {
        CliGraphicController.closeScanner();

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