package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.csv.FanDaoCsv;
import it.uniroma2.hoophub.dao.mysql.FanDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating FanDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of FanDao objects. It allows the application to work with
 * different persistence mechanisms without coupling to specific implementations.
 * </p>
 * <p>
 * <strong>Dependency Injection:</strong> This factory creates FanDao instances with their
 * required UserDao dependency, ensuring proper use of the Factory pattern without direct
 * instantiation via "new" inside DAOs.
 * </p>
 */
public class FanDaoFactory {

    /**
     * Creates a FanDao instance appropriate for the specified persistence type.
     * <p>
     * The factory automatically creates the required UserDao dependency using the
     * UserDaoFactory with the same persistence type, ensuring consistency.
     * </p>
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A FanDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public FanDao getFanDao(PersistenceType persistenceType) {
        // Create the required UserDao dependency using UserDaoFactory
        UserDao userDao = new UserDaoFactory().getUserDao(persistenceType);

        return switch (persistenceType) {
            case CSV -> createFanDaoCsv(userDao);
            case MYSQL -> createFanDaoMySql(userDao);
        };
    }

    /**
     * Creates a new instance of the CSV-based FanDao implementation.
     *
     * @param userDao The UserDao dependency to inject
     * @return A new FanDaoCsv instance with injected dependency
     */
    private FanDao createFanDaoCsv(UserDao userDao) {
        return new FanDaoCsv(userDao);
    }

    /**
     * Creates a new instance of the MySQL-based FanDao implementation.
     *
     * @param userDao The UserDao dependency to inject
     * @return A new FanDaoMySql instance with injected dependency
     */
    private FanDao createFanDaoMySql(UserDao userDao) {
        return new FanDaoMySql(userDao);
    }
}
