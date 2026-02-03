package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.NotificationDao;
import it.uniroma2.hoophub.dao.UserDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;

/**
 * Interface for the Abstract Factory Pattern (GoF).
 * Defines the contract for creating families of related DAO objects
 * without specifying their concrete classes.
 */
public interface DaoAbstractFactory {
    UserDao getUserDao();
    FanDao getFanDao();
    VenueManagerDao getVenueManagerDao();
    VenueDao getVenueDao();
    BookingDao getBookingDao();
    NotificationDao getNotificationDao();
}
