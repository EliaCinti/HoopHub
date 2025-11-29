package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.TeamNBA;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.model.UserType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of FanDao.
 * <p>
 * Manages Fan data in CSV file (fav_team, birthday) while delegating
 * common user operations to {@link UserDao}.
 * </p>
 * <p>
 * <strong>CSV Structure:</strong> username,fav_team,birthday
 * </p>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Factory</strong>: Created via FanDaoFactory</li>
 *   <li><strong>Facade</strong>: Uses DaoFactoryFacade to access BookingDao</li>
 *   <li><strong>Observer</strong>: Notifies observers for CSV-MySQL sync</li>
 *   <li><strong>Builder</strong>: Uses Fan.Builder for object construction</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Circular Dependency:</strong> Uses {@link DaoLoadingContext} to prevent infinite loops
 * when Fan loads Bookings and Booking loads Fan back.
 * </p>
 *
 * @see FanDao
 * @see DaoLoadingContext
 */
public class FanDaoCsv extends AbstractCsvDao implements FanDao {

    // ========== CSV CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "fans.csv";
    private static final String[] CSV_HEADER = {"username", "fav_team", "birthday"};

    // ========== COLUMN INDICES ==========

    private static final int COL_USERNAME = 0;
    private static final int COL_FAV_TEAM = 1;
    private static final int COL_BIRTHDAY = 2;

    // ========== DEPENDENCIES ==========

    private final UserDao userDao;

    // ========== CONSTRUCTORS ==========

