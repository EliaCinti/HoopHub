package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.NotificationDAO;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.NotificationType;
import it.uniroma2.hoophub.utilities.UserType;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CSV implementation of the NotificationDAO interface.
 * <p>
 * This class provides data access operations for Notification entities stored in CSV files.
 * It uses {@link CsvUtilities} for file I/O operations and provides the same functionality
 * as the MySQL implementation while maintaining thread safety through synchronized methods.
 * </p>
 * <p>
 * CSV file structure (notifications.csv):
 * <pre>
 * id,user_id,user_type,type,message,related_booking_id,is_read,created_at
 * 1,john_doe,FAN,BOOKING_APPROVED,Your booking has been approved,5,false,2025-11-02T10:30:00
 * </pre>
 * </p>
 *
 * @see NotificationDAO
 */
public class NotificationDaoCsv implements NotificationDAO {

    private static final Logger logger = Logger.getLogger(NotificationDaoCsv.class.getName());

    // CSV File configuration
    private static final String CSV_FILE_PATH = "data/notifications.csv";
    private static final String[] CSV_HEADER = {
            "id", "user_id", "user_type", "type", "message",
            "related_booking_id", "is_read", "created_at"
    };

    // Column indices
    private static final int COL_ID = 0;
    private static final int COL_USER_ID = 1;
    private static final int COL_USER_TYPE = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_MESSAGE = 4;
    private static final int COL_RELATED_BOOKING_ID = 5;
    private static final int COL_IS_READ = 6;
    private static final int COL_CREATED_AT = 7;

    // Error messages
    private static final String ERR_NULL_NOTIFICATION = "Notification cannot be null";
    private static final String ERR_INVALID_ID = "ID must be positive";
    private static final String ERR_INVALID_LIMIT = "Limit must be positive";

    private final File csvFile;

    /**
     * Constructs a new NotificationDaoCsv and initializes the CSV file.
     */
    public NotificationDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        initializeCsvFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Notification save(Notification notification) throws DAOException {
        if (notification == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION);
        }

        // Generate ID if not present
        long id = notification.getId() != null ? notification.getId() : getNextId();

        String[] newRow = {
                String.valueOf(id),
                String.valueOf(notification.getUserId()),
                notification.getUserType().name(),
                notification.getType().name(),
                notification.getMessage(),
                notification.getRelatedBookingId() != null ?
                        String.valueOf(notification.getRelatedBookingId()) : "",
                String.valueOf(notification.isRead()),
                notification.getCreatedAt().toString()
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "Notification saved successfully with ID: {0}", id);

