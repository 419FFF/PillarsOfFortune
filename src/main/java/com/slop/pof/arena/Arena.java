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
    public final int centerX;
    public final int centerZ;
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

    public Arena(int id, int centerX, int centerZ) {
        this.id = id;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public int aliveCount() {
        return alive.size();
    }

    public Location centerAt(double y) {
        if (world == null) {
            return null;
        }
        return new Location(world, centerX, y, centerZ);
    }
}
