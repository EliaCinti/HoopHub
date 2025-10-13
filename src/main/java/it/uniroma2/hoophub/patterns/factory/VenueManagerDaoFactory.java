package it.uniroma2.hoophub.patterns.factory;

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
 */
public class VenueManagerDaoFactory {

    /**
     * Creates a VenueManagerDao instance appropriate for the specified persistence type.
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A VenueManagerDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public VenueManagerDao getVenueManagerDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createVenueManagerDaoCsv();
            case MYSQL -> createVenueManagerDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based VenueManagerDao implementation.
     *
     * @return A new VenueManagerDaoCsv instance
     */
    private VenueManagerDao createVenueManagerDaoCsv() {
        return new VenueManagerDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based VenueManagerDao implementation.
     *
     * @return A new VenueManagerDaoMySql instance
     */
    private VenueManagerDao createVenueManagerDaoMySql() {
        return new VenueManagerDaoMySql();
    }
}
