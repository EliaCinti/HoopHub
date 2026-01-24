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
 * Abstract base class for CSV DAO implementations.
 *
 * <p>Extends {@link AbstractObservableDao} to support the <b>Observer pattern (GoF)</b>
 * for cross-persistence synchronization. Provides common functionality: file initialization,
 * CRUD helpers, validation, and centralized caching via {@link GlobalCache}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see GlobalCache
 */
public abstract class AbstractCsvDao extends AbstractObservableDao {

    protected final Logger logger;
    protected final File csvFile;

    /**
     * Constructs DAO and initializes the CSV file if needed.
     *
     * @param filePath path to the CSV file
     */
    protected AbstractCsvDao(String filePath) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.csvFile = new File(filePath);
        initializeCsvFile();
    }

    // ========== ABSTRACT METHOD ==========

    /**
     * Returns the CSV header columns for this entity.
     *
     * @return array of column names
     */
    protected abstract String[] getHeader();

    // ========== CACHE METHODS (Delegated to GlobalCache) ==========

    /**
     * Generates a unique cache key (e.g., "Booking:10").
     */
    protected String generateCacheKey(Class<?> clazz, Object id) {
        if (id == null) return null;
        return clazz.getSimpleName() + ":" + id;
    }

    /**
     * Retrieves entity from cache with type safety.
     *
     * @param clazz entity class
     * @param key   entity identifier
     * @return cached entity or null
     */
    protected <T> T getFromCache(Class<T> clazz, Object key) {
        String cacheKey = generateCacheKey(clazz, key);
        Object cachedObject = GlobalCache.getInstance().get(cacheKey);
        return cachedObject != null ? clazz.cast(cachedObject) : null;
    }

    /**
     * Stores entity in cache.
     *
     * @param entity the entity to cache
     * @param key    entity identifier
     */
    protected void putInCache(Object entity, Object key) {
        if (entity != null && key != null) {
            String cacheKey = generateCacheKey(entity.getClass(), key);
            GlobalCache.getInstance().put(cacheKey, entity);
        }
    }

    /**
     * Removes entity from cache.
     *
     * @param clazz entity class
     * @param key   entity identifier
     */
    protected void removeFromCache(Class<?> clazz, Object key) {
        if (key != null) {
            String cacheKey = generateCacheKey(clazz, key);
            GlobalCache.getInstance().remove(cacheKey);
        }
    }

    // ========== CSV HELPER METHODS ==========

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

    /**
     * Generates the next available ID from the CSV file.
     *
     * @param idColumnIndex column index containing IDs
     * @return next available ID
     */
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

    /**
     * Finds a single row by column value.
     *
     * @param columnIndex column to search
     * @param value       value to match
     * @return matching row or empty array if not found
     */
    protected synchronized String[] findRowByValue(int columnIndex, String value) throws DAOException {
        if (value == null) return new String[0];

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > columnIndex && row[columnIndex].equals(value)) {
                return row;
            }
        }
        return new String[0];
    }

    /**
     * Finds all rows matching a column value.
     *
     * @param columnIndex column to search
     * @param value       value to match
     * @return list of matching rows
     */
    protected synchronized List<String[]> findAllRowsByValue(int columnIndex, String value) throws DAOException {
        if (value == null) return new ArrayList<>();

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
     * Deletes a row by column value.
     *
     * @return true if row was found and deleted
     */
    protected synchronized boolean deleteByColumn(int columnIndex, String value) throws DAOException {
        if (value == null) return false;

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

    /**
     * Deletes a row by numeric ID.
     *
     * @return true if row was found and deleted
     */
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

    /**
     * Reads all data rows (excluding header).
     *
     * @return list of data rows
     */
    protected synchronized List<String[]> readAllDataRows() throws DAOException {
        List<String[]> allData = CsvUtilities.readAll(csvFile);

        if (allData.size() <= CsvDaoConstants.FIRST_DATA_ROW) {
            return new ArrayList<>();
        }
        return new ArrayList<>(allData.subList(CsvDaoConstants.FIRST_DATA_ROW, allData.size()));
    }

    /**
     * Checks if a CSV row is valid (not null/empty).
     *
     * @param row the row to check
     * @return true if row contains data
     */
    protected boolean isValidRow(String[] row) {
        return row != null && row.length > 0;
    }

    // ========== VALIDATION METHODS ==========

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
}