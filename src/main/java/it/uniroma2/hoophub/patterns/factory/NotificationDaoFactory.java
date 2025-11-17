package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.csv.NotificationDaoCsv;
import it.uniroma2.hoophub.dao.mysql.NotificationDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating NotificationDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of NotificationDao objects, providing flexibility in choosing
 * the persistence mechanism at runtime.
 * </p>
 * <p>
 * <strong>Polymorphic Solution with Switch/Case:</strong>
 * This factory uses a switch expression to provide a type-safe, polymorphic
 * solution for DAO creation. The Java compiler ensures exhaustiveness checking,
 * meaning all PersistenceType enum values must be handled.
 * </p>
 *
 * @author Elia Cinti
 */
public class NotificationDaoFactory {

    /**
     * Creates a NotificationDao instance appropriate for the specified persistence type.
     * <p>
     * This method implements polymorphism using switch expression:
     * - CSV → NotificationDaoCsv implementation
     * - MYSQL → NotificationDaoMySql implementation
     * </p>
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A NotificationDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported (compile-time exhaustiveness check prevents this)
     */
    public NotificationDao getNotificationDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createNotificationDaoCsv();
            case MYSQL -> createNotificationDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based NotificationDao implementation.
     *
     * @return A new NotificationDaoCsv instance
     */
    private NotificationDao createNotificationDaoCsv() {
        return new NotificationDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based NotificationDao implementation.
     *
     * @return A new NotificationDaoMySql instance
     */
    private NotificationDao createNotificationDaoMySql() {
        return new NotificationDaoMySql();
    }
}
