package com.slop.pof.arena;

import org.bukkit.World;
import com.slop.pof.PoFPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Arenas {
    private final PoFPlugin plugin;
    private final Pillars pillars;
    private final Map<Integer, Arena> arenas = new LinkedHashMap<>();
    private String pillarSignature;

    public Arenas(PoFPlugin plugin, Pillars pillars) {
        this.plugin = plugin;
        this.pillars = pillars;
        this.pillarSignature = plugin.settings().pillarSignature();
    }

    public void createAll(int count) {
        for (int id = 1; id <= count; id++) {
            create(id);
        }
    }

    public Arena get(int id) {
        return arenas.get(id);
    }

    public Collection<Arena> all() {
        return arenas.values();
    }

    public Arena free() {
        for (Arena arena : arenas.values()) {
            if (arena.state == Arena.State.WAITING) {
                return arena;
            }
        }
        return null;
    }

    /**
     * @return true when a full restart is required for the new world or pillar layout
     */
    public boolean applyReload() {
        boolean restart = false;
        int want = plugin.settings().arenaCount();
        String nextPillars = plugin.settings().pillarSignature();
        for (int id = 1; id <= want; id++) {
            if (!arenas.containsKey(id)) {
                create(id);
                plugin.resets().enqueue(arenas.get(id));
            }
        }
        List<Integer> ids = new ArrayList<>(arenas.keySet());
        for (int id : ids) {
            if (id <= want) {
                continue;
            }
            Arena arena = arenas.get(id);
            if (arena.state == Arena.State.WAITING) {
                arenas.remove(id);
            } else {
                restart = true;
            }
        }
        if (!nextPillars.equals(pillarSignature)) {
            pillarSignature = nextPillars;
            for (Arena arena : arenas.values()) {
                if (arena.state == Arena.State.WAITING) {
                    plugin.resets().enqueue(arena);
                } else if (arena.state != Arena.State.RESETTING) {
                    restart = true;
                }
            }
        }
        return restart;
    }

    private void create(int id) {
        Arena arena = new Arena(id);
        World world = plugin.worlds().world(id);
        arena.world = world;
        pillars.build(arena);
        arena.state = Arena.State.WAITING;
        arenas.put(id, arena);
    }
}
