package com.slop.pof.arena;

/**
 * Places arena ids on a square spiral so they spread on both X and Z,
 * and rejects spots that would leave the vanilla 1.8 world.
 */
public final class ArenaLayout {
    /** Vanilla 1.8 coordinates stop just inside ±30,000,000. */
    public static final int WORLD_EDGE = 29_999_872;

    private ArenaLayout() {
    }

    /** Grid steps from the origin. Index 0 is (0, 0). Later steps walk +X, +Z, -X, and -Z. */
    public static int[] steps(int index) {
        int x = 0;
        int z = 0;
        if (index <= 0) {
            return new int[]{0, 0};
        }
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        int direction = 0;
        int segment = 1;
        int walked = 0;
        while (walked < index) {
            for (int turn = 0; turn < 2 && walked < index; turn++) {
                for (int step = 0; step < segment && walked < index; step++) {
                    x += directions[direction][0];
                    z += directions[direction][1];
                    walked++;
                }
                direction = (direction + 1) % 4;
            }
            segment++;
        }
        return new int[]{x, z};
    }

    public static boolean inside(int x, int z, int radius) {
        return Math.abs((long) x) + radius < WORLD_EDGE && Math.abs((long) z) + radius < WORLD_EDGE;
    }
}
