package it.uniroma2.hoophub.patterns.observer;

import it.uniroma2.hoophub.sync.SyncContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NotificationBookingObserver}.
 *
 * <p>Tests the Observer that automatically creates notifications when
 * bookings are created or their status changes. Focuses on filtering
 * logic and SyncContext loop prevention.</p>
 *
 * <p>Note: Tests involving actual notification creation require DAO
 * infrastructure (IN_MEMORY or MySQL) and are covered by integration tests.</p>
 *
 * @author Elia Cinti
 */
class TestNotificationBookingObserver {

    private NotificationBookingObserver observer;

    @BeforeEach
    void setUp() {
        SyncContext.endSync();
        observer = new NotificationBookingObserver();
    }

    @AfterEach
    void tearDown() {
        SyncContext.endSync();
    }

    // ========== Interface implementation ==========

    @Test
    void testImplementsDaoObserverInterface() {
        assertInstanceOf(DaoObserver.class, observer);
    }

    // ========== onAfterInsert: entity type filtering ==========

    @Test
    void testOnAfterInsertIgnoresNonBookingEntity() {
        // Should silently ignore non-Booking entity types
        assertDoesNotThrow(() ->
                observer.onAfterInsert("Fan", "user1", new Object()));
    }

    @Test
    void testOnAfterInsertIgnoresVenueEntity() {
        assertDoesNotThrow(() ->
                observer.onAfterInsert("Venue", "1", new Object()));
    }

    @Test
    void testOnAfterInsertIgnoresNotificationEntity() {
        assertDoesNotThrow(() ->
                observer.onAfterInsert("Notification", "1", new Object()));
    }

    // ========== onAfterInsert: SyncContext loop prevention ==========

    @Test
    void testOnAfterInsertSkipsWhenSyncing() {
        SyncContext.startSync();
        // Even with "Booking" type, should skip during sync
        assertDoesNotThrow(() ->
                observer.onAfterInsert("Booking", "1", "invalid_entity"));
    }

    @Test
    void testOnAfterInsertSyncContextPreserved() {
        SyncContext.startSync();
        observer.onAfterInsert("Booking", "1", new Object());
        assertTrue(SyncContext.isSyncing());
    }

    // ========== onAfterUpdate: entity type filtering ==========

    @Test
    void testOnAfterUpdateIgnoresNonBookingEntity() {
        assertDoesNotThrow(() ->
                observer.onAfterUpdate("Fan", "user1", new Object()));
    }

    @Test
    void testOnAfterUpdateIgnoresVenueEntity() {
        assertDoesNotThrow(() ->
                observer.onAfterUpdate("Venue", "5", new Object()));
    }

    // ========== onAfterUpdate: SyncContext loop prevention ==========

    @Test
    void testOnAfterUpdateSkipsWhenSyncing() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                observer.onAfterUpdate("Booking", "1", "invalid_entity"));
    }

    @Test
    void testOnAfterUpdateSyncContextPreserved() {
        SyncContext.startSync();
        observer.onAfterUpdate("Booking", "1", new Object());
        assertTrue(SyncContext.isSyncing());
    }

    // ========== onAfterDelete ==========

    @Test
    void testOnAfterDeleteNoAction() {
        // Delete is not implemented in NotificationBookingObserver
        assertDoesNotThrow(() ->
                observer.onAfterDelete("Booking", "1"));
    }

    @Test
    void testOnAfterDeleteNonBookingNoAction() {
        assertDoesNotThrow(() ->
                observer.onAfterDelete("Fan", "user1"));
    }

    // ========== onAfterInsert: ClassCastException handling ==========

    @Test
    void testOnAfterInsertInvalidEntityTypeHandled() {
        // Passing a non-Booking object when entity type IS "Booking"
        // Should be caught internally as ClassCastException, not propagated
        assertDoesNotThrow(() ->
                observer.onAfterInsert("Booking", "1", "not_a_booking"));
    }

    @Test
    void testOnAfterUpdateInvalidEntityTypeHandled() {
        assertDoesNotThrow(() ->
                observer.onAfterUpdate("Booking", "1", "not_a_booking"));
    }
}