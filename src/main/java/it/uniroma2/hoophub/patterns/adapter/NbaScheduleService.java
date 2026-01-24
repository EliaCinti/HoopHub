package it.uniroma2.hoophub.patterns.adapter;

import it.uniroma2.hoophub.model.NbaGame;
import java.util.List;

/**
 * Target interface for the <b>Adapter pattern (GoF)</b>.
 *
 * <p>Defines the contract that application controllers expect.
 * The controller uses this interface, decoupled from the actual
 * data source (JSON file, database, or real API).</p>
 *
 * @author Elia Cinti
 * @version 1.0
 * @see NbaApiAdapter
 */
public interface NbaScheduleService {

    /**
     * Retrieves scheduled NBA games.
     *
     * @return list of validated NbaGame domain objects
     */
    List<NbaGame> getScheduledGames();
}
