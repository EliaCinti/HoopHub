package it.uniroma2.hoophub.sync;

import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.uniroma2.hoophub.patterns.facade.PersistenceType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CrossPersistenceSyncObserver}.
 *
 * <p>Tests the Observer pattern implementation for cross-persistence sync,
 * focusing on loop prevention via {@link SyncContext} and correct interface
 * implementation. Full integration tests require MySQL/CSV infrastructure.</p>
 *
 * @author Elia Cinti
 */
class TestCrossPersistenceSyncObserver {

    private CrossPersistenceSyncObserver csvSourceObserver;
    private CrossPersistenceSyncObserver mysqlSourceObserver;

    @BeforeEach
    void setUp() {
        SyncContext.endSync();
        csvSourceObserver = new CrossPersistenceSyncObserver(PersistenceType.CSV);
        mysqlSourceObserver = new CrossPersistenceSyncObserver(PersistenceType.MYSQL);
    }

    @AfterEach
    void tearDown() {
        SyncContext.endSync();
    }

    // ========== Interface implementation ==========

    @Test
    void testImplementsDaoObserverInterface() {
        assertInstanceOf(DaoObserver.class, csvSourceObserver);
    }

    // ========== Loop prevention: onAfterInsert ==========

    @Test
    void testOnAfterInsertSkipsWhenSyncing() {
        SyncContext.startSync();
        // Should NOT throw even with invalid entity (because it skips entirely)
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterInsert("Booking", "1", "not_a_booking"));
    }

    @Test
    void testOnAfterInsertSkipsWhenSyncingUnknownEntity() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterInsert("UnknownType", "999", new Object()));
    }

    // ========== Loop prevention: onAfterUpdate ==========

    @Test
    void testOnAfterUpdateSkipsWhenSyncing() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterUpdate("Booking", "1", "not_a_booking"));
    }

    @Test
    void testOnAfterUpdateSkipsWhenSyncingUnknownEntity() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                mysqlSourceObserver.onAfterUpdate("UnknownType", "999", new Object()));
    }

    // ========== Loop prevention: onAfterDelete ==========

    @Test
    void testOnAfterDeleteSkipsWhenSyncing() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterDelete("Booking", "1"));
    }

    @Test
    void testOnAfterDeleteSkipsWhenSyncingUnknownEntity() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                mysqlSourceObserver.onAfterDelete("UnknownType", "999"));
    }

    // ========== SyncContext restored after operations ==========

    @Test
    void testSyncContextNotActiveAfterInsert() {
        SyncContext.startSync();
        csvSourceObserver.onAfterInsert("Booking", "1", new Object());
        // SyncContext should still be active (it was active BEFORE the call)
        assertTrue(SyncContext.isSyncing());
    }

    @Test
    void testSyncContextNotActiveAfterUpdate() {
        SyncContext.startSync();
        csvSourceObserver.onAfterUpdate("Booking", "1", new Object());
        assertTrue(SyncContext.isSyncing());
    }

    @Test
    void testSyncContextNotActiveAfterDelete() {
        SyncContext.startSync();
        csvSourceObserver.onAfterDelete("Booking", "1");
        assertTrue(SyncContext.isSyncing());
    }

    // ========== Null/edge case handling ==========

    @Test
    void testOnAfterInsertNullEntityNoException() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterInsert("Booking", "1", null));
    }

    @Test
    void testOnAfterDeleteNullIdNoException() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterDelete("Booking", null));
    }

    @Test
    void testOnAfterInsertEmptyEntityTypeNoException() {
        SyncContext.startSync();
        assertDoesNotThrow(() ->
                csvSourceObserver.onAfterInsert("", "1", new Object()));
    }

    // ========== Both source types constructable ==========

    @Test
    void testCsvSourceObserverCreated() {
        assertNotNull(csvSourceObserver);
    }

    @Test
    void testMysqlSourceObserverCreated() {
        assertNotNull(mysqlSourceObserver);
    }
}