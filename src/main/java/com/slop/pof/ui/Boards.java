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
import com.slop.pof.util.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Boards {
    private static final String[] ENTRIES = new String[16];
    private static final String[] TAB_TEAMS = {"t0", "t1", "t2", "t3", "t4"};
    private static int tabState;

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

    public Boards(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh(Player player) {
        sidebar(player);
        tab(player);
    }

    public void clear(Player player) {
        views.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        String name = player.getName();
        player.setPlayerListName(name.length() <= 16 ? name : name.substring(0, 16));
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
        String rendered = settings.text(player, "board-" + mode,
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

    private void tab(Player player) {
        Settings settings = plugin.settings();
        Game game = plugin.game();
        Game.Session session = game.session(player);
        Arena arena = session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        String colored = listColor(player, session, arena, game) + player.getName();
        String ip = settings.serverIp();
        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        if (arena == null) {
            sendHeader(player,
                    lines(settings.text("tab-header-lobby", "ip", ip)),
                    lines(settings.text("tab-footer-lobby",
                            "queue", String.valueOf(game.queueSize()),
                            "max", String.valueOf(settings.maxPlayers()),
                            "online", online,
                            "ip", ip)));
        } else {
            sendHeader(player,
                    lines(settings.text("tab-header-arena",
                            "id", String.valueOf(arena.id),
                            "state", prettyState(arena),
                            "ip", ip)),
                    lines(settings.text("tab-footer-arena",
                            "state", prettyState(arena),
                            "alive", String.valueOf(arena.aliveCount()),
                            "time", clock(arena),
                            "online", online,
                            "ip", ip)));
        }
        player.setPlayerListName(trim(colored, 16));
        applyVisibility(player, arena);
        sortTab(player);
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

    private static String listColor(Player player, Game.Session session, Arena arena, Game game) {
        if (arena == null) {
            return game.queued(player.getUniqueId()) ? ChatColor.YELLOW.toString() : ChatColor.GRAY.toString();
        }
        if (arena.state == Arena.State.ENDING && arena.winner != null && arena.winner.equals(player.getUniqueId())) {
            return ChatColor.GOLD.toString();
        }
        return session.alive ? ChatColor.GREEN.toString() : ChatColor.DARK_GRAY.toString();
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

    /** A config {@code \n} becomes a real line break in the tab header or footer. */
    private static String lines(String text) {
        return text.replace("\\n", "\n");
    }

    private void sortTab(Player viewer) {
        View view = views.get(viewer.getUniqueId());
        if (view == null || view.board == null) {
            return;
        }
        for (String id : TAB_TEAMS) {
            if (view.board.getTeam(id) == null) {
                view.board.registerNewTeam(id);
            }
        }
        Arena viewerArena = arenaOf(viewer);
        Set<String> shown = new HashSet<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!sameTab(viewerArena, arenaOf(other))) {
                continue;
            }
            shown.add(other.getName());
            String teamId = tabGroup(other);
            for (String id : TAB_TEAMS) {
                Team team = view.board.getTeam(id);
                if (team != null && team.hasEntry(other.getName()) && !id.equals(teamId)) {
                    team.removeEntry(other.getName());
                }
            }
            Team target = view.board.getTeam(teamId);
            if (target != null && !target.hasEntry(other.getName())) {
                target.addEntry(other.getName());
            }
        }
        for (String id : TAB_TEAMS) {
            Team team = view.board.getTeam(id);
            if (team == null) {
                continue;
            }
            for (String entry : new ArrayList<>(team.getEntries())) {
                if (!shown.contains(entry)) {
                    team.removeEntry(entry);
                }
            }
        }
    }

    private String tabGroup(Player player) {
        Game game = plugin.game();
        Game.Session session = game.peek(player.getUniqueId());
        Arena arena = session == null || session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        if (arena != null && arena.state == Arena.State.ENDING && arena.winner != null && arena.winner.equals(player.getUniqueId())) {
            return "t0";
        }
        if (arena != null && session.alive) {
            return "t1";
        }
        if (game.queued(player.getUniqueId())) {
            return "t2";
        }
        if (arena != null) {
            return "t4";
        }
        return "t3";
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

    private static String trim(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static void sendHeader(Player player, String header, String footer) {
        if (tabState == 2) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            tabState = 2;
            return;
        }
        try {
            Class<?> library = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object manager = library.getMethod("getProtocolManager").invoke(null);
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Object play = packetTypeClass.getField("Play").get(null);
            Object server = play.getClass().getField("Server").get(play);
            Object type = server.getClass().getField("PLAYER_LIST_HEADER_FOOTER").get(server);
            Object packet = manager.getClass().getMethod("createPacket", packetTypeClass).invoke(manager, type);
            Class<?> wrapped = Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");
            Object headerComponent = component(wrapped, header);
            Object footerComponent = component(wrapped, footer);
            Object modifier = packet.getClass().getMethod("getChatComponents").invoke(packet);
            Method write = modifier.getClass().getMethod("write", int.class, Object.class);
            write.invoke(modifier, 0, headerComponent);
            write.invoke(modifier, 1, footerComponent);
            Class<?> container = Class.forName("com.comphenix.protocol.events.PacketContainer");
            manager.getClass().getMethod("sendServerPacket", Player.class, container).invoke(manager, player, packet);
            tabState = 1;
        } catch (Throwable ignored) {
            tabState = 2;
        }
    }

    private static Object component(Class<?> wrapped, String text) throws Exception {
        String json = "{\"text\":\"" + jsonEscape(text) + "\"}";
        try {
            return wrapped.getMethod("fromJson", String.class).invoke(null, json);
        } catch (NoSuchMethodException ex) {
            return wrapped.getMethod("fromText", String.class).invoke(null, text);
        }
    }

    private static String jsonEscape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static final class View {
        private int rows = -1;
        private Scoreboard board;
        private Objective objective;
    }
}
