package it.uniroma2.hoophub.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection factory using the <b>Singleton pattern (GoF)</b>.
 *
 * <p>Maintains a single database connection for the application. Loads configuration
 * from {@code config.properties} and provides automatic reconnection if needed.
 * Suitable for single-threaded applications.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class ConnectionFactory {

    private static final Logger logger = Logger.getLogger(ConnectionFactory.class.getName());
    private static Connection connection;
    private static String connectionUrl;
    private static String user;
    private static String pass;

    private ConnectionFactory() {}

    static {
        try {
            loadConfiguration();
            initializeConnection();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load database configuration", e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection", e);
        }
    }

    /**
     * Extracts database name from JDBC URL.
     *
     * @param jdbcUrl the JDBC URL (e.g., jdbc:mysql://host:port/database)
     * @return the database name, or null if not found
     */
    private static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == jdbcUrl.length() - 1) {
            return null;
        }
        String afterSlash = jdbcUrl.substring(lastSlash + 1);
        int questionMark = afterSlash.indexOf('?');
        return questionMark == -1 ? afterSlash : afterSlash.substring(0, questionMark);
    }

    /**
     * Loads configuration from config.properties file.
     */
    private static void loadConfiguration() throws IOException {
        try (InputStream input = ConnectionFactory.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.warning("Unable to find config.properties, using default configurations");
                connectionUrl = "jdbc:mysql://localhost:3306/hoophub";
                user = "root";
                pass = "";
                return;
            }
            Properties properties = new Properties();
            properties.load(input);
            connectionUrl = properties.getProperty("jdbcURL");
            user = properties.getProperty("jdbcUsername");
            pass = properties.getProperty("jdbcPassword");
            logger.info("Database configuration loaded successfully");
        }
    }

    /**
     * Initializes database connection and selects schema.
     */
    private static void initializeConnection() throws SQLException {
        if (connectionUrl == null) {
            throw new SQLException("Database URL is not configured");
        }
        connection = DriverManager.getConnection(connectionUrl, user, pass);
        String databaseName = extractDatabaseName(connectionUrl);
        if (databaseName != null && !databaseName.isEmpty()) {
            connection.setCatalog(databaseName);
            logger.log(Level.INFO, "Database schema ''{0}'' selected successfully", databaseName);
        }
        logger.info("Database connection established successfully");
    }

    /**
     * Returns the database connection, reconnecting if needed.
     *
     * @return the active database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            logger.info("Connection is closed or null, reconnecting...");
            initializeConnection();
        }
        return connection;
    }

    /**
     * Tests if the database connection is valid.
     *
     * @return true if connection is valid
     */
    public static boolean testConnection() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Connection test failed", e);
            return false;
        }
    }

    /**
     * Closes the database connection.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed successfully");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
}