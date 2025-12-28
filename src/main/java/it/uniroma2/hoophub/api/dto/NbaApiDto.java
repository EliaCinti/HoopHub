package it.uniroma2.hoophub.api.dto;

/**
 * DTO puro per mappare il JSON in arrivo dall'API.
 * Risiede nel package api.dto perché è un dettaglio implementativo dell'integrazione API.
 */
public record NbaApiDto(
        String id,
        String homeTeamCode,
        String awayTeamCode,
        String date,
        String time,
        String arenaName
) {}
