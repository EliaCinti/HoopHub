package it.uniroma2.hoophub.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton per la gestione centralizzata della cache in memoria.
 * <p>
 * Questa classe agisce come un repository in-memory condiviso per tutte le DAO.
 * Permette di salvare e recuperare entità usando chiavi univoche (es. "Booking:1"),
 * prevenendo accessi ridondanti al database all'interno della stessa sessione.
 * </p>
 */
@SuppressWarnings("java:S6548") // Singleton is required
public class GlobalCache {

    private static GlobalCache instance;
    private final Map<String, Object> cacheMap;
    private final Logger logger;

    private GlobalCache() {
        this.cacheMap = new HashMap<>();
        this.logger = Logger.getLogger(GlobalCache.class.getName());
    }

    public static GlobalCache getInstance() {
        if (instance == null) {
            instance = new GlobalCache();
        }
        return instance;
    }

    public void put(String key, Object value) {
        cacheMap.put(key, value);
        // Log a livello FINE (utile per debug ma non intasa la console in produzione)
        logger.log(Level.FINE, "Cache PUT: {0}", key);
    }

    public Object get(String key) {
        Object value = cacheMap.get(key);
        if (value != null) {
            logger.log(Level.FINE, "Cache HIT: {0}", key);
        } else {
            logger.log(Level.FINE, "Cache MISS: {0}", key);
        }
        return value;
    }

    public void remove(String key) {
        cacheMap.remove(key);
        logger.log(Level.FINE, "Cache REMOVE: {0}", key);
    }

    /**
     * Svuota completamente la cache.
     * Da chiamare al Logout dell'utente.
     */
    public void clearAll() {
        cacheMap.clear();
        logger.log(Level.INFO, "Global Cache cleared completely.");
    }
}
