package it.uniroma2.hoophub.graphic_controller.cli;

import java.util.logging.Logger;

/**
 * CLI graphic controller for the Fan dashboard.
 *
 * <p>Extends {@link AbstractCliHomepageController} implementing the
 * Template Method pattern with Fan-specific menu options.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliFanHomepageGraphicController extends AbstractCliHomepageController {

    private static final Logger LOGGER = Logger.getLogger(CliFanHomepageGraphicController.class.getName());

    private static final String OPTION_BOOK_SEAT = "Book Game Seat";
    private static final String OPTION_MANAGE_SEATS = "Manage My Seats";
    private static final String OPTION_LOGOUT = "Logout";

    private static final String TITLE = "HOOPHUB - FAN DASHBOARD";

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected String[] getMenuOptions() {
        return new String[]{
                OPTION_BOOK_SEAT,
                OPTION_MANAGE_SEATS,
                OPTION_LOGOUT
        };
    }

    @Override
    protected void processMenuOption(int option) {
        switch (option) {
            case 1 -> onBookSeatSelected();
            case 2 -> onManageSeatsSelected();
            case 3 -> performLogout();
            default -> {
                printWarning(INVALID_OPTION_MSG);
                pauseBeforeContinue();
            }
        }
    }

    private void onBookSeatSelected() {
        LOGGER.info("Fan selected: Book Game Seat");
        CliBookGameSeatGraphicController bookController = new CliBookGameSeatGraphicController();
        bookController.execute();
    }

    private void onManageSeatsSelected() {
        LOGGER.info("Fan selected: Manage My Seats");
        CliManageSeatsGraphicController manageSeatsController = new CliManageSeatsGraphicController();
        manageSeatsController.execute();
    }
}