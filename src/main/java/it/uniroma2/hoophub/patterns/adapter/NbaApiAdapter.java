package it.uniroma2.hoophub.patterns.adapter;

import it.uniroma2.hoophub.api.MockNbaScheduleApi;
import it.uniroma2.hoophub.api.dto.NbaApiDto;
import it.uniroma2.hoophub.model.NbaGame;
import it.uniroma2.hoophub.utilities.NbaGameMapper;

import java.util.List;

/**
 * Adapter implementation for the <b>Adapter pattern (GoF)</b>.
 *
 * <p>Adapts the {@link MockNbaScheduleApi} (Adaptee) to the
 * {@link NbaScheduleService} (Target) interface. Converts raw
 * API DTOs to domain model objects using {@link NbaGameMapper}.</p>
 *
 * <p>Pattern roles:
 * <ul>
 *   <li><b>Target</b>: {@link NbaScheduleService}</li>
 *   <li><b>Adapter</b>: This class</li>
 *   <li><b>Adaptee</b>: {@link MockNbaScheduleApi}</li>
 * </ul>
 * </p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
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