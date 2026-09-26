package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * Turns an older config.yml into the current one when the plugin loads or reloads.
 * {@code config-version} is the step the file has already finished. Do not change it by hand.
 * Each older number runs one migration, then any key still missing is copied from the jar.
 * The previous file is saved as {@code config.yml.vN.bak} before the converted file is written.
 */
public final class ConfigUpdater {
    public static final int CURRENT = 8;

    private ConfigUpdater() {
    }

    /** @return how many keys were added, or 0 when the file is already current */
    public static int update(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        int found = config.contains("config-version") ? config.getInt("config-version") : 0;
        if (found == CURRENT) {
            return 0;
        }
        if (found > CURRENT) {
            plugin.getLogger().warning("config.yml says version " + found
                    + ", but this jar understands " + CURRENT + ". The file was left unchanged.");
            return 0;
        }
        YamlConfiguration defaults = defaults(plugin);
        if (defaults == null) {
            return 0;
        }
        try {
            int version = found;
            while (version < CURRENT) {
                migrate(version, config, defaults);
                version++;
                config.set("config-version", version);
            }
            int added = fillMissing(defaults, config, "");
            backup(plugin, found);
            config.options().copyHeader(true);
            config.options().header(
                    "Converted automatically from config-version " + found + " to " + CURRENT + ".\n"
                            + "Do not change config-version.\n"
                            + "The previous file is config.yml.v" + found + ".bak.\n"
                            + "Comments from that file are not copied. The default config inside the jar explains each key."
            );
            plugin.saveConfig();
            plugin.getLogger().info("Converted config.yml from version " + found + " to " + CURRENT
                    + " (" + added + " new keys). Existing values were kept. Backup: config.yml.v" + found + ".bak");
            return added;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Config conversion from version " + found + " failed. The file on disk was not replaced.", ex);
            plugin.reloadConfig();
            return 0;
        }
    }

    /**
     * One step. {@code from} is the version currently stored in the file.
     * The step rewrites that version into {@code from + 1}.
     */
    private static void migrate(int from, FileConfiguration config, YamlConfiguration defaults) {
        switch (from) {
            case 0 -> to1(config, defaults);
            case 1 -> to2(config);
            case 2 -> to3(config, defaults);
            case 3 -> to4(config, defaults);
            case 4 -> to5(config, defaults);
            case 5 -> to6(config, defaults);
            case 6 -> to7(config);
            case 7 -> to8(config);
            default -> {
                // A gap still moves forward. Missing keys are filled after the last step.
            }
        }
    }

    /** Before config-version existed: old fireball key names and the old speed of 10. */
    private static void to1(FileConfiguration config, YamlConfiguration defaults) {
        move(config, "fireball.knockback-x", "fireball.knockback-horizontal");
        move(config, "fireball.knockback-y", "fireball.knockback-vertical");
        if (config.contains("fireball.accel") && Double.compare(config.getDouble("fireball.speed"), 10.0D) == 0) {
            config.set("fireball.speed", defaults.getDouble("fireball.speed", 1.6D));
        }
        config.set("fireball.accel", null);
    }

    /** Per-item amounts, enchant maps, reset radius, and the renamed fireball keys. */
    private static void to2(FileConfiguration config) {
        if (!config.contains("items.enchant-keep-chance") && config.contains("enchant-keep-chance")) {
            config.set("items.enchant-keep-chance", config.get("enchant-keep-chance"));
        }
    }

    /** Void-kill credit window. */
    private static void to3(FileConfiguration config, YamlConfiguration defaults) {
        ensure(config, defaults, "void-credit-seconds");
    }

    /** Multi-line tab header and footer. Replaces the old one-line text. */
    private static void to4(FileConfiguration config, YamlConfiguration defaults) {
        force(config, defaults, "messages.tab-header-lobby");
        force(config, defaults, "messages.tab-footer-lobby");
        force(config, defaults, "messages.tab-header-arena");
        force(config, defaults, "messages.tab-footer-arena");
    }

