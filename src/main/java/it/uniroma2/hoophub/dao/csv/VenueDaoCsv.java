package it.uniroma2.hoophub.dao.csv;

import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.helper_dao.VenueDaoHelper;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.Venue;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.dao.helper_dao.CsvUtilities;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import it.uniroma2.hoophub.enums.VenueType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * CSV implementation of {@link VenueDao}.
 *
 * <p>Supports UPSERT semantics for cross-persistence synchronization:
 * if an ID already exists, updates the row instead of creating a duplicate.</p>
 *
 * @author Elia Cinti
 * @version 1.1
 */
public class VenueDaoCsv extends AbstractCsvDao implements VenueDao {

    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venues.csv";
    private static final String[] CSV_HEADER = {"id", "name", "type", "address", "city", "max_capacity", "venue_manager_username"};

    private static final String VENUE_TEAMS_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venue_teams.csv";
    private static final String[] VENUE_TEAMS_HEADER = {"venue_id", "team_name"};

    private static final String VENUE = "Venue";

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_ADDRESS = 3;
    private static final int COL_CITY = 4;
    private static final int COL_MAX_CAPACITY = 5;
    private static final int COL_VENUE_MANAGER_USERNAME = 6;

    private static final int COL_VT_VENUE_ID = 0;
    private static final int COL_VT_TEAM_NAME = 1;

    private final File venueTeamsFile;

    public VenueDaoCsv() {
        super(CSV_FILE_PATH);
        this.venueTeamsFile = new File(VENUE_TEAMS_FILE_PATH);
        initTeamsFile();
    }

