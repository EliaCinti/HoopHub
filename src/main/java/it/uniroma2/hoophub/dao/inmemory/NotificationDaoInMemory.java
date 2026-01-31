package it.uniroma2.hoophub.dao.inmemory;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of {@link NotificationDao}.
 *
 * <p>Stores Notification data in RAM via {@link InMemoryDataStore}.
 * Supports notification queries, marking as read, and cleanup operations.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NotificationDaoInMemory extends AbstractObservableDao implements NotificationDao {

    private static final Logger LOGGER = Logger.getLogger(NotificationDaoInMemory.class.getName());

    private static final String NOTIFICATION = "Notification";
    private static final String ERR_NULL_NOTIFICATION = "Notification cannot be null";
    private static final String ERR_NOTIFICATION_NOT_FOUND = "Notification not found";

    private final InMemoryDataStore dataStore;
    private final GlobalCache cache;

    public NotificationDaoInMemory() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.cache = GlobalCache.getInstance();
    }

    @Override
    public Notification saveNotification(Notification notification) {
        validateNotificationInput(notification);

        int notificationId = notification.getId();

        // Generate ID if not set
        if (notificationId <= 0) {
            notificationId = dataStore.getNextNotificationId();
            notification = rebuildNotificationWithId(notification, notificationId);
        }

        dataStore.saveNotification(notification);
        cache.put(generateCacheKey(notificationId), notification);

        LOGGER.log(Level.INFO, "Notification saved with ID: {0}", notificationId);
        notifyObservers(DaoOperation.INSERT, NOTIFICATION, String.valueOf(notificationId), notification);

        return notification;
    }

    @Override
    public Notification retrieveNotification(int id) {
        // Check cache first
        Notification cached = (Notification) cache.get(generateCacheKey(id));
        if (cached != null) {
            return cached;
        }

        Notification notification = dataStore.getNotification(id);
        if (notification != null) {
            cache.put(generateCacheKey(id), notification);
        }

        return notification;
    }

    @Override
    public List<Notification> getNotificationsForUser(String username, UserType userType) {
        validateUserInput(username, userType);

        List<Notification> result = new ArrayList<>();

        for (Notification notification : dataStore.getAllNotifications().values()) {
            if (notification.getUsername().equals(username) && notification.getUserType() == userType) {
                result.add(notification);
            }
        }

        // Sort by creation date descending (newest first)
        result.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return result;
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(String username, UserType userType) {
        validateUserInput(username, userType);

        List<Notification> result = new ArrayList<>();

        for (Notification notification : dataStore.getAllNotifications().values()) {
            if (notification.getUsername().equals(username)
                    && notification.getUserType() == userType
                    && !notification.isRead()) {
                result.add(notification);
            }
        }

        // Sort by creation date descending (newest first)
        result.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return result;
    }

    @Override
    public void updateNotification(Notification notification) throws DAOException {
        validateNotificationInput(notification);
        int notificationId = notification.getId();

        if (dataStore.getNotification(notificationId) == null) {
            throw new DAOException(ERR_NOTIFICATION_NOT_FOUND + ": " + notificationId);
        }

        dataStore.saveNotification(notification);
        cache.put(generateCacheKey(notificationId), notification);

        LOGGER.log(Level.FINE, "Notification updated: {0}", notificationId);
        notifyObservers(DaoOperation.UPDATE, NOTIFICATION, String.valueOf(notificationId), notification);
    }

    @Override
    public void markAllAsReadForUser(String username, UserType userType) {
        validateUserInput(username, userType);

        List<Notification> unread = getUnreadNotificationsForUser(username, userType);

        for (Notification notification : unread) {
            Notification updated = new Notification.Builder()
                    .from(notification)
                    .isRead(true)
                    .build();

            dataStore.saveNotification(updated);
            cache.put(generateCacheKey(updated.getId()), updated);
        }

        LOGGER.log(Level.INFO, "Marked {0} notifications as read for user: {1}",
                new Object[]{unread.size(), username});
    }

    @Override
    public void deleteNotification(Notification notification) throws DAOException {
        validateNotificationInput(notification);
        int notificationId = notification.getId();

        if (dataStore.getNotification(notificationId) == null) {
            throw new DAOException(ERR_NOTIFICATION_NOT_FOUND + ": " + notificationId);
        }

        dataStore.deleteNotification(notificationId);
        cache.remove(generateCacheKey(notificationId));

        LOGGER.log(Level.INFO, "Notification deleted: {0}", notificationId);
        notifyObservers(DaoOperation.DELETE, NOTIFICATION, String.valueOf(notificationId), null);
    }

    @Override
    public void deleteNotificationsByBooking(int bookingId) {
        List<Integer> toDelete = new ArrayList<>();

        for (Notification notification : dataStore.getAllNotifications().values()) {
            if (notification.getBookingId() != null && notification.getBookingId() == bookingId) {
                toDelete.add(notification.getId());
            }
        }

        for (Integer id : toDelete) {
            dataStore.deleteNotification(id);
            cache.remove(generateCacheKey(id));
        }

        LOGGER.log(Level.INFO, "Deleted {0} notifications for booking: {1}",
                new Object[]{toDelete.size(), bookingId});
    }

    @Override
    public int getUnreadCount(String username, UserType userType) {
        validateUserInput(username, userType);

        int count = 0;

        for (Notification notification : dataStore.getAllNotifications().values()) {
            if (notification.getUsername().equals(username)
                    && notification.getUserType() == userType
                    && !notification.isRead()) {
                count++;
            }
        }

        return count;
    }

    // ========== PRIVATE HELPERS ==========

    private String generateCacheKey(int id) {
        return "Notification:" + id;
    }

    private void validateNotificationInput(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION);
        }
    }

    private void validateUserInput(String username, UserType userType) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (userType == null) {
            throw new IllegalArgumentException("UserType cannot be null");
        }
    }

    /**
     * Rebuilds a Notification with a new ID.
     */
    private Notification rebuildNotificationWithId(Notification original, int newId) {
        return new Notification.Builder()
                .from(original)
                .id(newId)
                .build();
    }
}