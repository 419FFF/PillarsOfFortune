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
            case "debug" -> debug(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(settings.chat("unknown"));
        }
        return true;
    }

    private void help(CommandSender sender, Settings settings) {
        if (sender instanceof Player player) {
            player.sendMessage(settings.chat("help"));
            if (player.hasPermission(Perms.ADMIN)) {
                player.sendMessage(settings.chat("help-admin"));
            }
        } else {
            sender.sendMessage(settings.text("help-console"));
        }
    }

    private void join(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.settings().chat("players-only"));
            return;
        }
        plugin.game().join(player);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.settings().chat("players-only"));
            return;
        }
        if (!player.hasPermission(Perms.PLAY)) {
            player.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        plugin.game().leave(player);
    }

    private void stats(CommandSender sender, String[] args) {
        Settings settings = plugin.settings();
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(settings.chat("players-only"));
                return;
            }
            if (!player.hasPermission(Perms.PLAY)) {
                player.sendMessage(settings.chat("no-permission"));
                return;
            }
            plugin.database().ensure(player.getUniqueId(), player.getName());
            showStats(sender, plugin.database().get(player.getUniqueId()), player.getName());
            return;
        }
        if (sender instanceof Player player && !player.hasPermission(Perms.STATS_OTHERS)) {
            player.sendMessage(settings.chat("no-permission"));
            return;
        }
        Player online = Bukkit.getPlayer(args[1]);
        if (online != null) {
            plugin.database().ensure(online.getUniqueId(), online.getName());
            showStats(sender, plugin.database().get(online.getUniqueId()), online.getName());
            return;
        }
        Stats found = plugin.database().findByName(args[1]);
        if (found == null) {
            sender.sendMessage(settings.chat("player-not-found"));
            return;
        }
        showStats(sender, found, found.name);
    }

    private void showStats(CommandSender sender, Stats stats, String name) {
        Settings settings = plugin.settings();
        if (stats == null) {
            sender.sendMessage(settings.chat("player-not-found"));
            return;
        }
        sender.sendMessage(settings.chat("stats-header", "player", name));
        sender.sendMessage(settings.chat("stats-line1",
                "wins", String.valueOf(stats.wins),
                "kills", String.valueOf(stats.kills),
                "deaths", String.valueOf(stats.deaths)));
        sender.sendMessage(settings.chat("stats-line2",
                "games", String.valueOf(stats.games),
                "streak", String.valueOf(stats.streak),
                "best", String.valueOf(stats.bestStreak)));
        sender.sendMessage(settings.chat("stats-line3",
                "items", String.valueOf(stats.items),
                "mins", String.valueOf(stats.playtimeMin)));
    }

    private void top(CommandSender sender) {
        Settings settings = plugin.settings();
        if (sender instanceof Player player && !player.hasPermission(Perms.PLAY)) {
            player.sendMessage(settings.chat("no-permission"));
            return;
        }
        plugin.game().refreshTop();
        if (!plugin.game().topReady()) {
            sender.sendMessage(settings.chat("top-error"));
            return;
        }
        List<TopEntry> rows = plugin.game().top();
        sender.sendMessage(settings.chat("top-header"));
        if (rows.isEmpty()) {
            sender.sendMessage(settings.chat("top-empty"));
        } else {
            int rank = 1;
            for (TopEntry row : rows) {
                sender.sendMessage(settings.chat("top-line",
                        "rank", String.valueOf(rank++),
                        "player", row.name,
                        "wins", String.valueOf(row.wins)));
            }
        }
        if (plugin.isDebug()) {
            sender.sendMessage(settings.chat("debug-line", "message", "rows=" + rows.size()));
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
            sender.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        Settings settings = plugin.settings();
        if (args.length >= 2) {
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(settings.chat("arena-missing", "id", args[1]));
                return;
            }
            if (!plugin.game().stopArena(id)) {
                sender.sendMessage(settings.chat("arena-missing", "id", args[1]));
                return;
            }
            sender.sendMessage(settings.chat("stopped-arena", "id", String.valueOf(id)));
            return;
        }
        plugin.game().stopAll();
        if (sender instanceof Player) {
            sender.sendMessage(settings.chat("stopped-all"));
        } else {
            sender.sendMessage(settings.text("stopped-all-console"));
        }
    }

    private void setLobby(CommandSender sender) {
        Settings settings = plugin.settings();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(settings.chat("players-only"));
            return;
        }
        if (!player.hasPermission(Perms.ADMIN)) {
            player.sendMessage(settings.chat("no-permission"));
            return;
        }
        plugin.lobby().save(player.getLocation());
        player.sendMessage(settings.chat("lobby-set"));
    }

    private void regen(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            sender.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        plugin.game().regen();
        if (sender instanceof Player) {
            sender.sendMessage(plugin.settings().chat("regen-done"));
        } else {
            sender.sendMessage(plugin.settings().text("regen-done-console"));
        }
    }

    private void arenas(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            sender.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        Settings settings = plugin.settings();
        boolean player = sender instanceof Player;
        for (Arena arena : plugin.arenas().all()) {
            if (player) {
                sender.sendMessage(settings.chat("arena-line",
                        "id", String.valueOf(arena.id),
                        "state", arena.state.name(),
                        "alive", String.valueOf(arena.aliveCount())));
            } else {
                sender.sendMessage(settings.text("arena-line-console",
                        "id", String.valueOf(arena.id),
                        "state", arena.state.name(),
                        "alive", String.valueOf(arena.aliveCount())));
            }
        }
    }

    private void debug(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            sender.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        plugin.setDebug(!plugin.isDebug());
        sender.sendMessage(plugin.settings().chat(plugin.isDebug() ? "debug-on" : "debug-off"));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(Perms.ADMIN)) {
            sender.sendMessage(plugin.settings().chat("no-permission"));
            return;
        }
        try {
            boolean restart = plugin.reloadPlugin();
            sender.sendMessage(plugin.settings().chat("reloaded"));
            if (restart) {
                sender.sendMessage(plugin.settings().chat("restart-required"));
            }
        } catch (RuntimeException ex) {
            sender.sendMessage(plugin.settings().chat("storage-failed"));
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
