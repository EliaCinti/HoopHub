package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CSV implementation of {@link NotificationDao}.
 *
 * <p>Persists notifications to {@code notifications.csv}. Returns notifications
 * sorted by creation date (newest first).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NotificationDaoCsv extends AbstractCsvDao implements NotificationDao {

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "notifications.csv";
    private static final String[] CSV_HEADER = {
            "id", "user_id", "user_type", "type", "message",
            "related_booking_id", "is_read", "created_at"
    };

    private static final String NOTIFICATION = "Notification";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int COL_ID = 0;
    private static final int COL_USER_ID = 1;
    private static final int COL_USER_TYPE = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_MESSAGE = 4;
    private static final int COL_RELATED_BOOKING_ID = 5;
    private static final int COL_IS_READ = 6;
    private static final int COL_CREATED_AT = 7;

    public NotificationDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized Notification saveNotification(Notification notification) throws DAOException {
        validateNotNull(notification, NOTIFICATION);

        int id = notification.getId();
        if (id == 0) {
            id = (int) getNextId(COL_ID);
        }

        Notification savedNotification = new Notification.Builder()
                .from(notification)
                .id(id)
                .build();

        String bookingIdStr = savedNotification.getBookingId() == null ? "" : String.valueOf(savedNotification.getBookingId());

        String[] newRow = {
                String.valueOf(id),
                savedNotification.getUsername(),
                savedNotification.getUserType().name(),
                savedNotification.getType().name(),
                savedNotification.getMessage(),
                bookingIdStr,
                String.valueOf(savedNotification.isRead()),
                savedNotification.getCreatedAt().format(DATE_TIME_FORMATTER)
        };

        CsvUtilities.writeFile(csvFile, newRow);
        putInCache(savedNotification, id);
        notifyObservers(DaoOperation.INSERT, NOTIFICATION, String.valueOf(id), savedNotification);

        return savedNotification;
    }

    @Override
    public synchronized Notification retrieveNotification(int id) throws DAOException {
        validatePositiveId(id);

        Notification cached = getFromCache(Notification.class, id);
        if (cached != null) return cached;

        String[] row = findRowByValue(COL_ID, String.valueOf(id));
        if (row == null || row.length == 0) return null;

        Notification notification = mapRowToNotification(row);
        putInCache(notification, id);

        return notification;
    }

    @Override
    public synchronized List<Notification> getNotificationsForUser(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, "UserType");

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row[COL_USER_ID].equals(username) && row[COL_USER_TYPE].equals(userType.name())) {
                notifications.add(resolveNotificationFromRow(row));
            }
        }
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return notifications;
    }

    @Override
    public synchronized List<Notification> getUnreadNotificationsForUser(String username, UserType userType) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);
        validateNotNull(userType, "UserType");

        List<Notification> notifications = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row[COL_USER_ID].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name()) &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                notifications.add(resolveNotificationFromRow(row));
            }
        }
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return notifications;
    }

    @Override
    public synchronized void updateNotification(Notification notification) throws DAOException {
        validateNotNull(notification, NOTIFICATION);
        validatePositiveId(notification.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (Integer.parseInt(row[COL_ID]) == notification.getId()) {
                row[COL_IS_READ] = String.valueOf(notification.isRead());
                found = true;
                break;
            }
        }

        if (!found) throw new DAOException("Notification not found: " + notification.getId());

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        putInCache(notification, notification.getId());
        notifyObservers(DaoOperation.UPDATE, NOTIFICATION, String.valueOf(notification.getId()), notification);
    }

    @Override
    public synchronized void markAllAsReadForUser(String username, UserType userType) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean changed = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row[COL_USER_ID].equals(username) &&
                    row[COL_USER_TYPE].equals(userType.name()) &&
                    !Boolean.parseBoolean(row[COL_IS_READ])) {
                row[COL_IS_READ] = "true";
                changed = true;
            }
        }
        if (changed) CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
    }

    @Override
    public synchronized void deleteNotification(Notification notification) throws DAOException {
        validateNotNull(notification, NOTIFICATION);
        int id = notification.getId();
        validatePositiveId(id);

        boolean found = deleteById(id, COL_ID);
        if (!found) throw new DAOException("Notification not found");

        removeFromCache(Notification.class, id);
        notifyObservers(DaoOperation.DELETE, NOTIFICATION, String.valueOf(id), null);
    }

    @Override
    public synchronized void deleteNotificationsByBooking(int bookingId) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<String[]> remaining = new ArrayList<>();
        if (!data.isEmpty()) remaining.add(data.getFirst());

        boolean changed = false;
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            String bId = row[COL_RELATED_BOOKING_ID];
            if (bId == null || bId.isEmpty() || Integer.parseInt(bId) != bookingId) {
                remaining.add(row);
            } else {
                changed = true;
                removeFromCache(Notification.class, Integer.parseInt(row[COL_ID]));
            }
        }
        if (changed) CsvUtilities.updateFile(csvFile, CSV_HEADER, remaining);
    }

    @Override
    public synchronized int getUnreadCount(String u, UserType t) throws DAOException {
        return getUnreadNotificationsForUser(u, t).size();
    }

    // ========== PRIVATE HELPERS ==========

    private Notification resolveNotificationFromRow(String[] row) throws DAOException {
        try {
            int id = Integer.parseInt(row[COL_ID]);
            Notification cached = getFromCache(Notification.class, id);
            if (cached != null) return cached;

            Notification n = mapRowToNotification(row);
            putInCache(n, id);
            return n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Notification mapRowToNotification(String[] row) throws DAOException {
        if (!isValidRow(row)) {
            throw new DAOException("Attempted to map an invalid CSV row to Notification");
        }

        try {
            int id = Integer.parseInt(row[COL_ID]);
            String username = row[COL_USER_ID];
            UserType userType = UserType.valueOf(row[COL_USER_TYPE]);
            NotificationType notificationType = NotificationType.valueOf(row[COL_TYPE]);
            String message = row[COL_MESSAGE];

            String bIdStr = row[COL_RELATED_BOOKING_ID];
            Integer bookingId = (bIdStr == null || bIdStr.isEmpty()) ? null : Integer.parseInt(bIdStr);

            boolean isRead = Boolean.parseBoolean(row[COL_IS_READ]);
            LocalDateTime createdAt = LocalDateTime.parse(row[COL_CREATED_AT], DATE_TIME_FORMATTER);

            return new Notification.Builder()
                    .id(id)
                    .username(username)
                    .userType(userType)
                    .type(notificationType)
                    .message(message)
                    .bookingId(bookingId)
                    .isRead(isRead)
                    .createdAt(createdAt)
                    .build();

        } catch (Exception e) {
            throw new DAOException("Error mapping notification CSV row", e);
        }
    }
}