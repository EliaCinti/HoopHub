package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.UserType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of the FanDao interface.
 * <p>
 * This class provides data access operations for Fan entities stored in CSV files.
 * It extends {@link AbstractCsvDao} to leverage common functionality and works in
 * conjunction with {@link UserDao} to handle both common user data and fan-specific data.
 * </p>
 * <p>
 * <strong>CSV File Structure (fans.csv):</strong>
 * <pre>
 * username,fav_team,birthday
 * john_doe,Lakers,1990-05-15
 * jane_fan,Warriors,1985-08-22
 * </pre>
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> This DAO demonstrates proper separation of concerns by:
 * <ul>
 *   <li>Using {@link UserDao} for common user operations (username, password, etc.)</li>
 *   <li>Managing only fan-specific data in its own CSV file</li>
 *   <li>Avoiding circular dependencies by creating Fan objects with EMPTY booking lists</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Circular Dependency Prevention:</strong> The {@link #retrieveFan(String)} method
 * creates Fan objects with an empty booking list. Bookings should be loaded separately
 * via BookingDao when needed, preventing circular DAO calls during object construction.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> All public methods are synchronized to prevent concurrent
 * modification issues when multiple threads access the CSV file.
 * </p>
 *
 * @see FanDao Interface defining the contract
 * @see AbstractCsvDao Base class providing common CSV functionality
 * @see UserDao DAO for common user operations
 * @see Fan Domain model representing a fan
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
     * Default constructor that creates a UserDaoCsv instance.
     * <p>
     * This constructor is convenient for standalone usage without dependency injection.
     * </p>
     */
    public FanDaoCsv() {
        this(new UserDaoCsv());
    }

    /**
     * Constructor with dependency injection for UserDao.
     * <p>
     * This constructor allows injecting a different UserDao implementation
     * (e.g., for testing or using MySQL instead of CSV).
     * </p>
     *
     * @param userDao The UserDao implementation to use for common user operations
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
     * This method performs a two-step save operation:
     * <ol>
     *   <li>Saves common user data via {@link UserDao#saveUser(UserBean)}</li>
     *   <li>Saves fan-specific data (favorite team, birthday) to fans.csv</li>
     * </ol>
     * </p>
     * <p>
     * After successful save, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void saveFan(FanBean fanBean) throws DAOException {
        validateNotNull(fanBean, "FanBean");
        validateNotNullOrEmpty(fanBean.getUsername(), "Username");
        validateNotNullOrEmpty(fanBean.getFavTeam(), "Favorite team");
        validateNotNull(fanBean.getBirthday(), "Birthday");

        // Set user type to FAN before saving
        fanBean.setType(UserType.FAN.getType());

        // Step 1: Save common user data (username, password, full name, gender, type)
        userDao.saveUser(fanBean);

        // Step 2: Save fan-specific data
        String[] fanRow = {
                fanBean.getUsername(),
                fanBean.getFavTeam(),
                fanBean.getBirthday().toString()
        };

        CsvUtilities.writeFile(csvFile, fanRow);

        logger.log(Level.INFO, "Fan saved successfully: {0}", fanBean.getUsername());
        notifyObservers(DaoOperation.INSERT, "Fan", fanBean.getUsername(), fanBean);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method performs a two-step retrieval:
     * <ol>
     *   <li>Retrieves common user data via {@link UserDao#retrieveUser(String)}</li>
     *   <li>Retrieves fan-specific data from fans.csv</li>
     *   <li>Constructs a Fan object with EMPTY booking list (no circular dependency)</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Circular Dependency Prevention:</strong> The returned Fan object has an
     * empty booking list. If bookings are needed, they should be loaded separately via
     * BookingDao after the Fan is constructed.
     * </p>
     */
    @Override
    public synchronized Fan retrieveFan(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");

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

    /**
     * {@inheritDoc}
     * <p>
     * Each returned Fan object has an empty booking list. Bookings should be loaded
     * separately if needed.
     * </p>
     */
    @Override
    public synchronized List<Fan> retrieveAllFans() throws DAOException {
        List<String[]> fanData = CsvUtilities.readAll(csvFile);
        List<Fan> fans = new ArrayList<>();

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < fanData.size(); i++) {
            String[] fanRow = fanData.get(i);
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
     * This method performs a two-step update:
     * <ol>
     *   <li>Updates common user data via {@link UserDao#updateUser(it.uniroma2.hoophub.model.User, UserBean)}</li>
     *   <li>Updates fan-specific data in fans.csv</li>
     * </ol>
     * </p>
     * <p>
     * After successful update, observers are notified for cross-persistence synchronization.
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
                row[COL_FAV_TEAM] = fan.getFavTeam();
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
     * This method performs a two-step deletion:
     * <ol>
     *   <li>Deletes fan-specific data from fans.csv</li>
     *   <li>Deletes common user data via {@link UserDao#deleteUser(it.uniroma2.hoophub.model.User)}</li>
     * </ol>
     * </p>
     * <p>
     * After successful deletion, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void deleteFan(Fan fan) throws DAOException {
        validateNotNull(fan, "Fan");

        // Step 1: Delete fan-specific data first
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(fan.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "Fan", "deletion", fan.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        // Step 2: Delete common user data
        userDao.deleteUser(fan);

        logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps CSV row data to a Fan domain object.
     * <p>
     * This method constructs a Fan using the Builder pattern. The Fan is created with
     * an EMPTY booking list to avoid circular dependencies. Bookings should be loaded
     * separately via BookingDao if needed.
     * </p>
     *
     * @param userData Array containing common user data [username, password_hash, full_name, gender, type]
     * @param fanData Array containing fan-specific data [username, fav_team, birthday]
     * @return A fully constructed Fan object with empty booking list
     * @throws DAOException If there's an error parsing the date or constructing the Fan
     */
    private Fan mapRowToFan(String[] userData, String[] fanData) throws DAOException {
        try {
            LocalDate birthday = LocalDate.parse(fanData[COL_BIRTHDAY]);

            return new Fan.Builder()
                    .username(userData[0])
                    .fullName(userData[2])
                    .gender(userData[3])
                    .favTeam(fanData[COL_FAV_TEAM])
                    .birthday(birthday)
                    .bookingList(Collections.emptyList())  // EMPTY list - no circular dependency
                    .build();
        } catch (DateTimeParseException e) {
            throw new DAOException("Invalid date format for fan: " + fanData[COL_USERNAME], e);
        } catch (IllegalArgumentException e) {
            throw new DAOException("Error constructing Fan object: " + e.getMessage(), e);
        }
    }
}
