package com.slop.pof.config;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final int minPlayers;
    private final int maxPlayers;
    private final int countdown;
    private final int queueSeconds;
    private final int itemSeconds;
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
        minPlayers = Math.max(1, c.getInt("min-players", 2));
        maxPlayers = Math.max(minPlayers, c.getInt("max-players", 12));
        countdown = Math.max(1, c.getInt("countdown", 15));
        queueSeconds = Math.max(1, c.getInt("queue-seconds", 30));
        itemSeconds = Math.max(1, c.getInt("item-seconds", 5));
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
        return Text.color(prefix + " " + apply(key, pairs));
    }

    public String text(String key, String... pairs) {
        return Text.color(apply(key, pairs));
    }

    public String raw(String key, String... pairs) {
        String body = apply(key, pairs).replace("{prefix}", prefix);
        return Text.color(body);
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
    public int minPlayers() { return minPlayers; }
    public int maxPlayers() { return maxPlayers; }
    public int countdown() { return countdown; }
    public int queueSeconds() { return queueSeconds; }
    public int itemSeconds() { return itemSeconds; }
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
}
