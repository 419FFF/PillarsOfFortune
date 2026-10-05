package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
    public static final int CURRENT = 20;

    private ConfigUpdater() {
    }

    /** @return how many keys were added, or 0 when the file is already current */
    public static int update(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        int found = own(config, "config-version") ? config.getInt("config-version") : 0;
        if (found > CURRENT) {
            plugin.getLogger().warning("config.yml says version " + found
                    + ", but this jar understands " + CURRENT + ". The file was left unchanged.");
            return 0;
        }
        byte[] raw = resource(plugin);
        if (raw == null) {
            return 0;
        }
        String template = new String(raw, StandardCharsets.UTF_8);
        YamlConfiguration defaults = defaults(raw, plugin);
        if (defaults == null) {
            return 0;
        }
        File file = new File(plugin.getDataFolder(), "config.yml");
        String disk = "";
        if (file.isFile()) {
            try {
                disk = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Could not read config.yml before updating it.", ex);
            }
        }
        boolean migrate = found < CURRENT;
        boolean healBoards = BoardLayouts.hasLegacy(key -> own(config, "messages." + key) ? config.getString("messages." + key) : null);
        boolean restoreComments = ConfigLayout.needsCommentRestore(disk, template);
        if (!migrate && !healBoards && !restoreComments) {
            return 0;
        }
        try {
            if (migrate) {
                int version = found;
                while (version < CURRENT) {
                    migrate(version, config, defaults);
                    version++;
                    config.set("config-version", version);
                }
            } else if (healBoards) {
                to10(config, defaults);
            }
            int added = fillMissing(defaults, config, "");
            String backupName = migrate ? "config.yml.v" + found + ".bak" : "config.yml.comments.bak";
            backup(plugin, backupName);
            String merged = ConfigLayout.merge(template, config);
            Files.writeString(file.toPath(), merged, StandardCharsets.UTF_8);
            plugin.reloadConfig();
            if (migrate) {
                plugin.getLogger().info("Converted config.yml from version " + found + " to " + CURRENT
                        + " (" + added + " new keys). Existing values were kept. Backup: " + backupName);
            } else if (healBoards) {
                plugin.getLogger().info("Updated the scoreboard lines from the previous per-line keys. Backup: " + backupName);
            } else {
                plugin.getLogger().info("Restored the notes in config.yml. Your values were kept. Backup: " + backupName);
            }
            return added;
        } catch (RuntimeException | IOException ex) {
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
            case 8 -> {
                // Gamemode blocks saved as "classic: {}" are filled from the jar after this step.
            }
            case 9 -> to10(config, defaults);
            case 10 -> to11(config, defaults);
            case 11 -> to12(config, defaults);
            case 12 -> to13(config, defaults);
            case 13 -> to14(config, defaults);
            case 14 -> to15(config, defaults);
            case 15 -> to16(config, defaults);
            case 16 -> to17(config, defaults);
            case 17 -> to18(config, defaults);
            case 18 -> to19(config, defaults);
            case 19 -> to20(config, defaults);
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
        java.util.function.Function<String, String> read = reader(config);
        for (String mode : BoardLayouts.MODES) {
            setLayout(config, "board-" + mode, BoardLayouts.compose(mode, read));
        }
    }

    /**
     * Folds the old per-line keys into {@code board-*} when a layout is missing, then removes the
     * per-line keys. A layout that is present, including the shipped default, is never overwritten:
     * the composed form would drop the newer {title}, {date}, {level_name} and {progress} tokens.
     */
    private static void to10(FileConfiguration config, YamlConfiguration defaults) {
        java.util.function.Function<String, String> read = reader(config);
        if (!BoardLayouts.hasLegacy(read)) {
            return;
        }
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            String current = own(config, path) ? config.getString(path) : null;
            if (current == null || current.isBlank()) {
                config.set(path, BoardLayouts.compose(mode, read));
            }
        }
        for (String old : BoardLayouts.LEGACY) {
            config.set("messages." + old, null);
        }
    }

    /**
     * Classic's icon moved to bedrock (Rush uses a diamond sword) and the fireball was retuned to
     * match the bedwars1058 fork. Only values still on the old default are changed.
     */
    private static void to11(FileConfiguration config, YamlConfiguration defaults) {
        if ("NETHER_STAR".equalsIgnoreCase(config.getString("gamemodes.classic.icon", ""))) {
            config.set("gamemodes.classic.icon", "BEDROCK");
        }
        fireballDefault(config, defaults, "fireball.speed", 1.6D);
        fireballDefault(config, defaults, "fireball.knockback-horizontal", 2.6D);
        fireballDefault(config, defaults, "fireball.knockback-vertical", 1.1D);
        fireballDefault(config, defaults, "fireball.radius", 3.5D);
        fireballDefault(config, defaults, "fireball.yield", 2.0D);
    }

    /** Mode descriptions shown in the mode menu, plus the line used when a mode has no item drops. */
    private static void to12(FileConfiguration config, YamlConfiguration defaults) {
        ensure(config, defaults, "messages.gamemode-lore-kit");
        for (String id : new String[]{"classic", "rush", "custom"}) {
            ensure(config, defaults, "gamemodes." + id + ".description");
        }
    }

    /**
     * Leveling, the lobby visibility toggle, and the level lines in the sidebar. A layout the admin
     * edited keeps its text; the level lines are appended only when they are not there yet.
     */
    private static void to13(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || layout.contains("{level_name}")) {
                continue;
            }
            // Literal backslash-n, the same shape the shipped layouts use; the sidebar reads both.
            config.set(path, layout + "\\n&fLevel: {level_name}\\n{progress}");
        }
        ensure(config, defaults, "visibility");
        ensure(config, defaults, "leveling");
        ensure(config, defaults, "messages.top-unknown");
    }

    /**
     * The sidebar title became {@code {title}} so a match can show its mode name (RUSH, CLASSIC),
     * and a light-gray {@code &7{date}} line was added right under it. Only a leading literal title
     * is swapped, and the date line is inserted once.
     */
    private static void to14(FileConfiguration config, YamlConfiguration defaults) {
        String[] oldTitles = {"&6&lPILLARS", "&6&lSTARTING", "&6&lFORTUNE", "&6&lFINISHED"};
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || layout.isBlank()) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            boolean changed = false;
            if (!lines.isEmpty()) {
                for (String old : oldTitles) {
                    if (lines.get(0).trim().equals(old)) {
                        lines.set(0, "{title}");
                        changed = true;
                        break;
                    }
                }
            }
            if (!layout.contains("{date}")) {
                lines.add(1, "&7{date}");
                changed = true;
            }
            if (changed) {
                config.set(path, String.join("\\n", lines));
            }
        }
        ensure(config, defaults, "queue-alone-seconds");
    }

    /**
     * The XP bar belongs right above the server IP, and the bed hub item was added (off by default).
     * The bar line is removed wherever it was and re-inserted above the last line.
     */
    private static void to15(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || !layout.contains("{progress}")) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            lines.removeIf(line -> line.contains("{progress}"));
            lines.add(Math.max(0, lines.size() - 1), "{progress}");
            config.set(path, String.join("\\n", lines));
        }
        ensure(config, defaults, "lobby-items.hub-enabled");
        ensure(config, defaults, "lobby-items.hub-material");
        ensure(config, defaults, "lobby-items.hub-name");
        ensure(config, defaults, "lobby-items.hub-command");
    }

    /**
     * Repairs the sidebar to the modern shape. Older configs, and ones healed from the per-line keys,
     * can have a literal title, no date, and no level or bar. The title becomes {@code {title}} so a
     * match shows its mode (RUSH, CLASSIC), the date line is added, arena boards drop the level line,
     * and the bar is placed above the last line (the server IP).
     */
    private static void to16(FileConfiguration config, YamlConfiguration defaults) {
        String[] oldTitles = {"&6&lPILLARS", "&6&lSTARTING", "&6&lFORTUNE", "&6&lFINISHED"};
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || layout.isBlank()) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            boolean changed = false;
            if (!lines.isEmpty()) {
                for (String old : oldTitles) {
                    if (lines.get(0).trim().equals(old)) {
                        lines.set(0, "{title}");
                        changed = true;
                        break;
                    }
                }
            }
            if (!String.join("\n", lines).contains("{date}")) {
                lines.add(Math.min(1, lines.size()), "&7{date}");
                changed = true;
            }
            boolean lobbyish = "lobby".equals(mode) || "queued".equals(mode);
            if (lobbyish && !String.join("\n", lines).contains("{level_name}")) {
                lines.add(Math.min(2, lines.size()), "&fLevel: {level_name}");
                changed = true;
            }
            if (!lobbyish) {
                changed |= lines.removeIf(line -> line.contains("{level_name}"));
            }
            if (!String.join("\n", lines).contains("{progress}")) {
                lines.add(Math.max(0, lines.size() - 1), "{progress}");
                changed = true;
            }
            if (changed) {
                config.set(path, String.join("\\n", lines));
            }
        }
    }

    /**
     * The level shows on every board again, the winstreak bonus is softer, and the fixed rankup table
     * gives way to the progressive curve in {@code leveling.rankup-base/growth/cap}. Values an admin
     * changed by hand are left alone.
     */
    private static void to17(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || layout.isBlank() || layout.contains("{level_name}")) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            int at = lines.size() < 2 ? lines.size() : (lines.get(1).contains("{date}") ? 2 : 1);
            lines.add(Math.min(at, lines.size()), "&fLevel: {level_name}");
            config.set(path, String.join("\\n", lines));
        }
        if (config.getInt("leveling.winstreak-step", 25) == 25) {
            config.set("leveling.winstreak-step", 10);
        }
        if (config.getInt("leveling.winstreak-cap", 250) == 250) {
            config.set("leveling.winstreak-cap", 100);
        }
        ConfigurationSection rankups = config.getConfigurationSection("leveling.rankups");
        if (rankups != null && isOldRankupTable(rankups)) {
            config.set("leveling.rankups", null);
        }
    }

    /** True when {@code leveling.rankups} still holds the exact values the jar used to ship. */
    private static boolean isOldRankupTable(ConfigurationSection rankups) {
        java.util.Map<String, Integer> expected = java.util.Map.of(
                "1", 500, "2", 1000, "3", 1500, "4-19", 2000, "others", 3000);
        if (!rankups.getKeys(false).equals(expected.keySet())) {
            return false;
        }
        for (java.util.Map.Entry<String, Integer> entry : expected.entrySet()) {
            if (rankups.getInt(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** The lobby sidebar no longer ships a "/pof join" hint line. */
    private static void to18(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || !layout.contains("&6/pof join")) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            if (lines.removeIf(line -> line.trim().equals("&6/pof join"))) {
                config.set(path, String.join("\\n", lines));
            }
        }
    }

    /** The level now sits right next to its progress bar on a single line. */
    private static void to19(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || !layout.contains("{progress}")) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            boolean changed = lines.removeIf(line -> line.contains("{level_name}") && !line.contains("{progress}"));
            String joined = "&fLevel: {level_name} {progress}";
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("{progress}")) {
                    if (!lines.get(i).equals(joined)) {
                        lines.set(i, joined);
                        changed = true;
                    }
                    break;
                }
            }
            if (changed) {
                config.set(path, String.join("\\n", lines));
            }
        }
    }

    /** The blank &8 spacer line was dropped from every board so the sidebar has no gaps. */
    private static void to20(FileConfiguration config, YamlConfiguration defaults) {
        for (String mode : BoardLayouts.MODES) {
            String path = "messages.board-" + mode;
            if (!own(config, path)) {
                continue;
            }
            String layout = config.getString(path);
            if (layout == null || !layout.contains("&8")) {
                continue;
            }
            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(layout.replace("\\n", "\n").split("\n", -1)));
            if (lines.removeIf(line -> line.trim().equals("&8"))) {
                config.set(path, String.join("\\n", lines));
            }
        }
    }

    private static void fireballDefault(FileConfiguration config, YamlConfiguration defaults,
                                        String path, double oldDefault) {
        if (Double.compare(config.getDouble(path, oldDefault), oldDefault) == 0) {
            config.set(path, defaults.getDouble(path, oldDefault));
        }
    }

    private static java.util.function.Function<String, String> reader(FileConfiguration config) {
        return key -> own(config, "messages." + key) ? config.getString("messages." + key) : null;
    }

    private static void setLayout(FileConfiguration config, String key, String layout) {
        if (!own(config, "messages." + key)) {
            config.set("messages." + key, layout);
        }
    }

    /** Runs one migration step. Tests use this to check a single version. */
    static void step(int from, FileConfiguration config, YamlConfiguration defaults) {
        migrate(from, config, defaults);
    }

    private static void replaceIf(FileConfiguration config, String path, String from, String to) {
        if (from.equals(config.getString(path))) {
            config.set(path, to);
        }
    }

    private static void move(FileConfiguration config, String from, String to) {
        if (!own(config, from) || own(config, to)) {
            return;
        }
        config.set(to, config.get(from));
        config.set(from, null);
    }

    private static void ensure(FileConfiguration config, YamlConfiguration defaults, String path) {
        if (!own(config, path) && defaults.contains(path)) {
            copy(config, path, defaults.get(path));
        }
    }

    private static void force(FileConfiguration config, YamlConfiguration defaults, String path) {
        if (defaults.contains(path)) {
            copy(config, path, defaults.get(path));
        }
    }

    /**
     * True when {@code path} is stored in this file. Jar defaults do not count.
     * {@code contains} is true for every key the jar ships, even when the file never set it.
     */
    static boolean own(ConfigurationSection section, String path) {
        if (section == null || path == null || path.isEmpty()) {
            return false;
        }
        String[] parts = path.split("\\.");
        ConfigurationSection current = section;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current == null || !current.getKeys(false).contains(parts[i])) {
                return false;
            }
            current = current.getConfigurationSection(parts[i]);
        }
        return current != null && current.getKeys(false).contains(parts[parts.length - 1]);
    }

    /**
     * Copies a value, including every nested key. Setting a configuration section directly
     * saves as {@code classic: {}} and drops enabled, name, and icon.
     */
    static void copy(FileConfiguration config, String path, Object value) {
        if (value instanceof ConfigurationSection section) {
            for (String key : section.getKeys(false)) {
                copy(config, path + "." + key, section.get(key));
            }
            return;
        }
        config.set(path, value);
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
                if (!own(config, path) || !config.isConfigurationSection(path)) {
                    copy(config, path, child);
                    added++;
                } else if (config.getConfigurationSection(path).getKeys(false).isEmpty()) {
                    copy(config, path, child);
                    added++;
                } else {
                    added += fillMissing(child, config, path);
                }
            } else if (!own(config, path)) {
                copy(config, path, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    private static byte[] resource(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                plugin.getLogger().severe("The jar is missing config.yml, so the old config was not converted.");
                return null;
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read the default config.yml", ex);
            return null;
        }
    }

    private static YamlConfiguration defaults(byte[] raw, JavaPlugin plugin) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(raw), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read the default config.yml", ex);
            return null;
        }
    }

    private static void backup(JavaPlugin plugin, String name) {
        File current = new File(plugin.getDataFolder(), "config.yml");
        if (!current.isFile()) {
            return;
        }
        File copy = new File(plugin.getDataFolder(), name);
        try {
            Files.copy(current.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not back up config.yml before converting it.", ex);
        }
    }
}
