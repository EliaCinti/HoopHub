package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.dao.inmemory.*;

public class InMemoryDaoFactory implements DaoAbstractFactory {

    @Override
    public UserDao getUserDao() {
        return new UserDaoInMemory();
    }

    @Override
    public FanDao getFanDao() {
        return new FanDaoInMemory(this.getUserDao());
    }

    @Override
    public VenueManagerDao getVenueManagerDao() {
        return new VenueManagerDaoInMemory(this.getUserDao());
    }

    @Override
    public VenueDao getVenueDao() {
        return new VenueDaoInMemory();
    }

    @Override
    public BookingDao getBookingDao() {
        return new BookingDaoInMemory();
    }

    @Override
    public NotificationDao getNotificationDao() {
        return new NotificationDaoInMemory();
    }
}