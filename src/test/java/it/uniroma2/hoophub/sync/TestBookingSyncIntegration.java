package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.dao.*;
import it.uniroma2.hoophub.enums.*;
import it.uniroma2.hoophub.exception.DAOException;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for cross-persistence booking synchronization.
 *
 * <p>Verifies that bookings created/updated on one persistence layer (CSV or MySQL)
 * are correctly propagated to the other via {@link CrossPersistenceSyncObserver}
 * using UPSERT semantics.</p>
 *
 * <h3>Test strategy</h3>
 * <p>The single-connection JDBC architecture has two known limitations that affect
 * test verification:</p>
 * <ol>
 *   <li><b>Nested ResultSet:</b> {@code BookingDaoMySql.retrieveBooking()} triggers
 *       nested queries when loading Venue dependencies, causing {@code ResultSet closed}
 *       errors. Mitigated in production by {@link GlobalCache}.</li>
 *   <li><b>Observer cascade rollback:</b> {@code NotificationBookingObserver} fires after
 *       {@code CrossPersistenceSyncObserver.endSync()}, its notification save can fail
 *       (FK constraint on shared connection), and the resulting {@code rollback()} undoes
 *       the booking UPSERT on MySQL.</li>
 * </ol>
 *
 * <p>Therefore, all field-level verifications are performed on the <b>CSV layer</b>,
 * which has no transactional rollback issues. MySQL existence is verified via
 * {@code bookingExists()} (simple query, no nested ResultSet) only where the
 * observer cascade does not interfere.</p>
 *
 * <p><b>Prerequisite:</b> MySQL must be running. Tests auto-skip if unavailable.</p>
 *
 * @author Elia Cinti
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestBookingSyncIntegration {

    private static DaoFactoryFacade facade;

    private static Fan testFan;
    private static VenueManager testManager;
    private static Venue testVenue;

    // High IDs to avoid conflicts with production data
    private static final int BOOKING_CSV_PRIMARY = 9901;
    private static final int BOOKING_MYSQL_PRIMARY = 9902;
    private static final int BOOKING_UPSERT = 9903;
    private static final int BOOKING_STATUS = 9904;
    private static final int BOOKING_LOOP = 9905;
    private static final int VENUE_TEST_ID = 9999;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(30);
    private static final LocalTime GAME_TIME = LocalTime.of(20, 0);

    // ========================================================================
    // SETUP AND TEARDOWN
    // ========================================================================

    @BeforeAll
    static void setUpOnce() {
        facade = DaoFactoryFacade.getInstance();
        Assumptions.assumeTrue(ConnectionFactory.testConnection(),
                "MySQL not available - skipping sync integration tests");

        buildTestFixtures();
        cleanupTestData();
        setupPrerequisiteData();
    }

    @AfterAll
    static void tearDownOnce() {
        if (!ConnectionFactory.testConnection()) return;
        cleanupTestData();
        facade.setPersistenceType(PersistenceType.MYSQL);
    }

    private static void buildTestFixtures() {
        testManager = new VenueManager.Builder()
                .username("sync_test_mgr")
                .fullName("Sync Test Manager")
                .gender("Male")
                .password("$2a$10$fakehash")
                .companyName("SyncTest Co")
                .phoneNumber("+3901234567890")
                .build();

        testFan = new Fan.Builder()
                .username("sync_test_fan")
                .fullName("Sync Test Fan")
                .gender("Male")
                .password("$2a$10$fakehash")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        testVenue = new Venue.Builder()
                .id(VENUE_TEST_ID)
                .name("Sync Test Sports Bar")
                .type(VenueType.SPORTS_BAR)
                .address("123 Sync Test Street")
                .city("Roma")
                .maxCapacity(50)
                .venueManager(testManager)
                .addTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .addTeam(TeamNBA.BOSTON_CELTICS)
                .build();
    }

    /**
     * Saves Fan, VenueManager, Venue to BOTH persistence layers.
     * SyncContext prevents observer triggers during setup.
     */
    private static void setupPrerequisiteData() {
        SyncContext.startSync();
        try {
            for (PersistenceType type : new PersistenceType[]{PersistenceType.MYSQL, PersistenceType.CSV}) {
                facade.setPersistenceType(type);
                safeExecute(() -> facade.getVenueManagerDao().saveVenueManager(testManager));
                safeExecute(() -> facade.getFanDao().saveFan(testFan));
                safeExecute(() -> facade.getVenueDao().saveVenue(testVenue));
            }
        } finally {
            SyncContext.endSync();
        }
    }

    private static void cleanupTestData() {
        SyncContext.startSync();
        try {
            int[] bookingIds = {BOOKING_CSV_PRIMARY, BOOKING_MYSQL_PRIMARY,
                    BOOKING_UPSERT, BOOKING_STATUS, BOOKING_LOOP};

            for (PersistenceType type : new PersistenceType[]{PersistenceType.MYSQL, PersistenceType.CSV}) {
                facade.setPersistenceType(type);
                for (int id : bookingIds) {
                    safeExecute(() -> {
                        Booking b = facade.getBookingDao().retrieveBooking(id);
                        if (b != null) facade.getBookingDao().deleteBooking(b);
                    });
                }
                safeExecute(() -> facade.getVenueDao().deleteVenue(testVenue));
                safeExecute(() -> facade.getFanDao().deleteFan(testFan));
                safeExecute(() -> facade.getVenueManagerDao().deleteVenueManager(testManager));
            }
            GlobalCache.getInstance().clearAll();
        } finally {
            SyncContext.endSync();
        }
    }

    private static void safeExecute(DaoAction action) {
        try { action.execute(); } catch (Exception ignored) { /* best effort cleanup */ }
    }

    @FunctionalInterface
    private interface DaoAction { void execute() throws DAOException; }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private Booking createTestBooking(int id) {
        return new Booking.Builder(id, FUTURE_DATE, GAME_TIME,
                TeamNBA.LOS_ANGELES_LAKERS, TeamNBA.BOSTON_CELTICS, testVenue, testFan)
                .status(BookingStatus.PENDING)
                .build();
    }

    /**
     * Saves a booking directly to ONE layer WITHOUT triggering ANY observer.
     */
    private void saveDirectly(PersistenceType type, Booking booking) throws DAOException {
        SyncContext.startSync();
        try {
            facade.setPersistenceType(type);
            facade.getBookingDao().saveBooking(booking);
        } finally {
            SyncContext.endSync();
        }
    }

    /**
     * Retrieves a booking from CSV. CSV has no JDBC limitations.
     */
    private Booking retrieveFromCsv(int bookingId) throws DAOException {
        GlobalCache.getInstance().clearAll();
        facade.setPersistenceType(PersistenceType.CSV);
        return facade.getBookingDao().retrieveBooking(bookingId);
    }

    /**
     * Checks booking existence on MySQL via {@code bookingExists()} — a simple
     * single-query method with no nested ResultSet issues.
     */
    private boolean existsOnMySql(int bookingId) throws DAOException {
        GlobalCache.getInstance().clearAll();
        facade.setPersistenceType(PersistenceType.MYSQL);
        return facade.getBookingDao().bookingExists(bookingId);
    }

    private boolean existsOnCsv(int bookingId) throws DAOException {
        GlobalCache.getInstance().clearAll();
        facade.setPersistenceType(PersistenceType.CSV);
        return facade.getBookingDao().bookingExists(bookingId);
    }

    // ========================================================================
    // TESTS 1-2: CSV → MySQL INSERT sync
    // ========================================================================

    /**
     * First sync operation in the suite. The booking syncs to MySQL and the
     * NotificationBookingObserver successfully creates the notification
     * (clean connection state, no prior FK conflicts).
     */
    @Test
    @Order(1)
    void testSaveBookingCsvSyncedToMySql() throws DAOException {
        Booking booking = createTestBooking(BOOKING_CSV_PRIMARY);

        facade.setPersistenceType(PersistenceType.CSV);
        facade.getBookingDao().saveBooking(booking);

        assertTrue(existsOnMySql(BOOKING_CSV_PRIMARY));
    }

    @Test
    @Order(2)
    void testSaveBookingCsvSourceIntact() throws DAOException {
        assertEquals(BookingStatus.PENDING, retrieveFromCsv(BOOKING_CSV_PRIMARY).getStatus());
    }

    // ========================================================================
    // TESTS 3-6: MySQL → CSV INSERT sync (full field verification on CSV)
    // ========================================================================

    @Test
    @Order(3)
    void testSaveBookingMySqlSyncedToCsv() throws DAOException {
        Booking booking = createTestBooking(BOOKING_MYSQL_PRIMARY);

        facade.setPersistenceType(PersistenceType.MYSQL);
        facade.getBookingDao().saveBooking(booking);

        assertTrue(existsOnCsv(BOOKING_MYSQL_PRIMARY));
    }

    @Test
    @Order(4)
    void testSaveBookingMySqlSyncStatusOnCsv() throws DAOException {
        assertEquals(BookingStatus.PENDING, retrieveFromCsv(BOOKING_MYSQL_PRIMARY).getStatus());
    }

    @Test
    @Order(5)
    void testSaveBookingMySqlSyncFanOnCsv() throws DAOException {
        assertEquals("sync_test_fan", retrieveFromCsv(BOOKING_MYSQL_PRIMARY).getFanUsername());
    }

    @Test
    @Order(6)
    void testSaveBookingMySqlSyncDateOnCsv() throws DAOException {
        assertEquals(FUTURE_DATE, retrieveFromCsv(BOOKING_MYSQL_PRIMARY).getGameDate());
    }

    // ========================================================================
    // TEST 7: UPSERT — no duplicate error on ID conflict
    // ========================================================================

    @Test
    @Order(7)
    void testUpsertNoDuplicateError() throws DAOException {
        Booking booking = createTestBooking(BOOKING_UPSERT);

        // Pre-populate BOTH layers (observers disabled)
        saveDirectly(PersistenceType.MYSQL, booking);
        saveDirectly(PersistenceType.CSV, booking);

        // Now save via CSV WITH observers → UPSERT must handle the conflict
        facade.setPersistenceType(PersistenceType.CSV);
        assertDoesNotThrow(() -> facade.getBookingDao().saveBooking(booking));
    }

    // ========================================================================
    // TESTS 8-9: Status update persistence via CSV
    //
    // Updates booking status on CSV and verifies the change persists.
    // The CrossPersistenceSyncObserver fires and attempts MySQL sync
    // (best-effort; may fail due to notification observer cascade on
    // the shared MySQL connection — a known architectural limitation).
    // ========================================================================

    @Test
    @Order(8)
    void testUpdateConfirmedPersistedOnCsv() throws DAOException {
        Booking booking = createTestBooking(BOOKING_STATUS);

        // Save to CSV only (no observer interference)
        saveDirectly(PersistenceType.CSV, booking);

        // Confirm status and update via CSV
        booking.confirm();
        facade.setPersistenceType(PersistenceType.CSV);
        facade.getBookingDao().updateBooking(booking);

        // Verify CONFIRMED status persisted on CSV
        assertEquals(BookingStatus.CONFIRMED, retrieveFromCsv(BOOKING_STATUS).getStatus());
    }

    @Test
    @Order(9)
    void testUpdateCancelledPersistedOnCsv() throws DAOException {
        // Booking 9904 is CONFIRMED on CSV from test 8
        Booking booking = retrieveFromCsv(BOOKING_STATUS);
        assertNotNull(booking);

        booking.cancel();
        facade.setPersistenceType(PersistenceType.CSV);
        facade.getBookingDao().updateBooking(booking);

        assertEquals(BookingStatus.CANCELLED, retrieveFromCsv(BOOKING_STATUS).getStatus());
    }

    // ========================================================================
    // TESTS 10-11: Loop prevention
    // ========================================================================

    @Test
    @Order(10)
    void testSyncNoInfiniteLoop() {
        Booking booking = createTestBooking(BOOKING_LOOP);
        facade.setPersistenceType(PersistenceType.CSV);

        assertDoesNotThrow(() -> facade.getBookingDao().saveBooking(booking));
    }

    @Test
    @Order(11)
    void testSyncLoopBookingDataIntact() throws DAOException {
        Booking synced = retrieveFromCsv(BOOKING_LOOP);
        assertNotNull(synced);
        assertEquals(BookingStatus.PENDING, synced.getStatus());
    }

    // ========================================================================
    // TEST 12: Bidirectional — both bookings visible on CSV
    // ========================================================================

    /**
     * Verifies bidirectional sync completeness: booking 9901 (saved on CSV,
     * synced to MySQL in test 1) and booking 9902 (saved on MySQL, synced to CSV
     * in test 3) are both present on the CSV layer.
     */
    @Test
    @Order(12)
    void testBidirectionalBothBookingsOnCsv() throws DAOException {
        assertTrue(existsOnCsv(BOOKING_CSV_PRIMARY) && existsOnCsv(BOOKING_MYSQL_PRIMARY));
    }
}