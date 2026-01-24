package it.uniroma2.hoophub.app_controller;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.session.SessionManager;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application controller for notification management.
 *
 * <p>Handles notification retrieval and read status updates for both
 * Fan and VenueManager users. Fails silently on errors, returning
 * safe defaults (false/0/empty list).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see Notification
 */
public class NotificationController {

    private final NotificationDao notificationDao;
    private static final Logger logger = Logger.getLogger(NotificationController.class.getName());

    /**
     * Creates controller with DAO from {@link DaoFactoryFacade}.
     */
    public NotificationController() {
        this.notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();
    }

    /**
     * Checks if current user has any unread notifications.
     *
     * @return true if unread notifications exist, false otherwise or on error
     */
    public boolean hasUnreadNotifications() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            return notificationDao.getUnreadCount(
                    currentUser.getUsername(),
                    currentUser.getType()
            ) > 0;
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Failed to check unread notifications", e);
            return false;
        }
    }

    /**
     * Gets the count of unread notifications for current user.
     *
     * @return unread notification count, or 0 on error/no user
     */
    public int getUnreadNotificationCount() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }

        try {
            return notificationDao.getUnreadCount(
                    currentUser.getUsername(),
                    currentUser.getType()
            );
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Failed to get unread notification count", e);
            return 0;
        }
    }

    /**
     * Gets all unread notifications for the current user.
     *
     * @return list of unread {@link Notification}, or empty list on error/no user
     */
    public List<Notification> getUnreadNotifications() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        try {
            return notificationDao.getUnreadNotificationsForUser(
                    currentUser.getUsername(),
                    currentUser.getType()
            );
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Failed to get unread notifications", e);
            return Collections.emptyList();
        }
    }

    /**
     * Checks if current user has unread notifications of a specific type.
     *
     * @param type the {@link NotificationType} to check
     * @return true if unread notifications of that type exist
     */
    public boolean hasUnreadNotificationsOfType(NotificationType type) {
        List<Notification> unread = getUnreadNotifications();
        return unread.stream().anyMatch(n -> n.getType() == type);
    }

    /**
     * Marks all notifications as read for current user.
     * Fails silently if no user logged in or on error.
     */
    public void markAllAsRead() {
        UserBean currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            notificationDao.markAllAsReadForUser(
                    currentUser.getUsername(),
                    currentUser.getType()
            );
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Failed to mark notifications as read", e);
        }
    }

    /**
     * Marks a single notification as read.
     *
     * @param notification the notification to mark (ignored if null or already read)
     */
    public void markAsRead(Notification notification) {
        if (notification == null || notification.isRead()) {
            return;
        }

        try {
            Notification updated = notification.markAsRead();
            notificationDao.updateNotification(updated);
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Failed to mark notification as read", e);
        }
    }
}