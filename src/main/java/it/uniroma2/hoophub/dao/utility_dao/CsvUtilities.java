package it.uniroma2.hoophub.dao.utility_dao;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import it.uniroma2.hoophub.exception.DAOException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Utility class providing static methods to read and write CSV files. This class is designed to handle
 * common CSV operations such as reading all data, updating a file, and writing new records.
 */
public class CsvUtilities {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CsvUtilities() {
        /* no instance */
    }

    public static final String ERR_ACCESS = "Errore di I/O accedendo al file: %s";
    public static final String ERR_PARSER = "Errore durante il parsing del file CSV: %s";
    public static final String ERR_MOVE_FILE = "Errore durante il trasferimento del file da %s a %s";

    /**
     * Reads all rows from a specified CSV file and returns them as a list of string arrays.
     * Each string array in the list represents a row from the CSV, where each element
     * in the array represents a column value.
     *
     * @param fd The CSV file to be read.
     * @return A list of string arrays, where each array represents a row in the CSV file.
     * @throws DAOException If there are any issues accessing or parsing the CSV file.
     */
    public static List<String[]> readAll(File fd) throws DAOException {
        try (CSVReader reader = new CSVReader(new FileReader(fd))) {
            return reader.readAll();
        } catch (IOException e) {
            throw new DAOException(String.format(ERR_ACCESS, fd), e);
        } catch (CsvException e) {
            throw new DAOException(String.format(ERR_PARSER, fd), e);
        }
    }

    /**
     * Updates a CSV file by writing a new table of data to a temporary file, including a specified header.
     * After successful writing, the temporary file replaces the original file.
     * This method is synchronized to ensure thread safety when accessing the file system.
     *
     * @param fd     The CSV file to update.
     * @param header An array representing the header row, to be added at the beginning of the file.
     * @param table  A list of string arrays, where each array represents a row in the updated CSV file.
     * @throws DAOException If there are any issues writing to or replacing the original CSV file.
     */
    public static synchronized void updateFile(File fd, String[] header, List<String[]> table) throws DAOException {
        File fdTmp = new File(fd.getAbsolutePath() + ".tmp");
        try (CSVWriter writer = new CSVWriter(new FileWriter(fdTmp))) {
            table.addFirst(header);  // Adding header as the first row
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
     * Writes a single record to the end of a specified CSV file.
     *
     * @param fd          The CSV file to which the record will be appended.
     * @param tableRecord An array of strings, each representing a column value of the record to be written.
     * @throws DAOException If there is an error writing to the CSV file.
     */
    public static synchronized void writeFile(File fd, String[] tableRecord) throws DAOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(fd, true))) {
            writer.writeNext(tableRecord);
        } catch (IOException e) {
            throw new DAOException(e.getMessage());
        }
    }
}
