package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.dao.mysql.*;

public class MySqlDaoFactory implements DaoAbstractFactory {

    @Override
    public UserDao getUserDao() {
        return new UserDaoMySql();
    }

    @Override
    public FanDao getFanDao() {
        return new FanDaoMySql(this.getUserDao());
    }

    @Override
    public VenueManagerDao getVenueManagerDao() {
        return new VenueManagerDaoMySql(this.getUserDao());
    }

    @Override
    public VenueDao getVenueDao() {
        return new VenueDaoMySql();
    }

    @Override
    public BookingDao getBookingDao() {
        return new BookingDaoMySql();
    }

    @Override
    public NotificationDao getNotificationDao() {
        return new NotificationDaoMySql();
    }
}
