package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.VenueType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the VenueDao interface.
 * <p>
 * This class provides data access operations for Venue entities stored in a MySQL database.
 * It handles venue CRUD operations and maintains referential integrity with the venue_managers table.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>venues table</strong>: id (PK, AUTO_INCREMENT), name, type, address, city,
 *       max_capacity, venue_manager_username (FK)</li>
 * </ul>
 * </p>
 *
 * @see VenueDao
 * @see AbstractMySqlDao
 */
public class VenueDaoMySql extends AbstractMySqlDao implements VenueDao {

    // SQL Queries
    private static final String SQL_INSERT_VENUE =
            "INSERT INTO venues (name, type, address, city, max_capacity, venue_manager_username) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_VENUE =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username " +
                    "FROM venues WHERE id = ?";

    private static final String SQL_SELECT_ALL_VENUES =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username " +
                    "FROM venues";

    private static final String SQL_SELECT_VENUES_BY_MANAGER =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username " +
                    "FROM venues WHERE venue_manager_username = ?";

    private static final String SQL_SELECT_VENUES_BY_CITY =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username " +
                    "FROM venues WHERE city = ?";

    private static final String SQL_UPDATE_VENUE =
            "UPDATE venues SET name = ?, type = ?, address = ?, city = ?, max_capacity = ? " +
                    "WHERE id = ?";

    private static final String SQL_DELETE_VENUE =
            "DELETE FROM venues WHERE id = ?";

    private static final String SQL_CHECK_VENUE_EXISTS =
            "SELECT COUNT(*) FROM venues WHERE id = ?";

    private static final String SQL_GET_MAX_ID =
            "SELECT COALESCE(MAX(id), 0) FROM venues";

    // Error messages
    private static final String ERR_NULL_VENUE_BEAN = "VenueBean cannot be null";
    private static final String ERR_NULL_CITY = "City cannot be null or empty";
    private static final String ERR_VENUE_NOT_FOUND = "Venue not found";

