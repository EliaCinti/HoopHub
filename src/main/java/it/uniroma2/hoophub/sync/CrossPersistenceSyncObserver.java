package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real-time bidirectional synchronization observer implementing the <b>Observer pattern (GoF)</b>.
 *
 * <p><b>Purpose:</b> Automatically propagates data changes from one persistence
 * layer to another. When a DAO performs INSERT/UPDATE/DELETE, this observer
 * replicates the operation to the opposite persistence type.</p>
 *
 * <h3>Bidirectional Sync</h3>
 * <ul>
 *   <li>Changes in MySQL → automatically synced to CSV</li>
 *   <li>Changes in CSV → automatically synced to MySQL</li>
 * </ul>
 *
 * <h3>UPSERT Strategy</h3>
 * <p>All save operations use UPSERT (Insert or Update) semantics to handle
 * ID conflicts gracefully. If an entity with the same ID already exists in
 * the target persistence, it will be updated instead of causing a duplicate key error.</p>
 *
 * <h3>Loop Prevention</h3>
 * <p>Uses {@link SyncContext} thread-local flag to prevent infinite sync loops.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 * @see SyncContext
 */
public class CrossPersistenceSyncObserver implements DaoObserver {

    private static final Logger logger = Logger.getLogger(CrossPersistenceSyncObserver.class.getName());

    /** The persistence type that THIS observer listens to (source of changes). */
    private final PersistenceType sourceType;

    private static final String NOTIFICATION = "Notification";

