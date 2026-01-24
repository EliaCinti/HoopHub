package it.uniroma2.hoophub.dao.csv;

/**
 * Shared constants for all CSV DAO implementations.
 *
 * <p>Centralizes error messages, directory paths, and common indices
 * following the DRY principle. Entity-specific constants remain in their DAOs.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public final class CsvDaoConstants {

    private CsvDaoConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== DIRECTORY CONFIGURATION ==========

    /** Base directory for all CSV files. */
    public static final String CSV_BASE_DIR = "db/csv/";

    // ========== COMMON WORDS ==========

    public static final String USERNAME = "Username";

    // ========== ERROR MESSAGES ==========

    /** Format: String.format(ERR_NULL_ENTITY, "Booking") */
    public static final String ERR_NULL_ENTITY = "%s cannot be null";

    /** Format: String.format(ERR_NULL_OR_EMPTY, "Username") */
    public static final String ERR_NULL_OR_EMPTY = "%s cannot be null or empty";

    public static final String ERR_INVALID_ID = "ID must be positive";

    /** Format: String.format(ERR_ENTITY_NOT_FOUND_FOR_OP, "Booking", "update", id) */
    public static final String ERR_ENTITY_NOT_FOUND_FOR_OP = "%s not found for %s: %s";

    // ========== LOG MESSAGES ==========

    public static final String MSG_CSV_INITIALIZED = "Initialized CSV file: %s";
    public static final String ERR_CSV_INIT_FAILED = "Failed to initialize CSV file: %s";

    // ========== CSV INDICES ==========

    /** Index of first data row (after header). */
    public static final int FIRST_DATA_ROW = 1;
}
