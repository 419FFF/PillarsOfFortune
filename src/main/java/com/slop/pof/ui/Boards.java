package com.slop.pof.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.config.Gamemode;
import com.slop.pof.config.Settings;
import com.slop.pof.game.Game;
import com.slop.pof.storage.Stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Boards {
    private static final String[] ENTRIES = new String[16];

    static {
        ChatColor[] colors = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
                ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
                ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
                ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };
        for (int i = 0; i < colors.length; i++) {
            ENTRIES[i] = colors[i].toString() + ChatColor.WHITE;
        }
    }

    private final PoFPlugin plugin;
    private final Map<UUID, View> views = new HashMap<>();
    private final Set<String> uiErrors = new HashSet<>();

    public Boards(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh(Player player) {
        try {
            sidebar(player);
        } catch (Throwable ex) {
            warnOnce("Scoreboard failed for " + player.getName() + ": " + ex.getMessage());
        }
        try {
            tab(player);
        } catch (Throwable ex) {
            warnOnce("Tab failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    private void warnOnce(String message) {
        if (plugin.isDebug() || uiErrors.add(message)) {
            plugin.getLogger().warning(message);
        }
    }

    public void clear(Player player) {
        views.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        // No tab name formatting is applied anywhere, so resetting to the default is enough.
        player.setPlayerListName(null);
    }

    /** Rebuilds the sidebar and tab visibility for everyone. Used after /pof reload. */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void clearAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(other)) {
                    viewer.showPlayer(other);
                }
            }
            clear(viewer);
        }
        views.clear();
    }

    private void sidebar(Player player) {
        Settings settings = plugin.settings();
        Game game = plugin.game();
        Game.Session session = game.session(player);
        Stats stats = plugin.database().get(player.getUniqueId());
        Arena arena = session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        String mode = mode(session, arena, game);
        String modeId = game.queuedMode(player.getUniqueId());
        if (modeId == null && arena != null) {
            modeId = arena.gamemodeId;
        }
        Gamemode gamemode = settings.gamemode(modeId);
        int queue = modeId == null || !game.queued(player.getUniqueId())
                ? game.queueSize()
                : game.queueSize(modeId);
        int max = gamemode == null ? settings.maxPlayers() : gamemode.maxPlayers(settings);
        String modeLabel = gamemode == null ? "" : gamemode.name;
        String start = game.waitingForPlayers(player.getUniqueId())
                ? settings.text(player, "board-start-waiting")
                : settings.text(player, "board-start", "value", String.valueOf(game.queueTimer(player.getUniqueId())));
        int alive = arena == null ? 0 : arena.aliveCount();
        String rendered = settings.board(player, mode,
                "ip", settings.serverIp(),
                "wins", String.valueOf(stats == null ? 0 : stats.wins),
                "kills", String.valueOf(stats == null ? 0 : stats.kills),
                "deaths", String.valueOf(stats == null ? 0 : stats.deaths),
                "streak", String.valueOf(stats == null ? 0 : stats.streak),
                "best", String.valueOf(stats == null ? 0 : stats.bestStreak),
                "games", String.valueOf(stats == null ? 0 : stats.games),
                "items", String.valueOf(stats == null ? 0 : stats.items),
                "mins", String.valueOf(stats == null ? 0 : stats.playtimeMin),
                "queue", String.valueOf(queue),
                "count", String.valueOf(queue),
                "max", String.valueOf(max),
                "mode", modeLabel,
                "id", arena == null ? "" : String.valueOf(arena.id),
                "alive", String.valueOf(alive),
                "starts", arena == null ? "0" : String.valueOf(arena.count),
                "item", arena == null ? "0" : String.valueOf(arena.item),
                "time", arena == null ? "0" : String.valueOf(arena.time),
                "grace", arena == null ? "0" : String.valueOf(arena.grace),
                "session_kills", String.valueOf(session.kills),
                "state", arena == null ? "" : prettyState(arena),
                "clock", arena == null ? "-" : clock(arena),
                "start", start);
        String[] rows = sidebarLines(rendered);
        int body = rows.length - 1;
        View view = views.computeIfAbsent(player.getUniqueId(), id -> new View());
        if (view.board == null || view.rows != body) {
            view.board = Bukkit.getScoreboardManager().getNewScoreboard();
            view.objective = view.board.registerNewObjective("pof", "dummy");
            view.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            view.rows = body;
            player.setScoreboard(view.board);
        }
        if (!rows[0].equals(view.objective.getDisplayName())) {
            view.objective.setDisplayName(rows[0]);
        }
        for (int i = 0; i < body; i++) {
            line(view, body - i, rows[i + 1]);
        }
    }

    /** Title, then up to 15 body lines. A config {@code \n} or a real line break separates lines. */
    static String[] sidebarLines(String rendered) {
        String normalized = rendered == null ? "" : rendered.replace("\\n", "\n");
        String[] parts = normalized.split("\n", -1);
        int body = Math.min(15, Math.max(0, parts.length - 1));
        String[] out = new String[body + 1];
        String title = parts[0];
        if (title.isEmpty()) {
            title = " ";
        }
        out[0] = title.length() <= 32 ? title : title.substring(0, 32);
        for (int i = 0; i < body; i++) {
            String row = parts[i + 1];
            out[i + 1] = row.isEmpty() ? ChatColor.RESET.toString() : row;
        }
        return out;
    }

    /**
     * The tab list shows only the people the viewer should see: the same match, or the lobby.
     * No header, footer, name colours, or team ordering are sent, so nothing can break the tab.
     */
    private void tab(Player player) {
        applyVisibility(player, arenaOf(player));
    }

    /** Hides everyone who is not in the viewer's current match, or not in the lobby with them. */
    public void refreshVisibility() {
        if (plugin.game() == null) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyVisibility(viewer, arenaOf(viewer));
        }
    }

    /**
     * In a match, the tab lists only that match. In the lobby, it lists only the lobby,
     * so players who are in a round are not mixed in.
     */
    private void applyVisibility(Player viewer, Arena viewerArena) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(viewer)) {
                continue;
            }
            if (sameTab(viewerArena, arenaOf(other))) {
                viewer.showPlayer(other);
            } else {
                viewer.hidePlayer(other);
            }
        }
    }

    private boolean sameTab(Arena viewerArena, Arena otherArena) {
        if (viewerArena == null) {
            return otherArena == null;
        }
        return otherArena != null && viewerArena.id == otherArena.id;
    }

    private Arena arenaOf(Player player) {
        Game.Session session = plugin.game().peek(player.getUniqueId());
        if (session == null || session.arenaId == 0) {
            return null;
        }
        return plugin.arenas().get(session.arenaId);
    }

    private static String prettyState(Arena arena) {
        return switch (arena.state) {
            case WAITING -> ChatColor.GRAY + "Waiting";
            case STARTING -> ChatColor.YELLOW + "Starting";
            case INGAME -> arena.grace > 0 ? ChatColor.GREEN + "Grace" : ChatColor.GOLD + "Live";
            case ENDING -> ChatColor.GOLD + "Finished";
            case RESETTING -> ChatColor.DARK_GRAY + "Resetting";
        };
    }

    private static String clock(Arena arena) {
        return switch (arena.state) {
            case STARTING -> arena.count + "s";
            case INGAME -> minutes(arena.time);
            case ENDING -> minutes(arena.end);
            default -> "-";
        };
    }

    private static String minutes(int seconds) {
        int safe = Math.max(0, seconds);
        return (safe / 60) + ":" + (safe % 60 < 10 ? "0" : "") + (safe % 60);
    }

    private static String mode(Game.Session session, Arena arena, Game game) {
        String mode = "lobby";
        if (arena != null) {
            if (arena.state == Arena.State.STARTING) {
                mode = "starting";
            } else if (arena.state == Arena.State.ENDING) {
                mode = "ending";
            } else if (arena.state == Arena.State.INGAME) {
                mode = arena.grace > 0 ? "grace" : "ingame";
            }
        }
        if (game.queued(session.uuid) && "lobby".equals(mode)) {
            mode = "queued";
        }
        return mode;
    }

    private static void line(View view, int score, String text) {
        String entry = ENTRIES[score];
        Team team = view.board.getTeam("l" + score);
        if (team == null) {
            team = view.board.registerNewTeam("l" + score);
            team.addEntry(entry);
            view.objective.getScore(entry).setScore(score);
        }
        String[] parts = split(text);
        if (!parts[0].equals(team.getPrefix())) {
            team.setPrefix(parts[0]);
        }
        if (!parts[1].equals(team.getSuffix())) {
            team.setSuffix(parts[1]);
        }
    }

    static String[] split(String colored) {
        if (colored.length() <= 16) {
            return new String[]{colored, ""};
        }
        int cut = 16;
        if (colored.charAt(15) == ChatColor.COLOR_CHAR) {
            cut = 15;
        }
        String prefix = colored.substring(0, cut);
        String rest = colored.substring(cut);
        String carry = ChatColor.getLastColors(prefix);
        if (!carry.isEmpty() && rest.length() + carry.length() <= 16) {
            rest = carry + rest;
        }
        if (rest.length() > 16) {
            rest = rest.substring(0, 16);
        }
        return new String[]{prefix, rest};
    }

    private static final class View {
        private int rows = -1;
        private Scoreboard board;
        private Objective objective;
    }
}
