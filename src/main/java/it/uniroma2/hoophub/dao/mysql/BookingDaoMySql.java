package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.helper_dao.BookingDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the BookingDao interface.
 * <p>
 * This class handles persistence for Booking entities using JDBC.
 * It implements a caching strategy (Identity Map) to optimize performance
 * and ensure object identity consistency within the same session.
 * </p>
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li><strong>Caching:</strong> Checks internal cache before querying DB.</li>
 * <li><strong>Code Reuse:</strong> Uses helper methods for mapping and parameter setting.</li>
 * <li><strong>Consistency:</strong> Invalidates cache entries on update/delete.</li>
 * </ul>
 * </p>
 *
 * @see BookingDao
 * @see AbstractMySqlDao
 */
public class BookingDaoMySql extends AbstractMySqlDao implements BookingDao {

    // ========== SQL Queries ==========
    private static final String SQL_INSERT_BOOKING =
            "INSERT INTO bookings (game_date, game_time, home_team, away_team, venue_id, " +
                    "fan_username, status, notified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BOOKING =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE id = ?";

    private static final String SQL_SELECT_ALL_BOOKINGS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_FAN =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE fan_username = ? ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_VENUE =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE venue_id = ? ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER =
            "SELECT b.id, b.game_date, b.game_time, b.home_team, b.away_team, b.venue_id, " +
                    "b.fan_username, b.status, b.notified " +
                    "FROM bookings b INNER JOIN venues v ON b.venue_id = v.id " +
                    "WHERE v.venue_manager_username = ? ORDER BY b.game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_DATE =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE game_date = ? ORDER BY game_time";

    private static final String SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE fan_username = ? AND status = ? " +
                    "ORDER BY game_date DESC";

    private static final String SQL_SELECT_UNNOTIFIED_BOOKINGS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, " +
                    "status, notified FROM bookings WHERE fan_username = ? AND notified = FALSE " +
                    "AND status IN ('CONFIRMED', 'REJECTED') ORDER BY game_date DESC";

    private static final String SQL_UPDATE_BOOKING =
            "UPDATE bookings SET game_date = ?, game_time = ?, home_team = ?, away_team = ?, " +
                    "venue_id = ?, status = ?, notified = ? WHERE id = ?";

    private static final String SQL_DELETE_BOOKING =
            "DELETE FROM bookings WHERE id = ?";

    private static final String SQL_CHECK_BOOKING_EXISTS =
            "SELECT COUNT(*) FROM bookings WHERE id = ?";

    private static final String SQL_GET_MAX_ID =
            "SELECT COALESCE(MAX(id), 0) FROM bookings";


    // ========== Constants ==========
    private static final String BOOKING = "Booking";
    // ========== Error messages ==========
    private static final String ERR_NULL_DATE = "Date cannot be null";
    private static final String ERR_NULL_STATUS = "Status cannot be null";
    private static final String ERR_BOOKING_NOT_FOUND = "Booking not found";

