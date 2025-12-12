package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.dao.ConnectionFactory;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MySQL implementation of the FanDao interface.
 * <p>
 * This class provides data access operations for Fan entities stored in a MySQL database.
 * It coordinates between the users and fans tables, delegating common user operations
 * to {@link UserDaoMySql} while handling fan-specific data directly.
 * </p>
 * <p>
 * Database structure:
 * <ul>
 *   <li><strong>users table</strong>: username (PK), password_hash, full_name, gender, user_type</li>
 *   <li><strong>fans table</strong>: username (PK, FK), fav_team, birthday</li>
 * </ul>
 * </p>
 *
 * @see FanDao
 * @see AbstractMySqlDao
 */
public class FanDaoMySql extends AbstractMySqlDao implements FanDao {

    private final UserDao userDao;

    // ========== SQL Queries ==========
    private static final String SQL_INSERT_FAN =
            "INSERT INTO fans (username, fav_team, birthday) VALUES (?, ?, ?)";

    private static final String SQL_SELECT_FAN =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "f.fav_team, f.birthday " +
                    "FROM users u INNER JOIN fans f ON u.username = f.username " +
                    "WHERE u.username = ?";

    private static final String SQL_SELECT_ALL_FANS =
            "SELECT u.username, u.password_hash, u.full_name, u.gender, u.user_type, " +
                    "f.fav_team, f.birthday " +
                    "FROM users u INNER JOIN fans f ON u.username = f.username";

    private static final String SQL_UPDATE_FAN =
            "UPDATE fans SET fav_team = ?, birthday = ? WHERE username = ?";

    private static final String SQL_DELETE_FAN =
            "DELETE FROM fans WHERE username = ?";

    // ========== Error messages ==========
    private static final String ERR_NULL_FAN = "Fan cannot be null";
    private static final String ERR_FAN_NOT_FOUND = "Fan not found";

