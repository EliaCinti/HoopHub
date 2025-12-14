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
 * CSV implementation of VenueDao.
 */
public class VenueDaoCsv extends AbstractCsvDao implements VenueDao {

    // ========== CSV CONFIGURATION ==========
    private static final String CSV_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venues.csv";
    private static final String[] CSV_HEADER = {"id", "name", "type", "address", "city", "max_capacity", "venue_manager_username"};

    // Configurazione file teams
    private static final String VENUE_TEAMS_FILE_PATH = CsvDaoConstants.CSV_BASE_DIR + "venue_teams.csv";
    private static final String[] VENUE_TEAMS_HEADER = {"venue_id", "team_name"};

    private static final String VENUE = "Venue";

    // Indici
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_ADDRESS = 3;
    private static final int COL_CITY = 4;
    private static final int COL_MAX_CAPACITY = 5;
    private static final int COL_VENUE_MANAGER_USERNAME = 6;

    // Indici file teams
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
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        logger.log(Level.SEVERE, "Impossible to create directory for venue teams: {0}", parentDir.getAbsolutePath());
                        return;
                    }
                }
                CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, new ArrayList<>());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize venue_teams.csv", e);
        }
    }

    @Override
    protected String[] getHeader() { return CSV_HEADER; }

    // ========== CRUD OPERATIONS (Model-First) ==========

    @Override
    public synchronized Venue saveVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);

        int id = venue.getId();
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

        String[] row = {
                String.valueOf(id),
                savedVenue.getName(),
                savedVenue.getType().name(),
                savedVenue.getAddress(),
                savedVenue.getCity(),
                String.valueOf(savedVenue.getMaxCapacity()),
                savedVenue.getVenueManagerUsername()
        };
        CsvUtilities.writeFile(csvFile, row);

        for (TeamNBA team : savedVenue.getAssociatedTeams()) {
            saveVenueTeamInternal(id, team);
        }

        // CACHE: Write-Through
        putInCache(savedVenue, id);

        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(id), savedVenue);
        return savedVenue;
    }

    @Override
    public synchronized void updateVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        validatePositiveId(venue.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);

            // FIX: Controllo validità riga prima di accedere all'ID
            if (row.length > COL_ID && Integer.parseInt(row[COL_ID]) == venue.getId()) {

                // FIX: Unico blocco If-Else per evitare doppi break
                if (row.length > COL_VENUE_MANAGER_USERNAME) {
                    // Riga valida: aggiorniamo
                    row[COL_NAME] = venue.getName();
                    row[COL_TYPE] = venue.getType().name();
                    row[COL_ADDRESS] = venue.getAddress();
                    row[COL_CITY] = venue.getCity();
                    row[COL_MAX_CAPACITY] = String.valueOf(venue.getMaxCapacity());
                    found = true;
                } else {
                    // Riga corrotta: logghiamo solo
                    logger.log(Level.WARNING, "Skipping update on corrupted Venue row ID {0}", venue.getId());
                }

                // UNICO BREAK: Abbiamo trovato l'ID (valido o no), usciamo dal ciclo.
                break;
            }
        }

        if (!found) throw new DAOException("Venue not found: " + venue.getId());
        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        // Aggiornamento Teams (Full Replace)
        deleteAllVenueTeamsInternal(venue.getId());
        for (TeamNBA team : venue.getAssociatedTeams()) {
            saveVenueTeamInternal(venue.getId(), team);
        }

        // CACHE: Write-Through
        putInCache(venue, venue.getId());

        logger.log(Level.INFO, "Venue updated: {0}", venue.getId());
        notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venue.getId()), venue);
    }

    @Override
    public synchronized void deleteVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        int id = venue.getId();

        boolean found = deleteById(id, COL_ID);
        if (!found) throw new DAOException("Venue not found: " + id);

        deleteAllVenueTeamsInternal(id);

        // CACHE: Remove
        removeFromCache(Venue.class, id);

        logger.log(Level.INFO, "Venue deleted: {0}", id);
        notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(id), null);
    }

    // ========== READ OPERATIONS (Refactored) ==========

    @Override
    public synchronized Venue retrieveVenue(int venueId) throws DAOException {
        // 1. CACHE CHECK
        Venue cachedVenue = getFromCache(Venue.class, venueId);
        if (cachedVenue != null) {
            return cachedVenue;
        }

        // 2. Load from file
        String[] row = findRowByValue(COL_ID, String.valueOf(venueId));

        // FIX: Usa isValidRow
        if (!isValidRow(row)) {
            return null;
        }

        Venue venue = mapRowToVenue(row);

        // 3. CACHE PUT
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
            // FIX: Controllo validità riga
            if (row.length > COL_CITY && row[COL_CITY].equalsIgnoreCase(city)) {
                matchingRows.add(row);
            }
        }
        return processRowsToVenues(matchingRows);
    }

    // ========== TEAM METHODS (Internal & Public) ==========

    private void saveVenueTeamInternal(int venueId, TeamNBA team) throws DAOException {
        Set<TeamNBA> existing = retrieveVenueTeams(venueId);
        if (!existing.contains(team)) {
            String[] row = {String.valueOf(venueId), team.name()};
            CsvUtilities.writeFile(venueTeamsFile, row);
        }
    }

    @Override
    public synchronized Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        Set<TeamNBA> teams = new HashSet<>();
        List<String[]> data = CsvUtilities.readAll(venueTeamsFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            // FIX: Controllo validità riga Teams
            if (row.length > COL_VT_TEAM_NAME && Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId) {
                TeamNBA team = TeamNBA.robustValueOf(row[COL_VT_TEAM_NAME]);
                if (team != null) {
                    teams.add(team);
                } else {
                    logger.log(Level.WARNING, "Team non riconosciuto nel CSV per venue {0}: {1}",
                            new Object[]{venueId, row[COL_VT_TEAM_NAME]});
                }
            }
        }
        return teams;
    }

    @Override
    public synchronized void saveVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        validateNotNull(venue, VENUE);
        if(team == null) throw new IllegalArgumentException("Team cannot be null");
        saveVenueTeamInternal(venue.getId(), team);
    }

    @Override
    public synchronized void deleteVenueTeam(Venue venue, TeamNBA team) throws DAOException {
        validateNotNull(venue, VENUE);
        if(team == null) throw new IllegalArgumentException("Team cannot be null");

        List<String[]> data = CsvUtilities.readAll(venueTeamsFile);
        boolean removed = data.removeIf(row ->
                row.length > COL_VT_TEAM_NAME && // FIX: Check length
                        Integer.parseInt(row[COL_VT_VENUE_ID]) == venue.getId() &&
                        row[COL_VT_TEAM_NAME].equals(team.name())
        );

        if(removed) CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, data);
    }

    @Override
    public synchronized void deleteAllVenueTeams(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        deleteAllVenueTeamsInternal(venue.getId());
    }

    // ========== HELPER MAPPING & PROCESSING ==========

    private List<Venue> processRowsToVenues(List<String[]> rows) throws DAOException {
        List<Venue> venues = new ArrayList<>();
        for (String[] row : rows) {
            Venue v = resolveVenueFromRow(row);
            if (v != null) {
                venues.add(v);
            }
        }
        return venues;
    }

    private Venue resolveVenueFromRow(String[] row) throws DAOException {
        // FIX: Check robusto
        if (!isValidRow(row)) return null;

        try {
            int id = Integer.parseInt(row[COL_ID]);
            Venue cached = getFromCache(Venue.class, id);
            if (cached != null) {
                return cached;
            }
            Venue v = mapRowToVenue(row);
            putInCache(v, id);
            return v;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid venue ID in CSV row");
            return null;
        }
    }

    private Venue mapRowToVenue(String[] row) throws DAOException {
        // FIX: Controllo che ci siano TUTTE le colonne necessarie, inclusa manager username (index 6)
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
        DaoFactoryFacade dao = DaoFactoryFacade.getInstance();
        VenueManager manager = dao.getVenueManagerDao().retrieveVenueManager(managerUsername);
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

    private void deleteAllVenueTeamsInternal(int venueId) throws DAOException {
        List<String[]> allTeams = CsvUtilities.readAll(venueTeamsFile);

        // REFACTORING: Rimosso codice inutile 'toWrite'.
        if (!allTeams.isEmpty()) {
            // Rimuoviamo la riga header dalla lista letta, perché 'updateFile' la aggiungerà di nuovo.
            allTeams.removeFirst();

            // Rimuoviamo i team della venue
            allTeams.removeIf(row ->
                    row.length > COL_VT_VENUE_ID &&
                            Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId
            );

            // Scriviamo solo i dati (l'header viene messo da CsvUtilities in base alla costante passata)
            CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, allTeams);
        }
    }

    @Override
    public synchronized boolean venueExists(int id) throws DAOException {
        // FIX: Usa isValidRow
        return isValidRow(findRowByValue(COL_ID, String.valueOf(id)));
    }

    @Override
    public synchronized int getNextVenueId() throws DAOException {
        return (int) getNextId(COL_ID);
    }
}