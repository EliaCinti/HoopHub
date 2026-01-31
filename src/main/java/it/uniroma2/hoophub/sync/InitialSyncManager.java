package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.csv.CsvDaoConstants;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs initial data synchronization between persistence layers at application startup.
 *
 * <h3>Purpose</h3>
 * <p>When the application starts, data in CSV and MySQL may be out of sync
 * (e.g., due to direct database edits, manual CSV changes, or crashes during
 * previous sync operations). This manager ensures both persistence layers
 * contain identical data before the application begins normal operation.</p>
 *
 * <h3>Difference from CrossPersistenceSyncObserver</h3>
 * <ul>
 *   <li><b>InitialSyncManager</b>: Bulk synchronization at startup, compares ALL records</li>
 *   <li><b>CrossPersistenceSyncObserver</b>: Real-time sync, propagates individual changes</li>
 * </ul>
 *
 * <h3>Synchronization Strategy</h3>
 * <p>Uses a "primary wins" conflict resolution strategy:</p>
 * <ol>
 *   <li><b>Entity exists only in primary</b>: Copy to secondary</li>
 *   <li><b>Entity exists only in secondary</b>: Copy to primary</li>
 *   <li><b>Entity exists in both but differs</b>: Primary version overwrites secondary</li>
 *   <li><b>Entity exists in both and matches</b>: No action needed</li>
 * </ol>
 *
 * <h3>Sync Order (Dependency-aware)</h3>
 * <p>Entities are synced in dependency order to maintain referential integrity:</p>
 * <ol>
 *   <li><b>Fans</b>: No dependencies</li>
 *   <li><b>VenueManagers</b>: No dependencies</li>
 *   <li><b>Venues</b>: Depends on VenueManager (venue_manager_username FK)</li>
 *   <li><b>Bookings</b>: Depends on Fan and Venue (fan_username, venue_id FKs)</li>
 * </ol>
 *
 * <h3>CSV Clearing Strategy</h3>
 * <p>When secondary is CSV, files are cleared before sync to prevent inconsistencies
 * between related files (e.g., users.csv having entries that fans.csv doesn't).
 * This "clean slate" approach ensures integrity.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // At application startup
 * InitialSyncManager syncManager = new InitialSyncManager();
 * syncManager.performInitialSync(PersistenceType.MYSQL); // MySQL is primary
 * </pre>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CrossPersistenceSyncObserver
 * @see SyncContext
 */
public class InitialSyncManager {

    private static final Logger logger = Logger.getLogger(InitialSyncManager.class.getName());
    private static final String USERNAME_KEY = "username";

    /**
     * Performs complete initial synchronization between primary and secondary persistence.
     *
     * <p>Orchestrates the sync process:
     * <ol>
     *   <li>Determines secondary persistence type</li>
     *   <li>If secondary is CSV, clears all CSV files (clean slate)</li>
     *   <li>Syncs entities in dependency order: Fans → VenueManagers → Venues → Bookings</li>
     *   <li>Clears GlobalCache to ensure fresh data on subsequent requests</li>
     * </ol>
     * </p>
     *
     * <p>The Entire operation runs within SyncContext to prevent real-time observers
     * from triggering during bulk sync (which would cause redundant operations).</p>
     *
     * @param primaryType the persistence type that takes precedence in conflict resolution
     */
    public void performInitialSync(PersistenceType primaryType) {
        logger.info("Starting initial synchronization...");
        SyncContext.startSync();
        try {
            PersistenceType secondaryType = (primaryType == PersistenceType.MYSQL)
                    ? PersistenceType.CSV : PersistenceType.MYSQL;

            // Clear CSV files if secondary to prevent inconsistencies
            if (secondaryType == PersistenceType.CSV) {
                clearCsvFiles();
                // Force DAO cache clear after file reset
                DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
                factory.setPersistenceType(PersistenceType.MYSQL);
                factory.setPersistenceType(primaryType);
            }

            // Sync entities in dependency order
            List<Fan> syncedFans = syncFans(primaryType, secondaryType);
            syncVenueManagers(primaryType, secondaryType);
            syncVenues(primaryType, secondaryType);
            syncBookings(syncedFans, primaryType, secondaryType);

            logger.info("Initial synchronization completed successfully.");
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Initial synchronization failed.", e);
        } finally {
            // Clear cache to ensure fresh data
            GlobalCache.getInstance().clearAll();
            logger.info("Global Cache cleared to ensure data consistency.");
            SyncContext.endSync();
            logger.info("Real-time synchronization observers reactivated.");
        }
    }

    /**
     * Clears all CSV files by reinitializing them with only headers.
     *
     * <p>Prevents inconsistencies like:
     * <ul>
     *   <li>users.csv having a user that fans.csv doesn't have</li>
     *   <li>bookings.csv referencing a venue_id that venues.csv doesn't have</li>
     * </ul>
     * </p>
     */
    private void clearCsvFiles() {
        try {
            java.io.File dataDir = new java.io.File(CsvDaoConstants.CSV_BASE_DIR);
            if (!dataDir.exists()) {
                boolean created = dataDir.mkdirs();
                if (!created) {
                    logger.log(Level.SEVERE, "Cannot create directory: {0}", dataDir.getAbsolutePath());
                    throw new IOException("Failed to create directory: " + dataDir.getAbsolutePath());
                }
            }

            // Reinitialize each CSV with only header row
            reinitializeCsvFile(new java.io.File(dataDir, "users.csv"),
                    new String[]{USERNAME_KEY, "password_hash", "full_name", "gender", "user_type"});
            reinitializeCsvFile(new java.io.File(dataDir, "fans.csv"),
                    new String[]{USERNAME_KEY, "fav_team", "birthday"});
            reinitializeCsvFile(new java.io.File(dataDir, "venue_managers.csv"),
                    new String[]{USERNAME_KEY, "company_name", "phone_number"});
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
     * Reinitialized a CSV file with only its header row.
     */
    private void reinitializeCsvFile(java.io.File file, String[] header) throws java.io.IOException {
        try (com.opencsv.CSVWriter writer = new com.opencsv.CSVWriter(new java.io.FileWriter(file))) {
            writer.writeNext(header);
        }
        logger.log(Level.FINE, "Reinitialized: {0}", file.getName());
    }

    /**
     * Synchronizes Fan records between primary and secondary persistence.
     *
     * <p>For each unique username across both systems:
     * <ul>
     *   <li>Only in primary → copy to secondary (with password hash)</li>
     *   <li>Only in secondary → copy to primary (with password hash)</li>
     *   <li>In both but different → primary overwrites secondary</li>
     * </ul>
     * </p>
     *
     * <p><b>Password handling:</b> When copying a Fan, the password hash must be
     * retrieved separately via UserDao since Fan objects don't expose passwords.
     * Uses {@link #retrieveCompleteFan} to build a complete Model with hash.</p>
     *
     * @param primary   primary persistence type
     * @param secondary secondary persistence type
     * @return list of all synced Fans (for subsequent Booking sync)
     * @throws DAOException if data access fails
     */
    private List<Fan> syncFans(PersistenceType primary, PersistenceType secondary) throws DAOException {
        logger.info("Synchronizing fans...");
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        // Load all Fans from both persistence types into maps for comparison
        factory.setPersistenceType(primary);
        Map<String, Fan> primaryMap = listToMap(factory.getFanDao().retrieveAllFans());

        factory.setPersistenceType(secondary);
        Map<String, Fan> secondaryMap = listToMap(factory.getFanDao().retrieveAllFans());

        // Get union of all usernames
        Set<String> allKeys = new HashSet<>(primaryMap.keySet());
        allKeys.addAll(secondaryMap.keySet());

        for (String key : allKeys) {
            Fan primaryFan = primaryMap.get(key);
            Fan secondaryFan = secondaryMap.get(key);

            if (primaryFan != null && secondaryFan == null) {
                // CASE 1: Only in primary → copy to secondary
                Fan fanToSave = retrieveCompleteFan(primaryFan, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getFanDao().saveFan(fanToSave);

            } else if (primaryFan == null && secondaryFan != null) {
                // CASE 2: Only in secondary → copy to primary
                Fan fanToSave = retrieveCompleteFan(secondaryFan, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getFanDao().saveFan(fanToSave);

            } else if (primaryFan != null && !primaryFan.isDataEquivalent(secondaryFan)) {
                // CASE 3: Conflict → primary wins
                logger.log(Level.INFO, "Sync Conflict: Different data for fan {0}. Primary source {1} takes precedence.",
                        new Object[]{key, primary});
                factory.setPersistenceType(secondary);
                factory.getFanDao().updateFan(primaryFan);
            }
        }

        // Return all Fans from primary for Booking sync
        factory.setPersistenceType(primary);
        return factory.getFanDao().retrieveAllFans();
    }

    /**
     * Synchronizes VenueManager records between primary and secondary persistence.
     *
     * <p>Same strategy as Fan sync. Password hash retrieved via UserDao.</p>
     *
     * @param primary   primary persistence type
     * @param secondary secondary persistence type
     * @throws DAOException if data access fails
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
                VenueManager vmToSave = retrieveCompleteVenueManager(primaryVM, factory, primary);
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().saveVenueManager(vmToSave);

            } else if (primaryVM == null && secondaryVM != null) {
                VenueManager vmToSave = retrieveCompleteVenueManager(secondaryVM, factory, secondary);
                factory.setPersistenceType(primary);
                factory.getVenueManagerDao().saveVenueManager(vmToSave);

            } else if (primaryVM != null && !primaryVM.isDataEquivalent(secondaryVM)) {
                logger.log(Level.INFO, "Sync Conflict: Different data for venue manager {0}. Primary source {1} takes precedence.",
                        new Object[]{key, primary});
                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().updateVenueManager(primaryVM);
            }
        }
    }

    /**
     * Synchronizes Venue records between primary and secondary persistence.
     *
     * <p>Venue objects are complete (no hidden fields like password),
     * so they can be copied directly without special handling.</p>
     *
     * @param primary   primary persistence type
     * @param secondary secondary persistence type
     * @throws DAOException if data access fails
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
                factory.setPersistenceType(secondary);
                factory.getVenueDao().saveVenue(primaryVenue);

            } else if (primaryVenue == null && secondaryVenue != null) {
                factory.setPersistenceType(primary);
                factory.getVenueDao().saveVenue(secondaryVenue);

            } else if (primaryVenue != null && !primaryVenue.equals(secondaryVenue)) {
                logger.log(Level.INFO, "Sync Conflict: Different data for venue {0}. Primary source {1} takes precedence.",
                        new Object[]{id, primary});
                factory.setPersistenceType(secondary);
                factory.getVenueDao().updateVenue(primaryVenue);
            }
        }
    }

    /**
     * Synchronizes Booking records for all Fans between persistence types.
     *
     * <p>Processes bookings per-fan to maintain association integrity.
     * Booking objects are complete (no hidden fields).</p>
     *
     * @param syncedFans list of Fans whose bookings should be synced
     * @param primary    primary persistence type
     * @param secondary  secondary persistence type
     * @throws DAOException if data access fails
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
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().saveBooking(primaryBooking);

                } else if (primaryBooking == null && secondaryBooking != null) {
                    factory.setPersistenceType(primary);
                    factory.getBookingDao().saveBooking(secondaryBooking);

                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    logger.log(Level.INFO, "Sync Conflict: Different data for booking {0}. Primary source {1} takes precedence.",
                            new Object[]{id, primary});
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().updateBooking(primaryBooking);
                }
            }
        }
    }

    // ========== Helper Methods ==========

    /** Converts Fan list to Map keyed by username. */
    private Map<String, Fan> listToMap(List<Fan> list) {
        Map<String, Fan> map = new HashMap<>();
        for (Fan f : list) {
            map.put(f.getUsername(), f);
        }
        return map;
    }

    /** Converts VenueManager list to Map keyed by username. */
    private Map<String, VenueManager> listToMapVenueManager(List<VenueManager> list) {
        Map<String, VenueManager> map = new HashMap<>();
        for (VenueManager vm : list) {
            map.put(vm.getUsername(), vm);
        }
        return map;
    }

    /** Converts Venue list to Map keyed by ID. */
    private Map<Integer, Venue> listToMapVenues(List<Venue> list) {
        Map<Integer, Venue> map = new HashMap<>();
        for (Venue v : list) {
            map.put(v.getId(), v);
        }
        return map;
    }

    /** Converts Booking list to Map keyed by ID. */
    private Map<Integer, Booking> listToMapBookings(List<Booking> list) {
        Map<Integer, Booking> map = new HashMap<>();
        for (Booking b : list) {
            map.put(b.getId(), b);
        }
        return map;
    }

    /**
     * Retrieves a complete Fan Model including password hash from source persistence.
     *
     * <p>Required because Fan objects returned by retrieveAllFans() don't include
     * the password hash (security). For SAVE operations, we need the hash.</p>
     *
     * @param fan               the Fan with basic data
     * @param factory           the DAO factory
     * @param sourcePersistence where to retrieve the password from
     * @return complete Fan Model with password hash
     */
    private Fan retrieveCompleteFan(Fan fan, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();

            // retrieveUser returns [username, hash, fullname, gender, type]
            String[] userInfo = userDao.retrieveUser(fan.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            return new Fan.Builder()
                    .username(fan.getUsername())
                    .password(hashedPassword)
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .birthday(fan.getBirthday())
                    .favTeam(fan.getFavTeam())
                    .build();
        } finally {
            factory.setPersistenceType(originalType);
        }
    }

    /**
     * Retrieves a complete VenueManager Model including password hash from source persistence.
     *
     * @param vm                the VenueManager with basic data
     * @param factory           the DAO factory
     * @param sourcePersistence where to retrieve the password from
     * @return complete VenueManager Model with password hash
     */
    private VenueManager retrieveCompleteVenueManager(VenueManager vm, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();

            String[] userInfo = userDao.retrieveUser(vm.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            return new VenueManager.Builder()
                    .username(vm.getUsername())
                    .password(hashedPassword)
                    .fullName(vm.getFullName())
                    .gender(vm.getGender())
                    .companyName(vm.getCompanyName())
                    .phoneNumber(vm.getPhoneNumber())
                    .managedVenues(new ArrayList<>()) // Venues synced separately
                    .build();
        } finally {
            factory.setPersistenceType(originalType);
        }
    }
}