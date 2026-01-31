package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.dao.csv.VenueManagerDaoCsv;
import it.uniroma2.hoophub.dao.inmemory.VenueManagerDaoInMemory;
import it.uniroma2.hoophub.dao.mysql.VenueManagerDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for VenueManagerDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Creates VenueManagerDao with injected UserDao dependency
 * for proper dependency management.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class VenueManagerDaoFactory {

    /**
     * Creates VenueManagerDao with UserDao dependency for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate VenueManagerDao implementation with dependency
     */
    public VenueManagerDao getVenueManagerDao(PersistenceType persistenceType) {
        UserDao userDao = new UserDaoFactory().getUserDao(persistenceType);

        return switch (persistenceType) {
            case CSV -> createVenueManagerDaoCsv(userDao);
            case MYSQL -> createVenueManagerDaoMySql(userDao);
            case IN_MEMORY -> createVenueManagerDaoInMemory(userDao);
        };
    }

    private VenueManagerDao createVenueManagerDaoCsv(UserDao userDao) {
        return new VenueManagerDaoCsv(userDao);
    }

    private VenueManagerDao createVenueManagerDaoMySql(UserDao userDao) {
        return new VenueManagerDaoMySql(userDao);
    }

    private VenueManagerDao createVenueManagerDaoInMemory(UserDao userDao) {
        return new VenueManagerDaoInMemory(userDao);
    }
}