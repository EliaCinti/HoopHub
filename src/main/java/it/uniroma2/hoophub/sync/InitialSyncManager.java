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

            // SIMPLIFIED APPROACH: If secondary is CSV, clear it completely and copy everything from primary
            if (secondaryType == PersistenceType.CSV) {
                logger.info("Simple sync strategy: Clear CSV and copy everything from MySQL");
                clearSecondaryData(secondaryType);
                copyAllFromPrimaryToSecondary(primaryType, secondaryType);
            } else {
                // If secondary is MySQL, use intelligent merge
                performNormalSync(primaryType, secondaryType);
            }

            logger.info("Initial synchronization completed successfully.");
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Initial synchronization failed.", e);
        } finally {
            SyncContext.endSync();
            logger.info("Real-time synchronization observers reactivated.");
        }
    }

    /**
     * Copies ALL data from primary to secondary without any checks.
     * This is the simplest and most reliable sync strategy.
     *
     * IMPORTANT: We create DAO instances directly WITHOUT using the factory
     * to avoid having observers registered during initial sync. This prevents
     * any observer notifications during the synchronization process.
     */
    private void copyAllFromPrimaryToSecondary(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Copying all data from " + primary + " to " + secondary);

        // Create DAOs directly without factory to avoid observer registration
        it.uniroma2.hoophub.dao.FanDao primaryFanDao = createDaoWithoutObservers(primary, "fan");
        it.uniroma2.hoophub.dao.VenueManagerDao primaryVmDao = createDaoWithoutObservers(primary, "venuemanager");
        it.uniroma2.hoophub.dao.VenueDao primaryVenueDao = createDaoWithoutObservers(primary, "venue");
        it.uniroma2.hoophub.dao.BookingDao primaryBookingDao = createDaoWithoutObservers(primary, "booking");

        it.uniroma2.hoophub.dao.FanDao secondaryFanDao = createDaoWithoutObservers(secondary, "fan");
        it.uniroma2.hoophub.dao.VenueManagerDao secondaryVmDao = createDaoWithoutObservers(secondary, "venuemanager");
        it.uniroma2.hoophub.dao.VenueDao secondaryVenueDao = createDaoWithoutObservers(secondary, "venue");
        it.uniroma2.hoophub.dao.BookingDao secondaryBookingDao = createDaoWithoutObservers(secondary, "booking");
        it.uniroma2.hoophub.dao.UserDao primaryUserDao = createDaoWithoutObservers(primary, "user");

        // Get all data from primary
        List<Fan> fans = primaryFanDao.retrieveAllFans();
        List<VenueManager> venueManagers = primaryVmDao.retrieveAllVenueManagers();
        List<Venue> venues = primaryVenueDao.retrieveAllVenues();

        logger.info("Found in primary: " + fans.size() + " fans, " + venueManagers.size() + " venue managers, " + venues.size() + " venues");

        // Copy fans
        for (Fan fan : fans) {
            logger.info("Copying fan: " + fan.getUsername());
            FanBean bean = createFanBeanFromModel(fan, primaryUserDao);
            secondaryFanDao.saveFan(bean);
        }

        // Copy venue managers
        for (VenueManager vm : venueManagers) {
            logger.info("Copying venue manager: " + vm.getUsername());
            VenueManagerBean bean = createVenueManagerBeanFromModel(vm, primaryUserDao);
            secondaryVmDao.saveVenueManager(bean);
        }

        // Copy venues
        for (Venue venue : venues) {
            logger.info("Copying venue: " + venue.getName());
            VenueBean bean = createVenueBeanFromModel(venue);
            secondaryVenueDao.saveVenue(bean);
        }

        // Copy bookings
        for (Fan fan : fans) {
            List<Booking> bookings = primaryBookingDao.retrieveBookingsByFan(fan.getUsername());
            for (Booking booking : bookings) {
                logger.info("Copying booking: " + booking.getId() + " for fan " + fan.getUsername());
                BookingBean bean = createBookingBeanFromModel(booking);
                secondaryBookingDao.saveBooking(bean);
            }
        }

        logger.info("All data copied successfully");
    }

    /**
     * Creates a DAO instance directly without using the factory,
     * ensuring no observers are registered.
     *
     * @param type The persistence type (MYSQL or CSV)
     * @param daoType The type of DAO to create
     * @return A DAO instance without observers
     */
    @SuppressWarnings("unchecked")
    private <T> T createDaoWithoutObservers(PersistenceType type, String daoType) {
        if (type == PersistenceType.MYSQL) {
            return switch (daoType.toLowerCase()) {
                case "fan" -> (T) new it.uniroma2.hoophub.dao.mysql.FanDaoMySql();
                case "venuemanager" -> (T) new it.uniroma2.hoophub.dao.mysql.VenueManagerDaoMySql();
                case "venue" -> (T) new it.uniroma2.hoophub.dao.mysql.VenueDaoMySql();
                case "booking" -> (T) new it.uniroma2.hoophub.dao.mysql.BookingDaoMySql();
                case "user" -> (T) new it.uniroma2.hoophub.dao.mysql.UserDaoMySql();
                default -> throw new IllegalArgumentException("Unknown DAO type: " + daoType);
            };
        } else {
            return switch (daoType.toLowerCase()) {
                case "fan" -> (T) new it.uniroma2.hoophub.dao.csv.FanDaoCsv();
                case "venuemanager" -> (T) new it.uniroma2.hoophub.dao.csv.VenueManagerDaoCsv();
                case "venue" -> (T) new it.uniroma2.hoophub.dao.csv.VenueDaoCsv();
                case "booking" -> (T) new it.uniroma2.hoophub.dao.csv.BookingDaoCsv();
                case "user" -> (T) new it.uniroma2.hoophub.dao.csv.UserDaoCsv();
                default -> throw new IllegalArgumentException("Unknown DAO type: " + daoType);
            };
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

        logger.info("Clearing all data from CSV files by reinitializing them...");

        try {
            // Clear CSV files by reinitializing them (keeps headers, removes all data)
            java.io.File dataDir = new java.io.File("data");

            // Reinitialize each CSV file with just its header
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

            logger.info("CSV data cleared successfully - all files reinitialized");

            // Verify the files are actually empty by checking users.csv
            DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
            factory.setPersistenceType(PersistenceType.CSV);
            try {
                List<Fan> csvFans = factory.getFanDao().retrieveAllFans();
                logger.info("Verification after clearing: CSV fans count = " + csvFans.size());
            } catch (DAOException e) {
                logger.log(Level.WARNING, "Could not verify CSV clearing", e);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during CSV data clearing", e);
            throw new DAOException("Failed to clear CSV data", e);
        }
    }

    /**
     * Reinitializes a CSV file with only its header row.
     *
     * @param file The CSV file to reinitialize
     * @param header The header row
     */
    private void reinitializeCsvFile(java.io.File file, String[] header) throws java.io.IOException {
        try (com.opencsv.CSVWriter writer = new com.opencsv.CSVWriter(new java.io.FileWriter(file))) {
            writer.writeNext(header);
        }
        logger.log(Level.FINE, "Reinitialized CSV file: {0}", file.getName());
    }

    /**
     * Synchronizes fan data between primary and secondary persistence types.
     * Uses DAOs without observers to avoid circular notifications.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @return List of synchronized fans from the primary persistence
     * @throws DAOException if fan data access or synchronization fails
     */
    private List<Fan> syncFans(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing fans...");

        // Create DAOs without observers
        it.uniroma2.hoophub.dao.FanDao primaryFanDao = createDaoWithoutObservers(primary, "fan");
        it.uniroma2.hoophub.dao.FanDao secondaryFanDao = createDaoWithoutObservers(secondary, "fan");
        it.uniroma2.hoophub.dao.UserDao primaryUserDao = createDaoWithoutObservers(primary, "user");
        it.uniroma2.hoophub.dao.UserDao secondaryUserDao = createDaoWithoutObservers(secondary, "user");

        Map<String, Fan> primaryMap = listToMap(primaryFanDao.retrieveAllFans());
        Map<String, Fan> secondaryMap = listToMap(secondaryFanDao.retrieveAllFans());

        Set<String> allKeys = new HashSet<>(primaryMap.keySet());
        allKeys.addAll(secondaryMap.keySet());

        for (String key : allKeys) {
            Fan primaryFan = primaryMap.get(key);
            Fan secondaryFan = secondaryMap.get(key);

            if (primaryFan != null && secondaryFan == null) {
                logger.info("Sync: Copying fan " + key + " from " + primary + " to " + secondary);
                FanBean beanToSave = createFanBeanFromModel(primaryFan, primaryUserDao);
                secondaryFanDao.saveFan(beanToSave);
            } else if (primaryFan == null && secondaryFan != null) {
                logger.info("Sync: Copying fan " + key + " from " + secondary + " to " + primary);
                FanBean beanToSave = createFanBeanFromModel(secondaryFan, secondaryUserDao);
                primaryFanDao.saveFan(beanToSave);
            } else if (primaryFan != null && !primaryFan.isDataEquivalent(secondaryFan)) {
                logger.info("Sync Conflict: Different data for fan " + key + ". Primary takes precedence.");
                FanBean beanToUpdate = createFanBeanFromModel(primaryFan, primaryUserDao);
                secondaryFanDao.updateFan(primaryFan, beanToUpdate);
            }
        }

        return primaryFanDao.retrieveAllFans();
    }

    /**
     * Synchronizes venue manager data between primary and secondary persistence types.
     * Uses DAOs without observers to avoid circular notifications.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if venue manager data access or synchronization fails
     */
    private void syncVenueManagers(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing venue managers...");

        // Create DAOs without observers
        it.uniroma2.hoophub.dao.VenueManagerDao primaryVmDao = createDaoWithoutObservers(primary, "venuemanager");
        it.uniroma2.hoophub.dao.VenueManagerDao secondaryVmDao = createDaoWithoutObservers(secondary, "venuemanager");
        it.uniroma2.hoophub.dao.UserDao primaryUserDao = createDaoWithoutObservers(primary, "user");
        it.uniroma2.hoophub.dao.UserDao secondaryUserDao = createDaoWithoutObservers(secondary, "user");

        Map<String, VenueManager> primaryMap = listToMapVenueManager(primaryVmDao.retrieveAllVenueManagers());
        Map<String, VenueManager> secondaryMap = listToMapVenueManager(secondaryVmDao.retrieveAllVenueManagers());

        Set<String> allKeys = new HashSet<>(primaryMap.keySet());
        allKeys.addAll(secondaryMap.keySet());

        for (String key : allKeys) {
            VenueManager primaryVM = primaryMap.get(key);
            VenueManager secondaryVM = secondaryMap.get(key);

            if (primaryVM != null && secondaryVM == null) {
                logger.info("Sync: Copying venue manager " + key + " from " + primary + " to " + secondary);
                VenueManagerBean bean = createVenueManagerBeanFromModel(primaryVM, primaryUserDao);
                secondaryVmDao.saveVenueManager(bean);
            } else if (primaryVM == null && secondaryVM != null) {
                logger.info("Sync: Copying venue manager " + key + " from " + secondary + " to " + primary);
                VenueManagerBean bean = createVenueManagerBeanFromModel(secondaryVM, secondaryUserDao);
                primaryVmDao.saveVenueManager(bean);
            } else if (primaryVM != null && !primaryVM.isDataEquivalent(secondaryVM)) {
                logger.info("Sync Conflict: Different data for venue manager " + key + ". Primary takes precedence.");
                VenueManagerBean beanToUpdate = createVenueManagerBeanFromModel(primaryVM, primaryUserDao);
                secondaryVmDao.updateVenueManager(primaryVM, beanToUpdate);
            }
        }
    }

    /**
     * Synchronizes venue data between primary and secondary persistence types.
     * Uses DAOs without observers to avoid circular notifications.
     *
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if venue data access or synchronization fails
     */
    private void syncVenues(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing venues...");

        // Create DAOs without observers
        it.uniroma2.hoophub.dao.VenueDao primaryVenueDao = createDaoWithoutObservers(primary, "venue");
        it.uniroma2.hoophub.dao.VenueDao secondaryVenueDao = createDaoWithoutObservers(secondary, "venue");

        Map<Integer, Venue> primaryMap = listToMapVenues(primaryVenueDao.retrieveAllVenues());
        Map<Integer, Venue> secondaryMap = listToMapVenues(secondaryVenueDao.retrieveAllVenues());

        Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
        allIds.addAll(secondaryMap.keySet());

        for (Integer id : allIds) {
            Venue primaryVenue = primaryMap.get(id);
            Venue secondaryVenue = secondaryMap.get(id);

            if (primaryVenue != null && secondaryVenue == null) {
                logger.info("Sync: Copying venue " + id + " from " + primary + " to " + secondary);
                VenueBean bean = createVenueBeanFromModel(primaryVenue);
                secondaryVenueDao.saveVenue(bean);
            } else if (primaryVenue == null && secondaryVenue != null) {
                logger.info("Sync: Copying venue " + id + " from " + secondary + " to " + primary);
                VenueBean bean = createVenueBeanFromModel(secondaryVenue);
                primaryVenueDao.saveVenue(bean);
            } else if (primaryVenue != null && !primaryVenue.equals(secondaryVenue)) {
                logger.info("Sync Conflict: Different data for venue " + id + ". Primary takes precedence.");
                VenueBean beanToUpdate = createVenueBeanFromModel(primaryVenue);
                secondaryVenueDao.updateVenue(beanToUpdate);
            }
        }
    }

    /**
     * Synchronizes booking data for all fans between persistence types.
     * Uses DAOs without observers to avoid circular notifications.
     *
     * @param syncedFans List of fans whose bookings should be synchronized
     * @param primary The primary persistence type
     * @param secondary The secondary persistence type
     * @throws DAOException if booking data access or synchronization fails
     */
    private void syncBookings(List<Fan> syncedFans, PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing bookings...");

        // Create DAOs without observers
        it.uniroma2.hoophub.dao.BookingDao primaryBookingDao = createDaoWithoutObservers(primary, "booking");
        it.uniroma2.hoophub.dao.BookingDao secondaryBookingDao = createDaoWithoutObservers(secondary, "booking");

        for (Fan fan : syncedFans) {
            Map<Integer, Booking> primaryMap = listToMapBookings(primaryBookingDao.retrieveBookingsByFan(fan.getUsername()));
            Map<Integer, Booking> secondaryMap = listToMapBookings(secondaryBookingDao.retrieveBookingsByFan(fan.getUsername()));

            Set<Integer> allIds = new HashSet<>(primaryMap.keySet());
            allIds.addAll(secondaryMap.keySet());

            for (Integer id : allIds) {
                Booking primaryBooking = primaryMap.get(id);
                Booking secondaryBooking = secondaryMap.get(id);

                if (primaryBooking != null && secondaryBooking == null) {
                    logger.info("Sync: Copying booking " + id + " from " + primary + " to " + secondary);
                    BookingBean beanToSave = createBookingBeanFromModel(primaryBooking);
                    secondaryBookingDao.saveBooking(beanToSave);
                } else if (primaryBooking == null && secondaryBooking != null) {
                    logger.info("Sync: Copying booking " + id + " from " + secondary + " to " + primary);
                    BookingBean beanToSave = createBookingBeanFromModel(secondaryBooking);
                    primaryBookingDao.saveBooking(beanToSave);
                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    logger.info("Sync Conflict: Different data for booking " + id + ". Primary takes precedence.");
                    BookingBean beanToUpdate = createBookingBeanFromModel(primaryBooking);
                    secondaryBookingDao.updateBooking(beanToUpdate);
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

    private FanBean createFanBeanFromModel(Fan fan, UserDao userDao) throws DAOException {
        logger.info("Creating FanBean from model: fan.username=" + fan.getUsername());
        String[] userInfo = userDao.retrieveUser(fan.getUsername());
        String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

        FanBean bean = new FanBean.Builder()
                .username(fan.getUsername())
                .password(hashedPassword)
                .fullName(fan.getFullName())
                .gender(fan.getGender())
                .birthday(fan.getBirthday())
                .favTeam(fan.getFavTeam())
                .type("FAN")
                .build();
        logger.info("Created FanBean: username=" + bean.getUsername() + ", fullName=" + bean.getFullName());
        return bean;
    }

    private VenueManagerBean createVenueManagerBeanFromModel(VenueManager vm, UserDao userDao) throws DAOException {
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