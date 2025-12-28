package it.uniroma2.hoophub.patterns.adapter;

import it.uniroma2.hoophub.api.MockNbaScheduleApi;
import it.uniroma2.hoophub.api.dto.NbaApiDto; // <--- IMPORT AGGIORNATO
import it.uniroma2.hoophub.model.NbaGame;
import it.uniroma2.hoophub.utilities.NbaGameMapper;

import java.util.List;

public class NbaApiAdapter implements NbaScheduleService {

    private final MockNbaScheduleApi externalApi;

    public NbaApiAdapter() {
        this.externalApi = new MockNbaScheduleApi();
    }

    @Override
    public List<NbaGame> getScheduledGames() {
        List<NbaApiDto> rawData = externalApi.fetchRawGames();
        return NbaGameMapper.fromApiList(rawData);
    }
}
