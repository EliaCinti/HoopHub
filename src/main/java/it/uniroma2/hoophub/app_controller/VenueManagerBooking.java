package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.util.List;

/**
 * Interface for VenueManager booking operations (ISP compliance).
 *
 * <p>Exposes only methods relevant to VenueManager users: viewing booking
 * requests for their venues and approving/rejecting them.</p>
 *
 * <p>This interface is part of the Interface Segregation Principle (ISP)
 * implementation, ensuring VenueManager graphic controllers only depend on
 * VenueManager-specific operations.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see FanBooking
 * @see BookGameSeatController
 */
public interface VenueManagerBooking {

    // ==================== BOOKING RETRIEVAL ====================

    /**
     * Gets all bookings for venues owned by the current VenueManager.
     *
     * @return List of bookings for all owned venues
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    List<BookingBean> getBookingsForMyVenues() throws DAOException, UserSessionException;

    /**
     * Gets bookings for venues owned by the current VenueManager, optionally filtered by status.
     *
     * @param statusFilter Optional status filter (null for all statuses)
     * @return List of filtered bookings
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    List<BookingBean> getBookingsForMyVenues(BookingStatus statusFilter)
            throws DAOException, UserSessionException;

    /**
     * Gets only pending bookings for quick access.
     *
     * @return List of pending bookings
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    List<BookingBean> getPendingBookings() throws DAOException, UserSessionException;

    // ==================== BOOKING ACTIONS ====================

    /**
     * Approves a pending booking request.
     *
     * @param bookingId The booking ID to approve
     * @throws DAOException          if database access fails
     * @throws UserSessionException  if no VenueManager is logged in
     * @throws IllegalStateException if booking is not PENDING or not owned
     */
    void approveBooking(int bookingId) throws DAOException, UserSessionException;

    /**
     * Rejects a pending booking request.
     *
     * @param bookingId The booking ID to reject
     * @throws DAOException          if database access fails
     * @throws UserSessionException  if no VenueManager is logged in
     * @throws IllegalStateException if booking is not PENDING or not owned
     */
    void rejectBooking(int bookingId) throws DAOException, UserSessionException;

    // ==================== NOTIFICATIONS ====================

    /**
     * Gets the count of unread notifications for the current VenueManager.
     *
     * @return Number of unread notifications
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    int getVmUnreadNotificationsCount() throws DAOException, UserSessionException;

    /**
     * Marks all notifications as read for the current VenueManager.
     *
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no VenueManager is logged in
     */
    void markVmNotificationsAsRead() throws DAOException, UserSessionException;
}
