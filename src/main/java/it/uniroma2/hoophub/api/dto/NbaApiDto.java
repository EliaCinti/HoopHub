package it.uniroma2.hoophub.api.dto;

/**
 * DTO for deserializing NBA game data from the external API JSON response.
 *
 * <p>Pure data carrier with no business logic. Field names match the JSON structure
 * for direct Jackson mapping.</p>
 *
 * @param id           unique game identifier from the API
 * @param homeTeamCode three-letter home team code (e.g., "LAL")
 * @param awayTeamCode three-letter away team code (e.g., "BOS")
 * @param date         game date as string (ISO-8601 format)
 * @param time         game start time as string ("HH:mm")
 * @param arenaName    name of the arena
 *
 * @author Elia Cinti
 * @version 1.0
 */
public record NbaApiDto(
        String id,
        String homeTeamCode,
        String awayTeamCode,
        String date,
        String time,
        String arenaName
) {}