        // Return notification with generated ID
        return new Notification.Builder()
                .from(notification)
                .id(id)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Optional<Notification> findById(Long id) throws DAOException {
        validateIdInput(id);

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_ID]) == id) {
                return Optional.of(mapRowToNotification(row));
            }
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId) {
                notifications.add(mapRowToNotification(row));
            }
        }

        // Sort by created_at descending
        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        logger.log(Level.INFO, "Retrieved {0} notifications for user {1}",
                new Object[]{notifications.size(), userId});
        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findUnreadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                notifications.add(mapRowToNotification(row));
            }
        }

        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        logger.log(Level.INFO, "Retrieved {0} unread notifications for user {1}",
                new Object[]{notifications.size(), userId});
        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findByUserIdAndType(Long userId, NotificationType type)
            throws DAOException {
        validateIdInput(userId);
        if (type == null) {
            throw new IllegalArgumentException("NotificationType cannot be null");
        }

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId &&
                    row[COL_TYPE].equals(type.name())) {
                notifications.add(mapRowToNotification(row));
            }
        }

        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        logger.log(Level.INFO, "Retrieved {0} {1} notifications for user {2}",
                new Object[]{notifications.size(), type, userId});
        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findByBookingId(Long bookingId) throws DAOException {
        validateIdInput(bookingId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (!row[COL_RELATED_BOOKING_ID].isEmpty() &&
                    Long.parseLong(row[COL_RELATED_BOOKING_ID]) == bookingId) {
                notifications.add(mapRowToNotification(row));
            }
        }

        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        logger.log(Level.INFO, "Retrieved {0} notifications for booking {1}",
                new Object[]{notifications.size(), bookingId});
        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int countUnreadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                count++;
            }
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean markAsRead(Long notificationId) throws DAOException {
        validateIdInput(notificationId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_ID]) == notificationId) {
                row[COL_IS_READ] = "true";
                found = true;
                break;
            }
        }

        if (found) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Notification marked as read: {0}", notificationId);
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int markAllAsReadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                row[COL_IS_READ] = "true";
                count++;
            }
        }

        if (count > 0) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Marked {0} notifications as read for user {1}",
                    new Object[]{count, userId});
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int markAsReadByBookingAndUser(Long bookingId, Long userId)
            throws DAOException {
        validateIdInput(bookingId);
        validateIdInput(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (!row[COL_RELATED_BOOKING_ID].isEmpty() &&
                    Long.parseLong(row[COL_RELATED_BOOKING_ID]) == bookingId &&
                    Long.parseLong(row[COL_USER_ID]) == userId &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                row[COL_IS_READ] = "true";
                count++;
            }
        }

        if (count > 0) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Marked {0} notifications as read for booking {1} and user {2}",
                    new Object[]{count, bookingId, userId});
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean update(Notification notification) throws DAOException {
        if (notification == null || notification.getId() == null) {
            throw new IllegalArgumentException(ERR_NULL_NOTIFICATION);
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_ID]) == notification.getId()) {
                row[COL_USER_ID] = String.valueOf(notification.getUserId());
                row[COL_USER_TYPE] = notification.getUserType().name();
                row[COL_TYPE] = notification.getType().name();
                row[COL_MESSAGE] = notification.getMessage();
                row[COL_RELATED_BOOKING_ID] = notification.getRelatedBookingId() != null ?
                        String.valueOf(notification.getRelatedBookingId()) : "";
                row[COL_IS_READ] = String.valueOf(notification.isRead());
                // Note: created_at is not updated
                found = true;
                break;
            }
        }

        if (found) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Notification updated successfully: {0}", notification.getId());
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean deleteById(Long id) throws DAOException {
        validateIdInput(id);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            if (Long.parseLong(data.get(i)[COL_ID]) == id) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (found) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Notification deleted successfully: {0}", id);
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int deleteByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> toKeep = new ArrayList<>();

        // Keep header
        toKeep.add(data.getFirst());

        int count = 0;
        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId) {
                count++;
            } else {
                toKeep.add(row);
            }
        }

        if (count > 0) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, toKeep);
            logger.log(Level.INFO, "Deleted {0} notifications for user {1}",
                    new Object[]{count, userId});
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int deleteReadByUserId(Long userId) throws DAOException {
        validateIdInput(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> toKeep = new ArrayList<>();

        // Keep header
        toKeep.add(data.getFirst());

        int count = 0;
        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId &&
                    Boolean.parseBoolean(row[COL_IS_READ])) {
                count++;
            } else {
                toKeep.add(row);
            }
        }

        if (count > 0) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, toKeep);
            logger.log(Level.INFO, "Deleted {0} read notifications for user {1}",
                    new Object[]{count, userId});
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findAll() throws DAOException {
        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            notifications.add(mapRowToNotification(data.get(i)));
        }

        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        logger.log(Level.INFO, "Retrieved {0} notifications", notifications.size());
        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findRecentByUserId(Long userId, int limit)
            throws DAOException {
        validateIdInput(userId);
        if (limit <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_LIMIT);
        }

        List<Notification> allNotifications = findByUserId(userId);

        // Return only the first 'limit' notifications (already sorted by date desc)
        List<Notification> recentNotifications = allNotifications.stream()
                .limit(limit)
                .collect(Collectors.toList());

        logger.log(Level.INFO, "Retrieved {0} recent notifications for user {1}",
                new Object[]{recentNotifications.size(), userId});
        return recentNotifications;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a CSV row to a Notification domain object.
     */
    private Notification mapRowToNotification(String[] row) {
        Long relatedBookingId = null;
        if (!row[COL_RELATED_BOOKING_ID].isEmpty()) {
            relatedBookingId = Long.parseLong(row[COL_RELATED_BOOKING_ID]);
        }

        return new Notification.Builder()
                .id(Long.parseLong(row[COL_ID]))
                .userId(Long.parseLong(row[COL_USER_ID]))
                .userType(UserType.valueOf(row[COL_USER_TYPE]))
                .type(NotificationType.valueOf(row[COL_TYPE]))
                .message(row[COL_MESSAGE])
                .relatedBookingId(relatedBookingId)
                .isRead(Boolean.parseBoolean(row[COL_IS_READ]))
                .createdAt(LocalDateTime.parse(row[COL_CREATED_AT]))
                .build();
    }

    /**
     * Generates the next available ID for a new notification.
     */
    private synchronized long getNextId() throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        long maxId = 0;

        for (int i = 1; i < data.size(); i++) {
            long id = Long.parseLong(data.get(i)[COL_ID]);
            if (id > maxId) {
                maxId = id;
            }
        }

        return maxId + 1;
    }

    /**
     * Initializes the CSV file if it doesn't exist.
     */
    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        logger.warning("Failed to create directories for CSV file");
                    }
                }

                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, CSV_HEADER, emptyData);
                logger.info("Initialized CSV file: " + CSV_FILE_PATH);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Failed to initialize CSV file", e);
        }
    }

    // ========== VALIDATION METHODS ==========

    private void validateIdInput(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_ID);
        }
    }
}
