package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.dao.csv.CsvDaoConstants;
import it.uniroma2.hoophub.dao.csv.UserDaoCsv;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages initial synchronization with master-slave strategy.
 *
 * <p><b>Strategy:</b> MySQL is always the master.
 * At startup, CSV files are
 * wiped and repopulated from MySQL to ensure consistency.</p>
 *
 * <p><b>Important:</b> This ensures that CSV ID counters are properly aligned
 * with existing data, preventing duplicate key errors during real-time sync.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class InitialSyncManager {

    private static final Logger LOGGER = Logger.getLogger(InitialSyncManager.class.getName());

    /**
     * Performs initial synchronization based on the primary persistence type.
     *
     * @param primaryType the persistence type selected as primary for this session
     */
    public void performInitialSync(PersistenceType primaryType) {
        if (primaryType == PersistenceType.IN_MEMORY) {
            LOGGER.info("Skipping sync for IN_MEMORY persistence.");
            return;
        }

        boolean mysqlAvailable = testMySqlConnection();

        if (!mysqlAvailable) {
            LOGGER.warning("MySQL unreachable. Using existing local data without sync.");
            return;
        }

        LOGGER.info("MySQL Connection OK. Starting Initial Synchronization (MySQL -> CSV)...");

        performMySqlToCsvSync();

        LOGGER.info(">>> Initial Synchronization Completed.");
    }

    private boolean testMySqlConnection() {
        try {
            return ConnectionFactory.testConnection();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "MySQL connection test failed: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Performs master-slave sync: MySQL (master) -> CSV (slave).
     * Wipes CSV files and repopulates from MySQL.
     */
    private void performMySqlToCsvSync() {
        SyncContext.startSync();
        PersistenceType originalType = DaoFactoryFacade.getInstance().getPersistenceType();

        try {
            // Clear caches and CSV files
            GlobalCache.getInstance().clearAll();
            wipeAllCsvFiles();

            DaoFactoryFacade dao = DaoFactoryFacade.getInstance();

            // Sync in dependency order
            syncUsersAndRoles(dao);
            syncVenues(dao);
            syncBookings(dao);
            syncNotifications(dao);

            LOGGER.info("CSV files aligned with MySQL.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Critical error during sync", e);
        } finally {
            DaoFactoryFacade.getInstance().setPersistenceType(originalType);
            SyncContext.endSync();
        }
    }

    /**
     * Wipes all CSV data files for clean sync.
     */
    private void wipeAllCsvFiles() {
        String[] files = {
                "users.csv", "fans.csv", "venue_managers.csv",
                "venues.csv", "venue_teams.csv",
                "bookings.csv", "notifications.csv"
        };

        for (String fileName : files) {
            File file = new File(CsvDaoConstants.CSV_BASE_DIR + fileName);
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not delete: {0}", fileName);
            }
        }
        LOGGER.info("CSV files wiped for clean sync.");
    }

    // ========================================================================
    // SYNC METHODS
    // ========================================================================

    private void syncUsersAndRoles(DaoFactoryFacade dao) throws DAOException {
        dao.setPersistenceType(PersistenceType.MYSQL);
        List<Fan> dbFans = dao.getFanDao().retrieveAllFans();
        List<VenueManager> dbManagers = dao.getVenueManagerDao().retrieveAllVenueManagers();
        UserDao mysqlUserDao = dao.getUserDao();

        dao.setPersistenceType(PersistenceType.CSV);

        for (Fan f : dbFans) {
            enrichAndSaveFan(f, mysqlUserDao, dao);
        }
        for (VenueManager vm : dbManagers) {
            enrichAndSaveManager(vm, mysqlUserDao, dao);
        }

        LOGGER.log(Level.INFO, "Synced {0} Fans, {1} Managers.",
                new Object[]{dbFans.size(), dbManagers.size()});
    }

    private void enrichAndSaveFan(Fan f, UserDao mysqlUserDao, DaoFactoryFacade csvDao) {
        try {
            String[] userData = mysqlUserDao.retrieveUser(f.getUsername());

            if (userData != null && userData.length > UserDaoCsv.COL_PASSWORD_HASH) {
                String passwordHash = userData[UserDaoCsv.COL_PASSWORD_HASH];

                Fan completeFan = new Fan.Builder()
                        .username(f.getUsername())
                        .password(passwordHash)
                        .fullName(f.getFullName())
                        .gender(f.getGender())
                        .birthday(f.getBirthday())
                        .favTeam(f.getFavTeam())
                        .build();
                csvDao.getFanDao().saveFan(completeFan);
            }
        } catch (DAOException e) {
            LOGGER.log(Level.WARNING, "Failed to sync Fan: {0}", f.getUsername());
        }
    }

    private void enrichAndSaveManager(VenueManager vm, UserDao mysqlUserDao, DaoFactoryFacade csvDao) {
        try {
            String[] userData = mysqlUserDao.retrieveUser(vm.getUsername());

            if (userData != null && userData.length > UserDaoCsv.COL_PASSWORD_HASH) {
                String passwordHash = userData[UserDaoCsv.COL_PASSWORD_HASH];

                VenueManager completeVm = new VenueManager.Builder()
                        .username(vm.getUsername())
                        .password(passwordHash)
                        .fullName(vm.getFullName())
                        .gender(vm.getGender())
                        .companyName(vm.getCompanyName())
                        .phoneNumber(vm.getPhoneNumber())
                        .managedVenues(vm.getManagedVenues())
                        .build();
                csvDao.getVenueManagerDao().saveVenueManager(completeVm);
            }
        } catch (DAOException e) {
            LOGGER.log(Level.WARNING, "Failed to sync Manager: {0}", vm.getUsername());
        }
    }

    private void syncVenues(DaoFactoryFacade dao) throws DAOException {
        dao.setPersistenceType(PersistenceType.MYSQL);
        List<Venue> dbVenues = dao.getVenueDao().retrieveAllVenues();

        dao.setPersistenceType(PersistenceType.CSV);
        for (Venue v : dbVenues) {
            dao.getVenueDao().saveVenue(v);
        }
        LOGGER.log(Level.INFO, "Synced {0} Venues.", dbVenues.size());
    }

    private void syncBookings(DaoFactoryFacade dao) throws DAOException {
        dao.setPersistenceType(PersistenceType.MYSQL);
        List<Booking> allBookings = dao.getBookingDao().retrieveAllBookings();

        dao.setPersistenceType(PersistenceType.CSV);
        BookingDao csvBookingDao = dao.getBookingDao();

        for (Booking b : allBookings) {
            csvBookingDao.saveBooking(b);
        }
        LOGGER.log(Level.INFO, "Synced {0} Bookings.", allBookings.size());
    }

    private void syncNotifications(DaoFactoryFacade dao) throws DAOException {
        dao.setPersistenceType(PersistenceType.MYSQL);
        List<Notification> allNotifications = dao.getNotificationDao().retrieveAllNotifications();

        dao.setPersistenceType(PersistenceType.CSV);
        NotificationDao csvNotifDao = dao.getNotificationDao();

        int count = 0;
        for (Notification n : allNotifications) {
            try {
                csvNotifDao.saveNotification(n);
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Skipped notification ID {0}", n.getId());
            }
        }
        LOGGER.log(Level.INFO, "Synced {0} Notifications.", count);
    }
}