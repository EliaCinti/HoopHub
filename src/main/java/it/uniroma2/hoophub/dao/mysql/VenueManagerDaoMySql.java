package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.UserBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.exception.DAOException;
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
 * MySQL implementation of the VenueManagerDao interface.
 * <p>
 * This class provides data access operations for VenueManager entities stored in a MySQL database.
 * It coordinates between the users and venue_managers tables, delegating common user operations
 * to {@link UserDaoMySql} while handling venue manager-specific data directly.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>users table</strong>: username (PK), password_hash, full_name, gender, user_type</li>
 *   <li><strong>venue_managers table</strong>: username (PK, FK), company_name, phone_number</li>
 * </ul>
 * </p>
 *
 * @see VenueManagerDao
 * @see AbstractMySqlDao
 */
public class VenueManagerDaoMySql extends AbstractMySqlDao implements VenueManagerDao {

    private final UserDao userDao;

    // ========== SQL Queries ==========
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

    private static final String SQL_SELECT_MANAGER_VENUES =
            "SELECT id FROM venues WHERE venue_manager_username = ?";

    // ========== Constants ==========
    private static final String VENUE_MANAGER = "VenueManager";

    // ========== Error messages ==========
    private static final String ERR_NULL_VENUE_MANAGER_BEAN = "VenueManagerBean cannot be null";
    private static final String ERR_NULL_VENUE_MANAGER = "VenueManager cannot be null";
    private static final String ERR_VENUE_MANAGER_NOT_FOUND = "VenueManager not found";

