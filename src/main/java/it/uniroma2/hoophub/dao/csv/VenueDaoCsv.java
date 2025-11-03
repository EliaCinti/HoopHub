package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.beans.VenueBean;
import it.uniroma2.hoophub.dao.AbstractObservableDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.utilities.CsvUtilities;
import it.uniroma2.hoophub.utilities.VenueType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV implementation of the VenueDao interface.
 */
public class VenueDaoCsv extends AbstractObservableDao implements VenueDao {

    private static final Logger logger = Logger.getLogger(VenueDaoCsv.class.getName());

    private static final String CSV_FILE_PATH = "data/venues.csv";
    private static final String[] CSV_HEADER = {"id", "name", "type", "address", "city", "max_capacity", "venue_manager_username"};

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_ADDRESS = 3;
    private static final int COL_CITY = 4;
    private static final int COL_MAX_CAPACITY = 5;
    private static final int COL_VENUE_MANAGER = 6;

    private final File csvFile;

    public VenueDaoCsv() {
        this.csvFile = new File(CSV_FILE_PATH);
        initializeCsvFile();
    }

    @Override
    public synchronized void saveVenue(VenueBean venueBean) throws DAOException {
        if (venueBean == null) {
            throw new IllegalArgumentException("VenueBean cannot be null");
        }

        int id = venueBean.getId() == 0 ? getNextVenueId() : venueBean.getId();
        venueBean.setId(id);

        String[] newRow = {
                String.valueOf(id),
                venueBean.getName(),
                venueBean.getType().name(),
                venueBean.getAddress(),
                venueBean.getCity(),
                String.valueOf(venueBean.getMaxCapacity()),
                venueBean.getVenueManagerUsername()
        };

        CsvUtilities.writeFile(csvFile, newRow);

        logger.log(Level.INFO, "Venue saved successfully: {0}", id);
        notifyObservers(DaoOperation.INSERT, "Venue", String.valueOf(id), venueBean);
    }

    @Override
    public synchronized Venue retrieveVenue(int venueId) throws DAOException {
        if (venueId <= 0) {
            throw new IllegalArgumentException("Venue ID must be positive");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == venueId) {
                return mapRowToVenue(row);
            }
        }

        return null;
    }

    @Override
    public synchronized List<Venue> retrieveAllVenues() throws DAOException {
        List<Venue> venues = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            venues.add(mapRowToVenue(data.get(i)));
        }

        logger.log(Level.INFO, "Retrieved {0} venues", venues.size());
        return venues;
    }

    @Override
    public synchronized List<Venue> retrieveVenuesByManager(VenueManager venueManager) throws DAOException {
        if (venueManager == null) {
            throw new IllegalArgumentException("VenueManager cannot be null");
        }

        List<Venue> venues = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_VENUE_MANAGER].equals(venueManager.getUsername())) {
                venues.add(mapRowToVenue(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} venues for manager {1}",
                new Object[]{venues.size(), venueManager.getUsername()});
        return venues;
    }

    @Override
    public synchronized List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }

        List<Venue> venues = new ArrayList<>();
        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (row[COL_CITY].equalsIgnoreCase(city)) {
                venues.add(mapRowToVenue(row));
            }
        }

        logger.log(Level.INFO, "Retrieved {0} venues in city {1}", new Object[]{venues.size(), city});
        return venues;
    }

    @Override
    public synchronized void updateVenue(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);

            if (Integer.parseInt(row[COL_ID]) == venue.getId()) {
                row[COL_NAME] = venue.getName();
                row[COL_TYPE] = venue.getType().name();
                row[COL_ADDRESS] = venue.getAddress();
                row[COL_CITY] = venue.getCity();
                row[COL_MAX_CAPACITY] = String.valueOf(venue.getMaxCapacity());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Venue not found for update: " + venue.getId());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Venue updated successfully: {0}", venue.getId());
        notifyObservers(DaoOperation.UPDATE, "Venue", String.valueOf(venue.getId()), venue);
    }

    @Override
    public synchronized void deleteVenue(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = 1; i < data.size(); i++) {
            if (Integer.parseInt(data.get(i)[COL_ID]) == venue.getId()) {
                data.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DAOException("Venue not found for deletion: " + venue.getId());
        }

        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        logger.log(Level.INFO, "Venue deleted successfully: {0}", venue.getId());
        notifyObservers(DaoOperation.DELETE, "Venue", String.valueOf(venue.getId()), null);
    }

    @Override
    public synchronized boolean venueExists(Venue venue) throws DAOException {
        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        List<String[]> data = CsvUtilities.readAll(csvFile);

        for (int i = 1; i < data.size(); i++) {
            if (Integer.parseInt(data.get(i)[COL_ID]) == venue.getId()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized int getNextVenueId() throws DAOException {
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

    private Venue mapRowToVenue(String[] row) throws DAOException {
        String managerUsername = row[COL_VENUE_MANAGER];
        VenueManagerDaoCsv vmDao = new VenueManagerDaoCsv();
        VenueManager manager = vmDao.retrieveVenueManager(managerUsername);

        if (manager == null) {
            throw new DAOException("VenueManager not found: " + managerUsername);
        }

        return new Venue.Builder()
                .id(Integer.parseInt(row[COL_ID]))
                .name(row[COL_NAME])
                .type(VenueType.valueOf(row[COL_TYPE]))
                .address(row[COL_ADDRESS])
                .city(row[COL_CITY])
                .maxCapacity(Integer.parseInt(row[COL_MAX_CAPACITY]))
                .venueManager(manager)
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
