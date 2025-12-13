package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

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

    private static final String VENUE_MANAGER = "VenueManager";
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
     * Constructs VenueManagerDaoCsv with UserDao dependency.
     * Injected by VenueManagerDaoFactory (Factory pattern).
     *
     * @param userDao DAO for common user operations
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
     * Saves in two steps: (1) common user data via UserDao, (2) manager-specific data to CSV.
     * Notifies observers for CSV-MySQL synchronization (Observer pattern).
     * </p>
     */
    @Override
    public synchronized void saveVenueManager(VenueManager venueManager) throws DAOException {
        // Validiamo l'oggetto Model (non più la Bean)
        validateNotNull(venueManager, VENUE_MANAGER);
        validateNotNullOrEmpty(venueManager.getUsername(), "Username");
        validateNotNullOrEmpty(venueManager.getCompanyName(), "Company name");
        validateNotNullOrEmpty(venueManager.getPhoneNumber(), "Phone number");

        // Nota: Non serve più settare il tipo manualmente (venueManagerBean.setType...)
        // L'oggetto VenueManager sa già di essere un VENUE_MANAGER tramite il metodo getUserType()

        // Step 1: Salviamo i dati comuni (User)
        // UserDao.saveUser ora accetta un oggetto User (e VenueManager estende User)
        userDao.saveUser(venueManager);

        // Step 2: Salviamo i dati specifici su CSV
        String[] managerRow = {
                venueManager.getUsername(),
                venueManager.getCompanyName(),
                venueManager.getPhoneNumber()
        };

        CsvUtilities.writeFile(csvFile, managerRow);

        // Notifichiamo l'observer passando il Model
        notifyObservers(DaoOperation.INSERT, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns fully populated VenueManager with complete managed venues list.
     * Uses {@link DaoLoadingContext} to prevent infinite loops.
     * </p>
     */
    @Override
    public synchronized VenueManager retrieveVenueManager(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");

        // Step 1: Retrieve common user data
        String[] userData = userDao.retrieveUser(username);

        // FIX: Controlla anche la lunghezza dell'array (AbstractCsvDao ritorna array vuoto se non trovato)
        if (userData == null || userData.length == 0) {
            return null;
        }

        // Step 2: Retrieve venue manager-specific data
        String[] managerData = findRowByValue(COL_USERNAME, username);
        if (managerData == null || managerData.length == 0) {
            return null;
        }

        // Step 3: Construct VenueManager object using Builder
        return mapRowToVenueManager(userData, managerData);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<String[]> managerData = readAllDataRows(); // Uses AbstractCsvDao helper
        List<VenueManager> venueManagers = new ArrayList<>();

        for (String[] managerRow : managerData) {
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
     * Updates in two steps: (1) common user data via UserDao, (2) manager data in CSV.
     * Notifies observers (Observer pattern).
     * </p>
     */
    @Override
    public synchronized void updateVenueManager(VenueManager venueManager) throws DAOException {
        // 1. Validazione sul Model (Rimosso riferimento a userBean)
        validateNotNull(venueManager, VENUE_MANAGER);

        // Validiamo i campi obbligatori del Model se necessario (opzionale, ma buona pratica)
        validateNotNullOrEmpty(venueManager.getCompanyName(), "Company Name");
        validateNotNullOrEmpty(venueManager.getPhoneNumber(), "Phone Number");

        // Step 1: Update common user data
        // Chiamiamo UserDao passando il Model. Lui aggiornerà nome, cognome, genere.
        userDao.updateUser(venueManager);

        // Step 2: Update venue manager-specific data (CSV)
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            // Cerchiamo per username
            if (row[COL_USERNAME].equals(venueManager.getUsername())) {
                // Leggiamo i nuovi dati direttamente dal MODEL
                row[COL_COMPANY_NAME] = venueManager.getCompanyName();
                row[COL_PHONE_NUMBER] = venueManager.getPhoneNumber();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    VENUE_MANAGER, "update", venueManager.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "VenueManager updated successfully: {0}", venueManager.getUsername());

        // Notifica Observer passando il Model
        notifyObservers(DaoOperation.UPDATE, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Refactoring:</strong> Uses the generic {@link #deleteByColumn(int, String)} method
     * from AbstractCsvDao to handle row deletion, improving code reuse and readability.
     * </p>
     */
    @Override
    public synchronized void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, VENUE_MANAGER);

        // Step 1: Delete venue manager-specific data using generic method
        // We delete by username column
        boolean found = deleteByColumn(COL_USERNAME, venueManager.getUsername());

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    VENUE_MANAGER, "deletion", venueManager.getUsername()));
        }

        // Step 2: Delete common user data
        userDao.deleteUser(venueManager);

        logger.log(Level.INFO, "VenueManager deleted successfully: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.DELETE, VENUE_MANAGER, venueManager.getUsername(), null);
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
        validateNotNull(venueManager, VENUE_MANAGER);

        // Use DaoFactoryFacade to get VenueDao (Factory pattern)
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueDao venueDao = daoFactory.getVenueDao();
        return venueDao.retrieveVenuesByManager(venueManager.getUsername());
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps CSV row to VenueManager object.
     * <p>
     * Uses {@link DaoLoadingContext} to prevent circular loops:
     * If already loading this manager → return with empty venues list (break cycle).
     * Otherwise, → load complete managed venues via VenueDao (Facade pattern).
     * </p>
     *
     * @param userData Common user data [username, password_hash, full_name, gender, type]
     * @param managerData Manager data [username, company_name, phone_number]
     * @return Fully populated VenueManager with complete venues list
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
                // Load the COMPLETE list of managed venues (not empty)
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
