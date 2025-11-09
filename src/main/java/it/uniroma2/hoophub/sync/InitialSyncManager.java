package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.utilities.UserType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the initial synchronization process between different persistence types.
 * <p>
 * This class is responsible for ensuring data consistency when the application starts
 * by comparing and synchronizing data between CSV and MySQL storage systems.
 * It handles conflict resolution by giving priority to the primary persistence type.
 * </p>
 * <p>
 * The synchronization process includes:
 * <ul>
 *   <li>Users and fan/venue manager profiles</li>
 *   <li>Venue records</li>
 *   <li>Booking records for all fans</li>
 *   <li>Conflict detection and resolution</li>
 * </ul>
 * </p>
 */
public class InitialSyncManager {

    private static final Logger logger = Logger.getLogger(InitialSyncManager.class.getName());

    public void performInitialSync(PersistenceType primaryType) {
        logger.info("Starting initial synchronization...");
        SyncContext.startSync();
        try {
            PersistenceType secondaryType = (primaryType == PersistenceType.MYSQL)
                    ? PersistenceType.CSV : PersistenceType.MYSQL;

            // Try normal synchronization first
            try {
                performNormalSync(primaryType, secondaryType);
                logger.info("Initial synchronization completed successfully.");
            } catch (DAOException e) {
                // Check if the error is due to data inconsistency
                if (isInconsistencyError(e) && secondaryType == PersistenceType.CSV) {
                    logger.warning("Data inconsistency detected in CSV files. Performing full resynchronization...");
                    clearSecondaryData(secondaryType);
                    performNormalSync(primaryType, secondaryType);
                    logger.info("Full resynchronization completed successfully.");
                } else {
                    throw e;  // Re-throw if it's not an inconsistency error
                }
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Initial synchronization failed.", e);
        } finally {
            SyncContext.endSync();
            logger.info("Real-time synchronization observers reactivated.");
        }
    }

    /**
     * Performs the normal synchronization process.
     *
     * @param primaryType The primary persistence type
     * @param secondaryType The secondary persistence type
     * @throws DAOException if synchronization fails
     */
    private void performNormalSync(PersistenceType primaryType, PersistenceType secondaryType) throws DAOException {
        // Synchronize entities in dependency order
        List<Fan> syncedFans = syncFans(primaryType, secondaryType);
        syncVenueManagers(primaryType, secondaryType);
        syncVenues(primaryType, secondaryType);
        syncBookings(syncedFans, primaryType, secondaryType);
    }

    /**
     * Checks if a DAOException is caused by data inconsistency.
     *
     * @param e The exception to check
     * @return true if the error indicates data inconsistency
     */
    private boolean isInconsistencyError(DAOException e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("already exists") ||
                message.contains("Username already exists") ||
                message.contains("duplicate")
        );
    }

