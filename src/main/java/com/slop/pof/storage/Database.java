package com.slop.pof.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HikariCP storage. H2 is the default file database. MySQL uses the same table.
 * Tables are created on start and never dropped.
 */
public final class Database implements AutoCloseable {
    private static final String CREATE = """
            CREATE TABLE IF NOT EXISTS players (
              uuid CHAR(36) PRIMARY KEY,
              name VARCHAR(16) NOT NULL,
              wins INT NOT NULL DEFAULT 0,
              kills INT NOT NULL DEFAULT 0,
              deaths INT NOT NULL DEFAULT 0,
              games INT NOT NULL DEFAULT 0,
              streak INT NOT NULL DEFAULT 0,
              best_streak INT NOT NULL DEFAULT 0,
              items INT NOT NULL DEFAULT 0,
              playtime_min INT NOT NULL DEFAULT 0,
              updated_at TIMESTAMP NULL
            )
            """;

    private final Logger logger;
    private final String jdbcUrl;
    private final String driver;
    private final String user;
    private final String password;
    private final Map<UUID, Stats> cache = new ConcurrentHashMap<>();
    private HikariDataSource source;

    public Database(File dataFolder, String type, String h2File, String host, int port,
                    String database, String user, String password, Logger logger) {
        this.logger = logger == null ? Logger.getLogger("PillarsOfFortune") : logger;
        String kind = type == null ? "h2" : type.trim().toLowerCase(Locale.ROOT);
        if ("mysql".equals(kind)) {
            this.driver = "com.mysql.cj.jdbc.Driver";
            this.user = user == null ? "" : user;
            this.password = password == null ? "" : password;
            this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC";
        } else {
            this.driver = "org.h2.Driver";
            this.user = "sa";
            this.password = "";
            File file = new File(dataFolder, h2File == null || h2File.isBlank() ? "data" : h2File);
            String path = file.getAbsolutePath().replace('\\', '/');
            this.jdbcUrl = "jdbc:h2:file:" + path
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
        }
    }

    public String fingerprint() {
        return driver + "|" + jdbcUrl + "|" + user + "|" + password;
    }

    public void start() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver missing: " + driver, e);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(driver);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(4);
        config.setPoolName("PillarsOfFortune");
        config.setConnectionTimeout(10_000L);
        source = new HikariDataSource(config);
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE)) {
            ps.execute();
        } catch (SQLException e) {
            source.close();
            source = null;
            throw new IllegalStateException("Could not create players table", e);
        }
    }

    public void close() {
        cache.clear();
        if (source != null) {
            source.close();
            source = null;
        }
    }

    public Stats ensure(UUID uuid, String name) {
        String trimmed = trimName(name);
        Stats existing = load(uuid);
        if (existing == null) {
            Stats created = new Stats(uuid, trimmed);
            insert(created);
            cache.put(uuid, created);
            return created;
        }
        if (!trimmed.equals(existing.name)) {
            existing.name = trimmed;
            save(existing);
        }
        cache.put(uuid, existing);
        return existing;
    }

    public Stats get(UUID uuid) {
        Stats cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        Stats loaded = load(uuid);
        if (loaded != null) {
            cache.put(uuid, loaded);
        }
        return loaded;
    }

    public Stats findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String sql = "SELECT uuid, name, wins, kills, deaths, games, streak, best_streak, items, playtime_min "
                + "FROM players WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimName(name));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Stats stats = read(rs);
                cache.put(stats.uuid, stats);
                return stats;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Stats lookup failed", e);
            return null;
        }
    }

    public void addWin(UUID uuid) {
        Stats stats = require(uuid);
        stats.wins++;
        stats.streak++;
        if (stats.streak > stats.bestStreak) {
            stats.bestStreak = stats.streak;
        }
        save(stats);
    }

    public void addKill(UUID uuid) {
        Stats stats = require(uuid);
        stats.kills++;
        save(stats);
    }

    public void addDeath(UUID uuid) {
        Stats stats = require(uuid);
        stats.deaths++;
        stats.streak = 0;
        save(stats);
    }

    public void addGame(UUID uuid) {
        Stats stats = require(uuid);
        stats.games++;
        save(stats);
    }

    public void addItem(UUID uuid) {
        Stats stats = require(uuid);
        stats.items++;
        save(stats);
    }

    public void addPlaytime(UUID uuid) {
        Stats stats = require(uuid);
        stats.playtimeMin++;
        save(stats);
    }

    public List<TopEntry> top(int limit) {
        int size = Math.max(1, limit);
        String sql = "SELECT name, wins FROM players WHERE wins > 0 ORDER BY wins DESC, name ASC LIMIT ?";
        List<TopEntry> rows = new ArrayList<>();
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TopEntry(rs.getString("name"), rs.getInt("wins")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Leaderboard query failed", e);
        }
        return rows;
    }

    private Stats require(UUID uuid) {
        Stats stats = get(uuid);
        if (stats == null) {
            stats = ensure(uuid, "unknown");
        }
        return stats;
    }

    private Stats load(UUID uuid) {
        String sql = "SELECT uuid, name, wins, kills, deaths, games, streak, best_streak, items, playtime_min "
                + "FROM players WHERE uuid = ?";
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return read(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load stats", e);
        }
    }

    private static Stats read(ResultSet rs) throws SQLException {
        String raw = rs.getString("uuid");
        UUID uuid = UUID.fromString(raw.trim());
        Stats stats = new Stats(uuid, rs.getString("name").trim());
        stats.wins = rs.getInt("wins");
        stats.kills = rs.getInt("kills");
        stats.deaths = rs.getInt("deaths");
        stats.games = rs.getInt("games");
        stats.streak = rs.getInt("streak");
        stats.bestStreak = rs.getInt("best_streak");
        stats.items = rs.getInt("items");
        stats.playtimeMin = rs.getInt("playtime_min");
        return stats;
    }

    private void insert(Stats stats) {
        String sql = "INSERT INTO players (uuid, name, wins, kills, deaths, games, streak, best_streak, items, playtime_min, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, stats);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not insert stats", e);
        }
    }

    private void save(Stats stats) {
        String sql = "UPDATE players SET name = ?, wins = ?, kills = ?, deaths = ?, games = ?, streak = ?, "
                + "best_streak = ?, items = ?, playtime_min = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
        try (Connection conn = source.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stats.name);
            ps.setInt(2, stats.wins);
            ps.setInt(3, stats.kills);
            ps.setInt(4, stats.deaths);
            ps.setInt(5, stats.games);
            ps.setInt(6, stats.streak);
            ps.setInt(7, stats.bestStreak);
            ps.setInt(8, stats.items);
            ps.setInt(9, stats.playtimeMin);
            ps.setString(10, stats.uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save stats", e);
        }
    }

    private static void bind(PreparedStatement ps, Stats stats) throws SQLException {
        ps.setString(1, stats.uuid.toString());
        ps.setString(2, stats.name);
        ps.setInt(3, stats.wins);
        ps.setInt(4, stats.kills);
        ps.setInt(5, stats.deaths);
        ps.setInt(6, stats.games);
        ps.setInt(7, stats.streak);
        ps.setInt(8, stats.bestStreak);
        ps.setInt(9, stats.items);
        ps.setInt(10, stats.playtimeMin);
    }

    public static String trimName(String name) {
        if (name == null || name.isBlank()) {
            return "unknown";
        }
        String trimmed = name.trim();
        return trimmed.length() <= 16 ? trimmed : trimmed.substring(0, 16);
    }
}
