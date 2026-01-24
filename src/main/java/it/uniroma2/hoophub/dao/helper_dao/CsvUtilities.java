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
 * Utility class for CSV file operations.
 *
 * <p>Uses the <b>Decorator pattern (GoF)</b> for Java I/O streams:</p>
 * <ul>
 *   <li>FileInputStream/FileOutputStream: byte stream (Component)</li>
 *   <li>InputStreamReader/OutputStreamWriter: byte-to-char bridge (Decorator)</li>
 *   <li>BufferedReader/BufferedWriter: buffering for performance (Concrete Decorator)</li>
 *   <li>CSVReader/CSVWriter: application-level CSV abstraction</li>
 * </ul>
 *
 * <p>All operations use UTF-8 encoding.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class CsvUtilities {

    private CsvUtilities() {
        // Utility class
    }

    public static final String ERR_ACCESS = "I/O error accessing file: %s";
    public static final String ERR_PARSER = "Error parsing CSV file: %s";
    public static final String ERR_MOVE_FILE = "Error moving file from %s to %s";

    /**
     * Reads all rows from a CSV file.
     *
     * @param fd the CSV file
     * @return list of rows (each row is a String array)
     * @throws DAOException if reading or parsing fails
     */
    public static List<String[]> readAll(File fd) throws DAOException {
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
     * Rewrites a CSV file atomically using a temp file.
     *
     * <p>Writes to a temporary file first, then performs an atomic move
     * to ensure data integrity.</p>
     *
     * @param fd     the target CSV file
     * @param header the header row (written if not already present)
     * @param table  the data rows
     * @throws DAOException if writing fails
     */
    public static synchronized void updateFile(File fd, String[] header, List<String[]> table) throws DAOException {
        File fdTmp = new File(fd.getAbsolutePath() + ".tmp");

        try (FileOutputStream fos = new FileOutputStream(fdTmp);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             CSVWriter writer = new CSVWriter(bw)) {

            if (header != null && (table.isEmpty() || !isHeaderPresent(table, header))) {
                writer.writeNext(header);
            }
            writer.writeAll(table);

        } catch (IOException e) {
            throw new DAOException(String.format(ERR_ACCESS, fdTmp), e);
        }

        try {
            Files.move(fdTmp.toPath(), fd.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DAOException(String.format(ERR_MOVE_FILE, fdTmp, fd), e);
        }
    }

    /**
     * Appends a single record to a CSV file.
     *
     * @param fd          the CSV file
     * @param tableRecord the row to append
     * @throws DAOException if writing fails
     */
    public static synchronized void writeFile(File fd, String[] tableRecord) throws DAOException {
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
     * Checks if the first row matches the header.
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