    private void initTeamsFile() {
        try {
            if (!venueTeamsFile.exists()) {
                File parentDir = venueTeamsFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    logger.log(Level.SEVERE, "Cannot create directory: {0}", parentDir.getAbsolutePath());
                    return;
                }
                CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, new ArrayList<>());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize venue_teams.csv", e);
        }
    }

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;
    }

    @Override
    public synchronized Venue saveVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);

        int id = venue.getId();

        // UPSERT: Check if ID already exists
        if (id > 0 && existsById(id)) {
            return upsertExistingVenue(venue);
        }

        // Generate new ID if not provided
        if (id == 0) {
            id = (int) getNextId(COL_ID);
        }

        Venue savedVenue = new Venue.Builder()
                .id(id)
                .name(venue.getName())
                .type(venue.getType())
                .address(venue.getAddress())
                .city(venue.getCity())
                .maxCapacity(venue.getMaxCapacity())
                .venueManager(venue.getVenueManager())
                .teams(venue.getAssociatedTeams())
                .build();

        CsvUtilities.writeFile(csvFile, venueToRow(savedVenue));

        for (TeamNBA team : savedVenue.getAssociatedTeams()) {
            saveVenueTeamInternal(id, team);
        }

        putInCache(savedVenue, id);
        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(id), savedVenue);
        return savedVenue;
    }

    /**
     * Updates an existing venue (UPSERT when ID already exists).
     */
    private Venue upsertExistingVenue(Venue venue) throws DAOException {
        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (Integer.parseInt(row[COL_ID]) == venue.getId()) {
                data.set(i, venueToRow(venue));
                found = true;
                break;
            }
        }

        if (!found) {
            CsvUtilities.writeFile(csvFile, venueToRow(venue));
        } else {
            CsvUtilities.updateFile(csvFile, CSV_HEADER, data);
        }

        // Update teams
        deleteAllVenueTeamsInternal(venue.getId());
        for (TeamNBA team : venue.getAssociatedTeams()) {
            saveVenueTeamInternal(venue.getId(), team);
        }

        putInCache(venue, venue.getId());
        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(venue.getId()), venue);
        return venue;
    }

    /**
     * Checks if a venue with the given ID already exists.
     */
    private boolean existsById(int id) throws DAOException {
        String[] row = findRowByValue(COL_ID, String.valueOf(id));
        return row != null && row.length > 0;
    }

    /**
     * Converts a Venue to a CSV row array.
     */
    private String[] venueToRow(Venue venue) {
        return new String[]{
                String.valueOf(venue.getId()),
                venue.getName(),
                venue.getType().name(),
                venue.getAddress(),
                venue.getCity(),
                String.valueOf(venue.getMaxCapacity()),
                venue.getVenueManagerUsername()
        };
    }

    @Override
    public synchronized Venue retrieveVenue(int venueId) throws DAOException {
        Venue cached = getFromCache(Venue.class, venueId);
        if (cached != null) return cached;

        String[] row = findRowByValue(COL_ID, String.valueOf(venueId));
        if (!isValidRow(row)) return null;

        Venue venue = mapRowToVenue(row);
        putInCache(venue, venueId);
        return venue;
    }

    @Override
    public synchronized List<Venue> retrieveAllVenues() throws DAOException {
        List<String[]> data = readAllDataRows();
        return processRowsToVenues(data);
    }

    @Override
    public synchronized List<Venue> retrieveVenuesByManager(String managerUsername) throws DAOException {
        List<String[]> matchingRows = findAllRowsByValue(COL_VENUE_MANAGER_USERNAME, managerUsername);
        return processRowsToVenues(matchingRows);
    }

    @Override
    public synchronized List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        List<String[]> data = readAllDataRows();
        List<String[]> matchingRows = new ArrayList<>();

        for (String[] row : data) {
            if (row.length > COL_CITY && row[COL_CITY].equalsIgnoreCase(city)) {
                matchingRows.add(row);
            }
        }
        return processRowsToVenues(matchingRows);
    }

    @Override
    public synchronized void updateVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        validatePositiveId(venue.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > COL_ID && Integer.parseInt(row[COL_ID]) == venue.getId()) {
                if (row.length > COL_VENUE_MANAGER_USERNAME) {
                    row[COL_NAME] = venue.getName();
                    row[COL_TYPE] = venue.getType().name();
                    row[COL_ADDRESS] = venue.getAddress();
                    row[COL_CITY] = venue.getCity();
                    row[COL_MAX_CAPACITY] = String.valueOf(venue.getMaxCapacity());
                    found = true;
                }
                break;
            }
        }

        if (!found) throw new DAOException("Venue not found: " + venue.getId());
        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        deleteAllVenueTeamsInternal(venue.getId());
        for (TeamNBA team : venue.getAssociatedTeams()) {
            saveVenueTeamInternal(venue.getId(), team);
        }

        putInCache(venue, venue.getId());
        notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venue.getId()), venue);
    }

    @Override
    public synchronized void deleteVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        int id = venue.getId();

        boolean found = deleteById(id, COL_ID);
        if (!found) throw new DAOException("Venue not found: " + id);

        deleteAllVenueTeamsInternal(id);
        removeFromCache(Venue.class, id);
        notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(id), null);
    }

    @Override
    public synchronized boolean venueExists(int id) throws DAOException {
        return isValidRow(findRowByValue(COL_ID, String.valueOf(id)));
    }

    @Override
    public synchronized int getNextVenueId() throws DAOException {
        return (int) getNextId(COL_ID);
    }

    // ========== TEAM METHODS ==========

    @Override
    public synchronized Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        Set<TeamNBA> teams = new HashSet<>();
        List<String[]> data = CsvUtilities.readAll(venueTeamsFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length > COL_VT_TEAM_NAME && Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId) {
                TeamNBA team = TeamNBA.robustValueOf(row[COL_VT_TEAM_NAME]);
                if (team != null) teams.add(team);
            }
        }
        return teams;
    }

    @Override
    public synchronized void saveVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        validateNotNull(venue, VENUE);
        if (team == null) throw new IllegalArgumentException("Team cannot be null");
        saveVenueTeamInternal(venue.getId(), team);
    }

    @Override
    public synchronized void deleteVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        validateNotNull(venue, VENUE);
        if (team == null) throw new IllegalArgumentException("Team cannot be null");

        List<String[]> data = CsvUtilities.readAll(venueTeamsFile);
        boolean removed = data.removeIf(row ->
                row.length > COL_VT_TEAM_NAME &&
                        Integer.parseInt(row[COL_VT_VENUE_ID]) == venue.getId() &&
                        row[COL_VT_TEAM_NAME].equals(team.name())
        );

        if (removed) CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, data);
    }

    @Override
    public synchronized void deleteAllVenueTeams(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        deleteAllVenueTeamsInternal(venue.getId());
    }

    // ========== PRIVATE HELPERS ==========

    private void saveVenueTeamInternal(int venueId, TeamNBA team) throws DAOException {
        Set<TeamNBA> existing = retrieveVenueTeams(venueId);
        if (!existing.contains(team)) {
            String[] row = {String.valueOf(venueId), team.name()};
            CsvUtilities.writeFile(venueTeamsFile, row);
        }
    }

    private void deleteAllVenueTeamsInternal(int venueId) throws DAOException {
        List<String[]> allTeams = CsvUtilities.readAll(venueTeamsFile);

        if (!allTeams.isEmpty()) {
            allTeams.removeFirst();
            allTeams.removeIf(row ->
                    row.length > COL_VT_VENUE_ID &&
                            Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId
            );
            CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, allTeams);
        }
    }

    private List<Venue> processRowsToVenues(List<String[]> rows) throws DAOException {
        List<Venue> venues = new ArrayList<>();
        for (String[] row : rows) {
            Venue v = resolveVenueFromRow(row);
            if (v != null) venues.add(v);
        }
        return venues;
    }

    private Venue resolveVenueFromRow(String[] row) throws DAOException {
        if (!isValidRow(row)) return null;

        try {
            int id = Integer.parseInt(row[COL_ID]);
            Venue cached = getFromCache(Venue.class, id);
            if (cached != null) return cached;

            Venue v = mapRowToVenue(row);
            putInCache(v, id);
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Venue mapRowToVenue(String[] row) throws DAOException {
        if (row == null || row.length <= COL_VENUE_MANAGER_USERNAME) {
            throw new DAOException("Invalid CSV row data for Venue (missing columns)");
        }

        int id = Integer.parseInt(row[COL_ID]);
        String managerUsername = row[COL_VENUE_MANAGER_USERNAME];
        String key = "Venue:" + id;

        if (DaoLoadingContext.isLoading(key)) {
            return createMinimalVenue(id, row, managerUsername);
        }

        DaoLoadingContext.startLoading(key);
        try {
            VenueManager manager = VenueDaoHelper.loadVenueManager(managerUsername);
            Set<TeamNBA> teams = retrieveVenueTeams(id);

            return new Venue.Builder()
                    .id(id)
                    .name(row[COL_NAME])
                    .type(VenueType.valueOf(row[COL_TYPE]))
                    .address(row[COL_ADDRESS])
                    .city(row[COL_CITY])
                    .maxCapacity(Integer.parseInt(row[COL_MAX_CAPACITY]))
                    .venueManager(manager)
                    .teams(teams)
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    private Venue createMinimalVenue(int id, String[] row, String managerUsername) throws DAOException {
        VenueManager manager = DaoFactoryFacade.getInstance().getVenueManagerDao().retrieveVenueManager(managerUsername);
        Set<TeamNBA> teams = retrieveVenueTeams(id);

        return new Venue.Builder()
                .id(id)
                .name(row[COL_NAME])
                .type(VenueType.valueOf(row[COL_TYPE]))
                .address(row[COL_ADDRESS])
                .city(row[COL_CITY])
                .maxCapacity(Integer.parseInt(row[COL_MAX_CAPACITY]))
                .venueManager(manager)
                .teams(teams)
                .build();
    }
}