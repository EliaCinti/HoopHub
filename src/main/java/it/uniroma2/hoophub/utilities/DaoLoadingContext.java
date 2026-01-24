package it.uniroma2.hoophub.utilities;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility to prevent infinite loops when loading entities with bidirectional relationships.
 * <p>
 * Prevents StackOverflowError by tracking which entities are currently being loaded.
 * When VenueManager loads Venues and Venue loads VenueManager, this breaks the cycle.
 * </p>
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * String key = "Venue:" + id;
 * if (DaoLoadingContext.isLoading(key)) {
 *     return minimalVenue();  // Break cycle
 * }
 * DaoLoadingContext.startLoading(key);
 * try {
 *     return fullVenue();  // Load complete object
 * } finally {
 *     DaoLoadingContext.finishLoading(key);
 * }
 * </pre>
 * </p>
 *
 * @see it.uniroma2.hoophub.dao.VenueDao
 * @see it.uniroma2.hoophub.dao.VenueManagerDao
 */
public final class DaoLoadingContext {

    /** Tracks entities currently being loaded (e.g., "Venue:123", "Fan:john_doe"). */
    private static final Set<String> loadingContext = new HashSet<>();

    /** Private constructor - utility class with static methods only. */
    private DaoLoadingContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks if an entity is currently being loaded.
     *
     * @param entityKey Unique identifier (e.g., "Venue:123")
     * @return true if already loading (circular reference detected)
     */
    public static boolean isLoading(String entityKey) {
        return loadingContext.contains(entityKey);
    }

    /**
     * Marks an entity as currently being loaded.
     * Must be used with {@link #finishLoading(String)} in try-finally.
     *
     * @param entityKey Unique identifier for the entity
     */
    public static void startLoading(String entityKey) {
        loadingContext.add(entityKey);
    }

    /**
     * Marks an entity as finished loading.
     * Always call in finally block.
     *
     * @param entityKey Unique identifier for the entity
     */
    public static void finishLoading(String entityKey) {
        loadingContext.remove(entityKey);
    }

    /** Clears all loading context. Useful for testing. */
    public static void clear() {
        loadingContext.clear();
    }

    /** @return Number of entities currently being loaded (for debugging). */
    public static int getLoadingCount() {
        return loadingContext.size();
    }
}

