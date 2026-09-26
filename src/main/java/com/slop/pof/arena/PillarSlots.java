package com.slop.pof.arena;

/**
 * Picks pillar indexes so a short queue does not stand on neighboring pillars.
 * Index 0 is the first pillar. The result is unique while {@code players <= pillars}.
 */
public final class PillarSlots {
    private PillarSlots() {
    }

    public static int slot(int index, int players, int pillars) {
        if (pillars <= 1 || players <= 1) {
            return 0;
        }
        int count = Math.min(players, pillars);
        int safeIndex = Math.max(0, Math.min(index, count - 1));
        return (safeIndex * pillars) / count;
    }
}
