package it.uniroma2.hoophub.patterns.factory;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.dao.csv.*;

public class CsvDaoFactory implements DaoAbstractFactory {

    @Override
    public UserDao getUserDao() {
        return new UserDaoCsv();
    }

    @Override
    public FanDao getFanDao() {
        return new FanDaoCsv(this.getUserDao());
    }

    @Override
    public VenueManagerDao getVenueManagerDao() {
        return new VenueManagerDaoCsv(this.getUserDao());
    }

    @Override
    public VenueDao getVenueDao() {
        return new VenueDaoCsv();
    }

    @Override
    public BookingDao getBookingDao() {
        return new BookingDaoCsv();
    }

    @Override
    public NotificationDao getNotificationDao() {
        return new NotificationDaoCsv();
    }
}