    /**
     * Constructs a new VenueManagerDaoMySql with a UserDao dependency.
     * <p>
     * <strong>Dependency Injection:</strong> The UserDao is injected via constructor
     * by the VenueManagerDaoFactory, ensuring proper use of the Factory pattern.
     * </p>
     *
     * @param userDao The UserDao implementation to use for common user operations
     */
    public VenueManagerDaoMySql(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs two operations within a transaction:
     * <ol>
     *   <li>Saves common user data via {@link UserDao#saveUser(UserBean)}</li>
     *   <li>Saves venue manager-specific data in the venue_managers table</li>
     * </ol>
     * If either operation fails, the entire transaction is rolled back.
     * </p>
     */
    @Override
    public void saveVenueManager(VenueManagerBean venueManagerBean) throws DAOException {
        validateVenueManagerBeanInput(venueManagerBean);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Save common user data first
            userDao.saveUser(venueManagerBean);

            // Then save venue manager-specific data
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENUE_MANAGER)) {
                stmt.setString(1, venueManagerBean.getUsername());
                stmt.setString(2, venueManagerBean.getCompanyName());
                stmt.setString(3, venueManagerBean.getPhoneNumber());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    logger.log(Level.INFO, "VenueManager saved successfully: {0}", venueManagerBean.getUsername());
                    notifyObservers(DaoOperation.INSERT, VENUE_MANAGER, venueManagerBean.getUsername(), venueManagerBean);
                } else {
                    conn.rollback();
                    throw new DAOException("Failed to insert venue manager-specific data");
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during venue manager save", e);
            throw new DAOException("Error saving venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves venue manager data using a JOIN between users and venue_managers tables.
     * The returned VenueManager object has an empty venues list - venues should be
     * loaded separately when needed using {@link #getVenues(VenueManager)}.
     * </p>
     */
    @Override
    public VenueManager retrieveVenueManager(String username) throws DAOException {
        validateUsernameInput(username);

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_VENUE_MANAGER)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToVenueManager(rs);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue manager retrieval", e);
            throw new DAOException("Error retrieving venue manager", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VenueManager> retrieveAllVenueManagers() throws DAOException {
        List<VenueManager> venueManagers = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_VENUE_MANAGERS);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    venueManagers.add(mapResultSetToVenueManager(rs));
                }

                return venueManagers;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venue managers retrieval", e);
            throw new DAOException("Error retrieving all venue managers", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation updates both:
     * <ul>
     *   <li>Common user data via {@link UserDao#updateUser(it.uniroma2.hoophub.model.User, UserBean)}</li>
     *   <li>Venue manager-specific data in the venue_managers table</li>
     * </ul>
     * </p>
     */
    @Override
    public void updateVenueManager(VenueManager venueManager, UserBean userBean) throws DAOException {
        validateVenueManagerInput(venueManager);
        validateUserBeanInput(userBean);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Update common user data first
            userDao.updateUser(venueManager, userBean);

            // Then update venue manager-specific data
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VENUE_MANAGER)) {
                stmt.setString(1, venueManager.getCompanyName());
                stmt.setString(2, venueManager.getPhoneNumber());
                stmt.setString(3, venueManager.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();
                    logger.log(Level.INFO, "VenueManager updated successfully: {0}", venueManager.getUsername());
                    notifyObservers(DaoOperation.UPDATE, VENUE_MANAGER, venueManager.getUsername(), venueManager);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND + ": " + venueManager.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during venue manager update", e);
            throw new DAOException("Error updating venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation deletes in the correct order due to foreign key constraints:
     * <ol>
     *   <li>Delete venue manager-specific data from venue_managers table</li>
     *   <li>Delete common user data via {@link UserDao#deleteUser(it.uniroma2.hoophub.model.User)}</li>
     * </ol>
     * </p>
     */
    @Override
    public void deleteVenueManager(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Delete venue manager-specific data first
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_VENUE_MANAGER)) {
                stmt.setString(1, venueManager.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // Then delete common user data
                    userDao.deleteUser(venueManager);

                    conn.commit();
                    logger.log(Level.INFO, "VenueManager deleted successfully: {0}", venueManager.getUsername());
                    notifyObservers(DaoOperation.DELETE, VENUE_MANAGER, venueManager.getUsername(), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_VENUE_MANAGER_NOT_FOUND + ": " + venueManager.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            logger.log(Level.SEVERE, "Database error during venue manager deletion", e);
            throw new DAOException("Error deleting venue manager", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation queries the venues table and reconstructs full Venue objects
     * using {@link VenueDaoMySql}. This avoids circular dependencies and respects
     * the single responsibility principle.
     * </p>
     */
    @Override
    public List<it.uniroma2.hoophub.model.Venue> getVenues(VenueManager venueManager) throws DAOException {
        validateVenueManagerInput(venueManager);

        List<it.uniroma2.hoophub.model.Venue> venues = new ArrayList<>();
        // Use DaoFactoryFacade to get VenueDao (Factory pattern)
        DaoFactoryFacade daoFactory = DaoFactoryFacade.getInstance();
        VenueDao venueDao = daoFactory.getVenueDao();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MANAGER_VENUES)) {

                stmt.setString(1, venueManager.getUsername());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int venueId = rs.getInt("id");
                        it.uniroma2.hoophub.model.Venue venue = venueDao.retrieveVenue(venueId);
                        if (venue != null) {
                            venues.add(venue);
                        }
                    }
                }

                logger.log(Level.INFO, "Retrieved {0} venues for manager {1}",
                        new Object[]{venues.size(), venueManager.getUsername()});
                return venues;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during venues retrieval for manager", e);
            throw new DAOException("Error retrieving venues for venue manager", e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a ResultSet row to a VenueManager domain object.
     * <p>
     * Includes anti-loop protection via DaoLoadingContext to prevent circular dependencies
     * when VenueManager is referenced from related entities.
     * </p>
     */
    private VenueManager mapResultSetToVenueManager(ResultSet rs) throws SQLException {
        String username = rs.getString("username");

        String key = "VenueManager:" + username;
        if (DaoLoadingContext.isLoading(key)) {
            // Return minimal VenueManager object without loading relationships
            return new VenueManager.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .companyName(rs.getString("company_name"))
                    .phoneNumber(rs.getString("phone_number"))
                    .managedVenues(new ArrayList<>())  // Empty list - venues not loaded during cycle
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
                    .managedVenues(new ArrayList<>())  // Empty list - venues loaded separately
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    // ========== VALIDATION METHODS ==========

    private void validateVenueManagerBeanInput(VenueManagerBean venueManagerBean) {
        if (venueManagerBean == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE_MANAGER_BEAN);
        }
    }

    private void validateVenueManagerInput(VenueManager venueManager) {
        if (venueManager == null) {
            throw new IllegalArgumentException(ERR_NULL_VENUE_MANAGER);
        }
    }
}