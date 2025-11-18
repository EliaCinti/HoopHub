package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the NotificationDao interface.
 * <p>
 * This class provides comprehensive data access operations for Notification entities stored in a MySQL database.
 * It handles notification CRUD operations for both Fan and VenueManager users.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>notifications table</strong>: id (PK, AUTO_INCREMENT), username, user_type,
 *       notification_type, message, related_booking_id, is_read, created_at</li>
 * </ul>
 * </p>
 *
 * @see NotificationDao
 * @see AbstractMySqlDao
 */
public class NotificationDaoMySql extends AbstractMySqlDao implements NotificationDao {

    // ========== SQL Queries ==========
    private static final String SQL_INSERT_NOTIFICATION =
            "INSERT INTO notifications (username, user_type, notification_type, message, " +
                    "related_booking_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_NOTIFICATION =
            "SELECT id, username, user_type, notification_type, message, related_booking_id, " +
                    "is_read, created_at FROM notifications WHERE id = ?";

    private static final String SQL_SELECT_NOTIFICATIONS_BY_USER =
            "SELECT id, username, user_type, notification_type, message, related_booking_id, " +
                    "is_read, created_at FROM notifications WHERE username = ? AND user_type = ? " +
                    "ORDER BY created_at DESC";

    private static final String SQL_SELECT_UNREAD_NOTIFICATIONS =
            "SELECT id, username, user_type, notification_type, message, related_booking_id, " +
                    "is_read, created_at FROM notifications WHERE username = ? AND user_type = ? " +
                    "AND is_read = FALSE ORDER BY created_at DESC";

    private static final String SQL_UPDATE_MARK_AS_READ =
            "UPDATE notifications SET is_read = TRUE WHERE id = ?";

    private static final String SQL_UPDATE_ALL_AS_READ =
            "UPDATE notifications SET is_read = TRUE WHERE username = ? AND user_type = ? AND is_read = FALSE";

    private static final String SQL_DELETE_NOTIFICATION =
            "DELETE FROM notifications WHERE id = ?";

    private static final String SQL_DELETE_BY_BOOKING =
            "DELETE FROM notifications WHERE related_booking_id = ?";

    private static final String SQL_COUNT_UNREAD =
            "SELECT COUNT(*) FROM notifications WHERE username = ? AND user_type = ? AND is_read = FALSE";


    // ========== Constants ==========
    private static final String NOTIFICATION = "Notification";

    // ========== Error messages ==========
    private static final String ERR_NULL_NOTIFICATION_BEAN = "NotificationBean cannot be null";
    private static final String ERR_NULL_USERNAME = "Username cannot be null";
    private static final String ERR_NULL_USER_TYPE = "UserType cannot be null";
    private static final String ERR_NOTIFICATION_NOT_FOUND = "Notification not found";

