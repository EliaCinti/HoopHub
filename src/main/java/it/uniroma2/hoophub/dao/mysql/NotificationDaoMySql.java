package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link NotificationDao}.
 *
 * <p>Supports UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) for cross-persistence
 * synchronization to handle ID conflicts gracefully.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class NotificationDaoMySql extends AbstractMySqlDao implements NotificationDao {

    private static final String NOTIFICATION = "Notification";
    private static final String COLUMN_LIST = "id, user_id, user_type, type, message, related_booking_id, is_read, created_at";
    private static final String SELECT = "SELECT ";

    // UPSERT: Insert or Update if ID exists (critical for sync)
    private static final String SQL_UPSERT =
            "INSERT INTO notifications (id, user_id, user_type, type, message, related_booking_id, is_read, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "user_id = VALUES(user_id), " +
                    "user_type = VALUES(user_type), " +
                    "type = VALUES(type), " +
                    "message = VALUES(message), " +
                    "related_booking_id = VALUES(related_booking_id), " +
                    "is_read = VALUES(is_read), " +
                    "created_at = VALUES(created_at)";

    // Standard INSERT for auto-generated ID
    private static final String SQL_INSERT_AUTO_ID =
            "INSERT INTO notifications (user_id, user_type, type, message, related_booking_id, is_read, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
            SELECT + COLUMN_LIST + " FROM notifications WHERE id = ?";

    private static final String SQL_SELECT_BY_USER =
            SELECT + COLUMN_LIST + " FROM notifications WHERE user_id = ? AND user_type = ? ORDER BY created_at DESC";

    private static final String SQL_SELECT_UNREAD_BY_USER =
            SELECT + COLUMN_LIST + " FROM notifications WHERE user_id = ? AND user_type = ? AND is_read = FALSE ORDER BY created_at DESC";

    private static final String SQL_UPDATE =
            "UPDATE notifications SET is_read = ? WHERE id = ?";

    private static final String SQL_MARK_ALL_READ =
            "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND user_type = ?";

    private static final String SQL_DELETE =
            "DELETE FROM notifications WHERE id = ?";

    private static final String SQL_DELETE_BY_BOOKING =
            "DELETE FROM notifications WHERE related_booking_id = ?";

    private static final String SQL_COUNT_UNREAD =
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND user_type = ? AND is_read = FALSE";

    private static final String SQL_RETRIEVE_ALL =
            SELECT + COLUMN_LIST + " FROM notifications ORDER BY created_at DESC";

    @Override
    public Notification saveNotification(Notification notification) throws DAOException {
        if (notification == null) throw new IllegalArgumentException("Notification cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            int newId;

            if (notification.getId() > 0) {
                // ID provided (from sync) - use UPSERT
                newId = saveWithUpsert(conn, notification);
            } else {
                // No ID - let MySQL generate one
                newId = saveWithAutoId(conn, notification);
            }

            Notification savedNotification = new Notification.Builder()
                    .from(notification)
                    .id(newId)
                    .build();

            conn.commit();

            putInCache(savedNotification, newId);
            logger.log(Level.INFO, "Notification saved with ID: {0}", newId);
            notifyObservers(DaoOperation.INSERT, NOTIFICATION, String.valueOf(newId), savedNotification);

            return savedNotification;

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving notification", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    private int saveWithUpsert(Connection conn, Notification notification) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, notification.getId());
            stmt.setString(2, notification.getUsername());
            stmt.setString(3, notification.getUserType().name());
            stmt.setString(4, notification.getType().name());
            stmt.setString(5, notification.getMessage());

            if (notification.getBookingId() != null) {
                stmt.setInt(6, notification.getBookingId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.setBoolean(7, notification.isRead());
            stmt.setTimestamp(8, Timestamp.valueOf(notification.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new DAOException("Creating notification failed, no rows affected.");
            }

            if (affectedRows == 2) {
                logger.log(Level.FINE, "Notification ID {0} updated via UPSERT", notification.getId());
            }

            return notification.getId();
        }
    }

    private int saveWithAutoId(Connection conn, Notification notification) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_AUTO_ID, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, notification.getUsername());
            stmt.setString(2, notification.getUserType().name());
            stmt.setString(3, notification.getType().name());
            stmt.setString(4, notification.getMessage());

            if (notification.getBookingId() != null) {
                stmt.setInt(5, notification.getBookingId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setBoolean(6, notification.isRead());
            stmt.setTimestamp(7, Timestamp.valueOf(notification.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new DAOException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new DAOException("Creating notification failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public Notification retrieveNotification(int id) throws DAOException {
        Notification cached = getFromCache(Notification.class, id);
        if (cached != null) return cached;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Notification n = mapResultSetToNotification(rs);
                    putInCache(n, id);
                    return n;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving notification", e);
        }
    }

    @Override
    public List<Notification> getNotificationsForUser(String u, UserType t) throws DAOException {
        return executeQuery(SQL_SELECT_BY_USER, u, t);
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(String u, UserType t) throws DAOException {
        return executeQuery(SQL_SELECT_UNREAD_BY_USER, u, t);
    }

    @Override
    public void updateNotification(Notification notification) throws DAOException {
        if (notification == null) throw new IllegalArgumentException("Notification cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
                stmt.setBoolean(1, notification.isRead());
                stmt.setInt(2, notification.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(notification, notification.getId());
                    logger.log(Level.INFO, "Notification updated: {0}", notification.getId());
                    notifyObservers(DaoOperation.UPDATE, NOTIFICATION, String.valueOf(notification.getId()), notification);
                } else {
                    conn.rollback();
                    throw new DAOException("Notification not found: " + notification.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating notification", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void markAllAsReadForUser(String u, UserType t) throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_MARK_ALL_READ)) {
            stmt.setString(1, u);
            stmt.setString(2, t.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Error marking all read", e);
        }
    }

    @Override
    public List<Notification> retrieveAllNotifications() throws DAOException {
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_RETRIEVE_ALL)) {

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all notifications", e);
        }
        return notifications;
    }

    @Override
    public void deleteNotification(Notification notification) throws DAOException {
        if (notification == null) throw new IllegalArgumentException("Notification cannot be null");
        int id = notification.getId();

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    removeFromCache(Notification.class, id);
                    logger.log(Level.INFO, "Notification deleted: {0}", id);
                    notifyObservers(DaoOperation.DELETE, NOTIFICATION, String.valueOf(id), null);
                } else {
                    conn.rollback();
                    throw new DAOException("Notification not found: " + id);
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting notification", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void deleteNotificationsByBooking(int bid) throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_BOOKING)) {
            stmt.setInt(1, bid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Error deleting notifications by booking", e);
        }
    }

    @Override
    public int getUnreadCount(String u, UserType t) throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_UNREAD)) {
            stmt.setString(1, u);
            stmt.setString(2, t.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Error counting unread", e);
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int relatedBookingId = rs.getInt("related_booking_id");
        Integer bookingIdVal = rs.wasNull() ? null : relatedBookingId;

        return new Notification.Builder()
                .id(id)
                .username(rs.getString("user_id"))
                .userType(UserType.valueOf(rs.getString("user_type")))
                .type(NotificationType.valueOf(rs.getString("type")))
                .message(rs.getString("message"))
                .bookingId(bookingIdVal)
                .isRead(rs.getBoolean("is_read"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    private List<Notification> executeQuery(String sql, String username, UserType type) throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) list.add(mapResultSetToNotification(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DAOException("Error fetching notifications", e);
        }
    }
}