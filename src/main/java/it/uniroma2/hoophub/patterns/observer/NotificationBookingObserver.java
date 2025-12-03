package it.uniroma2.hoophub.patterns.observer;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.NotificationBean;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.sync.SyncContext;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer that listens for Booking changes and automatically creates notifications.
 * <p>
 * This observer implements the Observer pattern (GoF) to achieve low coupling between
 * controllers and notification logic. Instead of controllers calling each other directly,
 * the BookingDao notifies this observer when bookings change, and the observer
 * handles notification creation automatically.
 * </p>
 * <p>
 * <strong>GRASP Principles Applied:</strong>
 * <ul>
 *   <li><strong>Low Coupling</strong>: Controllers don't know about each other</li>
 *   <li><strong>High Cohesion</strong>: This class only handles notification creation</li>
 *   <li><strong>Observer</strong>: Event-driven architecture for loose coupling</li>
 *   <li><strong>Information Expert</strong>: Has all info needed to create notifications</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Notification Flow:</strong>
 * <ul>
 *   <li>INSERT: New booking created → Notify VenueManager</li>
 *   <li>UPDATE: Booking status changed → Notify Fan (if CONFIRMED or REJECTED)</li>
 *   <li>DELETE: Not currently handled (could add if needed)</li>
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 */
public class NotificationBookingObserver implements DaoObserver {

    private static final Logger logger = Logger.getLogger(NotificationBookingObserver.class.getName());

    /**
     * Handles notification creation when a new booking is inserted.
     * <p>
     * When a Fan creates a booking request, this method automatically
     * creates a notification for the VenueManager who owns the venue.
     * </p>
     *
     * @param entityType The type of entity (should be "Booking")
     * @param entityId The booking ID
     * @param entity The BookingBean object
     */
    @Override
    public void onAfterInsert(String entityType, String entityId, Object entity) {
        // Only handle Booking entities
        if (!"Booking".equals(entityType)) {
            return;
        }

        // Ignore during sync to prevent infinite loops
        if (SyncContext.isSyncing()) {
            return;
        }

        try {
            BookingBean bookingBean = (BookingBean) entity;

            logger.log(Level.INFO, "NOTIFICATION OBSERVER: New booking created (ID: {0}), notifying VenueManager", entityId);

            // Create notification for VenueManager
            createVenueManagerNotification(bookingBean);

        } catch (ClassCastException e) {
            logger.log(Level.SEVERE, "Invalid entity type for booking observer", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create notification for new booking", e);
        }
    }

    /**
     * Handles notification creation when a booking status is updated.
     * <p>
     * When a VenueManager accepts or rejects a booking, this method
     * automatically creates a notification for the Fan who made the request.
     * </p>
     *
     * @param entityType The type of entity (should be "Booking")
     * @param entityId The booking ID
     * @param entity The BookingBean object
     */
    @Override
    public void onAfterUpdate(String entityType, String entityId, Object entity) {
        // Only handle Booking entities
        if (!"Booking".equals(entityType)) {
            return;
        }

        // Ignore during sync
        if (SyncContext.isSyncing()) {
            return;
        }

        try {
            BookingBean bookingBean = (BookingBean) entity;
            BookingStatus status = bookingBean.getStatus();

            // Only notify Fan if booking was CONFIRMED or REJECTED
            if (status == BookingStatus.CONFIRMED || status == BookingStatus.REJECTED) {
                logger.log(Level.INFO, "NOTIFICATION OBSERVER: Booking status changed to {0} (ID: {1}), notifying Fan",
                    new Object[]{status, entityId});

                createFanNotification(bookingBean);
            }

        } catch (ClassCastException e) {
            logger.log(Level.SEVERE, "Invalid entity type for booking observer", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create notification for booking update", e);
        }
    }

    /**
     * Handles notification creation when a booking is deleted.
     * <p>
     * Currently not implemented. Could be used to notify both
     * Fan and VenueManager if a booking is canceled.
     * </p>
     *
     * @param entityType The type of entity
     * @param entityId The booking ID
     */
    @Override
    public void onAfterDelete(String entityType, String entityId) {
        // Future implementation: Could notify both parties about cancellation
        // For now, cancellation is handled via UPDATE (status = CANCELLED)
    }

    // ========================================================================
    // PRIVATE NOTIFICATION CREATION METHODS
    // ========================================================================

    /**
     * Creates a notification for the VenueManager when a new booking arrives.
     * <p>
     * This method:
     * 1. Retrieves the Venue from VenueDao
     * 2. Get the VenueManager username
     * 3. Creates and saves NotificationBean with username directly
     * </p>
     *
     * @param bookingBean The booking data
     * @throws DAOException if there's an error saving the notification
     */
    private void createVenueManagerNotification(BookingBean bookingBean) throws DAOException {
        NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();

        // Get VenueManager username from Venue
        String venueManagerUsername = getVenueManagerUsernameForVenue(bookingBean.getVenueId());

        String message = String.format(
            "New booking request for %s vs %s on %s at venue",
            bookingBean.getHomeTeam().getDisplayName(),
            bookingBean.getAwayTeam().getDisplayName(),
            bookingBean.getGameDate()
        );

        NotificationBean notification = new NotificationBean.Builder()
            .username(venueManagerUsername)
            .userType(UserType.VENUE_MANAGER)
            .type(NotificationType.BOOKING_REQUESTED)
            .message(message)
            .relatedBookingId(bookingBean.getId())
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        notificationDao.saveNotification(notification);

        logger.log(Level.INFO, "Notification created for VenueManager: {0}", venueManagerUsername);
    }

    /**
     * Creates a notification for the Fan when their booking is confirmed/rejected.
     * <p>
     * This method:
     * 1. Uses Fan username directly from booking
     * 2. Determines notification type based on booking status
     * 3. Creates appropriate message
     * 4. Save NotificationBean
     * </p>
     *
     * @param bookingBean The booking bean object
     * @throws DAOException if there's an error saving the notification
     */
    private void createFanNotification(BookingBean bookingBean) throws DAOException {
        NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();

        String fanUsername = bookingBean.getFanUsername();

        NotificationType notificationType;
        String message;

        if (bookingBean.getStatus() == BookingStatus.CONFIRMED) {
            notificationType = NotificationType.BOOKING_APPROVED;
            message = String.format(
                "Great news! Your booking for %s vs %s has been APPROVED!",
                bookingBean.getHomeTeam().getDisplayName(),
                bookingBean.getAwayTeam().getDisplayName()
            );
        } else {
            notificationType = NotificationType.BOOKING_REJECTED;
            message = String.format(
                "Sorry, your booking for %s vs %s has been REJECTED.",
                bookingBean.getHomeTeam().getDisplayName(),
                bookingBean.getAwayTeam().getDisplayName()
            );
        }

        NotificationBean notification = new NotificationBean.Builder()
            .username(fanUsername)
            .userType(UserType.FAN)
            .type(notificationType)
            .message(message)
            .relatedBookingId(bookingBean.getId())
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        notificationDao.saveNotification(notification);

        logger.log(Level.INFO, "Notification created for Fan: {0}", fanUsername);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Retrieves the VenueManager username for a given venue.
     * <p>
     * Uses VenueDao to get the venue, then extracts the manager's username.
     * </p>
     *
     * @param venueId The venue ID
     * @return The VenueManager's username
     * @throws DAOException if venue is not found
     */
    private String getVenueManagerUsernameForVenue(int venueId) throws DAOException {
        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();
        Venue venue = venueDao.retrieveVenue(venueId);
        return venue.getVenueManagerUsername();
    }
}
