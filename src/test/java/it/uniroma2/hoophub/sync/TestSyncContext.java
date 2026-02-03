package it.uniroma2.hoophub.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SyncContext}.
 *
 * <p>Tests the thread-local mechanism that prevents infinite synchronization loops
 * between CSV and MySQL persistence layers.</p>
 *
 * @author Elia Cinti
 */
class TestSyncContext {

    @BeforeEach
    void setUp() {
        SyncContext.endSync();
    }

    @AfterEach
    void tearDown() {
        SyncContext.endSync();
    }

    // ========== isSyncing ==========

    @Test
    void testIsSyncingDefaultFalse() {
        assertFalse(SyncContext.isSyncing());
    }

    @Test
    void testIsSyncingTrueAfterStart() {
        SyncContext.startSync();
        assertTrue(SyncContext.isSyncing());
    }

    @Test
    void testIsSyncingFalseAfterEnd() {
        SyncContext.startSync();
        SyncContext.endSync();
        assertFalse(SyncContext.isSyncing());
    }

    // ========== startSync ==========

    @Test
    void testStartSyncMultipleCallsIdempotent() {
        SyncContext.startSync();
        SyncContext.startSync();
        SyncContext.startSync();
        assertTrue(SyncContext.isSyncing());
    }

    // ========== endSync ==========

    @Test
    void testEndSyncWithoutStartNoError() {
        SyncContext.endSync();
        assertFalse(SyncContext.isSyncing());
    }

    @Test
    void testEndSyncMultipleCallsNoError() {
        SyncContext.startSync();
        SyncContext.endSync();
        SyncContext.endSync();
        assertFalse(SyncContext.isSyncing());
    }

    // ========== startSync/endSync cycle ==========

    @Test
    void testStartEndCycleResetsProperly() {
        SyncContext.startSync();
        SyncContext.endSync();
        SyncContext.startSync();
        assertTrue(SyncContext.isSyncing());
    }

    @Test
    void testMultipleCyclesWorkCorrectly() {
        for (int i = 0; i < 10; i++) {
            SyncContext.startSync();
            SyncContext.endSync();
        }
        assertFalse(SyncContext.isSyncing());
    }

    // ========== Thread isolation ==========

    @Test
    void testThreadIsolationMainNotAffected() throws InterruptedException {
        CountDownLatch syncStarted = new CountDownLatch(1);
        CountDownLatch canFinish = new CountDownLatch(1);

        Thread syncThread = new Thread(() -> {
            SyncContext.startSync();
            syncStarted.countDown();
            try {
                canFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        syncThread.start();
        syncStarted.await();
        canFinish.countDown();
        syncThread.join();

        assertFalse(SyncContext.isSyncing());
    }

    @Test
    void testThreadIsolationChildThread() throws InterruptedException {
        SyncContext.startSync();

        AtomicBoolean childSyncing = new AtomicBoolean(true);
        Thread childThread = new Thread(() -> childSyncing.set(SyncContext.isSyncing()));

        childThread.start();
        childThread.join();

        assertFalse(childSyncing.get());
    }

    @Test
    void testConcurrentThreadsFirstThreadSyncing() throws InterruptedException {
        AtomicBoolean thread1Syncing = new AtomicBoolean(false);
        CountDownLatch t1Ready = new CountDownLatch(1);
        CountDownLatch canFinish = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            SyncContext.startSync();
            thread1Syncing.set(SyncContext.isSyncing());
            t1Ready.countDown();
            try {
                canFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SyncContext.endSync();
        });

        t1.start();
        t1Ready.await();
        canFinish.countDown();
        t1.join();

        assertTrue(thread1Syncing.get());
    }

    @Test
    void testConcurrentThreadsSecondThreadNotSyncing() throws InterruptedException {
        AtomicBoolean thread2Syncing = new AtomicBoolean(true);
        CountDownLatch t1Started = new CountDownLatch(1);
        CountDownLatch t2Done = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            SyncContext.startSync();
            t1Started.countDown();
            try {
                t2Done.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SyncContext.endSync();
        });

        Thread t2 = new Thread(() -> {
            try {
                t1Started.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread2Syncing.set(SyncContext.isSyncing());
            t2Done.countDown();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertFalse(thread2Syncing.get());
    }
}