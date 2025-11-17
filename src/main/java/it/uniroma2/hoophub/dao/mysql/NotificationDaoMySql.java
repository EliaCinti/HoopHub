package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation of NotificationDao.
 * <p>
 * This class handles notification persistence in MySQL database following the
 * same patterns as other MySQL DAO implementations. It extends AbstractMySqlDao
 * for common database operations and implements NotificationDao interface.
 * </p>
 *
 * @author Elia Cinti
 */
public class NotificationDaoMySql extends AbstractMySqlDao implements NotificationDao {

    private static final String TABLE_NAME = "notifications";

    @Override
    public void saveNotification(NotificationBean notificationBean) throws DAOException {
        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, user_type, type, message, related_booking_id, is_read, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, notificationBean.getUserId());
                pstmt.setString(2, notificationBean.getUserType().name());
                pstmt.setString(3, notificationBean.getType().name());
                pstmt.setString(4, notificationBean.getMessage());

                if (notificationBean.getRelatedBookingId() != null) {
                    pstmt.setLong(5, notificationBean.getRelatedBookingId());
                } else {
                    pstmt.setNull(5, Types.BIGINT);
                }

                pstmt.setBoolean(6, notificationBean.isRead());
                pstmt.setTimestamp(7, Timestamp.valueOf(notificationBean.getCreatedAt()));

                pstmt.executeUpdate();

                // Retrieve generated ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long generatedId = generatedKeys.getLong(1);
                        notificationBean.setId(generatedId);

                        // Notify observers for cross-persistence synchronization
                        notifyObservers(DaoOperation.INSERT, "Notification", generatedId.toString(), notificationBean);
                    } else {
                        throw new DAOException("Saving notification failed, no ID obtained.");
                    }
                }
            }
            return null;
        });
    }

    @Override
    public Notification retrieveNotification(Long id) throws DAOException {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";

        return executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return convertResultSetToNotification(rs);
                    } else {
                        throw new DAOException("Notification not found with id: " + id);
                    }
                }
            }
        });
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId, UserType userType) throws DAOException {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ? AND user_type = ? ORDER BY created_at DESC";

        return executeInTransaction(connection -> {
            List<Notification> notifications = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, userType.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(convertResultSetToNotification(rs));
                    }
                }
            }
            return notifications;
        });
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(Long userId, UserType userType) throws DAOException {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ? AND user_type = ? AND is_read = FALSE ORDER BY created_at DESC";

        return executeInTransaction(connection -> {
            List<Notification> notifications = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, userType.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(convertResultSetToNotification(rs));
                    }
                }
            }
            return notifications;
        });
    }

    @Override
    public void markAsRead(Long notificationId) throws DAOException {
        String sql = "UPDATE " + TABLE_NAME + " SET is_read = TRUE WHERE id = ?";

        executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, notificationId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new DAOException("Notification not found with id: " + notificationId);
                }

                // Retrieve updated notification and notify observers
                Notification updatedNotification = retrieveNotification(notificationId);
                notifyObservers(DaoOperation.UPDATE, "Notification", notificationId.toString(), updatedNotification);
            }
            return null;
        });
    }

    @Override
    public void markAllAsReadForUser(Long userId, UserType userType) throws DAOException {
        String sql = "UPDATE " + TABLE_NAME + " SET is_read = TRUE WHERE user_id = ? AND user_type = ? AND is_read = FALSE";

        executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, userType.name());
                pstmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void deleteNotification(Long notificationId) throws DAOException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";

        executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, notificationId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new DAOException("Notification not found with id: " + notificationId);
                }

                // Notify observers
                notifyObservers(DaoOperation.DELETE, "Notification", notificationId.toString());
            }
            return null;
        });
    }

    @Override
    public void deleteNotificationsByBooking(Long bookingId) throws DAOException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE related_booking_id = ?";

        executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, bookingId);
                pstmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public int getUnreadCount(Long userId, UserType userType) throws DAOException {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? AND user_type = ? AND is_read = FALSE";

        return executeInTransaction(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, userType.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }
            }
        });
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Converts a ResultSet row to Notification model object.
     */
    private Notification convertResultSetToNotification(ResultSet rs) throws SQLException {
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
