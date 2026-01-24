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
 * CSV implementation of {@link VenueManagerDao}.
 *
 * <p>Persists VenueManager-specific data to {@code venue_managers.csv} and delegates
 * common user data to {@link UserDao}. Uses {@link DaoLoadingContext} to prevent
 * circular dependencies when loading managed venues.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerDaoCsv extends AbstractCsvDao implements VenueManagerDao {

    private static final String VENUE_MANAGER = "VenueManager";
    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venue_managers.csv";
    private static final String[] CSV_HEADER = {"username", "company_name", "phone_number"};

    private static final int COL_USERNAME = 0;
    private static final int COL_COMPANY_NAME = 1;
    private static final int COL_PHONE_NUMBER = 2;

    private final UserDao userDao;

    public VenueManagerDaoCsv(UserDao userDao) {
        super(CSV_FILE_PATH);
        this.userDao = userDao;
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized void saveVenueManager(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, VENUE_MANAGER);
        validateNotNullOrEmpty(venueManager.getUsername(), "Username");
        validateNotNullOrEmpty(venueManager.getCompanyName(), "Company name");
        validateNotNullOrEmpty(venueManager.getPhoneNumber(), "Phone number");

        userDao.saveUser(venueManager);

        String[] managerRow = {
                venueManager.getUsername(),
                venueManager.getCompanyName(),
                venueManager.getPhoneNumber()
        };

        CsvUtilities.writeFile(csvFile, managerRow);
        putInCache(venueManager, venueManager.getUsername());
        notifyObservers(DaoOperation.INSERT, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    @Override
    public synchronized VenueManager retrieveVenueManager(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");

        VenueManager cached = getFromCache(VenueManager.class, username);
        if (cached != null) return cached;

        String[] userData = userDao.retrieveUser(username);
        if (!isValidRow(userData)) return null;

        String[] managerData = findRowByValue(COL_USERNAME, username);
        if (!isValidRow(managerData)) return null;

        VenueManager vm = mapRowToVenueManager(userData, managerData);
        putInCache(vm, username);

        return vm;
    }

    @Override
    public synchronized List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<String[]> managerData = readAllDataRows();
        List<VenueManager> venueManagers = new ArrayList<>();

        for (String[] managerRow : managerData) {
            String username = managerRow[COL_USERNAME];

            VenueManager cached = getFromCache(VenueManager.class, username);
            if (cached != null) {
                venueManagers.add(cached);
                continue;
            }

            String[] userData = userDao.retrieveUser(username);

            if (isValidRow(userData)) {
                VenueManager manager = mapRowToVenueManager(userData, managerRow);
                putInCache(manager, username);
                venueManagers.add(manager);
            } else {
                logger.log(Level.WARNING, "Orphaned VenueManager record found: {0}", username);
            }
        }

        return venueManagers;
    }

    @Override
    public synchronized void updateVenueManager(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, VENUE_MANAGER);
        validateNotNullOrEmpty(venueManager.getCompanyName(), "Company Name");
        validateNotNullOrEmpty(venueManager.getPhoneNumber(), "Phone Number");

        userDao.updateUser(venueManager);

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
                    VENUE_MANAGER, "update", venueManager.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        logger.log(Level.INFO, "VenueManager updated successfully: {0}", venueManager.getUsername());
        putInCache(venueManager, venueManager.getUsername());
        notifyObservers(DaoOperation.UPDATE, VENUE_MANAGER, venueManager.getUsername(), venueManager);
    }

    @Override
    public synchronized void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, VENUE_MANAGER);

        boolean found = deleteByColumn(COL_USERNAME, venueManager.getUsername());

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    VENUE_MANAGER, "deletion", venueManager.getUsername()));
        }

        userDao.deleteUser(venueManager);
        logger.log(Level.INFO, "VenueManager deleted successfully: {0}", venueManager.getUsername());
        removeFromCache(VenueManager.class, venueManager.getUsername());
        notifyObservers(DaoOperation.DELETE, VENUE_MANAGER, venueManager.getUsername(), null);
    }

    @Override
    public synchronized List<Venue> getVenues(VenueManager venueManager) throws DAOException {
        validateNotNull(venueManager, VENUE_MANAGER);
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueDao venueDao = daoFactory.getVenueDao();
        return venueDao.retrieveVenuesByManager(venueManager.getUsername());
    }

    // ========== PRIVATE HELPER ==========

    private VenueManager mapRowToVenueManager(String[] userData, String[] managerData) throws DAOException {
        try {
            String username = userData[0];
            String managerKey = "VenueManager:" + username;

            if (DaoLoadingContext.isLoading(managerKey)) {
                return new VenueManager.Builder()
                        .username(username)
                        .fullName(userData[UserDaoCsv.COL_FULL_NAME])
                        .gender(userData[UserDaoCsv.COL_GENDER])
                        .companyName(managerData[COL_COMPANY_NAME])
                        .phoneNumber(managerData[COL_PHONE_NUMBER])
                        .managedVenues(Collections.emptyList())
                        .build();
            }

            DaoLoadingContext.startLoading(managerKey);
            try {
                DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
                VenueDao venueDao = daoFactory.getVenueDao();
                List<Venue> managedVenues = venueDao.retrieveVenuesByManager(username);

                return new VenueManager.Builder()
                        .username(username)
                        .fullName(userData[UserDaoCsv.COL_FULL_NAME])
                        .gender(userData[UserDaoCsv.COL_GENDER])
                        .companyName(managerData[COL_COMPANY_NAME])
                        .phoneNumber(managerData[COL_PHONE_NUMBER])
                        .managedVenues(managedVenues)
                        .build();
            } finally {
                DaoLoadingContext.finishLoading(managerKey);
            }
        } catch (IllegalArgumentException e) {
            throw new DAOException("Error constructing VenueManager object: " + e.getMessage(), e);
        }
    }
}