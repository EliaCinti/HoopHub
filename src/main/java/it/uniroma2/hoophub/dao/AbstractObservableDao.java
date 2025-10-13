package it.uniroma2.hoophub.dao;

import it.uniroma2.hoophub.patterns.observer.DaoObserver;
import it.uniroma2.hoophub.patterns.observer.DaoOperation;
import it.uniroma2.hoophub.patterns.observer.ObservableDao;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract base class for Data Access Objects that support the Observer pattern.
 * <p>
 * This class provides a default implementation of the {@link ObservableDao} interface,
 * allowing concrete DAO implementations to notify observers about data operations
 * such as insertions, updates, and deletions.
 * </p>
 */
public abstract class AbstractObservableDao implements ObservableDao {

    private static final Logger logger = Logger.getLogger(AbstractObservableDao.class.getName());
    private final List<DaoObserver> observers = new ArrayList<>();

    /**
     * Adds an observer to the list of observers for this DAO.
     * If the observer is already registered, it will not be added again.
     * This method is thread-safe.
     *
     * @param observer The {@link DaoObserver} to add. Must not be {@code null}.
     */
    @Override
    public synchronized void addObserver(DaoObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Removes an observer from the list of observers for this DAO.
     * This method is thread-safe.
     *
     * @param observer The {@link DaoObserver} to remove. Must not be {@code null}.
     */
    @Override
    public synchronized void removeObserver(DaoObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers about a DAO operation.
     * <p>
     * This method creates a copy of the observers list to avoid
     * {@code ConcurrentModificationException} and calls the appropriate
     * observer method based on the operation type.
     * </p>
     *
     * @param operation The type of operation that was performed
     * @param entityType The type of entity that was affected
     * @param entityId The identifier of the specific entity
     * @param entity The entity object (for INSERT and UPDATE), or {@code null} for DELETE
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