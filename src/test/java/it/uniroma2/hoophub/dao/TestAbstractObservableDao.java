package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AbstractObservableDao}.
 *
 * <p>Tests the Observer pattern implementation used for cross-persistence
 * synchronization notifications (INSERT, UPDATE, DELETE).</p>
 *
 * @author Elia Cinti
 */
class TestAbstractObservableDao {

    /** Concrete subclass for testing (no actual persistence). */
    private static class TestableObservableDao extends AbstractObservableDao {
        // Minimal concrete class to test abstract behavior
    }

    /** Test observer that records received notifications. */
    private static class RecordingObserver implements DaoObserver {
        final List<String> insertCalls = new ArrayList<>();
        final List<String> updateCalls = new ArrayList<>();
        final List<String> deleteCalls = new ArrayList<>();

        @Override
        public void onAfterInsert(String entityType, String entityId, Object entity) {
            insertCalls.add(entityType + ":" + entityId);
        }

        @Override
        public void onAfterUpdate(String entityType, String entityId, Object entity) {
            updateCalls.add(entityType + ":" + entityId);
        }

        @Override
        public void onAfterDelete(String entityType, String entityId) {
            deleteCalls.add(entityType + ":" + entityId);
        }
    }

    /** Observer that throws exceptions to test error handling. */
    private static class FailingObserver implements DaoObserver {
        @Override
        public void onAfterInsert(String entityType, String entityId, Object entity) {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public void onAfterUpdate(String entityType, String entityId, Object entity) {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public void onAfterDelete(String entityType, String entityId) {
            throw new RuntimeException("Simulated failure");
        }
    }

    private TestableObservableDao dao;
    private RecordingObserver observer;

    @BeforeEach
    void setUp() {
        dao = new TestableObservableDao();
        observer = new RecordingObserver();
    }

    // ========== addObserver ==========

    @Test
    void testAddObserverReceivesNotifications() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(1, observer.insertCalls.size());
    }

    @Test
    void testAddObserverDuplicateIgnored() {
        dao.addObserver(observer);
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(1, observer.insertCalls.size());
    }

    @Test
    void testAddObserverMultipleDistinctObservers() {
        RecordingObserver observer2 = new RecordingObserver();
        dao.addObserver(observer);
        dao.addObserver(observer2);
        dao.notifyObservers(DaoOperation.INSERT, "Venue", "5", new Object());
        assertEquals(1, observer2.insertCalls.size());
    }

    // ========== removeObserver ==========

    @Test
    void testRemoveObserverNoNotifications() {
        dao.addObserver(observer);
        dao.removeObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(0, observer.insertCalls.size());
    }

    @Test
    void testRemoveObserverNonExistentNoError() {
        dao.removeObserver(observer);
        // Should not throw
        assertEquals(0, observer.insertCalls.size());
    }

    @Test
    void testRemoveObserverOnlyTargeted() {
        RecordingObserver observer2 = new RecordingObserver();
        dao.addObserver(observer);
        dao.addObserver(observer2);
        dao.removeObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Fan", "user1", new Object());
        assertEquals(1, observer2.insertCalls.size());
    }

    // ========== notifyObservers INSERT ==========

    @Test
    void testNotifyObserversInsertCallsOnAfterInsert() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "42", new Object());
        assertEquals("Booking:42", observer.insertCalls.getFirst());
    }

    @Test
    void testNotifyObserversInsertDoesNotCallUpdate() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(0, observer.updateCalls.size());
    }

    @Test
    void testNotifyObserversInsertDoesNotCallDelete() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(0, observer.deleteCalls.size());
    }

    // ========== notifyObservers UPDATE ==========

    @Test
    void testNotifyObserversUpdateCallsOnAfterUpdate() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.UPDATE, "Venue", "7", new Object());
        assertEquals("Venue:7", observer.updateCalls.getFirst());
    }

    @Test
    void testNotifyObserversUpdateDoesNotCallInsert() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.UPDATE, "Venue", "7", new Object());
        assertEquals(0, observer.insertCalls.size());
    }

    // ========== notifyObservers DELETE ==========

    @Test
    void testNotifyObserversDeleteCallsOnAfterDelete() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.DELETE, "Notification", "3", null);
        assertEquals("Notification:3", observer.deleteCalls.getFirst());
    }

    @Test
    void testNotifyObserversDeleteDoesNotCallInsert() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.DELETE, "Notification", "3", null);
        assertEquals(0, observer.insertCalls.size());
    }

    @Test
    void testNotifyObserversDeleteEntityNull() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.DELETE, "Booking", "10", null);
        assertEquals(1, observer.deleteCalls.size());
    }

    // ========== notifyObservers entity type/id ==========

    @Test
    void testNotifyObserversEntityTypePreserved() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Fan", "testuser", new Object());
        assertTrue(observer.insertCalls.getFirst().startsWith("Fan:"));
    }

    @Test
    void testNotifyObserversEntityIdPreserved() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Fan", "john_doe", new Object());
        assertTrue(observer.insertCalls.getFirst().endsWith(":john_doe"));
    }

    // ========== notifyObservers multiple notifications ==========

    @Test
    void testNotifyObserversMultipleCallsAccumulate() {
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "2", new Object());
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "3", new Object());
        assertEquals(3, observer.insertCalls.size());
    }

    @Test
    void testNotifyObserversNoObserversNoError() {
        // No observers registered - should not throw
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        assertEquals(0, observer.insertCalls.size());
    }

    // ========== notifyObservers error handling ==========

    @Test
    void testNotifyObserversExceptionDoesNotBreakOthers() {
        FailingObserver failing = new FailingObserver();
        dao.addObserver(failing);
        dao.addObserver(observer);
        dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object());
        // observer should still receive the notification despite failing observer
        assertEquals(1, observer.insertCalls.size());
    }

    @Test
    void testNotifyObserversExceptionDoesNotThrow() {
        FailingObserver failing = new FailingObserver();
        dao.addObserver(failing);
        // Should not throw despite observer exception
        assertDoesNotThrow(() ->
                dao.notifyObservers(DaoOperation.INSERT, "Booking", "1", new Object()));
    }
}