    /**
     * Clears all data from the secondary persistence storage.
     * This is only safe to do for CSV storage as it will be repopulated from primary.
     *
     * @param secondaryType The secondary persistence type to clear
     * @throws DAOException if clearing fails
     */
    private void clearSecondaryData(PersistenceType secondaryType) throws DAOException {
        if (secondaryType != PersistenceType.CSV) {
            logger.warning("Skipping clear operation - only CSV secondary storage can be safely cleared");
            return;
        }

        logger.info("Clearing all data from CSV files...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        factory.setPersistenceType(secondaryType);

        try {
            // Clear all CSV data by deleting all users (cascade effect)
            // Since CSV uses file-based storage, we'll retrieve all users and delete them
            List<Fan> fans = factory.getFanDao().retrieveAllFans();
            for (Fan fan : fans) {
                factory.getFanDao().deleteFan(fan);
            }

            List<VenueManager> vms = factory.getVenueManagerDao().retrieveAllVenueManagers();
            for (VenueManager vm : vms) {
                factory.getVenueManagerDao().deleteVenueManager(vm);
            }

            List<Venue> venues = factory.getVenueDao().retrieveAllVenues();
            for (Venue venue : venues) {
                factory.getVenueDao().deleteVenue(venue.getId());
            }

            logger.info("CSV data cleared successfully");
        } catch (DAOException e) {
            logger.log(Level.WARNING, "Error during CSV data clearing (may be expected if files are already clean)", e);
            // Continue anyway - we'll repopulate from primary
        }
    }

    /**
     * Synchronizes fan data between primary and secondary persistence types.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @return List of synchronized fans from the primary persistence
     * @throws DAOException if fan data access or synchronization fails
     */
    private List<Fan> syncFans(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing fans...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        factory.setPersistenceType(primary);
        Map<String, Fan> primaryMap = listToMap(factory.getFanDao().retrieveAllFans());

        factory.setPersistenceType(secondary);
        Map<String, Fan> secondaryMap = listToMap(factory.getFanDao().retrieveAllFans());

        Set<String> allKeys = new HashSet<>(primaryMap.keySet());
        allKeys.addAll(secondaryMap.keySet());

        for (String key : allKeys) {
            Fan primaryFan = primaryMap.get(key);
            Fan secondaryFan = secondaryMap.get(key);

            if (primaryFan != null && secondaryFan == null) {
                logger.info("Sync: Copying fan " + key + SyncConstants.FROM + primary + " to " + secondary);
                FanBean beanToSave = createFanBeanFromModel(primaryFan, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getFanDao().saveFan(beanToSave);
            } else if (primaryFan == null && secondaryFan != null) {
                logger.info("Sync: Copying fan " + key + SyncConstants.FROM + secondary + " to " + primary);
                FanBean beanToSave = createFanBeanFromModel(secondaryFan, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getFanDao().saveFan(beanToSave);
            } else if (primaryFan != null && !primaryFan.isDataEquivalent(secondaryFan)) {
                logger.info("Sync Conflict: Different data for fan " + key + SyncConstants.FROM + primary + SyncConstants.TAKES_PRECEDENCE);
                FanBean beanToUpdate = createFanBeanFromModel(primaryFan, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getFanDao().updateFan(primaryFan, beanToUpdate);
            }
        }

        factory.setPersistenceType(primary);
        return factory.getFanDao().retrieveAllFans();
    }

    /**
     * Synchronizes venue manager data between primary and secondary persistence types.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if venue manager data access or synchronization fails
     */
    private void syncVenueManagers(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing venue managers...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        factory.setPersistenceType(primary);
        Map<String, VenueManager> primaryMap = listToMapVenueManager(factory.getVenueManagerDao().retrieveAllVenueManagers());

        factory.setPersistenceType(secondary);
        Map<String, VenueManager> secondaryMap = listToMapVenueManager(factory.getVenueManagerDao().retrieveAllVenueManagers());

        Set<String> allKeys = new HashSet<>(primaryMap.keySet());
        allKeys.addAll(secondaryMap.keySet());

        for (String key : allKeys) {
            VenueManager primaryVM = primaryMap.get(key);
            VenueManager secondaryVM = secondaryMap.get(key);

            if (primaryVM != null && secondaryVM == null) {
                logger.info("Sync: Copying venue manager " + key + SyncConstants.FROM + primary + " to " + secondary);
                VenueManagerBean bean = createVenueManagerBeanFromModel(primaryVM, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().saveVenueManager(bean);
            } else if (primaryVM == null && secondaryVM != null) {
                logger.info("Sync: Copying venue manager " + key + SyncConstants.FROM + secondary + " to " + primary);
                VenueManagerBean bean = createVenueManagerBeanFromModel(secondaryVM, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getVenueManagerDao().saveVenueManager(bean);
            } else if (primaryVM != null && !primaryVM.isDataEquivalent(secondaryVM)) {
                logger.info("Sync Conflict: Different data for venue manager " + key + SyncConstants.PRIMARY_SOURCE + primary + SyncConstants.TAKES_PRECEDENCE);
                VenueManagerBean beanToUpdate = createVenueManagerBeanFromModel(primaryVM, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().updateVenueManager(primaryVM, beanToUpdate);
            }
        }
    }

    /**
     * Synchronizes venue data between primary and secondary persistence types.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if venue data access or synchronization fails
     */
    private void syncVenues(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing venues...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        factory.setPersistenceType(primary);
        Map<Integer, Venue> primaryMap = listToMapVenues(factory.getVenueDao().retrieveAllVenues());

        factory.setPersistenceType(secondary);
        Map<Integer, Venue> secondaryMap = listToMapVenues(factory.getVenueDao().retrieveAllVenues());

        Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
        allIds.addAll(secondaryMap.keySet());

        for (Integer id : allIds) {
            Venue primaryVenue = primaryMap.get(id);
            Venue secondaryVenue = secondaryMap.get(id);

            if (primaryVenue != null && secondaryVenue == null) {
                logger.info("Sync: Copying venue " + id + SyncConstants.FROM + primary + " to " + secondary);
                VenueBean bean = createVenueBeanFromModel(primaryVenue);
                factory.setPersistenceType(secondary);
                factory.getVenueDao().saveVenue(bean);
            } else if (primaryVenue == null && secondaryVenue != null) {
                logger.info("Sync: Copying venue " + id + SyncConstants.FROM + secondary + " to " + primary);
                VenueBean bean = createVenueBeanFromModel(secondaryVenue);
                factory.setPersistenceType(primary);
                factory.getVenueDao().saveVenue(bean);
            } else if (primaryVenue != null && !primaryVenue.equals(secondaryVenue)) {
                logger.info("Sync Conflict: Different data for venue " + id + ". Primary source " + primary + " takes precedence.");
                VenueBean beanToUpdate = createVenueBeanFromModel(primaryVenue);
                factory.setPersistenceType(secondary);
                factory.getVenueDao().updateVenue(beanToUpdate);
            }
        }
        // Rimosso il return
    }

    /**
     * Synchronizes booking data for all fans between persistence types.
     *
     * @param syncedFans List of fans whose bookings should be synchronized
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if booking data access or synchronization fails
     */
    private void syncBookings(List<Fan> syncedFans, PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing bookings...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        for (Fan fan : syncedFans) {
            factory.setPersistenceType(primary);
            Map<Integer, Booking> primaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(fan.getUsername()));

            factory.setPersistenceType(secondary);
            Map<Integer, Booking> secondaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(fan.getUsername()));

            Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
            allIds.addAll(secondaryMap.keySet());

            for (Integer id : allIds) {
                Booking primaryBooking = primaryMap.get(id);
                Booking secondaryBooking = secondaryMap.get(id);

                if (primaryBooking != null && secondaryBooking == null) {
                    logger.info("Sync: Copying booking " + id + SyncConstants.FROM + primary + " to " + secondary);
                    BookingBean beanToSave = createBookingBeanFromModel(primaryBooking);
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().saveBooking(beanToSave);
                } else if (primaryBooking == null && secondaryBooking != null) {
                    logger.info("Sync: Copying booking " + id + SyncConstants.FROM + secondary + " to " + primary);
                    BookingBean beanToSave = createBookingBeanFromModel(secondaryBooking);
                    factory.setPersistenceType(primary);
                    factory.getBookingDao().saveBooking(beanToSave);
                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    logger.info("Sync Conflict: Different data for booking " + id + SyncConstants.PRIMARY_SOURCE + primary + SyncConstants.TAKES_PRECEDENCE);
                    BookingBean beanToUpdate = createBookingBeanFromModel(primaryBooking);
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().updateBooking(beanToUpdate);
                }
            }
        }
    }

    // ========== Helper Methods ==========

    private Map<String, Fan> listToMap(List<Fan> list) {
        Map<String, Fan> map = new HashMap<>();
        for (Fan f : list) {
            map.put(f.getUsername(), f);
        }
        return map;
    }

    private Map<String, VenueManager> listToMapVenueManager(List<VenueManager> list) {
        Map<String, VenueManager> map = new HashMap<>();
        for (VenueManager vm : list) {
            map.put(vm.getUsername(), vm);
        }
        return map;
    }

    private Map<Integer, Venue> listToMapVenues(List<Venue> list) {
        Map<Integer, Venue> map = new HashMap<>();
        for (Venue v : list) {
            map.put(v.getId(), v);
        }
        return map;
    }

    private Map<Integer, Booking> listToMapBookings(List<Booking> list) {
        Map<Integer, Booking> map = new HashMap<>();
        for (Booking b : list) {
            map.put(b.getId(), b);
        }
        return map;
    }

    private FanBean createFanBeanFromModel(Fan fan, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();
            String[] userInfo = userDao.retrieveUser(fan.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            return new FanBean.Builder()
                    .username(fan.getUsername())
                    .password(hashedPassword)
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .birthday(fan.getBirthday())
                    .favTeam(fan.getFavTeam())
                    .type("FAN")
                    .build();
        } finally {
            factory.setPersistenceType(originalType);
        }
    }

    private VenueManagerBean createVenueManagerBeanFromModel(VenueManager vm, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();
            String[] userInfo = userDao.retrieveUser(vm.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            return new VenueManagerBean.Builder()
                    .username(vm.getUsername())
                    .password(hashedPassword)
                    .fullName(vm.getFullName())
                    .gender(vm.getGender())
                    .companyName(vm.getCompanyName())
                    .phoneNumber(vm.getPhoneNumber())
                    .type(UserType.VENUE_MANAGER.toString())
                    .build();
        } finally {
            factory.setPersistenceType(originalType);
        }
    }

    private VenueBean createVenueBeanFromModel(Venue venue) {
        return new VenueBean.Builder()
                .id(venue.getId())
                .name(venue.getName())
                .type(venue.getType())
                .address(venue.getAddress())
                .city(venue.getCity())
                .maxCapacity(venue.getMaxCapacity())
                .venueManagerUsername(venue.getVenueManagerUsername())
                .build();
    }

    /**
     * Creates a BookingBean from a Booking model object.
     * <p>
     * This helper method extracts primitive values from the Booking model
     * to create a lightweight Bean suitable for DAO operations.
     * </p>
     *
     * @param booking The Booking model object
     * @return A BookingBean with data from the model
     */
    private BookingBean createBookingBeanFromModel(Booking booking) {
        return new BookingBean.Builder()
                .id(booking.getId())
                .gameDate(booking.getGameDate())
                .gameTime(booking.getGameTime())
                .homeTeam(booking.getHomeTeam())
                .awayTeam(booking.getAwayTeam())
                .venueId(booking.getVenueId())
                .fanUsername(booking.getFanUsername())
                .status(booking.getStatus())
                .notified(booking.isNotified())
                .build();
    }
}