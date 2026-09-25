package com.slop.pof.storage;

import java.util.UUID;

public final class Stats {
    public final UUID uuid;
    public String name;
    public int wins;
    public int kills;
    public int deaths;
    public int games;
    public int streak;
    public int bestStreak;
    public int items;
    public int playtimeMin;

    public Stats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
