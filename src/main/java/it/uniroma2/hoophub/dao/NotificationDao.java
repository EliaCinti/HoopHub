package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

import java.util.List;

/**
 * DAO interface for Notification entity persistence.
 *
 * <p>Extends {@link ObservableDao} for cross-persistence sync via <b>Observer pattern (GoF)</b>.
 * Handles notification CRUD and read-status management.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Notification
 */
public interface NotificationDao extends ObservableDao {

    /**
     * Saves a notification. Returns entity with generated ID.
     *
     * @param notification the notification to save
     * @return saved notification with ID
     * @throws DAOException if save fails
     */
    Notification saveNotification(Notification notification) throws DAOException;

    /**
     * Retrieves a notification by ID.
     *
     * @param id the notification ID
     * @return the notification, or null if not found
     * @throws DAOException if retrieval fails
     */
    Notification retrieveNotification(int id) throws DAOException;

    /**
     * Retrieves all notifications for a user (newest first).
     *
     * @param username the user's username
     * @param userType the user type (FAN or VENUE_MANAGER)
     * @return list of notifications
     * @throws DAOException if retrieval fails
     */
    List<Notification> getNotificationsForUser(String username, UserType userType) throws DAOException;

    /**
     * Retrieves unread notifications for a user (newest first).
     *
     * @param username the user's username
     * @param userType the user type
     * @return list of unread notifications
     * @throws DAOException if retrieval fails
     */
    List<Notification> getUnreadNotificationsForUser(String username, UserType userType) throws DAOException;

    /**
     * Updates a notification (e.g., mark as read).
     *
     * @param notification the notification with updated state
     * @throws DAOException if update fails
     */
    void updateNotification(Notification notification) throws DAOException;

    /**
     * Marks all notifications as read for a user.
     *
     * @param username the user's username
     * @param userType the user type
     * @throws DAOException if update fails
     */
    void markAllAsReadForUser(String username, UserType userType) throws DAOException;

    /**
     * Deletes a notification.
     *
     * @param notification the notification to delete
     * @throws DAOException if deletion fails
     */
    void deleteNotification(Notification notification) throws DAOException;

    /**
     * Deletes all notifications for a booking (cleanup on booking delete).
     *
     * @param bookingId the booking ID
     * @throws DAOException if deletion fails
     */
    void deleteNotificationsByBooking(int bookingId) throws DAOException;

    /**
     * Gets unread notification count for a user.
     *
     * @param username the user's username
     * @param userType the user type
     * @return count of unread notifications
     * @throws DAOException if count fails
     */
    int getUnreadCount(String username, UserType userType) throws DAOException;
}