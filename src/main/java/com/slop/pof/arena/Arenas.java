package com.slop.pof.arena;

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
    private String layoutSignature;

    public Arenas(PoFPlugin plugin, Pillars pillars) {
        this.plugin = plugin;
        this.pillars = pillars;
        this.pillarSignature = plugin.settings().pillarSignature();
        this.layoutSignature = plugin.settings().layoutSignature();
    }

    public void createAll(int count) {
        for (int id = 1; id <= count; id++) {
            if (create(id) == null) {
                break;
            }
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
                Arena created = create(id);
                if (created == null) {
                    break;
                }
                plugin.resets().enqueue(created);
            }
        }
        if (!plugin.settings().layoutSignature().equals(layoutSignature)) {
            layoutSignature = plugin.settings().layoutSignature();
            restart = true;
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

    private Arena create(int id) {
        int reach = plugin.settings().resetReach();
        int spacing = plugin.settings().arenaSpacing();
        int[] step = ArenaLayout.steps(id - 1);
        int centerX = step[0] * spacing;
        int centerZ = step[1] * spacing;
        if (!ArenaLayout.inside(centerX, centerZ, reach)) {
            plugin.getLogger().warning("Arena " + id + " at " + centerX + ", " + centerZ
                    + " would leave the vanilla world border, so it was not created.");
            return null;
        }
        Arena arena = new Arena(id, centerX, centerZ);
        arena.world = plugin.worlds().world(id);
        pillars.build(arena);
        arena.state = Arena.State.WAITING;
        arenas.put(id, arena);
        return arena;
    }
}
