package it.uniroma2.hoophub.graphic_controller.cli;

import it.uniroma2.hoophub.beans.VenueManagerBean;

import java.util.logging.Logger;

/**
 * CLI graphic controller for the VenueManager dashboard.
 *
 * <p>Extends {@link AbstractCliHomepageController} implementing the
 * Template Method pattern with VenueManager-specific menu options.
 * Overrides {@link #displayAdditionalInfo()} hook to show company name.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CliVenueManagerHomepageGraphicController extends AbstractCliHomepageController {

    private static final Logger LOGGER = Logger.getLogger(CliVenueManagerHomepageGraphicController.class.getName());

    private static final String OPTION_MANAGE_VENUES = "Manage Venues";
    private static final String OPTION_VIEW_BOOKINGS = "View Bookings";
    private static final String OPTION_NOTIFICATIONS = "Notifications";
    private static final String OPTION_LOGOUT = "Logout";

    private static final String TITLE = "HOOPHUB - VENUE MANAGER DASHBOARD";
    private static final String COMPANY_INFO_MSG = "Company: %s";

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected String[] getMenuOptions() {
        return new String[]{
                OPTION_MANAGE_VENUES,
                OPTION_VIEW_BOOKINGS,
                OPTION_NOTIFICATIONS,
                OPTION_LOGOUT
        };
    }

    /**
     * Hook: displays company name for VenueManagers.
     */
    @Override
    protected void displayAdditionalInfo() {
        if (currentUser instanceof VenueManagerBean vmBean && vmBean.getCompanyName() != null) {
            printInfo(String.format(COMPANY_INFO_MSG, vmBean.getCompanyName()));
        }
    }

    @Override
    protected void processMenuOption(int option) {
        switch (option) {
            case 1 -> onManageVenuesSelected();
            case 2 -> onViewBookingsSelected();
            case 3 -> onNotificationsSelected();
            case 4 -> performLogout();
            default -> {
                printWarning(INVALID_OPTION_MSG);
                pauseBeforeContinue();
            }
        }
    }

    private void onManageVenuesSelected() {
        CliManageVenuesGraphicController manageVenuesController = new CliManageVenuesGraphicController();
        manageVenuesController.execute();
    }

    private void onViewBookingsSelected() {
        LOGGER.info("Venue Manager selected: View Bookings");
        showNotImplemented();
    }

    private void onNotificationsSelected() {
        LOGGER.info("Venue Manager selected: Notifications");
        showNotImplemented();
    }
}