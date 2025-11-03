package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV implementation of the VenueManagerDao interface.
 */
public class VenueManagerDaoCsv extends AbstractObservableDao implements VenueManagerDao {

    private static final Logger logger = Logger.getLogger(VenueManagerDaoCsv.class.getName());

    private static final String CSV_FILE_PATH = "data/venue_managers.csv";
    private static final String[] CSV_HEADER = {"username", "company_name", "phone_number"};

    private static final int COL_USERNAME = 0;
    private static final int COL_COMPANY_NAME = 1;
    private static final int COL_PHONE_NUMBER = 2;

    private final File csvFile;
    private final UserDao userDao;

    public VenueManagerDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        this.userDao = new UserDaoCsv();
        initializeCsvFile();
    }

    @Override
    public synchronized void saveVenueManager(VenueManagerBean venueManagerBean) throws DAOException {
        if (venueManagerBean == null) {
            throw new IllegalArgumentException("VenueManagerBean cannot be null");
        }

        userDao.saveUser(venueManagerBean);

        String[] newRow = {
                venueManagerBean.getUsername(),
                venueManagerBean.getCompanyName(),
                venueManagerBean.getPhoneNumber()
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "VenueManager saved successfully: {0}", venueManagerBean.getUsername());
        notifyObservers(DaoOperation.INSERT, "VenueManager", venueManagerBean.getUsername(), venueManagerBean);
    }

    @Override
    public synchronized VenueManager retrieveVenueManager(String username) throws DAOException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String[] userData = userDao.retrieveUser(username);
        if (userData == null) {
            return null;
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username)) {
                return new VenueManager.Builder()
                        .username(userData[0])
                        .fullName(userData[2])
                        .gender(userData[3])
                        .companyName(row[COL_COMPANY_NAME])
                        .phoneNumber(row[COL_PHONE_NUMBER])
                        .managedVenues(new ArrayList<>())
                        .build();
            }
        }

        return null;
    }

    @Override
    public synchronized List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<VenueManager> venueManagers = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);
            VenueManager vm = retrieveVenueManager(row[COL_USERNAME]);
            if (vm != null) {
                venueManagers.add(vm);
            }
        }

        logger.log(Level.INFO, "Retrieved {0} venue managers", venueManagers.size());
        return venueManagers;
    }

    @Override
    public synchronized void updateVenueManager(VenueManager venueManager, UserBean userBean) throws DAOException {
        if (venueManager == null || userBean == null) {
            throw new IllegalArgumentException("VenueManager and UserBean cannot be null");
        }

        userDao.updateUser(venueManager, userBean);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(venueManager.getUsername())) {
                row[COL_COMPANY_NAME] = venueManager.getCompanyName();
                row[COL_PHONE_NUMBER] = venueManager.getPhoneNumber();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("VenueManager not found for update: " + venueManager.getUsername());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "VenueManager updated successfully: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.UPDATE, "VenueManager", venueManager.getUsername(), venueManager);
    }

    @Override
    public synchronized void deleteVenueManager(VenueManager venueManager) throws DAOException {
        if (venueManager == null) {
            throw new IllegalArgumentException("VenueManager cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(venueManager.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("VenueManager not found for deletion: " + venueManager.getUsername());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        userDao.deleteUser(venueManager);

        logger.log(Level.INFO, "VenueManager deleted successfully: {0}", venueManager.getUsername());
        notifyObservers(DaoOperation.DELETE, "VenueManager", venueManager.getUsername(), null);
    }

    @Override
    public synchronized List<Venue> getVenues(VenueManager venueManager) throws DAOException {
        if (venueManager == null) {
            throw new IllegalArgumentException("VenueManager cannot be null");
        }

        VenueDaoCsv venueDao = new VenueDaoCsv();
        return venueDao.retrieveVenuesByManager(venueManager);
    }

    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, CSV_HEADER, emptyData);
                logger.info("Initialized CSV file: " + CSV_FILE_PATH);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Failed to initialize CSV file", e);
        }
    }
}
