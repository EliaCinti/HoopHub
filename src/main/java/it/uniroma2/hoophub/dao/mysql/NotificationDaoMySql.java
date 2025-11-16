package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.NotificationDAO;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * MySQL implementation of the NotificationDAO interface.
 * <p>
 * This class provides comprehensive data access operations for Notification entities stored in a MySQL database.
 * It handles notification CRUD operations and provides various filtering and marking methods
 * for efficient notification management.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>notifications table</strong>: id (PK, AUTO_INCREMENT), user_id, user_type,
 *       type, message, related_booking_id (FK), is_read, created_at</li>
 * </ul>
 * </p>
 *
 * @see NotificationDAO
 * @see AbstractMySqlDao
 */
public class NotificationDaoMySql extends AbstractMySqlDao implements NotificationDAO {

    // ========== SQL Queries ==========
    private static final String SQL_INSERT_NOTIFICATION =
            "INSERT INTO notifications (user_id, user_type, type, message, related_booking_id, " +
                    "is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE id = ?";

    private static final String SQL_SELECT_BY_USER =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE user_id = ? ORDER BY created_at DESC";

    private static final String SQL_SELECT_UNREAD_BY_USER =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";

    private static final String SQL_SELECT_BY_USER_AND_TYPE =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE user_id = ? AND type = ? ORDER BY created_at DESC";

    private static final String SQL_SELECT_BY_BOOKING =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE related_booking_id = ? ORDER BY created_at DESC";

    private static final String SQL_COUNT_UNREAD_BY_USER =
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

    private static final String SQL_MARK_AS_READ =
            "UPDATE notifications SET is_read = TRUE WHERE id = ?";

    private static final String SQL_MARK_ALL_AS_READ_BY_USER =
            "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";

    private static final String SQL_MARK_AS_READ_BY_BOOKING_AND_USER =
            "UPDATE notifications SET is_read = TRUE WHERE related_booking_id = ? AND user_id = ?";

    private static final String SQL_UPDATE_NOTIFICATION =
            "UPDATE notifications SET user_id = ?, user_type = ?, type = ?, message = ?, " +
                    "related_booking_id = ?, is_read = ? WHERE id = ?";

    private static final String SQL_DELETE_BY_ID =
            "DELETE FROM notifications WHERE id = ?";

    private static final String SQL_DELETE_BY_USER =
            "DELETE FROM notifications WHERE user_id = ?";

    private static final String SQL_DELETE_READ_BY_USER =
            "DELETE FROM notifications WHERE user_id = ? AND is_read = TRUE";

    private static final String SQL_SELECT_ALL =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications ORDER BY created_at DESC";

    private static final String SQL_SELECT_RECENT_BY_USER =
            "SELECT id, user_id, user_type, type, message, related_booking_id, is_read, created_at " +
                    "FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

    // ========== Error messages ==========
    private static final String ERR_NULL_NOTIFICATION = "Notification cannot be null";
    private static final String ERR_INVALID_LIMIT = "Limit must be positive";

