package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.csv.FanDaoCsv;
import it.uniroma2.hoophub.dao.mysql.FanDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for FanDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Creates FanDao with injected UserDao dependency for proper
 * dependency management without direct instantiation in DAOs.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class FanDaoFactory {

    /**
     * Creates FanDao with UserDao dependency for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate FanDao implementation with dependency
     */
    public FanDao getFanDao(PersistenceType persistenceType) {
        UserDao userDao = new UserDaoFactory().getUserDao(persistenceType);

        return switch (persistenceType) {
            case CSV -> createFanDaoCsv(userDao);
            case MYSQL -> createFanDaoMySql(userDao);
        };
    }

    private FanDao createFanDaoCsv(UserDao userDao) {
        return new FanDaoCsv(userDao);
    }

    private FanDao createFanDaoMySql(UserDao userDao) {
        return new FanDaoMySql(userDao);
    }
}