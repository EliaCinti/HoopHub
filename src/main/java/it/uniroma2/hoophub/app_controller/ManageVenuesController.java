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
 *
 * <p>Provides CRUD operations for venues with ownership verification.
 * Ensures only venue managers can access their own venues.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see VenueBean
 */
public class ManageVenuesController {

    private final VenueDao venueDao;
    private final VenueManagerDao venueManagerDao;

    /**
     * Creates controller with DAOs from {@link DaoFactoryFacade}.
     */
    public ManageVenuesController() {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.venueDao = daoFactory.getVenueDao();
        this.venueManagerDao = daoFactory.getVenueManagerDao();
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Retrieves all venues owned by the current logged-in venue manager.
     *
     * @return list of {@link VenueBean} for the manager's venues
     * @throws UserSessionException if not logged in or not a venue manager
     * @throws DAOException if database access fails
     */
    public List<VenueBean> getMyVenues() throws UserSessionException, DAOException {
        UserBean currentUser = getCurrentVenueManager();

        List<Venue> venues = venueDao.retrieveVenuesByManager(currentUser.getUsername());

        return venues.stream()
                .map(this::convertToBean)
                .toList();
    }

    /**
     * Retrieves a venue by ID after verifying ownership.
     *
     * @param venueId the venue ID
     * @return the {@link VenueBean}
     * @throws UserSessionException if not logged in or doesn't own the venue
     * @throws DAOException if venue not found
     */
    public VenueBean getVenueById(int venueId) throws UserSessionException, DAOException {
        Venue venue = getOwnedVenue(venueId);
        return convertToBean(venue);
    }

    // ==================== CREATE OPERATION ====================

    /**
     * Creates a new venue for the current venue manager.
     *
     * @param venueBean venue data to create
     * @return created {@link VenueBean} with assigned ID
     * @throws UserSessionException if not logged in or not a venue manager
     * @throws DAOException if database access fails
     */
    public VenueBean createVenue(VenueBean venueBean) throws UserSessionException, DAOException {
        UserBean currentUser = getCurrentVenueManager();

        VenueManager manager = venueManagerDao.retrieveVenueManager(currentUser.getUsername());
        if (manager == null) {
            throw new DAOException("Venue manager not found: " + currentUser.getUsername());
        }

        int newId = venueDao.getNextVenueId();

        Venue.Builder venueBuilder = new Venue.Builder()
                .id(newId)
                .name(venueBean.getName())
                .type(venueBean.getType())
                .address(venueBean.getAddress())
                .city(venueBean.getCity())
                .maxCapacity(venueBean.getMaxCapacity())
                .venueManager(manager);

        for (TeamNBA team : venueBean.getAssociatedTeams()) {
            venueBuilder.addTeam(team);
        }

        Venue venue = venueBuilder.build();
        Venue savedVenue = venueDao.saveVenue(venue);

        for (TeamNBA team : venueBean.getAssociatedTeams()) {
            venueDao.saveVenueTeam(savedVenue, team);
        }

        manager.addVenue(savedVenue);

        return convertToBean(savedVenue);
    }

    // ==================== UPDATE OPERATION ====================

    /**
     * Updates an existing venue after verifying ownership.
     *
     * @param venueBean updated venue data (must include valid ID)
     * @throws UserSessionException if not logged in or doesn't own the venue
     * @throws DAOException if venue not found or database access fails
     */
    public void updateVenue(VenueBean venueBean) throws UserSessionException, DAOException {
        Venue existingVenue = getOwnedVenue(venueBean.getId());

        existingVenue.updateVenueDetails(
                venueBean.getName(),
                venueBean.getType(),
                venueBean.getAddress(),
                venueBean.getCity(),
                venueBean.getMaxCapacity(),
                venueBean.getAssociatedTeams()
        );

        venueDao.updateVenue(existingVenue);

        venueDao.deleteAllVenueTeams(existingVenue);
        for (TeamNBA team : venueBean.getAssociatedTeams()) {
            venueDao.saveVenueTeam(existingVenue, team);
        }
    }

    // ==================== DELETE OPERATION ====================

    /**
     * Deletes a venue after verifying ownership and no existing bookings.
     *
     * @param venueId ID of the venue to delete
     * @throws UserSessionException if not logged in or doesn't own the venue
     * @throws DAOException if venue has bookings or database access fails
     */
    public void deleteVenue(int venueId) throws UserSessionException, DAOException {
        Venue existingVenue = getOwnedVenue(venueId);

        if (!existingVenue.getAllBookings().isEmpty()) {
            throw new DAOException("Cannot delete venue with existing bookings. Please cancel all bookings first.");
        }

        venueDao.deleteAllVenueTeams(existingVenue);
        venueDao.deleteVenue(existingVenue);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validates current user is a logged-in venue manager.
     *
     * @return current user as {@link UserBean}
     * @throws UserSessionException if not logged in or not a venue manager
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
     * Retrieves a venue and verifies the current user owns it.
     *
     * @param venueId the venue ID
     * @return the {@link Venue} model
     * @throws UserSessionException if a user doesn't own the venue
     * @throws DAOException if venue not found
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
     * Converts a Venue model to VenueBean.
     *
     * @param venue the venue model
     * @return corresponding {@link VenueBean}
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