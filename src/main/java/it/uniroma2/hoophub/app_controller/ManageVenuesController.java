package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.util.List;

/**
 * Application controller for the "Manage Venues" use case.
 * <p>
 * Handles CRUD operations for venues, ensuring proper business rules
 * and ownership verification. Used by both GUI and CLI graphic controllers.
 * </p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ManageVenuesController {

    private final VenueDao venueDao;
    private final VenueManagerDao venueManagerDao;

    /**
     * Default constructor using DaoFactoryFacade.
     */
    public ManageVenuesController() {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.venueDao = daoFactory.getVenueDao();
        this.venueManagerDao = daoFactory.getVenueManagerDao();
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Retrieves all venues managed by the currently logged-in venue manager.
     *
     * @return List of VenueBean representing the manager's venues
     * @throws UserSessionException if no user is logged in or user is not a venue manager
     * @throws DAOException         if database access fails
     */
    public List<VenueBean> getMyVenues() throws UserSessionException, DAOException {
        UserBean currentUser = getCurrentVenueManager();

        List<Venue> venues = venueDao.retrieveVenuesByManager(currentUser.getUsername());

        return venues.stream()
                .map(this::convertToBean)
                .toList();
    }

    /**
     * Retrieves a single venue by ID.
     * Verifies that the current user owns the venue.
     *
     * @param venueId The venue ID
     * @return The VenueBean
     * @throws UserSessionException if no user is logged in, not a venue manager, or doesn't own the venue
     * @throws DAOException         if venue not found or database access fails
     */
    public VenueBean getVenueById(int venueId) throws UserSessionException, DAOException {
        Venue venue = getOwnedVenue(venueId);
        return convertToBean(venue);
    }

    // ==================== CREATE OPERATION ====================

    /**
     * Creates a new venue for the currently logged-in venue manager.
     *
     * @param venueBean The venue data to create
     * @throws UserSessionException     if no user is logged in or user is not a venue manager
     * @throws DAOException             if database access fails
     * @throws IllegalArgumentException if venueBean contains invalid data
     */
    public void createVenue(VenueBean venueBean) throws UserSessionException, DAOException {
        UserBean currentUser = getCurrentVenueManager();

        // Retrieve the VenueManager model
        VenueManager manager = venueManagerDao.retrieveVenueManager(currentUser.getUsername());
        if (manager == null) {
            throw new DAOException("Venue manager not found: " + currentUser.getUsername());
        }

        // Get next available ID
        int newId = venueDao.getNextVenueId();

        // Build the Venue model with teams
        Venue.Builder venueBuilder = new Venue.Builder()
                .id(newId)
                .name(venueBean.getName())
                .type(venueBean.getType())
                .address(venueBean.getAddress())
                .city(venueBean.getCity())
                .maxCapacity(venueBean.getMaxCapacity())
                .venueManager(manager);

        // Add associated teams to the model
        for (TeamNBA team : venueBean.getAssociatedTeams()) {
            venueBuilder.addTeam(team);
        }

        Venue venue = venueBuilder.build();

        // Save venue to database (saveVenue already persists team associations)
        Venue savedVenue = venueDao.saveVenue(venue);

        // Add venue to manager's list
        manager.addVenue(savedVenue);
    }

    // ==================== UPDATE OPERATION ====================

    /**
     * Updates an existing venue.
     *
     * @param venueBean The updated venue data (must include valid ID)
     * @throws UserSessionException     if no user is logged in or user doesn't own the venue
     * @throws DAOException             if database access fails or venue not found
     * @throws IllegalArgumentException if venueBean contains invalid data
     */
    public void updateVenue(VenueBean venueBean) throws UserSessionException, DAOException {
        Venue existingVenue = getOwnedVenue(venueBean.getId());

        // Update venue details using the model's business method
        existingVenue.updateVenueDetails(
                venueBean.getName(),
                venueBean.getType(),
                venueBean.getAddress(),
                venueBean.getCity(),
                venueBean.getMaxCapacity(),
                venueBean.getAssociatedTeams()
        );

        // Persist venue changes
        venueDao.updateVenue(existingVenue);

        // Update team associations: remove all existing and re-add
        venueDao.deleteAllVenueTeams(existingVenue);
        for (TeamNBA team : venueBean.getAssociatedTeams()) {
            venueDao.saveVenueTeam(existingVenue, team);
        }
    }

    // ==================== DELETE OPERATION ====================

    /**
     * Deletes a venue.
     *
     * @param venueId The ID of the venue to delete
     * @throws UserSessionException if no user is logged in or user doesn't own the venue
     * @throws DAOException         if database access fails or venue not found
     */
    public void deleteVenue(int venueId) throws UserSessionException, DAOException {
        Venue existingVenue = getOwnedVenue(venueId);

        // Check for existing bookings (business rule)
        if (!existingVenue.getAllBookings().isEmpty()) {
            throw new DAOException("Cannot delete venue with existing bookings. Please cancel all bookings first.");
        }

        // Delete team associations first (foreign key constraint)
        venueDao.deleteAllVenueTeams(existingVenue);

        // Delete the venue
        venueDao.deleteVenue(existingVenue);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Gets the current logged-in user and verifies they are a venue manager.
     *
     * @return The current user as UserBean
     * @throws UserSessionException if no user is logged in or user is not a venue manager
     */
    private UserBean getCurrentVenueManager() throws UserSessionException {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();

        if (currentUser == null) {
            throw new UserSessionException("No user logged in");
        }

        if (currentUser.getType() != UserType.VENUE_MANAGER) {
            throw new UserSessionException("Current user is not a venue manager");
        }

        return currentUser;
    }

    /**
     * Retrieves a venue and verifies that the current user owns it.
     *
     * @param venueId The venue ID to retrieve
     * @return The Venue model
     * @throws UserSessionException if no user is logged in, not a venue manager, or doesn't own the venue
     * @throws DAOException         if venue not found or database access fails
     */
    private Venue getOwnedVenue(int venueId) throws UserSessionException, DAOException {
        UserBean currentUser = getCurrentVenueManager();

        Venue venue = venueDao.retrieveVenue(venueId);
        if (venue == null) {
            throw new DAOException("Venue not found: " + venueId);
        }

        if (!venue.getVenueManagerUsername().equals(currentUser.getUsername())) {
            throw new UserSessionException("You do not own this venue");
        }

        return venue;
    }

    /**
     * Converts a Venue model to a VenueBean.
     *
     * @param venue The Venue model
     * @return The corresponding VenueBean
     */
    private VenueBean convertToBean(Venue venue) {
        return new VenueBean.Builder()
                .id(venue.getId())
                .name(venue.getName())
                .type(venue.getType())
                .address(venue.getAddress())
                .city(venue.getCity())
                .maxCapacity(venue.getMaxCapacity())
                .venueManagerUsername(venue.getVenueManagerUsername())
                .associatedTeams(venue.getAssociatedTeams())
                .build();
    }
}
