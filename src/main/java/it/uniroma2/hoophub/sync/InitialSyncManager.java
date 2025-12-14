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
    private static final String USERNAME_KEY = "username";
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
            GlobalCache.getInstance().clearAll();
            logger.info("Global Cache cleared to ensure data consistency.");
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
            java.io.File dataDir = new java.io.File(CsvDaoConstants.CSV_BASE_DIR);
            if (!dataDir.exists()) {
                boolean created = dataDir.mkdirs();
                if (!created) {
                    logger.log(Level.SEVERE, "Impossibile creare la directory: {0}", dataDir.getAbsolutePath());
                    throw new IOException("Failed to create directory: " + dataDir.getAbsolutePath());
                }
            }

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
                // CASO 1: Esiste solo su Primary -> Copia su Secondary
                // Recuperiamo il modello completo (con password) dalla Primary
                Fan fanToSave = retrieveCompleteFan(primaryFan, factory, primary);

                factory.setPersistenceType(secondary);
                factory.getFanDao().saveFan(fanToSave); // Passiamo il Model!

            } else if (primaryFan == null && secondaryFan != null) {
                // CASO 2: Esiste solo su Secondary -> Copia su Primary
                // Recuperiamo il modello completo dalla Secondary
                Fan fanToSave = retrieveCompleteFan(secondaryFan, factory, secondary);

                factory.setPersistenceType(primary);
                factory.getFanDao().saveFan(fanToSave); // Passiamo il Model!

            } else if (primaryFan != null && !primaryFan.isDataEquivalent(secondaryFan)) {
                // CASO 3: Conflitto -> Vince Primary
                logger.log(Level.INFO, "Sync Conflict: Different data for fan {0}. Primary source {1} takes precedence.",
                        new Object[]{key, primary});

                factory.setPersistenceType(secondary);
                // Per l'update non serve la password (aggiorniamo solo anagrafica),
                // quindi possiamo passare direttamente l'oggetto primaryFan
                factory.getFanDao().updateFan(primaryFan);
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
                // CASO 1: Copia da Primary a Secondary
                VenueManager vmToSave = retrieveCompleteVenueManager(primaryVM, factory, primary);

                factory.setPersistenceType(secondary);
                factory.getVenueManagerDao().saveVenueManager(vmToSave); // Model-First

            } else if (primaryVM == null && secondaryVM != null) {
                // CASO 2: Copia da Secondary a Primary
                VenueManager vmToSave = retrieveCompleteVenueManager(secondaryVM, factory, secondary);

                factory.setPersistenceType(primary);
                factory.getVenueManagerDao().saveVenueManager(vmToSave); // Model-First

            } else if (primaryVM != null && !primaryVM.isDataEquivalent(secondaryVM)) {
                // CASO 3: Conflitto -> Vince Primary (Update)
                logger.log(Level.INFO, "Sync Conflict: Different data for venue manager {0}. Primary source {1} takes precedence.",
                        new Object[]{key, primary});

                factory.setPersistenceType(secondary);
                // Per l'update passiamo direttamente il model Primary (la password non serve aggiornarla qui)
                factory.getVenueManagerDao().updateVenueManager(primaryVM);
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
                // CASO 1: Manca su Secondary -> Copia da Primary
                // Passiamo direttamente il Model!
                factory.setPersistenceType(secondary);
                factory.getVenueDao().saveVenue(primaryVenue);

            } else if (primaryVenue == null && secondaryVenue != null) {
                // CASO 2: Manca su Primary -> Copia da Secondary
                // Passiamo direttamente il Model!
                factory.setPersistenceType(primary);
                factory.getVenueDao().saveVenue(secondaryVenue);

            } else if (primaryVenue != null && !primaryVenue.equals(secondaryVenue)) {
                // CASO 3: Conflitto -> Vince Primary (Update)
                logger.log(Level.INFO, "Sync Conflict: Different data for venue {0}. Primary source {1} takes precedence.",
                        new Object[]{id, primary});

                factory.setPersistenceType(secondary);
                // Update diretto con il Model
                factory.getVenueDao().updateVenue(primaryVenue);
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
                    // CASO 1: Esiste solo su Primary -> Salva su Secondary
                    // Passiamo direttamente il Model!
                    factory.setPersistenceType(secondary);
                    factory.getBookingDao().saveBooking(primaryBooking);

                } else if (primaryBooking == null && secondaryBooking != null) {
                    // CASO 2: Esiste solo su Secondary -> Salva su Primary
                    // Passiamo direttamente il Model!
                    factory.setPersistenceType(primary);
                    factory.getBookingDao().saveBooking(secondaryBooking);

                } else if (primaryBooking != null && !primaryBooking.isDataEquivalent(secondaryBooking)) {
                    // CASO 3: Conflitto -> Vince Primary (Update)
                    logger.log(Level.INFO, "Sync Conflict: Different data for booking {0}. Primary source {1} takes precedence.",
                            new Object[]{id, primary});

                    factory.setPersistenceType(secondary);
                    // Update diretto con il Model (nessuna bean necessaria)
                    factory.getBookingDao().updateBooking(primaryBooking);
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
     * Recupera un oggetto Fan completo (inclusa la password hashata) dalla persistenza sorgente.
     * Necessario per le operazioni di SAVE che richiedono tutti i dati.
     */
    private Fan retrieveCompleteFan(Fan fan, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            // Switch alla persistenza sorgente per leggere i dati completi (inclusa password)
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();

            // retrieveUser restituisce l'array [username, hash, fullname, gender, type]
            String[] userInfo = userDao.retrieveUser(fan.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            // Ricostruiamo il Model completo
            return new Fan.Builder()
                    .username(fan.getUsername())
                    .password(hashedPassword) // Fondamentale per il salvataggio!
                    .fullName(fan.getFullName())
                    .gender(fan.getGender())
                    .birthday(fan.getBirthday())
                    .favTeam(fan.getFavTeam())
                    .build();
        } finally {
            // Ripristiniamo il tipo di persistenza originale
            factory.setPersistenceType(originalType);
        }
    }

    /**
     * Recupera un oggetto VenueManager completo (inclusa la password hashata) dalla persistenza sorgente.
     * Necessario per le operazioni di SAVE che richiedono tutti i dati.
     */
    private VenueManager retrieveCompleteVenueManager(VenueManager vm, DaoFactoryFacade factory, PersistenceType sourcePersistence) throws DAOException {
        PersistenceType originalType = factory.getPersistenceType();
        try {
            // Switch alla persistenza sorgente per leggere i dati completi
            factory.setPersistenceType(sourcePersistence);
            UserDao userDao = factory.getUserDao();

            String[] userInfo = userDao.retrieveUser(vm.getUsername());
            String hashedPassword = (userInfo != null && userInfo.length > 1) ? userInfo[1] : "";

            // Ricostruiamo il Model completo
            return new VenueManager.Builder()
                    .username(vm.getUsername())
                    .password(hashedPassword) // La password hashata è fondamentale
                    .fullName(vm.getFullName())
                    .gender(vm.getGender())
                    .companyName(vm.getCompanyName())
                    .phoneNumber(vm.getPhoneNumber())
                    // In fase di sync iniziale del manager, la lista venues può essere vuota.
                    // Le venue verranno sincronizzate subito dopo dal metodo syncVenues.
                    .managedVenues(new ArrayList<>())
                    .build();
        } finally {
            factory.setPersistenceType(originalType);
        }
    }
}
