package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.csv.BookingDaoCsv;
import it.uniroma2.hoophub.dao.mysql.BookingDaoMySql;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;

/**
 * Factory class for creating BookingDao instances based on persistence type.
 * <p>
 * This factory implementation follows the Factory Method pattern to abstract
 * the creation of BookingDao objects, providing flexibility in choosing
 * the persistence mechanism at runtime.
 * </p>
 */
public class BookingDaoFactory {

    /**
     * Creates a BookingDao instance appropriate for the specified persistence type.
     *
     * @param persistenceType The type of persistence mechanism to use
     * @return A BookingDao implementation suitable for the specified persistence type
     * @throws IllegalArgumentException if the persistence type is not supported
     */
    public BookingDao getBookingDao(PersistenceType persistenceType) {
        return switch (persistenceType) {
            case CSV -> createBookingDaoCsv();
            case MYSQL -> createBookingDaoMySql();
        };
    }

    /**
     * Creates a new instance of the CSV-based BookingDao implementation.
     *
     * @return A new BookingDaoCsv instance
     */
    private BookingDao createBookingDaoCsv() {
        return new BookingDaoCsv();
    }

    /**
     * Creates a new instance of the MySQL-based BookingDao implementation.
     *
     * @return A new BookingDaoMySql instance
     */
    private BookingDao createBookingDaoMySql() {
        return new BookingDaoMySql();
    }
}