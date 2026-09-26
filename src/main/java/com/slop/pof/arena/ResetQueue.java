package com.slop.pof.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.slop.pof.PoFPlugin;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Clears one arena at a time. Region mode sets blocks to air with the Bukkit API.
 * World-per-arena unloads that arena world, deletes the folder, and creates a new void world.
 */
public final class ResetQueue {
    private final PoFPlugin plugin;
    private final Pillars pillars;
    private final Queue<Arena> pending = new ArrayDeque<>();
    private Arena active;
    private boolean worldReset;
    private int phase;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;
    private int x;
    private int y;
    private int z;

    public ResetQueue(PoFPlugin plugin, Pillars pillars) {
        this.plugin = plugin;
        this.pillars = pillars;
    }

    public void enqueue(Arena arena) {
        if (arena == null) {
            return;
        }
        if (arena.state == Arena.State.RESETTING || pending.contains(arena) || arena == active) {
            return;
        }
        arena.state = Arena.State.RESETTING;
        pending.add(arena);
    }

    public void tick() {
        if (active == null) {
            active = pending.poll();
            if (active == null) {
                return;
            }
            begin();
        }
        if (active == null) {
            return;
        }
        if (worldReset) {
            stepWorld();
        } else {
            stepRegion();
        }
    }

    private void begin() {
        World world = active.world;
        worldReset = plugin.worlds().perArena();
        phase = 0;
        if (world == null) {
            finish();
            return;
        }
        removeEntities(world);
        if (worldReset) {
            return;
        }
        int radius = plugin.settings().resetReach();
        minX = active.centerX - radius;
        maxX = active.centerX + radius;
        minZ = active.centerZ - radius;
        maxZ = active.centerZ + radius;
        minY = 0;
        maxY = world.getMaxHeight() - 1;
        int cx0 = minX >> 4;
        int cx1 = maxX >> 4;
        int cz0 = minZ >> 4;
        int cz1 = maxZ >> 4;
        for (int chunkX = cx0; chunkX <= cx1; chunkX++) {
            for (int chunkZ = cz0; chunkZ <= cz1; chunkZ++) {
                world.loadChunk(chunkX, chunkZ);
            }
        }
        x = minX;
        y = minY;
        z = minZ;
    }

    private void stepRegion() {
        World world = active.world;
        if (world == null) {
            finish();
            return;
        }
        int budget = plugin.settings().resetBlocksPerTick();
        int done = 0;
        while (done < budget) {
            if (world.getBlockAt(x, y, z).getType() != Material.AIR) {
                world.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
            done++;
            if (!advance()) {
                pillars.build(active);
                finish();
                return;
            }
        }
    }

    private boolean advance() {
        y++;
        if (y <= maxY) {
            return true;
        }
        y = minY;
        z++;
        if (z <= maxZ) {
            return true;
        }
        z = minZ;
        x++;
        return x <= maxX;
    }

    private void stepWorld() {
        World world = active.world;
        if (phase == 0) {
            if (world != null && !world.getPlayers().isEmpty()) {
                for (Player player : world.getPlayers()) {
                    Location lobby = plugin.lobby().resolve();
                    if (lobby != null) {
                        player.teleport(lobby);
                    }
                }
                return;
            }
            phase = 1;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (active == null) {
                    return;
                }
                World fresh = plugin.worlds().recreate(active.id);
                active.world = fresh;
                pillars.build(active);
                finish();
            });
        }
    }

    private void removeEntities(World world) {
        int radius = plugin.settings().resetReach();
        Location center = active.centerAt(plugin.settings().pillarY());
        if (center == null) {
            return;
        }
        double height = world.getMaxHeight();
        for (Entity entity : world.getNearbyEntities(center, radius + 1.0, height, radius + 1.0)) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    private void finish() {
        active.players.clear();
        active.alive.clear();
        active.winner = null;
        active.gamemodeId = null;
        active.count = 0;
        active.item = 0;
        active.grace = 0;
        active.time = 0;
        active.end = 0;
        active.state = Arena.State.WAITING;
        active = null;
        phase = 0;
    }
}
