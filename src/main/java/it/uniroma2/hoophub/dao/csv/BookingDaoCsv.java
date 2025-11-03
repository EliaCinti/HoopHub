package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Booking;
import it.uniroma2.hoophub.model.Fan;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.BookingStatus;
import it.uniroma2.hoophub.utilities.CsvUtilities;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV implementation of the BookingDao interface.
 */
public class BookingDaoCsv extends AbstractObservableDao implements BookingDao {

    private static final Logger logger = Logger.getLogger(BookingDaoCsv.class.getName());

    private static final String CSV_FILE_PATH = "data/bookings.csv";
    private static final String[] CSV_HEADER = {"id", "game_date", "game_time", "home_team", "away_team",
            "venue_id", "fan_username", "status", "notified"};

    private static final int COL_ID = 0;
    private static final int COL_GAME_DATE = 1;
    private static final int COL_GAME_TIME = 2;
    private static final int COL_HOME_TEAM = 3;
    private static final int COL_AWAY_TEAM = 4;
    private static final int COL_VENUE_ID = 5;
    private static final int COL_FAN_USERNAME = 6;
    private static final int COL_STATUS = 7;
    private static final int COL_NOTIFIED = 8;

    private final File csvFile;

    public BookingDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        initializeCsvFile();
    }

    @Override
    public synchronized void saveBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        String[] newRow = {
                String.valueOf(booking.getId()),
                booking.getGameDate().toString(),
                booking.getGameTime().toString(),
                booking.getHomeTeam(),
                booking.getAwayTeam(),
                String.valueOf(booking.getVenueId()),
                booking.getFanUsername(),
                booking.getStatus().name(),
                String.valueOf(booking.isNotified())
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "Booking saved successfully: {0}", booking.getId());
        notifyObservers(DaoOperation.INSERT, "Booking", String.valueOf(booking.getId()), booking);
    }

    @Override
    public synchronized Booking retrieveBooking(int bookingId) throws DAOException {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == bookingId) {
                return mapRowToBooking(row);
            }
        }

        return null;
    }

    @Override
    public synchronized List<Booking> retrieveAllBookings() throws DAOException {
        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            bookings.add(mapRowToBooking(data.get(i)));
        }

        logger.log(Level.INFO, "Retrieved {0} bookings", bookings.size());
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByFan(Fan fan) throws DAOException {
        if (fan == null) {
            throw new IllegalArgumentException("Fan cannot be null");
        }

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fan.getUsername())) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for fan {1}",
                new Object[]{bookings.size(), fan.getUsername()});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenue(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_VENUE_ID]) == venue.getId()) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for venue {1}",
                new Object[]{bookings.size(), venue.getId()});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByVenueManager(VenueManager venueManager) throws DAOException {
        if (venueManager == null) {
            throw new IllegalArgumentException("VenueManager cannot be null");
        }

        VenueDaoCsv venueDao = new VenueDaoCsv();
        List<Venue> managerVenues = venueDao.retrieveVenuesByManager(venueManager);

        List<Booking> bookings = new ArrayList<>();
        for (Venue venue : managerVenues) {
            bookings.addAll(retrieveBookingsByVenue(venue));
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for venue manager {1}",
                new Object[]{bookings.size(), venueManager.getUsername()});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByDate(LocalDate date) throws DAOException {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (LocalDate.parse(row[COL_GAME_DATE]).equals(date)) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} bookings for date {1}", new Object[]{bookings.size(), date});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveBookingsByStatus(Fan fan, BookingStatus status) throws DAOException {
        if (fan == null || status == null) {
            throw new IllegalArgumentException("Fan and Status cannot be null");
        }

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fan.getUsername()) &&
                    row[COL_STATUS].equals(status.name())) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} {1} bookings for fan {2}",
                new Object[]{bookings.size(), status, fan.getUsername()});
        return bookings;
    }

    @Override
    public synchronized List<Booking> retrieveUnnotifiedBookings(Fan fan) throws DAOException {
        if (fan == null) {
            throw new IllegalArgumentException("Fan cannot be null");
        }

        List<Booking> bookings = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_FAN_USERNAME].equals(fan.getUsername()) &&
                    !Boolean.parseBoolean(row[COL_NOTIFIED])) {
                bookings.add(mapRowToBooking(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} unnotified bookings for fan {1}",
                new Object[]{bookings.size(), fan.getUsername()});
        return bookings;
    }

    @Override
    public synchronized void updateBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == booking.getId()) {
                row[COL_GAME_DATE] = booking.getGameDate().toString();
                row[COL_GAME_TIME] = booking.getGameTime().toString();
                row[COL_HOME_TEAM] = booking.getHomeTeam();
                row[COL_AWAY_TEAM] = booking.getAwayTeam();
                row[COL_VENUE_ID] = String.valueOf(booking.getVenueId());
                row[COL_STATUS] = booking.getStatus().name();
                row[COL_NOTIFIED] = String.valueOf(booking.isNotified());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Booking not found for update: " + booking.getId());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Booking updated successfully: {0}", booking.getId());
        notifyObservers(DaoOperation.UPDATE, "Booking", String.valueOf(booking.getId()), booking);
    }

    @Override
    public synchronized void deleteBooking(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            if (Integer.parseInt(data.get(i)[COL_ID]) == booking.getId()) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Booking not found for deletion: " + booking.getId());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Booking deleted successfully: {0}", booking.getId());
        notifyObservers(DaoOperation.DELETE, "Booking", String.valueOf(booking.getId()), null);
    }

    @Override
    public synchronized boolean bookingExists(Booking booking) throws DAOException {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            if (Integer.parseInt(data.get(i)[COL_ID]) == booking.getId()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized int getNextBookingId() throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        int maxId = 0;

        for (int i = 1; i < data.size(); i++) {
            int id = Integer.parseInt(data.get(i)[COL_ID]);
            if (id > maxId) {
                maxId = id;
            }
        }

        return maxId + 1;
    }

    private Booking mapRowToBooking(String[] row) throws DAOException {
        String fanUsername = row[COL_FAN_USERNAME];
        int venueId = Integer.parseInt(row[COL_VENUE_ID]);

        FanDaoCsv fanDao = new FanDaoCsv();
        Fan fan = fanDao.retrieveFan(fanUsername);

        VenueDaoCsv venueDao = new VenueDaoCsv();
        Venue venue = venueDao.retrieveVenue(venueId);

        if (fan == null) {
            throw new DAOException("Fan not found for booking: " + fanUsername);
        }
        if (venue == null) {
            throw new DAOException("Venue not found for booking: " + venueId);
        }

        return new Booking.Builder(
                Integer.parseInt(row[COL_ID]),
                LocalDate.parse(row[COL_GAME_DATE]),
                LocalTime.parse(row[COL_GAME_TIME]),
                row[COL_HOME_TEAM],
                row[COL_AWAY_TEAM],
                venue,
                fan
        )
                .status(BookingStatus.valueOf(row[COL_STATUS]))
                .notified(Boolean.parseBoolean(row[COL_NOTIFIED]))
                .build();
    }

    private void initializeCsvFile() {
        try {
            if (!csvFile.exists()) {
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                List<String[]> emptyData = new ArrayList<>();
                CsvUtilities.updateFile(csvFile, CSV_HEADER, emptyData);
                logger.info("Initialized CSV file: " + CSV_FILE_PATH);
            }
        } catch (DAOException e) {
            logger.log(Level.SEVERE, "Failed to initialize CSV file", e);
        }
    }
}
