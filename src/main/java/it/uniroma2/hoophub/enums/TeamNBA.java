package it.uniroma2.hoophub.enums;

/**
 * Enumeration representing all 30 NBA teams.
 * Provides type-safe team management and prevents invalid team names.
 */
public enum TeamNBA {
    BOSTON_CELTICS("Boston Celtics", "BOS"),
    BROOKLYN_NETS("Brooklyn Nets", "BKN"),
    NEW_YORK_KNICKS("New York Knicks", "NYK"),
    PHILADELPHIA_76ERS("Philadelphia 76ers", "PHI"),
    TORONTO_RAPTORS("Toronto Raptors", "TOR"),
    CHICAGO_BULLS("Chicago Bulls", "CHI"),
    CLEVELAND_CAVALIERS("Cleveland Cavaliers", "CLE"),
    DETROIT_PISTONS("Detroit Pistons", "DET"),
    INDIANA_PACERS("Indiana Pacers", "IND"),
    MILWAUKEE_BUCKS("Milwaukee Bucks", "MIL"),
    ATLANTA_HAWKS("Atlanta Hawks", "ATL"),
    CHARLOTTE_HORNETS("Charlotte Hornets", "CHA"),
    MIAMI_HEAT("Miami Heat", "MIA"),
    ORLANDO_MAGIC("Orlando Magic", "ORL"),
    WASHINGTON_WIZARDS("Washington Wizards", "WAS"),
    DENVER_NUGGETS("Denver Nuggets", "DEN"),
    MINNESOTA_TIMBERWOLVES("Minnesota Timberwolves", "MIN"),
    OKLAHOMA_CITY_THUNDER("Oklahoma City Thunder", "OKC"),
    PORTLAND_TRAIL_BLAZERS("Portland Trail Blazers", "POR"),
    UTAH_JAZZ("Utah Jazz", "UTA"),
    GOLDEN_STATE_WARRIORS("Golden State Warriors", "GSW"),
    LA_CLIPPERS("LA Clippers", "LAC"),
    LOS_ANGELES_LAKERS("Los Angeles Lakers", "LAL"),
    PHOENIX_SUNS("Phoenix Suns", "PHX"),
    SACRAMENTO_KINGS("Sacramento Kings", "SAC"),
    DALLAS_MAVERICKS("Dallas Mavericks", "DAL"),
    HOUSTON_ROCKETS("Houston Rockets", "HOU"),
    MEMPHIS_GRIZZLIES("Memphis Grizzlies", "MEM"),
    NEW_ORLEANS_PELICANS("New Orleans Pelicans", "NOP"),
    SAN_ANTONIO_SPURS("San Antonio Spurs", "SAS");

    private final String displayName;
    private final String abbreviation;

    TeamNBA(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static TeamNBA fromAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            return null;
        }
        for (TeamNBA team : TeamNBA.values()) {
            if (team.abbreviation.equalsIgnoreCase(abbreviation.trim())) {
                return team;
            }
        }
        return null;
    }

    /**
     * Finds a team by loose matching (abbreviation, full name, or partial name).
     * Used by the UI to help users find teams easily (e.g. input "Lakers" -> finds LOS_ANGELES_LAKERS).
     *
     * @param input The user input string.
     * @return The matching TeamNBA enum, or null if not found.
     */
    public static TeamNBA fromInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String normalizedInput = input.trim();

        for (TeamNBA team : TeamNBA.values()) {
            // 1. Exact Abbreviation (e.g. "LAL")
            if (team.abbreviation.equalsIgnoreCase(normalizedInput)) {
                return team;
            }
            // 2. Contains Check (e.g. "Lakers" in "Los Angeles Lakers")
            if (team.displayName.toLowerCase().contains(normalizedInput.toLowerCase())) {
                return team;
            }
            // 3. Enum Name (e.g. "LOS_ANGELES_LAKERS")
            if (team.name().equalsIgnoreCase(normalizedInput)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Tenta di parsare una stringa in un TeamNBA in modo robusto.
     * Supporta: Display Name ("Los Angeles Lakers"), Abbreviation ("LAL"),
     * Enum Name ("LOS_ANGELES_LAKERS") e Case Insensitive.
     *
     * @param value La stringa da parsare
     * @return L'istanza TeamNBA o null se non trovata
     */
    public static TeamNBA robustValueOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String sanitized = value.trim();

        // 1. Prova con Display Name (es. "Los Angeles Lakers")
        TeamNBA team = fromDisplayName(sanitized);

        // 2. Prova con Abbreviazione (es. "LAL")
        if (team == null) {
            team = fromAbbreviation(sanitized);
        }

        // 3. Prova con il nome standard dell'Enum (es. "LOS_ANGELES_LAKERS")
        if (team == null) {
            try {
                team = TeamNBA.valueOf(sanitized);
            } catch (IllegalArgumentException e) {
                // Ignora e prova l'ultimo tentativo
            }
        }

        // 4. Ultimo tentativo: Case Insensitive scan (es. "Lakers" o "lakers")
        if (team == null) {
            for (TeamNBA t : values()) {
                if (t.name().equalsIgnoreCase(sanitized) ||
                        t.getDisplayName().equalsIgnoreCase(sanitized) ||
                        t.getAbbreviation().equalsIgnoreCase(sanitized)) {
                    return t;
                }
            }
        }

        return team;
    }

    private static TeamNBA fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        for (TeamNBA team : TeamNBA.values()) {
            if (team.displayName.equalsIgnoreCase(displayName.trim())) {
                return team;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}