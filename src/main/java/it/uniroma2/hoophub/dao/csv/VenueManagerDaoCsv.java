package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.model.UserType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of the VenueManagerDao interface.
 * <p>
 * This class provides data access operations for VenueManager entities stored in CSV files.
 * It extends {@link AbstractCsvDao} to leverage common functionality and works in
 * conjunction with {@link UserDao} to handle both common user data and venue manager-specific data.
 * </p>
 * <p>
 * <strong>CSV File Structure (venue_managers.csv):</strong>
 * <pre>
 * username,company_name,phone_number
 * manager1,Hoops Entertainment LLC,555-0101
 * manager2,Sports Venues Inc,555-0202
 * </pre>
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> This DAO demonstrates proper separation of concerns by:
 * <ul>
 *   <li>Using {@link UserDao} for common user operations (username, password, etc.)</li>
 *   <li>Managing only venue manager-specific data in its own CSV file</li>
 *   <li>Using {@link DaoLoadingContext} to prevent circular dependencies while loading complete objects</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Circular Dependency Prevention:</strong> This implementation uses {@link DaoLoadingContext}
 * to prevent infinite loops when loading related entities. When a VenueManager is being loaded and needs
 * to load its managed venues, the context prevents re-loading the same VenueManager during venue construction,
 * breaking the circular dependency while still providing complete objects.
 * </p>
 * <p>
 * <strong>Object Completeness:</strong> Unlike previous stub-based approaches, this DAO always
 * returns fully populated VenueManager objects with their complete managed venues list, ensuring
 * consistency with the MySQL implementation and respecting the Liskov Substitution Principle.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> All public methods are synchronized to prevent concurrent
 * modification issues when multiple threads access the CSV file.
 * </p>
 *
 * @see VenueManagerDao Interface defining the contract
 * @see AbstractCsvDao Base class providing common CSV functionality
 * @see UserDao DAO for common user operations
 * @see VenueManager Domain model representing a venue manager
 * @see DaoLoadingContext Utility for preventing circular loading
 */
public class VenueManagerDaoCsv extends AbstractCsvDao implements VenueManagerDao {

    // ========== CSV CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venue_managers.csv";
    private static final String[] CSV_HEADER = {"username", "company_name", "phone_number"};

    // ========== COLUMN INDICES ==========

    private static final int COL_USERNAME = 0;
    private static final int COL_COMPANY_NAME = 1;
    private static final int COL_PHONE_NUMBER = 2;

    // ========== DEPENDENCIES ==========

    private final UserDao userDao;

    // ========== CONSTRUCTORS ==========

