package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV implementation of the FanDao interface.
 * <p>
 * This class manages fan data across two CSV files:
 * <ul>
 *   <li>users.csv - common user data (via UserDao)</li>
 *   <li>fans.csv - fan-specific data</li>
 * </ul>
 * </p>
 *
 * @see FanDao
 * @see AbstractObservableDao
 */
public class FanDaoCsv extends AbstractObservableDao implements FanDao {

    private static final Logger logger = Logger.getLogger(FanDaoCsv.class.getName());

    private static final String CSV_FILE_PATH = "data/fans.csv";
    private static final String[] CSV_HEADER = {"username", "fav_team", "birthday"};

    private static final int COL_USERNAME = 0;
    private static final int COL_FAV_TEAM = 1;
    private static final int COL_BIRTHDAY = 2;

    private final File csvFile;
    private final UserDao userDao;

    public FanDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        this.userDao = new UserDaoCsv();
        initializeCsvFile();
    }

    @Override
    public synchronized void saveFan(FanBean fanBean) throws DAOException {
        if (fanBean == null) {
            throw new IllegalArgumentException("FanBean cannot be null");
        }

        // Save user data first
        userDao.saveUser(fanBean);

        // Then save fan-specific data
        String[] newRow = {
                fanBean.getUsername(),
                fanBean.getFavTeam(),
                fanBean.getBirthday().toString()
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "Fan saved successfully: {0}", fanBean.getUsername());
        notifyObservers(DaoOperation.INSERT, "Fan", fanBean.getUsername(), fanBean);
    }

    @Override
    public synchronized Fan retrieveFan(String username) throws DAOException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // Get user data
        String[] userData = userDao.retrieveUser(username);
        if (userData == null) {
            return null;
        }

        // Get fan-specific data
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(username)) {
                return new Fan.Builder()
                        .username(userData[0])
                        .fullName(userData[2])
                        .gender(userData[3])
                        .favTeam(row[COL_FAV_TEAM])
                        .birthday(LocalDate.parse(row[COL_BIRTHDAY]))
                        .bookingList(new ArrayList<>())
                        .build();
            }
        }

        return null;
    }

    @Override
    public synchronized List<Fan> retrieveAllFans() throws DAOException {
        List<Fan> fans = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);
            Fan fan = retrieveFan(row[COL_USERNAME]);
            if (fan != null) {
                fans.add(fan);
            }
        }

        logger.log(Level.INFO, "Retrieved {0} fans", fans.size());
        return fans;
    }

    @Override
    public synchronized void updateFan(Fan fan, UserBean userBean) throws DAOException {
        if (fan == null || userBean == null) {
            throw new IllegalArgumentException("Fan and UserBean cannot be null");
        }

        // Update user data first
        userDao.updateUser(fan, userBean);

        // Then update fan-specific data
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_USERNAME].equals(fan.getUsername())) {
                row[COL_FAV_TEAM] = fan.getFavTeam();
                row[COL_BIRTHDAY] = fan.getBirthday().toString();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Fan not found for update: " + fan.getUsername());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
    }

    @Override
    public synchronized void deleteFan(Fan fan) throws DAOException {
        if (fan == null) {
            throw new IllegalArgumentException("Fan cannot be null");
        }

        // Delete fan-specific data first
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            if (data.get(i)[COL_USERNAME].equals(fan.getUsername())) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Fan not found for deletion: " + fan.getUsername());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        // Then delete user data
        userDao.deleteUser(fan);

        logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
        notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
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
