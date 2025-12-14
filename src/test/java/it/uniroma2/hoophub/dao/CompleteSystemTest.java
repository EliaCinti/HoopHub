package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.enums.*;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.utilities.DaoLoadingContext;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MASTER TEST SUITE
 * Copre tutte le DAO, entrambe le persistenze (CSV/MySQL), la Sincronizzazione e la Cache.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompleteSystemTest {

    private static DaoFactoryFacade factory;

    @BeforeAll
    static void init() {
        factory = DaoFactoryFacade.getInstance();
    }

    @BeforeEach
    void setUp() {
        // Pulizia totale prima di ogni test per garantire isolamento
        GlobalCache.getInstance().clearAll();
        DaoLoadingContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Pulizia cache finale
        GlobalCache.getInstance().clearAll();
    }

    // =================================================================================
    // 1. TEST COMPLETO CSV (Ciclo di vita di tutte le entità)
    // =================================================================================
    @Test
    @Order(1)
    void testFullLifecycleCSV() throws DAOException {
        System.out.println("\n=== 1. TEST FULL LIFECYCLE: CSV ===");
        factory.setPersistenceType(PersistenceType.CSV);
        executeFullSystemLifecycle("csv_test");
    }

    // =================================================================================
    // 2. TEST COMPLETO MYSQL (Ciclo di vita di tutte le entità)
    // =================================================================================
    @Test
    @Order(2)
    void testFullLifecycleMySQL() throws DAOException {
        System.out.println("\n=== 2. TEST FULL LIFECYCLE: MYSQL ===");
        factory.setPersistenceType(PersistenceType.MYSQL);
        executeFullSystemLifecycle("sql_test");
    }

    // =================================================================================
    // 3. TEST SINCRONIZZAZIONE (Cross-Persistence)
    // =================================================================================
    @Test
    @Order(3)
    void testSynchronization() throws DAOException {
        System.out.println("\n=== 3. TEST SINCRONIZZAZIONE (CSV <-> MYSQL) ===");

        String username = "sync_master";
        cleanUpUser(username); // Pulizia preventiva

        // A. INSERT su CSV -> Check MySQL
        factory.setPersistenceType(PersistenceType.CSV);

        // FIX: Aggiunto birthday
        Fan fan = new Fan.Builder()
                .username(username)
                .password("SyncPass1")
                .fullName("Sync Master")
                .gender("Male")
                .favTeam(TeamNBA.CHICAGO_BULLS)
                .birthday(LocalDate.of(1990, 1, 1)) // DATI MANCANTI AGGIUNTI
                .build();

        factory.getFanDao().saveFan(fan);
        System.out.println("-> Salvato Fan su CSV.");

        factory.setPersistenceType(PersistenceType.MYSQL);
        Fan fanSql = factory.getFanDao().retrieveFan(username);
        assertNotNull(fanSql, "SYNC FAIL: Il fan salvato su CSV non esiste su MySQL!");
        assertEquals("CHICAGO_BULLS", fanSql.getFavTeam().name());
        System.out.println("-> Check MySQL OK.");

        // B. UPDATE su MySQL -> Check CSV
        // FIX: Aggiunto birthday
        Fan toUpdate = new Fan.Builder()
                .username(username)
                .password("SyncPass1")
                .fullName("Sync Master Updated")
                .gender("Male")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS) // Cambio squadra
                .birthday(LocalDate.of(1990, 1, 1)) // DATI MANCANTI AGGIUNTI
                .build();

        factory.getFanDao().updateFan(toUpdate);
        System.out.println("-> Aggiornato Fan su MySQL.");

        factory.setPersistenceType(PersistenceType.CSV);
        GlobalCache.getInstance().clearAll(); // Forzo rilettura da file

        Fan fanCsv = factory.getFanDao().retrieveFan(username);
        assertEquals("LOS_ANGELES_LAKERS", fanCsv.getFavTeam().name(), "SYNC FAIL: L'aggiornamento MySQL non è arrivato al CSV!");
        System.out.println("-> Check CSV OK.");

        // C. DELETE su CSV -> Check MySQL
        factory.getFanDao().deleteFan(fanCsv);
        System.out.println("-> Cancellato Fan su CSV.");

        factory.setPersistenceType(PersistenceType.MYSQL);
        Fan fanDeleted = factory.getFanDao().retrieveFan(username);
        assertNull(fanDeleted, "SYNC FAIL: Fan ancora presente su MySQL dopo cancellazione CSV!");
        System.out.println("-> Check Eliminazione MySQL OK.");

        // Pulizia finale CSV (per sicurezza)
        factory.setPersistenceType(PersistenceType.CSV);
        if(factory.getFanDao().retrieveFan(username) != null) {
            factory.getFanDao().deleteFan(fanCsv);
        }
    }

    // =================================================================================
    // 4. TEST CACHE (Identity & Performance)
    // =================================================================================
    @Test
    @Order(4)
    void testCachePerformance() throws DAOException {
        System.out.println("\n=== 4. TEST CACHE ===");
        factory.setPersistenceType(PersistenceType.CSV);
        String username = "cache_king";
        cleanUpUser(username);

        // FIX: Aggiunti team e birthday che mancavano e causavano il crash
        Fan fan = new Fan.Builder()
                .username(username)
                .password("123")
                .fullName("Cache King")
                .gender("Male")
                .favTeam(TeamNBA.BOSTON_CELTICS) // FIX
                .birthday(LocalDate.of(2000, 5, 20)) // FIX
                .build();

        factory.getFanDao().saveFan(fan);

        // Prima lettura (popola cache se non c'è, o usa quella del save)
        Fan ref1 = factory.getFanDao().retrieveFan(username);

        // Seconda lettura
        Fan ref2 = factory.getFanDao().retrieveFan(username);

        assertSame(ref1, ref2, "CACHE FAIL: Retrieve successivi devono restituire lo STESSO oggetto in memoria!");
        System.out.println("-> Identity Check OK (Oggetti identici).");

        factory.getFanDao().deleteFan(ref1);

        // Check post-delete
        assertNull(factory.getFanDao().retrieveFan(username), "CACHE FAIL: Oggetto cancellato deve sparire dalla cache!");
        System.out.println("-> Eviction Check OK.");
    }


    // =================================================================================
    // HELPER: LOGICA DI TEST GENERICA (Usata sia per CSV che MySQL)
    // =================================================================================
    private void executeFullSystemLifecycle(String suffix) throws DAOException {
        // Nomi univoci per evitare conflitti
        String fanUser = "fan_" + suffix;
        String mgrUser = "mgr_" + suffix;

        // Pulizia preventiva
        cleanUpUser(fanUser);
        cleanUpUser(mgrUser);

        // --- 1. FAN DAO ---
        System.out.println("   [1] Testing FanDao...");

        // FIX: Aggiunto birthday
        Fan fan = new Fan.Builder()
                .username(fanUser)
                .password("Pass123")
                .fullName("Test Fan")
                .gender("Male")
                .favTeam(TeamNBA.BOSTON_CELTICS)
                .birthday(LocalDate.of(1998, 12, 25)) // DATI MANCANTI AGGIUNTI
                .build();
        factory.getFanDao().saveFan(fan);

        Fan retrievedFan = factory.getFanDao().retrieveFan(fanUser);
        assertNotNull(retrievedFan);
        assertEquals(TeamNBA.BOSTON_CELTICS, retrievedFan.getFavTeam());

        // --- 2. VENUE MANAGER DAO ---
        System.out.println("   [2] Testing VenueManagerDao...");

        // FIX: Aggiunto birthday (se il manager lo richiede)
        VenueManager vm = new VenueManager.Builder()
                .username(mgrUser)
                .password("Pass123")
                .fullName("Test Manager")
                .gender("Female")
                .companyName("Test Arena Corp")
                .phoneNumber("3331234567")
                .managedVenues(Collections.emptyList())
                .build(); // Se anche VenueManager richiede birthday, aggiungilo qui: .birthday(...)

        factory.getVenueManagerDao().saveVenueManager(vm);

        VenueManager retrievedVm = factory.getVenueManagerDao().retrieveVenueManager(mgrUser);
        assertNotNull(retrievedVm);
        assertEquals("Test Arena Corp", retrievedVm.getCompanyName());

        // --- 3. VENUE DAO ---
        System.out.println("   [3] Testing VenueDao...");
        int venueId = 1000 + (suffix.equals("csv_test") ? 1 : 2); // ID diverso per sicurezza

        // Pulizia preventiva Venue (se rimasta sporca)
        if(factory.getVenueDao().venueExists(venueId)) {
            Venue v = factory.getVenueDao().retrieveVenue(venueId);
            factory.getVenueDao().deleteVenue(v);
        }

        Venue venue = new Venue.Builder()
                .id(venueId)
                .name("Grand Arena " + suffix)
                .type(VenueType.LOUNGE)
                .address("Via Roma 1")
                .city("Rome")
                .maxCapacity(1000)
                .venueManager(retrievedVm)
                .teams(Set.of(TeamNBA.LOS_ANGELES_LAKERS, TeamNBA.LA_CLIPPERS))
                .build();
        factory.getVenueDao().saveVenue(venue);

        Venue retrievedVenue = factory.getVenueDao().retrieveVenue(venueId);
        assertNotNull(retrievedVenue);
        assertEquals(2, retrievedVenue.getAssociatedTeams().size());

        // Test RetrieveByCity
        List<Venue> cityVenues = factory.getVenueDao().retrieveVenuesByCity("Rome");
        assertTrue(cityVenues.stream().anyMatch(v -> v.getId() == venueId));

        // --- 4. BOOKING DAO ---
        System.out.println("   [4] Testing BookingDao...");
        Booking booking = new Booking.Builder(0, LocalDate.now().plusDays(1), LocalTime.of(20, 0),
                TeamNBA.LOS_ANGELES_LAKERS, TeamNBA.LA_CLIPPERS, retrievedVenue, retrievedFan)
                .status(BookingStatus.PENDING)
                .notified(false)
                .build();

        booking = factory.getBookingDao().saveBooking(booking); // Assegna ID
        int bookingId = booking.getId();
        assertTrue(bookingId > 0);

        Booking retrievedBooking = factory.getBookingDao().retrieveBooking(bookingId);
        assertNotNull(retrievedBooking);
        assertEquals(BookingStatus.PENDING, retrievedBooking.getStatus());

        // Update Booking
        Booking toUpdate = new Booking.Builder(bookingId, booking.getGameDate(), booking.getGameTime(),
                booking.getHomeTeam(), booking.getAwayTeam(), booking.getVenue(), booking.getFan())
                .status(BookingStatus.CONFIRMED)
                .notified(false)
                .build();
        factory.getBookingDao().updateBooking(toUpdate);

        assertEquals(BookingStatus.CONFIRMED, factory.getBookingDao().retrieveBooking(bookingId).getStatus());

        // --- 5. NOTIFICATION DAO ---
        System.out.println("   [5] Testing NotificationDao...");
        Notification notif = new Notification.Builder()
                .id(0) // Auto-increment
                .username(fanUser)
                .userType(UserType.FAN)
                .type(NotificationType.BOOKING_APPROVED)
                .message("Your booking is confirmed!")
                .bookingId(bookingId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notif = factory.getNotificationDao().saveNotification(notif);
        int notifId = notif.getId();

        List<Notification> unread = factory.getNotificationDao().getUnreadNotificationsForUser(fanUser, UserType.FAN);
        assertTrue(unread.stream().anyMatch(n -> n.getId() == notifId));

        factory.getNotificationDao().markAllAsReadForUser(fanUser, UserType.FAN);
        // Pulizia cache per verificare lettura disco
        GlobalCache.getInstance().clearAll();

        assertEquals(0, factory.getNotificationDao().getUnreadNotificationsForUser(fanUser, UserType.FAN).size());

        // --- 6. CLEANUP (Reverse Order) ---
        System.out.println("   [6] Cleanup...");

        // Delete Notification
        factory.getNotificationDao().deleteNotification(notif);
        assertNull(factory.getNotificationDao().retrieveNotification(notifId));

        // Delete Booking
        factory.getBookingDao().deleteBooking(toUpdate);
        assertNull(factory.getBookingDao().retrieveBooking(bookingId));

        // Delete Venue
        factory.getVenueDao().deleteVenue(retrievedVenue);
        assertNull(factory.getVenueDao().retrieveVenue(venueId));

        // Delete Users
        factory.getFanDao().deleteFan(retrievedFan);
        factory.getVenueManagerDao().deleteVenueManager(retrievedVm);

        System.out.println("   -> Lifecycle " + suffix + " completato con successo!");
    }

    private void cleanUpUser(String username) {
        try {
            // Tenta pulizia su MySQL
            factory.setPersistenceType(PersistenceType.MYSQL);
            deleteUserIfFound(username);

            // Tenta pulizia su CSV
            factory.setPersistenceType(PersistenceType.CSV);
            deleteUserIfFound(username);
        } catch (Exception e) {
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }

    private void deleteUserIfFound(String username) throws DAOException {
        // Prova Fan
        Fan f = factory.getFanDao().retrieveFan(username);
        if (f != null) {
            factory.getFanDao().deleteFan(f);
            return;
        }
        // Prova Manager
        VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
        if (vm != null) {
            factory.getVenueManagerDao().deleteVenueManager(vm);
        }
    }
}