    /**
     * {@inheritDoc}
     * <p>
     * After successful insertion, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public void saveNotification(NotificationBean notificationBean) throws DAOException {
        validateNotificationBeanInput(notificationBean);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_NOTIFICATION)) {

                stmt.setString(1, notificationBean.getUsername());
                stmt.setString(2, notificationBean.getUserType().name());
                stmt.setString(3, notificationBean.getType().name());
                stmt.setString(4, notificationBean.getMessage());

                if (notificationBean.getRelatedBookingId() == 0) {
                    stmt.setNull(5, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(5, notificationBean.getRelatedBookingId());
                }

                stmt.setBoolean(6, notificationBean.isRead());
                stmt.setTimestamp(7, Timestamp.valueOf(notificationBean.getCreatedAt()));

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification saved successfully for user: {0}",
                            notificationBean.getUsername());
                    notifyObservers(DaoOperation.INSERT, NOTIFICATION,
                            notificationBean.getUsername(), notificationBean);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notification save", e);
            throw new DAOException("Error saving notification", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Notification retrieveNotification(int id) throws DAOException {
        validateIdInput(id);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_NOTIFICATION)) {

                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToNotification(rs);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notification retrieval", e);
            throw new DAOException("Error retrieving notification", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> getNotificationsForUser(String username, UserType userType) throws DAOException {
        validateUsernameInput(username);
        validateUserTypeInput(userType);

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_NOTIFICATIONS_BY_USER)) {

                stmt.setString(1, username);
                stmt.setString(2, userType.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.FINE, "Retrieved {0} notifications for user {1}",
                        new Object[]{notifications.size(), username});
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notifications retrieval by user", e);
            throw new DAOException("Error retrieving notifications by user", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> getUnreadNotificationsForUser(String username, UserType userType) throws DAOException {
        validateUsernameInput(username);
        validateUserTypeInput(userType);

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNREAD_NOTIFICATIONS)) {

                stmt.setString(1, username);
                stmt.setString(2, userType.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.FINE, "Retrieved {0} unread notifications for user {1}",
                        new Object[]{notifications.size(), username});
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during unread notifications retrieval", e);
            throw new DAOException("Error retrieving unread notifications", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAsRead(int notificationId) throws DAOException {
        validateIdInput(notificationId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_MARK_AS_READ)) {

                stmt.setInt(1, notificationId);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification marked as read: {0}", notificationId);

                    // Create a minimal bean for notification
                    NotificationBean bean = new NotificationBean.Builder()
                            .id(notificationId)
                            .isRead(true)
                            .build();

                    notifyObservers(DaoOperation.UPDATE, NOTIFICATION, String.valueOf(notificationId), bean);
                } else {
                    throw new DAOException(ERR_NOTIFICATION_NOT_FOUND + ": " + notificationId);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notification update", e);
            throw new DAOException("Error marking notification as read", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAllAsReadForUser(String username, UserType userType) throws DAOException {
        validateUsernameInput(username);
        validateUserTypeInput(userType);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_ALL_AS_READ)) {

                stmt.setString(1, username);
                stmt.setString(2, userType.name());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Marked {0} notifications as read for user {1}",
                            new Object[]{affectedRows, username});
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during mark all as read", e);
            throw new DAOException("Error marking all notifications as read", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotification(int notificationId) throws DAOException {
        validateIdInput(notificationId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_NOTIFICATION)) {

                stmt.setInt(1, notificationId);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification deleted successfully: {0}", notificationId);
                    notifyObservers(DaoOperation.DELETE, NOTIFICATION, String.valueOf(notificationId), null);
                } else {
                    throw new DAOException(ERR_NOTIFICATION_NOT_FOUND + ": " + notificationId);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notification deletion", e);
            throw new DAOException("Error deleting notification", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationsByBooking(int bookingId) throws DAOException {
        validateIdInput(bookingId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_BOOKING)) {

                stmt.setInt(1, bookingId);

                int affectedRows = stmt.executeUpdate();

                logger.log(Level.INFO, "Deleted {0} notifications for booking: {1}",
                        new Object[]{affectedRows, bookingId});
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notifications deletion by booking", e);
            throw new DAOException("Error deleting notifications by booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnreadCount(String username, UserType userType) throws DAOException {
        validateUsernameInput(username);
        validateUserTypeInput(userType);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_UNREAD)) {

                stmt.setString(1, username);
                stmt.setString(2, userType.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during unread count retrieval", e);
            throw new DAOException("Error getting unread notification count", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps ResultSet to Notification.
     */
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException, DAOException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        UserType userType = UserType.valueOf(rs.getString("user_type"));
        NotificationType notificationType = NotificationType.valueOf(rs.getString("notification_type"));
        String message = rs.getString("message");
        int relatedBookingId = rs.getInt("related_booking_id");
        // If was NULL, getInt returns 0 and wasNull() returns true
        if (rs.wasNull()) {
            relatedBookingId = 0;
        }
        boolean isRead = rs.getBoolean("is_read");
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");

        return new Notification.Builder()
                .id(id)
                .username(username)
                .userType(userType)
                .type(notificationType)
                .message(message)
                .relatedBookingId(relatedBookingId)
                .isRead(isRead)
                .createdAt(createdAtTimestamp.toLocalDateTime())
                .build();
    }

    // ========== VALIDATION METHODS ==========

    private void validateNotificationBeanInput(NotificationBean notificationBean) {
        if (notificationBean == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION_BEAN);
        }
    }

    private void validateUsernameInput(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_USERNAME);
        }
    }

    private void validateUserTypeInput(UserType userType) {
        if (userType == null) {
            throw new IllegalArgumentException(ERR_NULL_USER_TYPE);
        }
    }
}