    /**
     * {@inheritDoc}
     * <p>
     * The venue ID from the bean is used if provided (for CSV sync), otherwise
     * MySQL AUTO_INCREMENT generates it. After insertion, observers are notified.
     * </p>
     */
    @Override
    public void saveVenue(VenueBean venueBean) throws DAOException {
        validateVenueBeanInput(venueBean);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venueBean.getName());
            stmt.setString(2, venueBean.getType().name());
            stmt.setString(3, venueBean.getAddress());
            stmt.setString(4, venueBean.getCity());
            stmt.setInt(5, venueBean.getMaxCapacity());
            stmt.setString(6, venueBean.getVenueManagerUsername());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Get generated ID if not provided
                if (venueBean.getId() == 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            venueBean.setId(generatedKeys.getInt(1));
                        }
                    }
                }

                logger.log(Level.INFO, "Venue saved successfully: {0} (ID: {1})",
                        new Object[]{venueBean.getName(), venueBean.getId()});
                notifyObservers(DaoOperation.INSERT, "Venue", String.valueOf(venueBean.getId()), venueBean);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue save", e);
            throw new DAOException("Error saving venue", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves venue data and reconstructs the associated VenueManager object.
     * The venue's bookings map is empty - bookings should be loaded separately when needed.
     * </p>
     */
    @Override
    public Venue retrieveVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE)) {

            stmt.setInt(1, venueId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVenue(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue retrieval", e);
            throw new DAOException("Error retrieving venue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveAllVenues() throws DAOException {
        List<Venue> venues = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUES);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                venues.add(mapResultSetToVenue(rs));
            }

            logger.log(Level.INFO, "Retrieved {0} venues", venues.size());
            return venues;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venues retrieval", e);
            throw new DAOException("Error retrieving all venues", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveVenuesByManager(String venueManagerUsername) throws DAOException {
        validateUsernameInput(venueManagerUsername);

        List<Venue> venues = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_MANAGER)) {

            stmt.setString(1, venueManagerUsername);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    venues.add(mapResultSetToVenue(rs));
                }
            }

            logger.log(Level.INFO, "Retrieved {0} venues for manager {1}",
                    new Object[]{venues.size(), venueManagerUsername});
            return venues;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venues retrieval by manager", e);
            throw new DAOException("Error retrieving venues by manager", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        validateCityInput(city);

        List<Venue> venues = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_CITY)) {

            stmt.setString(1, city);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    venues.add(mapResultSetToVenue(rs));
                }
            }

            logger.log(Level.INFO, "Retrieved {0} venues in city {1}",
                    new Object[]{venues.size(), city});
            return venues;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venues retrieval by city", e);
            throw new DAOException("Error retrieving venues by city", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This implementation does NOT update the venue_manager_username field
     * as manager transfer should be a separate operation with proper validation.
     * </p>
     */
    @Override
    public void updateVenue(VenueBean venueBean) throws DAOException {
        validateVenueBeanInput(venueBean);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE)) {

            stmt.setString(1, venueBean.getName());
            stmt.setString(2, venueBean.getType().name());
            stmt.setString(3, venueBean.getAddress());
            stmt.setString(4, venueBean.getCity());
            stmt.setInt(5, venueBean.getMaxCapacity());
            stmt.setInt(6, venueBean.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.log(Level.INFO, "Venue updated successfully: {0}", venueBean.getId());
                notifyObservers(DaoOperation.UPDATE, "Venue", String.valueOf(venueBean.getId()), venueBean);
            } else {
                logger.log(Level.WARNING, "Venue not found for update: {0}", venueBean.getId());
                throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueBean.getId());
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue update", e);
            throw new DAOException("Error updating venue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE)) {

            stmt.setInt(1, venueId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.log(Level.INFO, "Venue deleted successfully: {0}", venueId);
                notifyObservers(DaoOperation.DELETE, "Venue", String.valueOf(venueId), null);
            } else {
                logger.log(Level.WARNING, "Venue not found for deletion: {0}", venueId);
                throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueId);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue deletion", e);
            throw new DAOException("Error deleting venue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean venueExists(int venueId) throws DAOException {
        validateIdInput(venueId);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_VENUE_EXISTS)) {

            stmt.setInt(1, venueId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue existence check", e);
            throw new DAOException("Error checking venue existence", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * In MySQL, this returns the current MAX(id) + 1. Note that if using AUTO_INCREMENT,
     * this value may differ from the actual next ID if there are concurrent insertions.
     * </p>
     */
    @Override
    public int getNextVenueId() throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_MAX_ID);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
            return 1;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during next venue ID retrieval", e);
            throw new DAOException("Error getting next venue ID", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a ResultSet row to a Venue domain object.
     * <p>
     * This method reconstructs the VenueManager reference by querying the VenueManagerDao.
     * </p>
     */
    private Venue mapResultSetToVenue(ResultSet rs) throws SQLException, DAOException {
        String managerUsername = rs.getString("venue_manager_username");

        // Retrieve the full VenueManager object
        VenueManagerDaoMySql venueManagerDao = new VenueManagerDaoMySql();
        VenueManager venueManager = venueManagerDao.retrieveVenueManager(managerUsername);

        if (venueManager == null) {
            logger.log(Level.SEVERE, "VenueManager not found for venue mapping: {0}", managerUsername);
            throw new DAOException("VenueManager not found: " + managerUsername);
        }

        return new Venue.Builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .type(VenueType.valueOf(rs.getString("type")))
                .address(rs.getString("address"))
                .city(rs.getString("city"))
                .maxCapacity(rs.getInt("max_capacity"))
                .venueManager(venueManager)
                .build();
    }

    // ========== VALIDATION METHODS ==========

    private void validateVenueBeanInput(VenueBean venueBean) {
        if (venueBean == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE_BEAN);
        }
    }

    private void validateCityInput(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_CITY);
        }
    }
}