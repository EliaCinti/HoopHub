package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.VenueManager;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveAllSyncTest {

    private DaoFactoryFacade factory;
    // Usiamo una lista per tenere traccia dei dati di test e pulirli alla fine
    private final List<String> testUsernames = List.of("test_all_1", "test_all_2", "test_all_3");

    @BeforeEach
    void setUp() {
        GlobalCache.getInstance().clearAll();
        DaoLoadingContext.clear();
        factory = DaoFactoryFacade.getInstance();

        // Pulizia preventiva sicura (su entrambe le persistenze)
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // Pulizia finale
        cleanupTestData();
        GlobalCache.getInstance().clearAll();
    }

    @Test
    void testRetrieveAllSynchronization() throws DAOException {
        // --- 1. INSERIMENTO IN CSV ---
        System.out.println("=== 1. Salvataggio multiplo su CSV ===");
        factory.setPersistenceType(PersistenceType.CSV);

        for (String username : testUsernames) {
            VenueManager vm = new VenueManager.Builder()
                    .username(username)
                    .password("Pass123")
                    .fullName("Test User " + username)
                    .gender("Other")
                    .companyName("Test Corp " + username)
                    .phoneNumber("3330000000")
                    .managedVenues(Collections.emptyList())
                    .build();
            factory.getVenueManagerDao().saveVenueManager(vm);
        }

        // --- 2. VERIFICA SU MYSQL (RetrieveAll) ---
        System.out.println("=== 2. Verifica retrieveAll() su MySQL ===");
        factory.setPersistenceType(PersistenceType.MYSQL);

        // Puliamo la cache per costringere il DAO a fare una SELECT vera sul DB
        GlobalCache.getInstance().clearAll();

        List<VenueManager> mysqlManagers = factory.getVenueManagerDao().retrieveAllVenueManagers();

        // Verifichiamo che la lista contenga i nostri 3 utenti
        long countMysql = mysqlManagers.stream()
                .filter(vm -> testUsernames.contains(vm.getUsername()))
                .count();

        assertEquals(3, countMysql, "MySQL deve contenere tutti e 3 i manager salvati via CSV!");
        System.out.println("-> OK: MySQL ha trovato i 3 manager.");

        // --- 3. CANCELLAZIONE SU MYSQL ---
        System.out.println("=== 3. Cancellazione di 'test_all_1' su MySQL ===");
        // Siamo già in MySQL. Cancelliamo il primo.
        VenueManager toDelete = factory.getVenueManagerDao().retrieveVenueManager(testUsernames.getFirst());
        assertNotNull(toDelete, "Dovremmo trovare l'utente da cancellare");

        factory.getVenueManagerDao().deleteVenueManager(toDelete);

        // --- 4. VERIFICA SU CSV (RetrieveAll) ---
        System.out.println("=== 4. Verifica retrieveAll() su CSV ===");
        factory.setPersistenceType(PersistenceType.CSV);

        // Puliamo la cache per costringere a rileggere il file CSV aggiornato dall'Observer
        GlobalCache.getInstance().clearAll();

        List<VenueManager> csvManagers = factory.getVenueManagerDao().retrieveAllVenueManagers();

        // Verifichiamo che ora ce ne siano solo 2
        long countCsv = csvManagers.stream()
                .filter(vm -> testUsernames.contains(vm.getUsername()))
                .count();

        assertEquals(2, countCsv, "Il CSV deve riflettere la cancellazione avvenuta su MySQL!");

        // Verifica specifica: test_all_1 non deve esserci, gli altri sì
        boolean hasUser1 = csvManagers.stream().anyMatch(vm -> vm.getUsername().equals(testUsernames.getFirst()));
        boolean hasUser2 = csvManagers.stream().anyMatch(vm -> vm.getUsername().equals(testUsernames.get(1)));

        assertFalse(hasUser1, "L'utente 1 cancellato non deve esistere nel CSV");
        assertTrue(hasUser2, "L'utente 2 deve ancora esistere");

        System.out.println("-> OK: CSV aggiornato correttamente dopo la delete MySQL.");
    }

    /**
     * Helper per pulire i dati di test da entrambe le persistenze
     * per evitare errori "Duplicate entry" nei lanci successivi.
     */
    private void cleanupTestData() {
        try {
            // Pulisci MySQL
            factory.setPersistenceType(PersistenceType.MYSQL);
            for (String username : testUsernames) {
                VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
                if (vm != null) factory.getVenueManagerDao().deleteVenueManager(vm);
            }

            // Pulisci CSV
            factory.setPersistenceType(PersistenceType.CSV);
            for (String username : testUsernames) {
                VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
                if (vm != null) factory.getVenueManagerDao().deleteVenueManager(vm);
            }
        } catch (Exception e) {
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }
}