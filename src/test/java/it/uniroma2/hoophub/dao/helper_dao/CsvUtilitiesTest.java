package it.uniroma2.hoophub.dao.helper_dao;

import it.uniroma2.hoophub.exception.DAOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Suite for CsvUtilities.
 * Follows ISPW guidelines: 1 Assert per method.
 */
class CsvUtilitiesTest {

    private static final String TEST_FILE_NAME = "test_decorator.csv";
    private File testFile;

    @BeforeEach
    void setUp() {
        testFile = new File(TEST_FILE_NAME);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(testFile.toPath());
    }

    /**
     * Test Case 1: Write and Read standard data.
     * Verifies that data written is exactly retrieved.
     */
    @Test
    void testUpdateFile_StandardData() throws DAOException {
        // ARRANGE
        String[] header = {"ID", "Name"};
        String[] row1 = {"1", "Mario"};
        List<String[]> dataToWrite = new ArrayList<>();
        dataToWrite.add(row1);

        // ACT
        CsvUtilities.updateFile(testFile, header, dataToWrite);
        List<String[]> result = CsvUtilities.readAll(testFile);

        // ASSERT
        // Usiamo assertAll per raggruppare i controlli in un'unica "asserzione logica"
        // e usiamo assertArrayEquals che confronta correttamente il contenuto degli array.
        assertAll("Verifica contenuto CSV",
                () -> assertEquals(2, result.size(), "Dimensione errata"),
                () -> assertArrayEquals(header, result.getFirst(), "Header errato"),
                () -> assertArrayEquals(row1, result.get(1), "Dati errati")
        );
    }

    /**
     * Test Case 2: Special Characters (UTF-8).
     * Verifies Decorator Pattern functionality for encoding.
     */
    @Test
    void testUpdateFile_SpecialCharacters() throws DAOException {
        // ARRANGE
        String[] header = {"Text"};
        // Caratteri critici: Euro, Accenti, Copyright
        String[] specialRow = {"€ 50.00 - Città - ©"};
        List<String[]> data = new ArrayList<>();
        data.add(specialRow);

        // ACT
        CsvUtilities.updateFile(testFile, header, data);
        List<String[]> result = CsvUtilities.readAll(testFile);

        // ASSERT
        // Verifichiamo solo la riga dati (indice 1)
        assertArrayEquals(specialRow, result.get(1), "I caratteri speciali devono essere preservati (UTF-8)");
    }

    /**
     * Test Case 3: Append functionality.
     * Verifies that new rows are added without overwriting.
     */
    @Test
    void testWriteFile_Append() throws DAOException {
        // ARRANGE
        // Initial setup
        CsvUtilities.updateFile(testFile, new String[]{"H"}, new ArrayList<>());
        String[] newRow = {"Appended"};

        // ACT
        CsvUtilities.writeFile(testFile, newRow);
        List<String[]> result = CsvUtilities.readAll(testFile);

        // ASSERT
        // Ci aspettiamo: Header + Row
        String[] lastRow = result.getLast();
        assertArrayEquals(newRow, lastRow, "L'ultima riga deve essere quella appena aggiunta");
    }

    /**
     * Test Case 4: Exception Handling (File not found / Access error).
     * Verifies that DAOException is thrown for invalid paths.
     */
    @Test
    void testReadAll_InvalidFile() {
        // ARRANGE
        File invalidFile = new File("/invalid/path/file.csv");

        // ACT & ASSERT (Single assert checking exception)
        assertThrows(DAOException.class, () -> CsvUtilities.readAll(invalidFile));
    }
}
