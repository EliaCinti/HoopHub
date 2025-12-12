package it.uniroma2.hoophub.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // 1. Inizializzazione EAGER: Thread-safe per definizione e più semplice
    private static GlobalCache instance = new GlobalCache();

    // 2. ConcurrentHashMap: Gestisce accessi concorrenti senza corrompersi
    private final Map<String, Object> cacheMap;
    private final Logger logger;

    private GlobalCache() {
        this.cacheMap = new ConcurrentHashMap<>();
        this.logger = Logger.getLogger(GlobalCache.class.getName());
    }

    public static GlobalCache getInstance() {
        return instance;
    }

    public void put(String key, Object value) {
        if (key != null && value != null) {
            cacheMap.put(key, value);
            // Log a livello FINE
            logger.log(Level.FINE, "Cache PUT: {0}", key);
        }
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
        if (key != null) {
            cacheMap.remove(key);
            logger.log(Level.FINE, "Cache REMOVE: {0}", key);
        }
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
