package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.csv.UserDaoCsv;
import it.uniroma2.hoophub.dao.mysql.UserDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating UserDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of UserDao objects. It allows the application to work with
 * different persistence mechanisms (CSV files or MySQL database) without
 * coupling the client code to specific implementations.
 * </p>
 */
public class UserDaoFactory {

    /**
     * Creates a UserDao instance appropriate for the specified persistence type.
     * <p>
     * This method encapsulates the instantiation logic and returns the correct
     * implementation based on the persistence strategy.
     * </p>
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A UserDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public UserDao getUserDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createUserDaoCsv();
            case MYSQL -> createUserDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based UserDao implementation.
     *
     * @return A new UserDaoCsv instance
     */
    private UserDao createUserDaoCsv() {
        return new UserDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based UserDao implementation.
     *
     * @return A new UserDaoMySql instance
     */
    private UserDao createUserDaoMySql() {
        return new UserDaoMySql();
    }
}
