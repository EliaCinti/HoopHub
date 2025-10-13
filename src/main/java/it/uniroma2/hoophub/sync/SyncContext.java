package it.uniroma2.hoophub.sync;

/**
 * Manages the synchronization context to prevent infinite loops between DAO observers.
 * <p>
 * It uses a ThreadLocal variable to track whether the current thread is already
 * performing a synchronization operation. This prevents circular synchronization
 * where CSV → MySQL sync triggers MySQL → CSV sync, creating an infinite loop.
 * </p>
 * <p>
 * Example scenario without SyncContext:
 * <ol>
 *   <li>User saves Fan in MySQL</li>
 *   <li>MySQL observer triggers: sync to CSV</li>
 *   <li>CSV observer triggers: sync back to MySQL</li>
 *   <li>MySQL observer triggers again: infinite loop!</li>
 * </ol>
 * </p>
 * <p>
 * With SyncContext, step 3 is prevented because the thread is marked as "syncing".
 * </p>
 */
public class SyncContext {

    // ThreadLocal ensures each thread has its own sync state
    private static final ThreadLocal<Boolean> isSyncing = ThreadLocal.withInitial(() -> false);

    /**
     * Private constructor to prevent instantiation.
     */
    private SyncContext() {
        // Utility class
    }

    /**
     * Marks the beginning of a synchronization operation for the current thread.
     * <p>
     * This should be called before performing any cross-persistence operation
     * to prevent observers from creating infinite loops.
     * </p>
     */
    public static void startSync() {
        isSyncing.set(true);
    }

    /**
     * Marks the end of a synchronization operation for the current thread.
     * <p>
     * This cleans up the ThreadLocal to prevent memory leaks and allows
     * future synchronization operations on this thread.
     * </p>
     */
    public static void endSync() {
        isSyncing.remove(); // Clean up the ThreadLocal
    }

    /**
     * Checks if the current thread is already in a synchronization process.
     * <p>
     * Observers should check this before performing synchronization:
     * if the thread is already syncing, they should skip their operation
     * to prevent infinite loops.
     * </p>
     *
     * @return true if the thread is currently syncing, false otherwise
     */
    public static boolean isSyncing() {
        return isSyncing.get();
    }
}
