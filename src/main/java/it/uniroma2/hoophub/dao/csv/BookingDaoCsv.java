package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.helper_dao.BookingDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of BookingDao.
 * <p>
 * Manages Booking data in CSV file with references to Fan and Venue.
 * </p>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Factory</strong>: Created via BookingDaoFactory</li>
 *   <li><strong>Facade</strong>: Uses DaoFactoryFacade to access FanDao and VenueDao</li>
 *   <li><strong>Observer</strong>: Notifies observers for CSV-MySQL sync</li>
 *   <li><strong>Builder</strong>: Uses Booking.Builder for object construction</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Circular Dependency:</strong> Uses {@link DaoLoadingContext} to prevent infinite loops
 * when Booking loads Fan and Fan loads Bookings back.
 * </p>
 *
 * @see BookingDao
 * @see DaoLoadingContext
 */
public class BookingDaoCsv extends AbstractCsvDao implements BookingDao {

    // ========== CSV FILE CONFIGURATION ==========

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "bookings.csv";
    private static final String[] CSV_HEADER = {
            "id", "game_date", "game_time", "home_team", "away_team",
            "venue_id", "fan_username", "status", "notified"
    };

    // ========== CONSTANTS ==========
    private static final String BOOKING = "Booking";
    private static final String FAN_USERNAME = "Fan username";

    // ========== COLUMN INDICES ==========

    private static final int COL_ID = 0;
    private static final int COL_GAME_DATE = 1;
    private static final int COL_GAME_TIME = 2;
    private static final int COL_HOME_TEAM = 3;
    private static final int COL_AWAY_TEAM = 4;
    private static final int COL_VENUE_ID = 5;
    private static final int COL_FAN_USERNAME = 6;
    private static final int COL_STATUS = 7;
    private static final int COL_NOTIFIED = 8;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructs a new BookingDaoCsv.
     * <p>
     * Initializes the CSV file if it doesn't exist (handled by AbstractCsvDao).
     * </p>
     */
    public BookingDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    // ========== PUBLIC DAO METHODS ==========

