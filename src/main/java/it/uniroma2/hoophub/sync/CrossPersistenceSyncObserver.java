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
import it.uniroma2.hoophub.utilities.UserType;

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
                case SyncConstants.FAN -> {
                    FanDao targetDao = getTargetFactory().getFanDao();
                    targetDao.saveFan((FanBean) entity);
                }
                case SyncConstants.VENUE_MANAGER -> {
                    VenueManagerDao targetDao = getTargetFactory().getVenueManagerDao();
                    targetDao.saveVenueManager((VenueManagerBean) entity);
                }
                case SyncConstants.VENUE -> {
                    VenueDao targetDao = getTargetFactory().getVenueDao();
                    targetDao.saveVenue((VenueBean) entity);
                }
                case SyncConstants.BOOKING -> {
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
                case SyncConstants.FAN -> {
                    FanDao targetDao = getTargetFactory().getFanDao();
                    Fan fan = (Fan) entity;
                    UserBean userBean = new UserBean.Builder<>()
                            .username(fan.getUsername())
                            .fullName(fan.getFullName())
                            .gender(fan.getGender())
                            .type(UserType.FAN.toString())
                            .password(null) // Password not updated through this flow
                            .build();
                    targetDao.updateFan(fan, userBean);
                }
                case SyncConstants.VENUE_MANAGER -> {
                    VenueManagerDao targetDao = getTargetFactory().getVenueManagerDao();
                    VenueManager venueManager = (VenueManager) entity;
                    UserBean userBean = new UserBean.Builder<>()
                            .username(venueManager.getUsername())
                            .fullName(venueManager.getFullName())
                            .gender(venueManager.getGender())
                            .type(UserType.VENUE_MANAGER.toString())
                            .password(null)
                            .build();
                    targetDao.updateVenueManager(venueManager, userBean);
                }
                case SyncConstants.VENUE -> {
                    VenueDao targetDao = getTargetFactory().getVenueDao();
                    targetDao.updateVenue((Venue) entity);
                }
                case SyncConstants.BOOKING -> {
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
                case SyncConstants.USER -> deleteUser(entityId, targetFactory);
                case SyncConstants.FAN -> deleteFan(entityId, targetFactory);
                case SyncConstants.VENUE_MANAGER -> deleteVenueManager(entityId, targetFactory);
                case SyncConstants.VENUE -> deleteVenue(entityId, targetFactory);
                case SyncConstants.BOOKING -> deleteBooking(entityId, targetFactory);
                default -> logger.log(Level.WARNING, "Sync DELETE not handled for entity type: {0}", entityType);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Sync DELETE failed", e);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, e, () -> "Invalid entity ID format for deletion: " + entityId);
        } finally {
            SyncContext.endSync();
        }
    }

    // ========== Helper Methods for Deletion ==========

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