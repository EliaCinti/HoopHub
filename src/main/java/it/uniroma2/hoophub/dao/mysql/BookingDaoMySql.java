package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.helper_dao.BookingDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.enums.BookingStatus;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link BookingDao}.
 *
 * <p>Uses UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) for cross-persistence
 * synchronization to handle ID conflicts gracefully.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class BookingDaoMySql extends AbstractMySqlDao implements BookingDao {

    // UPSERT query for sync
    private static final String SQL_UPSERT =
            "INSERT INTO bookings (id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "game_date = VALUES(game_date), game_time = VALUES(game_time), home_team = VALUES(home_team), " +
                    "away_team = VALUES(away_team), venue_id = VALUES(venue_id), fan_username = VALUES(fan_username), " +
                    "status = VALUES(status), notified = VALUES(notified)";

    // Standard INSERT for auto-generated ID
    private static final String SQL_INSERT_AUTO_ID =
            "INSERT INTO bookings (game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BOOKING =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE id = ?";

    private static final String SQL_SELECT_ALL_BOOKINGS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_FAN =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE fan_username = ? ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_VENUE =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE venue_id = ? ORDER BY game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER =
            "SELECT b.id, b.game_date, b.game_time, b.home_team, b.away_team, b.venue_id, b.fan_username, b.status, b.notified " +
                    "FROM bookings b INNER JOIN venues v ON b.venue_id = v.id WHERE v.venue_manager_username = ? ORDER BY b.game_date DESC";

    private static final String SQL_SELECT_BOOKINGS_BY_DATE =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE game_date = ? ORDER BY game_time";

    private static final String SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE fan_username = ? AND status = ? ORDER BY game_date DESC";

    private static final String SQL_SELECT_UNNOTIFIED_BOOKINGS =
            "SELECT id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified FROM bookings WHERE fan_username = ? AND notified = FALSE AND status IN ('CONFIRMED', 'REJECTED') ORDER BY game_date DESC";

    private static final String SQL_UPDATE_BOOKING =
            "UPDATE bookings SET game_date = ?, game_time = ?, home_team = ?, away_team = ?, venue_id = ?, status = ?, notified = ? WHERE id = ?";

    private static final String SQL_DELETE_BOOKING = "DELETE FROM bookings WHERE id = ?";
    private static final String SQL_CHECK_BOOKING_EXISTS = "SELECT COUNT(*) FROM bookings WHERE id = ?";
    private static final String SQL_GET_MAX_ID = "SELECT COALESCE(MAX(id), 0) FROM bookings";

    private static final String BOOKING = "Booking";

    @Override
    public void saveBooking(Booking booking) throws DAOException {
        if (booking == null) throw new IllegalArgumentException("Booking cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            int newId = booking.getId() > 0 ? saveWithUpsert(conn, booking) : saveWithAutoId(conn, booking);

            Booking savedBooking = new Booking.Builder(newId, booking.getGameDate(), booking.getGameTime(),
                    booking.getHomeTeam(), booking.getAwayTeam(), booking.getVenue(), booking.getFan())
                    .status(booking.getStatus()).notified(booking.isNotified()).build();

            conn.commit();
            putInCache(savedBooking, newId);
            logger.log(Level.INFO, "Booking saved with ID: {0}", newId);
            notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(newId), savedBooking);

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    private int saveWithUpsert(Connection conn, Booking booking) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, booking.getId());
            stmt.setDate(2, Date.valueOf(booking.getGameDate()));
            stmt.setTime(3, Time.valueOf(booking.getGameTime()));
            stmt.setString(4, booking.getHomeTeam().name());
            stmt.setString(5, booking.getAwayTeam().name());
            stmt.setInt(6, booking.getVenue().getId());
            stmt.setString(7, booking.getFan().getUsername());
            stmt.setString(8, booking.getStatus().name());
            stmt.setBoolean(9, booking.isNotified());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new DAOException("Creating booking failed, no rows affected.");
            if (affectedRows == 2) logger.log(Level.FINE, "Booking ID {0} updated via UPSERT", booking.getId());
            return booking.getId();
        }
    }

    private int saveWithAutoId(Connection conn, Booking booking) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_AUTO_ID, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(booking.getGameDate()));
            stmt.setTime(2, Time.valueOf(booking.getGameTime()));
            stmt.setString(3, booking.getHomeTeam().name());
            stmt.setString(4, booking.getAwayTeam().name());
            stmt.setInt(5, booking.getVenue().getId());
            stmt.setString(6, booking.getFan().getUsername());
            stmt.setString(7, booking.getStatus().name());
            stmt.setBoolean(8, booking.isNotified());

            if (stmt.executeUpdate() == 0) throw new DAOException("Creating booking failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
                throw new DAOException("Creating booking failed, no ID obtained.");
            }
        }
    }

    @Override
    public Booking retrieveBooking(int bookingId) throws DAOException {
        validateIdInput(bookingId);
        Booking cached = getFromCache(Booking.class, bookingId);
        if (cached != null) return cached;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKING)) {
            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    putInCache(booking, bookingId);
                    return booking;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving booking", e);
        }
    }

    @Override
    public List<Booking> retrieveAllBookings() throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_BOOKINGS);
             ResultSet rs = stmt.executeQuery()) {
            return processBookingResultSet(rs);
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all bookings", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN)) {
            stmt.setString(1, fanUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by fan", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validateIdInput(venueId);
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE)) {
            stmt.setInt(1, venueId);
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by venue", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateUsernameInput(venueManagerUsername);
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER)) {
            stmt.setString(1, venueManagerUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by venue manager", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_DATE)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by date", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateUsernameInput(fanUsername);
        if (status == null) throw new IllegalArgumentException("Status cannot be null");
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS)) {
            stmt.setString(1, fanUsername);
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by status", e);
        }
    }

    @Override
    public List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNNOTIFIED_BOOKINGS)) {
            stmt.setString(1, fanUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving unnotified bookings", e);
        }
    }

    @Override
    public void updateBooking(Booking booking) throws DAOException {
        if (booking == null) throw new IllegalArgumentException("Booking cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKING)) {
                stmt.setDate(1, Date.valueOf(booking.getGameDate()));
                stmt.setTime(2, Time.valueOf(booking.getGameTime()));
                stmt.setString(3, booking.getHomeTeam().name());
                stmt.setString(4, booking.getAwayTeam().name());
                stmt.setInt(5, booking.getVenueId());
                stmt.setString(6, booking.getStatus().name());
                stmt.setBoolean(7, booking.isNotified());
                stmt.setInt(8, booking.getId());

                if (stmt.executeUpdate() > 0) {
                    conn.commit();
                    putInCache(booking, booking.getId());
                    logger.log(Level.INFO, "Booking updated: {0}", booking.getId());
                    notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(booking.getId()), booking);
                } else {
                    conn.rollback();
                    throw new DAOException("Booking not found: " + booking.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void deleteBooking(Booking booking) throws DAOException {
        if (booking == null) throw new IllegalArgumentException("Booking cannot be null");
        validateIdInput(booking.getId());

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BOOKING)) {
                stmt.setInt(1, booking.getId());
                if (stmt.executeUpdate() > 0) {
                    conn.commit();
                    removeFromCache(Booking.class, booking.getId());
                    logger.log(Level.INFO, "Booking deleted: {0}", booking.getId());
                    notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(booking.getId()), null);
                } else {
                    conn.rollback();
                    throw new DAOException("Booking not found: " + booking.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting booking", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public boolean bookingExists(int bookingId) throws DAOException {
        validateIdInput(bookingId);
        return existsById(conn -> conn.prepareStatement(SQL_CHECK_BOOKING_EXISTS), bookingId);
    }

    @Override
    public int getNextBookingId() throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_MAX_ID);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) + 1 : 1;
        } catch (SQLException e) {
            throw new DAOException("Error getting next booking ID", e);
        }
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException, DAOException {
        // Materialize ALL columns from the ResultSet BEFORE any nested query.
        // BookingDaoHelper.loadDependencies() calls retrieveFan() and retrieveVenue(),
        // which open new statements on the shared connection and invalidate this ResultSet.
        int bookingId = rs.getInt("id");
        String fanUsername = rs.getString("fan_username");
        int venueId = rs.getInt("venue_id");
        LocalDate gameDate = rs.getDate("game_date").toLocalDate();
        java.time.LocalTime gameTime = rs.getTime("game_time").toLocalTime();
        String homeTeamStr = rs.getString("home_team");
        String awayTeamStr = rs.getString("away_team");
        String statusStr = rs.getString("status");
        boolean notified = rs.getBoolean("notified");

        TeamNBA homeTeam = TeamNBA.robustValueOf(homeTeamStr);
        TeamNBA awayTeam = TeamNBA.robustValueOf(awayTeamStr);
        if (homeTeam == null || awayTeam == null) throw new DAOException("Invalid team in DB for booking " + bookingId);
        BookingStatus status = BookingStatus.valueOf(statusStr);

        String key = "Booking:" + bookingId;
        if (DaoLoadingContext.isLoading(key)) {
            return new Booking.Builder(bookingId, gameDate, gameTime, homeTeam, awayTeam, null, null)
                    .status(status).notified(notified).build();
        }

        DaoLoadingContext.startLoading(key);
        try {
            BookingDaoHelper.BookingDependencies deps = BookingDaoHelper.loadDependencies(fanUsername, venueId);
            return new Booking.Builder(bookingId, gameDate, gameTime, homeTeam, awayTeam, deps.venue(), deps.fan())
                    .status(status).notified(notified).build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    private List<Booking> processBookingResultSet(ResultSet rs) throws SQLException, DAOException {
        // Phase 1: Materialize ALL rows from the ResultSet into raw data.
        // This must complete before ANY nested query (fan, venue) because
        // those queries reuse the shared connection and invalidate this ResultSet.
        List<Object[]> uncachedRows = new ArrayList<>();
        List<Booking> bookings = new ArrayList<>();
        List<Integer> insertionOrder = new ArrayList<>(); // -1 = cached, >=0 = uncached index

        int uncachedIndex = 0;
        while (rs.next()) {
            int id = rs.getInt("id");
            Booking cached = getFromCache(Booking.class, id);
            if (cached != null) {
                bookings.add(cached);
                insertionOrder.add(-1);
            } else {
                uncachedRows.add(new Object[]{
                        id,
                        rs.getString("fan_username"),
                        rs.getInt("venue_id"),
                        rs.getDate("game_date").toLocalDate(),
                        rs.getTime("game_time").toLocalTime(),
                        rs.getString("home_team"),
                        rs.getString("away_team"),
                        rs.getString("status"),
                        rs.getBoolean("notified")
                });
                bookings.add(null); // placeholder
                insertionOrder.add(uncachedIndex++);
            }
        }

        // Phase 2: Build Booking objects from materialized data (safe to do nested queries now)
        List<Booking> builtBookings = new ArrayList<>();
        for (Object[] row : uncachedRows) {
            int bookingId = (int) row[0];
            String fanUsername = (String) row[1];
            int venueId = (int) row[2];
            LocalDate gameDate = (LocalDate) row[3];
            java.time.LocalTime gameTime = (java.time.LocalTime) row[4];
            TeamNBA homeTeam = TeamNBA.robustValueOf((String) row[5]);
            TeamNBA awayTeam = TeamNBA.robustValueOf((String) row[6]);
            BookingStatus status = BookingStatus.valueOf((String) row[7]);
            boolean notified = (boolean) row[8];

            if (homeTeam == null || awayTeam == null) throw new DAOException("Invalid team in DB for booking " + bookingId);

            String key = "Booking:" + bookingId;
            Booking booking;
            if (DaoLoadingContext.isLoading(key)) {
                booking = new Booking.Builder(bookingId, gameDate, gameTime, homeTeam, awayTeam, null, null)
                        .status(status).notified(notified).build();
            } else {
                DaoLoadingContext.startLoading(key);
                try {
                    BookingDaoHelper.BookingDependencies deps = BookingDaoHelper.loadDependencies(fanUsername, venueId);
                    booking = new Booking.Builder(bookingId, gameDate, gameTime, homeTeam, awayTeam, deps.venue(), deps.fan())
                            .status(status).notified(notified).build();
                } finally {
                    DaoLoadingContext.finishLoading(key);
                }
            }
            putInCache(booking, bookingId);
            builtBookings.add(booking);
        }

        // Phase 3: Merge cached and built bookings in original order
        for (int i = 0; i < bookings.size(); i++) {
            if (insertionOrder.get(i) >= 0) {
                bookings.set(i, builtBookings.get(insertionOrder.get(i)));
            }
        }
        return bookings;
    }
}