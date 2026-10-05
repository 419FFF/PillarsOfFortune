package com.slop.pof.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.game.Game;
import com.slop.pof.storage.Stats;
import com.slop.pof.storage.TopEntry;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI identifiers under {@code %pof_*%}.
 * Personal stats need a player. Leaderboard placeholders work without one.
 */
public final class PofPlaceholders extends PlaceholderExpansion {
    private final PoFPlugin plugin;

    public PofPlaceholders(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pof";
    }

    @Override
    public String getAuthor() {
        return "example.com";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null || params.isBlank()) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("top_")) {
            return top(key);
        }
        Game game = plugin.game();
        if ("queue".equals(key)) {
            return String.valueOf(game.queueSize());
        }
        if (player == null) {
            return "";
        }
        Stats stats = plugin.database().get(player.getUniqueId());
        Game.Session session = game.peek(player.getUniqueId());
        return switch (key) {
            case "wins" -> stat(stats, stats == null ? 0 : stats.wins);
            case "kills" -> stat(stats, stats == null ? 0 : stats.kills);
            case "deaths" -> stat(stats, stats == null ? 0 : stats.deaths);
            case "games" -> stat(stats, stats == null ? 0 : stats.games);
            case "streak" -> stat(stats, stats == null ? 0 : stats.streak);
            case "best_streak", "beststreak" -> stat(stats, stats == null ? 0 : stats.bestStreak);
            case "items" -> stat(stats, stats == null ? 0 : stats.items);
            case "playtime", "playtime_min" -> stat(stats, stats == null ? 0 : stats.playtimeMin);
            case "level" -> stat(stats, stats == null ? 1 : Math.max(1, stats.level));
            case "xp" -> stat(stats, stats == null ? 0 : stats.xp);
            case "xp_next" -> stat(stats, plugin.settings().levels().nextCost(stats == null ? 1 : stats.level));
            case "name" -> player.getName();
            case "arena" -> session == null || session.arenaId == 0 ? "0" : String.valueOf(session.arenaId);
            case "alive" -> session != null && session.alive ? "true" : "false";
            case "state" -> state(session);
            case "session_kills" -> session == null ? "0" : String.valueOf(session.kills);
            default -> "";
        };
    }

    private String top(String key) {
        String[] parts = key.split("_");
        if (parts.length < 3) {
            return "";
        }
        int rank;
        try {
            rank = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return "";
        }
        if (rank < 1) {
            return "";
        }
        List<TopEntry> rows = plugin.game().top();
        if (rank > rows.size()) {
            return "";
        }
        TopEntry entry = rows.get(rank - 1);
        String field = parts[2];
        if ("name".equals(field)) {
            return entry.name;
        }
        if ("wins".equals(field) || "value".equals(field) || "kills".equals(field)
                || "streak".equals(field) || "level".equals(field)) {
            return String.valueOf(entry.value);
        }
        return "";
    }

    private static String stat(Stats stats, int value) {
        return String.valueOf(value);
    }

    private String state(Game.Session session) {
        if (session == null || session.arenaId == 0) {
            return "LOBBY";
        }
        Arena arena = plugin.arenas().get(session.arenaId);
        return arena == null ? "LOBBY" : arena.state.name();
    }
}
