package it.uniroma2.hoophub.patterns.observer;

/**
 * Enumeration of database operations that trigger observer notifications.
 *
 * <p>Used by the <b>Observer pattern (GoF)</b> implementation
 * in cross-persistence synchronization.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public enum DaoOperation {
    /** Entity insertion operation. */
    INSERT,
    /** Entity update operation. */
    UPDATE,
    /** Entity deletion operation. */
    DELETE
}