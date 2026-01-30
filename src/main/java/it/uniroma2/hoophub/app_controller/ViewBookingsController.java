package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application controller for the View Bookings use case (VenueManager side).
 *
 * <p>Handles viewing, approving, and rejecting booking requests for venues
 * owned by the current VenueManager. Notifications are automatically generated
 * by {@link it.uniroma2.hoophub.patterns.observer.NotificationBookingObserver}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ViewBookingsController {

    private static final Logger LOGGER = Logger.getLogger(ViewBookingsController.class.getName());

    private final BookingDao bookingDao;
    private final VenueDao venueDao;
    private final NotificationDao notificationDao;

    public ViewBookingsController() {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.bookingDao = daoFactory.getBookingDao();
        this.venueDao = daoFactory.getVenueDao();
        this.notificationDao = daoFactory.getNotificationDao();
    }

    // ==================== BOOKING RETRIEVAL ====================

    /**
     * Gets all bookings for venues owned by the current VenueManager.
     *
     * @return List of bookings for all owned venues
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    public List<BookingBean> getBookingsForMyVenues() throws DAOException, UserSessionException {
        return getBookingsForMyVenues(null);
    }

    /**
     * Gets bookings for venues owned by the current VenueManager, optionally filtered by status.
     *
     * @param statusFilter Optional status filter (null for all statuses)
     * @return List of filtered bookings
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    public List<BookingBean> getBookingsForMyVenues(BookingStatus statusFilter)
            throws DAOException, UserSessionException {

        UserBean currentUser = getCurrentVenueManager();

        // Usa il metodo ottimizzato che recupera tutte le booking per manager
        List<Booking> allBookings = bookingDao.retrieveBookingsByVenueManager(currentUser.getUsername());
        List<BookingBean> result = new ArrayList<>();

        for (Booking booking : allBookings) {
            if (statusFilter == null || booking.getStatus() == statusFilter) {
                Venue venue = venueDao.retrieveVenue(booking.getVenueId());
                BookingBean bean = convertToBookingBean(booking, venue);
                result.add(bean);
            }
        }

        // Sort: PENDING first, then by date descending
        result.sort((b1, b2) -> {
            if (b1.getStatus() == BookingStatus.PENDING && b2.getStatus() != BookingStatus.PENDING) {
                return -1;
            }
            if (b2.getStatus() == BookingStatus.PENDING && b1.getStatus() != BookingStatus.PENDING) {
                return 1;
            }
            return b2.getGameDate().compareTo(b1.getGameDate());
        });

        return result;
    }

    /**
     * Gets only pending bookings for quick access.
     *
     * @return List of pending bookings
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    public List<BookingBean> getPendingBookings() throws DAOException, UserSessionException {
        return getBookingsForMyVenues(BookingStatus.PENDING);
    }

    // ==================== BOOKING ACTIONS ====================

    /**
     * Approves a pending booking request.
     *
     * @param bookingId The booking ID to approve
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     * @throws IllegalStateException if booking is not PENDING or not owned by current user
     */
    public void approveBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be approved");
        }

        booking.confirm();  // USA confirm() invece di setStatus()
        bookingDao.updateBooking(booking);

        LOGGER.log(Level.INFO, "Booking {0} approved", bookingId);
    }

    /**
     * Rejects a pending booking request.
     *
     * @param bookingId The booking ID to reject
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     * @throws IllegalStateException if booking is not PENDING or not owned by current user
     */
    public void rejectBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be rejected");
        }

        booking.reject();  // USA reject() invece di setStatus()
        bookingDao.updateBooking(booking);

        LOGGER.log(Level.INFO, "Booking {0} rejected", bookingId);
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Gets the count of unread notifications for the current VenueManager.
     *
     * @return Number of unread notifications
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    public int getUnreadNotificationsCount() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();
        return notificationDao.getUnreadCount(currentUser.getUsername(), UserType.VENUE_MANAGER);
    }

    /**
     * Marks all notifications as read for the current VenueManager.
     *
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    public void markNotificationsAsRead() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();
        notificationDao.markAllAsReadForUser(currentUser.getUsername(), UserType.VENUE_MANAGER);

        LOGGER.log(Level.INFO, "Marked all notifications as read for VenueManager: {0}",
                currentUser.getUsername());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Gets the current logged-in VenueManager.
     *
     * @return Current user as UserBean
     * @throws UserSessionException if no user or wrong user type is logged in
     */
    private UserBean getCurrentVenueManager() throws UserSessionException {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();

        if (currentUser == null) {
            throw new UserSessionException("No user logged in");
        }

        if (currentUser.getType() != UserType.VENUE_MANAGER) {
            throw new UserSessionException("Current user is not a VenueManager");
        }

        return currentUser;
    }

    /**
     * Gets and validates a booking belongs to current VenueManager's venues.
     *
     * @param bookingId The booking ID
     * @return The booking if valid
     * @throws DAOException if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     * @throws IllegalStateException if booking not found or not owned
     */
    private Booking getAndValidateBooking(int bookingId) throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentVenueManager();

        Booking booking = bookingDao.retrieveBooking(bookingId);
        if (booking == null) {
            throw new IllegalStateException("Booking not found: " + bookingId);
        }

        // Validate ownership
        Venue venue = venueDao.retrieveVenue(booking.getVenueId());
        if (venue == null || !venue.getVenueManagerUsername().equals(currentUser.getUsername())) {
            throw new IllegalStateException("Booking does not belong to your venues");
        }

        return booking;
    }

    /**
     * Converts a Booking model to BookingBean.
     *
     * @param booking The booking model
     * @param venue The associated venue
     * @return BookingBean DTO
     */
    private BookingBean convertToBookingBean(Booking booking, Venue venue) {
        return new BookingBean.Builder()
                .id(booking.getId())
                .gameDate(booking.getGameDate())
                .gameTime(booking.getGameTime())
                .homeTeam(booking.getHomeTeam())
                .awayTeam(booking.getAwayTeam())
                .venueId(booking.getVenueId())
                .venueName(venue != null ? venue.getName() : "Unknown")
                .fanUsername(booking.getFanUsername())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}