package com.slop.pof.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Arena {
    public enum State {
        WAITING, STARTING, INGAME, ENDING, RESETTING
    }

    public final int id;
    public State state = State.WAITING;
    public final Set<UUID> players = new LinkedHashSet<>();
    public final Set<UUID> alive = new LinkedHashSet<>();
    public final List<Location> pillars = new ArrayList<>();
    public World world;
    public int count;
    public int item;
    public int grace;
    public int time;
    public int end;
    public UUID winner;

    public Arena(int id) {
        this.id = id;
    }

    public int aliveCount() {
        return alive.size();
    }

    public Location center() {
        if (world == null) {
            return null;
        }
        double y = pillars.isEmpty() ? 70 : pillars.get(0).getY();
        return new Location(world, id * 2000.0, y, 0);
    }
}
