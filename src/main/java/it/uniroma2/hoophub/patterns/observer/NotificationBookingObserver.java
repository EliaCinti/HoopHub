package it.uniroma2.hoophub.patterns.observer;

import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Notification;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.sync.SyncContext;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete observer implementing the <b>Observer pattern (GoF)</b>.
 *
 * <p>Listens for Booking changes and automatically creates notifications:
 * <ul>
 *   <li>New booking → notifies VenueManager</li>
 *   <li>Status CONFIRMED/REJECTED → notifies Fan</li>
 * </ul>
 * Uses Model-First approach with domain objects.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NotificationBookingObserver implements DaoObserver {

    private static final Logger logger = Logger.getLogger(NotificationBookingObserver.class.getName());

    @Override
    public void onAfterInsert(String entityType, String entityId, Object entity) {
        if (!"Booking".equals(entityType)) return;
        if (SyncContext.isSyncing()) return;

        try {
            Booking booking = (Booking) entity;
            logger.log(Level.INFO, "NOTIFICATION OBSERVER: New booking created (ID: {0}), notifying VenueManager", entityId);
            createVenueManagerNotification(booking);
        } catch (ClassCastException e) {
            logger.log(Level.SEVERE, "Invalid entity type for booking observer: expected Booking model", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create notification for new booking", e);
        }
    }

    @Override
    public void onAfterUpdate(String entityType, String entityId, Object entity) {
        if (!"Booking".equals(entityType)) return;
        if (SyncContext.isSyncing()) return;

        try {
            Booking booking = (Booking) entity;
            BookingStatus status = booking.getStatus();

            if (status == BookingStatus.CONFIRMED || status == BookingStatus.REJECTED) {
                logger.log(Level.INFO, "NOTIFICATION OBSERVER: Booking status changed to {0} (ID: {1}), notifying Fan",
                        new Object[]{status, entityId});
                createFanNotification(booking);
            }
        } catch (ClassCastException e) {
            logger.log(Level.SEVERE, "Invalid entity type for booking observer", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create notification for booking update", e);
        }
    }

    @Override
    public void onAfterDelete(String entityType, String entityId) {
        // Not implemented
    }

    private void createVenueManagerNotification(Booking booking) throws DAOException {
        NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();
        String venueManagerUsername = getVenueManagerUsernameForVenue(booking.getVenueId());

        String message = String.format(
                "New booking request for %s vs %s on %s",
                booking.getHomeTeam().getDisplayName(),
                booking.getAwayTeam().getDisplayName(),
                booking.getGameDate()
        );

        Notification notification = new Notification.Builder()
                .username(venueManagerUsername)
                .userType(UserType.VENUE_MANAGER)
                .type(NotificationType.BOOKING_REQUESTED)
                .message(message)
                .bookingId(booking.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationDao.saveNotification(notification);
        logger.log(Level.INFO, "Notification created for VenueManager: {0}", venueManagerUsername);
    }

    private void createFanNotification(Booking booking) throws DAOException {
        NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();
        String fanUsername = booking.getFanUsername();

        NotificationType notificationType;
        String message;

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            notificationType = NotificationType.BOOKING_APPROVED;
            message = String.format(
                    "Great news! Your booking for %s vs %s has been APPROVED!",
                    booking.getHomeTeam().getDisplayName(),
                    booking.getAwayTeam().getDisplayName()
            );
        } else {
            notificationType = NotificationType.BOOKING_REJECTED;
            message = String.format(
                    "Sorry, your booking for %s vs %s has been REJECTED.",
                    booking.getHomeTeam().getDisplayName(),
                    booking.getAwayTeam().getDisplayName()
            );
        }

        Notification notification = new Notification.Builder()
                .username(fanUsername)
                .userType(UserType.FAN)
                .type(notificationType)
                .message(message)
                .bookingId(booking.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationDao.saveNotification(notification);
        logger.log(Level.INFO, "Notification created for Fan: {0}", fanUsername);
    }

    private String getVenueManagerUsernameForVenue(int venueId) throws DAOException {
        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();
        Venue venue = venueDao.retrieveVenue(venueId);
        return venue.getVenueManagerUsername();
    }
}