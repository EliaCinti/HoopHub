package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of {@link VenueManagerDao}.
 *
 * <p>Delegates common user data to {@link UserDao} and manages VenueManager-specific
 * data (company name, phone number) in the {@code venue_managers} table.
 * Uses {@link DaoLoadingContext} to prevent circular dependencies.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class VenueManagerDaoMySql extends AbstractMySqlDao implements VenueManagerDao {

    private final UserDao userDao;

    private static final String SQL_INSERT_VENUE_MANAGER =
            "INSERT INTO venue_managers (username, company_name, phone_number) VALUES (?, ?, ?)";

    private static final String SQL_SELECT_VENUE_MANAGER =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "vm.company_name, vm.phone_number " +
                    "FROM users u INNER JOIN venue_managers vm ON u.username = vm.username " +
                    "WHERE u.username = ?";

    private static final String SQL_SELECT_ALL_VENUE_MANAGERS =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "vm.company_name, vm.phone_number " +
                    "FROM users u INNER JOIN venue_managers vm ON u.username = vm.username";

    private static final String SQL_UPDATE_VENUE_MANAGER =
            "UPDATE venue_managers SET company_name = ?, phone_number = ? WHERE username = ?";

    private static final String SQL_DELETE_VENUE_MANAGER =
            "DELETE FROM venue_managers WHERE username = ?";

    private static final String VENUE_MANAGER = "VenueManager";
    private static final String ERR_NULL_VENUE_MANAGER = "VenueManager cannot be null";
    private static final String ERR_VENUE_MANAGER_NOT_FOUND = "VenueManager not found";

    public VenueManagerDaoMySql(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void saveVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            userDao.saveUser(venueManager);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE_MANAGER)) {
                stmt.setString(1, venueManager.getUsername());
                stmt.setString(2, venueManager.getCompanyName());
                stmt.setString(3, venueManager.getPhoneNumber());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(venueManager, venueManager.getUsername());

                    logger.log(Level.INFO, "VenueManager saved: {0}", venueManager.getUsername());
                    notifyObservers(DaoOperation.INSERT, VENUE_MANAGER, venueManager.getUsername(), venueManager);
                } else {
                    conn.rollback();
                    throw new DAOException("Failed to insert manager specific data");
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public VenueManager retrieveVenueManager(String username) throws DAOException {
        validateUsernameInput(username);

        VenueManager cached = getFromCache(VenueManager.class, username);
        if (cached != null) return cached;

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE_MANAGER)) {
                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        VenueManager vm = mapResultSetToManager(rs);
                        putInCache(vm, username);
                        return vm;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving venue manager", e);
        }
    }

    @Override
    public List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<VenueManager> managers = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUE_MANAGERS);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String username = rs.getString("username");
                    VenueManager cached = getFromCache(VenueManager.class, username);
                    if (cached != null) {
                        managers.add(cached);
                    } else {
                        VenueManager vm = mapResultSetToManager(rs);
                        putInCache(vm, username);
                        managers.add(vm);
                    }
                }
                return managers;
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving all managers", e);
        }
    }

    @Override
    public void updateVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            userDao.updateUser(venueManager);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE_MANAGER)) {
                stmt.setString(1, venueManager.getCompanyName());
                stmt.setString(2, venueManager.getPhoneNumber());
                stmt.setString(3, venueManager.getUsername());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit();
                    putInCache(venueManager, venueManager.getUsername());

                    logger.log(Level.INFO, "VenueManager updated: {0}", venueManager.getUsername());
                    notifyObservers(DaoOperation.UPDATE, VENUE_MANAGER, venueManager.getUsername(), venueManager);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE_MANAGER)) {
                stmt.setString(1, venueManager.getUsername());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    userDao.deleteUser(venueManager);

                    conn.commit();
                    removeFromCache(VenueManager.class, venueManager.getUsername());

                    logger.log(Level.INFO, "VenueManager deleted: {0}", venueManager.getUsername());
                    notifyObservers(DaoOperation.DELETE, VENUE_MANAGER, venueManager.getUsername(), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public List<Venue> getVenues(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);
        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();
        return venueDao.retrieveVenuesByManager(venueManager.getUsername());
    }

    // ========== PRIVATE HELPERS ==========

    /**
     * Maps ResultSet to VenueManager. Uses {@link DaoLoadingContext} to prevent cycles.
     */
    private VenueManager mapResultSetToManager(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String key = "VenueManager:" + username;

        if (DaoLoadingContext.isLoading(key)) {
            return new VenueManager.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .companyName(rs.getString("company_name"))
                    .phoneNumber(rs.getString("phone_number"))
                    .managedVenues(new ArrayList<>())
                    .build();
        }

        DaoLoadingContext.startLoading(key);
        try {
            return new VenueManager.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .companyName(rs.getString("company_name"))
                    .phoneNumber(rs.getString("phone_number"))
                    .managedVenues(new ArrayList<>())
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    private void validateVenueManagerInput(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE_MANAGER);
        }
    }
}