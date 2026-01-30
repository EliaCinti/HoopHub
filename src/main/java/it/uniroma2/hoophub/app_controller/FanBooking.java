package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.NbaGameBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.exception.UserSessionException;

import java.util.List;

/**
 * Interface for Fan booking operations (ISP compliance).
 *
 * <p>Exposes only methods relevant to Fan users: browsing games,
 * selecting venues, creating bookings, and managing own reservations.</p>
 *
 * <p>This interface is part of the Interface Segregation Principle (ISP)
 * implementation, ensuring Fan graphic controllers only depend on
 * Fan-specific operations.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see VenueManagerBooking
 * @see BookGameSeatController
 */
public interface FanBooking {

    // ==================== GAME BROWSING ====================

    /**
     * Retrieves upcoming NBA games within the schedule window.
     *
     * @return List of NbaGameBean for display
     */
    List<NbaGameBean> getUpcomingGames();

    // ==================== VENUE SELECTION ====================

    /**
     * Retrieves all venues that broadcast the selected game.
     *
     * @param game The selected game
     * @return List of VenueBean
     * @throws DAOException if database access fails
     */
    List<VenueBean> getVenuesForGame(NbaGameBean game) throws DAOException;

    /**
     * Retrieves venues for a game with optional filters.
     *
     * @param game          The selected game
     * @param cityFilter    Filter by city (null or empty to skip)
     * @param typeFilter    Filter by venue type (null to skip)
     * @param onlyWithSeats If true, only return venues with available seats
     * @return Filtered list of VenueBean
     * @throws DAOException if database access fails
     */
    List<VenueBean> getVenuesForGame(NbaGameBean game, String cityFilter,
                                     String typeFilter, boolean onlyWithSeats) throws DAOException;

    /**
     * Gets all unique cities from venues that show a specific game.
     *
     * @param game The selected game
     * @return List of city names for filter dropdown
     * @throws DAOException if database access fails
     */
    List<String> getAvailableCitiesForGame(NbaGameBean game) throws DAOException;

    // ==================== SEAT AVAILABILITY ====================

    /**
     * Calculates available seats for a venue and game.
     *
     * @param venueId The venue ID
     * @param game    The game
     * @return Number of available seats (0 if full)
     */
    int getAvailableSeats(int venueId, NbaGameBean game);

    /**
     * Checks if a venue has available seats for a game.
     *
     * @param venueId The venue ID
     * @param game    The game
     * @return true if at least one seat is available
     */
    boolean hasAvailableSeats(int venueId, NbaGameBean game);

    /**
     * Checks if the current fan has already booked this game.
     *
     * @param game The game to check
     * @return true if already booked (PENDING or CONFIRMED)
     * @throws DAOException         if database access fails
     * @throws UserSessionException if no fan is logged in
     */
    boolean hasAlreadyBooked(NbaGameBean game) throws DAOException, UserSessionException;

    // ==================== BOOKING CREATION ====================

    /**
     * Creates a new booking request (status: PENDING).
     *
     * @param game    The selected game
     * @param venueId The selected venue ID
     * @throws UserSessionException  if no fan is logged in
     * @throws DAOException          if database access fails
     * @throws IllegalStateException if no seats available
     */
    void createBookingRequest(NbaGameBean game, int venueId) throws UserSessionException, DAOException;

    // ==================== BOOKING MANAGEMENT ====================

    /**
     * Retrieves all bookings for the current Fan.
     *
     * @return List of BookingBeans sorted by status then date
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    List<BookingBean> getMyBookings() throws DAOException, UserSessionException;

    /**
     * Retrieves bookings for the current Fan with optional status filter.
     *
     * @param statusFilter Optional status to filter by (null for all)
     * @return List of BookingBeans sorted by status then date
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    List<BookingBean> getMyBookings(BookingStatus statusFilter) throws DAOException, UserSessionException;

    /**
     * Retrieves only cancellable bookings.
     *
     * @return List of cancellable BookingBeans
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    List<BookingBean> getCancellableBookings() throws DAOException, UserSessionException;

    /**
     * Cancels a booking.
     *
     * @param bookingId The booking ID to cancel
     * @throws DAOException          if database error occurs
     * @throws UserSessionException  if no user logged in
     * @throws IllegalStateException if booking cannot be canceled
     */
    void cancelBooking(int bookingId) throws DAOException, UserSessionException;

    /**
     * Checks if a booking can be canceled.
     *
     * @param bookingId The booking ID to check
     * @return true if cancellation is allowed
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    boolean canCancelBooking(int bookingId) throws DAOException, UserSessionException;

    // ==================== NOTIFICATIONS ====================

    /**
     * Gets the count of unread notifications for the current Fan.
     *
     * @return Number of unread notifications
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    int getFanUnreadNotificationsCount() throws DAOException, UserSessionException;

    /**
     * Marks all notifications as read for the current Fan.
     *
     * @throws DAOException         if database error occurs
     * @throws UserSessionException if no user logged in
     */
    void markFanNotificationsAsRead() throws DAOException, UserSessionException;
}