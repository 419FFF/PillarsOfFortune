package com.slop.pof.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import com.slop.pof.PoFPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class GameWorlds {
    private final PoFPlugin plugin;
    private final boolean perArena;
    private final String gameName;
    private final String lobbyName;
    private World shared;
    private final Map<Integer, World> each = new HashMap<>();

    public GameWorlds(PoFPlugin plugin) {
        this.plugin = plugin;
        this.perArena = plugin.settings().worldPerArena();
        this.gameName = plugin.settings().gameWorld();
        this.lobbyName = plugin.settings().lobbyWorld();
    }

    public void enable(int arenaCount) {
        if (perArena) {
            for (int id = 1; id <= arenaCount; id++) {
                ensure(id);
            }
            return;
        }
        shared = create(gameName);
    }

    public boolean perArena() {
        return perArena;
    }

    public World world(int arenaId) {
        if (perArena) {
            return ensure(arenaId);
        }
        return shared;
    }

    public void keepWeatherClear() {
        if (perArena) {
            for (World world : each.values()) {
                clear(world);
            }
        } else if (shared != null) {
            clear(shared);
        }
        if (plugin.settings().clearLobbyWeather()) {
            World lobby = Bukkit.getWorld(lobbyName);
            if (lobby != null && lobby != shared && !each.containsValue(lobby)) {
                clear(lobby);
            }
        }
    }

    public World recreate(int arenaId) {
        if (!perArena) {
            return shared;
        }
        World current = each.get(arenaId);
        String name = worldName(arenaId);
        if (current != null) {
            if (!current.getPlayers().isEmpty()) {
                return current;
            }
            File folder = current.getWorldFolder();
            Bukkit.unloadWorld(current, false);
            delete(folder);
            each.remove(arenaId);
        }
        World fresh = create(name);
        each.put(arenaId, fresh);
        return fresh;
    }

    private World ensure(int arenaId) {
        World world = each.get(arenaId);
        if (world != null) {
            return world;
        }
        World created = create(worldName(arenaId));
        each.put(arenaId, created);
        return created;
    }

    private String worldName(int arenaId) {
        return gameName + "_" + arenaId;
    }

    private World create(String name) {
        if (name.equalsIgnoreCase(lobbyName)) {
            throw new IllegalStateException("Refusing to create the lobby world '" + name + "'");
        }
        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            prepare(existing);
            return existing;
        }
        WorldCreator creator = new WorldCreator(name);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);
        creator.environment(World.Environment.NORMAL);
        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Could not create void world '" + name + "'");
        }
        prepare(world);
        plugin.getLogger().info("Void world ready: " + name);
        return world;
    }

    private void prepare(World world) {
        clear(world);
        world.setSpawnFlags(false, false);
        world.setAnimalSpawnLimit(0);
        world.setMonsterSpawnLimit(0);
        world.setWaterAnimalSpawnLimit(0);
        world.setAmbientSpawnLimit(0);
        world.setKeepSpawnInMemory(false);
        world.setDifficulty(Difficulty.NORMAL);
        world.setPVP(true);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(6000L);
        int y = plugin.settings().pillarY();
        world.setSpawnLocation(0, Math.max(1, y), 0);
    }

    private static void clear(World world) {
        if (world == null) {
            return;
        }
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setThunderDuration(0);
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) {
                for (File kid : kids) {
                    delete(kid);
                }
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }
}
