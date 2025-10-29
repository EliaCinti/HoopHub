package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
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
 * Implements real-time synchronization between different persistence types using the Observer pattern.
 * <p>
 * This class observes data changes in one persistence system (source) and automatically
 * replicates them to another persistence system (target). It supports bidirectional
 * synchronization between CSV files and MySQL database, ensuring data consistency.
 * </p>
 * <p>
 * The observer handles three types of operations:
 * <ul>
 *   <li><strong>INSERT</strong>: Receives Bean objects from the UI layer</li>
 *   <li><strong>UPDATE</strong>: Receives Model objects with updated data</li>
 *   <li><strong>DELETE</strong>: Receives entity identifiers, retrieves objects, then deletes</li>
 * </ul>
 * </p>
 * <p>
 * To prevent infinite synchronization loops, this observer checks {@link SyncContext}
 * before processing any operation and sets the sync flag during execution.
 * </p>
 */
public class CrossPersistenceSyncObserver implements DaoObserver {

    private static final Logger logger = Logger.getLogger(CrossPersistenceSyncObserver.class.getName());
    private final PersistenceType sourceType;

    /**
     * Creates a new cross-persistence synchronization observer.
     *
     * @param sourceType The persistence type that this observer is monitoring for changes
     */
    public CrossPersistenceSyncObserver(PersistenceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Determines the target persistence type for synchronization.
     * <p>
     * The target is always the opposite of the source type:
     * if source is CSV, target is MySQL, and vice versa.
     * </p>
     *
     * @return The target persistence type for replication
     */
    private PersistenceType getTargetType() {
        return sourceType == PersistenceType.CSV ? PersistenceType.MYSQL : PersistenceType.CSV;
    }

    /**
     * Gets a DAO factory configured for the target persistence type.
     *
     * @return DaoFactoryFacade configured for target persistence
     */
    private DaoFactoryFacade getTargetFactory() {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        factory.setPersistenceType(getTargetType());
        return factory;
    }

    /**
     * Handles entity insertion synchronization.
     * <p>
     * When a new entity is inserted in the source persistence, this method
     * replicates the insertion to the target persistence. For INSERT operations,
     * the system always receives Bean objects from the UI layer.
     * </p>
     *
     * @param entityType The type of entity being inserted (Fan, VenueManager, Venue, Booking)
     * @param entityId The unique identifier of the inserted entity
     * @param entity The entity data (Bean object for insertions)
     */
    @Override
    public void onAfterInsert(String entityType, String entityId, Object entity) {
        if (SyncContext.isSyncing()) return;
        SyncContext.startSync();
        try {
            logger.log(Level.INFO, "SYNC INSERT: Propagating {0} ({1}) from {2} to {3}",
                    new Object[]{entityType, entityId, sourceType, getTargetType()});

            switch (entityType) {
                case "Fan" -> {
                    FanDao targetDao = getTargetFactory().getFanDao();
                    targetDao.saveFan((FanBean) entity);
                }
                case "VenueManager" -> {
                    VenueManagerDao targetDao = getTargetFactory().getVenueManagerDao();
                    targetDao.saveVenueManager((VenueManagerBean) entity);
                }
                case "Venue" -> {
                    VenueDao targetDao = getTargetFactory().getVenueDao();
                    targetDao.saveVenue((VenueBean) entity);
                }
                case "Booking" -> {
                    BookingDao targetDao = getTargetFactory().getBookingDao();
                    // Booking already contains Fan reference, no need for separate username
                    targetDao.saveBooking((Booking) entity);
                }
                default -> logger.log(Level.WARNING, "Sync INSERT not handled for entity type: {0}", entityType);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Sync INSERT failed", e);
        } finally {
            SyncContext.endSync();
        }
    }

    /**
     * Handles entity update synchronization.
     * <p>
     * When an entity is updated in the source persistence, this method
     * replicates the update to the target persistence. For UPDATE operations,
     * the system always receives Model objects that contain the updated data.
     * </p>
     *
     * @param entityType The type of entity being updated
     * @param entityId The unique identifier of the updated entity
     * @param entity The entity data (Model object for updates)
     */
    @Override
    public void onAfterUpdate(String entityType, String entityId, Object entity) {
        if (SyncContext.isSyncing()) return;
        SyncContext.startSync();
        try {
            logger.log(Level.INFO, "SYNC UPDATE: Propagating {0} ({1}) from {2} to {3}",
                    new Object[]{entityType, entityId, sourceType, getTargetType()});

            switch (entityType) {
                case "Fan" -> {
                    FanDao targetDao = getTargetFactory().getFanDao();
                    Fan fan = (Fan) entity;
                    UserBean userBean = new UserBean.Builder<>()
                            .username(fan.getUsername())
                            .fullName(fan.getFullName())
                            .gender(fan.getGender())
                            .type("FAN")
                            .password(null) // Password not updated through this flow
                            .build();
                    targetDao.updateFan(fan, userBean);
                }
                case "VenueManager" -> {
                    VenueManagerDao targetDao = getTargetFactory().getVenueManagerDao();
                    VenueManager venueManager = (VenueManager) entity;
                    UserBean userBean = new UserBean.Builder<>()
                            .username(venueManager.getUsername())
                            .fullName(venueManager.getFullName())
                            .gender(venueManager.getGender())
                            .type("VENUE_MANAGER")
                            .password(null)
                            .build();
                    targetDao.updateVenueManager(venueManager, userBean);
                }
                case "Venue" -> {
                    VenueDao targetDao = getTargetFactory().getVenueDao();
                    targetDao.updateVenue((Venue) entity);
                }
                case "Booking" -> {
                    BookingDao targetDao = getTargetFactory().getBookingDao();
                    targetDao.updateBooking((Booking) entity);
                }
                default -> logger.log(Level.WARNING, "Sync UPDATE not handled for entity type: {0}", entityType);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Sync UPDATE failed", e);
        } finally {
            SyncContext.endSync();
        }
    }

    /**
     * Handles entity deletion synchronization.
     * <p>
     * When an entity is deleted from the source persistence, this method
     * replicates the deletion to the target persistence. Since DAOs now require
     * objects instead of IDs, we first retrieve the object from the target persistence,
     * then delete it.
     * </p>
     *
     * @param entityType The type of entity being deleted
     * @param entityId The unique identifier of the deleted entity
     */
    @Override
    public void onAfterDelete(String entityType, String entityId) {
        if (SyncContext.isSyncing()) return;
        SyncContext.startSync();
        try {
            logger.log(Level.INFO, "SYNC DELETE: Propagating {0} ({1}) from {2} to {3}",
                    new Object[]{entityType, entityId, sourceType, getTargetType()});

            DaoFactoryFacade targetFactory = getTargetFactory();

            switch (entityType) {
                case "User" -> {
                    // Retrieve User object first, then delete
                    UserDao userDao = targetFactory.getUserDao();
                    String[] userData = userDao.retrieveUser(entityId);
                    if (userData != null) {
                        // Create a minimal User object for deletion
                        // We need to determine the actual type
                        Fan fan = targetFactory.getFanDao().retrieveFan(entityId);
                        if (fan != null) {
                            targetFactory.getUserDao().deleteUser(fan);
                        } else {
                            VenueManager vm = targetFactory.getVenueManagerDao().retrieveVenueManager(entityId);
                            if (vm != null) {
                                targetFactory.getUserDao().deleteUser(vm);
                            }
                        }
                    }
                }
                case "Fan" -> {
                    Fan fan = targetFactory.getFanDao().retrieveFan(entityId);
                    if (fan != null) {
                        targetFactory.getFanDao().deleteFan(fan);
                    }
                }
                case "VenueManager" -> {
                    VenueManager venueManager = targetFactory.getVenueManagerDao().retrieveVenueManager(entityId);
                    if (venueManager != null) {
                        targetFactory.getVenueManagerDao().deleteVenueManager(venueManager);
                    }
                }
                case "Venue" -> {
                    int venueId = Integer.parseInt(entityId);
                    Venue venue = targetFactory.getVenueDao().retrieveVenue(venueId);
                    if (venue != null) {
                        targetFactory.getVenueDao().deleteVenue(venue);
                    }
                }
                case "Booking" -> {
                    int bookingId = Integer.parseInt(entityId);
                    Booking booking = targetFactory.getBookingDao().retrieveBooking(bookingId);
                    if (booking != null) {
                        targetFactory.getBookingDao().deleteBooking(booking);
                    }
                }
                default -> logger.log(Level.WARNING, "Sync DELETE not handled for entity type: {0}", entityType);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Sync DELETE failed", e);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Invalid entity ID format for deletion: " + entityId, e);
        } finally {
            SyncContext.endSync();
        }
    }
}