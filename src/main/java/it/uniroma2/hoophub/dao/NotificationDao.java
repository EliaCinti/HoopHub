package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

import java.util.List;

/**
 * Data Access Object interface for Notification entity operations.
 * <p>
 * Defines the contract for notification persistence following the <strong>Model-First</strong>
 * and <strong>Return the Entity</strong> patterns.
 * </p>
 *
 * @author Elia Cinti
 */
public interface NotificationDao extends ObservableDao {

    /**
     * Saves a new notification to the persistence layer.
     * <p>
     * Returns the saved Notification entity with the generated ID.
     * Notifies observers (INSERT) for synchronization.
     * </p>
     *
     * @param notification The notification model to save (without ID)
     * @return The saved Notification with the generated ID
     * @throws DAOException             If there is an error during the save operation
     * @throws IllegalArgumentException If notification is null
     */
    Notification saveNotification(Notification notification) throws DAOException;

    /**
     * Retrieves a notification by its unique identifier.
     *
     * @param id The notification ID
     * @return The Notification model object, or null if not found
     * @throws DAOException If there is a database error
     */
    Notification retrieveNotification(int id) throws DAOException;

    /**
     * Retrieves all notifications for a specific user.
     *
     * @param username The user's username
     * @param userType The type of user (FAN or VENUE_MANAGER)
     * @return List of notifications for the user, newest first
     * @throws DAOException If there is an error retrieving notifications
     */
    List<Notification> getNotificationsForUser(String username, UserType userType) throws DAOException;

    /**
     * Retrieves all unread notifications for a specific user.
     *
     * @param username The user's username
     * @param userType The type of user (FAN or VENUE_MANAGER)
     * @return List of unread notifications, newest first
     * @throws DAOException If there is an error retrieving notifications
     */
    List<Notification> getUnreadNotificationsForUser(String username, UserType userType) throws DAOException;

    /**
     * Updates an existing notification.
     * <p>
     * Used mainly to mark notifications as read.
     * Notifies observers (UPDATE) for synchronization.
     * </p>
     *
     * @param notification The notification model with updated state (e.g. isRead = true)
     * @throws DAOException If the notification is not found or there is a database error
     * @throws IllegalArgumentException If notification is null
     */
    void updateNotification(Notification notification) throws DAOException;

    /**
     * Marks all notifications for a user as read.
     * <p>
     * This is a batch update operation for convenience.
     * </p>
     *
     * @param username The user's username
     * @param userType The type of user
     * @throws DAOException If there is an error updating notifications
     */
    void markAllAsReadForUser(String username, UserType userType) throws DAOException;

    /**
     * Deletes a notification.
     * <p>
     * Notifies observers (DELETE) for synchronization.
     * </p>
     *
     * @param notification The notification object to delete
     * @throws DAOException If the notification is not found or there is a database error
     * @throws IllegalArgumentException If notification is null
     */
    void deleteNotification(Notification notification) throws DAOException;

    /**
     * Deletes all notifications associated with a specific booking.
     * <p>
     * Used for cleanup when a booking is deleted.
     * </p>
     *
     * @param bookingId The booking ID
     * @throws DAOException If there is an error deleting notifications
     */
    void deleteNotificationsByBooking(int bookingId) throws DAOException;

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param username The user's username
     * @param userType The type of user
     * @return The number of unread notifications
     * @throws DAOException If there is an error counting notifications
     */
    int getUnreadCount(String username, UserType userType) throws DAOException;
}
