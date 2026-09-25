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
import com.slop.pof.config.Settings;
import com.slop.pof.game.Game;
import com.slop.pof.storage.Stats;
import com.slop.pof.util.Text;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Boards {
    private static final String[] ENTRIES = new String[16];
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        views.clear();
    }

    private void sidebar(Player player) {
        Settings settings = plugin.settings();
        Game game = plugin.game();
        Game.Session session = game.session(player);
        Stats stats = plugin.database().get(player.getUniqueId());
        int wins = stats == null ? 0 : stats.wins;
        int kills = stats == null ? 0 : stats.kills;
        int streak = stats == null ? 0 : stats.streak;
        int queue = game.queueSize();
        Arena arena = session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        String mode = mode(session, arena, game);
        View view = views.computeIfAbsent(player.getUniqueId(), id -> new View());
        if (!mode.equals(view.mode) || view.board == null) {
            view.mode = mode;
            view.board = Bukkit.getScoreboardManager().getNewScoreboard();
            view.objective = view.board.registerNewObjective("pof", "dummy");
            view.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            view.objective.setDisplayName(trim(title(settings, mode), 32));
            player.setScoreboard(view.board);
            line(view, 2, settings.text("board-pad"));
            line(view, 1, settings.text("board-ip", "ip", settings.serverIp()));
            if ("lobby".equals(mode)) {
                line(view, 11, settings.text("board-join"));
                line(view, 10, settings.text("board-wins", "value", String.valueOf(wins)));
                line(view, 9, settings.text("board-kills", "value", String.valueOf(kills)));
                line(view, 8, settings.text("board-streak", "value", String.valueOf(streak)));
            } else if ("queued".equals(mode)) {
                line(view, 11, settings.text("board-start", "value", String.valueOf(game.queueTimer())));
                line(view, 10, settings.text("board-queue", "count", String.valueOf(queue), "max", String.valueOf(settings.maxPlayers())));
            } else if ("starting".equals(mode)) {
                line(view, 11, settings.text("board-arena", "value", String.valueOf(arena.id)));
                line(view, 10, settings.text("board-starts", "value", String.valueOf(arena.count)));
                line(view, 9, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
            } else if ("ending".equals(mode)) {
                line(view, 11, settings.text("board-arena", "value", String.valueOf(arena.id)));
                line(view, 10, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
                line(view, 9, settings.text("board-kills", "value", String.valueOf(session.kills)));
            } else {
                line(view, 12, settings.text("board-arena", "value", String.valueOf(arena.id)));
                line(view, 11, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
                line(view, 10, settings.text("board-kills", "value", String.valueOf(session.kills)));
                line(view, 9, settings.text("board-item", "value", String.valueOf(arena.item)));
                line(view, 8, settings.text("board-time", "value", String.valueOf(arena.time)));
                if ("grace".equals(mode)) {
                    line(view, 7, settings.text("board-grace", "value", String.valueOf(arena.grace)));
                }
            }
            return;
        }
        if ("lobby".equals(mode)) {
            line(view, 10, settings.text("board-wins", "value", String.valueOf(wins)));
            line(view, 9, settings.text("board-kills", "value", String.valueOf(kills)));
            line(view, 8, settings.text("board-streak", "value", String.valueOf(streak)));
        } else if ("queued".equals(mode)) {
            line(view, 11, settings.text("board-start", "value", String.valueOf(game.queueTimer())));
            line(view, 10, settings.text("board-queue", "count", String.valueOf(queue), "max", String.valueOf(settings.maxPlayers())));
        } else if ("starting".equals(mode)) {
            line(view, 10, settings.text("board-starts", "value", String.valueOf(arena.count)));
            line(view, 9, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
        } else if ("ending".equals(mode)) {
            line(view, 10, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
            line(view, 9, settings.text("board-kills", "value", String.valueOf(session.kills)));
        } else {
            line(view, 11, settings.text("board-alive", "value", String.valueOf(arena.aliveCount())));
            line(view, 10, settings.text("board-kills", "value", String.valueOf(session.kills)));
            line(view, 9, settings.text("board-item", "value", String.valueOf(arena.item)));
            line(view, 8, settings.text("board-time", "value", String.valueOf(arena.time)));
            if ("grace".equals(mode)) {
                line(view, 7, settings.text("board-grace", "value", String.valueOf(arena.grace)));
            }
        }
    }

    private void tab(Player player) {
        Settings settings = plugin.settings();
        Game game = plugin.game();
        Game.Session session = game.session(player);
        Arena arena = session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        String colored;
        if (arena == null) {
            colored = game.queued(player.getUniqueId())
                    ? ChatColor.YELLOW + player.getName()
                    : ChatColor.GRAY + player.getName();
            sendHeader(player,
                    settings.text("tab-header-lobby"),
                    settings.text("tab-footer-lobby",
                            "queue", String.valueOf(game.queueSize()),
                            "online", String.valueOf(Bukkit.getOnlinePlayers().size()),
                            "ip", settings.serverIp()));
        } else {
            if (arena.state == Arena.State.ENDING && arena.winner != null && arena.winner.equals(player.getUniqueId())) {
                colored = ChatColor.GOLD + player.getName();
            } else if (session.alive) {
                colored = ChatColor.GREEN + player.getName();
            } else {
                colored = ChatColor.DARK_GRAY + player.getName();
            }
            sendHeader(player,
                    settings.text("tab-header-arena", "id", String.valueOf(arena.id)),
                    settings.text("tab-footer-arena",
                            "state", arena.state.name(),
                            "alive", String.valueOf(arena.aliveCount()),
                            "ip", settings.serverIp()));
        }
        player.setPlayerListName(trim(colored, 16));
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

    private static String title(Settings settings, String mode) {
        return switch (mode) {
            case "starting" -> settings.text("board-title-starting");
            case "ending" -> settings.text("board-title-finished");
            case "ingame", "grace" -> settings.text("board-title-fortune");
            default -> settings.text("board-title-lobby");
        };
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
        try {
            return wrapped.getMethod("fromText", String.class).invoke(null, text);
        } catch (NoSuchMethodException ex) {
            return wrapped.getMethod("fromLegacyText", String.class).invoke(null, text);
        }
    }

    private static final class View {
        private String mode = "";
        private Scoreboard board;
        private Objective objective;
    }
}
