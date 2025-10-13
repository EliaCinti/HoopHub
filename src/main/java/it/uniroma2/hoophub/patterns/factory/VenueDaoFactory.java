package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.csv.VenueDaoCsv;
import it.uniroma2.hoophub.dao.mysql.VenueDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating VenueDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of VenueDao objects, enabling flexible persistence strategy selection.
 * </p>
 */
public class VenueDaoFactory {

    /**
     * Creates a VenueDao instance appropriate for the specified persistence type.
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A VenueDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public VenueDao getVenueDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createVenueDaoCsv();
            case MYSQL -> createVenueDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based VenueDao implementation.
     *
     * @return A new VenueDaoCsv instance
     */
    private VenueDao createVenueDaoCsv() {
        return new VenueDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based VenueDao implementation.
     *
     * @return A new VenueDaoMySql instance
     */
    private VenueDao createVenueDaoMySql() {
        return new VenueDaoMySql();
    }
}