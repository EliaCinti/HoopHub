package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.helper_dao.VenueDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.enums.VenueType;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link VenueDao}.
 *
 * <p>Uses UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) for cross-persistence
 * synchronization to handle ID conflicts gracefully.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class VenueDaoMySql extends AbstractMySqlDao implements VenueDao {

    // UPSERT query for sync
    private static final String SQL_UPSERT =
            "INSERT INTO venues (id, name, type, address, city, max_capacity, venue_manager_username) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), type = VALUES(type), address = VALUES(address), " +
                    "city = VALUES(city), max_capacity = VALUES(max_capacity), venue_manager_username = VALUES(venue_manager_username)";

    // Standard INSERT for auto-generated ID
    private static final String SQL_INSERT_AUTO_ID =
            "INSERT INTO venues (name, type, address, city, max_capacity, venue_manager_username) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_VENUE =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username FROM venues WHERE id = ?";

    private static final String SQL_SELECT_ALL_VENUES =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username FROM venues";

    private static final String SQL_SELECT_VENUES_BY_MANAGER =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username FROM venues WHERE venue_manager_username = ?";

    private static final String SQL_SELECT_VENUES_BY_CITY =
            "SELECT id, name, type, address, city, max_capacity, venue_manager_username FROM venues WHERE city = ?";

    private static final String SQL_UPDATE_VENUE =
            "UPDATE venues SET name = ?, type = ?, address = ?, city = ?, max_capacity = ? WHERE id = ?";

    private static final String SQL_DELETE_VENUE = "DELETE FROM venues WHERE id = ?";
    private static final String SQL_CHECK_VENUE_EXISTS = "SELECT COUNT(*) FROM venues WHERE id = ?";
    private static final String SQL_GET_MAX_ID = "SELECT COALESCE(MAX(id), 0) FROM venues";

    private static final String SQL_DELETE_VENUE_TEAM = "DELETE FROM venue_teams WHERE venue_id = ? AND team_name = ?";
    private static final String SQL_SELECT_VENUE_TEAMS = "SELECT team_name FROM venue_teams WHERE venue_id = ?";
    private static final String SQL_DELETE_ALL_VENUE_TEAMS = "DELETE FROM venue_teams WHERE venue_id = ?";

    // UPSERT for venue_teams (IGNORE duplicate entries)
    private static final String SQL_UPSERT_VENUE_TEAM =
            "INSERT IGNORE INTO venue_teams (venue_id, team_name) VALUES (?, ?)";

    private static final String VENUE = "Venue";

    @Override
    public Venue saveVenue(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            int newId = venue.getId() > 0 ? saveWithUpsert(conn, venue) : saveWithAutoId(conn, venue);

            // Save teams (using UPSERT to avoid duplicates)
            saveVenueTeamsInternal(conn, newId, venue.getAssociatedTeams());

            Venue savedVenue = new Venue.Builder()
                    .id(newId)
                    .name(venue.getName())
                    .type(venue.getType())
                    .address(venue.getAddress())
                    .city(venue.getCity())
                    .maxCapacity(venue.getMaxCapacity())
                    .venueManager(venue.getVenueManager())
                    .teams(venue.getAssociatedTeams())
                    .build();

            conn.commit();
            putInCache(savedVenue, newId);
            logger.log(Level.INFO, "Venue saved with ID: {0}", newId);
            notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(newId), savedVenue);

            return savedVenue;

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    private int saveWithUpsert(Connection conn, Venue venue) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setInt(1, venue.getId());
            stmt.setString(2, venue.getName());
            stmt.setString(3, venue.getType().name());
            stmt.setString(4, venue.getAddress());
            stmt.setString(5, venue.getCity());
            stmt.setInt(6, venue.getMaxCapacity());
            stmt.setString(7, venue.getVenueManagerUsername());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new DAOException("Creating venue failed, no rows affected.");
            if (affectedRows == 2) {
                logger.log(Level.FINE, "Venue ID {0} updated via UPSERT", venue.getId());
                // Clear existing teams for update case
                deleteAllVenueTeamsInternal(conn, venue.getId());
            }
            return venue.getId();
        }
    }

    private int saveWithAutoId(Connection conn, Venue venue) throws SQLException, DAOException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_AUTO_ID, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, venue.getName());
            stmt.setString(2, venue.getType().name());
            stmt.setString(3, venue.getAddress());
            stmt.setString(4, venue.getCity());
            stmt.setInt(5, venue.getMaxCapacity());
            stmt.setString(6, venue.getVenueManagerUsername());

            if (stmt.executeUpdate() == 0) throw new DAOException("Creating venue failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
                throw new DAOException("Creating venue failed, no ID obtained.");
            }
        }
    }

    private void saveVenueTeamsInternal(Connection conn, int venueId, Set<TeamNBA> teams) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT_VENUE_TEAM)) {
            stmt.setInt(1, venueId);
            for (TeamNBA team : teams) {
                stmt.setString(2, team.name());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void deleteAllVenueTeamsInternal(Connection conn, int venueId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_VENUE_TEAMS)) {
            stmt.setInt(1, venueId);
            stmt.executeUpdate();
        }
    }

    @Override
    public Venue retrieveVenue(int venueId) throws DAOException {
        validateIdInput(venueId);
        Venue cached = getFromCache(Venue.class, venueId);
        if (cached != null) return cached;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE)) {
            stmt.setInt(1, venueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Venue venue = mapResultSetToVenue(rs);
                    putInCache(venue, venueId);
                    return venue;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venue", e);
        }
    }

    @Override
    public List<Venue> retrieveAllVenues() throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUES);
             ResultSet rs = stmt.executeQuery()) {
            return processVenueResultSet(rs);
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all venues", e);
        }
    }

    @Override
    public List<Venue> retrieveVenuesByManager(String managerUsername) throws DAOException {
        validateUsernameInput(managerUsername);
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_MANAGER)) {
            stmt.setString(1, managerUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                return processVenueResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venues by manager", e);
        }
    }

    @Override
    public List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        if (city == null || city.trim().isEmpty()) throw new IllegalArgumentException("City cannot be null or empty");
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUES_BY_CITY)) {
            stmt.setString(1, city);
            try (ResultSet rs = stmt.executeQuery()) {
                return processVenueResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venues by city", e);
        }
    }

    @Override
    public void updateVenue(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE)) {
                stmt.setString(1, venue.getName());
                stmt.setString(2, venue.getType().name());
                stmt.setString(3, venue.getAddress());
                stmt.setString(4, venue.getCity());
                stmt.setInt(5, venue.getMaxCapacity());
                stmt.setInt(6, venue.getId());

                if (stmt.executeUpdate() > 0) {
                    // Update teams
                    deleteAllVenueTeamsInternal(conn, venue.getId());
                    saveVenueTeamsInternal(conn, venue.getId(), venue.getAssociatedTeams());

                    conn.commit();
                    putInCache(venue, venue.getId());
                    logger.log(Level.INFO, "Venue updated: {0}", venue.getId());
                    notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venue.getId()), venue);
                } else {
                    conn.rollback();
                    throw new DAOException("Venue not found: " + venue.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void deleteVenue(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");
        int venueId = venue.getId();

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Delete teams first
            deleteAllVenueTeamsInternal(conn, venueId);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE)) {
                stmt.setInt(1, venueId);
                if (stmt.executeUpdate() > 0) {
                    conn.commit();
                    removeFromCache(Venue.class, venueId);
                    logger.log(Level.INFO, "Venue deleted: {0}", venueId);
                    notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(venueId), null);
                } else {
                    conn.rollback();
                    throw new DAOException("Venue not found: " + venueId);
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public boolean venueExists(int venueId) throws DAOException {
        validateIdInput(venueId);
        return existsById(conn -> conn.prepareStatement(SQL_CHECK_VENUE_EXISTS), venueId);
    }

    @Override
    public int getNextVenueId() throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_MAX_ID);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) + 1 : 1;
        } catch (SQLException e) {
            throw new DAOException("Error getting next venue ID", e);
        }
    }

    // ========== TEAM MANAGEMENT ==========

    @Override
    public Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        validateIdInput(venueId);
        Set<TeamNBA> teams = new HashSet<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE_TEAMS)) {
            stmt.setInt(1, venueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TeamNBA team = TeamNBA.robustValueOf(rs.getString("team_name"));
                    if (team != null) teams.add(team);
                }
            }
            return teams;
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venue teams", e);
        }
    }

    @Override
    public void saveVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");
        if (team == null) throw new IllegalArgumentException("Team cannot be null");

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT_VENUE_TEAM)) {
            stmt.setInt(1, venue.getId());
            stmt.setString(2, team.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Error saving venue team association", e);
        }
    }

    @Override
    public void deleteVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");
        if (team == null) throw new IllegalArgumentException("Team cannot be null");

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE_TEAM)) {
            stmt.setInt(1, venue.getId());
            stmt.setString(2, team.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Error deleting venue team association", e);
        }
    }

    @Override
    public void deleteAllVenueTeams(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");

        try (Connection conn = ConnectionFactory.getConnection()) {
            deleteAllVenueTeamsInternal(conn, venue.getId());
        } catch (SQLException e) {
            throw new DAOException("Error deleting all venue teams", e);
        }
    }

    // ========== PRIVATE HELPERS ==========

    private Venue mapResultSetToVenue(ResultSet rs) throws SQLException, DAOException {
        // Materialize ALL columns from the ResultSet BEFORE any nested query.
        // This prevents "ResultSet closed" errors caused by single-connection
        // reuse: retrieveVenueTeams() and loadVenueManager() open new statements
        // on the shared connection, which invalidates the current ResultSet.
        int venueId = rs.getInt("id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        String address = rs.getString("address");
        String city = rs.getString("city");
        int maxCapacity = rs.getInt("max_capacity");
        String managerUsername = rs.getString("venue_manager_username");

        String venueKey = "Venue:" + venueId;

        if (DaoLoadingContext.isLoading(venueKey)) {
            VenueManager vm = DaoFactoryFacade.getInstance().getVenueManagerDao().retrieveVenueManager(managerUsername);
            return buildVenueFromMaterialized(venueId, name, type, address, city, maxCapacity, vm);
        }

        DaoLoadingContext.startLoading(venueKey);
        try {
            VenueManager venueManager = VenueDaoHelper.loadVenueManager(managerUsername);
            return buildVenueFromMaterialized(venueId, name, type, address, city, maxCapacity, venueManager);
        } finally {
            DaoLoadingContext.finishLoading(venueKey);
        }
    }

    private Venue buildVenueFromMaterialized(int venueId, String name, String type, String address,
                                             String city, int maxCapacity, VenueManager venueManager) throws DAOException {
        Set<TeamNBA> teams = retrieveVenueTeams(venueId);

        return new Venue.Builder()
                .id(venueId)
                .name(name)
                .type(VenueType.valueOf(type))
                .address(address)
                .city(city)
                .maxCapacity(maxCapacity)
                .venueManager(venueManager)
                .teams(teams)
                .build();
    }

    private List<Venue> processVenueResultSet(ResultSet rs) throws SQLException, DAOException {
        // Phase 1: Materialize ALL rows from the ResultSet into raw data.
        // This must complete before ANY nested query (teams, manager) because
        // those queries reuse the shared connection and invalidate this ResultSet.
        List<int[]> uncachedIds = new ArrayList<>();
        List<String[]> uncachedData = new ArrayList<>();
        List<Venue> venues = new ArrayList<>();
        List<Integer> insertionOrder = new ArrayList<>();  // tracks order: -1 = uncached slot, >=0 = cached index

        int uncachedIndex = 0;
        while (rs.next()) {
            int id = rs.getInt("id");
            Venue cached = getFromCache(Venue.class, id);
            if (cached != null) {
                venues.add(cached);
                insertionOrder.add(-1);
            } else {
                // Materialize row data immediately
                uncachedIds.add(new int[]{ id, rs.getInt("max_capacity") });
                uncachedData.add(new String[]{
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getString("venue_manager_username")
                });
                venues.add(null);  // placeholder
                insertionOrder.add(uncachedIndex++);
            }
        }

        // Phase 2: Build Venue objects from materialized data (safe to do nested queries now)
        List<Venue> builtVenues = new ArrayList<>();
        for (int i = 0; i < uncachedIds.size(); i++) {
            int venueId = uncachedIds.get(i)[0];
            int maxCapacity = uncachedIds.get(i)[1];
            String[] data = uncachedData.get(i);

            String venueKey = "Venue:" + venueId;
            VenueManager venueManager;
            if (DaoLoadingContext.isLoading(venueKey)) {
                venueManager = DaoFactoryFacade.getInstance().getVenueManagerDao().retrieveVenueManager(data[4]);
            } else {
                DaoLoadingContext.startLoading(venueKey);
                try {
                    venueManager = VenueDaoHelper.loadVenueManager(data[4]);
                } finally {
                    DaoLoadingContext.finishLoading(venueKey);
                }
            }

            Venue venue = buildVenueFromMaterialized(venueId, data[0], data[1], data[2], data[3], maxCapacity, venueManager);
            putInCache(venue, venueId);
            builtVenues.add(venue);
        }

        // Phase 3: Merge cached and built venues in original order
        for (int i = 0; i < venues.size(); i++) {
            if (insertionOrder.get(i) >= 0) {
                venues.set(i, builtVenues.get(insertionOrder.get(i)));
            }
        }
        return venues;
    }
}