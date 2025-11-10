package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.dao.csv.VenueManagerDaoCsv;
import it.uniroma2.hoophub.dao.mysql.VenueManagerDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating VenueManagerDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of VenueManagerDao objects, allowing flexible switching between
 * different persistence mechanisms.
 * </p>
 * <p>
 * <strong>Dependency Injection:</strong> This factory creates VenueManagerDao instances with their
 * required UserDao dependency, ensuring proper use of the Factory pattern without direct
 * instantiation via "new" inside DAOs.
 * </p>
 */
public class VenueManagerDaoFactory {

    /**
     * Creates a VenueManagerDao instance appropriate for the specified persistence type.
     * <p>
     * The factory automatically creates the required UserDao dependency using the
     * UserDaoFactory with the same persistence type, ensuring consistency.
     * </p>
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A VenueManagerDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public VenueManagerDao getVenueManagerDao(PersistenceType persistenceType) {
        // Create the required UserDao dependency using UserDaoFactory
        UserDao userDao = new UserDaoFactory().getUserDao(persistenceType);

        return switch (persistenceType) {
            case CSV -> createVenueManagerDaoCsv(userDao);
            case MYSQL -> createVenueManagerDaoMySql(userDao);
        };
    }

    /**
     * Creates a new instance of the CSV-based VenueManagerDao implementation.
     *
     * @param userDao The UserDao dependency to inject
     * @return A new VenueManagerDaoCsv instance with injected dependency
     */
    private VenueManagerDao createVenueManagerDaoCsv(UserDao userDao) {
        return new VenueManagerDaoCsv(userDao);
    }

    /**
     * Creates a new instance of the MySQL-based VenueManagerDao implementation.
     *
     * @param userDao The UserDao dependency to inject
     * @return A new VenueManagerDaoMySql instance with injected dependency
     */
    private VenueManagerDao createVenueManagerDaoMySql(UserDao userDao) {
        return new VenueManagerDaoMySql(userDao);
    }
}
