package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.helper_dao.BookingDaoHelper;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.enums.TeamNBA;
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
 * CSV implementation of {@link BookingDao}.
 *
 * <p>Supports UPSERT semantics for cross-persistence synchronization:
 * if an ID already exists, updates the row instead of creating a duplicate.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class BookingDaoCsv extends AbstractCsvDao implements BookingDao {

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "bookings.csv";
    private static final String[] CSV_HEADER = {
            "id", "game_date", "game_time", "home_team", "away_team",
            "venue_id", "fan_username", "status", "notified"
    };

    private static final String BOOKING = "Booking";
    private static final String FAN_USERNAME = "Fan username";

    private static final int COL_ID = 0;
    private static final int COL_GAME_DATE = 1;
    private static final int COL_GAME_TIME = 2;
    private static final int COL_HOME_TEAM = 3;
    private static final int COL_AWAY_TEAM = 4;
    private static final int COL_VENUE_ID = 5;
    private static final int COL_FAN_USERNAME = 6;
    private static final int COL_STATUS = 7;
    private static final int COL_NOTIFIED = 8;

    public BookingDaoCsv() {
        super(CSV_FILE_PATH);
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized void saveBooking(Booking booking) throws DAOException {
        validateNotNull(booking, BOOKING);

        int id = booking.getId();

        // UPSERT: Check if ID already exists
        if (id > 0 && existsById(id)) {
            upsertExistingBooking(booking);
            return;
        }

        // Generate new ID if not provided
        if (id == 0) {
            id = (int) getNextId(COL_ID);
        }

        Booking savedBooking = new Booking.Builder(
                id, booking.getGameDate(), booking.getGameTime(),
                booking.getHomeTeam(), booking.getAwayTeam(),
                booking.getVenue(), booking.getFan())
                .status(booking.getStatus())
                .notified(booking.isNotified())
                .build();

        CsvUtilities.writeFile(csvFile, bookingToRow(savedBooking));
        putInCache(savedBooking, id);
        notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(id), savedBooking);
    }

    /**
     * Updates an existing booking (UPSERT when ID already exists).
     */
    private void upsertExistingBooking(Booking booking) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (Integer.parseInt(row[COL_ID]) == booking.getId()) {
                data.set(i, bookingToRow(booking));
                found = true;
                break;
            }
        }

        if (!found) {
            CsvUtilities.writeFile(csvFile, bookingToRow(booking));
        } else {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        }

        putInCache(booking, booking.getId());
        notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(booking.getId()), booking);
    }

    /**
     * Checks if a booking with the given ID already exists.
     */
    private boolean existsById(int id) throws DAOException {
        String[] row = findRowByValue(COL_ID, String.valueOf(id));
        return row != null && row.length > 0;
    }

    /**
     * Converts a Booking to a CSV row array.
     */
    private String[] bookingToRow(Booking booking) {
        return new String[]{
                String.valueOf(booking.getId()),
                booking.getGameDate().toString(),
                booking.getGameTime().toString(),
                booking.getHomeTeam().name(),
                booking.getAwayTeam().name(),
                String.valueOf(booking.getVenue().getId()),
                booking.getFan().getUsername(),
                booking.getStatus().name(),
                String.valueOf(booking.isNotified())
        };
    }

    @Override
    public synchronized Booking retrieveBooking(int bookingId) throws DAOException {
        validatePositiveId(bookingId);

        Booking cached = getFromCache(Booking.class, bookingId);
        if (cached != null) return cached;

        String[] row = findRowByValue(COL_ID, String.valueOf(bookingId));
        if (!isValidRow(row)) return null;

        Booking booking = mapRowToBooking(row);
        putInCache(booking, bookingId);
        return booking;
    }

    @Override
    public synchronized List<Booking> retrieveAllBookings() throws DAOException {
        List<String[]> data = readAllDataRows();
        return processRowsToBookings(data);
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);
        List<String[]> matchingRows = findAllRowsByValue(COL_FAN_USERNAME, fanUsername);
        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validatePositiveId(venueId);
        List<String[]> matchingRows = findAllRowsByValue(COL_VENUE_ID, String.valueOf(venueId));
        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateNotNullOrEmpty(venueManagerUsername, "Venue manager username");

        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();
        List<Venue> managedVenues = venueDao.retrieveVenuesByManager(venueManagerUsername);
        if (managedVenues.isEmpty()) return new ArrayList<>();

        List<Integer> managedVenueIds = new ArrayList<>();
        for (Venue venue : managedVenues) {
            managedVenueIds.add(venue.getId());
        }

        List<String[]> data = readAllDataRows();
        List<String[]> matchingRows = new ArrayList<>();

        for (String[] row : data) {
            if (row.length <= COL_VENUE_ID) continue;
            try {
                int bookingVenueId = Integer.parseInt(row[COL_VENUE_ID]);
                if (managedVenueIds.contains(bookingVenueId)) {
                    matchingRows.add(row);
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid booking row: invalid venue ID");
            }
        }

        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        validateNotNull(date, "Date");

        List<String[]> data = readAllDataRows();
        List<String[]> matchingRows = new ArrayList<>();

        for (String[] row : data) {
            if (row.length <= COL_GAME_DATE) continue;
            if (LocalDate.parse(row[COL_GAME_DATE]).equals(date)) {
                matchingRows.add(row);
            }
        }

        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);
        validateNotNull(status, "BookingStatus");

        List<String[]> data = readAllDataRows();
        List<String[]> matchingRows = new ArrayList<>();

        for (String[] row : data) {
            if (row.length <= COL_STATUS) continue;
            if (row[COL_FAN_USERNAME].equals(fanUsername) && row[COL_STATUS].equals(status.name())) {
                matchingRows.add(row);
            }
        }
        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateNotNullOrEmpty(fanUsername, FAN_USERNAME);

        List<String[]> data = readAllDataRows();
        List<String[]> matchingRows = new ArrayList<>();

        for (String[] row : data) {
            if (row.length <= COL_NOTIFIED) continue;
            if (row[COL_FAN_USERNAME].equals(fanUsername) && !Boolean.parseBoolean(row[COL_NOTIFIED])) {
                matchingRows.add(row);
            }
        }
        return processRowsToBookings(matchingRows);
    }

    @Override
    public synchronized void updateBooking(Booking booking) throws DAOException {
        validateNotNull(booking, BOOKING);
        validatePositiveId(booking.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > COL_ID && Integer.parseInt(row[COL_ID]) == booking.getId()) {
                if (row.length > COL_NOTIFIED) {
                    updateRowData(row, booking);
                    found = true;
                }
                break;
            }
        }

        if (!found) {
            throw new DAOException("Booking not found for update: " + booking.getId());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        putInCache(booking, booking.getId());
        notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(booking.getId()), booking);
    }

    @Override
    public synchronized void deleteBooking(Booking booking) throws DAOException {
        validateNotNull(booking, BOOKING);
        int bookingId = booking.getId();
        if (bookingId <= 0) throw new IllegalArgumentException("Invalid booking ID: " + bookingId);

        boolean found = deleteByColumn(COL_ID, String.valueOf(bookingId));
        if (!found) throw new DAOException("Booking not found for deletion: " + bookingId);

        removeFromCache(Booking.class, bookingId);
        notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(bookingId), null);
    }

    @Override
    public synchronized boolean bookingExists(int bookingId) throws DAOException {
        validatePositiveId(bookingId);
        return isValidRow(findRowByValue(COL_ID, String.valueOf(bookingId)));
    }

    @Override
    public synchronized int getNextBookingId() throws DAOException {
        return (int) getNextId(COL_ID);
    }

    // ========== PRIVATE HELPERS ==========

    private void updateRowData(String[] row, Booking booking) {
        row[COL_GAME_DATE] = booking.getGameDate().toString();
        row[COL_GAME_TIME] = booking.getGameTime().toString();
        row[COL_HOME_TEAM] = booking.getHomeTeam().name();
        row[COL_AWAY_TEAM] = booking.getAwayTeam().name();
        row[COL_VENUE_ID] = String.valueOf(booking.getVenueId());
        row[COL_STATUS] = booking.getStatus().name();
        row[COL_NOTIFIED] = String.valueOf(booking.isNotified());
    }

    private List<Booking> processRowsToBookings(List<String[]> rows) throws DAOException {
        List<Booking> bookings = new ArrayList<>();
        for (String[] row : rows) {
            Booking b = resolveBookingFromRow(row);
            if (b != null) bookings.add(b);
        }
        return bookings;
    }

    private Booking resolveBookingFromRow(String[] row) throws DAOException {
        if (!isValidRow(row)) return null;
        try {
            int id = Integer.parseInt(row[COL_ID]);
            Booking cached = getFromCache(Booking.class, id);
            if (cached != null) return cached;

            Booking b = mapRowToBooking(row);
            putInCache(b, id);
            return b;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Booking mapRowToBooking(String[] row) throws DAOException {
        try {
            int bookingId = Integer.parseInt(row[COL_ID]);
            String fanUsername = row[COL_FAN_USERNAME];
            int venueId = Integer.parseInt(row[COL_VENUE_ID]);
            String bookingKey = "Booking:" + bookingId;

            if (DaoLoadingContext.isLoading(bookingKey)) {
                return loadAndBuildBooking(bookingId, fanUsername, venueId, row);
            }

            DaoLoadingContext.startLoading(bookingKey);
            try {
                return loadAndBuildBooking(bookingId, fanUsername, venueId, row);
            } finally {
                DaoLoadingContext.finishLoading(bookingKey);
            }
        } catch (Exception e) {
            throw new DAOException("Error mapping booking data from CSV row", e);
        }
    }

    private Booking loadAndBuildBooking(int bookingId, String fanUsername, int venueId, String[] row) throws DAOException {
        BookingDaoHelper.BookingDependencies deps = BookingDaoHelper.loadDependencies(fanUsername, venueId);

        TeamNBA homeTeam = TeamNBA.robustValueOf(row[COL_HOME_TEAM]);
        TeamNBA awayTeam = TeamNBA.robustValueOf(row[COL_AWAY_TEAM]);

        if (homeTeam == null || awayTeam == null) {
            throw new DAOException("Invalid team data in CSV for booking ID: " + bookingId);
        }

        return new Booking.Builder(
                bookingId,
                LocalDate.parse(row[COL_GAME_DATE]),
                LocalTime.parse(row[COL_GAME_TIME]),
                homeTeam,
                awayTeam,
                deps.venue(),
                deps.fan()
        )
                .status(BookingStatus.valueOf(row[COL_STATUS]))
                .notified(Boolean.parseBoolean(row[COL_NOTIFIED]))
                .build();
    }
}