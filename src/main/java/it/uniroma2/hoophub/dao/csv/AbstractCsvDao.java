package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.utilities.CsvUtilities;

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
 *   <li>CSV file initialization and validation</li>
 *   <li>Common CRUD helper methods (getNextId, findRow, etc.)</li>
 *   <li>Centralized logging</li>
 *   <li>Error message formatting</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> Template Method - subclasses provide entity-specific
 * details (headers, column indices, mapping logic) while this class handles common operations.
 * </p>
 *
 * @see AbstractObservableDao
 * @see CsvUtilities
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

    /**
     * Constructs a new AbstractCsvDao with the specified file path.
     * <p>
     * This constructor:
     * <ol>
     *   <li>Initializes the logger with the concrete subclass name</li>
     *   <li>Creates the File object for the CSV file</li>
     *   <li>Initializes the CSV file if it doesn't exist</li>
     * </ol>
     * </p>
     *
     * @param filePath The relative or absolute path to the CSV file
     */
    protected AbstractCsvDao(String filePath) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.csvFile = new File(filePath);
        initializeCsvFile();
    }

    /**
     * Returns the CSV header for this entity.
     * <p>
     * Subclasses must implement this method to provide the column names for their CSV file.
     * </p>
     *
     * @return Array of column names (e.g., ["id", "name", "email"])
     */
    protected abstract String[] getHeader();

    /**
     * Initializes the CSV file if it doesn't exist.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates parent directories if needed</li>
     *   <li>Creates an empty CSV file with just the header row</li>
     *   <li>Logs the initialization</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Thread Safety:</strong> This method is called from the constructor,
     * which executes in a single-threaded context during object creation.
     * </p>
     */
    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                // Create parent directories if they don't exist
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        logger.log(Level.WARNING, "Could not create parent directories for: {0}", csvFile.getPath());
                    }
                }

                // Create file with header only
                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, getHeader(), emptyData);
                logger.log(Level.INFO, CsvDaoConstants.MSG_CSV_INITIALIZED, csvFile.getPath());
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, String.format(CsvDaoConstants.ERR_CSV_INIT_FAILED, csvFile.getPath()), e);
        }
    }

    /**
     * Generates the next available ID for a new entity.
     * <p>
     * This method scans all existing rows in the CSV file, finds the maximum ID value,
     * and returns maxId + 1. This approach works for both integer and long IDs.
     * </p>
     * <p>
     * <strong>Note:</strong> For high-concurrency scenarios, consider using a more
     * sophisticated ID generation mechanism (e.g., UUID, database sequence).
     * </p>
     *
     * @param idColumnIndex The index of the ID column (typically 0)
     * @return The next available ID (maxId + 1), or 1 if no records exist
     * @throws DAOException If there is an error reading the CSV file
     */
    protected synchronized long getNextId(int idColumnIndex) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        long maxId = 0;

        // Start from index 1 to skip header
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
     * Finds a specific row in the CSV file by matching a value in a specified column.
     * <p>
     * This is a generic search method that can be used to find rows by ID, username,
     * or any other unique identifier.
     * </p>
     * <p>
     * <strong>Performance Note:</strong> This method performs a linear search (O(n)).
     * For large datasets, consider indexing or caching strategies.
     * </p>
     *
     * @param columnIndex The index of the column to search in
     * @param value The value to search for (exact match, case-sensitive)
     * @return The matching row as a String array, or {@code null} if not found
     * @throws DAOException If there is an error reading the CSV file
     */
    protected synchronized String[] findRowByValue(int columnIndex, String value) throws DAOException {
        if (value == null) {
            return new String[0];
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Start from index 1 to skip header
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > columnIndex && row[columnIndex].equals(value)) {
                return row;
            }
        }

        return new String[0];
    }

    /**
     * Counts the total number of entities (rows) in the CSV file.
     * <p>
     * Excludes the header row from the count.
     * </p>
     *
     * @return The number of data rows in the file (excluding header)
     * @throws DAOException If there is an error reading the CSV file
     */
    protected synchronized int countEntities() throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        return Math.max(0, data.size() - 1); // Subtract 1 for header
    }

    /**
     * Validates that a string value is not null or empty.
     * <p>
     * This is a common validation pattern used across all DAO methods.
     * </p>
     *
     * @param value The string value to validate
     * @param fieldName The name of the field (for error messages)
     * @throws IllegalArgumentException If the value is null or empty (after trimming)
     */
    protected void validateNotNullOrEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format(CsvDaoConstants.ERR_NULL_OR_EMPTY, fieldName));
        }
    }

    /**
     * Validates that an object is not null.
     * <p>
     * This is a common validation pattern used across all DAO methods.
     * </p>
     *
     * @param object The object to validate
     * @param entityName The name of the entity (for error messages)
     * @throws IllegalArgumentException If the object is null
     */
    protected void validateNotNull(Object object, String entityName) {
        if (object == null) {
            throw new IllegalArgumentException(String.format(CsvDaoConstants.ERR_NULL_ENTITY, entityName));
        }
    }

    /**
     * Validates that an ID is positive.
     * <p>
     * This is a common validation pattern for methods that accept ID parameters.
     * </p>
     *
     * @param id The ID to validate
     * @throws IllegalArgumentException If the ID is not positive (less than or equal to 0)
     */
    protected void validatePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(CsvDaoConstants.ERR_INVALID_ID);
        }
    }

    /**
     * Deletes an entity from the CSV file by its ID.
     * <p>
     * This is a common delete pattern used across all CSV DAOs. It:
     * <ol>
     *   <li>Reads all data from the CSV file</li>
     *   <li>Searches for the row with the matching ID</li>
     *   <li>Removes that row if found</li>
     *   <li>Writes the updated data back to the file</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Thread Safety:</strong> This method is synchronized to prevent
     * concurrent modification issues.
     * </p>
     *
     * @param id The ID of the entity to delete
     * @param idColumnIndex The index of the ID column in the CSV file
     * @return {@code true} if the entity was found and deleted, {@code false} otherwise
     * @throws DAOException If there is an error reading or writing the CSV file
     */
    protected synchronized boolean deleteById(long id, int idColumnIndex) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, find and remove matching row
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
     * Returns the path of the CSV file managed by this DAO.
     * <p>
     * Useful for logging and debugging purposes.
     * </p>
     *
     * @return The absolute path of the CSV file
     */
    protected String getFilePath() {
        return csvFile.getAbsolutePath();
    }

    /**
     * Reads all data rows from the CSV file, automatically skipping the header.
     * <p>
     * This is a convenience method that eliminates the need to manually skip the header
     * row when iterating over CSV data. Returns an empty list if the file only contains
     * a header or is empty.
     * </p>
     *
     * @return List of data rows (without header), or empty list if no data exists
     * @throws DAOException If there is an error reading the CSV file
     */
    protected synchronized List<String[]> readAllDataRows() throws DAOException {
        List<String[]> allData = CsvUtilities.readAll(csvFile);

        if (allData.size() <= CsvDaoConstants.FIRST_DATA_ROW) {
            return new ArrayList<>();
        }

        // Return all rows except header
        return new ArrayList<>(allData.subList(CsvDaoConstants.FIRST_DATA_ROW, allData.size()));
    }

    /**
     * Finds all rows matching a specific value in a column.
     * <p>
     * Unlike {@link #findRowByValue(int, String)} which returns only the first match,
     * this method returns all matching rows. Useful for one-to-many relationships.
     * </p>
     *
     * @param columnIndex The index of the column to search in
     * @param value The value to search for (exact match, case-sensitive)
     * @return List of all matching rows, or empty list if none found
     * @throws DAOException If there is an error reading the CSV file
     */
    protected synchronized List<String[]> findAllRowsByValue(int columnIndex, String value) throws DAOException {
        if (value == null) {
            return new ArrayList<>();
        }

        List<String[]> matches = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        // Start from index 1 to skip header
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > columnIndex && row[columnIndex].equals(value)) {
                matches.add(row);
            }
        }

        return matches;
    }
}
