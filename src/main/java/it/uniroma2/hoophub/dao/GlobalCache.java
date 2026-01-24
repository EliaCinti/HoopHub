package it.uniroma2.hoophub.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized in-memory cache using the <b>Singleton pattern (GoF)</b>.
 *
 * <p>Shared repository for all DAOs to store and retrieve entities by unique keys
 * (e.g., "Booking:1"), preventing redundant database access within a session.
 * Uses eager initialization and {@link ConcurrentHashMap} for thread safety.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
@SuppressWarnings("java:S6548")
public class GlobalCache {

    private static final GlobalCache instance = new GlobalCache();
    private final Map<String, Object> cacheMap;
    private final Logger logger;

    private GlobalCache() {
        this.cacheMap = new ConcurrentHashMap<>();
        this.logger = Logger.getLogger(GlobalCache.class.getName());
    }

    /**
     * Returns the singleton instance.
     *
     * @return the global cache instance
     */
    public static GlobalCache getInstance() {
        return instance;
    }

    /**
     * Stores an entity in cache.
     *
     * @param key   the cache key (e.g., "Booking:1")
     * @param value the entity to cache
     */
    public void put(String key, Object value) {
        if (key != null && value != null) {
            cacheMap.put(key, value);
            logger.log(Level.FINE, "Cache PUT: {0}", key);
        }
    }

    /**
     * Retrieves an entity from cache.
     *
     * @param key the cache key
     * @return the cached entity, or null if not found
     */
    public Object get(String key) {
        Object value = cacheMap.get(key);
        if (value != null) {
            logger.log(Level.FINE, "Cache HIT: {0}", key);
        } else {
            logger.log(Level.FINE, "Cache MISS: {0}", key);
        }
        return value;
    }

    /**
     * Removes an entity from cache.
     *
     * @param key the cache key to remove
     */
    public void remove(String key) {
        if (key != null) {
            cacheMap.remove(key);
            logger.log(Level.FINE, "Cache REMOVE: {0}", key);
        }
    }

    /**
     * Clears all cached entries. Call on user logout.
     */
    public void clearAll() {
        cacheMap.clear();
        logger.log(Level.INFO, "Global Cache cleared completely.");
    }
}