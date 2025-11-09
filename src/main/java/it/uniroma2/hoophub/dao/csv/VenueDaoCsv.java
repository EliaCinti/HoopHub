package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.VenueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of the VenueDao interface.
 * <p>
 * This class provides data access operations for Venue entities stored in CSV files.
 * It extends {@link AbstractCsvDao} to leverage common functionality like file initialization,
 * ID generation, and validation, eliminating code duplication.
 * </p>
 * <p>
 * <strong>CSV File Structure (venues.csv):</strong>
 * <pre>
 * id,name,type,address,city,max_capacity,venue_manager_username
 * 1,The Sports Hub,PUB,123 Main St,Los Angeles,150,manager1
 * 2,Hoops Central,SPORTS_BAR,456 Oak Ave,New York,200,manager2
 * </pre>
 * </p>
 * <p>
 * <strong>Circular Dependency Prevention:</strong> The {@link #mapRowToVenue(String[])} method
 * creates Venue objects with a STUB VenueManager (only username populated). The full VenueManager
 * object with all managed venues should be loaded separately via VenueManagerDao if needed.
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> This class uses primitive parameters (int, String) for query
 * methods and Bean objects for write operations, following the standardized DAO design pattern
 * to prevent circular dependencies.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> All public methods are synchronized to prevent concurrent
 * modification issues when multiple threads access the CSV file.
 * </p>
 *
 * @see VenueDao Interface defining the contract
 * @see AbstractCsvDao Base class providing common CSV functionality
 * @see Venue Domain model representing a venue
 * @see VenueBean DTO for data transfer
 */
public class VenueDaoCsv extends AbstractCsvDao implements VenueDao {

    // ========== CSV CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venues.csv";
    private static final String[] CSV_HEADER = {"id", "name", "type", "address", "city", "max_capacity", "venue_manager_username"};

    // ========== CONSTANTS ==========

    private static final String VENUE = "Venue";

    // ========== COLUMN INDICES ==========

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_ADDRESS = 3;
    private static final int COL_CITY = 4;
    private static final int COL_MAX_CAPACITY = 5;
    private static final int COL_VENUE_MANAGER_USERNAME = 6;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructs a new VenueDaoCsv and initializes the CSV file.
     * <p>
     * The parent constructor ({@link AbstractCsvDao}) handles:
     * <ul>
     *   <li>Creating the File object</li>
     *   <li>Creating parent directories if needed</li>
     *   <li>Initializing the CSV file with headers if it doesn't exist</li>
     *   <li>Setting up the logger</li>
     * </ul>
     * </p>
     */
    public VenueDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC METHODS (VenueDao Interface Implementation) ==========

    /**
     * {@inheritDoc}
     * <p>
     * If the venue bean has an ID of 0, a new ID is automatically generated using
     * {@link #getNextId(int)}. The ID is set in the bean before saving.
     * </p>
     * <p>
     * After successful save, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void saveVenue(VenueBean venueBean) throws DAOException {
        validateNotNull(venueBean, "VenueBean");
        validateNotNullOrEmpty(venueBean.getName(), "Venue name");
        validateNotNull(venueBean.getType(), "Venue type");
        validateNotNullOrEmpty(venueBean.getAddress(), "Address");
        validateNotNullOrEmpty(venueBean.getCity(), "City");
        validateNotNullOrEmpty(venueBean.getVenueManagerUsername(), "Venue manager username");

        if (venueBean.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive");
        }

        // Generate ID if not provided
        int id = venueBean.getId() == 0 ? (int) getNextId(COL_ID) : venueBean.getId();
        venueBean.setId(id);

        String[] newRow = {
                String.valueOf(id),
                venueBean.getName(),
                venueBean.getType().name(),
                venueBean.getAddress(),
                venueBean.getCity(),
                String.valueOf(venueBean.getMaxCapacity()),
                venueBean.getVenueManagerUsername()
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, () ->
                "Venue saved successfully: ID=" + id + ", name=" + venueBean.getName());
        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(id), venueBean);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned Venue object contains a STUB VenueManager with only the username
     * populated (to avoid circular dependency). Load the full VenueManager separately
     * via VenueManagerDao if needed.
     * </p>
     */
    @Override
    public synchronized Venue retrieveVenue(int venueId) throws DAOException {
        validatePositiveId(venueId);

        String[] venueRow = findRowByValue(COL_ID, String.valueOf(venueId));
        if (venueRow == null) {
            return null;
        }

        return mapRowToVenue(venueRow);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each returned Venue object has a STUB VenueManager (only username populated).
     * </p>
     */
    @Override
    public synchronized List<Venue> retrieveAllVenues() throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<Venue> venues = new ArrayList<>();

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            venues.add(mapRowToVenue(data.get(i)));
        }

        return venues;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each returned Venue object has a STUB VenueManager (only username populated).
     * </p>
     */
    @Override
    public synchronized List<Venue> retrieveVenuesByManager(String venueManagerUsername) throws DAOException {
        validateNotNullOrEmpty(venueManagerUsername, "Venue manager username");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<Venue> venues = new ArrayList<>();

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_VENUE_MANAGER_USERNAME].equals(venueManagerUsername)) {
                venues.add(mapRowToVenue(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} venues for manager: {1}",
                new Object[]{venues.size(), venueManagerUsername});
        return venues;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each returned Venue object has a STUB VenueManager (only username populated).
     * </p>
     */
    @Override
    public synchronized List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        validateNotNullOrEmpty(city, "City");

        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<Venue> venues = new ArrayList<>();

        // Skip header row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_CITY].equalsIgnoreCase(city)) {
                venues.add(mapRowToVenue(row));
            }
        }

        logger.log(Level.INFO, () ->
                "Retrieved " + venues.size() + " venues in city: " + city);
        return venues;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful update, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void updateVenue(VenueBean venueBean) throws DAOException {
        validateNotNull(venueBean, "VenueBean");
        validatePositiveId(venueBean.getId());
        validateNotNullOrEmpty(venueBean.getName(), "Venue name");
        validateNotNull(venueBean.getType(), "Venue type");
        validateNotNullOrEmpty(venueBean.getAddress(), "Address");
        validateNotNullOrEmpty(venueBean.getCity(), "City");

        if (venueBean.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // Skip header, update matching row
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == venueBean.getId()) {
                row[COL_NAME] = venueBean.getName();
                row[COL_TYPE] = venueBean.getType().name();
                row[COL_ADDRESS] = venueBean.getAddress();
                row[COL_CITY] = venueBean.getCity();
                row[COL_MAX_CAPACITY] = String.valueOf(venueBean.getMaxCapacity());
                // Note: venue_manager_username is NOT updated here - use separate method to reassign manager
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    VENUE, "update", venueBean.getId()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Venue updated successfully: {0}", venueBean.getId());
        notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venueBean.getId()), venueBean);
    }

    /**
     * {@inheritDoc}
     * <p>
     * After successful deletion, observers are notified for cross-persistence synchronization.
     * </p>
     */
    @Override
    public synchronized void deleteVenue(int venueId) throws DAOException {
        validatePositiveId(venueId);

        boolean found = deleteById(venueId, COL_ID);

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    VENUE, "deletion", venueId));
        }

        logger.log(Level.INFO, "Venue deleted successfully: {0}", venueId);
        notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(venueId), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean venueExists(int venueId) throws DAOException {
        validatePositiveId(venueId);
        return findRowByValue(COL_ID, String.valueOf(venueId)) != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses {@link AbstractCsvDao#getNextId(int)} to find the maximum ID and return maxId + 1.
     * </p>
     */
    @Override
    public synchronized int getNextVenueId() throws DAOException {
        return (int) getNextId(COL_ID);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps CSV row data to a Venue domain object.
     * <p>
     * <strong>Circular Dependency Prevention:</strong> This method creates a STUB VenueManager
     * with only the username populated. The VenueManager has an empty managed venues list.
     * This prevents circular calls between VenueDao and VenueManagerDao during object construction.
     * </p>
     * <p>
     * If the full VenueManager object with all data is needed, it should be loaded separately
     * via VenueManagerDao after the Venue is constructed.
     * </p>
     *
     * @param row Array containing venue data [id, name, type, address, city, max_capacity, venue_manager_username]
     * @return A fully constructed Venue object with STUB VenueManager
     * @throws DAOException If there's an error parsing data or constructing the Venue
     */
    private Venue mapRowToVenue(String[] row) throws DAOException {
        try {
            int id = Integer.parseInt(row[COL_ID]);
            VenueType type = VenueType.valueOf(row[COL_TYPE]);
            int maxCapacity = Integer.parseInt(row[COL_MAX_CAPACITY]);
            String managerUsername = row[COL_VENUE_MANAGER_USERNAME];

            // Create STUB VenueManager with only username (no circular dependency)
            VenueManager stubManager = new VenueManager.Builder()
                    .username(managerUsername)
                    .fullName("") // Placeholder - not loaded
                    .gender("") // Placeholder - not loaded
                    .companyName("") // Placeholder - not loaded
                    .phoneNumber("0000000000") // Placeholder - not loaded
                    .managedVenues(Collections.emptyList()) // EMPTY list - no circular dependency
                    .build();

            return new Venue.Builder()
                    .id(id)
                    .name(row[COL_NAME])
                    .type(type)
                    .address(row[COL_ADDRESS])
                    .city(row[COL_CITY])
                    .maxCapacity(maxCapacity)
                    .venueManager(stubManager)
                    .build();
        } catch (NumberFormatException e) {
            throw new DAOException("Invalid number format in venue data: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new DAOException("Error constructing Venue object: " + e.getMessage(), e);
        }
    }
}
