package it.uniroma2.hoophub.enums;

/**
 * Enumeration of all 30 NBA teams.
 *
 * <p>Provides type-safe team management with multiple lookup methods:
 * by abbreviation, display name, enum name, or fuzzy input matching.</p>
 *
 * @author Elia Cinti
 * @version 1.0
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

    TeamNBA(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    /**
     * Returns the full team name (e.g., "Los Angeles Lakers").
     *
     * @return human-readable team name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the 3-letter abbreviation (e.g., "LAL").
     *
     * @return team abbreviation
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Finds a team by its 3-letter abbreviation.
     *
     * @param abbreviation the abbreviation to search (case-insensitive)
     * @return matching team or null if not found
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

    /**
     * Finds a team by loose matching for UI input.
     *
     * <p>Matches against: exact abbreviation, partial display name,
     * or enum name. Example: "Lakers" → {@code LOS_ANGELES_LAKERS}.</p>
     *
     * @param input user input string
     * @return matching team or null if not found
     */
    public static TeamNBA fromInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String normalizedInput = input.trim();

        for (TeamNBA team : TeamNBA.values()) {
            if (team.abbreviation.equalsIgnoreCase(normalizedInput)) {
                return team;
            }
            if (team.displayName.toLowerCase().contains(normalizedInput.toLowerCase())) {
                return team;
            }
            if (team.name().equalsIgnoreCase(normalizedInput)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Robust parsing from any string format.
     *
     * <p>Attempts matching in order:
     * <ol>
     *   <li>Display name ("Los Angeles Lakers")</li>
     *   <li>Abbreviation ("LAL")</li>
     *   <li>Enum name ("LOS_ANGELES_LAKERS")</li>
     *   <li>Case-insensitive scan</li>
     * </ol>
     * Used by CSV/MySQL DAOs for flexible data parsing.</p>
     *
     * @param value string to parse
     * @return matching team or null if not found
     */
    public static TeamNBA robustValueOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String sanitized = value.trim();

        TeamNBA team = fromDisplayName(sanitized);

        if (team == null) {
            team = fromAbbreviation(sanitized);
        }

        if (team == null) {
            try {
                team = TeamNBA.valueOf(sanitized);
            } catch (IllegalArgumentException e) {
                // Continue to fallback
            }
        }

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

    /**
     * Finds a team by exact display name match.
     *
     * @param displayName the display name to search (case-insensitive)
     * @return matching team or null if not found
     */
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

    /**
     * Returns the display name for UI presentation.
     *
     * @return same as {@link #getDisplayName()}
     */
    @Override
    public String toString() {
        return displayName;
    }
}