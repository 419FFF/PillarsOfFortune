package com.slop.pof;

import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.slop.pof.arena.Arenas;
import com.slop.pof.arena.Pillars;
import com.slop.pof.arena.ResetQueue;
import com.slop.pof.command.PofCommand;
import com.slop.pof.config.Settings;
import com.slop.pof.game.Fireballs;
import com.slop.pof.game.Game;
import com.slop.pof.game.Items;
import com.slop.pof.listener.PoFListener;
import com.slop.pof.lobby.LobbyStore;
import com.slop.pof.storage.Database;
import com.slop.pof.ui.Boards;
import com.slop.pof.world.GameWorlds;

public final class PoFPlugin extends JavaPlugin {
    private Settings settings;
    private Database database;
    private LobbyStore lobby;
    private GameWorlds worlds;
    private Pillars pillars;
    private Arenas arenas;
    private ResetQueue resets;
    private Items items;
    private Fireballs fireballs;
    private Boards boards;
    private Game game;
    private BukkitTask task;
    private boolean debug;
    private int ticks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        settings = Settings.from(getConfig());
        if (settings.lobbyWorld().equalsIgnoreCase(settings.gameWorld())) {
            getLogger().severe("worlds.game-name must be different from worlds.lobby-name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        database = openDatabase(settings);
        try {
            database.start();
        } catch (RuntimeException ex) {
            getLogger().severe("Database failed to start: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        lobby = new LobbyStore(this);
        worlds = new GameWorlds(this);
        try {
            worlds.enable(settings.arenaCount());
        } catch (RuntimeException ex) {
            getLogger().severe("Could not prepare the void world: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        World lobbyWorld = getServer().getWorld(settings.lobbyWorld());
        if (lobbyWorld == null) {
            getLogger().warning("Lobby world '" + settings.lobbyWorld() + "' is not loaded. Use /pof setlobby after it is.");
        }
        pillars = new Pillars(this);
        arenas = new Arenas(this, pillars);
        arenas.createAll(settings.arenaCount());
        resets = new ResetQueue(this, pillars);
        for (var arena : arenas.all()) {
            resets.enqueue(arena);
        }
        items = new Items(this);
        fireballs = new Fireballs(this);
        boards = new Boards(this);
        game = new Game(this);
        game.refreshTop();
        PofCommand commands = new PofCommand(this);
        PluginCommand command = getCommand("pof");
        if (command != null) {
            command.setExecutor(commands);
            command.setTabCompleter(commands);
        }
        getServer().getPluginManager().registerEvents(new PoFListener(this), this);
        task = getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        for (Player player : getServer().getOnlinePlayers()) {
            Player online = player;
            getServer().getScheduler().runTaskLater(this, () -> {
                if (online.isOnline()) {
                    game.handleJoin(online);
                }
            }, 5L);
        }
        getLogger().info("Pillars of Fortune loaded. Arenas: " + settings.arenaCount() + ".");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (boards != null) {
            boards.clearAll();
        }
        if (database != null) {
            database.close();
            database = null;
        }
    }

    /**
     * @return true when a restart is required for the new world or pillar layout
     */
    public boolean reloadPlugin() {
        reloadConfig();
        Settings next = Settings.from(getConfig());
        if (!next.storageFingerprint().equals(settings.storageFingerprint())) {
            Database fresh = openDatabase(next);
            try {
                fresh.start();
            } catch (RuntimeException ex) {
                getLogger().severe("Storage reconnect failed: " + ex.getMessage());
                throw ex;
            }
            database.close();
            database = fresh;
            for (Player player : getServer().getOnlinePlayers()) {
                database.ensure(player.getUniqueId(), player.getName());
            }
        }
        boolean restart = !next.worldSignature().equals(settings.worldSignature());
        settings = next;
        if (arenas.applyReload()) {
            restart = true;
        }
        game.refreshTop();
        return restart;
    }

    private void tick() {
        ticks++;
        if (fireballs != null) {
            fireballs.tick();
        }
        if (resets != null) {
            resets.tick();
        }
        if (game == null) {
            return;
        }
        if (ticks % 5 == 0) {
            game.fastVoid();
        }
        if (ticks % 20 == 0) {
            game.second();
        }
        if (ticks % 100 == 0) {
            items.stripAll();
        }
        if (ticks % 200 == 0) {
            game.upkeep();
        }
        if (ticks % 1200 == 0) {
            game.playtime();
        }
        int refresh = settings.leaderboardRefreshTicks();
        if (refresh > 0 && ticks % refresh == 0) {
            game.refreshTop();
        }
    }

    private Database openDatabase(Settings source) {
        return new Database(
                getDataFolder(),
                source.storageType(),
                source.h2File(),
                source.mysqlHost(),
                source.mysqlPort(),
                source.mysqlDatabase(),
                source.mysqlUser(),
                source.mysqlPassword(),
                getLogger()
        );
    }

    public Settings settings() {
        return settings;
    }

    public Database database() {
        return database;
    }

    public LobbyStore lobby() {
        return lobby;
    }

    public GameWorlds worlds() {
        return worlds;
    }

    public Pillars pillars() {
        return pillars;
    }

    public Arenas arenas() {
        return arenas;
    }

    public ResetQueue resets() {
        return resets;
    }

    public Items items() {
        return items;
    }

    public Fireballs fireballs() {
        return fireballs;
    }

    public Boards boards() {
        return boards;
    }

    public Game game() {
        return game;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
