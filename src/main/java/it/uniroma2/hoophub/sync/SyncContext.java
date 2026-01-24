package it.uniroma2.hoophub.sync;

/**
 * Thread-local context manager that prevents infinite synchronization loops.
 *
 * <p><b>Problem solved:</b> When using the Observer pattern for cross-persistence
 * sync, each DAO notifies observers after INSERT/UPDATE/DELETE. Without protection,
 * this creates infinite loops:</p>
 *
 * <pre>
 * 1. User saves Fan to MySQL
 * 2. MySqlToCsvObserver triggers → saves Fan to CSV
 * 3. CsvToMySqlObserver triggers → saves Fan back to MySQL
 * 4. MySqlToCsvObserver triggers again → INFINITE LOOP!
 * </pre>
 *
 * <p><b>Solution:</b> SyncContext uses a {@link ThreadLocal} boolean flag.
 * When a sync operation starts, it sets the flag to {@code true}. All observers
 * check this flag before executing: if already syncing, they skip their operation.</p>
 *
 * <pre>
 * 1. User saves Fan to MySQL
 * 2. Observer calls SyncContext.startSync() → flag = true
 * 3. Observer saves Fan to CSV
 * 4. CSV DAO notifies its observers, but they check isSyncing() → true → SKIP
 * 5. Observer calls SyncContext.endSync() → flag removed
 * </pre>
 *
 * <p><b>Thread safety:</b> Uses {@link ThreadLocal} so each thread has its own
 * independent sync state. Multiple concurrent users won't interfere with each other.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see CrossPersistenceSyncObserver
 * @see InitialSyncManager
 */
public class SyncContext {

    /**
     * Thread-local flag indicating if current thread is performing sync.
     * Initialized to {@code false} for each new thread.
     */
    private static final ThreadLocal<Boolean> isSyncing = ThreadLocal.withInitial(() -> false);

    private SyncContext() {
        // Utility class
    }

    /**
     * Marks the beginning of a sync operation for the current thread.
     *
     * <p>Must be called BEFORE any cross-persistence operation.
     * Always pair with {@link #endSync()} in a finally block.</p>
     *
     * <pre>
     * SyncContext.startSync();
     * try {
     *     // perform sync operations
     * } finally {
     *     SyncContext.endSync();
     * }
     * </pre>
     */
    public static void startSync() {
        isSyncing.set(true);
    }

    /**
     * Marks the end of a sync operation and cleans up ThreadLocal.
     *
     * <p>Uses {@code remove()} instead of {@code set(false)} to prevent
     * memory leaks in thread pools where threads are reused.</p>
     */
    public static void endSync() {
        isSyncing.remove();
    }

    /**
     * Checks if the current thread is already performing synchronization.
     *
     * <p>Observers MUST check this at the start of every callback:</p>
     * <pre>
     * public void onAfterInsert(...) {
     *     if (SyncContext.isSyncing()) return; // Skip to prevent loop
     *     // ... perform sync
     * }
     * </pre>
     *
     * @return {@code true} if thread is syncing, {@code false} otherwise
     */
    public static boolean isSyncing() {
        return isSyncing.get();
    }
}