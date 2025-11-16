package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Notification entity.
 * Defines CRUD operations and specific queries for notifications.
 */
public interface NotificationDAO {

    /**
     * Saves a new notification to the persistence layer.
     *
     * @param notification the notification to save
     * @return the saved notification with generated ID
     * @throws DAOException if save operation fails
     */
    Notification save(Notification notification) throws DAOException;

    /**
     * Finds a notification by its ID.
     *
     * @param id the notification ID
     * @return Optional containing the notification if found, empty otherwise
     * @throws DAOException if query fails
     */
    Optional<Notification> findById(Long id) throws DAOException;

    /**
     * Retrieves all notifications for a specific user.
     *
     * @param userId the user ID
     * @return list of notifications (empty if none found)
     * @throws DAOException if query fails
     */
    List<Notification> findByUserId(Long userId) throws DAOException;

    /**
     * Retrieves all unread notifications for a specific user.
     *
     * @param userId the user ID
     * @return list of unread notifications (empty if none found)
     * @throws DAOException if query fails
     */
    List<Notification> findUnreadByUserId(Long userId) throws DAOException;

    /**
     * Retrieves notifications filtered by user and type.
     *
     * @param userId the user ID
     * @param type the notification type
     * @return list of matching notifications (empty if none found)
     * @throws DAOException if query fails
     */
    List<Notification> findByUserIdAndType(Long userId, NotificationType type) throws DAOException;

    /**
     * Retrieves notifications related to a specific booking.
     *
     * @param bookingId the booking ID
     * @return list of related notifications (empty if none found)
     * @throws DAOException if query fails
     */
    List<Notification> findByBookingId(Long bookingId) throws DAOException;

    /**
     * Counts unread notifications for a specific user.
     *
     * @param userId the user ID
     * @return number of unread notifications
     * @throws DAOException if query fails
     */
    int countUnreadByUserId(Long userId) throws DAOException;

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId the notification ID
     * @return true if update was successful, false otherwise
     * @throws DAOException if update fails
     */
    boolean markAsRead(Long notificationId) throws DAOException;

    /**
     * Marks all notifications for a user as read.
     *
     * @param userId the user ID
     * @return number of notifications updated
     * @throws DAOException if update fails
     */
    int markAllAsReadByUserId(Long userId) throws DAOException;

    /**
     * Marks all notifications related to a booking as read for a specific user.
     * Useful when a user views the booking details.
     *
     * @param bookingId the booking ID
     * @param userId the user ID
     * @return number of notifications updated
     * @throws DAOException if update fails
     */
    int markAsReadByBookingAndUser(Long bookingId, Long userId) throws DAOException;

    /**
     * Updates an existing notification.
     *
     * @param notification the notification to update
     * @return true if update was successful, false otherwise
     * @throws DAOException if update fails
     */
    boolean update(Notification notification) throws DAOException;

    /**
     * Deletes a notification by ID.
     *
     * @param id the notification ID
     * @return true if deletion was successful, false otherwise
     * @throws DAOException if delete fails
     */
    boolean deleteById(Long id) throws DAOException;

    /**
     * Deletes all notifications for a specific user.
     * Useful when a user account is deleted.
     *
     * @param userId the user ID
     * @return number of notifications deleted
     * @throws DAOException if delete fails
     */
    int deleteByUserId(Long userId) throws DAOException;

    /**
     * Deletes all read notifications for a specific user.
     * Useful for cleanup operations.
     *
     * @param userId the user ID
     * @return number of notifications deleted
     * @throws DAOException if delete fails
     */
    int deleteReadByUserId(Long userId) throws DAOException;

    /**
     * Retrieves all notifications in the system.
     * Primarily for admin purposes or testing.
     *
     * @return list of all notifications
     * @throws DAOException if query fails
     */
    List<Notification> findAll() throws DAOException;

    /**
     * Retrieves the most recent notifications for a user.
     *
     * @param userId the user ID
     * @param limit maximum number of notifications to retrieve
     * @return list of recent notifications ordered by creation date (newest first)
     * @throws DAOException if query fails
     */
    List<Notification> findRecentByUserId(Long userId, int limit) throws DAOException;
}