    /**
     * Creates an observer that syncs changes FROM the specified source type.
     *
     * @param sourceType the persistence type this observer monitors
     */
    public CrossPersistenceSyncObserver(PersistenceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Determines the target persistence type (opposite of source).
     */
    private PersistenceType getTargetType() {
        return sourceType == PersistenceType.CSV ? PersistenceType.MYSQL : PersistenceType.CSV;
    }

    /**
     * Functional interface for sync operations.
     */
    @FunctionalInterface
    private interface SyncAction {
        void execute(DaoFactoryFacade factory) throws DAOException;
    }

    /**
     * Template method that executes sync operations with proper context management.
     */
    private void performSync(String operationName, String entityType, String entityId, SyncAction action) {
        // Skip if already syncing (prevents infinite loops)
        if (SyncContext.isSyncing()) {
            logger.log(Level.FINEST, "Skipping sync (already syncing): {0} {1}",
                    new Object[]{operationName, entityType});
            return;
        }

        SyncContext.startSync();

        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        PersistenceType originalType = factory.getPersistenceType();

        try {
            // Switch to target persistence for the sync operation
            factory.setPersistenceType(getTargetType());

            logger.log(Level.FINE, "SYNC {0}: {1} ({2}) from {3} to {4}",
                    new Object[]{operationName, entityType, entityId, sourceType, getTargetType()});

            // Execute the actual sync operation
            action.execute(factory);

            logger.log(Level.FINE, "SYNC {0} successful: {1} ({2})",
                    new Object[]{operationName, entityType, entityId});

        } catch (DAOException e) {
            // Log but don't throw - sync failures shouldn't break the main operation
            logger.log(Level.WARNING, "Sync {0} failed for {1} ({2}): {3}",
                    new Object[]{operationName, entityType, entityId, e.getMessage()});
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Unexpected error during sync " + operationName);
        } finally {
            // ALWAYS restore original persistence type
            factory.setPersistenceType(originalType);
            SyncContext.endSync();
        }
    }

    // =================================================================================
    // OBSERVER INTERFACE IMPLEMENTATION
    // =================================================================================

    /**
     * Called after an entity is inserted in the source persistence.
     * Replicates the INSERT to the target persistence using UPSERT semantics.
     */
    @Override
    public void onAfterInsert(String entityType, String entityId, Object entity) {
        performSync("INSERT", entityType, entityId, factory -> {
            switch (entityType) {
                case SyncConstants.FAN -> factory.getFanDao().saveFan((Fan) entity);
                case SyncConstants.VENUE_MANAGER -> factory.getVenueManagerDao().saveVenueManager((VenueManager) entity);
                case SyncConstants.VENUE -> factory.getVenueDao().saveVenue((Venue) entity);
                case SyncConstants.BOOKING -> factory.getBookingDao().saveBooking((Booking) entity);
                case NOTIFICATION -> factory.getNotificationDao().saveNotification((Notification) entity);
                default -> logger.log(Level.FINE, "Sync INSERT not handled for: {0}", entityType);
            }
        });
    }

    /**
     * Called after an entity is updated in the source persistence.
     * Replicates the UPDATE to the target persistence.
     */
    @Override
    public void onAfterUpdate(String entityType, String entityId, Object entity) {
        performSync("UPDATE", entityType, entityId, factory -> {
            switch (entityType) {
                case SyncConstants.FAN -> factory.getFanDao().updateFan((Fan) entity);
                case SyncConstants.VENUE_MANAGER -> factory.getVenueManagerDao().updateVenueManager((VenueManager) entity);
                case SyncConstants.VENUE -> factory.getVenueDao().updateVenue((Venue) entity);
                case SyncConstants.BOOKING -> factory.getBookingDao().updateBooking((Booking) entity);
                case NOTIFICATION -> factory.getNotificationDao().updateNotification((Notification) entity);
                default -> logger.log(Level.FINE, "Sync UPDATE not handled for: {0}", entityType);
            }
        });
    }

    /**
     * Called after an entity is deleted from the source persistence.
     * Replicates the DELETE to the target persistence.
     */
    @Override
    public void onAfterDelete(String entityType, String entityId) {
        performSync("DELETE", entityType, entityId, factory -> {
            switch (entityType) {
                case SyncConstants.USER -> deleteUser(entityId, factory);
                case SyncConstants.FAN -> deleteFan(entityId, factory);
                case SyncConstants.VENUE_MANAGER -> deleteVenueManager(entityId, factory);
                case SyncConstants.VENUE -> deleteVenue(entityId, factory);
                case SyncConstants.BOOKING -> deleteBooking(entityId, factory);
                case NOTIFICATION -> deleteNotification(entityId, factory);
                default -> logger.log(Level.FINE, "Sync DELETE not handled for: {0}", entityType);
            }
        });
    }

    // =================================================================================
    // DELETE HELPER METHODS
    // =================================================================================

    private void deleteUser(String username, DaoFactoryFacade factory) throws DAOException {
        UserDao userDao = factory.getUserDao();
        String[] userData = userDao.retrieveUser(username);
        if (userData != null) {
            Fan fan = factory.getFanDao().retrieveFan(username);
            if (fan != null) {
                factory.getUserDao().deleteUser(fan);
            } else {
                VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
                if (vm != null) {
                    factory.getUserDao().deleteUser(vm);
                }
            }
        }
    }

    private void deleteFan(String username, DaoFactoryFacade factory) throws DAOException {
        Fan fan = factory.getFanDao().retrieveFan(username);
        if (fan != null) {
            factory.getFanDao().deleteFan(fan);
        }
    }

    private void deleteVenueManager(String username, DaoFactoryFacade factory) throws DAOException {
        VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
        if (vm != null) {
            factory.getVenueManagerDao().deleteVenueManager(vm);
        }
    }

    private void deleteVenue(String entityId, DaoFactoryFacade factory) throws DAOException {
        try {
            int venueId = Integer.parseInt(entityId);
            Venue venue = factory.getVenueDao().retrieveVenue(venueId);
            if (venue != null) {
                factory.getVenueDao().deleteVenue(venue);
            }
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid venue ID for deletion: {0}", entityId);
        }
    }

    private void deleteBooking(String entityId, DaoFactoryFacade factory) throws DAOException {
        try {
            int bookingId = Integer.parseInt(entityId);
            Booking booking = factory.getBookingDao().retrieveBooking(bookingId);
            if (booking != null) {
                factory.getBookingDao().deleteBooking(booking);
            }
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid booking ID for deletion: {0}", entityId);
        }
    }

    private void deleteNotification(String entityId, DaoFactoryFacade factory) throws DAOException {
        try {
            int id = Integer.parseInt(entityId);
            Notification n = factory.getNotificationDao().retrieveNotification(id);
            if (n != null) {
                factory.getNotificationDao().deleteNotification(n);
            }
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid notification ID for deletion: {0}", entityId);
        }
    }
}