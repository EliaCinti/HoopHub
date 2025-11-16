package it.uniroma2.hoophub.utilities;

import java.util.HashSet;
import java.util.Set;

/**
 * Thread-safe context to prevent circular dependencies when DAOs load related entities.
 * <p>
 * This utility prevents infinite loops that occur when DAOs call other DAOs to reconstruct
 * object graphs with bidirectional relationships. For example:
 * <ul>
 *   <li>VenueDao loads a Venue and needs to load its VenueManager</li>
 *   <li>VenueManagerDao loads the manager and needs to load all managed Venues</li>
 *   <li>This would cause VenueDao to be called again, creating an infinite loop</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage Pattern:</strong>
 * <pre>
 * public Venue retrieveVenue(int id) {
 *     String contextKey = "Venue:" + id;
 *
 *     if (DaoLoadingContext.isLoading(contextKey)) {
 *         return createMinimalVenue(id);  // Return minimal object to break cycle
 *     }
 *
 *     DaoLoadingContext.startLoading(contextKey);
 *     try {
 *         // Load full object including related entities
 *         return loadFullVenue(id);
 *     } finally {
 *         DaoLoadingContext.finishLoading(contextKey);
 *     }
 * }
 * </pre>
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> Uses ThreadLocal to maintain separate contexts for each thread,
 * ensuring that concurrent operations don't interfere with each other.
 * </p>
 *
 * @see it.uniroma2.hoophub.dao.VenueDao
 * @see it.uniroma2.hoophub.dao.VenueManagerDao
 * @see it.uniroma2.hoophub.dao.BookingDao
 */
public final class DaoLoadingContext {

    /**
     * Thread-local set tracking entities currently being loaded in this thread.
     * Each entry is a string key identifying the entity (e.g., "Venue:123", "Fan:john_doe").
     */
    private static final ThreadLocal<Set<String>> LOADING_CONTEXT =
        ThreadLocal.withInitial(HashSet::new);

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private DaoLoadingContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if an entity with the given key is currently being loaded in this thread.
     * <p>
     * This method is used to detect circular loading and prevent infinite recursion.
     * If it returns true, the calling DAO should return a minimal representation
     * of the entity instead of attempting to load related entities.
     * </p>
     *
     * @param entityKey Unique identifier for the entity being loaded (e.g., "Venue:123")
     * @return true if this entity is already being loaded in the current call stack
     */
    public static boolean isLoading(String entityKey) {
        return LOADING_CONTEXT.get().contains(entityKey);
    }

    /**
     * Marks an entity as currently being loaded in this thread.
     * <p>
     * This method should be called before starting to load an entity and its relations.
     * It must be paired with {@link #finishLoading(String)} in a try-finally block
     * to ensure proper cleanup.
     * </p>
     *
     * @param entityKey Unique identifier for the entity being loaded
     */
    public static void startLoading(String entityKey) {
        LOADING_CONTEXT.get().add(entityKey);
    }

    /**
     * Marks an entity as finished loading in this thread.
     * <p>
     * This method should always be called in a finally block to ensure cleanup
     * even if an exception occurs during loading.
     * </p>
     *
     * @param entityKey Unique identifier for the entity that finished loading
     */
    public static void finishLoading(String entityKey) {
        LOADING_CONTEXT.get().remove(entityKey);
    }

    /**
     * Clears the entire loading context for the current thread.
     * <p>
     * This is primarily useful for cleanup in exceptional situations or testing.
     * Under normal circumstances, {@link #finishLoading(String)} should be used
     * to remove specific entries.
     * </p>
     */
    public static void clear() {
        LOADING_CONTEXT.get().clear();
    }

    /**
     * Returns the number of entities currently being loaded in this thread.
     * <p>
     * This is primarily useful for debugging and testing to verify that the
     * context is being properly maintained.
     * </p>
     *
     * @return The number of entities in the loading context
     */
    public static int getLoadingCount() {
        return LOADING_CONTEXT.get().size();
    }
}
