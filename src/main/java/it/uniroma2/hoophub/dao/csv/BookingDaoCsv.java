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
    public synchronized Booking saveBooking(Booking booking) throws DAOException {
        validateNotNull(booking, BOOKING);

        // 1. Gestione ID: Se è 0, ne generiamo uno nuovo. Se è > 0 (es. da sync), lo manteniamo.
        int id = booking.getId();
        if (id == 0) {
            id = (int) getNextId(COL_ID); // Metodo helper di AbstractCsvDao
        }

        // 2. Creazione dell'oggetto "Salvato" (con l'ID definitivo)
        Booking savedBooking = new Booking.Builder(
                id,
                booking.getGameDate(),
                booking.getGameTime(),
                booking.getHomeTeam(),
                booking.getAwayTeam(),
                booking.getVenue(),
                booking.getFan()
        )
                .status(booking.getStatus())
                .notified(booking.isNotified())
                .build();

        // 3. Preparazione riga CSV leggendo dal NUOVO oggetto
        String[] newRow = {
                String.valueOf(savedBooking.getId()),
                savedBooking.getGameDate().toString(),
                savedBooking.getGameTime().toString(),
                savedBooking.getHomeTeam().name(), // Enum -> String
                savedBooking.getAwayTeam().name(), // Enum -> String
                String.valueOf(savedBooking.getVenue().getId()),
                savedBooking.getFan().getUsername(),
                savedBooking.getStatus().name(),
                String.valueOf(savedBooking.isNotified())
        };

        // 4. Scrittura su file
        CsvUtilities.writeFile(csvFile, newRow);

        // 5. Notifica e Ritorno
        notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(id), savedBooking);

        return savedBooking;
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
    public synchronized void updateBooking(Booking booking) throws DAOException {
        // 1. Validazione sul Model
        validateNotNull(booking, BOOKING);
        validatePositiveId(booking.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            // Cerchiamo la riga con l'ID corrispondente
            if (Integer.parseInt(row[COL_ID]) == booking.getId()) {
                // 2. Aggiornamento dati leggendo dal MODEL
                row[COL_GAME_DATE] = booking.getGameDate().toString();
                row[COL_GAME_TIME] = booking.getGameTime().toString();
                row[COL_HOME_TEAM] = booking.getHomeTeam().name(); // Enum -> String
                row[COL_AWAY_TEAM] = booking.getAwayTeam().name(); // Enum -> String
                row[COL_VENUE_ID] = String.valueOf(booking.getVenueId());
                row[COL_STATUS] = booking.getStatus().name();      // Enum -> String
                row[COL_NOTIFIED] = String.valueOf(booking.isNotified());

                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    BOOKING, "update", booking.getId()));
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Booking updated successfully: {0}", booking.getId());

        // 3. Notifica Observer passando il Model
        notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(booking.getId()), booking);
    }

    @Override
    public synchronized void deleteBooking(Booking booking) throws DAOException {
        // 1. Validazione Model
        validateNotNull(booking, BOOKING);

        int bookingId = booking.getId();
        // Validazione ID positivo (se non hai un helper dedicato, usa questo check diretto)
        if (bookingId <= 0) {
            throw new IllegalArgumentException("Invalid booking ID: " + bookingId);
        }

        // 2. Cancellazione dal CSV
        // Usiamo deleteByColumn (metodo di AbstractCsvDao) passando la colonna ID e il valore
        boolean found = deleteByColumn(COL_ID, String.valueOf(bookingId));

        if (!found) {
            throw new DAOException(String.format(CsvDaoConstants.ERR_ENTITY_NOT_FOUND_FOR_OP,
                    BOOKING, "deletion", bookingId));
        }

        logger.log(Level.INFO, "Booking deleted successfully: {0}", bookingId);

        // 3. Notifica Observer
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
