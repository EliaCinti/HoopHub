package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.CsvUtilities;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV implementation of the BookingDao interface.
 * <p>
 * This class has been refactored to eliminate circular dependencies with FanDao and VenueDao.
 * It follows best practices:
 * <ul>
 *   <li>Extends {@link AbstractCsvDao} to eliminate code duplication</li>
 *   <li>Accepts {@link BookingBean} for write operations (save/update)</li>
 *   <li>Returns {@link Booking} model objects with lazy-loaded references</li>
 *   <li>Does NOT call other DAOs during object mapping</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Breaking Circular Dependencies:</strong><br>
 * Previous implementation called FanDao and VenueDao during mapRowToBooking(), causing
 * StackOverflowError when entities navigated back. New implementation creates "stub" entities
 * containing only identifiers, leaving full object population to the service layer.
 * </p>
 *
 * @see BookingDao
 * @see AbstractCsvDao
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
                bookingBean.getHomeTeam(),
                bookingBean.getAwayTeam(),
                String.valueOf(bookingBean.getVenueId()),
                bookingBean.getFanUsername(),
                bookingBean.getStatus().name(),
                String.valueOf(bookingBean.isNotified())
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "Booking saved successfully: {0}", id);
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

        logger.log(Level.INFO, "Retrieved {0} bookings", bookings.size());
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fanUsername)) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for fan {1}",
                new Object[]{bookings.size(), fanUsername});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validatePositiveId(venueId);

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_VENUE_ID]) == venueId) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for venue {1}",
                new Object[]{bookings.size(), venueId});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateNotNullOrEmpty(venueManagerUsername, "Venue manager username");

        // NOTE: This requires querying VenueDao to find venues for this manager.
        // To avoid circular dependency, we keep it simple: the service layer
        // should handle this aggregation. For now, we return empty list.
        // Alternative: Store venueManagerUsername in bookings.csv (denormalization)

        logger.log(Level.WARNING, "retrieveBookingsByVenueManager should be handled at service layer to avoid circular dependencies");
        return new ArrayList<>();
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

        logger.log(Level.INFO, "Retrieved {0} {1} bookings for fan {2}",
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

        logger.log(Level.INFO, "Retrieved {0} unnotified bookings for fan {1}",
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
                row[COL_HOME_TEAM] = bookingBean.getHomeTeam();
                row[COL_AWAY_TEAM] = bookingBean.getAwayTeam();
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
     * <strong>IMPORTANT - Avoiding Circular Dependencies:</strong><br>
     * This method creates "stub" Fan and Venue objects containing ONLY the identifiers
     * (username/id). It does NOT call FanDao or VenueDao to fetch full entity data,
     * which would cause circular dependencies and StackOverflowError.
     * </p>
     * <p>
     * The service layer is responsible for populating full Fan/Venue details if needed.
     * This approach maintains clean separation of concerns and prevents DAO coupling.
     * </p>
     *
     * @param row The CSV row data
     * @return A Booking object with lazily-loaded Fan and Venue references
     * @throws DAOException If the row data is invalid or cannot be parsed
     */
    private Booking mapRowToBooking(String[] row) throws DAOException {
        try {
            // Create lazy-loaded "stub" entities with only identifiers
            // These are NOT fully populated - service layer handles that if needed
            String fanUsername = row[COL_FAN_USERNAME];
            int venueId = Integer.parseInt(row[COL_VENUE_ID]);

            // Create stub Fan (only username populated, other fields null/empty)
            Fan stubFan = new Fan.Builder()
                    .username(fanUsername)
                    .fullName("") // Placeholder
                    .gender("") // Placeholder
                    .favTeam("") // Placeholder
                    .birthday(LocalDate.now()) // Placeholder
                    .bookingList(new ArrayList<>())
                    .build();

            // Create stub Venue (only ID populated, other fields' placeholder)
            Venue stubVenue = new Venue.Builder()
                    .id(venueId)
                    .name("") // Placeholder
                    .type(null) // Placeholder
                    .address("") // Placeholder
                    .city("") // Placeholder
                    .maxCapacity(0) // Placeholder
                    .venueManager(null) // Placeholder
                    .build();

            return new Booking.Builder(
                    Integer.parseInt(row[COL_ID]),
                    LocalDate.parse(row[COL_GAME_DATE]),
                    LocalTime.parse(row[COL_GAME_TIME]),
                    row[COL_HOME_TEAM],
                    row[COL_AWAY_TEAM],
                    stubVenue,
                    stubFan
            )
                    .status(BookingStatus.valueOf(row[COL_STATUS]))
                    .notified(Boolean.parseBoolean(row[COL_NOTIFIED]))
                    .build();

        } catch (Exception e) {
            throw new DAOException("Error mapping booking data from CSV row", e);
        }
    }
}
