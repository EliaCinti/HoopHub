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
 * Implements real-time synchronization between different persistence types using the Observer pattern.
 * <p>
 * Refactored to reduce code duplication using a centralized synchronization executor.
 * </p>
 */
public class CrossPersistenceSyncObserver implements DaoObserver {

    private static final Logger logger = Logger.getLogger(CrossPersistenceSyncObserver.class.getName());
    private final PersistenceType sourceType;
    private static final String NOTIFICATION = "Notification";

    public CrossPersistenceSyncObserver(PersistenceType sourceType) {
        this.sourceType = sourceType;
    }

    private PersistenceType getTargetType() {
        return sourceType == PersistenceType.CSV ? PersistenceType.MYSQL : PersistenceType.CSV;
    }

    // =================================================================================
    // INTERFACCIA FUNZIONALE PER L'AZIONE DI SYNC
    // =================================================================================
    @FunctionalInterface
    private interface SyncAction {
        // FIX: Sostituito throws Exception con throws DAOException
        void execute(DaoFactoryFacade factory) throws DAOException;
    }

    // =================================================================================
    // METODO CENTRALIZZATO (TEMPLATE METHOD)
    // =================================================================================
    /**
     * Esegue la logica comune di sincronizzazione: gestione contesto, cambio persistenza, gestione errori.
     */
    private void performSync(String operationName, String entityType, String entityId, SyncAction action) {
        if (SyncContext.isSyncing()) return;
        SyncContext.startSync();

        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        PersistenceType originalType = factory.getPersistenceType();

        try {
            factory.setPersistenceType(getTargetType());

            logger.log(Level.INFO, "SYNC {0}: Propagating {1} ({2}) from {3} to {4}",
                    new Object[]{operationName, entityType, entityId, sourceType, getTargetType()});

            action.execute(factory);

        } catch (Exception e) {
            // FIX: Uso della Lambda per il logging
            logger.log(Level.SEVERE, e, () -> "Sync " + operationName + " failed");
        } finally {
            factory.setPersistenceType(originalType);
            SyncContext.endSync();
        }
    }

    // =================================================================================
    // IMPLEMENTAZIONE METODI OBSERVER (Ora molto più snelli)
    // =================================================================================

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
                        if(n != null) factory.getNotificationDao().deleteNotification(n);
                    } catch (NumberFormatException e) {
                        // FIX: Uso della Lambda per il logging
                        logger.log(Level.SEVERE, e, () -> "Invalid entity ID format for deletion: " + entityId);
                    }
                }
                default -> logger.log(Level.WARNING, "Sync DELETE not handled for entity type: {0}", entityType);
            }
        });
    }

    // =================================================================================
    // HELPER METHODS FOR DELETION (Invariati)
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