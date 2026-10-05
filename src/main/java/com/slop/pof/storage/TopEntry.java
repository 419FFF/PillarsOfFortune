package com.slop.pof.storage;

/** One leaderboard row. {@code value} is the score for the board it came from. */
public final class TopEntry {
    public final String name;
    public final int value;

    public TopEntry(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
