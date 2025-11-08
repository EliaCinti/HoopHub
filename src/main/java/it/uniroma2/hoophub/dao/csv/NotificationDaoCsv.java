package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.NotificationDAO;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.NotificationType;
import it.uniroma2.hoophub.utilities.UserType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * CSV implementation of the NotificationDAO interface.
 * <p>
 * This class provides data access operations for Notification entities stored in CSV files.
 * It extends {@link AbstractCsvDao} to leverage common functionality like file initialization,
 * ID generation, and validation, eliminating code duplication.
 * </p>
 * <p>
 * <strong>CSV File Structure (notifications.csv):</strong>
 * <pre>
 * id,user_id,user_type,type,message,related_booking_id,is_read,created_at
 * 1,john_doe,FAN,BOOKING_APPROVED,Your booking has been approved,5,false,2025-11-02T10:30:00
 * 2,manager1,VENUE_MANAGER,BOOKING_REQUESTED,New booking request received,6,false,2025-11-02T11:00:00
 * </pre>
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> This DAO handles notifications for both Fans and VenueManagers,
 * using the user_id (username) and user_type fields to distinguish between them. No circular
 * dependencies are created since notifications only store IDs/usernames, not full objects.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> All public methods are synchronized to prevent concurrent
 * modification issues when multiple threads access the CSV file.
 * </p>
 * <p>
 * <strong>Note:</strong> The NotificationDAO interface uses Long for IDs (for MySQL compatibility),
 * but user_id is stored as a String (username) in the CSV implementation. The interface methods
 * that take Long userId are interpreted as hash codes or unique identifiers for users.
 * </p>
 *
 * @see NotificationDAO Interface defining the contract
 * @see AbstractCsvDao Base class providing common CSV functionality
 * @see Notification Domain model representing a notification
 */
public class NotificationDaoCsv extends AbstractCsvDao implements NotificationDAO {

    // ========== CSV CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "notifications.csv";
    private static final String[] CSV_HEADER = {
            "id", "user_id", "user_type", "type", "message",
            "related_booking_id", "is_read", "created_at"
    };

    // ========== COLUMN INDICES ==========

    private static final int COL_ID = 0;
    private static final int COL_USER_ID = 1;
    private static final int COL_USER_TYPE = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_MESSAGE = 4;
    private static final int COL_RELATED_BOOKING_ID = 5;
    private static final int COL_IS_READ = 6;
    private static final int COL_CREATED_AT = 7;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructs a new NotificationDaoCsv and initializes the CSV file.
     * <p>
     * The parent constructor ({@link AbstractCsvDao}) handles:
     * <ul>
     *   <li>Creating the File object</li>
     *   <li>Creating parent directories if needed</li>
     *   <li>Initializing the CSV file with headers if it doesn't exist</li>
     *   <li>Setting up the logger</li>
     * </ul>
     * </p>
     */
    public NotificationDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC METHODS (NotificationDAO Interface Implementation) ==========

    /**
     * {@inheritDoc}
     * <p>
     * If the notification has no ID, a new ID is automatically generated using
     * {@link AbstractCsvDao#getNextId(int)}.
     * </p>
     */
    @Override
    public synchronized Notification save(Notification notification) throws DAOException {
        validateNotNull(notification, "Notification");

        // Generate ID if not present
        long id = notification.getId() != null ? notification.getId() : getNextId(COL_ID);

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
        validatePositiveId(id);

        String[] row = findRowByValue(COL_ID, String.valueOf(id));
        if (row == null) {
            return Optional.empty();
        }

        return Optional.of(mapRowToNotification(row));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Notification> findByUserId(Long userId) throws DAOException {
        validatePositiveId(userId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_USER_ID]) == userId) {
                notifications.add(mapRowToNotification(row));
            }
        }

        // Sort by created_at descending (most recent first)
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
        validatePositiveId(userId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(userId);
        validateNotNull(type, "NotificationType");

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(bookingId);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(notificationId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(bookingId);
        validatePositiveId(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validateNotNull(notification, "Notification");
        if (notification.getId() == null) {
            throw new IllegalArgumentException("Notification ID cannot be null for update");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Long.parseLong(row[COL_ID]) == notification.getId()) {
                row[COL_USER_ID] = String.valueOf(notification.getUserId());
                row[COL_USER_TYPE] = notification.getUserType().name();
                row[COL_TYPE] = notification.getType().name();
                row[COL_MESSAGE] = notification.getMessage();
                row[COL_RELATED_BOOKING_ID] = notification.getRelatedBookingId() != null ?
                        String.valueOf(notification.getRelatedBookingId()) : "";
                row[COL_IS_READ] = String.valueOf(notification.isRead());
                // Note: created_at is not updated (immutable timestamp)
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
     * <p>
     * This implementation uses the {@link AbstractCsvDao#deleteById(long, int)} helper method
     * to eliminate code duplication and ensure correct CSV file handling.
     * </p>
     */
    @Override
    public synchronized boolean deleteById(Long id) throws DAOException {
        validatePositiveId(id);

        boolean found = deleteById(id.longValue(), COL_ID);

        if (found) {
            logger.log(Level.INFO, "Notification deleted successfully: {0}", id);
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int deleteByUserId(Long userId) throws DAOException {
        validatePositiveId(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> toKeep = new ArrayList<>();

        // Keep header
        toKeep.add(data.get(CsvDaoConstants.HEADER_ROW));

        int count = 0;
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(userId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> toKeep = new ArrayList<>();

        // Keep header
        toKeep.add(data.get(CsvDaoConstants.HEADER_ROW));

        int count = 0;
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
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
        validatePositiveId(userId);
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
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
     * Maps CSV row data to a Notification domain object.
     * <p>
     * This method constructs a Notification using the Builder pattern, parsing all fields
     * from the CSV row including timestamps and enum types.
     * </p>
     *
     * @param row Array containing notification data
     * @return A fully constructed Notification object
     * @throws DAOException If there's an error parsing data or constructing the Notification
     */
    private Notification mapRowToNotification(String[] row) throws DAOException {
        try {
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
        } catch (IllegalArgumentException e) {
            // Catches NumberFormatException (subclass) and other IllegalArgumentExceptions
            throw new DAOException("Error parsing notification data: " + e.getMessage(), e);
        }
    }
}
