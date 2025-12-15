package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.BookingDao;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.dao.VenueDao;
import it.uniroma2.hoophub.dao.VenueManagerDao;
import it.uniroma2.hoophub.enums.*;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.dao.GlobalCache;
import it.uniroma2.hoophub.exception.DAOException;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoCacheIntegrationTest {

    // Utenti e costanti di test
    private static final String USER_FAN = "junit_test_fan";
    private static final String USER_VM = "junit_vm";
    private static final String USER_OWNER = "junit_venue_owner";

    @BeforeAll
    static void setup() {
        DaoFactoryFacade.getInstance().setPersistenceType(PersistenceType.MYSQL);
    }

    @BeforeEach
    void cleanStart() {
        GlobalCache.getInstance().clearAll();
        cleanupAllTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupAllTestData();
        GlobalCache.getInstance().clearAll();
    }

    // === TEST 1: CICLO DI VITA FAN (CRUD) ===
    @Test
    @Order(1)
    @DisplayName("Fan: Create, Read, Update, Delete & Data Consistency")
    void testFanLifecycle() throws Exception {
        FanDao fanDao = DaoFactoryFacade.getInstance().getFanDao();

        // 1. CREATE (Password > 6 chars, Nome Cognome validi)
        FanBean fanBean = new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").type(UserType.FAN)
                .fullName("Fan Original").gender("Male").favTeam(TeamNBA.CHICAGO_BULLS)
                .birthday(LocalDate.of(2000, 1, 1)).build();

        Fan fanModel = createFanModel(fanBean);
        fanDao.saveFan(fanModel);

        // 2. READ
        Fan retrievedFan = fanDao.retrieveFan(USER_FAN);
        assertNotNull(retrievedFan, "Il Fan deve esistere nel DB");
        assertEquals("Fan Original", retrievedFan.getFullName());
        assertEquals(TeamNBA.CHICAGO_BULLS, retrievedFan.getFavTeam());

        // 3. UPDATE
        Fan updatedModel = new Fan.Builder()
                .username(USER_FAN)
                .password(fanModel.getPasswordHash())
                .fullName("Fan Updated")
                .gender("Male")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .birthday(fanModel.getBirthday())
                .build();

        fanDao.updateFan(updatedModel);

        // 4. VERIFY UPDATE (Usa assertEquals logico)
        Fan retrievedAfterUpdate = fanDao.retrieveFan(USER_FAN);
        assertEquals("Fan Updated", retrievedAfterUpdate.getFullName());
        assertEquals(TeamNBA.LOS_ANGELES_LAKERS, retrievedAfterUpdate.getFavTeam());
        assertEquals(updatedModel, retrievedAfterUpdate);
    }

    // === TEST 2: CICLO DI VITA VENUE MANAGER ===
    @Test
    @Order(2)
    @DisplayName("VenueManager: Lifecycle & Persistence")
    void testVenueManagerLifecycle() throws Exception {
        VenueManagerDao vmDao = DaoFactoryFacade.getInstance().getVenueManagerDao();

        // 1. SAVE
        VenueManagerBean bean = new VenueManagerBean.Builder()
                .username(USER_VM).password("Pass123!").type(UserType.VENUE_MANAGER)
                .fullName("VM Original").gender("Male").companyName("Corp A").phoneNumber("3331234567").build();

        VenueManager vmModel = createVenueManagerModel(bean);
        vmDao.saveVenueManager(vmModel);

        // 2. RETRIEVE
        VenueManager retrieved = vmDao.retrieveVenueManager(USER_VM);
        assertNotNull(retrieved);
        assertEquals("Corp A", retrieved.getCompanyName());

        // 3. UPDATE
        VenueManager updateVm = new VenueManager.Builder()
                .username(USER_VM).password(vmModel.getPasswordHash())
                .fullName("VM Updated").gender("Female")
                .companyName("Corp B").phoneNumber("3339998887")
                .managedVenues(new ArrayList<>()).build();

        vmDao.updateVenueManager(updateVm);

        // 4. VERIFY
        VenueManager finalVm = vmDao.retrieveVenueManager(USER_VM);
        assertEquals("Corp B", finalVm.getCompanyName());
        assertEquals(updateVm, finalVm);
    }

    // === TEST 3: CICLO DI VITA VENUE ===
    @Test
    @Order(3)
    @DisplayName("Venue: Save, Update, Team Association")
    void testVenueLifecycle() throws Exception {
        VenueManagerDao vmDao = DaoFactoryFacade.getInstance().getVenueManagerDao();
        VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();

        // Setup Manager
        VenueManager owner = new VenueManager.Builder()
                .username(USER_OWNER).password("Password123").fullName("Owner Test").gender("M")
                .companyName("HoopHub Inc").phoneNumber("3330000000").managedVenues(new ArrayList<>()).build();
        vmDao.saveVenueManager(owner);

        // 1. SAVE VENUE
        Venue venueModel = new Venue.Builder()
                .name("Arena Test").city("Rome").address("Via Roma 1").maxCapacity(100)
                .type(VenueType.PUB)
                .venueManager(owner)
                .addTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .build();

        Venue savedVenue = venueDao.saveVenue(venueModel);
        int venueId = savedVenue.getId();

        assertTrue(venueId > 0);

        // Verify (assertEquals)
        Venue retrieved = venueDao.retrieveVenue(venueId);
        assertEquals(savedVenue, retrieved);

        // 2. UPDATE VENUE
        Venue updatedVenue = new Venue.Builder()
                .id(venueId)
                .name("Arena Pro")
                .city("Rome").address("Via Roma 1").maxCapacity(500)
                .type(VenueType.LOUNGE)
                .venueManager(owner)
                .addTeam(TeamNBA.BOSTON_CELTICS)
                .build();

        venueDao.updateVenue(updatedVenue);

        // 3. VERIFY UPDATE
        Venue finalVenue = venueDao.retrieveVenue(venueId);
        assertEquals("Arena Pro", finalVenue.getName());
        assertTrue(finalVenue.getAssociatedTeams().contains(TeamNBA.BOSTON_CELTICS));
        assertEquals(updatedVenue, finalVenue);
    }

    // === TEST 4: CICLO DI VITA BOOKING ===
    @Test
    @Order(4)
    @DisplayName("Booking: Workflow Creation -> Confirmation")
    void testBookingProcess() throws Exception {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        // 1. Setup - Nomi validi ("Nome Cognome") e Password lunghe
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").fullName("Fan Test").gender("M")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS).birthday(LocalDate.of(2000,1,1)).type(UserType.FAN).build());
        factory.getFanDao().saveFan(fan);

        VenueManager vm = createVenueManagerModel(new VenueManagerBean.Builder()
                .username(USER_VM).password("Pass123!").fullName("Manager Test").gender("F")
                .companyName("C").phoneNumber("1237654567").type(UserType.VENUE_MANAGER).build());
        factory.getVenueManagerDao().saveVenueManager(vm);

        Venue venue = new Venue.Builder()
                .name("V").city("C").address("A").maxCapacity(50).type(VenueType.BAR)
                .venueManager(vm).addTeam(TeamNBA.LOS_ANGELES_LAKERS).build();
        Venue savedVenue = factory.getVenueDao().saveVenue(venue);

        // 2. SAVE BOOKING
        Booking booking = new Booking.Builder(0, LocalDate.now().plusDays(5), LocalTime.of(20, 0),
                TeamNBA.LOS_ANGELES_LAKERS, TeamNBA.BOSTON_CELTICS, savedVenue, fan)
                .status(BookingStatus.PENDING).build();

        Booking savedBooking = factory.getBookingDao().saveBooking(booking);
        int bookingId = savedBooking.getId();

        assertTrue(bookingId > 0);

        // 3. UPDATE (Confirm)
        Booking confirmedBooking = new Booking.Builder(bookingId, booking.getGameDate(), booking.getGameTime(),
                booking.getHomeTeam(), booking.getAwayTeam(), savedVenue, fan)
                .status(BookingStatus.CONFIRMED).notified(true).build();

        factory.getBookingDao().updateBooking(confirmedBooking);

        // 4. VERIFY
        Booking retrievedBooking = factory.getBookingDao().retrieveBooking(bookingId);
        assertEquals(BookingStatus.CONFIRMED, retrievedBooking.getStatus());
        assertEquals(confirmedBooking, retrievedBooking);
    }

    // === TEST 5: NOTIFICHE ===
    @Test
    @Order(5)
    @DisplayName("Notification: Push & Read")
    void testNotificationLifecycle() throws Exception {
        // Setup User - Dati validi
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").fullName("Fan Test").gender("M")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS).birthday(LocalDate.of(2000,1,1)).type(UserType.FAN).build());
        DaoFactoryFacade.getInstance().getFanDao().saveFan(fan);

        // 1. SAVE
        Notification notification = new Notification.Builder()
                .username(USER_FAN)
                .userType(UserType.FAN)
                .type(NotificationType.BOOKING_APPROVED)
                .message("Prenotazione Confermata!")
                .isRead(false)
                .build();

        Notification savedNotif = DaoFactoryFacade.getInstance().getNotificationDao().saveNotification(notification);
        int notifId = savedNotif.getId();
        assertTrue(notifId > 0);

        // 2. RETRIEVE
        Notification retrieved = DaoFactoryFacade.getInstance().getNotificationDao().retrieveNotification(notifId);
        assertNotNull(retrieved);
        assertEquals("Prenotazione Confermata!", retrieved.getMessage());

        // 3. MARK AS READ
        Notification readNotif = retrieved.markAsRead();
        DaoFactoryFacade.getInstance().getNotificationDao().updateNotification(readNotif);

        // 4. VERIFY
        Notification finalNotif = DaoFactoryFacade.getInstance().getNotificationDao().retrieveNotification(notifId);
        assertTrue(finalNotif.isRead());
        assertEquals(readNotif, finalNotif);
    }

    // === TEST 6: NEGATIVE TESTS (ERROR HANDLING) ===

    @Test
    @Order(6)
    @DisplayName("Negative: Save Null Entities")
    void testSaveNullEntities() {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        FanDao fanDao = factory.getFanDao();
        VenueDao venueDao = factory.getVenueDao();
        BookingDao bookingDao = factory.getBookingDao();

        assertThrows(IllegalArgumentException.class, () -> fanDao.saveFan(null));
        assertThrows(IllegalArgumentException.class, () -> venueDao.saveVenue(null));
        assertThrows(IllegalArgumentException.class, () -> bookingDao.saveBooking(null));
    }

    @Test
    @Order(7)
    @DisplayName("Negative: Duplicate Primary Key")
    void testDuplicatePrimaryKey() throws Exception {
        FanDao fanDao = DaoFactoryFacade.getInstance().getFanDao();

        // Dati validi (Nome + Cognome, Pwd > 6)
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").fullName("Fan Test")
                .gender("M").favTeam(TeamNBA.LOS_ANGELES_LAKERS).birthday(LocalDate.of(2000,1,1)).type(UserType.FAN).build());

        fanDao.saveFan(fan);

        // Provo a salvare di nuovo -> DAOException
        assertThrows(DAOException.class, () -> fanDao.saveFan(fan));
    }

    @Test
    @Order(8)
    @DisplayName("Negative: Update Non-Existent Entity")
    void testUpdateNonExistentEntity() {
        FanDao fanDao = DaoFactoryFacade.getInstance().getFanDao();

        // Dati validi per la costruzione del bean, anche se non esiste nel DB
        Fan ghostFan = createFanModel(new FanBean.Builder()
                .username("ghost_user").password("Pass123!").fullName("Ghost User")
                .gender("M").favTeam(TeamNBA.LOS_ANGELES_LAKERS).birthday(LocalDate.of(2000,1,1)).type(UserType.FAN).build());

        assertThrows(DAOException.class, () -> fanDao.updateFan(ghostFan));
    }

    @Test
    @Order(9)
    @DisplayName("Negative: Foreign Key Violation")
    void testForeignKeyViolation() throws Exception {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        // Dati validi
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").fullName("Fan Test")
                .gender("M").favTeam(TeamNBA.LOS_ANGELES_LAKERS).birthday(LocalDate.of(2000,1,1)).type(UserType.FAN).build());
        factory.getFanDao().saveFan(fan);

        VenueManager vm = createVenueManagerModel(new VenueManagerBean.Builder()
                .username(USER_VM).password("Pass123!").fullName("Manager Test")
                .gender("F").companyName("C").phoneNumber("1238765456").type(UserType.VENUE_MANAGER).build());

        // Venue con ID inesistente
        Venue fakeVenue = new Venue.Builder()
                .id(99999)
                .name("Fake").city("C").address("A").maxCapacity(50).type(VenueType.BAR)
                .venueManager(vm).addTeam(TeamNBA.LOS_ANGELES_LAKERS).build();

        Booking invalidBooking = new Booking.Builder(0, LocalDate.now().plusDays(1), LocalTime.of(20, 0),
                TeamNBA.LOS_ANGELES_LAKERS, TeamNBA.BOSTON_CELTICS, fakeVenue, fan)
                .status(BookingStatus.PENDING).build();

        BookingDao bookingDao = factory.getBookingDao();
        assertThrows(DAOException.class, () -> bookingDao.saveBooking(invalidBooking));
    }

    // =================================================================================
    // UTILITIES
    // =================================================================================

    private void cleanupAllTestData() {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        cleanSpecificUser(factory, USER_FAN);
        cleanSpecificUser(factory, USER_VM);
        cleanSpecificUser(factory, USER_OWNER);
        cleanSpecificUser(factory, "ghost_user");
    }

    private void cleanSpecificUser(DaoFactoryFacade factory, String username) {
        try {
            // Tentativo 1: Pulizia specifica Fan (cancella anche da 'fans')
            Fan f = factory.getFanDao().retrieveFan(username);
            if (f != null) {
                factory.getFanDao().deleteFan(f);
                return;
            }

            // Tentativo 2: Pulizia specifica Manager (cancella da 'venue_managers' e a cascata venues)
            VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
            if (vm != null) {
                factory.getVenueManagerDao().deleteVenueManager(vm);
                return;
            }

            // Tentativo 3: Pulizia generica (se rimasto solo in 'users')
            it.uniroma2.hoophub.dao.UserDao userDao = factory.getUserDao();
            // Creiamo un dummy user valido per soddisfare il metodo deleteUser se richiede un oggetto User
            User dummy = new Fan.Builder()
                    .username(username).fullName("D U").gender("O")
                    .favTeam(TeamNBA.ATLANTA_HAWKS).birthday(LocalDate.now()).build();

            // Verifichiamo se esiste prima di cancellare per evitare errori
            if (userDao.retrieveUser(username) != null) {
                userDao.deleteUser(dummy);
            }
        } catch (Exception ignored) {
            // Ignora errori di cleanup per non sporcare il log dei test
        }
    }

    private Fan createFanModel(FanBean bean) {
        return new Fan.Builder()
                .username(bean.getUsername())
                .password("hashed_" + bean.getPassword())
                .fullName(bean.getFullName())
                .gender(bean.getGender())
                .favTeam(bean.getFavTeam())
                .birthday(bean.getBirthday())
                .build();
    }

    private VenueManager createVenueManagerModel(VenueManagerBean bean) {
        return new VenueManager.Builder()
                .username(bean.getUsername())
                .password("hashed_" + bean.getPassword())
                .fullName(bean.getFullName())
                .gender(bean.getGender())
                .companyName(bean.getCompanyName())
                .phoneNumber(bean.getPhoneNumber())
                .managedVenues(new ArrayList<>())
                .build();
    }
}