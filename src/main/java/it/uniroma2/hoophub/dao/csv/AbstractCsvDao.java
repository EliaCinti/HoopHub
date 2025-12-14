package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all CSV DAO implementations.
 * <p>
 * This class eliminates code duplication by providing common functionality shared across
 * all CSV-based DAO classes. It extends {@link AbstractObservableDao} to support the
 * Observer pattern for cross-persistence synchronization.
 * </p>
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 * <li>CSV file initialization and validation</li>
 * <li>Common CRUD helper methods (getNextId, findRow, etc.)</li>
 * <li>Centralized logging</li>
 * <li>Error message formatting</li>
 * <li><strong>Centralized Caching:</strong> Delegates to {@link GlobalCache}.</li>
 * </ul>
 * </p>
 */
public abstract class AbstractCsvDao extends AbstractObservableDao {

    /**
     * Logger instance for this DAO (uses the concrete subclass name).
     */
    protected final Logger logger;

    /**
     * The CSV file managed by this DAO.
     */
    protected final File csvFile;

    // NOTA: La mappa locale 'cache' è stata rimossa in favore di GlobalCache

    /**
     * Constructs a new AbstractCsvDao with the specified file path.
     *
     * @param filePath The relative or absolute path to the CSV file
     */
    protected AbstractCsvDao(String filePath) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.csvFile = new File(filePath);
        initializeCsvFile();
    }

    // =================================================================================
    // CACHE MANAGEMENT METHODS (Delegated to GlobalCache)
    // =================================================================================

    /**
     * Genera una chiave univoca per la cache globale (es. "Booking:10").
     */
    protected String generateCacheKey(Class<?> clazz, Object id) {
        if (id == null) return null;
        return clazz.getSimpleName() + ":" + id;
    }

    /**
     * Retrieves an entity from the Global Cache safely using Generics.
     *
     * @param clazz The class type of the object (e.g., User.class)
     * @param key The unique identifier (ID or Username)
     * @return The entity if found and matches the type, null otherwise
     */
    protected <T> T getFromCache(Class<T> clazz, Object key) {
        String cacheKey = generateCacheKey(clazz, key);
        Object cachedObject = GlobalCache.getInstance().get(cacheKey);
        return cachedObject != null ? clazz.cast(cachedObject) : null;
    }

    /**
     * Stores an entity in the Global Cache.
     * The key is automatically generated based on the entity's class and the ID.
     *
     * @param entity The entity to cache
     * @param key The unique identifier for the entity
     */
    protected void putInCache(Object entity, Object key) {
        if (entity != null && key != null) {
            String cacheKey = generateCacheKey(entity.getClass(), key);
            GlobalCache.getInstance().put(cacheKey, entity);
        }
    }

    /**
     * Removes an entity from the Global Cache.
     *
     * @param clazz The class type of the object to remove
     * @param key The unique identifier
     */
    protected void removeFromCache(Class<?> clazz, Object key) {
        if (key != null) {
            String cacheKey = generateCacheKey(clazz, key);
            GlobalCache.getInstance().remove(cacheKey);
        }
    }

    // =================================================================================
    // CSV HANDLING METHODS (Invariati)
    // =================================================================================

    /**
     * Returns the CSV header for this entity.
     * Subclasses must implement this method to provide the column names for their CSV file.
     *
     * @return Array of column names (e.g., ["id", "name", "email"])
     */
    protected abstract String[] getHeader();

    /**
     * Initializes the CSV file if it doesn't exist.
     */
    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        logger.log(Level.WARNING, "Could not create parent directories for: {0}", csvFile.getPath());
                    }
                }

                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, getHeader(), emptyData);
                logger.log(Level.INFO, CsvDaoConstants.MSG_CSV_INITIALIZED, csvFile.getPath());
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, String.format(CsvDaoConstants.ERR_CSV_INIT_FAILED, csvFile.getPath()), e);
        }
    }

    protected synchronized long getNextId(int idColumnIndex) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        long maxId = 0;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            try {
                long id = Long.parseLong(data.get(i)[idColumnIndex]);
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException e) {
                int finalI = i;
                logger.log(Level.WARNING, () ->
                        "Invalid ID format in row " + finalI + ": " + data.get(finalI)[idColumnIndex]);
            }
        }

        return maxId + 1;
    }

    protected synchronized String[] findRowByValue(int columnIndex, String value) throws DAOException {
        if (value == null) {
            return new String[0];
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > columnIndex && row[columnIndex].equals(value)) {
                return row;
            }
        }

        return new String[0];
    }

    protected synchronized boolean deleteByColumn(int columnIndex, String value) throws DAOException {
        if (value == null) {
            return false;
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            if (data.get(i).length > columnIndex && data.get(i)[columnIndex].equals(value)) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (found) {
            CsvUtilities.updateFile(csvFile, getHeader(), data.subList(1, data.size()));
        }

        return found;
    }

    protected void validateNotNullOrEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format(CsvDaoConstants.ERR_NULL_OR_EMPTY, fieldName));
        }
    }

    protected void validateNotNull(Object object, String entityName) {
        if (object == null) {
            throw new IllegalArgumentException(String.format(CsvDaoConstants.ERR_NULL_ENTITY, entityName));
        }
    }

    protected void validatePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(CsvDaoConstants.ERR_INVALID_ID);
        }
    }

    protected synchronized boolean deleteById(long id, int idColumnIndex) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            if (Long.parseLong(data.get(i)[idColumnIndex]) == id) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (found) {
            CsvUtilities.updateFile(csvFile, getHeader(), data.subList(1, data.size()));
        }

        return found;
    }

    protected synchronized List<String[]> readAllDataRows() throws DAOException {
        List<String[]> allData = CsvUtilities.readAll(csvFile);

        if (allData.size() <= CsvDaoConstants.FIRST_DATA_ROW) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allData.subList(CsvDaoConstants.FIRST_DATA_ROW, allData.size()));
    }

    protected synchronized List<String[]> findAllRowsByValue(int columnIndex, String value) throws DAOException {
        if (value == null) {
            return new ArrayList<>();
        }

        List<String[]> matches = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > columnIndex && row[columnIndex].equals(value)) {
                matches.add(row);
            }
        }

        return matches;
    }

    /**
     * Checks if a CSV row is valid (not null and not empty).
     * Useful for validating results from findRowByValue or UserDao.retrieveUser.
     *
     * @param row The CSV row array
     * @return true if the row contains data, false otherwise
     */
    protected boolean isValidRow(String[] row) {
        return row != null && row.length > 0;
    }
}