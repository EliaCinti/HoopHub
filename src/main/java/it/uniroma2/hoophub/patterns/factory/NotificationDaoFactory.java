package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.csv.NotificationDaoCsv;
import it.uniroma2.hoophub.dao.mysql.NotificationDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for NotificationDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Uses polymorphic switch expression for type-safe DAO creation
 * with compile-time exhaustiveness checking.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NotificationDaoFactory {

    /**
     * Creates NotificationDao for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate NotificationDao implementation
     */
    public NotificationDao getNotificationDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createNotificationDaoCsv();
            case MYSQL -> createNotificationDaoMySql();
        };
    }

    private NotificationDao createNotificationDaoCsv() {
        return new NotificationDaoCsv();
    }

    private NotificationDao createNotificationDaoMySql() {
        return new NotificationDaoMySql();
    }
}