    /** Logs drop in a smaller stack than the other blocks. */
    private static void to5(FileConfiguration config, YamlConfiguration defaults) {
        ensure(config, defaults, "items.amounts.LOG");
    }

    /** Cage countdown default 10, item delay, gamemode list, and the waiting scoreboard line. */
    private static void to6(FileConfiguration config, YamlConfiguration defaults) {
        if (config.getInt("countdown") == 15) {
            config.set("countdown", 10);
        }
        ensure(config, defaults, "item-delay-seconds");
        ensure(config, defaults, "gamemodes");
        ensure(config, defaults, "messages.board-start-waiting");
        ensure(config, defaults, "messages.gamemode-title");
        ensure(config, defaults, "messages.gamemode-none");
        ensure(config, defaults, "messages.gamemode-lore-players");
        ensure(config, defaults, "messages.gamemode-lore-start");
        ensure(config, defaults, "messages.gamemode-lore-items");
        replaceIf(config, "messages.queued",
                "&eQueued. &7Players waiting: &f{count}&7/&f{max}",
                "&eQueued for {mode}&e. &7Players waiting: &f{count}&7/&f{max}");
        replaceIf(config, "messages.queue-join-broadcast",
                "&e{player} &7joined the queue &8(&f{count}&7/&f{max}&8)",
                "&e{player} &7joined the {mode} queue &8(&f{count}&7/&f{max}&8)");
        replaceIf(config, "messages.queue-leave-broadcast",
                "&e{player} &7left the queue &8(&f{count}&7/&f{max}&8)",
                "&e{player} &7left the {mode} queue &8(&f{count}&7/&f{max}&8)");
    }

    /** LuckPerms prefix on the lines that name a player, only when the text is still the old default. */
    private static void to7(FileConfiguration config) {
        replaceIf(config, "messages.join-message",
                "{prefix} &e{player} &7joined.",
                "{prefix} {luckperms_prefix}&e{player} &7joined.");
        replaceIf(config, "messages.quit-message",
                "{prefix} &e{player} &7left.",
                "{prefix} {luckperms_prefix}&e{player} &7left.");
        replaceIf(config, "messages.chat-alive",
                "&8[&6PoF&8] &f{player}&8: &7{message}",
                "&8[&6PoF&8] {luckperms_prefix}&f{player}&8: &7{message}");
        replaceIf(config, "messages.chat-spec",
                "&8[&7SPEC&8] &8{player}&8: &7{message}",
                "&8[&7SPEC&8] {luckperms_prefix}&8{player}&8: &7{message}");
    }