    /**
     * {@inheritDoc}
     * <p>
     * Extracts fan username and venue ID from the BookingBean to save the relationship.
     * After successful insertion, observers are notified for cross-persistence sync.
     * </p>
     *
     * @return
     */
    @Override
    public Booking saveBooking(Booking booking) throws DAOException {
        // Validazione Model
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_BOOKING,
                    Statement.RETURN_GENERATED_KEYS)) {

                // 1. Setta parametri comuni dal Model
                stmt.setDate(1, java.sql.Date.valueOf(booking.getGameDate()));
                stmt.setTime(2, java.sql.Time.valueOf(booking.getGameTime()));
                stmt.setString(3, booking.getHomeTeam().name());
                stmt.setString(4, booking.getAwayTeam().name());
                stmt.setInt(5, booking.getVenue().getId());

                // 2. Setta parametri specifici insert
                stmt.setString(6, booking.getFan().getUsername());
                stmt.setString(7, booking.getStatus().name());
                stmt.setBoolean(8, booking.isNotified());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);

                            // Costruiamo la NUOVA istanza con l'ID generato dal DB
                            Booking savedBooking = new Booking.Builder(
                                    newId, // ID AUTO-GENERATO
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

                            conn.commit();

                            // Cache Write-Through
                            // CACHE PUT
                            putInCache(savedBooking, newId);

                            logger.log(Level.INFO, "Booking saved successfully with ID: {0}", newId);
                            notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(newId), savedBooking);

                            return savedBooking; // RESTITUIAMO L'ENTITÀ
                        } else {
                            conn.rollback();
                            throw new DAOException("Creating booking failed, no ID obtained.");
                        }
                    }
                } else {
                    conn.rollback();
                    throw new DAOException("Creating booking failed, no rows affected.");
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reconstructs the full Booking object including Fan and Venue references.
     * </p>
     */
    @Override
    public Booking retrieveBooking(int bookingId) throws DAOException {
        validateIdInput(bookingId);

        // 1. CACHE CHECK (Identity Map)
        Booking cachedBooking = getFromCache(Booking.class, bookingId);
        if (cachedBooking != null) {
            return cachedBooking;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKING)) {
                stmt.setInt(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Booking booking = mapResultSetToBooking(rs);

                        // 2. CACHE PUT
                        putInCache(booking, bookingId);
                        return booking;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveAllBookings() throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_BOOKINGS);
                 ResultSet rs = stmt.executeQuery()) {

                // USE CENTRALIZED HELPER FOR CACHING LOGIC
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all bookings", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN)) {
                stmt.setString(1, fanUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    return processBookingResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by fan", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE)) {
                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    List<Booking> bookings = processBookingResultSet(rs);

                    logger.log(Level.FINE, "Retrieved {0} bookings for venue {1}",
                            new Object[]{bookings.size(), venueId});
                    return bookings;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by venue", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses a JOIN with the venues table to find all bookings for venues managed
     * by the specified venue manager.
     * </p>
     */
    @Override
    public List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateUsernameInput(venueManagerUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER)) {
                stmt.setString(1, venueManagerUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    return processBookingResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings for manager: " + venueManagerUsername, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        validateDateInput(date);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_DATE)) {
                stmt.setDate(1, Date.valueOf(date));

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    List<Booking> bookings = processBookingResultSet(rs);

                    logger.log(Level.FINE, "Retrieved {0} bookings for date {1}",
                            new Object[]{bookings.size(), date});
                    return bookings;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by date", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateUsernameInput(fanUsername);
        validateStatusInput(status);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS)) {
                stmt.setString(1, fanUsername);
                stmt.setString(2, status.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    List<Booking> bookings = processBookingResultSet(rs);

                    logger.log(Level.FINE, "Retrieved {0} {1} bookings for fan {2}",
                            new Object[]{bookings.size(), status, fanUsername});
                    return bookings;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by status", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNNOTIFIED_BOOKINGS)) {
                stmt.setString(1, fanUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    // USE CENTRALIZED HELPER FOR CACHING LOGIC
                    List<Booking> bookings = processBookingResultSet(rs);

                    logger.log(Level.FINE, "Retrieved {0} unnotified bookings for fan {1}",
                            new Object[]{bookings.size(), fanUsername});
                    return bookings;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving unnotified bookings", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBooking(Booking booking) throws DAOException {
        // 1. Validazione sul Model
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKING)) {

                // 2. Set parametri dal Model (Sostituisce setCommonStatementParameters se usava Bean)
                // Usiamo i getter diretti del Model per date, team e venue
                stmt.setDate(1, java.sql.Date.valueOf(booking.getGameDate()));
                stmt.setTime(2, java.sql.Time.valueOf(booking.getGameTime()));
                stmt.setString(3, booking.getHomeTeam().name());
                stmt.setString(4, booking.getAwayTeam().name());
                stmt.setInt(5, booking.getVenueId());

                // Parametri specifici (Status, Notified)
                stmt.setString(6, booking.getStatus().name());
                stmt.setBoolean(7, booking.isNotified());

                // WHERE Clause
                stmt.setInt(8, booking.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();

                    // === CACHE WRITE-THROUGH ===
                    // L'oggetto 'booking' è già aggiornato (Model-First), lo salviamo in cache.
                    putInCache(booking, booking.getId());

                    logger.log(Level.INFO, "Booking updated successfully: {0}", booking.getId());

                    // Notifica Observer passando il Model
                    notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(booking.getId()), booking);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + booking.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBooking(Booking booking) throws DAOException {
        // 1. Validazione
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        validateIdInput(booking.getId());

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BOOKING)) {
                // Usiamo l'ID preso dal Model
                stmt.setInt(1, booking.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();

                    // === CACHE REMOVE ===
                    // Attenzione: removeFromCache vuole la chiave (ID), non l'oggetto intero!
                    removeFromCache(Booking.class, booking.getId());

                    logger.log(Level.INFO, "Booking deleted successfully: {0}", booking.getId());

                    // Notifica Observer (passiamo ID come stringa e null come payload)
                    notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(booking.getId()), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + booking.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean bookingExists(int bookingId) throws DAOException {
        validateIdInput(bookingId);
        // Delegate to safe helper in AbstractMySqlDao
        return existsById(conn -> conn.prepareStatement(SQL_CHECK_BOOKING_EXISTS), bookingId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNextBookingId() throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_MAX_ID);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
                return 1;
            }
        } catch (SQLException e) {
            throw new DAOException("Error getting next booking ID", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateDateInput(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException(ERR_NULL_DATE);
        }
    }

    private void validateStatusInput(BookingStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(ERR_NULL_STATUS);
        }
    }

    /**
     * Parses a team string to TeamNBA enum.
     * Tries multiple formats: display name, abbreviation, enum constant.
     *
     * @param teamString Team name or abbreviation from database
     * @param context Context for error message (e.g., "home_team for booking 123")
     * @return TeamNBA enum
     * @throws DAOException if team cannot be parsed
     */
    private TeamNBA parseTeam(String teamString, String context) throws DAOException {
        TeamNBA team = TeamNBA.fromDisplayName(teamString);  // "Golden State Warriors"
        if (team == null) {
            team = TeamNBA.fromAbbreviation(teamString);  // "GSW"
        }
        if (team == null) {
            // Try enum constant name as last resort: "GOLDEN_STATE_WARRIORS"
            try {
                team = TeamNBA.valueOf(teamString);
            } catch (IllegalArgumentException ignored) {
                // Not a valid enum constant
            }
        }
        if (team == null) {
            throw new DAOException("Invalid team " + context + ": " + teamString);
        }
        return team;
    }

    /**
     * Maps ResultSet to Booking.
     * <p>
     * Uses {@link DaoLoadingContext} to prevent circular loops.
     * Loads complete Fan and Venue via respective DAOs (Facade pattern).
     * </p>
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException, DAOException {
        int bookingId = rs.getInt("id");
        String fanUsername = rs.getString("fan_username");
        int venueId = rs.getInt("venue_id");

        // Parse teams robustly
        TeamNBA homeTeam = parseTeam(rs.getString("home_team"), "home_team for booking " + bookingId);
        TeamNBA awayTeam = parseTeam(rs.getString("away_team"), "away_team for booking " + bookingId);

        String key = "Booking:" + bookingId;
        if (DaoLoadingContext.isLoading(key)) {
            // Return minimal booking object without loading relationships
            return new Booking.Builder(
                    bookingId,
                    rs.getDate("game_date").toLocalDate(),
                    rs.getTime("game_time").toLocalTime(),
                    homeTeam,
                    awayTeam,
                    null,  // Minimal object: no venue
                    null   // Minimal object: no fan
            )
                    .status(BookingStatus.valueOf(rs.getString("status")))
                    .notified(rs.getBoolean("notified"))
                    .build();
        }

        DaoLoadingContext.startLoading(key);
        try {
            // Retrieve Fan and Venue objects using DaoFactoryFacade (Factory pattern)
            BookingDaoHelper.BookingDependencies deps = BookingDaoHelper.loadDependencies(fanUsername, venueId);

            Fan fan = deps.fan();
            Venue venue = deps.venue();

            return new Booking.Builder(
                    bookingId,
                    rs.getDate("game_date").toLocalDate(),
                    rs.getTime("game_time").toLocalTime(),
                    homeTeam,
                    awayTeam,
                    venue,
                    fan
            )
                    .status(BookingStatus.valueOf(rs.getString("status")))
                    .notified(rs.getBoolean("notified"))
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    /**
     * Helper to process ResultSet into a List of Bookings using Cache logic.
     * <p>
     * Iterates through the ResultSet, checking the cache for each ID.
     * If cached, uses the existing instance; otherwise, maps the row and caches it.
     * </p>
     *
     * @param rs The ResultSet to process
     * @return List of fully populated Booking objects
     * @throws SQLException If database access error occurs
     * @throws DAOException If mapping fails
     */
    private List<Booking> processBookingResultSet(ResultSet rs) throws SQLException, DAOException {
        List<Booking> bookings = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");

            // 1. CACHE CHECK (Identity Map)
            Booking cached = getFromCache(Booking.class, id);

            if (cached != null) {
                bookings.add(cached);
            } else {
                // 2. LOAD & CACHE
                Booking booking = mapResultSetToBooking(rs);
                putInCache(booking, id);
                bookings.add(booking);
            }
        }
        return bookings;
    }
}