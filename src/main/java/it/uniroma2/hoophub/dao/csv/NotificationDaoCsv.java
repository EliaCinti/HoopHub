package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CSV implementation of NotificationDao.
 * <p>
 * This class handles notification persistence in CSV format following the
 * same patterns as other CSV DAO implementations. It extends AbstractCsvDao
 * for common CSV operations and implements NotificationDao interface.
 * </p>
 *
 * @author Elia Cinti
 */
public class NotificationDaoCsv extends AbstractCsvDao implements NotificationDao {

    private static final String CSV_FILE_PATH = "data/notifications.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor initializes the CSV file with headers.
     */
    public NotificationDaoCsv() {
        super(CSV_FILE_PATH);
        initializeCsvFile();
    }

    @Override
    protected String getHeader() {
        return "id,userId,userType,type,message,relatedBookingId,isRead,createdAt";
    }

    @Override
    public void saveNotification(NotificationBean notificationBean) throws DAOException {
        try {
            Long id = getNextId();
            String csvRow = buildCsvRow(id, notificationBean);
            appendRowToCsv(csvRow);

            // Set generated ID back to bean
            notificationBean.setId(id);

            // Notify observers for cross-persistence synchronization
            notifyObservers(DaoOperation.INSERT, "Notification", id.toString(), notificationBean);

        } catch (IOException e) {
            throw new DAOException("Error saving notification to CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public Notification retrieveNotification(Long id) throws DAOException {
        try {
            String[] row = findRowByValue(0, id.toString()); // Column 0 is id
            if (row == null) {
                throw new DAOException("Notification not found with id: " + id);
            }
            return convertRowToNotification(row);
        } catch (IOException e) {
            throw new DAOException("Error retrieving notification: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId, UserType userType) throws DAOException {
        List<Notification> notifications = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length >= 8 && row[1].equals(userId.toString()) && row[2].equals(userType.name())) {
                    notifications.add(convertRowToNotification(row));
                }
            }
        } catch (IOException e) {
            throw new DAOException("Error retrieving notifications for user: " + e.getMessage(), e);
        }

        // Sort by createdAt descending (newest first)
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return notifications;
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(Long userId, UserType userType) throws DAOException {
        List<Notification> allNotifications = getNotificationsForUser(userId, userType);
        return allNotifications.stream()
                .filter(Notification::isUnread)
                .toList();
    }

    @Override
    public void markAsRead(Long notificationId) throws DAOException {
        try {
            Notification notification = retrieveNotification(notificationId);
            Notification updatedNotification = notification.markAsRead();

            // Update CSV row
            String[] oldRow = findRowByValue(0, notificationId.toString());
            if (oldRow != null) {
                oldRow[6] = "true"; // isRead column
                updateRow(0, notificationId.toString(), oldRow);
            }

            // Notify observers
            notifyObservers(DaoOperation.UPDATE, "Notification", notificationId.toString(), updatedNotification);

        } catch (IOException e) {
            throw new DAOException("Error marking notification as read: " + e.getMessage(), e);
        }
    }

    @Override
    public void markAllAsReadForUser(Long userId, UserType userType) throws DAOException {
        List<Notification> notifications = getUnreadNotificationsForUser(userId, userType);
        for (Notification notification : notifications) {
            markAsRead(notification.getId());
        }
    }

    @Override
    public void deleteNotification(Long notificationId) throws DAOException {
        try {
            deleteById(0, notificationId.toString());
            // Notify observers
            notifyObservers(DaoOperation.DELETE, "Notification", notificationId.toString());
        } catch (IOException e) {
            throw new DAOException("Error deleting notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteNotificationsByBooking(Long bookingId) throws DAOException {
        List<Notification> allNotifications = getAllNotifications();
        for (Notification notification : allNotifications) {
            if (notification.getRelatedBookingId() != null && notification.getRelatedBookingId().equals(bookingId)) {
                deleteNotification(notification.getId());
            }
        }
    }

    @Override
    public int getUnreadCount(Long userId, UserType userType) throws DAOException {
        return getUnreadNotificationsForUser(userId, userType).size();
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Builds a CSV row from NotificationBean.
     */
    private String buildCsvRow(Long id, NotificationBean bean) {
        return String.format("%d,%d,%s,%s,%s,%s,%s,%s",
                id,
                bean.getUserId(),
                bean.getUserType().name(),
                bean.getType().name(),
                escapeCsvField(bean.getMessage()),
                bean.getRelatedBookingId() != null ? bean.getRelatedBookingId().toString() : "",
                bean.isRead(),
                bean.getCreatedAt().format(DATE_TIME_FORMATTER)
        );
    }

    /**
     * Converts a CSV row to Notification model object.
     */
    private Notification convertRowToNotification(String[] row) {
        return new Notification.Builder()
                .id(Long.parseLong(row[0]))
                .userId(Long.parseLong(row[1]))
                .userType(UserType.valueOf(row[2]))
                .type(NotificationType.valueOf(row[3]))
                .message(unescapeCsvField(row[4]))
                .relatedBookingId(row[5].isEmpty() ? null : Long.parseLong(row[5]))
                .isRead(Boolean.parseBoolean(row[6]))
                .createdAt(LocalDateTime.parse(row[7], DATE_TIME_FORMATTER))
                .build();
    }

    /**
     * Escapes CSV field content (handles commas in messages).
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Unescapes CSV field content.
     */
    private String unescapeCsvField(String field) {
        if (field == null || field.isEmpty()) return "";
        if (field.startsWith("\"") && field.endsWith("\"")) {
            return field.substring(1, field.length() - 1).replace("\"\"", "\"");
        }
        return field;
    }

    /**
     * Gets all notifications (for internal operations).
     */
    private List<Notification> getAllNotifications() throws DAOException {
        List<Notification> notifications = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length >= 8) {
                    notifications.add(convertRowToNotification(row));
                }
            }
        } catch (IOException e) {
            throw new DAOException("Error retrieving all notifications: " + e.getMessage(), e);
        }
        return notifications;
    }
}
