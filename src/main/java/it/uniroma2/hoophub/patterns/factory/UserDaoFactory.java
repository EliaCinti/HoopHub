package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.csv.UserDaoCsv;
import it.uniroma2.hoophub.dao.inmemory.UserDaoInMemory;
import it.uniroma2.hoophub.dao.mysql.UserDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for UserDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Decouples client code from specific DAO implementations,
 * enabling flexible persistence strategy switching.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class UserDaoFactory {

    /**
     * Creates UserDao for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate UserDao implementation
     */
    public UserDao getUserDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createUserDaoCsv();
            case MYSQL -> createUserDaoMySql();
            case IN_MEMORY -> createUserDaoInMemory();
        };
    }

    private UserDao createUserDaoCsv() {
        return new UserDaoCsv();
    }

    private UserDao createUserDaoMySql() {
        return new UserDaoMySql();
    }

    private UserDao createUserDaoInMemory() {
        return new UserDaoInMemory();
    }
}