    /**
     * Constructor with dependency injection for UserDao.
     * <p>
     * <strong>Dependency Injection:</strong> The UserDao is injected via constructor
     * by the VenueManagerDaoFactory, ensuring proper use of the Factory pattern and avoiding
     * direct instantiation with "new" inside DAOs.
     * </p>
     *
     * @param userDao The UserDao implementation to use for common user operations
     */
    public VenueManagerDaoCsv(UserDao userDao) {
        super(CSV_FILE_PATH);
        this.userDao = userDao;
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC METHODS (VenueManagerDao Interface Implementation) ==========

    /**
     * {@inheritDoc}
     * <p>
     * This method performs a two-step save operation:
     * <ol>
     *   <li>Saves common user data via {@link UserDao#saveUser(UserBean)}</li>
     *   <li>Saves venue manager-specific data (company name, phone number) to venue_managers.csv</li>
     * </ol>
     * </p>
     * <p>
     * After successful save, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void saveVenueManager(VenueManagerBean venueManagerBean) throws DAOException {
        validateNotNull(venueManagerBean, "VenueManagerBean");
        validateNotNullOrEmpty(venueManagerBean.getUsername(), "Username");
        validateNotNullOrEmpty(venueManagerBean.getCompanyName(), "Company name");
        validateNotNullOrEmpty(venueManagerBean.getPhoneNumber(), "Phone number");

        // Set user type to VENUE_MANAGER before saving
        venueManagerBean.setType(UserType.VENUE_MANAGER.getType());

        // Step 1: Save common user data (username, password, full name, gender, type)
        userDao.saveUser(venueManagerBean);

        // Step 2: Save venue manager-specific data
        String[] managerRow = {
                venueManagerBean.getUsername(),
                venueManagerBean.getCompanyName(),
                venueManagerBean.getPhoneNumber()
        };

        CsvUtilities.writeFile(csvFile, managerRow);

        notifyObservers(DaoOperation.INSERT, "VenueManager", venueManagerBean.getUsername(), venueManagerBean);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method performs a three-step retrieval:
     * <ol>
     *   <li>Retrieves common user data via {@link UserDao#retrieveUser(String)}</li>
     *   <li>Retrieves venue manager-specific data from venue_managers.csv</li>
     *   <li>Loads all managed venues with full data using {@link DaoLoadingContext} to prevent loops</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Object Completeness:</strong> The returned VenueManager object contains a fully
     * populated managedVenues list with all real data, ensuring consistency with MySQL implementation.
     * </p>
     */
    @Override
    public synchronized VenueManager retrieveVenueManager(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");

        // Step 1: Retrieve common user data
        String[] userData = userDao.retrieveUser(username);
        if (userData == null) {
            return null;
        }

        // Step 2: Retrieve venue manager-specific data
        String[] managerData = findRowByValue(COL_USERNAME, username);
        if (managerData == null) {
            return null;
        }

        // Step 3: Construct VenueManager object using Builder
        return mapRowToVenueManager(userData, managerData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each returned VenueManager object contains a fully populated managedVenues list with all real data.
     * </p>
     */
    @Override
    public synchronized List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<String[]> managerData = CsvUtilities.readAll(csvFile);
        List<VenueManager> venueManagers = new ArrayList<>();

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < managerData.size(); i++) {
            String[] managerRow = managerData.get(i);
            String username = managerRow[COL_USERNAME];

            String[] userData = userDao.retrieveUser(username);
            if (userData != null) {
                VenueManager manager = mapRowToVenueManager(userData, managerRow);
                venueManagers.add(manager);
            } else {
                logger.log(Level.WARNING, "User data not found for venue manager: {0}", username);
            }
        }

        return venueManagers;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method performs a two-step update:
     * <ol>
     *   <li>Updates common user data via {@link UserDao#updateUser(it.uniroma2.hoophub.model.User, UserBean)}</li>
     *   <li>Updates venue manager-specific data in venue_managers.csv</li>
     * </ol>
     * </p>
     * <p>
     * After successful update, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void updateVenueManager(VenueManager venueManager, UserBean userBean) throws DAOException {
        validateNotNull(venueManager, "VenueManager");
        validateNotNull(userBean, "UserBean");

        // Step 1: Update common user data
        userDao.updateUser(venueManager, userBean);

        // Step 2: Update venue manager-specific data
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(venueManager.getUsername())) {
                row[COL_COMPANY_NAME] = venueManager.getCompanyName();
                row[COL_PHONE_NUMBER] = venueManager.getPhoneNumber();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "VenueManager", "update", venueManager.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "VenueManager updated successfully: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.UPDATE, "VenueManager", venueManager.getUsername(), venueManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method performs a two-step deletion:
     * <ol>
     *   <li>Deletes venue manager-specific data from venue_managers.csv</li>
     *   <li>Deletes common user data via {@link UserDao#deleteUser(it.uniroma2.hoophub.model.User)}</li>
     * </ol>
     * </p>
     * <p>
     * After successful deletion, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, "VenueManager");

        // Step 1: Delete venue manager-specific data first
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(venueManager.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    "VenueManager", "deletion", venueManager.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        // Step 2: Delete common user data
        userDao.deleteUser(venueManager);

        logger.log(Level.INFO, "VenueManager deleted successfully: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.DELETE, "VenueManager", venueManager.getUsername(), null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method retrieves all venues managed by the specified venue manager by delegating
     * to {@link VenueDao#retrieveVenuesByManager(String)}.
     * </p>
     * <p>
     * This is a convenience method that allows loading the manager's venues lazily
     * after the VenueManager object has been constructed.
     * </p>
     */
    @Override
    public synchronized List<Venue> getVenues(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, "VenueManager");

        // Use DaoFactoryFacade to get VenueDao (Factory pattern)
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueDao venueDao = daoFactory.getVenueDao();
        return venueDao.retrieveVenuesByManager(venueManager.getUsername());
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps CSV row data to a VenueManager domain object.
     * <p>
     * <strong>Circular Dependency Prevention:</strong> This method uses {@link DaoLoadingContext}
     * to detect and prevent infinite loops when loading related entities. If this VenueManager is
     * already being loaded in the current call stack (circular reference), it creates a minimal
     * VenueManager without reloading venues, breaking the cycle.
     * </p>
     * <p>
     * <strong>Object Completeness:</strong> When not in a circular loading situation, this method
     * loads all managed venues with complete data by delegating to VenueDao, ensuring that the
     * returned VenueManager object is fully populated.
     * </p>
     *
     * @param userData Array containing common user data [username, password_hash, full_name, gender, type]
     * @param managerData Array containing venue manager-specific data [username, company_name, phone_number]
     * @return A fully constructed VenueManager object with complete managedVenues list
     * @throws DAOException If there's an error constructing the VenueManager
     */
    private VenueManager mapRowToVenueManager(String[] userData, String[] managerData) throws DAOException {
        try {
            String username = userData[0];
            String managerKey = "VenueManager:" + username;

            // Check if we're in a circular loading situation
            if (DaoLoadingContext.isLoading(managerKey)) {
                // Break the cycle by returning a minimal manager without loading venues
                return new VenueManager.Builder()
                        .username(username)
                        .fullName(userData[2])
                        .gender(userData[3])
                        .companyName(managerData[COL_COMPANY_NAME])
                        .phoneNumber(managerData[COL_PHONE_NUMBER])
                        .managedVenues(Collections.emptyList())  // Empty to break cycle
                        .build();
            }

            // Mark this manager as being loaded
            DaoLoadingContext.startLoading(managerKey);
            try {
                // Load COMPLETE list of managed venues (not empty)
                DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
                VenueDao venueDao = daoFactory.getVenueDao();
                List<Venue> managedVenues = venueDao.retrieveVenuesByManager(username);

                // Build VenueManager with COMPLETE venues list
                return new VenueManager.Builder()
                        .username(username)
                        .fullName(userData[2])
                        .gender(userData[3])
                        .companyName(managerData[COL_COMPANY_NAME])
                        .phoneNumber(managerData[COL_PHONE_NUMBER])
                        .managedVenues(managedVenues)  // COMPLETE list
                        .build();
            } finally {
                // Always clean up the loading context
                DaoLoadingContext.finishLoading(managerKey);
            }
        } catch (IllegalArgumentException e) {
            throw new DAOException("Error constructing VenueManager object: " + e.getMessage(), e);
        }
    }
}