    /**
     * {@inheritDoc}
     */
    @Override
    public Notification save(Notification notification) throws DAOException {
        if (notification == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION);
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_NOTIFICATION,
                         Statement.RETURN_GENERATED_KEYS)) {

                setNotificationParameters(stmt, notification, 1);
                stmt.setTimestamp(7, Timestamp.valueOf(notification.getCreatedAt()));

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long generatedId = generatedKeys.getLong(1);
                            logger.log(Level.INFO, "Notification saved successfully with ID: {0}", generatedId);

                            // Return new notification with generated ID
                            return new Notification.Builder()
                                    .from(notification)
                                    .id(generatedId)
                                    .build();
                        }
                    }
                }

                throw new DAOException("Failed to save notification");
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
    public Optional<Notification> findById(Long id) throws DAOException {
        validateIdInput(id);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {

                stmt.setLong(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToNotification(rs));
                    }
                    return Optional.empty();
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
    public List<Notification> findByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_USER)) {

                stmt.setLong(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} notifications for user {1}",
                        new Object[]{notifications.size(), userId});
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
    public List<Notification> findUnreadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNREAD_BY_USER)) {

                stmt.setLong(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} unread notifications for user {1}",
                        new Object[]{notifications.size(), userId});
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
    public List<Notification> findByUserIdAndType(Long userId, NotificationType type) throws DAOException {
        validateIdInput(userId);
        if (type == null) {
            throw new IllegalArgumentException("NotificationType cannot be null");
        }

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_USER_AND_TYPE)) {

                stmt.setLong(1, userId);
                stmt.setString(2, type.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} {1} notifications for user {2}",
                        new Object[]{notifications.size(), type, userId});
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notifications retrieval by type", e);
            throw new DAOException("Error retrieving notifications by type", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> findByBookingId(Long bookingId) throws DAOException {
        validateIdInput(bookingId);

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_BOOKING)) {

                stmt.setLong(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} notifications for booking {1}",
                        new Object[]{notifications.size(), bookingId});
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notifications retrieval by booking", e);
            throw new DAOException("Error retrieving notifications by booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countUnreadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_UNREAD_BY_USER)) {

                stmt.setLong(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during unread count", e);
            throw new DAOException("Error counting unread notifications", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markAsRead(Long notificationId) throws DAOException {
        validateIdInput(notificationId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_AS_READ)) {

                stmt.setLong(1, notificationId);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification marked as read: {0}", notificationId);
                }

                return affectedRows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during mark as read", e);
            throw new DAOException("Error marking notification as read", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int markAllAsReadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_ALL_AS_READ_BY_USER)) {

                stmt.setLong(1, userId);

                int affectedRows = stmt.executeUpdate();

                logger.log(Level.INFO, "Marked {0} notifications as read for user {1}",
                        new Object[]{affectedRows, userId});

                return affectedRows;
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
    public int markAsReadByBookingAndUser(Long bookingId, Long userId) throws DAOException {
        validateIdInput(bookingId);
        validateIdInput(userId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_AS_READ_BY_BOOKING_AND_USER)) {

                stmt.setLong(1, bookingId);
                stmt.setLong(2, userId);

                int affectedRows = stmt.executeUpdate();

                logger.log(Level.INFO, "Marked {0} notifications as read for booking {1} and user {2}",
                        new Object[]{affectedRows, bookingId, userId});

                return affectedRows;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during mark as read by booking", e);
            throw new DAOException("Error marking notifications as read by booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(Notification notification) throws DAOException {
        if (notification == null || notification.getId() == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION);
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_NOTIFICATION)) {

                setNotificationParameters(stmt, notification, 1);
                stmt.setLong(7, notification.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification updated successfully: {0}", notification.getId());
                }

                return affectedRows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notification update", e);
            throw new DAOException("Error updating notification", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteById(Long id) throws DAOException {
        validateIdInput(id);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_ID)) {

                stmt.setLong(1, id);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Notification deleted successfully: {0}", id);
                }

                return affectedRows > 0;
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
    public int deleteByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_USER)) {

                stmt.setLong(1, userId);

                int affectedRows = stmt.executeUpdate();

                logger.log(Level.INFO, "Deleted {0} notifications for user {1}",
                        new Object[]{affectedRows, userId});

                return affectedRows;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during notifications deletion by user", e);
            throw new DAOException("Error deleting notifications by user", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteReadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_READ_BY_USER)) {

                stmt.setLong(1, userId);

                int affectedRows = stmt.executeUpdate();

                logger.log(Level.INFO, "Deleted {0} read notifications for user {1}",
                        new Object[]{affectedRows, userId});

                return affectedRows;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during read notifications deletion", e);
            throw new DAOException("Error deleting read notifications", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> findAll() throws DAOException {
        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }

                logger.log(Level.INFO, "Retrieved {0} notifications", notifications.size());
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during all notifications retrieval", e);
            throw new DAOException("Error retrieving all notifications", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> findRecentByUserId(Long userId, int limit) throws DAOException {
        validateIdInput(userId);
        if (limit <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_LIMIT);
        }

        List<Notification> notifications = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_RECENT_BY_USER)) {

                stmt.setLong(1, userId);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapResultSetToNotification(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} recent notifications for user {1}",
                        new Object[]{notifications.size(), userId});
                return notifications;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during recent notifications retrieval", e);
            throw new DAOException("Error retrieving recent notifications", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Sets the common notification parameters in a PreparedStatement.
     * <p>
     * This helper method eliminates code duplication between save() and update() methods
     * by centralizing the logic for setting notification fields.
     * </p>
     *
     * @param stmt The PreparedStatement to populate
     * @param notification The Notification object containing the data
     * @param startIndex The starting parameter index (1 for save, varies for update)
     * @throws SQLException If there is an error setting parameters
     */
    private void setNotificationParameters(PreparedStatement stmt, Notification notification, int startIndex)
            throws SQLException {
        stmt.setLong(startIndex, notification.getUserId());
        stmt.setString(startIndex + 1, notification.getUserType().name());
        stmt.setString(startIndex + 2, notification.getType().name());
        stmt.setString(startIndex + 3, notification.getMessage());

        if (notification.getRelatedBookingId() != null) {
            stmt.setLong(startIndex + 4, notification.getRelatedBookingId());
        } else {
            stmt.setNull(startIndex + 4, java.sql.Types.BIGINT);
        }

        stmt.setBoolean(startIndex + 5, notification.isRead());
    }

    /**
     * Maps a ResultSet row to a Notification domain object.
     */
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Long relatedBookingId = rs.getLong("related_booking_id");
        if (rs.wasNull()) {
            relatedBookingId = null;
        }

        return new Notification.Builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .userType(UserType.valueOf(rs.getString("user_type")))
                .type(NotificationType.valueOf(rs.getString("type")))
                .message(rs.getString("message"))
                .relatedBookingId(relatedBookingId)
                .isRead(rs.getBoolean("is_read"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

}