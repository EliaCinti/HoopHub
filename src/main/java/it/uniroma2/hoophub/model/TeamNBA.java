package it.uniroma2.hoophub.model;

/**
 * Enumeration representing all 30 NBA teams.
 * Provides type-safe team management and prevents invalid team names.
 *
 * Each team includes:
 * - Display name (full team name)
 * - Abbreviation (3-letter code)
 */
public enum TeamNBA {
    // Eastern Conference - Atlantic Division
    BOSTON_CELTICS("Boston Celtics", "BOS"),
    BROOKLYN_NETS("Brooklyn Nets", "BKN"),
    NEW_YORK_KNICKS("New York Knicks", "NYK"),
    PHILADELPHIA_76ERS("Philadelphia 76ers", "PHI"),
    TORONTO_RAPTORS("Toronto Raptors", "TOR"),

    // Eastern Conference - Central Division
    CHICAGO_BULLS("Chicago Bulls", "CHI"),
    CLEVELAND_CAVALIERS("Cleveland Cavaliers", "CLE"),
    DETROIT_PISTONS("Detroit Pistons", "DET"),
    INDIANA_PACERS("Indiana Pacers", "IND"),
    MILWAUKEE_BUCKS("Milwaukee Bucks", "MIL"),

    // Eastern Conference - Southeast Division
    ATLANTA_HAWKS("Atlanta Hawks", "ATL"),
    CHARLOTTE_HORNETS("Charlotte Hornets", "CHA"),
    MIAMI_HEAT("Miami Heat", "MIA"),
    ORLANDO_MAGIC("Orlando Magic", "ORL"),
    WASHINGTON_WIZARDS("Washington Wizards", "WAS"),

    // Western Conference - Northwest Division
    DENVER_NUGGETS("Denver Nuggets", "DEN"),
    MINNESOTA_TIMBERWOLVES("Minnesota Timberwolves", "MIN"),
    OKLAHOMA_CITY_THUNDER("Oklahoma City Thunder", "OKC"),
    PORTLAND_TRAIL_BLAZERS("Portland Trail Blazers", "POR"),
    UTAH_JAZZ("Utah Jazz", "UTA"),

    // Western Conference - Pacific Division
    GOLDEN_STATE_WARRIORS("Golden State Warriors", "GSW"),
    LA_CLIPPERS("LA Clippers", "LAC"),
    LOS_ANGELES_LAKERS("Los Angeles Lakers", "LAL"),
    PHOENIX_SUNS("Phoenix Suns", "PHX"),
    SACRAMENTO_KINGS("Sacramento Kings", "SAC"),

    // Western Conference - Southwest Division
    DALLAS_MAVERICKS("Dallas Mavericks", "DAL"),
    HOUSTON_ROCKETS("Houston Rockets", "HOU"),
    MEMPHIS_GRIZZLIES("Memphis Grizzlies", "MEM"),
    NEW_ORLEANS_PELICANS("New Orleans Pelicans", "NOP"),
    SAN_ANTONIO_SPURS("San Antonio Spurs", "SAS");

    private final String displayName;
    private final String abbreviation;

    /**
     * Constructor for TeamNBA enum.
     *
     * @param displayName the full team name for display purposes
     * @param abbreviation the 3-letter team abbreviation
     */
    TeamNBA(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    /**
     * Gets the full team name for display.
     *
     * @return the team's display name (e.g., "Los Angeles Lakers")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the team's 3-letter abbreviation.
     *
     * @return the team abbreviation (e.g., "LAL")
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Finds a team by its display name (case-insensitive).
     *
     * @param displayName the full team name to search for
     * @return the matching TeamNBA enum, or null if not found
     */
    public static TeamNBA fromDisplayName(String displayName) {
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

    /**
     * Finds a team by its abbreviation (case-insensitive).
     *
     * @param abbreviation the 3-letter team code to search for
     * @return the matching TeamNBA enum, or null if not found
     */
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

    @Override
    public String toString() {
        return displayName;
    }
}
