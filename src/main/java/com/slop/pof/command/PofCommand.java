package com.slop.pof.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import com.slop.pof.Perms;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.config.Settings;
import com.slop.pof.storage.Stats;
import com.slop.pof.storage.TopEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PofCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ALL = List.of(
            "join", "leave", "stats", "top", "start", "stop", "setlobby", "regen", "arenas", "debug", "reload"
    );
    private static final List<String> PLAY = List.of("join", "leave", "stats", "top");

    private final PoFPlugin plugin;

    public PofCommand(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings settings = plugin.settings();
        if (args.length == 0) {
            help(sender, settings);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> join(sender);
            case "leave" -> leave(sender);
            case "stats" -> stats(sender, args);
            case "top" -> top(sender);
            case "start" -> start(sender);
            case "stop" -> stop(sender, args);
            case "setlobby" -> setLobby(sender);
            case "regen" -> regen(sender);
            case "arenas" -> arenas(sender);
            case "debug" -> debug(sender, args);
            case "reload" -> reload(sender);
            default -> tell(sender, "unknown");
        }
        return true;
    }

    private void help(CommandSender sender, Settings settings) {
        if (sender instanceof Player player) {
            tell(player, "help");
            if (player.hasPermission(Perms.ADMIN)) {
                tell(player, "help-admin");
            }
        } else {
            sender.sendMessage(settings.text("help-console"));
        }
    }

    private void join(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            tell(sender, "players-only");
            return;
        }
        plugin.game().join(player);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            tell(sender, "players-only");
            return;
        }
        if (!player.hasPermission(Perms.PLAY)) {
            tell(player, "no-permission");
            return;
        }
        plugin.game().leave(player);
    }

    private void stats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                tell(sender, "players-only");
                return;
            }
            if (!player.hasPermission(Perms.PLAY)) {
                tell(player, "no-permission");
                return;
            }
            plugin.database().ensure(player.getUniqueId(), player.getName());
            showStats(sender, plugin.database().get(player.getUniqueId()), player.getName(), player);
            return;
        }
        if (sender instanceof Player player && !player.hasPermission(Perms.STATS_OTHERS)) {
            tell(player, "no-permission");
            return;
        }
        Player online = Bukkit.getPlayer(args[1]);
        if (online != null) {
            plugin.database().ensure(online.getUniqueId(), online.getName());
            showStats(sender, plugin.database().get(online.getUniqueId()), online.getName(), online);
            return;
        }
        Stats found = plugin.database().findByName(args[1]);
        if (found == null) {
            tell(sender, "player-not-found");
            return;
        }
        showStats(sender, found, found.name, Bukkit.getPlayerExact(found.name));
    }

    private void showStats(CommandSender sender, Stats stats, String name, Player about) {
        if (stats == null) {
            tell(sender, "player-not-found");
            return;
        }
        tell(sender, about, "stats-header", "player", name);
        tell(sender, about, "stats-line1",
                "wins", String.valueOf(stats.wins),
                "kills", String.valueOf(stats.kills),
                "deaths", String.valueOf(stats.deaths));
        tell(sender, about, "stats-line2",
                "games", String.valueOf(stats.games),
                "streak", String.valueOf(stats.streak),
                "best", String.valueOf(stats.bestStreak));
        tell(sender, about, "stats-line3",
                "items", String.valueOf(stats.items),
                "mins", String.valueOf(stats.playtimeMin));
    }

    private void top(CommandSender sender) {
        if (sender instanceof Player player && !player.hasPermission(Perms.PLAY)) {
            tell(player, "no-permission");
            return;
        }
        plugin.game().refreshTop();
        if (!plugin.game().topReady()) {
            tell(sender, "top-error");
            return;
        }
        List<TopEntry> rows = plugin.game().top();
        tell(sender, "top-header");
        if (rows.isEmpty()) {
            tell(sender, "top-empty");
        } else {
            int rank = 1;
            for (TopEntry row : rows) {
                tell(sender, Bukkit.getPlayerExact(row.name), "top-line",
                        "rank", String.valueOf(rank++),
                        "player", row.name,
                        "wins", String.valueOf(row.wins));
            }
        }
        if (plugin.isDebug()) {
            tell(sender, "debug-line", "message", "rows=" + rows.size());
        }
    }

    private void start(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.game().vipStart(player, false);
        } else {
            plugin.game().skipTimer(null);
        }
    }

    private void stop(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            tell(sender, "no-permission");
            return;
        }
        Settings settings = plugin.settings();
        if (args.length >= 2) {
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                tell(sender, "arena-missing", "id", args[1]);
                return;
            }
            if (!plugin.game().stopArena(id)) {
                tell(sender, "arena-missing", "id", args[1]);
                return;
            }
            tell(sender, "stopped-arena", "id", String.valueOf(id));
            return;
        }
        plugin.game().stopAll();
        if (sender instanceof Player) {
            tell(sender, "stopped-all");
        } else {
            sender.sendMessage(settings.text("stopped-all-console"));
        }
    }

    private void setLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            tell(sender, "players-only");
            return;
        }
        if (!player.hasPermission(Perms.ADMIN)) {
            tell(player, "no-permission");
            return;
        }
        plugin.lobby().save(player.getLocation());
        tell(player, "lobby-set");
    }

    private void regen(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            tell(sender, "no-permission");
            return;
        }
        plugin.game().regen();
        if (sender instanceof Player) {
            tell(sender, "regen-done");
        } else {
            sender.sendMessage(plugin.settings().text("regen-done-console"));
        }
    }

    private void arenas(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            tell(sender, "no-permission");
            return;
        }
        Settings settings = plugin.settings();
        boolean player = sender instanceof Player;
        for (Arena arena : plugin.arenas().all()) {
            if (player) {
                tell(sender, "arena-line",
                        "id", String.valueOf(arena.id),
                        "state", arena.state.name(),
                        "alive", String.valueOf(arena.aliveCount()));
            } else {
                sender.sendMessage(settings.text("arena-line-console",
                        "id", String.valueOf(arena.id),
                        "state", arena.state.name(),
                        "alive", String.valueOf(arena.aliveCount())));
            }
        }
    }

    /**
     * {@code /pof debug} toggles verbose logging; {@code /pof debug <topic>} prints a snapshot.
     * Topics: on, off, arenas, queue, world, storage, config.
     */
    private void debug(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            tell(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.setDebug(!plugin.isDebug());
            tell(sender, plugin.isDebug() ? "debug-on" : "debug-off");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                plugin.setDebug(true);
                tell(sender, "debug-on");
            }
            case "off" -> {
                plugin.setDebug(false);
                tell(sender, "debug-off");
            }
            case "arenas", "list" -> debugArenas(sender);
            case "queue", "state" -> debugLine(sender, plugin.game().debugStatus());
            case "world", "worlds" -> debugLine(sender, worldsLine());
            case "storage", "db" -> debugLine(sender, storageLine());
            case "config" -> debugLine(sender, configLine());
            default -> {
                tell(sender, "debug-unknown");
                debugLine(sender, "on | off | arenas | queue | world | storage | config");
            }
        }
    }

    private void debugArenas(CommandSender sender) {
        debugLine(sender, "arenas=" + plugin.arenas().size()
                + " waiting=" + plugin.arenas().waitingCount());
        for (Arena arena : plugin.arenas().all()) {
            debugLine(sender, "#" + arena.id + " " + arena.state
                    + " world=" + (arena.world == null ? "?" : arena.world.getName())
                    + " center=" + arena.centerX + "," + arena.centerZ
                    + " players=" + arena.players.size()
                    + " alive=" + arena.aliveCount()
                    + " mode=" + (arena.gamemodeId == null ? "-" : arena.gamemodeId)
                    + " pillars=" + arena.pillars.size());
        }
    }

    private void debugLine(CommandSender sender, String message) {
        tell(sender, "debug-line", "message", message);
    }

    private String worldsLine() {
        Settings settings = plugin.settings();
        return "lobby=" + settings.lobbyWorld() + " game=" + settings.gameWorld()
                + " per-arena=" + plugin.worlds().perArena();
    }

    private String storageLine() {
        Settings settings = plugin.settings();
        return "storage=" + settings.storageType() + " fingerprint=" + settings.storageFingerprint();
    }

    private String configLine() {
        Settings settings = plugin.settings();
        return "arenas=" + settings.arenaCount() + " pillars=" + settings.pillarCount()
                + " reset-mode=" + settings.resetMode()
                + " reset-blocks-per-tick=" + settings.resetBlocksPerTick()
                + " min-players=" + settings.minPlayers() + " max-players=" + settings.maxPlayers();
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            tell(sender, "no-permission");
            return;
        }
        try {
            boolean restart = plugin.reloadPlugin();
            tell(sender, "reloaded");
            if (restart) {
                tell(sender, "restart-required");
            }
        } catch (RuntimeException ex) {
            tell(sender, "storage-failed");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = sender.hasPermission(Perms.ADMIN) ? ALL : PLAY;
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stop") && sender.hasPermission(Perms.ADMIN)) {
            List<String> ids = new ArrayList<>();
            for (Arena arena : plugin.arenas().all()) {
                ids.add(String.valueOf(arena.id));
            }
            return filter(ids, args[1]);
        }
        return List.of();
    }

    /** Plugin chat for the sender. Placeholders use that player when the sender is a player. */
    private void tell(CommandSender sender, String key, String... pairs) {
        tell(sender, sender instanceof Player player ? player : null, key, pairs);
    }

    /** Plugin chat about {@code about}. Offline or console subjects leave LuckPerms tokens empty. */
    private void tell(CommandSender sender, Player about, String key, String... pairs) {
        sender.sendMessage(plugin.settings().chat(about, key, pairs));
    }

    private static List<String> filter(List<String> options, String prefix) {
        String needle = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(option);
            }
        }
        return out;
    }
}
