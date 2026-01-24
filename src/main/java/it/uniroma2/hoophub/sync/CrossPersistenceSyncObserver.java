package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.patterns.observer.DaoObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real-time synchronization observer implementing the <b>Observer pattern (GoF)</b>.
 *
 * <p><b>Purpose:</b> Automatically propagates data changes from one persistence
 * layer to another. When a DAO performs INSERT/UPDATE/DELETE, this observer
 * replicates the operation to the opposite persistence type.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Application uses MySQL as primary persistence</li>
 *   <li>User creates a new Fan → FanDaoMySql.saveFan() executes</li>
 *   <li>FanDaoMySql calls notifyObservers(INSERT, "Fan", username, fan)</li>
 *   <li>This observer receives the notification</li>
 *   <li>Observer temporarily switches to CSV persistence</li>
 *   <li>Observer calls FanDaoCsv.saveFan() to replicate the data</li>
 *   <li>Observer restores original persistence type</li>
 * </ol>
 *
 * <h3>Sync direction</h3>
 * <p>Direction is determined by the {@code sourceType} constructor parameter:</p>
 * <ul>
 *   <li>{@code sourceType = MYSQL} → syncs MySQL changes TO CSV</li>
 *   <li>{@code sourceType = CSV} → syncs CSV changes TO MySQL</li>
 * </ul>
 *
 * <h3>Loop prevention</h3>
 * <p>Uses {@link SyncContext} to prevent infinite loops. When this observer
 * writes to the target persistence, that DAO also notifies its observers.
 * Without protection, this would trigger a sync back to the source (loop).
 * SyncContext flags the thread as "syncing" so observers skip their callbacks.</p>
 *
 * <h3>Template Method pattern</h3>
 * <p>The {@link #performSync} method implements a Template Method that handles
 * common logic (context management, persistence switching, error handling),
 * while the actual sync operation is provided as a lambda ({@link SyncAction}).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see SyncContext
 * @see InitialSyncManager
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
     *                   (changes FROM this type are synced TO the opposite type)
     */
    public CrossPersistenceSyncObserver(PersistenceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Determines the target persistence type (opposite of source).
     *
     * @return CSV if source is MYSQL, MYSQL if source is CSV
     */
    private PersistenceType getTargetType() {
        return sourceType == PersistenceType.CSV ? PersistenceType.MYSQL : PersistenceType.CSV;
    }

    /**
     * Functional interface for sync operations.
     * Allows passing the specific DAO operation as a lambda to {@link #performSync}.
     */
    @FunctionalInterface
    private interface SyncAction {
        void execute(DaoFactoryFacade factory) throws DAOException;
    }

    /**
     * Template method that executes sync operations with proper context management.
     *
     * <p>Handles:
     * <ul>
     *   <li>Checking and setting SyncContext to prevent loops</li>
     *   <li>Temporarily switching to target persistence type</li>
     *   <li>Executing the actual sync operation (provided as lambda)</li>
     *   <li>Restoring original persistence type in finally block</li>
     *   <li>Error logging and handling</li>
     * </ul>
     * </p>
     *
     * @param operationName operation name for logging (INSERT/UPDATE/DELETE)
     * @param entityType    entity type being synced (Fan, Venue, etc.)
     * @param entityId      identifier of the entity
     * @param action        the sync operation to execute
     */
    private void performSync(String operationName, String entityType, String entityId, SyncAction action) {
        // Skip if already syncing (prevents infinite loops)
        if (SyncContext.isSyncing()) return;

        SyncContext.startSync();

        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        PersistenceType originalType = factory.getPersistenceType();

        try {
            // Switch to target persistence for the sync operation
            factory.setPersistenceType(getTargetType());

            logger.log(Level.INFO, "SYNC {0}: Propagating {1} ({2}) from {3} to {4}",
                    new Object[]{operationName, entityType, entityId, sourceType, getTargetType()});

            // Execute the actual sync operation
            action.execute(factory);

        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Sync " + operationName + " failed");
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
     * Replicates the INSERT to the target persistence.
     *
     * @param entityType type identifier (Fan, Venue, Booking, etc.)
     * @param entityId   unique identifier of the inserted entity
     * @param entity     the Model object that was inserted
     */
    @Override
    public void onAfterInsert(String entityType, String entityId, Object entity) {
        performSync("INSERT", entityType, entityId, factory -> {
            switch (entityType) {
                case SyncConstants.FAN -> factory.getFanDao().saveFan((Fan) entity);
                case SyncConstants.VENUE_MANAGER -> factory.getVenueManagerDao().saveVenueManager((VenueManager) entity);
                case SyncConstants.VENUE -> factory.getVenueDao().saveVenue((Venue) entity);
                case SyncConstants.BOOKING -> factory.getBookingDao().saveBooking((Booking) entity);
                case NOTIFICATION -> factory.getNotificationDao().saveNotification((it.uniroma2.hoophub.model.Notification) entity);
                default -> logger.log(Level.WARNING, "Sync INSERT not handled for entity type: {0}", entityType);
            }
        });
    }

    /**
     * Called after an entity is updated in the source persistence.
     * Replicates the UPDATE to the target persistence.
     *
     * @param entityType type identifier
     * @param entityId   unique identifier of the updated entity
     * @param entity     the Model object with updated data
     */
    @Override
    public void onAfterUpdate(String entityType, String entityId, Object entity) {
        performSync("UPDATE", entityType, entityId, factory -> {
            switch (entityType) {
                case SyncConstants.FAN -> factory.getFanDao().updateFan((Fan) entity);
                case SyncConstants.VENUE_MANAGER -> factory.getVenueManagerDao().updateVenueManager((VenueManager) entity);
                case SyncConstants.VENUE -> factory.getVenueDao().updateVenue((Venue) entity);
                case SyncConstants.BOOKING -> factory.getBookingDao().updateBooking((Booking) entity);
                case NOTIFICATION -> factory.getNotificationDao().updateNotification((it.uniroma2.hoophub.model.Notification) entity);
                default -> logger.log(Level.WARNING, "Sync UPDATE not handled for entity type: {0}", entityType);
            }
        });
    }

    /**
     * Called after an entity is deleted from the source persistence.
     * Replicates the DELETE to the target persistence.
     *
     * <p>Note: DELETE requires retrieving the entity from target first,
     * since only the ID is available (not the full object).</p>
     *
     * @param entityType type identifier
     * @param entityId   unique identifier of the deleted entity
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
                case NOTIFICATION -> {
                    try {
                        int id = Integer.parseInt(entityId);
                        it.uniroma2.hoophub.model.Notification n = factory.getNotificationDao().retrieveNotification(id);
                        if (n != null) factory.getNotificationDao().deleteNotification(n);
                    } catch (NumberFormatException e) {
                        logger.log(Level.SEVERE, e, () -> "Invalid entity ID format for deletion: " + entityId);
                    }
                }
                default -> logger.log(Level.WARNING, "Sync DELETE not handled for entity type: {0}", entityType);
            }
        });
    }

    // =================================================================================
    // DELETE HELPER METHODS
    // =================================================================================

    /**
     * Deletes a User by determining if it's a Fan or VenueManager first.
     */
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
        VenueManager venueManager = factory.getVenueManagerDao().retrieveVenueManager(username);
        if (venueManager != null) {
            factory.getVenueManagerDao().deleteVenueManager(venueManager);
        }
    }

    private void deleteVenue(String entityId, DaoFactoryFacade factory) throws DAOException {
        int venueId = Integer.parseInt(entityId);
        Venue venue = factory.getVenueDao().retrieveVenue(venueId);
        if (venue != null) {
            factory.getVenueDao().deleteVenue(venue);
        }
    }

    private void deleteBooking(String entityId, DaoFactoryFacade factory) throws DAOException {
        int bookingId = Integer.parseInt(entityId);
        Booking booking = factory.getBookingDao().retrieveBooking(bookingId);
        if (booking != null) {
            factory.getBookingDao().deleteBooking(booking);
        }
    }
}