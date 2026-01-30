package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.GlobalCache;
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
 * MySQL implementation of {@link BookingDao}.
 *
 * <p>Uses {@link GlobalCache} for caching and {@link DaoLoadingContext} to prevent
 * circular dependency loops when loading related entities (Fan, Venue).
 * Supports identity preservation during cross-persistence sync.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class BookingDaoMySql extends AbstractMySqlDao implements BookingDao {

    // ========== SQL Queries ==========
    private static final String SQL_INSERT_BOOKING_WITH_ID =
            "INSERT INTO bookings (id, game_date, game_time, home_team, away_team, venue_id, fan_username, status, notified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

    private static final String BOOKING = "Booking";
    private static final String ERR_NULL_DATE = "Date cannot be null";
    private static final String ERR_NULL_STATUS = "Status cannot be null";
    private static final String ERR_BOOKING_NOT_FOUND = "Booking not found";

    @Override
    public void saveBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_BOOKING_WITH_ID,
                    Statement.RETURN_GENERATED_KEYS)) {

                if (booking.getId() > 0) {
                    stmt.setInt(1, booking.getId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }

                stmt.setDate(2, java.sql.Date.valueOf(booking.getGameDate()));
                stmt.setTime(3, java.sql.Time.valueOf(booking.getGameTime()));
                stmt.setString(4, booking.getHomeTeam().name());
                stmt.setString(5, booking.getAwayTeam().name());
                stmt.setInt(6, booking.getVenue().getId());
                stmt.setString(7, booking.getFan().getUsername());
                stmt.setString(8, booking.getStatus().name());
                stmt.setBoolean(9, booking.isNotified());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    int newId = booking.getId();

                    if (newId == 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                newId = generatedKeys.getInt(1);
                            } else {
                                conn.rollback();
                                throw new DAOException("Creating booking failed, no ID obtained.");
                            }
                        }
                    }

                    Booking savedBooking = new Booking.Builder(
                            newId,
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

                    putInCache(savedBooking, newId);
                    logger.log(Level.INFO, "Booking saved successfully with ID: {0}", newId);
                    notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(newId), savedBooking);

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

    @Override
    public Booking retrieveBooking(int bookingId) throws DAOException {
        validateIdInput(bookingId);

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

    @Override
    public List<Booking> retrieveAllBookings() throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_BOOKINGS);
                 ResultSet rs = stmt.executeQuery()) {
                return processBookingResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all bookings", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN)) {
                stmt.setString(1, fanUsername);
                try (ResultSet rs = stmt.executeQuery()) {
                    return processBookingResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings by fan", e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE)) {
                stmt.setInt(1, venueId);
                try (ResultSet rs = stmt.executeQuery()) {
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

    @Override
    public List<Booking> retrieveBookingsByVenueManager(String venueManagerUsername) throws DAOException {
        validateUsernameInput(venueManagerUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER)) {
                stmt.setString(1, venueManagerUsername);
                try (ResultSet rs = stmt.executeQuery()) {
                    return processBookingResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving bookings for manager: " + venueManagerUsername, e);
        }
    }

    @Override
    public List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        validateDateInput(date);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_DATE)) {
                stmt.setDate(1, Date.valueOf(date));
                try (ResultSet rs = stmt.executeQuery()) {
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

    @Override
    public List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateUsernameInput(fanUsername);
        validateStatusInput(status);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS)) {
                stmt.setString(1, fanUsername);
                stmt.setString(2, status.name());
                try (ResultSet rs = stmt.executeQuery()) {
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

    @Override
    public List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNNOTIFIED_BOOKINGS)) {
                stmt.setString(1, fanUsername);
                try (ResultSet rs = stmt.executeQuery()) {
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

    @Override
    public void updateBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKING)) {
                stmt.setDate(1, java.sql.Date.valueOf(booking.getGameDate()));
                stmt.setTime(2, java.sql.Time.valueOf(booking.getGameTime()));
                stmt.setString(3, booking.getHomeTeam().name());
                stmt.setString(4, booking.getAwayTeam().name());
                stmt.setInt(5, booking.getVenueId());
                stmt.setString(6, booking.getStatus().name());
                stmt.setBoolean(7, booking.isNotified());
                stmt.setInt(8, booking.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(booking, booking.getId());

                    logger.log(Level.INFO, "Booking updated successfully: {0}", booking.getId());
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

    @Override
    public void deleteBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        validateIdInput(booking.getId());

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BOOKING)) {
                stmt.setInt(1, booking.getId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    removeFromCache(Booking.class, booking.getId());

                    logger.log(Level.INFO, "Booking deleted successfully: {0}", booking.getId());
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

    @Override
    public boolean bookingExists(int bookingId) throws DAOException {
        validateIdInput(bookingId);
        return existsById(conn -> conn.prepareStatement(SQL_CHECK_BOOKING_EXISTS), bookingId);
    }

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

    // ========== PRIVATE HELPERS ==========

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
     * Maps a ResultSet row to a Booking entity.
     * Uses {@link DaoLoadingContext} to prevent circular dependencies.
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException, DAOException {
        int bookingId = rs.getInt("id");
        String fanUsername = rs.getString("fan_username");
        int venueId = rs.getInt("venue_id");

        TeamNBA homeTeam = TeamNBA.robustValueOf(rs.getString("home_team"));
        TeamNBA awayTeam = TeamNBA.robustValueOf(rs.getString("away_team"));

        if (homeTeam == null) throw new DAOException("Invalid home team in DB for booking " + bookingId);
        if (awayTeam == null) throw new DAOException("Invalid away team in DB for booking " + bookingId);

        String key = "Booking:" + bookingId;
        if (DaoLoadingContext.isLoading(key)) {
            return new Booking.Builder(
                    bookingId,
                    rs.getDate("game_date").toLocalDate(),
                    rs.getTime("game_time").toLocalTime(),
                    homeTeam,
                    awayTeam,
                    null,
                    null
            )
                    .status(BookingStatus.valueOf(rs.getString("status")))
                    .notified(rs.getBoolean("notified"))
                    .build();
        }

        DaoLoadingContext.startLoading(key);
        try {
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
     * Processes a ResultSet into a list of Bookings with cache lookup.
     */
    private List<Booking> processBookingResultSet(ResultSet rs) throws SQLException, DAOException {
        List<Booking> bookings = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            Booking cached = getFromCache(Booking.class, id);

            if (cached != null) {
                bookings.add(cached);
            } else {
                Booking booking = mapResultSetToBooking(rs);
                putInCache(booking, id);
                bookings.add(booking);
            }
        }
        return bookings;
    }
}