    /**
     * Constructs a new FanDaoMySql with a UserDao dependency.
     * <p>
     * The UserDao is used to handle common user operations, following the
     * DRY principle and avoiding code duplication.
     * </p>
     * <p>
     * <strong>Dependency Injection:</strong> The UserDao is injected via constructor
     * by the FanDaoFactory, ensuring proper use of the Factory pattern.
     * </p>
     *
     * @param userDao The UserDao implementation to use for common user operations
     */
    public FanDaoMySql(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs two operations within a transaction:
     * <ol>
     *   <li>Saves fan-specific data in the fans table</li>
     * </ol>
     * If either operation fails, the entire transaction is rolled back.
     * </p>
     */
    @Override
    public void saveFan(Fan fan) throws DAOException { // Firma cambiata: prende Fan, non Bean
        validateFanInput(fan); // Assumendo tu abbia un validatore per il model

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // 1. Save common user data first (delegate to UserDao)
            // UserDao.saveUser ora si aspetta un oggetto User (e Fan è un User)
            userDao.saveUser(fan);

            // 2. Save fan-specific data
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_FAN)) {
                stmt.setString(1, fan.getUsername());
                // Leggiamo direttamente dal Model, non dalla Bean
                stmt.setString(2, fan.getFavTeam().name());
                stmt.setDate(3, java.sql.Date.valueOf(fan.getBirthday()));

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();

                    // === CACHE PUT (Refactoring) ===
                    // Non serve più ricostruire l'oggetto "newFanModel" dai pezzi della bean!
                    // L'oggetto 'fan' passato come parametro è GIÀ il modello completo e corretto.
                    // Lo mettiamo direttamente in cache.
                    putInCache(fan, fan.getUsername());

                    logger.log(Level.INFO, "Fan saved successfully: {0}", fan.getUsername());

                    // Notifica Observer passando il Model (che è Serializable)
                    notifyObservers(DaoOperation.INSERT, "Fan", fan.getUsername(), fan);
                } else {
                    conn.rollback();
                    throw new DAOException("Failed to insert fan-specific data");
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error saving fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * Retrieves fan data using a JOIN between users and fans tables.
     * The returned Fan object has an empty booking list - bookings should be
     * loaded separately when needed.
     * </p>
     */
    @Override
    public Fan retrieveFan(String username) throws DAOException {
        validateUsernameInput(username);

        // 1. CACHE CHECK (Identity Map)
        Fan cachedFan = getFromCache(Fan.class, username);
        if (cachedFan != null) {
            return cachedFan;
        }

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_FAN)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Fan fan = mapResultSetToFan(rs);

                        // 2. CACHE PUT
                        putInCache(fan, username);

                        return fan;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error retrieving fan", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses the Identity Map (Cache) pattern to ensure that if a Fan is already loaded
     * in memory, the existing instance is returned instead of creating a new one.
     * </p>
     */
    @Override
    public List<Fan> retrieveAllFans() throws DAOException {
        List<Fan> fans = new ArrayList<>();

        try {
            Connection conn = ConnectionFactory.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_FANS);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String username = rs.getString("username");

                    // 1. CACHE CHECK
                    // Se l'oggetto è già in memoria, usiamo l'istanza esistente (Identity Map)
                    Fan cachedFan = getFromCache(Fan.class, username);

                    if (cachedFan != null) {
                        fans.add(cachedFan);
                    } else {
                        // 2. LOAD & CACHE
                        // Se non c'è, lo costruiamo dal ResultSet e lo cachiamo
                        Fan fan = mapResultSetToFan(rs);
                        putInCache(fan, username);
                        fans.add(fan);
                    }
                }

                return fans;
            }
        } catch (SQLException e) {
            // Nota: il logger nel catch è stato rimosso come da refactoring code smells
            throw new DAOException("Error retrieving all fans", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation updates both:
     * <ul>
     *   <li>Common user data via {@link UserDao#updateUser(it.uniroma2.hoophub.model.User)}</li>
     *   <li>Fan-specific data in the fans table</li>
     * </ul>
     * </p>
     */
    @Override
    public void updateFan(Fan fan) throws DAOException {
        // Validiamo solo il modello
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // 1. Aggiorna dati comuni (User)
            // Chiama il NUOVO metodo di UserDao che accetta (User)
            userDao.updateUser(fan);

            // 2. Aggiorna dati specifici (Fan)
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_FAN)) {
                // Leggiamo i dati direttamente dal MODEL (fan)
                stmt.setString(1, fan.getFavTeam().name());
                stmt.setDate(2, Date.valueOf(fan.getBirthday()));
                stmt.setString(3, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit();

                    // === CACHE WRITE-THROUGH ===
                    // Mettiamo in cache l'oggetto aggiornato (che è già 'fan')
                    putInCache(fan, fan.getUsername());

                    logger.log(Level.INFO, "Fan updated successfully: {0}", fan.getUsername());

                    // Notifichiamo l'observer passando il Model
                    notifyObservers(DaoOperation.UPDATE, "Fan", fan.getUsername(), fan);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error updating fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the entity from the internal cache upon successful deletion.
     * </p>
     */
    @Override
    public void deleteFan(Fan fan) throws DAOException {
        validateFanInput(fan);

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // Delete fan-specific data first
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_FAN)) {
                stmt.setString(1, fan.getUsername());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // Then delete common user data
                    userDao.deleteUser(fan);

                    conn.commit();

                    // === GESTIONE CACHE ===
                    // Rimuoviamo l'oggetto dalla cache per mantenere la consistenza
                    removeFromCache(Fan.class, fan.getUsername());

                    logger.log(Level.INFO, "Fan deleted successfully: {0}", fan.getUsername());
                    notifyObservers(DaoOperation.DELETE, "Fan", fan.getUsername(), null);
                } else {
                    conn.rollback();
                    throw new DAOException(ERR_FAN_NOT_FOUND + ": " + fan.getUsername());
                }
            }

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new DAOException("Error deleting fan", e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Maps a ResultSet row to a Fan domain object.
     * <p>
     * The ResultSet must contain all required columns from the JOIN query.
     * Includes anti-loop protection via DaoLoadingContext to prevent circular dependencies
     * when Fan is referenced from related entities.
     * </p>
     */
    private Fan mapResultSetToFan(ResultSet rs) throws SQLException, DAOException {
        String username = rs.getString("username");
        String teamString = rs.getString("fav_team");

        // FIX: Sostituita la logica manuale con il metodo centralizzato robusto
        TeamNBA team = TeamNBA.robustValueOf(teamString);

        if (team == null) {
            throw new DAOException("Invalid team for fan " + username + ": " + teamString);
        }

        String key = "Fan:" + username;
        if (DaoLoadingContext.isLoading(key)) {
            // Return minimal Fan object without loading relationships
            return new Fan.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .favTeam(team)
                    .birthday(rs.getDate("birthday").toLocalDate())
                    .bookingList(new ArrayList<>())  // Empty list - bookings not loaded during cycle
                    .build();
        }

        DaoLoadingContext.startLoading(key);
        try {
            return new Fan.Builder()
                    .username(username)
                    .fullName(rs.getString("full_name"))
                    .gender(rs.getString("gender"))
                    .favTeam(team)
                    .birthday(rs.getDate("birthday").toLocalDate())
                    // Nota: qui potresti voler caricare le booking reali se la logica lo prevede,
                    // ma se il metodo originale usava ArrayList vuoto, lascialo così.
                    .bookingList(new ArrayList<>())
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    // ========== VALIDATION METHODS ==========

    private void validateFanInput(Fan fan) {
        if (fan == null) {
            throw new IllegalArgumentException(ERR_NULL_FAN);
        }
    }
}