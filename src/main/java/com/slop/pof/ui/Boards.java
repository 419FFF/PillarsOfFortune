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
import com.slop.pof.config.Levels;
import com.slop.pof.config.Settings;
import com.slop.pof.config.Visibility;
import com.slop.pof.game.Game;
import com.slop.pof.storage.Stats;
import com.slop.pof.util.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Boards {
    private static final String OBJECTIVE = "pof";
    private static final String TEAM_PREFIX = "l";
    private static final String[] ENTRIES = new String[16];
    /** Sidebar rows 1..15. ENTRIES[0] is unused. */
    private static final int MAX_ROWS = ENTRIES.length - 1;
    /** Current date under the title, in the requested MM/DD/YY shape. */
    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy");

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
    private final Map<UUID, Visibility> visibilityModes = new HashMap<>();

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

    /** The viewer's lobby visibility. New players start on the config default. */
    public Visibility visibility(Player player) {
        return player == null ? plugin.settings().defaultVisibility() : visibility(player.getUniqueId());
    }

    public Visibility visibility(UUID uuid) {
        return visibilityModes.getOrDefault(uuid, plugin.settings().defaultVisibility());
    }

    /** Show all, then hide non-queue, then hide all. Refreshes the hotbar and everyone's view. */
    public void cycleVisibility(Player player) {
        Visibility next = visibility(player).next();
        visibilityModes.put(player.getUniqueId(), next);
        if (plugin.game().queued(player.getUniqueId())) {
            plugin.items().giveQueued(player);
        } else {
            plugin.items().giveLobby(player);
        }
        refreshVisibility();
        player.sendMessage(plugin.settings().chat(player, "visibility-changed",
                "mode", plugin.settings().text(player, "visibility-name-" + next.key())));
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
        visibilityModes.clear();
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
        Levels levels = settings.levels();
        int level = stats == null ? 1 : Math.max(1, stats.level);
        int xp = stats == null ? 0 : Math.max(0, stats.xp);
        int xpNext = levels.nextCost(level);
        if (levels.enabled()) {
            player.setLevel(level);
            player.setExp((float) levels.progress(level, xp));
        }
        String rendered = settings.board(player, mode,
                "ip", settings.serverIp(),
                "title", title(mode, arena),
                "date", DATE.format(java.time.LocalDate.now()),
                "level", String.valueOf(level),
                "level_name", levels.name(level),
                "xp", String.valueOf(xp),
                "xp_next", String.valueOf(xpNext),
                "progress", levels.bar(level, xp),
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
        if (view.board == null) {
            view.board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective existing = view.board.getObjective(OBJECTIVE);
            view.objective = existing != null ? existing : view.board.registerNewObjective(OBJECTIVE, "dummy");
            view.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        // Another plugin or /scoreboard can swap the board out; make sure ours is the one shown.
        if (player.getScoreboard() != view.board) {
            player.setScoreboard(view.board);
        }
        // A changed layout wipes every row first, so nothing from the previous screen can linger
        // (for example lobby lines during the first seconds of a match).
        if (!rendered.equals(view.rendered)) {
            for (int score = 1; score <= MAX_ROWS; score++) {
                clearRow(view, score);
            }
            view.rendered = rendered;
        }
        if (!rows[0].equals(view.objective.getDisplayName())) {
            view.objective.setDisplayName(rows[0]);
        }
        // Row N is score N, so a shorter layout must clear the rows it no longer uses.
        for (int score = 1; score <= MAX_ROWS; score++) {
            if (score > body) {
                clearRow(view, score);
            } else {
                line(view, score, rows[body - score + 1]);
            }
        }
    }

    /** Removes a sidebar row completely, team and score, so nothing is left behind. */
    private static void clearRow(View view, int score) {
        Team team = view.board.getTeam(TEAM_PREFIX + score);
        if (team != null) {
            team.unregister();
        }
        view.board.resetScores(ENTRIES[score]);
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
        Visibility mode = visibility(viewer);
        String viewerQueue = plugin.game().queuedMode(viewer.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(viewer)) {
                continue;
            }
            Arena otherArena = arenaOf(other);
            boolean show;
            if (viewerArena != null || otherArena != null) {
                // A match always shows that match and nothing else.
                show = sameTab(viewerArena, otherArena);
            } else {
                show = switch (mode) {
                    case ALL -> true;
                    case NONE -> false;
                    case QUEUE -> {
                        String otherQueue = plugin.game().queuedMode(other.getUniqueId());
                        yield viewerQueue == null ? otherQueue == null : viewerQueue.equals(otherQueue);
                    }
                };
            }
            if (show) {
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

    /**
     * The bold title line. During a match it is the mode's own name, so a Rush board says RUSH and a
     * Classic board says CLASSIC. The lobby, cage, and result phases keep their fixed titles.
     */
    private String title(String mode, Arena arena) {
        Gamemode gamemode = arena == null ? null : plugin.settings().gamemode(arena.gamemodeId);
        String name = gamemode == null
                ? ""
                : ChatColor.stripColor(gamemode.name).toUpperCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "starting" -> "&6&lSTARTING";
            case "ending" -> "&6&lFINISHED";
            case "grace", "ingame" -> name.isEmpty() ? "&6&lFORTUNE" : Text.bold("&6" + name);
            default -> "&6&lPILLARS";
        };
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
        Team team = view.board.getTeam(TEAM_PREFIX + score);
        if (team == null) {
            team = view.board.registerNewTeam(TEAM_PREFIX + score);
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
        private Scoreboard board;
        private Objective objective;
        private String rendered = "";
    }
}
