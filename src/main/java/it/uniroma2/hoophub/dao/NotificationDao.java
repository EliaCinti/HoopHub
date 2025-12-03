package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

import java.util.List;

/**
 * Data Access Object interface for Notification entity operations.
 * <p>
 * This interface defines the contract for notification persistence operations
 * across different storage mechanisms (CSV, MySQL). It extends ObservableDao
 * to support the Observer pattern for cross-persistence synchronization.
 * </p>
 * <p>
 * Implementations of this interface must:
 * <ul>
 *   <li>Handle notification CRUD operations</li>
 *   <li>Support querying notifications by user and type</li>
 *   <li>Implement ObservableDao for synchronization</li>
 *   <li>Notify observers after data changes</li>
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 */
public interface NotificationDao extends ObservableDao {

    /**
     * Saves a new notification to the persistence layer.
     * <p>
     * After saving, this method MUST call notifyObservers(INSERT, ...)
     * to trigger cross-persistence synchronization and business logic observers.
     * </p>
     *
     * @param notificationBean The notification data to save
     * @throws DAOException If there is an error during the save operation
     */
    void saveNotification(NotificationBean notificationBean) throws DAOException;

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
     * <p>
     * This method returns notifications in reverse chronological order
     * (newest first) to provide a natural inbox-like experience.
     * </p>
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
     * Marks a notification as read.
     * <p>
     * After updating, this method MUST call notifyObservers(UPDATE, ...)
     * to trigger synchronization.
     * </p>
     *
     * @param notificationId The ID of the notification to mark as read
     * @throws DAOException If the notification is not found or there is a database error
     */
    void markAsRead(int notificationId) throws DAOException;

    /**
     * Marks all notifications for a user as read.
     *
     * @param username The user's username
     * @param userType The type of user (FAN or VENUE_MANAGER)
     * @throws DAOException If there is an error updating notifications
     */
    void markAllAsReadForUser(String username, UserType userType) throws DAOException;

    /**
     * Deletes a notification by ID.
     * <p>
     * After deleting, this method MUST call notifyObservers(DELETE, ...)
     * to trigger synchronization.
     * </p>
     *
     * @param notificationId The ID of the notification to delete
     * @throws DAOException If the notification is not found or there is a database error
     */
    void deleteNotification(int notificationId) throws DAOException;

    /**
     * Deletes all notifications for a specific booking.
     * <p>
     * This is useful when a booking is cancelled or deleted, and all
     * related notifications should be cleaned up.
     * </p>
     *
     * @param bookingId The booking ID
     * @throws DAOException If there is an error deleting notifications
     */
    void deleteNotificationsByBooking(int bookingId) throws DAOException;

    /**
     * Gets the count of unread notifications for a user.
     * <p>
     * Used for displaying notification badges in the UI.
     * </p>
     *
     * @param username The user's username
     * @param userType The type of user (FAN or VENUE_MANAGER)
     * @return The number of unread notifications
     * @throws DAOException If there is an error counting notifications
     */
    int getUnreadCount(String username, UserType userType) throws DAOException;
}
