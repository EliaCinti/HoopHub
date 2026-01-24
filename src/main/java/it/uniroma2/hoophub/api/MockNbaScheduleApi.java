package it.uniroma2.hoophub.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniroma2.hoophub.api.dto.NbaApiDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mock implementation of an NBA schedule API client.
 *
 * <p>Simulates an external NBA API by reading game data from a local JSON file
 * ({@code /json/nba_schedule_mock.json}) for development and testing purposes.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see NbaApiDto
 */
public class MockNbaScheduleApi {

    private static final Logger logger = Logger.getLogger(MockNbaScheduleApi.class.getName());
    private static final String MOCK_FILE = "/json/nba_schedule_mock.json";

    /**
     * Fetches NBA game schedule data from the mock JSON file.
     *
     * <p>Uses Jackson with {@link JavaTimeModule} for deserialization.
     * Returns an empty list if the file is not found or parsing fails.</p>
     *
     * @return list of {@link NbaApiDto} representing scheduled games, or empty list on error
     */
    public List<NbaApiDto> fetchRawGames() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try (InputStream is = getClass().getResourceAsStream(MOCK_FILE)) {
            if (is == null) {
                logger.log(Level.SEVERE, "File JSON non trovato: {0}", MOCK_FILE);
                return List.of();
            }
            return mapper.readValue(is, new TypeReference<>() {
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore parsing API Mock", e);
            return List.of();
        }
    }
}
