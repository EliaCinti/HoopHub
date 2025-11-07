package it.uniroma2.hoophub.dao.csv;

/**
 * Constants shared across all CSV DAO implementations.
 * <p>
 * This class centralizes common constants to reduce duplication and improve maintainability.
 * It follows the DRY (Don't Repeat Yourself) principle by providing shared error messages,
 * configuration values, and common patterns used by multiple CSV DAO classes.
 * </p>
 * <p>
 * <strong>Design Note:</strong> Entity-specific constants (CSV headers, column indices, file paths)
 * remain in their respective DAO classes. This class contains only truly shared constants.
 * </p>
 */
public final class CsvDaoConstants {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CsvDaoConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== DIRECTORY CONFIGURATION ==========

    /**
     * Base directory for all CSV files.
     */
    public static final String CSV_BASE_DIR = "data/";

    // ========== COMMON ERROR MESSAGES ==========

    /**
     * Error message format for null entity validation.
     * Usage: String.format(ERR_NULL_ENTITY, "Booking")
     */
    public static final String ERR_NULL_ENTITY = "%s cannot be null";

    /**
     * Error message format for null or empty string validation.
     * Usage: String.format(ERR_NULL_OR_EMPTY, "Username")
     */
    public static final String ERR_NULL_OR_EMPTY = "%s cannot be null or empty";

    /**
     * Error message for invalid ID (must be positive).
     */
    public static final String ERR_INVALID_ID = "ID must be positive";

    /**
     * Error message format for entity not found during retrieval.
     * Usage: String.format(ERR_ENTITY_NOT_FOUND, "Booking", id)
     */
    public static final String ERR_ENTITY_NOT_FOUND = "%s not found: %s";

    /**
     * Error message format for entity not found during specific operation.
     * Usage: String.format(ERR_ENTITY_NOT_FOUND_FOR_OP, "Booking", "update", id)
     */
    public static final String ERR_ENTITY_NOT_FOUND_FOR_OP = "%s not found for %s: %s";

    // ========== CSV FILE OPERATION MESSAGES ==========

    /**
     * Log message format for successful CSV file initialization.
     * Usage: String.format(MSG_CSV_INITIALIZED, "bookings.csv")
     */
    public static final String MSG_CSV_INITIALIZED = "Initialized CSV file: %s";

    /**
     * Error message format for CSV file initialization failure.
     * Usage: String.format(ERR_CSV_INIT_FAILED, "bookings.csv")
     */
    public static final String ERR_CSV_INIT_FAILED = "Failed to initialize CSV file: %s";

    // ========== COMMON COLUMN NAMES ==========

    /**
     * Standard column name for ID fields.
     */
    public static final String COL_NAME_ID = "id";

    /**
     * Standard column name for username fields.
     */
    public static final String COL_NAME_USERNAME = "username";

    // ========== CSV INDICES ==========

    /**
     * Index of the first data row (after header).
     */
    public static final int FIRST_DATA_ROW = 1;

    /**
     * Index of the header row.
     */
    public static final int HEADER_ROW = 0;
}
