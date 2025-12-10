package it.uniroma2.hoophub.dao.mysql;

import it.uniroma2.hoophub.beans.FanBean;
import it.uniroma2.hoophub.beans.VenueManagerBean;
import it.uniroma2.hoophub.dao.FanDao;
import it.uniroma2.hoophub.enums.*;
import it.uniroma2.hoophub.model.*;
import it.uniroma2.hoophub.patterns.facade.DaoFactoryFacade;
import it.uniroma2.hoophub.patterns.facade.PersistenceType;
import it.uniroma2.hoophub.dao.GlobalCache;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoCacheIntegrationTest {

    private static FanDao fanDao;

    // Utenti di test
    private static final String USER_FAN = "junit_test_fan";
    private static final String USER_VM = "junit_vm";
    private static final String USER_OWNER = "junit_venue_owner";

    @BeforeAll
    static void setup() {
        DaoFactoryFacade.getInstance().setPersistenceType(PersistenceType.MYSQL);
        fanDao = DaoFactoryFacade.getInstance().getFanDao();
    }

    @BeforeEach
    void cleanStart() {
        GlobalCache.getInstance().clearAll();
        forceCleanUser(USER_FAN);
        forceCleanUser(USER_VM);
        forceCleanUser(USER_OWNER);
    }

    @AfterEach
    void tearDown() {
        forceCleanUser(USER_FAN);
        forceCleanUser(USER_VM);
        forceCleanUser(USER_OWNER);
        GlobalCache.getInstance().clearAll();
    }

    // === HELPER ANTI-ZOMBIE ===
    private void forceCleanUser(String username) {
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();
        factory.setPersistenceType(PersistenceType.MYSQL);

        try {
            Fan f = factory.getFanDao().retrieveFan(username);
            if (f != null) { factory.getFanDao().deleteFan(f); return; }

            VenueManager vm = factory.getVenueManagerDao().retrieveVenueManager(username);
            if (vm != null) { factory.getVenueManagerDao().deleteVenueManager(vm); return; }

            it.uniroma2.hoophub.dao.UserDao userDao = factory.getUserDao();
            String[] rawUser = userDao.retrieveUser(username);
            if (rawUser != null && rawUser.length > 0) {
                User dummy = new Fan.Builder()
                        .username(username)
                        .fullName("Z")
                        .gender("O")
                        .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                        .birthday(LocalDate.now())
                        .build();
                userDao.deleteUser(dummy);
                System.out.println("CLEANUP: Zombie User " + username + " rimosso.");
            }
        } catch (Exception e) {
            System.err.println("Cleanup warning per " + username + ": " + e.getMessage());
        }
    }

    // === TEST FAN ===

    @Test
    @Order(1)
    @DisplayName("Fan: Identity Map")
    void testFanIdentityMap() throws Exception {
        FanBean newFanBean = new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").type(UserType.FAN.name())
                .fullName("Junit Fan").gender("Male").favTeam(TeamNBA.CHICAGO_BULLS)
                .birthday(LocalDate.of(2000, 1, 1)).build();

        fanDao.saveFan(createFanModel(newFanBean));

        Fan fan1 = fanDao.retrieveFan(USER_FAN);
        Fan fan2 = fanDao.retrieveFan(USER_FAN);

        assertNotNull(fan1);
        assertSame(fan1, fan2, "La cache deve restituire la stessa istanza");
    }

    @Test
    @Order(2)
    @DisplayName("Fan: Update Write-Through")
    void testFanUpdate() throws Exception {
        // Arrange
        FanBean initialBean = new FanBean.Builder()
                .username(USER_FAN).password("Pass123!").type(UserType.FAN.name())
                .fullName("Old Name").gender("Male").favTeam(TeamNBA.CHICAGO_BULLS)
                .birthday(LocalDate.of(2000, 1, 1)).build();
        Fan initialModel = createFanModel(initialBean);
        fanDao.saveFan(initialModel);

        // Act: Update Model-First
        Fan updatedModel = new Fan.Builder()
                .username(USER_FAN)
                .password(initialModel.getPasswordHash())
                .fullName("New Name")
                .gender("Male")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .birthday(initialModel.getBirthday())
                .build();

        fanDao.updateFan(updatedModel);

        // Assert
        Fan retrievedFan = fanDao.retrieveFan(USER_FAN);
        assertEquals("New Name", retrievedFan.getFullName());
        assertSame(updatedModel, retrievedFan, "L'istanza aggiornata deve essere in cache");
    }

    // === TEST VENUE MANAGER ===

    @Test
    @Order(3)
    @DisplayName("VenueManager: Lifecycle")
    void testVenueManagerLifecycle() throws Exception {
        it.uniroma2.hoophub.dao.VenueManagerDao vmDao = DaoFactoryFacade.getInstance().getVenueManagerDao();

        // Save
        VenueManagerBean bean = new VenueManagerBean.Builder()
                .username(USER_VM).password("Pass1!").type(UserType.VENUE_MANAGER.name())
                .fullName("VM One").gender("Male").companyName("Corp A").phoneNumber("3331234567").build();
        VenueManager vmModel = createVenueManagerModel(bean);
        vmDao.saveVenueManager(vmModel);

        // Cache Hit
        VenueManager vm1 = vmDao.retrieveVenueManager(USER_VM);
        assertSame(vm1, vmDao.retrieveVenueManager(USER_VM));

        // Update
        VenueManager updatedVmModel = new VenueManager.Builder()
                .username(USER_VM).password(vmModel.getPasswordHash())
                .fullName("VM Two").gender("Female")
                .companyName("Corp B").phoneNumber("3339998887")
                .managedVenues(new ArrayList<>()).build();

        vmDao.updateVenueManager(updatedVmModel);

        // Verify
        VenueManager retrievedVm = vmDao.retrieveVenueManager(USER_VM);
        assertEquals("Corp B", retrievedVm.getCompanyName());
        assertSame(updatedVmModel, retrievedVm);
    }

    // === TEST VENUE (Completo di Team) ===

    @Test
    @Order(4)
    @DisplayName("Venue: Save & Update con Teams")
    void testVenueLifecycle() throws Exception {
        it.uniroma2.hoophub.dao.VenueManagerDao vmDao = DaoFactoryFacade.getInstance().getVenueManagerDao();
        it.uniroma2.hoophub.dao.VenueDao venueDao = DaoFactoryFacade.getInstance().getVenueDao();

        // 1. Setup Manager
        VenueManager owner = new VenueManager.Builder()
                .username(USER_OWNER).password("pass").fullName("Owner Name").gender("M")
                .companyName("C").phoneNumber("3330000000").managedVenues(new ArrayList<>()).build();
        vmDao.saveVenueManager(owner);

        // 2. Save Venue (Model-First)
        Venue venueModel = new Venue.Builder()
                .name("Arena Test").city("Rome").address("Via Roma").maxCapacity(100)
                .type(VenueType.BAR)
                .venueManager(owner)
                .addTeam(TeamNBA.LOS_ANGELES_LAKERS) // Aggiungo un team
                .build();

        Venue savedVenue = venueDao.saveVenue(venueModel);
        // ID per Venue e Booking (verranno popolati durante i test)
        int venueId = savedVenue.getId();

        // Verify
        assertTrue(savedVenue.getId() > 0);
        assertTrue(savedVenue.getAssociatedTeams().contains(TeamNBA.LOS_ANGELES_LAKERS));

        // Cache Hit
        assertSame(savedVenue, venueDao.retrieveVenue(venueId));

        // 3. Update (Cambio nome e cambio team)
        Venue updatedVenue = new Venue.Builder()
                .id(venueId)
                .name("Arena Updated")
                .city("Rome").address("Via Roma").maxCapacity(500)
                .type(VenueType.BAR)
                .venueManager(owner)
                .addTeam(TeamNBA.BOSTON_CELTICS) // Cambio team (da Lakers a Celtics)
                .build();

        venueDao.updateVenue(updatedVenue);

        // Verify Update & Cache
        Venue vRetrieved = venueDao.retrieveVenue(venueId);
        assertEquals("Arena Updated", vRetrieved.getName());
        assertTrue(vRetrieved.getAssociatedTeams().contains(TeamNBA.BOSTON_CELTICS));
        assertFalse(vRetrieved.getAssociatedTeams().contains(TeamNBA.LOS_ANGELES_LAKERS));
        assertSame(updatedVenue, vRetrieved);

        // Cleanup (Cancella manager -> cancella venues -> cancella teams via cascade)
        vmDao.deleteVenueManager(owner);
    }

    // === TEST BOOKING ===

    @Test
    @Order(5)
    @DisplayName("Booking: Lifecycle Model-First")
    void testBookingLifecycle() throws Exception {
        // Setup Completo: Fan + Manager + Venue
        DaoFactoryFacade factory = DaoFactoryFacade.getInstance();

        // Fan
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN)
                .password("p")
                .fullName("F")
                .gender("M")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .birthday(LocalDate.now())
                .type("FAN")
                .build());
        factory.getFanDao().saveFan(fan);

        // Manager
        VenueManager vm = createVenueManagerModel(new VenueManagerBean.Builder()
                .username(USER_OWNER)
                .password("p")
                .fullName("M")
                .gender("F")
                .companyName("C")
                .phoneNumber("3330000000")
                .type("VENUE_MANAGER")
                .build());
        factory.getVenueManagerDao().saveVenueManager(vm);

        // Venue
        Venue venue = new Venue.Builder()
                .name("V")
                .city("C")
                .address("A")
                .maxCapacity(100)
                .type(VenueType.BAR)
                .venueManager(vm)
                .addTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .build();
        Venue savedVenue = factory.getVenueDao().saveVenue(venue);

        // 1. SAVE BOOKING
        Booking booking = new Booking.Builder(
                0, // ID temp
                LocalDate.now().plusDays(1),
                LocalTime.of(20, 0),
                TeamNBA.LOS_ANGELES_LAKERS,
                TeamNBA.BOSTON_CELTICS,
                savedVenue,
                fan
        ).status(BookingStatus.PENDING).build();

        Booking savedBooking = factory.getBookingDao().saveBooking(booking);
        int bookingId = savedBooking.getId();

        assertTrue(bookingId > 0);

        // 2. CACHE HIT
        assertSame(savedBooking, factory.getBookingDao().retrieveBooking(bookingId));

        // 3. UPDATE (Confirm)
        Booking confirmedBooking = new Booking.Builder(
                bookingId,
                booking.getGameDate(), booking.getGameTime(),
                booking.getHomeTeam(), booking.getAwayTeam(),
                savedVenue, fan
        ).status(BookingStatus.CONFIRMED).notified(true).build();

        factory.getBookingDao().updateBooking(confirmedBooking);

        // Verify
        Booking retrieved = factory.getBookingDao().retrieveBooking(bookingId);
        assertEquals(BookingStatus.CONFIRMED, retrieved.getStatus());
        assertSame(confirmedBooking, retrieved);

        // Cleanup (Booking cancellato via cascade o manuale? Meglio manuale per test pulito)
        factory.getBookingDao().deleteBooking(retrieved);
        factory.getVenueManagerDao().deleteVenueManager(vm);
        factory.getFanDao().deleteFan(fan);
    }

    // === TEST NOTIFICATION ===

    @Test
    @Order(6)
    @DisplayName("Notification: Save & Retrieve")
    void testNotification() throws Exception {
        // Setup User (serve per la FK)
        Fan fan = createFanModel(new FanBean.Builder()
                .username(USER_FAN)
                .password("p")
                .fullName("F")
                .gender("M")
                .favTeam(TeamNBA.LOS_ANGELES_LAKERS)
                .birthday(LocalDate.now())
                .type("FAN")
                .build());
        DaoFactoryFacade.getInstance().getFanDao().saveFan(fan);

        // 1. SAVE
        Notification notification = new Notification.Builder()
                .username(USER_FAN)
                .userType(UserType.FAN)
                .type(NotificationType.BOOKING_APPROVED)
                .message("Test Msg")
                .isRead(false)
                .build();

        Notification savedNotification = DaoFactoryFacade.getInstance().getNotificationDao().saveNotification(notification);

        assertTrue(savedNotification.getId() > 0);

        // 2. RETRIEVE & CACHE
        Notification retrieved = DaoFactoryFacade.getInstance().getNotificationDao().retrieveNotification(savedNotification.getId());
        assertSame(savedNotification, retrieved);

        // Cleanup
        DaoFactoryFacade.getInstance().getNotificationDao().deleteNotification(savedNotification);
        DaoFactoryFacade.getInstance().getFanDao().deleteFan(fan);
    }

    // === FACTORY METHODS SIMULATI (Controller Logic) ===

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