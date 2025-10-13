package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.FanDao;
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
 */
public class FanDaoFactory {

    /**
     * Creates a FanDao instance appropriate for the specified persistence type.
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A FanDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public FanDao getFanDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createFanDaoCsv();
            case MYSQL -> createFanDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based FanDao implementation.
     *
     * @return A new FanDaoCsv instance
     */
    private FanDao createFanDaoCsv() {
        return new FanDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based FanDao implementation.
     *
     * @return A new FanDaoMySql instance
     */
    private FanDao createFanDaoMySql() {
        return new FanDaoMySql();
    }
}
