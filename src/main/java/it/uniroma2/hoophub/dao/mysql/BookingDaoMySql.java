package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.BookingBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.model.BookingStatus;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the BookingDao interface.
 * <p>
 * This class provides comprehensive data access operations for Booking entities stored in a MySQL database.
 * It handles complex relationships with Fan and Venue entities, providing various query methods
 * for filtering bookings by different criteria.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>bookings table</strong>: id (PK, AUTO_INCREMENT), game_date, game_time,
 *       home_team, away_team, venue_id (FK), fan_username (FK), status, notified</li>
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
    private static final String ERR_NULL_BOOKING_BEAN = "BookingBean cannot be null";
    private static final String ERR_NULL_DATE = "Date cannot be null";
    private static final String ERR_NULL_STATUS = "Status cannot be null";
    private static final String ERR_BOOKING_NOT_FOUND = "Booking not found";

    /**
     * {@inheritDoc}
     * <p>
     * Extracts fan username and venue ID from the BookingBean to save the relationship.
     * After successful insertion, observers are notified for cross-persistence sync.
     * </p>
     */
    @Override
    public void saveBooking(BookingBean bookingBean) throws DAOException {
        validateBookingBeanInput(bookingBean);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_BOOKING,
                         Statement.RETURN_GENERATED_KEYS)) {

                stmt.setDate(1, Date.valueOf(bookingBean.getGameDate()));
                stmt.setTime(2, Time.valueOf(bookingBean.getGameTime()));
                stmt.setString(3, bookingBean.getHomeTeam().name());
                stmt.setString(4, bookingBean.getAwayTeam().name());
                stmt.setInt(5, bookingBean.getVenueId());
                stmt.setString(6, bookingBean.getFanUsername());
                stmt.setString(7, bookingBean.getStatus().name());
                stmt.setBoolean(8, bookingBean.isNotified());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Booking saved successfully: {0}", bookingBean.getId());
                    notifyObservers(DaoOperation.INSERT, BOOKING, String.valueOf(bookingBean.getId()), bookingBean);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during booking save", e);
            throw new DAOException("Error saving booking", e);
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKING)) {

                stmt.setInt(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToBooking(rs);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during booking retrieval", e);
            throw new DAOException("Error retrieving booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveAllBookings() throws DAOException {
        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_BOOKINGS);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    bookings.add(mapResultSetToBooking(rs));
                }

                logger.log(Level.INFO, "Retrieved {0} bookings", bookings.size());
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval", e);
            throw new DAOException("Error retrieving all bookings", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByFan(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN)) {

                stmt.setString(1, fanUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval by fan", e);
            throw new DAOException("Error retrieving bookings by fan", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE)) {

                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} bookings for venue {1}",
                        new Object[]{bookings.size(), venueId});
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval by venue", e);
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

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_VENUE_MANAGER)) {

                stmt.setString(1, venueManagerUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} bookings for venue manager {1}",
                        new Object[]{bookings.size(), venueManagerUsername});
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval by venue manager", e);
            throw new DAOException("Error retrieving bookings by venue manager", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        validateDateInput(date);

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_DATE)) {

                stmt.setDate(1, Date.valueOf(date));

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} bookings for date {1}",
                        new Object[]{bookings.size(), date});
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval by date", e);
            throw new DAOException("Error retrieving bookings by date", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveBookingsByStatus(String fanUsername, BookingStatus status) throws DAOException {
        validateUsernameInput(fanUsername);
        validateStatusInput(status);

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BOOKINGS_BY_FAN_AND_STATUS)) {

                stmt.setString(1, fanUsername);
                stmt.setString(2, status.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} {1} bookings for fan {2}",
                        new Object[]{bookings.size(), status, fanUsername});
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during bookings retrieval by status", e);
            throw new DAOException("Error retrieving bookings by status", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Booking> retrieveUnnotifiedBookings(String fanUsername) throws DAOException {
        validateUsernameInput(fanUsername);

        List<Booking> bookings = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_UNNOTIFIED_BOOKINGS)) {

                stmt.setString(1, fanUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bookings.add(mapResultSetToBooking(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} unnotified bookings for fan {1}",
                        new Object[]{bookings.size(), fanUsername});
                return bookings;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during unnotified bookings retrieval", e);
            throw new DAOException("Error retrieving unnotified bookings", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBooking(BookingBean bookingBean) throws DAOException {
        validateBookingBeanInput(bookingBean);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BOOKING)) {

                stmt.setDate(1, Date.valueOf(bookingBean.getGameDate()));
                stmt.setTime(2, Time.valueOf(bookingBean.getGameTime()));
                stmt.setString(3, bookingBean.getHomeTeam().name());
                stmt.setString(4, bookingBean.getAwayTeam().name());
                stmt.setInt(5, bookingBean.getVenueId());
                stmt.setString(6, bookingBean.getStatus().name());
                stmt.setBoolean(7, bookingBean.isNotified());
                stmt.setInt(8, bookingBean.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Booking updated successfully: {0}", bookingBean.getId());
                    notifyObservers(DaoOperation.UPDATE, BOOKING, String.valueOf(bookingBean.getId()), bookingBean);
                } else {
                    throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + bookingBean.getId());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during booking update", e);
            throw new DAOException("Error updating booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBooking(int bookingId) throws DAOException {
        validateIdInput(bookingId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BOOKING)) {

                stmt.setInt(1, bookingId);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Booking deleted successfully: {0}", bookingId);
                    notifyObservers(DaoOperation.DELETE, BOOKING, String.valueOf(bookingId), null);
                } else {
                    throw new DAOException(ERR_BOOKING_NOT_FOUND + ": " + bookingId);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during booking deletion", e);
            throw new DAOException("Error deleting booking", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean bookingExists(int bookingId) throws DAOException {
        validateIdInput(bookingId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_BOOKING_EXISTS)) {

                stmt.setInt(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during booking existence check", e);
            throw new DAOException("Error checking booking existence", e);
        }
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
            logger.log(Level.SEVERE, "Database error during next booking ID retrieval", e);
            throw new DAOException("Error getting next booking ID", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a ResultSet row to a Booking domain object.
     * <p>
     * This method reconstructs Fan and Venue references by querying their respective DAOs.
     * Includes anti-loop protection via DaoLoadingContext to prevent circular dependencies.
     * </p>
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException, DAOException {
        int bookingId = rs.getInt("id");
        String fanUsername = rs.getString("fan_username");
        int venueId = rs.getInt("venue_id");

        String key = "Booking:" + bookingId;
        if (DaoLoadingContext.isLoading(key)) {
            // Return minimal booking object without loading relationships
            return new Booking.Builder(
                    bookingId,
                    rs.getDate("game_date").toLocalDate(),
                    rs.getTime("game_time").toLocalTime(),
                    TeamNBA.valueOf(rs.getString("home_team")),
                    TeamNBA.valueOf(rs.getString("away_team")),
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

            return new Booking.Builder(
                    bookingId,
                    rs.getDate("game_date").toLocalDate(),
                    rs.getTime("game_time").toLocalTime(),
                    TeamNBA.valueOf(rs.getString("home_team")),
                    TeamNBA.valueOf(rs.getString("away_team")),
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

    // ========== VALIDATION METHODS ==========

    private void validateBookingBeanInput(BookingBean bookingBean) {
        if (bookingBean == null) {
            throw new IllegalArgumentException(ERR_NULL_BOOKING_BEAN);
        }
    }

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
}