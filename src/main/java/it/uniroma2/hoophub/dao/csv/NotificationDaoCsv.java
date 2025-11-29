package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.utility_dao.CsvUtilities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of NotificationDao.
 * <p>
 * Manages Notification data in CSV file.
 * </p>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Factory</strong>: Created via NotificationDaoFactory</li>
 *   <li><strong>Observer</strong>: Notifies observers for CSV-MySQL sync</li>
 *   <li><strong>Builder</strong>: Uses Notification.Builder for object construction</li>
 *   <li><strong>Template Method</strong>: Extends AbstractCsvDao</li>
 * </ul>
 * </p>
 *
 * @see NotificationDao
 */
public class NotificationDaoCsv extends AbstractCsvDao implements NotificationDao {

    // ========== CSV FILE CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "notifications.csv";
    private static final String[] CSV_HEADER = {
            "id", "username", "user_type", "notification_type", "message",
            "related_booking_id", "is_read", "created_at"
    };

    // ========== CONSTANTS ==========
    private static final String NOTIFICATION = "Notification";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USER_TYPE = "UserType";

    // ========== COLUMN INDICES ==========

    private static final int COL_ID = 0;
    private static final int COL_USERNAME = 1;
    private static final int COL_USER_TYPE = 2;
    private static final int COL_NOTIFICATION_TYPE = 3;
    private static final int COL_MESSAGE = 4;
    private static final int COL_RELATED_BOOKING_ID = 5;
    private static final int COL_IS_READ = 6;
    private static final int COL_CREATED_AT = 7;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructs a new NotificationDaoCsv.
     * <p>
     * Initializes the CSV file if it doesn't exist (handled by AbstractCsvDao).
     * </p>
     */
    public NotificationDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC DAO METHODS ==========

    @Override
    public synchronized void saveNotification(NotificationBean notificationBean) throws DAOException {
        validateNotNull(notificationBean, "NotificationBean");

        // Generate ID if not present
        int id = notificationBean.getId() == 0 ? (int) getNextId(COL_ID) : notificationBean.getId();

        String[] newRow = {
                String.valueOf(id),
                notificationBean.getUsername(),
                notificationBean.getUserType().name(),
                notificationBean.getType().name(),
                notificationBean.getMessage(),
                notificationBean.getRelatedBookingId() == 0 ? "" : String.valueOf(notificationBean.getRelatedBookingId()),
                String.valueOf(notificationBean.isRead()),
                notificationBean.getCreatedAt().format(DATE_TIME_FORMATTER)
        };

        CsvUtilities.writeFile(csvFile, newRow);

        notifyObservers(DaoOperation.INSERT, NOTIFICATION, String.valueOf(id), notificationBean);
    }

    @Override
    public synchronized Notification retrieveNotification(int id) throws DAOException {
        validatePositiveId(id);

        String[] row = findRowByValue(COL_ID, String.valueOf(id));
        if (row == null) {
            return null;
        }

        return mapRowToNotification(row);
    }

    @Override
    public synchronized List<Notification> getNotificationsForUser(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, USER_TYPE);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name())) {
                notifications.add(mapRowToNotification(row));
            }
        }

        // Sort by createdAt descending (newest first)
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());

        logger.log(Level.FINE, "Retrieved {0} notifications for user {1}",
                new Object[]{notifications.size(), username});
        return notifications;
    }

    @Override
    public synchronized List<Notification> getUnreadNotificationsForUser(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, USER_TYPE);

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name()) &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                notifications.add(mapRowToNotification(row));
            }
        }

        // Sort by createdAt descending (newest first)
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());

        logger.log(Level.FINE, "Retrieved {0} unread notifications for user {1}",
                new Object[]{notifications.size(), username});
        return notifications;
    }

    @Override
    public synchronized void markAsRead(int notificationId) throws DAOException {
        validatePositiveId(notificationId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == notificationId) {
                row[COL_IS_READ] = "true";
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    NOTIFICATION, "mark as read", notificationId));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Notification marked as read: {0}", notificationId);

        // Create a bean for notification (we don't need to full object for update notification)
        NotificationBean bean = new NotificationBean.Builder()
                .id(notificationId)
                .isRead(true)
                .build();

        notifyObservers(DaoOperation.UPDATE, NOTIFICATION, String.valueOf(notificationId), bean);
    }

    @Override
    public synchronized void markAllAsReadForUser(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, USER_TYPE);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean anyUpdated = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name()) &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                row[COL_IS_READ] = "true";
                anyUpdated = true;
            }
        }

        if (anyUpdated) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
            logger.log(Level.INFO, "Marked all notifications as read for user {0}", username);
        }
    }

    @Override
    public synchronized void deleteNotification(int notificationId) throws DAOException {
        validatePositiveId(notificationId);

        boolean found = deleteById(notificationId, COL_ID);

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    NOTIFICATION, "deletion", notificationId));
        }

        logger.log(Level.INFO, "Notification deleted successfully: {0}", notificationId);
        notifyObservers(DaoOperation.DELETE, NOTIFICATION, String.valueOf(notificationId), null);
    }

    @Override
    public synchronized void deleteNotificationsByBooking(int bookingId) throws DAOException {
        validatePositiveId(bookingId);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> remainingData = new ArrayList<>();

        // Keep header
        if (!data.isEmpty()) {
            remainingData.add(data.getFirst());
        }

        // Filter out notifications related to this booking
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            String relatedBookingIdStr = row[COL_RELATED_BOOKING_ID];

            // Keep the row if it's not related to this booking
            if (relatedBookingIdStr.isEmpty() || Integer.parseInt(relatedBookingIdStr) != bookingId) {
                remainingData.add(row);
            }
        }

        // Update file only if something was deleted
        if (remainingData.size() < data.size()) {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, remainingData);
            logger.log(Level.INFO, "Deleted notifications for booking: {0}", bookingId);
        }
    }

    @Override
    public synchronized int getUnreadCount(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, USER_TYPE);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        int count = 0;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name()) &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                count++;
            }
        }

        return count;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a CSV row to a Notification domain object.
     *
     * @param row The CSV row data
     * @return A Notification object
     * @throws DAOException If the row data is invalid or cannot be parsed
     */
    private Notification mapRowToNotification(String[] row) throws DAOException {
        try {
            int id = Integer.parseInt(row[COL_ID]);
            String username = row[COL_USERNAME];
            UserType userType = UserType.valueOf(row[COL_USER_TYPE]);
            NotificationType notificationType = NotificationType.valueOf(row[COL_NOTIFICATION_TYPE]);
            String message = row[COL_MESSAGE];
            String relatedBookingIdStr = row[COL_RELATED_BOOKING_ID];
            int relatedBookingId = relatedBookingIdStr.isEmpty() ? 0 : Integer.parseInt(relatedBookingIdStr);
            boolean isRead = Boolean.parseBoolean(row[COL_IS_READ]);
            LocalDateTime createdAt = LocalDateTime.parse(row[COL_CREATED_AT], DATE_TIME_FORMATTER);

            return new Notification.Builder()
                    .id(id)
                    .username(username)
                    .userType(userType)
                    .type(notificationType)
                    .message(message)
                    .relatedBookingId(relatedBookingId)
                    .isRead(isRead)
                    .createdAt(createdAt)
                    .build();

        } catch (Exception e) {
            throw new DAOException("Error mapping notification data from CSV row", e);
        }
    }
}
