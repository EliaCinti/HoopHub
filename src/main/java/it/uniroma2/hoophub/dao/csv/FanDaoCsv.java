package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of {@link FanDao}.
 *
 * <p>Persists Fan-specific data to {@code fans.csv} and delegates common user data
 * to {@link UserDao}. Implements a join mechanism between the two files.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanDaoCsv extends AbstractCsvDao implements FanDao {

    private static final String FAN = "Fan";
    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "fans.csv";
    private static final String[] CSV_HEADER = {"username", "favorite_team", "birthday"};

    private static final int COL_USERNAME = 0;
    private static final int COL_FAVORITE_TEAM = 1;
    private static final int COL_BIRTHDAY = 2;

    private final UserDao userDao;

    public FanDaoCsv(UserDao userDao) {
        super(CSV_FILE_PATH);
        this.userDao = userDao;
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized void saveFan(Fan fan) throws DAOException {
        validateNotNull(fan, FAN);
        validateNotNullOrEmpty(fan.getUsername(), "Username");

        userDao.saveUser(fan);

        String teamStr = (fan.getFavTeam() != null) ? fan.getFavTeam().name() : "";
        String birthStr = (fan.getBirthday() != null) ? fan.getBirthday().toString() : "";

        String[] fanRow = { fan.getUsername(), teamStr, birthStr };
        CsvUtilities.writeFile(csvFile, fanRow);

        putInCache(fan, fan.getUsername());
        notifyObservers(DaoOperation.INSERT, FAN, fan.getUsername(), fan);
    }

    @Override
    public synchronized Fan retrieveFan(String username) throws DAOException {
        validateNotNullOrEmpty(username, "Username");

        Fan cachedFan = getFromCache(Fan.class, username);
        if (cachedFan != null) return cachedFan;

        String[] userData = userDao.retrieveUser(username);
        if (!isValidRow(userData)) return null;

        String[] fanData = findRowByValue(COL_USERNAME, username);
        if (!isValidRow(fanData)) return null;

        Fan fan = mapRowToFan(userData, fanData);
        putInCache(fan, username);

        return fan;
    }

    @Override
    public synchronized List<Fan> retrieveAllFans() throws DAOException {
        List<String[]> fanRows = readAllDataRows();
        List<Fan> fans = new ArrayList<>();

        for (String[] fanRow : fanRows) {
            String username = fanRow[COL_USERNAME];

            Fan cached = getFromCache(Fan.class, username);
            if (cached != null) {
                fans.add(cached);
                continue;
            }

            String[] userData = userDao.retrieveUser(username);
            if (isValidRow(userData)) {
                Fan fan = mapRowToFan(userData, fanRow);
                putInCache(fan, username);
                fans.add(fan);
            } else {
                logger.log(Level.WARNING, "User data not found for fan: {0}", username);
            }
        }
        return fans;
    }

    @Override
    public synchronized void updateFan(Fan fan) throws DAOException {
        validateNotNull(fan, FAN);

        userDao.updateUser(fan);

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row.length > COL_USERNAME && row[COL_USERNAME].equals(fan.getUsername())) {
                if (row.length <= COL_BIRTHDAY) {
                    String[] newRow = new String[COL_BIRTHDAY + 1];
                    System.arraycopy(row, 0, newRow, 0, row.length);
                    row = newRow;
                    data.set(i, row);
                }

                row[COL_FAVORITE_TEAM] = (fan.getFavTeam() != null) ? fan.getFavTeam().name() : "";
                row[COL_BIRTHDAY] = (fan.getBirthday() != null) ? fan.getBirthday().toString() : "";

                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    FAN, "update", fan.getUsername()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());
        putInCache(fan, fan.getUsername());
        notifyObservers(DaoOperation.UPDATE, FAN, fan.getUsername(), fan);
    }

    @Override
    public synchronized void deleteFan(Fan fan) throws DAOException {
        validateNotNull(fan, FAN);

        boolean found = deleteByColumn(COL_USERNAME, fan.getUsername());

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    FAN, "deletion", fan.getUsername()));
        }

        userDao.deleteUser(fan);
        logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
        removeFromCache(Fan.class, fan.getUsername());
        notifyObservers(DaoOperation.DELETE, FAN, fan.getUsername(), null);
    }

    // ========== PRIVATE HELPER ==========

    private Fan mapRowToFan(String[] userData, String[] fanData) {
        TeamNBA team = null;
        if (fanData.length > COL_FAVORITE_TEAM && !fanData[COL_FAVORITE_TEAM].isEmpty()) {
            team = TeamNBA.robustValueOf(fanData[COL_FAVORITE_TEAM]);
        }

        LocalDate birthday = null;
        if (fanData.length > COL_BIRTHDAY && !fanData[COL_BIRTHDAY].isEmpty()) {
            try {
                birthday = LocalDate.parse(fanData[COL_BIRTHDAY]);
            } catch (Exception ignored) {
                logger.log(Level.WARNING, "Invalid birthday format for user {0}: {1}",
                        new Object[]{userData[UserDaoCsv.COL_USERNAME], fanData[COL_BIRTHDAY]});
            }
        }

        return new Fan.Builder()
                .username(userData[UserDaoCsv.COL_USERNAME])
                .fullName(userData[UserDaoCsv.COL_FULL_NAME])
                .gender(userData[UserDaoCsv.COL_GENDER])
                .birthday(birthday)
                .favTeam(team)
                .build();
    }
}