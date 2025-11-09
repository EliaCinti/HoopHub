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
 * Factory class that provides database connections.
 * This implementation maintains a single connection for the application.
 * Suitable for non-multithreaded applications.
 */
public class ConnectionFactory {
    private static final Logger logger = Logger.getLogger(ConnectionFactory.class.getName());
    private static Connection connection;
    private static String connectionUrl;
    private static String user;
    private static String pass;

    /**
     * Private constructor to enforce singleton pattern
     */
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
     * Extracts the database name from a JDBC URL.
     * <p>
     * Parses URLs in the format: jdbc:mysql://host:port/database
     * or jdbc:mysql://host:port/database?params
     * </p>
     *
     * @param jdbcUrl The JDBC connection URL
     * @return The database name, or null if not found
     */
    private static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }

        // Find the last '/' which precedes the database name
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == jdbcUrl.length() - 1) {
            return null;
        }

        // Extract everything after the last '/', up to '?' (if query params exist)
        String afterSlash = jdbcUrl.substring(lastSlash + 1);
        int questionMark = afterSlash.indexOf('?');

        return questionMark == -1 ? afterSlash : afterSlash.substring(0, questionMark);
    }

    /**
     * Loads database configuration from the properties file
     */
    private static void loadConfiguration() throws IOException {
        try (InputStream input = ConnectionFactory.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.warning("Unable to find config.properties, using default configurations");
                // Default configuration for development
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
     * Initializes the database connection and explicitly selects the schema
     */
    private static void initializeConnection() throws SQLException {
        if (connectionUrl == null) {
            throw new SQLException("Database URL is not configured");
        }

        connection = DriverManager.getConnection(connectionUrl, user, pass);

        // Explicitly select the database schema to avoid MySQL client issues
        // Extract database name from URL (format: jdbc:mysql://host:port/database)
        String databaseName = extractDatabaseName(connectionUrl);
        if (databaseName != null && !databaseName.isEmpty()) {
            try (var statement = connection.createStatement()) {
                statement.execute("USE " + databaseName);
                logger.info("Database schema '" + databaseName + "' selected successfully");
            }
        }

        logger.info("Database connection established successfully");
    }

    /**
     * Gets the database connection, creating a new one if necessary
     *
     * @return the database connection
     * @throws SQLException if there is an error getting the connection
     */
    public static Connection getConnection() throws SQLException {
        // Check if connection is closed or null and reconnect if needed
        if (connection == null || connection.isClosed()) {
            logger.info("Connection is closed or null, reconnecting...");
            initializeConnection();
        }
        return connection;
    }

    /**
     * Tests if the database connection is valid
     *
     * @return true if the connection is valid, false otherwise
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
     * Closes the database connection
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
