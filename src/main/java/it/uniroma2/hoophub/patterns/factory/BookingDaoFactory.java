package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.csv.BookingDaoCsv;
import it.uniroma2.hoophub.dao.inmemory.BookingDaoInMemory;
import it.uniroma2.hoophub.dao.mysql.BookingDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory for BookingDao instances implementing <b>Factory Method pattern (GoF)</b>.
 *
 * <p>Abstracts DAO creation, enabling runtime persistence mechanism selection.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class BookingDaoFactory {

    /**
     * Creates BookingDao for specified persistence type.
     *
     * @param persistenceType persistence mechanism to use
     * @return appropriate BookingDao implementation
     */
    public BookingDao getBookingDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createBookingDaoCsv();
            case MYSQL -> createBookingDaoMySql();
            case IN_MEMORY -> createBookingDaoInMemory();
        };
    }

    private BookingDao createBookingDaoCsv() {
        return new BookingDaoCsv();
    }

    private BookingDao createBookingDaoMySql() {
        return new BookingDaoMySql();
    }

    private BookingDao createBookingDaoInMemory() {
        return new BookingDaoInMemory();
    }
}