    @Override
    public synchronized void saveBooking(BookingBean bookingBean) throws DAOException {
        validateNotNull(bookingBean, "BookingBean");

        // Generate ID if not present
        int id = bookingBean.getId() == 0 ? (int) getNextId(COL_ID) : bookingBean.getId();

        String[] newRow = {
                String.valueOf(id),
                bookingBean.getGameDate().toString(),
                bookingBean.getGameTime().toString(),
                String.valueOf(bookingBean.getHomeTeam()),
                String.valueOf(bookingBean.getAwayTeam()),
                String.valueOf(bookingBean.getVenueId()),
                bookingBean.getFanUsername(),
                bookingBean.getStatus().name(),
                String.valueOf(bookingBean.isNotified())
        };

        CsvUtilities.writeFile(csvFile, newRow);

        notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(id), bookingBean);
    }

    @Override
    public synchronized Booking retrieveBooking(int bookingId) throws DAOException {
        validatePositiveId(bookingId);

        String[] row = findRowByValue(COL_ID, String.valueOf(bookingId));
        if (row == null) {
            return null;
        }

        return mapRowToBooking(row);
    }

    @Override
    public synchronized List<Booking> retrieveAllBookings() throws DAOException {
        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            bookings.add(mapRowToBooking(data.get(i)));
        }

        logger.log(Level.FINE, "Retrieved {0} bookings", bookings.size());
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);

        // Uses AbstractCsvDao helper to find all matching rows
        List<String[]> matchingRows = findAllRowsByValue(COL_FAN_USERNAME, fanUsername);
        List<Booking> bookings = new ArrayList<>();

        for (String[] row : matchingRows) {
            bookings.add(mapRowToBooking(row));
        }

        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validatePositiveId(venueId);

        // Uses AbstractCsvDao helper to find all matching rows
        List<String[]> matchingRows = findAllRowsByValue(COL_VENUE_ID, String.valueOf(venueId));
        List<Booking> bookings = new ArrayList<>();

        for (String[] row : matchingRows) {
            bookings.add(mapRowToBooking(row));
        }

        logger.log(Level.FINE, "Retrieved {0} bookings for venue {1}",
                new Object[]{bookings.size(), venueId});
        return bookings;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Implementation Note:</strong> This method performs an in-memory "JOIN" equivalent.
     * It first retrieves all venues managed by the user, collects their IDs, and then
     * filters all bookings that match those venue IDs.
     * </p>
     */
    @Override
    public synchronized List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateNotNullOrEmpty(venueManagerUsername, "Venue manager username");

        // Step 1: Retrieve all venues managed by this manager using VenueDao
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueDao venueDao = daoFactory.getVenueDao();

        List<Venue> managedVenues = venueDao.retrieveVenuesByManager(venueManagerUsername);

        // Optimization: If no venues, return empty list immediately
        if (managedVenues.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 2: Collect IDs of managed venues for fast lookup
        List<Integer> managedVenueIds = new ArrayList<>();
        for (Venue venue : managedVenues) {
            managedVenueIds.add(venue.getId());
        }

        // Step 3: Retrieve all bookings and filter by venue ID
        // Note: We use readAll directly instead of retrieveAllBookings to avoid building objects we might discard
        List<String[]> data = CsvUtilities.readAll(csvFile);
        List<Booking> matchingBookings = new ArrayList<>();

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            try {
                int bookingVenueId = Integer.parseInt(row[COL_VENUE_ID]);

                // Filter: Does this booking belong to one of the manager's venues?
                if (managedVenueIds.contains(bookingVenueId)) {
                    // Only map (and load heavy dependencies) if it's a match
                    matchingBookings.add(mapRowToBooking(row));
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid booking row at index {0}", i);
            }
        }

        logger.log(Level.FINE, "Retrieved {0} bookings for manager {1}",
                new Object[]{matchingBookings.size(), venueManagerUsername});
        return matchingBookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        validateNotNull(date, "Date");

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (LocalDate.parse(row[COL_GAME_DATE]).equals(date)) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, () ->
                "Retrieved " + bookings.size() + " bookings for date " + date);
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);
        validateNotNull(status, "BookingStatus");

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fanUsername) &&
                    row[COL_STATUS].equals(status.name())) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.FINE, "Retrieved {0} {1} bookings for fan {2}",
                new Object[]{bookings.size(), status, fanUsername});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fanUsername) &&
                    !Boolean.parseBoolean(row[COL_NOTIFIED])) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.FINE, "Retrieved {0} unnotified bookings for fan {1}",
                new Object[]{bookings.size(), fanUsername});
        return bookings;
    }

    @Override
    public synchronized void updateBooking(BookingBean bookingBean) throws DAOException {
        validateNotNull(bookingBean, "BookingBean");
        validatePositiveId(bookingBean.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == bookingBean.getId()) {
                row[COL_GAME_DATE] = bookingBean.getGameDate().toString();
                row[COL_GAME_TIME] = bookingBean.getGameTime().toString();
                row[COL_HOME_TEAM] = String.valueOf(bookingBean.getHomeTeam());
                row[COL_AWAY_TEAM] = String.valueOf(bookingBean.getAwayTeam());
                row[COL_VENUE_ID] = String.valueOf(bookingBean.getVenueId());
                row[COL_STATUS] = bookingBean.getStatus().name();
                row[COL_NOTIFIED] = String.valueOf(bookingBean.isNotified());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    BOOKING, "update", bookingBean.getId()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Booking updated successfully: {0}", bookingBean.getId());
        notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(bookingBean.getId()), bookingBean);
    }

    @Override
    public synchronized void deleteBooking(int bookingId) throws DAOException {
        validatePositiveId(bookingId);

        boolean found = deleteById(bookingId, COL_ID);

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    BOOKING, "deletion", bookingId));
        }

        logger.log(Level.INFO, "Booking deleted successfully: {0}", bookingId);
        notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(bookingId), null);
    }

    @Override
    public synchronized boolean bookingExists(int bookingId) throws DAOException {
        validatePositiveId(bookingId);
        return findRowByValue(COL_ID, String.valueOf(bookingId)) != null;
    }

    @Override
    public synchronized int getNextBookingId() throws DAOException {
        return (int) getNextId(COL_ID);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a CSV row to a Booking domain object.
     * <p>
     * <strong>Refactoring:</strong> The duplication has been removed by extracting the
     * object retrieval and building logic into the {@link #loadAndBuildBooking} helper method.
     * The circular dependency check now simply decides whether to wrap that call
     * in a new loading context or not.
     * </p>
     *
     * @param row The CSV row data
     * @return A Booking object with loaded Fan and Venue references
     * @throws DAOException If the row data is invalid or cannot be parsed
     */
    private Booking mapRowToBooking(String[] row) throws DAOException {
        try {
            int bookingId = Integer.parseInt(row[COL_ID]);
            String fanUsername = row[COL_FAN_USERNAME];
            int venueId = Integer.parseInt(row[COL_VENUE_ID]);

            String bookingKey = "Booking:" + bookingId;

            // Check if we're in a circular loading situation
            if (DaoLoadingContext.isLoading(bookingKey)) {
                // Already loading: just proceed to load/build (retrieveFan/Venue will handle their own cycles)
                return loadAndBuildBooking(bookingId, fanUsername, venueId, row);
            }

            // Not loading: mark as loading, then proceed
            DaoLoadingContext.startLoading(bookingKey);
            try {
                return loadAndBuildBooking(bookingId, fanUsername, venueId, row);
            } finally {
                // Always clean up the loading context
                DaoLoadingContext.finishLoading(bookingKey);
            }

        } catch (Exception e) {
            throw new DAOException("Error mapping booking data from CSV row", e);
        }
    }

    /**
     * Helper method to retrieve dependencies and build the Booking object.
     * <p>
     * This centralizes the logic for loading the Fan and Venue and constructing
     * the Booking entity, eliminating code duplication.
     * </p>
     *
     * @param bookingId The ID of the booking
     * @param fanUsername The username of the fan
     * @param venueId The ID of the venue
     * @param row The raw CSV row data containing other fields
     * @return Fully constructed Booking object
     * @throws DAOException If dependencies cannot be found
     */
    private Booking loadAndBuildBooking(int bookingId, String fanUsername, int venueId, String[] row) throws DAOException {
        // Retrieve Fan and Venue objects using DaoFactoryFacade (Factory pattern)
        BookingDaoHelper.BookingDependencies deps = BookingDaoHelper.loadDependencies(fanUsername, venueId);

        Fan fan = deps.fan();
        Venue venue = deps.venue();

        // Build Booking with loaded Fan and Venue
        return new Booking.Builder(
                bookingId,
                LocalDate.parse(row[COL_GAME_DATE]),
                LocalTime.parse(row[COL_GAME_TIME]),
                TeamNBA.fromDisplayName(row[COL_HOME_TEAM]),
                TeamNBA.fromDisplayName(row[COL_AWAY_TEAM]),
                venue,
                fan
        )
                .status(BookingStatus.valueOf(row[COL_STATUS]))
                .notified(Boolean.parseBoolean(row[COL_NOTIFIED]))
                .build();
    }
}
