package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application controller for the Manage Seats use case (Fan side).
 *
 * <p>Handles retrieval and cancellation of bookings for the current Fan.
 * Follows BCE architecture - orchestrates business logic between
 * graphic controllers and DAOs.</p>
 *
 * <h3>Supported Operations</h3>
 * <ul>
 *   <li>View all own bookings (with optional status filter)</li>
 *   <li>Cancel bookings (PENDING or CONFIRMED, within deadline)</li>
 *   <li>Check/mark notifications as read</li>
 * </ul>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ManageSeatsController {

    private static final Logger LOGGER = Logger.getLogger(ManageSeatsController.class.getName());

    /** Days before game date when cancellation is no longer allowed. */
    private static final int CANCELLATION_DEADLINE_DAYS = 2;

    private final BookingDao bookingDao;
    private final VenueDao venueDao;
    private final NotificationDao notificationDao;

    public ManageSeatsController() {
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        this.bookingDao = daoFactory.getBookingDao();
        this.venueDao = daoFactory.getVenueDao();
        this.notificationDao = daoFactory.getNotificationDao();
    }

    // ========================================================================
    // BOOKING RETRIEVAL
    // ========================================================================

    /**
     * Retrieves all bookings for the current Fan.
     *
     * @return List of BookingBeans sorted by status (actionable first) then date
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public List<BookingBean> getMyBookings() throws DAOException, UserSessionException {
        return getMyBookings(null);
    }

    /**
     * Retrieves bookings for the current Fan with optional status filter.
     *
     * @param statusFilter Optional status to filter by (null for all)
     * @return List of BookingBeans sorted by status then date
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public List<BookingBean> getMyBookings(BookingStatus statusFilter) throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        String username = currentUser.getUsername();

        List<Booking> bookings;
        if (statusFilter != null) {
            bookings = bookingDao.retrieveBookingsByStatus(username, statusFilter);
        } else {
            bookings = bookingDao.retrieveBookingsByFan(username);
        }

        // Convert to beans with venue info
        List<BookingBean> bookingBeans = new ArrayList<>();
        for (Booking booking : bookings) {
            Venue venue = venueDao.retrieveVenue(booking.getVenueId());
            bookingBeans.add(convertToBookingBean(booking, venue));
        }

        // Sort: actionable (PENDING, CONFIRMED) first, then by game date descending
        bookingBeans.sort(Comparator
                .comparing((BookingBean b) -> isTerminalStatus(b.getStatus()))
                .thenComparing(BookingBean::getGameDate, Comparator.reverseOrder()));
        return bookingBeans;
    }

    /**
     * Retrieves only actionable bookings (PENDING or CONFIRMED within deadline).
     *
     * @return List of cancellable BookingBeans
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public List<BookingBean> getCancellableBookings() throws DAOException, UserSessionException {
        List<BookingBean> allBookings = getMyBookings();
        return allBookings.stream()
                .filter(this::canBeCancelled)
                .toList();
    }

    // ========================================================================
    // BOOKING CANCELLATION
    // ========================================================================

    /**
     * Cancels a booking.
     *
     * <p>Validates ownership, status, and deadline before cancelling.
     * Notifies the VenueManager of the cancellation.</p>
     *
     * @param bookingId The booking ID to cancel
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     * @throws IllegalStateException if booking cannot be canceled
     */
    public void cancelBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = getAndValidateBooking(bookingId);

        // Validate cancellation is allowed
        validateCancellation(booking);

        // Cancel the booking (model handles state transition)
        booking.cancel();
        bookingDao.updateBooking(booking);

        // Notify the VenueManager
        notifyVenueManager(booking);

        LOGGER.log(Level.INFO, "Booking {0} cancelled by fan", bookingId);
    }

    /**
     * Checks if a booking can be canceled.
     *
     * @param bookingId The booking ID to check
     * @return true if cancellation is allowed
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public boolean canCancelBooking(int bookingId) throws DAOException, UserSessionException {
        try {
            Booking booking = getAndValidateBooking(bookingId);
            validateCancellation(booking);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ========================================================================
    // NOTIFICATIONS
    // ========================================================================

    /**
     * Gets the count of unread notifications for the current Fan.
     *
     * @return Number of unread notifications
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public int getUnreadNotificationsCount() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        return notificationDao.getUnreadCount(currentUser.getUsername(), UserType.FAN);
    }

    /**
     * Marks all notifications as read for the current Fan.
     *
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     */
    public void markNotificationsAsRead() throws DAOException, UserSessionException {
        UserBean currentUser = getCurrentFan();
        notificationDao.markAllAsReadForUser(currentUser.getUsername(), UserType.FAN);

        LOGGER.log(Level.INFO, "Marked all notifications as read for Fan: {0}",
                currentUser.getUsername());
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Gets the current logged-in Fan.
     *
     * @return Current UserBean
     * @throws UserSessionException if no user logged in or not a Fan
     */
    private UserBean getCurrentFan() throws UserSessionException {
        UserBean user = SessionManager.INSTANCE.getCurrentUser();

        if (user == null) {
            throw new UserSessionException("No user logged in");
        }

        if (user.getType() != UserType.FAN) {
            throw new UserSessionException("Current user is not a Fan");
        }

        return user;
    }

    /**
     * Retrieves and validates booking ownership.
     *
     * @param bookingId The booking ID
     * @return The Booking if valid
     * @throws DAOException if database error occurs
     * @throws UserSessionException if no user logged in
     * @throws IllegalStateException if booking not found or not owned
     */
    private Booking getAndValidateBooking(int bookingId) throws DAOException, UserSessionException {
        Booking booking = bookingDao.retrieveBooking(bookingId);
        if (booking == null) {
            throw new IllegalStateException("Booking not found: " + bookingId);
        }

        UserBean currentUser = getCurrentFan();
        if (!booking.getFanUsername().equals(currentUser.getUsername())) {
            throw new IllegalStateException("Booking does not belong to you");
        }

        return booking;
    }

    /**
     * Validates that a booking can be canceled.
     *
     * @param booking The booking to validate
     * @throws IllegalStateException if cancellation not allowed
     */
    private void validateCancellation(Booking booking) {
        // Check status
        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only PENDING or CONFIRMED bookings can be cancelled");
        }

        // Check deadline
        LocalDate deadline = booking.getGameDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        if (LocalDate.now().isAfter(deadline)) {
            throw new IllegalStateException(
                    String.format("Cannot cancel bookings within %d days of the game", CANCELLATION_DEADLINE_DAYS));
        }
    }

    /**
     * Checks if a status is terminal (cannot be canceled).
     */
    private boolean isTerminalStatus(BookingStatus status) {
        return status == BookingStatus.REJECTED || status == BookingStatus.CANCELLED;
    }

    /**
     * Checks if a booking can be canceled (status + deadline).
     */
    private boolean canBeCancelled(BookingBean booking) {
        if (isTerminalStatus(booking.getStatus())) {
            return false;
        }
        LocalDate deadline = booking.getGameDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        return !LocalDate.now().isAfter(deadline);
    }

    /**
     * Notifies the VenueManager about the cancellation.
     */
    private void notifyVenueManager(Booking booking) throws DAOException {
        Venue venue = venueDao.retrieveVenue(booking.getVenueId());
        if (venue == null) {
            LOGGER.log(Level.WARNING, "Venue not found for booking {0}", booking.getId());
            return;
        }

        Notification notification = new Notification.Builder()
                .username(venue.getVenueManagerUsername())
                .userType(UserType.VENUE_MANAGER)
                .type(NotificationType.BOOKING_CANCELLED)
                .message(String.format("Booking cancelled: %s for %s on %s",
                        booking.getFanUsername(),
                        booking.getMatchup(),
                        booking.getGameDate()))
                .bookingId(booking.getId())
                .build();

        notificationDao.saveNotification(notification);
    }

    /**
     * Converts a Booking model to BookingBean.
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