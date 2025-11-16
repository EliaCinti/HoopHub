package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
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
     * <strong>Circular Dependency Prevention:</strong> This method uses {@link DaoLoadingContext}
     * to detect and prevent infinite loops when loading related entities. If a Fan or Venue is
     * already being loaded in the current call stack (circular reference), it skips reloading
     * to break the cycle.
     * </p>
     * <p>
     * <strong>Object Completeness:</strong> When not in a circular loading situation, this method
     * loads complete Fan and Venue objects with all real data by delegating to FanDao and VenueDao,
     * ensuring that the returned Booking object is fully populated.
     * </p>
     *
     * @param row The CSV row data
     * @return A Booking object with fully loaded Fan and Venue references
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
                // Break the cycle by loading Fan and Venue which will detect the cycle
                // and return minimal objects (Fan with empty bookings, Venue with minimal data)
                DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();

                FanDao fanDao = daoFactory.getFanDao();
                Fan minimalFan = fanDao.retrieveFan(fanUsername);

                VenueDao venueDao = daoFactory.getVenueDao();
                Venue minimalVenue = venueDao.retrieveVenue(venueId);

                if (minimalFan == null) {
                    throw new DAOException("Fan not found for booking: " + fanUsername);
                }
                if (minimalVenue == null) {
                    throw new DAOException("Venue not found for booking: " + venueId);
                }

                return new Booking.Builder(
                        bookingId,
                        LocalDate.parse(row[COL_GAME_DATE]),
                        LocalTime.parse(row[COL_GAME_TIME]),
                        TeamNBA.fromDisplayName(row[COL_HOME_TEAM]),
                        TeamNBA.fromDisplayName(row[COL_AWAY_TEAM]),
                        minimalVenue,
                        minimalFan
                )
                        .status(BookingStatus.valueOf(row[COL_STATUS]))
                        .notified(Boolean.parseBoolean(row[COL_NOTIFIED]))
                        .build();
            }

            // Mark this booking as being loaded
            DaoLoadingContext.startLoading(bookingKey);
            try {
                // Load COMPLETE Fan and Venue objects (not stubs)
                DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();

                FanDao fanDao = daoFactory.getFanDao();
                Fan fan = fanDao.retrieveFan(fanUsername);

                VenueDao venueDao = daoFactory.getVenueDao();
                Venue venue = venueDao.retrieveVenue(venueId);

                if (fan == null) {
                    throw new DAOException("Fan not found for booking: " + fanUsername);
                }
                if (venue == null) {
                    throw new DAOException("Venue not found for booking: " + venueId);
                }

                // Build Booking with COMPLETE Fan and Venue
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
            } finally {
                // Always clean up the loading context
                DaoLoadingContext.finishLoading(bookingKey);
            }

        } catch (Exception e) {
            throw new DAOException("Error mapping booking data from CSV row", e);
        }
    }
}
