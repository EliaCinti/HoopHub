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
import it.uniroma2.hoophub.model.UserType;

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
 *
 * @see CrossPersistenceSyncObserver for real-time synchronization
 * @see SyncContext for preventing synchronization loops
 */
public class InitialSyncManager {

    private static final Logger logger = Logger.getLogger(InitialSyncManager.class.getName());

    /**
     * Performs a complete initial synchronization between primary and secondary persistence types.
     * <p>
     * This method coordinates the synchronization of all entities in the correct order:
     * fans first, then venue managers, then venues, and finally bookings (which depend on fans).
     * </p>
     * <p>
     * The synchronization is performed within a {@link SyncContext} to prevent
     * observer notifications that could cause infinite loops.
     * </p>
     *
     * @param primaryType The primary persistence type that takes precedence in conflict resolution
     */
    public void performInitialSync(PersistenceType primaryType) {
        logger.info("Starting initial synchronization...");
        SyncContext.startSync();
        try {
            PersistenceType secondaryType = (primaryType == PersistenceType.MYSQL)
                    ? PersistenceType.CSV : PersistenceType.MYSQL;

            // Clear secondary CSV files to prevent inconsistencies (users.csv vs fans.csv mismatch)
            if (secondaryType == PersistenceType.CSV) {
                clearCsvFiles();
                // Force factory to clear its DAO cache after clearing CSV files
                // This prevents using stale DAO instances that might have cached file references
                DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
                factory.setPersistenceType(PersistenceType.MYSQL);
                factory.setPersistenceType(primaryType);
            }

            // Synchronize entities in dependency order
            List<Fan> syncedFans = syncFans(primaryType, secondaryType);
            syncVenueManagers(primaryType, secondaryType);
            syncVenues(primaryType, secondaryType);
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
     * Clears all CSV files by reinitializing them with only headers.
     * This prevents inconsistencies between related CSV files (e.g., users.csv vs fans.csv).
     */
    private void clearCsvFiles() {
        try {
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            reinitializeCsvFile(new java.io.File(dataDir, "users.csv"),
                    new String[]{"username", "password_hash", "full_name", "gender", "user_type"});
            reinitializeCsvFile(new java.io.File(dataDir, "fans.csv"),
                    new String[]{"username", "fav_team", "birthday"});
            reinitializeCsvFile(new java.io.File(dataDir, "venue_managers.csv"),
                    new String[]{"username", "company_name", "phone_number"});
            reinitializeCsvFile(new java.io.File(dataDir, "venues.csv"),
                    new String[]{"id", "name", "type", "address", "city", "max_capacity", "venue_manager_username"});
            reinitializeCsvFile(new java.io.File(dataDir, "bookings.csv"),
                    new String[]{"id", "game_date", "game_time", "home_team", "away_team", "venue_id", "fan_username", "status", "notified"});
            reinitializeCsvFile(new java.io.File(dataDir, "notifications.csv"),
                    new String[]{"id", "user_id", "type", "title", "message", "booking_id", "is_read", "created_at"});
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error clearing CSV files", e);
        }
    }

    /**
     * Reinitializes a CSV file with only its header row.
     */
    private void reinitializeCsvFile(java.io.File file, String[] header) throws java.io.IOException {
        try (com.opencsv.CSVWriter writer = new com.opencsv.CSVWriter(new java.io.FileWriter(file))) {
            writer.writeNext(header);
        }
        logger.log(Level.FINE, "Reinitialized: {0}", file.getName());
    }

    /**
     * Synchronizes fan data between primary and secondary persistence types.
     * <p>
     * This method compares fan records from both storage systems and:
     * <ul>
     *   <li>Copies missing fans from one system to another</li>
     *   <li>Resolves conflicts by prioritizing the primary persistence type</li>
     *   <li>Ensures data consistency across both systems</li>
     * </ul>
     * </p>
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
                FanBean beanToSave = createFanBeanFromModel(primaryFan, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getFanDao().saveFan(beanToSave);
            } else if (primaryFan == null && secondaryFan != null) {
                FanBean beanToSave = createFanBeanFromModel(secondaryFan, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getFanDao().saveFan(beanToSave);
            } else if (primaryFan != null && !primaryFan.isDataEquivalent(secondaryFan)) {
                logger.info("Sync Conflict: Different data for fan " + key + ". Primary source " + primary + " takes precedence.");
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
     * <p>
     * Similar to fan synchronization, this method ensures that venue manager
     * records are consistent across both storage systems, with conflict resolution
     * favoring the primary persistence type.
     * </p>
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
                VenueManagerBean bean = createVenueManagerBeanFromModel(primaryVM, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().saveVenueManager(bean);
            } else if (primaryVM == null && secondaryVM != null) {
                VenueManagerBean bean = createVenueManagerBeanFromModel(secondaryVM, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getVenueManagerDao().saveVenueManager(bean);
            } else if (primaryVM != null && !primaryVM.isDataEquivalent(secondaryVM)) {
                logger.info("Sync Conflict: Different data for venue manager " + key + ". Primary source " + primary + " takes precedence.");
                VenueManagerBean beanToUpdate = createVenueManagerBeanFromModel(primaryVM, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().updateVenueManager(primaryVM, beanToUpdate);
            }
        }
    }

    /**
     * Synchronizes venue data between primary and secondary persistence types.
     * <p>
     * This method ensures that venue records are consistent across both storage systems.
     * </p>
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
                VenueBean bean = createVenueBeanFromModel(primaryVenue);
                factory.setPersistenceType(secondary);
                factory.getVenueDao().saveVenue(bean);
            } else if (primaryVenue == null && secondaryVenue != null) {
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
    }

    /**
     * Synchronizes booking data for all fans between persistence types.
     * <p>
     * This method processes bookings for each fan individually, ensuring
     * that booking schedules are consistent across both storage systems.
     * Since bookings are fan-specific, this method requires the list
     * of synchronized fans to process.
     * </p>
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
            String username = fan.getUsername();

            factory.setPersistenceType(primary);
            Map<Integer, Booking> primaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(username));

            factory.setPersistenceType(secondary);
            Map<Integer, Booking> secondaryMap = listToMapBookings(factory.getBookingDao().retrieveBookingsByFan(username));

            Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
            allIds.addAll(secondaryMap.keySet());

            for (Integer id : allIds) {
                Booking primaryBooking = primaryMap.get(id);
                Booking secondaryBooking = secondaryMap.get(id);

                if (primaryBooking != null && secondaryBooking == null) {
                    BookingBean beanToSave = createBookingBeanFromModel(primaryBooking);
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().saveBooking(beanToSave);
                } else if (primaryBooking == null && secondaryBooking != null) {
                    BookingBean beanToSave = createBookingBeanFromModel(secondaryBooking);
                    factory.setPersistenceType(primary);
                    factory.getBookingDao().saveBooking(beanToSave);
                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    logger.info("Sync Conflict: Different data for booking " + id + ". Primary source " + primary + " takes precedence.");
                    BookingBean beanToUpdate = createBookingBeanFromModel(primaryBooking);
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().updateBooking(beanToUpdate);
                }
            }
        }
    }

    // ========== Helper Methods ==========

    /**
     * Converts a list of fans to a map with username as key.
     * <p>
     * This helper method facilitates efficient lookup and comparison
     * of fan records during synchronization.
     * </p>
     *
     * @param list List of fans to convert
     * @return Map with username as key and Fan object as value
     */
    private Map<String, Fan> listToMap(List<Fan> list) {
        Map<String, Fan> map = new HashMap<>();
        for (Fan f : list) {
            map.put(f.getUsername(), f);
        }
        return map;
    }

    /**
     * Converts a list of venue managers to a map with username as key.
     * <p>
     * This helper method facilitates efficient lookup and comparison
     * of venue manager records during synchronization.
     * </p>
     *
     * @param list List of venue managers to convert
     * @return Map with username as key and VenueManager object as value
     */
    private Map<String, VenueManager> listToMapVenueManager(List<VenueManager> list) {
        Map<String, VenueManager> map = new HashMap<>();
        for (VenueManager vm : list) {
            map.put(vm.getUsername(), vm);
        }
        return map;
    }

    /**
     * Converts a list of venues to a map with venue ID as key.
     * <p>
     * This helper method facilitates efficient lookup and comparison
     * of venue records during synchronization.
     * </p>
     *
     * @param list List of venues to convert
     * @return Map with venue ID as key and Venue object as value
     */
    private Map<Integer, Venue> listToMapVenues(List<Venue> list) {
        Map<Integer, Venue> map = new HashMap<>();
        for (Venue v : list) {
            map.put(v.getId(), v);
        }
        return map;
    }

    /**
     * Converts a list of bookings to a map with booking ID as key.
     * <p>
     * This helper method facilitates efficient lookup and comparison
     * of booking records during synchronization.
     * </p>
     *
     * @param list List of bookings to convert
     * @return Map with booking ID as key and Booking object as value
     */
    private Map<Integer, Booking> listToMapBookings(List<Booking> list) {
        Map<Integer, Booking> map = new HashMap<>();
        for (Booking b : list) {
            map.put(b.getId(), b);
        }
        return map;
    }

    /**
     * Creates a FanBean from a Fan model object for persistence operations.
     * <p>
     * This method retrieves the complete user information (including hashed password)
     * from the source persistence and creates a properly formatted bean for saving
     * or updating in the destination persistence.
     * </p>
     *
     * @param fan The fan model object to convert
     * @param factory The DAO factory facade for accessing persistence
     * @param sourcePersistence The persistence type to retrieve user data from
     * @return A FanBean with complete information for persistence operations
     * @throws DAOException if user data retrieval fails
     */
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

    /**
     * Creates a VenueManagerBean from a VenueManager model object for persistence operations.
     * <p>
     * This method retrieves the complete user information (including hashed password)
     * from the source persistence and creates a properly formatted bean for saving
     * or updating in the destination persistence.
     * </p>
     *
     * @param vm The venue manager model object to convert
     * @param factory The DAO factory facade for accessing persistence
     * @param sourcePersistence The persistence type to retrieve user data from
     * @return A VenueManagerBean with complete information for persistence operations
     * @throws DAOException if user data retrieval fails
     */
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

    /**
     * Creates a VenueBean from a Venue model object.
     * <p>
     * This helper method extracts primitive values from the Venue model
     * to create a lightweight Bean suitable for DAO operations.
     * </p>
     *
     * @param venue The Venue model object
     * @return A VenueBean with data from the model
     */
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
