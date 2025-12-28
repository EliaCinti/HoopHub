package it.uniroma2.hoophub.patterns.adapter;

import it.uniroma2.hoophub.model.NbaGame;
import java.util.List;

/**
 * TARGET Interface (Pattern Adapter).
 * Definisce il contratto che il Controller dell'applicazione si aspetta di usare.
 * Il Controller userà questa interfaccia, ignorando se dietro c'è un file JSON, un DB o un'API reale.
 */
public interface NbaScheduleService {

    /**
     * Recupera la lista delle partite NBA in programma.
     * @return Una lista di oggetti di dominio NbaGame (già puliti e validati).
     */
    List<NbaGame> getScheduledGames();
}