    /**
     * Constructs FanDaoCsv with UserDao dependency.
     * Injected by FanDaoFactory (Factory pattern).
     *
     * @param userDao DAO for common user operations
     */
    public FanDaoCsv(UserDao userDao) {
        super(CSV_FILE_PATH);
        this.userDao = userDao;
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC METHODS (FanDao Interface Implementation) ==========

    /**
     * {@inheritDoc}
     * <p>
     * Saves in two steps: (1) common user data via UserDao, (2) fan-specific data to CSV.
     * Notifies observers for CSV-MySQL synchronization (Observer pattern).
     * </p>
     */
    @Override
    public synchronized void saveFan(FanBean fanBean) throws DAOException {
        validateNotNull(fanBean, "FanBean");
        validateNotNullOrEmpty(fanBean.getUsername(), CsvDaoConstants.USERNAME);
        validateNotNullOrEmpty(String.valueOf(fanBean.getFavTeam()), "Favorite team");
        validateNotNull(fanBean.getBirthday(), "Birthday");

        // Set user type to FAN before saving
        fanBean.setType(UserType.FAN.getType());

        // Step 1: Save common user data (username, password, full name, gender, type)
        userDao.saveUser(fanBean);

        // Step 2: Save fan-specific data
        String[] fanRow = {
                fanBean.getUsername(),
                String.valueOf(fanBean.getFavTeam()),
                fanBean.getBirthday().toString()
        };

        CsvUtilities.writeFile(csvFile, fanRow);

        notifyObservers(DaoOperation.INSERT, "Fan", fanBean.getUsername(), fanBean);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns fully populated Fan with complete booking list.
     * Uses {@link DaoLoadingContext} to prevent infinite loops.
     * </p>
     */
    @Override
    public synchronized Fan retrieveFan(String username) throws DAOException {
        validateNotNullOrEmpty(username, CsvDaoConstants.USERNAME);

        // Step 1: Retrieve common user data
        String[] userData = userDao.retrieveUser(username);
        if (userData == null) {
            return null;
        }

        // Step 2: Retrieve fan-specific data
        String[] fanData = findRowByValue(COL_USERNAME, username);
        if (fanData == null) {
            return null;
        }

        // Step 3: Construct Fan object using Builder
        return mapRowToFan(userData, fanData);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Fan> retrieveAllFans() throws DAOException {
        List<String[]> fanData = readAllDataRows(); // Uses AbstractCsvDao helper
        List<Fan> fans = new ArrayList<>();

        for (String[] fanRow : fanData) {
            String username = fanRow[COL_USERNAME];

            String[] userData = userDao.retrieveUser(username);
            if (userData != null) {
                Fan fan = mapRowToFan(userData, fanRow);
                fans.add(fan);
            } else {
                logger.log(Level.WARNING, "User data not found for fan: {0}", username);
            }
        }

        return fans;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updates in two steps: (1) common user data via UserDao, (2) fan data in CSV.
     * Notifies observers (Observer pattern).
     * </p>
     */
    @Override
    public synchronized void updateFan(Fan fan, UserBean userBean) throws DAOException {
        validateNotNull(fan, "Fan");
        validateNotNull(userBean, "UserBean");

        // Step 1: Update common user data
        userDao.updateUser(fan, userBean);

        // Step 2: Update fan-specific data
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(fan.getUsername())) {
                row[COL_FAV_TEAM] = String.valueOf(fan.getFavTeam());
                row[COL_BIRTHDAY] = fan.getBirthday().toString();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "Fan", "update", fan.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Deletes fan-specific data from CSV, then common user data via UserDao.
     * Notifies observers (Observer pattern).
     * </p>
     */
    @Override
    public synchronized void deleteFan(Fan fan) throws DAOException {
        validateNotNull(fan, "Fan");

        // Step 1: Delete fan-specific data using the generic method
        boolean found = deleteByColumn(COL_USERNAME, fan.getUsername());

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "Fan", "deletion", fan.getUsername()));
        }

        // Step 2: Delete common user data
        userDao.deleteUser(fan);

        logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps CSV row to Fan object.
     * <p>
     * Uses {@link DaoLoadingContext} to prevent circular loops:
     * If already loading this fan → return with empty bookings list (break cycle).
     * Otherwise, → load complete bookings via BookingDao (Facade pattern).
     * </p>
     *
     * @param userData Common user data [username, password_hash, full_name, gender, type]
     * @param fanData Fan data [username, fav_team, birthday]
     * @return Fully populated Fan with complete bookings list
     */
    private Fan mapRowToFan(String[] userData, String[] fanData) throws DAOException {
        try {
            String username = userData[0];
            LocalDate birthday = LocalDate.parse(fanData[COL_BIRTHDAY]);
            String fanKey = "Fan:" + username;

            // Check if we're in a circular loading situation
            if (DaoLoadingContext.isLoading(fanKey)) {
                // Break the cycle by returning a minimal fan without loading bookings
                return new Fan.Builder()
                        .username(username)
                        .fullName(userData[2])
                        .gender(userData[3])
                        .favTeam(TeamNBA.fromDisplayName(fanData[COL_FAV_TEAM]))
                        .birthday(birthday)
                        .bookingList(Collections.emptyList())  // Empty to break cycle
                        .build();
            }

            // Mark this fan as being loaded
            DaoLoadingContext.startLoading(fanKey);
            try {
                // Load the COMPLETE list of bookings (not empty)
                DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
                BookingDao bookingDao = daoFactory.getBookingDao();
                List<Booking> bookings = bookingDao.retrieveBookingsByFan(username);

                // Build Fan with COMPLETE bookings list
                return new Fan.Builder()
                        .username(username)
                        .fullName(userData[2])
                        .gender(userData[3])
                        .favTeam(TeamNBA.fromDisplayName(fanData[COL_FAV_TEAM]))
                        .birthday(birthday)
                        .bookingList(bookings)  // COMPLETE list
                        .build();
            } finally {
                // Always clean up the loading context
                DaoLoadingContext.finishLoading(fanKey);
            }
        } catch (DateTimeParseException e) {
            throw new DAOException("Invalid date format for fan: " + fanData[COL_USERNAME], e);
        } catch (IllegalArgumentException e) {
            throw new DAOException("Error constructing Fan object: " + e.getMessage(), e);
        }
    }
}
