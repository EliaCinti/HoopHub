package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.dao.helper_dao.VenueDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.enums.VenueType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private static final String ERR_NULL_CITY = "City cannot be null or empty";
    private static final String ERR_VENUE_NOT_FOUND = "Venue not found";

    /**
     * {@inheritDoc}
     */
    @Override
    public Venue saveVenue(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false); // Transazione unica per Venue + Teams

            // 1. INSERT VENUE
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE,
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, venue.getName());
                stmt.setString(2, venue.getType().name());
                stmt.setString(3, venue.getAddress());
                stmt.setString(4, venue.getCity());
                stmt.setInt(5, venue.getMaxCapacity());
                stmt.setString(6, venue.getVenueManagerUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);

                            // 2. INSERT TEAMS (Batch)
                            try (PreparedStatement teamStmt = conn.prepareStatement(SQL_INSERT_VENUE_TEAM)) {

                                // OTTIMIZZAZIONE: Settiamo l'ID una volta sola FUORI dal ciclo.
                                // Questo valore verrà usato per tutte le righe aggiunte al batch.
                                teamStmt.setInt(1, newId);

                                for (TeamNBA team : venue.getAssociatedTeams()) {
                                    // Nel ciclo cambiamo solo il secondo parametro (il nome della squadra)
                                    teamStmt.setString(2, team.name());
                                    teamStmt.addBatch();
                                }

                                // Eseguiamo tutto in un colpo solo
                                teamStmt.executeBatch();
                            }

                            // 3. Ricostruzione oggetto con ID
                            Venue savedVenue = new Venue.Builder()
                                    .id(newId)
                                    .name(venue.getName())
                                    .type(venue.getType())
                                    .address(venue.getAddress())
                                    .city(venue.getCity())
                                    .maxCapacity(venue.getMaxCapacity())
                                    .venueManager(venue.getVenueManager())
                                    .teams(venue.getAssociatedTeams()) // Copia i team
                                    .build();

                            conn.commit();

                            // 4. Cache & Notify
                            putInCache(savedVenue, newId);
                            logger.log(Level.INFO, "Venue saved successfully with ID: {0}", newId);
                            notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(newId), savedVenue);

                            return savedVenue;
                        } else {
                            conn.rollback();
                            throw new DAOException("Creating venue failed, no ID obtained.");
                        }
                    }
                } else {
                    conn.rollback();
                    throw new DAOException("Creating venue failed, no rows affected.");
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Venue retrieveVenue(int venueId) throws DAOException {
        validateIdInput(venueId);

        // 1. CACHE CHECK
        Venue cachedVenue = getFromCache(Venue.class, venueId);
        if (cachedVenue != null) {
            return cachedVenue;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE)) {

                stmt.setInt(1, venueId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Venue venue = mapResultSetToVenue(rs);
                        // 2. CACHE PUT
                        putInCache(venue, venueId);
                        return venue;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveAllVenues() throws DAOException {
        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUES);
                 ResultSet rs = stmt.executeQuery()) {
                return processVenueResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all venues", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveVenuesByManager(String managerUsername) throws DAOException {
        validateUsernameInput(managerUsername);
        return executeSimpleVenueQuery(SQL_SELECT_VENUES_BY_MANAGER, managerUsername, "for manager: " + managerUsername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        validateCityInput(city);
        return executeSimpleVenueQuery(SQL_SELECT_VENUES_BY_CITY, city, "by city: " + city);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateVenue(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // 1. Update Tabella venues
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE)) {
                stmt.setString(1, venue.getName());
                stmt.setString(2, venue.getType().name());
                stmt.setString(3, venue.getAddress());
                stmt.setString(4, venue.getCity());
                stmt.setInt(5, venue.getMaxCapacity());
                stmt.setInt(6, venue.getId());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // 2. Update Teams (Delete All + Insert New)
                    try (PreparedStatement delStmt = conn.prepareStatement(SQL_DELETE_ALL_VENUE_TEAMS);
                         PreparedStatement insStmt = conn.prepareStatement(SQL_INSERT_VENUE_TEAM)) {

                        delStmt.setInt(1, venue.getId());
                        delStmt.executeUpdate();

                        for (TeamNBA team : venue.getAssociatedTeams()) {
                            insStmt.setInt(1, venue.getId());
                            insStmt.setString(2, team.name());
                            insStmt.addBatch();
                        }
                        insStmt.executeBatch();
                    }

                    conn.commit();

                    // 3. Cache Write-Through
                    putInCache(venue, venue.getId());

                    logger.log(Level.INFO, "Venue updated successfully: {0}", venue.getId());
                    notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venue.getId()), venue);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venue.getId());
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVenue(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");
        int venueId = venue.getId();
        validateIdInput(venueId);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Cascade delete on DB handles teams
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE)) {
                stmt.setInt(1, venueId);
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    // CACHE REMOVE
                    removeFromCache(Venue.class, venueId);
                    logger.log(Level.INFO, "Venue deleted successfully: {0}", venueId);
                    notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(venueId), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_NOT_FOUND + ": " + venueId);
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting venue", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean venueExists(int venueId) throws DAOException {
        validateIdInput(venueId);
        return existsById(conn -> conn.prepareStatement(SQL_CHECK_VENUE_EXISTS), venueId);
    }

    /**
     * {@inheritDoc}
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
            throw new DAOException("Error getting next venue ID", e);
        }
    }

    // ========== TEAM MANAGEMENT METHODS ==========

    @Override
    public void saveVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");
        validateTeamInput(team);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE_TEAM)) {
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
        validateTeamInput(team);

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
    public Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        validateIdInput(venueId);
        Set<TeamNBA> teams = new HashSet<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE_TEAMS)) {
            stmt.setInt(1, venueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TeamNBA team = TeamNBA.robustValueOf(rs.getString("team_name"));
                    if (team != null) {
                        teams.add(team);
                    }
                }
            }
            return teams;
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venue teams", e);
        }
    }

    @Override
    public void deleteAllVenueTeams(Venue venue) throws DAOException {
        if (venue == null) throw new IllegalArgumentException("Venue cannot be null");

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_ALL_VENUE_TEAMS)) {
            stmt.setInt(1, venue.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Error deleting all venue teams", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps ResultSet to Venue.
     * Uses {@link DaoLoadingContext} to prevent circular loops.
     */
    private Venue mapResultSetToVenue(ResultSet rs) throws SQLException, DAOException {
        String managerUsername = rs.getString("venue_manager_username");
        int venueId = rs.getInt("id");
        String venueKey = "Venue:" + venueId;

        if (DaoLoadingContext.isLoading(venueKey)) {
            DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
            VenueManagerDao venueManagerDao = daoFactory.getVenueManagerDao();
            VenueManager venueManager = venueManagerDao.retrieveVenueManager(managerUsername);
            if (venueManager == null) throw new DAOException("VenueManager not found: " + managerUsername);

            return buildVenueWithTeams(venueId, rs, venueManager);
        }

        DaoLoadingContext.startLoading(venueKey);
        try {
            VenueManager venueManager = VenueDaoHelper.loadVenueManager(managerUsername);
            return buildVenueWithTeams(venueId, rs, venueManager);
        } finally {
            DaoLoadingContext.finishLoading(venueKey);
        }
    }

    private Venue buildVenueWithTeams(int venueId, ResultSet rs, VenueManager venueManager) throws SQLException, DAOException {
        // 1. RECUPERA I TEAM PRIMA DI COSTRUIRE
        Set<TeamNBA> teams = retrieveVenueTeams(venueId);

        // 2. COSTRUISCI L'OGGETTO
        return new Venue.Builder()
                .id(venueId)
                .name(rs.getString("name"))
                .type(VenueType.valueOf(rs.getString("type")))
                .address(rs.getString("address"))
                .city(rs.getString("city"))
                .maxCapacity(rs.getInt("max_capacity"))
                .venueManager(venueManager)
                .teams(teams) // Passa i team qui
                .build();
    }

    private void validateCityInput(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_CITY);
        }
    }

    private void validateTeamInput(TeamNBA team){
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
    }

    /**
     * Helper method to process a ResultSet and convert it to a list of Venues.
     * Centralizes the "Check Cache -> Map -> Put Cache" logic used by all list retrievers.
     */
    private List<Venue> processVenueResultSet(ResultSet rs) throws SQLException, DAOException {
        List<Venue> venues = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");

            // 1. CACHE CHECK (Identity Map)
            // Se l'oggetto è già stato caricato in questa sessione, usiamo QUELLO.
            // Questo garantisce che useremo sempre la stessa istanza.
            Venue cachedVenue = getFromCache(Venue.class, id);

            if (cachedVenue != null) {
                venues.add(cachedVenue);
            } else {
                // 2. LOAD & CACHE
                // Se non è in cache, lo creiamo dal ResultSet e lo salviamo in cache.
                Venue venue = mapResultSetToVenue(rs);
                putInCache(venue, id);
                venues.add(venue);
            }
        }
        return venues;
    }

    /**
     * Helper method to execute a simple query with one String parameter.
     * Reduces duplication for retrieveVenuesByManager and retrieveVenuesByCity.
     */
    private List<Venue> executeSimpleVenueQuery(String sql, String param, String errorContext) throws DAOException {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                return processVenueResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venues " + errorContext, e);
        }
    }
}