package it.uniroma2.hoophub.dao.helper_dao;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import it.uniroma2.hoophub.exception.DAOException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Utility class providing static methods to read and write CSV files.
 * <p>
 * This implementation explicitly uses the <strong>Decorator Pattern</strong> for Java I/O streams:
 * <ul>
 * <li><strong>FileInputStream/FileOutputStream</strong>: Low-level byte stream (Component)</li>
 * <li><strong>InputStreamReader/OutputStreamWriter</strong>: Bridge from bytes to characters (Decorator/Adapter)</li>
 * <li><strong>BufferedReader/BufferedWriter</strong>: High-level buffering for performance (Concrete Decorator)</li>
 * <li><strong>CSVReader/CSVWriter</strong>: Application-level abstraction</li>
 * </ul>
 * This layered approach ensures correct character set handling (UTF-8) and optimal performance.
 * </p>
 */
public class CsvUtilities {

    private CsvUtilities() {
        /* Utility class - no instance */
    }

    public static final String ERR_ACCESS = "Errore di I/O accedendo al file: %s";
    public static final String ERR_PARSER = "Errore durante il parsing del file CSV: %s";
    public static final String ERR_MOVE_FILE = "Errore durante il trasferimento del file da %s a %s";

    /**
     * Reads all rows from a specified CSV file.
     * Uses the Decorator pattern to build a buffered character stream from a file byte stream.
     *
     * @param fd The CSV file to be read.
     * @return A list of string arrays, where each array represents a row.
     * @throws DAOException If access or parsing fails.
     */
    public static List<String[]> readAll(File fd) throws DAOException {
        // DECORATOR PATTERN: File -> FileInputStream -> InputStreamReader -> BufferedReader -> CSVReader
        try (FileInputStream fis = new FileInputStream(fd);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr);
             CSVReader reader = new CSVReader(br)) {

            return reader.readAll();

        } catch (IOException e) {
            throw new DAOException(String.format(ERR_ACCESS, fd), e);
        } catch (CsvException e) {
            throw new DAOException(String.format(ERR_PARSER, fd), e);
        }
    }

    /**
     * Updates a CSV file by rewriting it entirely to a temp file first.
     * Uses the Decorator pattern for efficient writing.
     *
     * @param fd     The original CSV file.
     * @param header The header row (if needed).
     * @param table  The data rows.
     * @throws DAOException If writing fails.
     */
    public static synchronized void updateFile(File fd, String[] header, List<String[]> table) throws DAOException {
        File fdTmp = new File(fd.getAbsolutePath() + ".tmp");

        // DECORATOR PATTERN: File -> FileOutputStream -> OutputStreamWriter -> BufferedWriter -> CSVWriter
        try (FileOutputStream fos = new FileOutputStream(fdTmp);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             CSVWriter writer = new CSVWriter(bw)) {

            // Se header è specificato, scrivilo come prima riga
            // Controllo opzionale: se la tabella contiene già header alla riga 0, evita di duplicarlo
            if (header != null && (table.isEmpty() || !isHeaderPresent(table, header))) {
                writer.writeNext(header);
            }

            writer.writeAll(table);

        } catch (IOException e) {
            throw new DAOException(String.format(ERR_ACCESS, fdTmp), e);
        }

        // Atomic move (replace)
        try {
            Files.move(fdTmp.toPath(), fd.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DAOException(String.format(ERR_MOVE_FILE, fdTmp, fd), e);
        }
    }

    /**
     * Appends a single record to the CSV file.
     * Uses the Decorator pattern with append mode enabled.
     *
     * @param fd          The CSV file.
     * @param tableRecord The row data to append.
     * @throws DAOException If writing fails.
     */
    public static synchronized void writeFile(File fd, String[] tableRecord) throws DAOException {
        // DECORATOR PATTERN con flag 'append = true' nel FileOutputStream
        try (FileOutputStream fos = new FileOutputStream(fd, true);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             CSVWriter writer = new CSVWriter(bw)) {

            writer.writeNext(tableRecord);

        } catch (IOException e) {
            throw new DAOException(String.format(ERR_ACCESS, fd), e);
        }
    }

    /**
     * Helper to check if the first row of data matches the header.
     */
    private static boolean isHeaderPresent(List<String[]> table, String[] header) {
        if (table.isEmpty()) return false;
        String[] firstRow = table.getFirst();
        if (firstRow.length != header.length) return false;
        for (int i = 0; i < header.length; i++) {
            if (!firstRow[i].equals(header[i])) return false;
        }
        return true;
    }
}
