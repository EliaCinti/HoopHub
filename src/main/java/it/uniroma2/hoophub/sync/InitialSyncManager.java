package it.uniroma2.hoophub.sync;

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

            // Synchronize entities in dependency order
            List<Fan> syncedFans = syncFans(primaryType, secondaryType);
            syncVenueManagers(primaryType, secondaryType);
            syncVenues(primaryType, secondaryType);  // <-- Rimosso "List<Venue> syncedVenues ="
            syncBookings(syncedFans, primaryType, secondaryType);

            logger.info("Initial synchronization completed successfully.");
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Initial synchronization failed.", e);
        } finally {
            SyncContext.endSync();
            logger.info("Real-time synchronization observers reactivated.");
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
                factory.setPersistenceType(secondary);
                factory.getVenueDao().updateVenue(primaryVenue);
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
            Map<Integer, Booking> primaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(fan));

            factory.setPersistenceType(secondary);
            Map<Integer, Booking> secondaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(fan));

            Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
            allIds.addAll(secondaryMap.keySet());

            for (Integer id : allIds) {
                Booking primaryBooking = primaryMap.get(id);
                Booking secondaryBooking = secondaryMap.get(id);

                if (primaryBooking != null && secondaryBooking == null) {
                    logger.info("Sync: Copying booking " + id + SyncConstants.FROM + primary + " to " + secondary);
                    factory.setPersistenceType(secondary);
                    // Booking already contains Fan reference
                    factory.getBookingDao().saveBooking(primaryBooking);
                } else if (primaryBooking == null && secondaryBooking != null) {
                    logger.info("Sync: Copying booking " + id + SyncConstants.FROM + secondary + " to " + primary);
                    factory.setPersistenceType(primary);
                    // Booking already contains Fan reference
                    factory.getBookingDao().saveBooking(secondaryBooking);
                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    logger.info("Sync Conflict: Different data for booking " + id + SyncConstants.PRIMARY_SOURCE + primary + SyncConstants.TAKES_PRECEDENCE);
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().updateBooking(primaryBooking);
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
                .venueManagerUsername(venue.getVenueManagerUsername())  // Fixed: use getVenueManagerUsername()
                .build();
    }
}