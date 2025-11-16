package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.model.VenueType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * MySQL implementation of VenueDao.
 * <p>
 * Manages Venue data across venues and venue_teams tables.
 * </p>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Factory</strong>: Created via VenueDaoFactory</li>
 *   <li><strong>Facade</strong>: Uses DaoFactoryFacade to access VenueManagerDao</li>
 *   <li><strong>Observer</strong>: Notifies observers for CSV-MySQL sync</li>
 *   <li><strong>Builder</strong>: Uses Venue.Builder for object construction</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Circular Dependency:</strong> Uses {@link DaoLoadingContext} to prevent infinite loops.
 * </p>
 *
 * @see VenueDao
 * @see DaoLoadingContext
 */
public class VenueDaoMySql extends AbstractMySqlDao implements VenueDao {

    // ========== SQL Queries ==========
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

    private static final String SQL_INSERT_VENUE_TEAM =
            "INSERT INTO venue_teams (venue_id, team_name) VALUES (?, ?)";

    private static final String SQL_DELETE_VENUE_TEAM =
            "DELETE FROM venue_teams WHERE venue_id = ? AND team_name = ?";

    private static final String SQL_SELECT_VENUE_TEAMS =
            "SELECT team_name FROM venue_teams WHERE venue_id = ?";

    private static final String SQL_DELETE_ALL_VENUE_TEAMS =
            "DELETE FROM venue_teams WHERE venue_id = ?";

    // ========== Constants ==========
    private static final String VENUE = "Venue";

