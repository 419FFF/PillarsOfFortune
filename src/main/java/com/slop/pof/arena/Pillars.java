package com.slop.pof.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import com.slop.pof.PoFPlugin;

import java.util.List;

public final class Pillars {
    private final PoFPlugin plugin;

    public Pillars(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Arena arena) {
        World world = arena.world;
        if (world == null) {
            return;
        }
        int top = plugin.settings().pillarY();
        int bot = top - plugin.settings().pillarHeight() + 1;
        if (bot < 1) {
            bot = 1;
        }
        List<int[]> offsets = plugin.settings().pillarOffsets();
        arena.pillars.clear();
        for (int[] offset : offsets) {
            int x = arena.centerX + offset[0];
            int z = arena.centerZ + offset[1];
            for (int y = bot; y <= top; y++) {
                world.getBlockAt(x, y, z).setType(Material.BEDROCK, false);
            }
            arena.pillars.add(new Location(world, x, top, z));
        }
    }

    public void cage(Arena arena, int pillarIndex, boolean on) {
        if (pillarIndex < 1 || pillarIndex > arena.pillars.size()) {
            return;
        }
        Location loc = arena.pillars.get(pillarIndex - 1);
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Material material = on ? Material.GLASS : Material.AIR;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    boolean interior = dx == 0 && dz == 0 && (dy == 1 || dy == 2 || dy == 3);
                    if (on && interior) {
                        continue;
                    }
                    world.getBlockAt(x + dx, y + dy, z + dz).setType(material, false);
                }
            }
        }
    }

    public void openAll(Arena arena) {
        for (int i = 1; i <= arena.pillars.size(); i++) {
            cage(arena, i, false);
        }
    }
}
