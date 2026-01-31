package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.csv.VenueDaoCsv;
import it.uniroma2.hoophub.dao.inmemory.VenueDaoInMemory;
import it.uniroma2.hoophub.dao.mysql.VenueDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for VenueDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Enables flexible persistence strategy selection for venue data.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class VenueDaoFactory {

    /**
     * Creates VenueDao for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate VenueDao implementation
     */
    public VenueDao getVenueDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createVenueDaoCsv();
            case MYSQL -> createVenueDaoMySql();
            case IN_MEMORY -> createVenueDaoInMemory();
        };
    }

    private VenueDao createVenueDaoCsv() {
        return new VenueDaoCsv();
    }

    private VenueDao createVenueDaoMySql() {
        return new VenueDaoMySql();
    }

    private VenueDao createVenueDaoInMemory() {
        return new VenueDaoInMemory();
    }
}