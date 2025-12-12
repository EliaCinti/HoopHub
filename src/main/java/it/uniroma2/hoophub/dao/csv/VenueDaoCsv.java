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

                // FIX: Controlliamo il risultato di mkdirs()
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        logger.log(Level.SEVERE, "Impossible to create directory for venue teams: {0}", parentDir.getAbsolutePath());
                        // Se non riusciamo a creare la cartella, inutile provare a scrivere il file
                        return;
                    }
                }

                // Se siamo qui, la cartella esiste (o è stata creata). Creiamo il file vuoto con header.
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
        // 1. Validazione
        validateNotNull(venue, VENUE);
        // Il Model garantisce già che i team non siano vuoti

        // 2. Generazione ID
        int id = venue.getId();
        if (id == 0) {
            id = (int) getNextId(COL_ID);
        }

        // 3. Creazione Oggetto con ID (Immutabile)
        Venue savedVenue = new Venue.Builder()
                .id(id)
                .name(venue.getName())
                .type(venue.getType())
                .address(venue.getAddress())
                .city(venue.getCity())
                .maxCapacity(venue.getMaxCapacity())
                .venueManager(venue.getVenueManager())
                .teams(venue.getAssociatedTeams()) // Copia i team!
                .build();

        // 4. Scrittura Venue (venues.csv)
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

        // 5. Scrittura Teams (venue_teams.csv)
        // Scriviamo tutti i team associati
        for (TeamNBA team : savedVenue.getAssociatedTeams()) {
            saveVenueTeamInternal(id, team);
        }

        // 6. Notifica e Ritorno
        notifyObservers(DaoOperation.INSERT, VENUE, String.valueOf(id), savedVenue);
        return savedVenue;
    }

    @Override
    public synchronized void updateVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        validatePositiveId(venue.getId());

        List<String[]> data = CsvUtilities.readAll(csvFile);
        boolean found = false;

        // 1. Update Venue (venues.csv)
        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            if (Integer.parseInt(row[COL_ID]) == venue.getId()) {
                row[COL_NAME] = venue.getName();
                row[COL_TYPE] = venue.getType().name();
                row[COL_ADDRESS] = venue.getAddress();
                row[COL_CITY] = venue.getCity();
                row[COL_MAX_CAPACITY] = String.valueOf(venue.getMaxCapacity());
                // Manager username usually not updated here
                found = true;
                break;
            }
        }

        if (!found) throw new DAOException("Venue not found: " + venue.getId());
        CsvUtilities.updateFile(csvFile, CSV_HEADER, data);

        // 2. Update Teams (venue_teams.csv)
        // Strategia: Delete All + Insert New (per allineare CSV al Model)
        deleteAllVenueTeamsInternal(venue.getId());

        for (TeamNBA team : venue.getAssociatedTeams()) {
            saveVenueTeamInternal(venue.getId(), team);
        }

        logger.log(Level.INFO, "Venue updated: {0}", venue.getId());
        notifyObservers(DaoOperation.UPDATE, VENUE, String.valueOf(venue.getId()), venue);
    }

    @Override
    public synchronized void deleteVenue(Venue venue) throws DAOException {
        validateNotNull(venue, VENUE);
        int id = venue.getId();

        // 1. Delete Venue (venues.csv)
        boolean found = deleteById(id, COL_ID);
        if (!found) throw new DAOException("Venue not found: " + id);

        // 2. Delete Teams (Cascade Manuale per CSV)
        deleteAllVenueTeamsInternal(id);

        logger.log(Level.INFO, "Venue deleted: {0}", id);
        notifyObservers(DaoOperation.DELETE, VENUE, String.valueOf(id), null);
    }

    // ========== READ OPERATIONS ==========

    @Override
    public synchronized Venue retrieveVenue(int venueId) throws DAOException {
        String[] row = findRowByValue(COL_ID, String.valueOf(venueId));
        if (row == null) return null;
        return mapRowToVenue(row);
    }

    @Override
    public synchronized List<Venue> retrieveAllVenues() throws DAOException {
        List<String[]> data = readAllDataRows();
        List<Venue> venues = new ArrayList<>();
        for (String[] row : data) {
            venues.add(mapRowToVenue(row));
        }
        return venues;
    }

    // ... retrieveByManager e retrieveByCity rimangono simili, chiamano mapRowToVenue ...
    @Override
    public synchronized List<Venue> retrieveVenuesByManager(String managerUsername) throws DAOException {
        List<String[]> data = readAllDataRows();
        List<Venue> venues = new ArrayList<>();
        for (String[] row : data) {
            if (row[COL_VENUE_MANAGER_USERNAME].equals(managerUsername)) {
                venues.add(mapRowToVenue(row));
            }
        }
        return venues;
    }

    @Override
    public synchronized List<Venue> retrieveVenuesByCity(String city) throws DAOException {
        List<String[]> data = readAllDataRows();
        List<Venue> venues = new ArrayList<>();
        for (String[] row : data) {
            if (row[COL_CITY].equalsIgnoreCase(city)) {
                venues.add(mapRowToVenue(row));
            }
        }
        return venues;
    }

    // ========== TEAM METHODS (Internal & Public) ==========

    // Helper per salvare team (usato da saveVenue e updateVenue)
    private void saveVenueTeamInternal(int venueId, TeamNBA team) throws DAOException {
        // Controlla duplicati prima di scrivere
        Set<TeamNBA> existing = retrieveVenueTeams(venueId);
        if (!existing.contains(team)) {
            String[] row = {String.valueOf(venueId), team.name()};
            CsvUtilities.writeFile(venueTeamsFile, row);
        }
    }

    // Helper per cancellare tutti i team (usato da deleteVenue e updateVenue)
    private void deleteAllVenueTeamsInternal(int venueId) throws DAOException {
        List<String[]> allTeams = CsvUtilities.readAll(venueTeamsFile);
        // Rimuove se venue_id corrisponde
        allTeams.removeIf(row -> row.length > 0 && Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId);
        CsvUtilities.updateFile(venueTeamsFile, VENUE_TEAMS_HEADER, allTeams);
    }

    @Override
    public synchronized Set<TeamNBA> retrieveVenueTeams(int venueId) throws DAOException {
        Set<TeamNBA> teams = new HashSet<>();
        List<String[]> data = CsvUtilities.readAll(venueTeamsFile);

        for (int i = CsvDaoConstants.FIRST_DATA_ROW; i < data.size(); i++) {
            String[] row = data.get(i);
            // Controllo indice per evitare IndexOutOfBounds
            if (row.length > COL_VT_TEAM_NAME && Integer.parseInt(row[COL_VT_VENUE_ID]) == venueId) {

                // FIX: Usa robustValueOf che gestisce sigle, nomi completi e enum
                TeamNBA team = TeamNBA.robustValueOf(row[COL_VT_TEAM_NAME]);

                if (team != null) {
                    teams.add(team);
                } else {
                    // Logghiamo un warning invece di crashare o ignorare silenziosamente
                    logger.log(Level.WARNING, "Team non riconosciuto nel CSV per venue {0}: {1}",
                            new Object[]{venueId, row[COL_VT_TEAM_NAME]});
                }
            }
        }
        return teams;
    }

    // Esposizione pubblica dei metodi team (delegano agli internal o reimplementano se serve singolo)
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
                row.length > 0 &&
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

    // ========== HELPER MAPPING ==========

    private Venue mapRowToVenue(String[] row) throws DAOException {
        int id = Integer.parseInt(row[COL_ID]);
        String managerUsername = row[COL_VENUE_MANAGER_USERNAME];
        String key = "Venue:" + id;

        if (DaoLoadingContext.isLoading(key)) {
            // Break Cycle: Minimal Venue
            return createMinimalVenue(id, row, managerUsername);
        }

        DaoLoadingContext.startLoading(key);
        try {
            // 1. Load Manager
            VenueManager manager = VenueDaoHelper.loadVenueManager(managerUsername);

            // 2. Load Teams (PRIMA DEL BUILD)
            Set<TeamNBA> teams = retrieveVenueTeams(id);

            // 3. Build Venue
            return new Venue.Builder()
                    .id(id)
                    .name(row[COL_NAME])
                    .type(VenueType.valueOf(row[COL_TYPE]))
                    .address(row[COL_ADDRESS])
                    .city(row[COL_CITY])
                    .maxCapacity(Integer.parseInt(row[COL_MAX_CAPACITY]))
                    .venueManager(manager)
                    .teams(teams) // Passa i team qui!
                    .build();
        } finally {
            DaoLoadingContext.finishLoading(key);
        }
    }

    private Venue createMinimalVenue(int id, String[] row, String managerUsername) throws DAOException {
        // Stub Manager per rompere il ciclo
        DaoFactoryFacade dao = DaoFactoryFacade.getInstance();
        VenueManager manager = dao.getVenueManagerDao().retrieveVenueManager(managerUsername);

        // Carica team anche per il minimal (non creano cicli)
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

    @Override
    public synchronized boolean venueExists(int id) throws DAOException {
        return findRowByValue(COL_ID, String.valueOf(id)) != null;
    }
    @Override
    public synchronized int getNextVenueId() throws DAOException {
        return (int) getNextId(COL_ID);
    }
}