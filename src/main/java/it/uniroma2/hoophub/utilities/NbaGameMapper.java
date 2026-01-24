package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.api.dto.NbaApiDto; // <--- IMPORT AGGIORNATO
import it.uniroma2.hoophub.enums.TeamNBA;
import it.uniroma2.hoophub.model.NbaGame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NbaGameMapper {
    private static final Logger logger = Logger.getLogger(NbaGameMapper.class.getName());

    private NbaGameMapper() {}

    public static List<NbaGame> fromApiList(List<NbaApiDto> rawList) {
        if (rawList == null || rawList.isEmpty()) return List.of();

        return rawList.stream()
                .map(NbaGameMapper::mapToModel)
                .filter(Objects::nonNull)
                .toList();
    }

    private static NbaGame mapToModel(NbaApiDto dto) {
        try {
            return new NbaGame.Builder()
                    .gameId(dto.id())
                    .homeTeam(TeamNBA.robustValueOf(dto.homeTeamCode()))
                    .awayTeam(TeamNBA.robustValueOf(dto.awayTeamCode()))
                    .date(LocalDate.parse(dto.date()))
                    .time(LocalTime.parse(dto.time()))
                    .build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Skipping invalid game: {0}", e.getMessage());
            return null;
        }
    }
}

