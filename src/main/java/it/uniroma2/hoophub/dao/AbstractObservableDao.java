package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract base class for DAOs supporting the <b>Observer pattern (GoF)</b>.
 *
 * <p>Provides default implementation of {@link ObservableDao}, allowing concrete DAOs
 * to notify observers about data operations (INSERT, UPDATE, DELETE) for cross-persistence
 * synchronization.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see DaoObserver
 * @see DaoOperation
 */
public abstract class AbstractObservableDao implements ObservableDao {

    private static final Logger logger = Logger.getLogger(AbstractObservableDao.class.getName());
    private final List<DaoObserver> observers = new ArrayList<>();

    /**
     * Registers an observer. Duplicates are ignored. Thread-safe.
     *
     * @param observer the observer to add
     */
    @Override
    public synchronized void addObserver(DaoObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Unregisters an observer. Thread-safe.
     *
     * @param observer the observer to remove
     */
    @Override
    public synchronized void removeObserver(DaoObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers about a DAO operation.
     *
     * <p>Creates a defensive copy to avoid ConcurrentModificationException.
     * Observers must check {@link it.uniroma2.hoophub.sync.SyncContext} to prevent
     * circular updates during synchronization.</p>
     *
     * @param operation  the operation type (INSERT, UPDATE, DELETE)
     * @param entityType the affected entity type name
     * @param entityId   the entity identifier
     * @param entity     the entity object (null for DELETE)
     */
    @Override
    public void notifyObservers(DaoOperation operation, String entityType, String entityId, Object entity) {
        List<DaoObserver> observersCopy = new ArrayList<>(observers);

        for (DaoObserver observer : observersCopy) {
            try {
                switch (operation) {
                    case INSERT -> observer.onAfterInsert(entityType, entityId, entity);
                    case UPDATE -> observer.onAfterUpdate(entityType, entityId, entity);
                    case DELETE -> observer.onAfterDelete(entityType, entityId);
                }
            } catch (Exception e) {
                logger.severe("Observer notification failed: " + e.getMessage());
            }
        }
    }
}