    /**
     * One sidebar string per mode, built from the old per-line keys so edited text is kept.
     * {@code board-start} and {@code board-start-waiting} stay; {@code {start}} picks between them.
     */
    private static void to8(FileConfiguration config) {
        String pad = line(config, "board-pad", "&8");
        String ip = line(config, "board-ip", "&e{ip}");
        String titleLobby = line(config, "board-title-lobby", "&6&lPILLARS");
        String titleStart = line(config, "board-title-starting", "&6&lSTARTING");
        String titleEnd = line(config, "board-title-finished", "&6&lFINISHED");
        String titleLive = line(config, "board-title-fortune", "&6&lFORTUNE");
        String arena = withToken(line(config, "board-arena", "&fArena: &e{value}"), "value", "id");
        String alive = withToken(line(config, "board-alive", "&fAlive: &e{value}"), "value", "alive");
        String kills = withToken(line(config, "board-kills", "&fKills: &e{value}"), "value", "session_kills");
        setLayout(config, "board-lobby", String.join("\n",
                titleLobby,
                line(config, "board-join", "&6/pof join"),
                withToken(line(config, "board-wins", "&fWins: &e{value}"), "value", "wins"),
                withToken(line(config, "board-kills", "&fKills: &e{value}"), "value", "kills"),
                withToken(line(config, "board-streak", "&fStreak: &e{value}"), "value", "streak"),
                pad,
                ip));
        setLayout(config, "board-queued", String.join("\n",
                titleLobby,
                "{start}",
                withToken(line(config, "board-queue", "&fQueue: &e{count}&7/&f{max}"), "count", "queue"),
                pad,
                ip));
        setLayout(config, "board-starting", String.join("\n",
                titleStart,
                arena,
                withToken(line(config, "board-starts", "&fStarts: &e{value}s"), "value", "starts"),
                alive,
                pad,
                ip));
        setLayout(config, "board-grace", String.join("\n",
                titleLive,
                arena,
                alive,
                kills,
                withToken(line(config, "board-item", "&fItem: &e{value}s"), "value", "item"),
                withToken(line(config, "board-time", "&fTime: &e{value}s"), "value", "time"),
                withToken(line(config, "board-grace", "&aGrace: &f{value}s"), "value", "grace"),
                pad,
                ip));
        setLayout(config, "board-ingame", String.join("\n",
                titleLive,
                arena,
                alive,
                kills,
                withToken(line(config, "board-item", "&fItem: &e{value}s"), "value", "item"),
                withToken(line(config, "board-time", "&fTime: &e{value}s"), "value", "time"),
                pad,
                ip));
        setLayout(config, "board-ending", String.join("\n",
                titleEnd,
                arena,
                alive,
                kills,
                pad,
                ip));
        for (String old : new String[]{
                "board-title-lobby", "board-title-starting", "board-title-finished", "board-title-fortune",
                "board-join", "board-wins", "board-kills", "board-streak", "board-queue",
                "board-arena", "board-starts", "board-alive", "board-item", "board-time", "board-grace",
                "board-pad", "board-ip"
        }) {
            config.set("messages." + old, null);
        }
    }

    private static void setLayout(FileConfiguration config, String key, String layout) {
        if (!config.contains("messages." + key)) {
            config.set("messages." + key, layout);
        }
    }

    private static String line(FileConfiguration config, String key, String fallback) {
        String value = config.getString("messages." + key);
        return value == null ? fallback : value;
    }

    private static String withToken(String text, String from, String to) {
        return text.replace("{" + from + "}", "{" + to + "}");
    }

    private static void replaceIf(FileConfiguration config, String path, String from, String to) {
        if (from.equals(config.getString(path))) {
            config.set(path, to);
        }
    }

    private static void move(FileConfiguration config, String from, String to) {
        if (!config.contains(from) || config.contains(to)) {
            return;
        }
        config.set(to, config.get(from));
        config.set(from, null);
    }

    private static void ensure(FileConfiguration config, YamlConfiguration defaults, String path) {
        if (!config.contains(path) && defaults.contains(path)) {
            config.set(path, defaults.get(path));
        }
    }

    private static void force(FileConfiguration config, YamlConfiguration defaults, String path) {
        if (defaults.contains(path)) {
            config.set(path, defaults.get(path));
        }
    }

    private static int fillMissing(ConfigurationSection defaults, FileConfiguration config, String prefix) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            if ("config-version".equals(key) && prefix.isEmpty()) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            ConfigurationSection child = defaults.getConfigurationSection(key);
            if (child != null) {
                if (!config.isConfigurationSection(path)) {
                    config.set(path, defaults.get(key));
                    added++;
                } else {
                    added += fillMissing(child, config, path);
                }
            } else if (!config.contains(path)) {
                config.set(path, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    private static YamlConfiguration defaults(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                plugin.getLogger().severe("The jar is missing config.yml, so the old config was not converted.");
                return null;
            }
            return YamlConfiguration.loadConfiguration(in);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read the default config.yml", ex);
            return null;
        }
    }

    private static void backup(JavaPlugin plugin, int from) {
        File current = new File(plugin.getDataFolder(), "config.yml");
        if (!current.isFile()) {
            return;
        }
        File copy = new File(plugin.getDataFolder(), "config.yml.v" + from + ".bak");
        try {
            Files.copy(current.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not back up config.yml before converting it.", ex);
        }
    }
}