    // ========== Error messages ==========
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE,
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
                    notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(venueBean.getId()), venueBean);
                }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE)) {

                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToVenue(rs);
                    }
                    return null;
                }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUES);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    venues.add(mapResultSetToVenue(rs));
                }

                return venues;
            }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_MANAGER)) {

                stmt.setString(1, venueManagerUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        venues.add(mapResultSetToVenue(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} venues for manager {1}",
                        new Object[]{venues.size(), venueManagerUsername});
                return venues;
            }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_CITY)) {

                stmt.setString(1, city);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        venues.add(mapResultSetToVenue(rs));
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} venues in city {1}",
                        new Object[]{venues.size(), city});
                return venues;
            }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE)) {

                stmt.setString(1, venueBean.getName());
                stmt.setString(2, venueBean.getType().name());
                stmt.setString(3, venueBean.getAddress());
                stmt.setString(4, venueBean.getCity());
                stmt.setInt(5, venueBean.getMaxCapacity());
                stmt.setInt(6, venueBean.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Venue updated successfully: {0}", venueBean.getId());
                    notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venueBean.getId()), venueBean);
                } else {
                    logger.log(Level.WARNING, "Venue not found for update: {0}", venueBean.getId());
                    throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueBean.getId());
                }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE)) {

                stmt.setInt(1, venueId);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Venue deleted successfully: {0}", venueId);
                    notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(venueId), null);
                } else {
                    logger.log(Level.WARNING, "Venue not found for deletion: {0}", venueId);
                    throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueId);
                }
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

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_VENUE_EXISTS)) {

                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                    return false;
                }
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
            logger.log(Level.SEVERE, "Database error during next venue ID retrieval", e);
            throw new DAOException("Error getting next venue ID", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps ResultSet to Venue.
     * <p>
     * Uses {@link DaoLoadingContext} to prevent circular loops:
     * If already loading → return Venue with VenueManager (break cycle).
     * Otherwise → load complete VenueManager via VenueManagerDao (Facade pattern).
     * </p>
     */
    private Venue mapResultSetToVenue(ResultSet rs) throws SQLException, DAOException {
        String managerUsername = rs.getString("venue_manager_username");
        int venueId = rs.getInt("id");
        String venueKey = "Venue:" + venueId;

        // Check if we're in a circular loading situation
        if (DaoLoadingContext.isLoading(venueKey)) {
            // Break the cycle by loading VenueManager without its managed venues
            DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
            VenueManagerDao venueManagerDao = daoFactory.getVenueManagerDao();
            VenueManager venueManager = venueManagerDao.retrieveVenueManager(managerUsername);

            if (venueManager == null) {
                throw new DAOException("VenueManager not found: " + managerUsername);
            }

            Venue venue = new Venue.Builder()
                    .id(venueId)
                    .name(rs.getString("name"))
                    .type(VenueType.valueOf(rs.getString("type")))
                    .address(rs.getString("address"))
                    .city(rs.getString("city"))
                    .maxCapacity(rs.getInt("max_capacity"))
                    .venueManager(venueManager)
                    .build();

            Set<TeamNBA> teams = retrieveVenueTeams(venueId);
            for (TeamNBA team : teams) {
                venue.addTeam(team);
            }

            return venue;
        }

        // Mark this venue as being loaded
        DaoLoadingContext.startLoading(venueKey);
        try {
            // Retrieve the full VenueManager object using DaoFactoryFacade (Factory pattern)
            DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
            VenueManagerDao venueManagerDao = daoFactory.getVenueManagerDao();
            VenueManager venueManager = venueManagerDao.retrieveVenueManager(managerUsername);

            if (venueManager == null) {
                logger.log(Level.SEVERE, "VenueManager not found for venue mapping: {0}", managerUsername);
                throw new DAOException("VenueManager not found: " + managerUsername);
            }

            // Build the venue with COMPLETE manager
            Venue venue = new Venue.Builder()
                    .id(venueId)
                    .name(rs.getString("name"))
                    .type(VenueType.valueOf(rs.getString("type")))
                    .address(rs.getString("address"))
                    .city(rs.getString("city"))
                    .maxCapacity(rs.getInt("max_capacity"))
                    .venueManager(venueManager)
                    .build();

            // Load associated teams
            Set<TeamNBA> teams = retrieveVenueTeams(venueId);
            for (TeamNBA team : teams) {
                venue.addTeam(team);
            }

            return venue;
        } finally {
            // Always clean up the loading context
            DaoLoadingContext.finishLoading(venueKey);
        }
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

    // ========== TEAM MANAGEMENT METHODS ==========

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveVenueTeam(int venueId, TeamNBA team) throws DAOException {
        validateIdInput(venueId);
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE_TEAM)) {
                stmt.setInt(1, venueId);
                stmt.setString(2, team.name());

                stmt.executeUpdate();
                logger.log(Level.INFO, "Team {0} associated with venue {1}",
                        new Object[]{team.getDisplayName(), venueId});
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue team save", e);
            throw new DAOException("Error saving venue team association", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVenueTeam(int venueId, TeamNBA team) throws DAOException {
        validateIdInput(venueId);
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE_TEAM)) {
                stmt.setInt(1, venueId);
                stmt.setString(2, team.name());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    logger.log(Level.INFO, "Team {0} removed from venue {1}",
                            new Object[]{team.getDisplayName(), venueId});
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue team deletion", e);
            throw new DAOException("Error deleting venue team association", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        validateIdInput(venueId);
        Set<TeamNBA> teams = new HashSet<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE_TEAMS)) {
                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String teamName = rs.getString("team_name");
                        // Try multiple formats: display name, abbreviation, enum constant
                        TeamNBA team = TeamNBA.fromDisplayName(teamName);
                        if (team == null) {
                            team = TeamNBA.fromAbbreviation(teamName);
                        }
                        if (team == null) {
                            // Try enum constant name as last resort: "GOLDEN_STATE_WARRIORS"
                            try {
                                team = TeamNBA.valueOf(teamName);
                            } catch (IllegalArgumentException ignored) {
                                // Not a valid enum constant
                            }
                        }
                        if (team != null) {
                            teams.add(team);
                        } else {
                            logger.log(Level.WARNING, "Invalid team name in database for venue {0}: {1}",
                                    new Object[]{venueId, teamName});
                        }
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} teams for venue {1}",
                        new Object[]{teams.size(), venueId});
                return teams;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue teams retrieval", e);
            throw new DAOException("Error retrieving venue teams", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllVenueTeams(int venueId) throws DAOException {
        validateIdInput(venueId);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_VENUE_TEAMS)) {
                stmt.setInt(1, venueId);

                int affectedRows = stmt.executeUpdate();
                logger.log(Level.INFO, "Deleted {0} team associations for venue {1}",
                        new Object[]{affectedRows, venueId});
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during all venue teams deletion", e);
            throw new DAOException("Error deleting all venue teams", e);
        }
    }
}