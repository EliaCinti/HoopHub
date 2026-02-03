package it.uniroma2.hoophub.patterns.observer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DaoOperation}.
 *
 * <p>Tests the enumeration of database operations that trigger
 * observer notifications in the cross-persistence sync system.</p>
 *
 * @author Elia Cinti
 */
class TestDaoOperation {

    @Test
    void testInsertExists() {
        assertNotNull(DaoOperation.INSERT);
    }

    @Test
    void testUpdateExists() {
        assertNotNull(DaoOperation.UPDATE);
    }

    @Test
    void testDeleteExists() {
        assertNotNull(DaoOperation.DELETE);
    }

    @Test
    void testValueOfInsert() {
        assertEquals(DaoOperation.INSERT, DaoOperation.valueOf("INSERT"));
    }

    @Test
    void testValueOfUpdate() {
        assertEquals(DaoOperation.UPDATE, DaoOperation.valueOf("UPDATE"));
    }

    @Test
    void testValueOfDelete() {
        assertEquals(DaoOperation.DELETE, DaoOperation.valueOf("DELETE"));
    }

    @Test
    void testValuesCountThree() {
        assertEquals(3, DaoOperation.values().length);
    }

    @Test
    void testValueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DaoOperation.valueOf("UPSERT"));
    }
}