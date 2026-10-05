package com.slop.pof.storage;

import java.util.Locale;

/**
 * A sortable board. {@code column} is a fixed database column, never raw user input,
 * so it is safe to paste into SQL.
 */
public enum Leaderboard {
    WINS("wins", "wins"),
    KILLS("kills", "kills"),
    STREAK("best_streak", "streak"),
    LEVEL("level", "level");

    public final String column;
    /** Short label used in commands, messages, and placeholders. */
    public final String label;

    Leaderboard(String column, String label) {
        this.column = column;
        this.label = label;
    }

    /** @return the board for a command word, or null when the word is not a board */
    public static Leaderboard of(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "wins", "win" -> WINS;
            case "kills", "kill" -> KILLS;
            case "streak", "winstreak", "best", "beststreak" -> STREAK;
            case "level", "levels", "lvl" -> LEVEL;
            default -> null;
        };
    }
}
