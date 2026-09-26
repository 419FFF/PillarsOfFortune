package com.slop.pof.config;

import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.slop.pof.arena.ArenaLayout;
import com.slop.pof.util.Placeholders;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class Settings {
    private final String prefix;
    private final String serverIp;
    private final String lobbyWorld;
    private final String gameWorld;
    private final boolean clearLobbyWeather;
    private final int arenaCount;
    private final String resetMode;
    private final int resetBlocksPerTick;
    private final int arenaSpacing;
    private final int resetRadius;
    private final int pillarCount;
    private final int pillarRadius;
    private final int pillarHeight;
    private final int pillarY;
    private final List<int[]> offsets;
    private final int voidY;
    private final int arenaSize;
    private final int buildRadius;
    private final int clearRadius;
    private final int clearHeight;
    private final int maxBuildY;
    private final int borderKill;
    private final int voidCreditSeconds;
    private final int minPlayers;
    private final int maxPlayers;
    private final int countdown;
    private final int queueSeconds;
    private final int itemSeconds;
    private final int itemDelaySeconds;
    private final List<Gamemode> gamemodes;
    private final int graceSeconds;
    private final int endSeconds;
    private final int maxGameSeconds;
    private final GameMode lobbyGamemode;
    private final double fireballSpeed;
    private final double fireballKnockX;
    private final double fireballKnockY;
    private final double fireballRadius;
    private final double fireballCooldown;
    private final double fireballDamageSelf;
    private final double fireballDamageEnemy;
    private final boolean fireballFire;
    private final double fireballYield;
    private final int blockMin;
    private final int blockMax;
    private final int pearlChance;
    private final int enchantKeepChance;
    private final int rateWeapons;
    private final int rateArmor;
    private final int rateBlocks;
    private final int rateChaos;
    private final String joinMaterial;
    private final String joinName;
    private final String leaveMaterial;
    private final String leaveName;
    private final String vipMaterial;
    private final String vipName;
    private final String storageType;
    private final String h2File;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final int leaderboardSize;
    private final int leaderboardRefreshTicks;
    private final boolean playtimeOnlineOnly;
    private final Map<String, String> sounds;
    private final Map<String, String> messages;
    private final List<String> weapons;
    private final List<String> armor;
    private final List<String> blocks;
    private final List<String> chaos;
    private final List<String> illegal;
    private final int defaultAmount;
    private final Map<String, String> itemAmounts;
    private final Map<String, Integer> itemChances;
    private final Map<String, Integer> enchantKeepByItem;
    private final Map<String, List<String>> itemEnchants;
    private final String fallbackWeapons;
    private final String fallbackArmor;
    private final String fallbackBlocks;
    private final String fallbackChaos;

    private Settings(FileConfiguration c) {
        prefix = c.getString("prefix", "&8[&6PoF&8]&r");
        serverIp = c.getString("server-ip", "example.com");
        lobbyWorld = c.getString("worlds.lobby-name", "world");
        gameWorld = c.getString("worlds.game-name", "pof_void");
        clearLobbyWeather = c.getBoolean("worlds.clear-lobby-weather", false);
        arenaCount = Math.max(1, c.getInt("arenas.count", 6));
        resetMode = c.getString("arenas.reset-mode", "region");
        resetBlocksPerTick = Math.max(1, c.getInt("arenas.reset-blocks-per-tick", 8000));
        arenaSpacing = Math.max(1, c.getInt("arenas.spacing", 2000));
        resetRadius = Math.max(1, c.getInt("arenas.reset-radius", 128));
        pillarCount = Math.max(1, c.getInt("pillars.count", 12));
        pillarRadius = c.getInt("pillars.radius", 20);
        pillarHeight = Math.max(1, c.getInt("pillars.height", 45));
        pillarY = c.getInt("pillars.y", 70);
        offsets = readOffsets(c.getStringList("pillars.offsets"));
        voidY = c.getInt("void-y", 8);
        arenaSize = c.getInt("arena-size", 40);
        buildRadius = c.getInt("build-radius", 48);
        clearRadius = Math.max(1, c.getInt("clear-radius", 72));
        clearHeight = Math.max(1, c.getInt("clear-height", 120));
        maxBuildY = c.getInt("max-build-y", 120);
        borderKill = c.getInt("border-kill", 70);
        voidCreditSeconds = Math.max(0, c.getInt("void-credit-seconds", 10));
        minPlayers = Math.max(1, c.getInt("min-players", 2));
        maxPlayers = Math.max(minPlayers, c.getInt("max-players", 12));
        countdown = Math.max(1, c.getInt("countdown", 10));
        queueSeconds = Math.max(1, c.getInt("queue-seconds", 30));
        itemSeconds = Math.max(1, c.getInt("item-seconds", 5));
        itemDelaySeconds = Math.max(0, c.getInt("item-delay-seconds", 3));
        gamemodes = readGamemodes(c);
        graceSeconds = Math.max(0, c.getInt("grace-seconds", 5));
        endSeconds = Math.max(1, c.getInt("end-seconds", 8));
        maxGameSeconds = Math.max(1, c.getInt("max-game-seconds", 600));
        lobbyGamemode = parseMode(c.getString("lobby-gamemode", "ADVENTURE"));
        fireballSpeed = c.getDouble("fireball.speed", 1.6);
        fireballKnockX = c.getDouble("fireball.knockback-horizontal", c.getDouble("fireball.knockback-x", 2.6));
        fireballKnockY = c.getDouble("fireball.knockback-vertical", c.getDouble("fireball.knockback-y", 1.1));
        fireballRadius = c.getDouble("fireball.radius", 3.5);
        fireballCooldown = c.getDouble("fireball.cooldown", 0.5);
        fireballDamageSelf = c.getDouble("fireball.damage-self", 2.0);
        fireballDamageEnemy = c.getDouble("fireball.damage-enemy", 2.0);
        fireballFire = c.getBoolean("fireball.fire", false);
        fireballYield = c.getDouble("fireball.yield", 2.0);
        blockMin = c.getInt("block-min", 4);
        blockMax = c.getInt("block-max", 12);
        pearlChance = c.getInt("pearl-chance", 15);
        enchantKeepChance = c.getInt("items.enchant-keep-chance", c.getInt("enchant-keep-chance", 12));
        rateWeapons = c.getInt("rates.weapons", 8);
        rateArmor = c.getInt("rates.armor", 7);
        rateBlocks = c.getInt("rates.blocks", 72);
        rateChaos = c.getInt("rates.chaos", 13);
        joinMaterial = c.getString("lobby-items.join-material", "NETHER_STAR");
        joinName = c.getString("lobby-items.join-name", "&6&lJoin Queue");
        leaveMaterial = c.getString("lobby-items.leave-material", "BARRIER");
        leaveName = c.getString("lobby-items.leave-name", "&c&lLeave Queue");
        vipMaterial = c.getString("lobby-items.vip-material", "DIAMOND");
        vipName = c.getString("lobby-items.vip-name", "&6&lStart Match");
        storageType = c.getString("storage.type", "h2");
        h2File = c.getString("storage.h2.file", "data");
        mysqlHost = c.getString("storage.mysql.host", "127.0.0.1");
        mysqlPort = c.getInt("storage.mysql.port", 3306);
        mysqlDatabase = c.getString("storage.mysql.database", "minecraft");
        mysqlUser = c.getString("storage.mysql.user", "root");
        mysqlPassword = c.getString("storage.mysql.password", "");
        leaderboardSize = Math.max(1, c.getInt("leaderboards.size", 10));
        leaderboardRefreshTicks = Math.max(20, c.getInt("leaderboards.refresh-ticks", 200));
        playtimeOnlineOnly = c.getBoolean("stats.playtime-online-only", true);
        sounds = readSection(c, "sounds");
        messages = readSection(c, "messages");
        weapons = list(c, "items.weapons");
        armor = list(c, "items.armor");
        blocks = list(c, "items.blocks");
        chaos = list(c, "items.chaos");
        illegal = list(c, "items.illegal");
        defaultAmount = Math.max(1, c.getInt("items.default-amount", 1));
        itemAmounts = normalizeKeys(readSection(c, "items.amounts"));
        itemChances = readInts(c, "items.chances");
        enchantKeepByItem = readInts(c, "items.enchant-keep");
        itemEnchants = readNamedLists(c, "items.enchants");
        fallbackWeapons = c.getString("items.enchant-fallback.weapons", "WOOD_SWORD");
        fallbackArmor = c.getString("items.enchant-fallback.armor", "LEATHER_HELMET");
        fallbackBlocks = c.getString("items.enchant-fallback.blocks", "COBBLESTONE");
        fallbackChaos = c.getString("items.enchant-fallback.chaos", "COBBLESTONE");
    }

    public static Settings from(FileConfiguration config) {
        return new Settings(config);
    }

    public String storageFingerprint() {
        return storageType + "|" + h2File + "|" + mysqlHost + "|" + mysqlPort + "|"
                + mysqlDatabase + "|" + mysqlUser + "|" + mysqlPassword;
    }

    public String pillarSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(pillarCount).append('/').append(pillarHeight).append('/').append(pillarY).append('/').append(pillarRadius);
        for (int[] offset : offsets) {
            sb.append(';').append(offset[0]).append(',').append(offset[1]);
        }
        return sb.toString();
    }

    public String worldSignature() {
        return gameWorld + "|" + resetMode + "|" + lobbyWorld;
    }

    public String layoutSignature() {
        return arenaSpacing + "/" + resetRadius + "/" + buildRadius + "/" + borderKill + "/" + clearRadius;
    }

    /** Box half-size used when an arena is wiped. Always larger than the playable arena. */
    public int resetReach() {
        int play = Math.max(buildRadius, Math.max(Math.abs(borderKill), Math.max(clearRadius, pillarRadius + 4)));
        return Math.max(resetRadius, play + 16);
    }

    /** Distance between arena centers. Never small enough for two reset boxes to overlap. */
    public int arenaSpacing() {
        int min = resetReach() * 2 + 32;
        return Math.max(min, arenaSpacing);
    }

    public boolean worldPerArena() {
        return "world-per-arena".equalsIgnoreCase(resetMode);
    }

    public List<int[]> pillarOffsets() {
        List<int[]> out = new ArrayList<>();
        int n = pillarCount;
        for (int i = 0; i < n; i++) {
            if (i < offsets.size()) {
                out.add(offsets.get(i));
            } else {
                double angle = (Math.PI * 2.0 * i) / n;
                int x = (int) Math.round(pillarRadius * Math.cos(angle));
                int z = (int) Math.round(pillarRadius * Math.sin(angle));
                out.add(new int[]{x, z});
            }
        }
        return out;
    }

    public String chat(String key, String... pairs) {
        return chat(null, key, pairs);
    }

    /** Same as {@link #chat(String, String...)}, then LuckPerms and PlaceholderAPI for this player. */
    public String chat(Player player, String key, String... pairs) {
        return Placeholders.apply(player, Text.color(prefix + " " + apply(key, pairs)));
    }

    public String text(String key, String... pairs) {
        return text(null, key, pairs);
    }

    /** Same as {@link #text(String, String...)}, then LuckPerms and PlaceholderAPI for this player. */
    public String text(Player player, String key, String... pairs) {
        return Placeholders.apply(player, Text.color(apply(key, pairs)));
    }

    public String raw(String key, String... pairs) {
        return raw(null, key, pairs);
    }

    /** Join and quit lines. {@code {prefix}} is this plugin's prefix, not the LuckPerms prefix. */
    public String raw(Player player, String key, String... pairs) {
        String body = apply(key, pairs).replace("{prefix}", prefix);
        return Placeholders.apply(player, Text.color(body));
    }

    /**
     * Arena chat for one speaker. {@code {message}} is inserted after LuckPerms and PlaceholderAPI
     * run, so the words they typed cannot inject placeholders.
     */
    public String chatLine(Player speaker, String key, String message) {
        String body = messages.getOrDefault(key, key);
        String name = speaker == null ? "" : speaker.getName();
        String spoken = Text.color(message);
        int at = body.indexOf("{message}");
        if (at < 0) {
            return Placeholders.apply(speaker, Text.color(body.replace("{player}", name)));
        }
        String left = body.substring(0, at).replace("{player}", name);
        String right = body.substring(at + "{message}".length()).replace("{player}", name);
        return Placeholders.apply(speaker, Text.color(left)) + spoken + Placeholders.apply(speaker, Text.color(right));
    }

    public String sound(String key, String fallback) {
        return sounds.getOrDefault(key, fallback);
    }

    private String apply(String key, String... pairs) {
        String body = messages.getOrDefault(key, key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            body = body.replace("{" + pairs[i] + "}", pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return body;
    }

    private static Map<String, String> readSection(FileConfiguration c, String path) {
        Map<String, String> map = new HashMap<>();
        if (c.getConfigurationSection(path) == null) {
            return map;
        }
        for (String key : c.getConfigurationSection(path).getKeys(false)) {
            map.put(key, c.getString(path + "." + key, ""));
        }
        return map;
    }

    private static Map<String, String> normalizeKeys(Map<String, String> raw) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            out.put(key(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private static Map<String, Integer> readInts(FileConfiguration c, String path) {
        Map<String, Integer> out = new HashMap<>();
        ConfigurationSection section = c.getConfigurationSection(path);
        if (section == null) {
            return out;
        }
        for (String name : section.getKeys(false)) {
            out.put(key(name), section.getInt(name));
        }
        return out;
    }

    private static Map<String, List<String>> readNamedLists(FileConfiguration c, String path) {
        Map<String, List<String>> out = new HashMap<>();
        ConfigurationSection section = c.getConfigurationSection(path);
        if (section == null) {
            return out;
        }
        for (String name : section.getKeys(false)) {
            out.put(key(name), List.copyOf(section.getStringList(name)));
        }
        return out;
    }

    private static String key(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static List<String> list(FileConfiguration c, String path) {
        List<String> values = c.getStringList(path);
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<int[]> readOffsets(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaultOffsets();
        }
        List<int[]> parsed = new ArrayList<>();
        for (String line : raw) {
            String[] parts = line.split(",");
            if (parts.length != 2) {
                continue;
            }
            try {
                parsed.add(new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())});
            } catch (NumberFormatException ignored) {
                // skip a bad offset line
            }
        }
        return parsed.isEmpty() ? defaultOffsets() : parsed;
    }

    private static List<int[]> defaultOffsets() {
        return List.of(
                new int[]{20, 0}, new int[]{17, 10}, new int[]{10, 17}, new int[]{0, 20},
                new int[]{-10, 17}, new int[]{-17, 10}, new int[]{-20, 0}, new int[]{-17, -10},
                new int[]{-10, -17}, new int[]{0, -20}, new int[]{10, -17}, new int[]{17, -10}
        );
    }

    private static GameMode parseMode(String name) {
        try {
            return GameMode.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GameMode.ADVENTURE;
        }
    }

    public String prefix() { return prefix; }
    public String serverIp() { return serverIp; }
    public String lobbyWorld() { return lobbyWorld; }
    public String gameWorld() { return gameWorld; }
    public boolean clearLobbyWeather() { return clearLobbyWeather; }
    public int arenaCount() { return arenaCount; }
    public String resetMode() { return resetMode; }
    public int resetBlocksPerTick() { return resetBlocksPerTick; }
    public int pillarCount() { return pillarCount; }
    public int pillarRadius() { return pillarRadius; }
    public int pillarHeight() { return pillarHeight; }
    public int pillarY() { return pillarY; }
    public int voidY() { return voidY; }
    public int arenaSize() { return arenaSize; }
    public int buildRadius() { return buildRadius; }
    public int clearRadius() { return clearRadius; }
    public int clearHeight() { return clearHeight; }
    public int maxBuildY() { return maxBuildY; }
    public int borderKill() { return borderKill; }
    public int voidCreditSeconds() { return voidCreditSeconds; }
    public int minPlayers() { return minPlayers; }
    public int maxPlayers() { return maxPlayers; }
    public int countdown() { return countdown; }
    public int queueSeconds() { return queueSeconds; }
    public int itemSeconds() { return itemSeconds; }
    public int itemDelaySeconds() { return itemDelaySeconds; }

    public List<Gamemode> gamemodes() {
        return gamemodes;
    }

    public List<Gamemode> enabledGamemodes() {
        List<Gamemode> enabled = new ArrayList<>();
        for (Gamemode mode : gamemodes) {
            if (mode.enabled) {
                enabled.add(mode);
            }
        }
        return enabled;
    }

    public Gamemode gamemode(String id) {
        if (id == null) {
            return null;
        }
        for (Gamemode mode : gamemodes) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }

    private static List<Gamemode> readGamemodes(FileConfiguration config) {
        List<Gamemode> modes = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("gamemodes");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection child = section.getConfigurationSection(id);
                if (child != null) {
                    modes.add(Gamemode.read(id, child));
                }
            }
        }
        if (modes.isEmpty()) {
            modes.add(Gamemode.classic());
        }
        return modes;
    }
    public int graceSeconds() { return graceSeconds; }
    public int endSeconds() { return endSeconds; }
    public int maxGameSeconds() { return maxGameSeconds; }
    public GameMode lobbyGamemode() { return lobbyGamemode; }
    public double fireballSpeed() { return fireballSpeed; }
    public double fireballKnockX() { return fireballKnockX; }
    public double fireballKnockY() { return fireballKnockY; }
    public double fireballRadius() { return fireballRadius; }
    public double fireballCooldown() { return fireballCooldown; }
    public double fireballDamageSelf() { return fireballDamageSelf; }
    public double fireballDamageEnemy() { return fireballDamageEnemy; }
    public boolean fireballFire() { return fireballFire; }
    public double fireballYield() { return fireballYield; }
    public int defaultAmount() { return defaultAmount; }
    public String amountSpec(String materialName) { return itemAmounts.get(materialName); }
    public int chanceFor(String materialName, int fallback) { return itemChances.getOrDefault(materialName, fallback); }
    public int enchantKeepFor(String materialName) { return enchantKeepByItem.getOrDefault(materialName, enchantKeepChance); }
    public List<String> enchantsFor(String materialName) { return itemEnchants.getOrDefault(materialName, List.of()); }
    public String fallbackWeapons() { return fallbackWeapons; }
    public String fallbackArmor() { return fallbackArmor; }
    public String fallbackBlocks() { return fallbackBlocks; }
    public String fallbackChaos() { return fallbackChaos; }
    public int blockMin() { return blockMin; }
    public int blockMax() { return blockMax; }
    public int pearlChance() { return pearlChance; }
    public int enchantKeepChance() { return enchantKeepChance; }
    public int rateWeapons() { return rateWeapons; }
    public int rateArmor() { return rateArmor; }
    public int rateBlocks() { return rateBlocks; }
    public int rateChaos() { return rateChaos; }
    public String joinMaterial() { return joinMaterial; }
    public String joinName() { return joinName; }
    public String leaveMaterial() { return leaveMaterial; }
    public String leaveName() { return leaveName; }
    public String vipMaterial() { return vipMaterial; }
    public String vipName() { return vipName; }
    public String storageType() { return storageType; }
    public String h2File() { return h2File; }
    public String mysqlHost() { return mysqlHost; }
    public int mysqlPort() { return mysqlPort; }
    public String mysqlDatabase() { return mysqlDatabase; }
    public String mysqlUser() { return mysqlUser; }
    public String mysqlPassword() { return mysqlPassword; }
    public int leaderboardSize() { return leaderboardSize; }
    public int leaderboardRefreshTicks() { return leaderboardRefreshTicks; }
    public boolean playtimeOnlineOnly() { return playtimeOnlineOnly; }
    public List<String> weapons() { return weapons; }
    public List<String> armor() { return armor; }
    public List<String> blocks() { return blocks; }
    public List<String> chaos() { return chaos; }
    public List<String> illegal() { return illegal; }

    public List<int[]> configuredOffsets() {
        return Collections.unmodifiableList(offsets);
    }

    /** Logs settings that fight each other. The match still starts; the numbers in the file are what is wrong. */
    public void warnAboutConfig(Logger log, FileConfiguration file) {
        List<String> smaller = new ArrayList<>();
        if (resetRadius < buildRadius) {
            smaller.add("build-radius (" + buildRadius + ")");
        }
        if (resetRadius < borderKill) {
            smaller.add("border-kill (" + borderKill + ")");
        }
        if (resetRadius < clearRadius) {
            smaller.add("clear-radius (" + clearRadius + ")");
        }
        if (!smaller.isEmpty()) {
            log.warning("arenas.reset-radius (" + resetRadius + ") is smaller than " + String.join(", ", smaller)
                    + ". Blocks outside that radius would survive a reset. The wipe uses " + resetReach()
                    + " instead so the play area is still cleared.");
        }
        int neededSpacing = resetReach() * 2 + 32;
        if (arenaSpacing < neededSpacing) {
            log.warning("arenas.spacing (" + arenaSpacing + ") is smaller than two reset boxes (" + neededSpacing
                    + "). Neighboring arenas would erase each other. Spacing is raised to " + arenaSpacing() + ".");
        }
        int fileMin = Math.max(1, file.getInt("min-players", minPlayers));
        int fileMax = file.getInt("max-players", maxPlayers);
        if (fileMax < fileMin) {
            log.warning("max-players (" + fileMax + ") is below min-players (" + fileMin
                    + "). A round cannot fill. max-players is treated as " + maxPlayers + ".");
        }
        if (minPlayers > pillarCount) {
            log.warning("min-players (" + minPlayers + ") is higher than pillars.count (" + pillarCount
                    + "). The queue asks for more people than the arena has pillars.");
        } else if (maxPlayers > pillarCount) {
            log.warning("max-players (" + maxPlayers + ") is higher than pillars.count (" + pillarCount
                    + "). Extra queued players are left out of the round.");
        }
        int farthestAxis = 0;
        double farthest = 0;
        for (int[] offset : pillarOffsets()) {
            farthestAxis = Math.max(farthestAxis, Math.max(Math.abs(offset[0]), Math.abs(offset[1])));
            farthest = Math.max(farthest, Math.hypot(offset[0], offset[1]));
        }
        if (farthest > borderKill) {
            log.warning("border-kill (" + borderKill + ") is inside the farthest pillar (" + (int) Math.ceil(farthest)
                    + " blocks out). Players spawn already outside the border and are eliminated.");
        }
        if (farthestAxis > buildRadius) {
            log.warning("build-radius (" + buildRadius + ") does not reach the outer pillars (" + farthestAxis
                    + " blocks on X or Z). Those players cannot place blocks.");
        }
        if (voidY > pillarY) {
            log.warning("void-y (" + voidY + ") is above pillars.y (" + pillarY
                    + "). Players are eliminated as soon as they stand on a pillar.");
        }
        int pillarBottom = pillarY - pillarHeight + 1;
        if (pillarBottom < 1 || pillarY > 255 || pillarY < 1) {
            log.warning("Pillars from Y " + pillarBottom + " to " + pillarY
                    + " do not fit in a 1.8 world (Y 1 through 255). The pillar is clipped.");
        }
        if (maxBuildY < pillarY) {
            log.warning("max-build-y (" + maxBuildY + ") is below pillars.y (" + pillarY
                    + "). Players cannot place blocks from the top of a pillar.");
        }
        if (blockMin > blockMax) {
            log.warning("block-min (" + blockMin + ") is higher than block-max (" + blockMax
                    + "). Block stacks will not roll across that range.");
        }
        if (rateWeapons < 0 || rateArmor < 0 || rateBlocks < 0 || rateChaos < 0) {
            log.warning("An item rate is negative. Category rolls will not match rates.weapons, armor, blocks, and chaos.");
        } else {
            int sum = rateWeapons + rateArmor + rateBlocks + rateChaos;
            if (sum != 100) {
                log.warning("Item rates add up to " + sum + ", not 100. Rolls are still on a scale of 1 to 100, so some categories will be skipped or chosen too often.");
            }
        }
        if (pearlChance < 0 || pearlChance > 100) {
            log.warning("pearl-chance (" + pearlChance + ") is outside 0-100, so ender pearls will not drop at the chance you wrote.");
        }
        if (enchantKeepChance < 0 || enchantKeepChance > 100) {
            log.warning("enchant-keep-chance (" + enchantKeepChance + ") is outside 0-100.");
        }
        if (fireballRadius <= 0) {
            log.warning("fireball.radius (" + fireballRadius + ") does not hit anyone. Fireball knockback and damage will not apply.");
        }
        String mode = file.getString("arenas.reset-mode", resetMode);
        if (!"region".equalsIgnoreCase(mode) && !"world-per-arena".equalsIgnoreCase(mode)) {
            log.warning("arenas.reset-mode '" + mode + "' is not region or world-per-arena. Resets use region.");
        }
        String storage = file.getString("storage.type", storageType);
        if (!"h2".equalsIgnoreCase(storage) && !"mysql".equalsIgnoreCase(storage)) {
            log.warning("storage.type '" + storage + "' is not h2 or mysql. Stats will use the H2 file.");
        }
        String modeName = file.getString("lobby-gamemode", "ADVENTURE");
        try {
            GameMode.valueOf(modeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warning("lobby-gamemode '" + modeName + "' is not a 1.8 gamemode. The lobby uses ADVENTURE.");
        }
        if (weapons.isEmpty() && armor.isEmpty() && blocks.isEmpty() && chaos.isEmpty()) {
            log.warning("items.weapons, armor, blocks, and chaos are all empty. Rounds will not give items.");
        }
        if (enabledGamemodes().isEmpty()) {
            log.warning("No gamemode has enabled: true. Players cannot join a queue.");
        }
        int reach = resetReach();
        int spacing = arenaSpacing();
        int fit = 0;
        for (int id = 1; id <= arenaCount; id++) {
            int[] step = ArenaLayout.steps(id - 1);
            if (!ArenaLayout.inside(step[0] * spacing, step[1] * spacing, reach)) {
                break;
            }
            fit++;
        }
        if (fit < arenaCount) {
            log.warning("arenas.count is " + arenaCount + ", but only " + fit
                    + " fit inside the world border at the current spacing and reset size. The rest are not created.");
        }
        for (String boardMode : new String[]{"lobby", "queued", "starting", "grace", "ingame", "ending"}) {
            String layout = file.getString("messages.board-" + boardMode, "");
            int body = layout.replace("\\n", "\n").split("\n", -1).length - 1;
            if (body > 15) {
                log.warning("messages.board-" + boardMode + " has " + body
                        + " lines under the title. The sidebar shows 15.");
            }
        }
    }
}
