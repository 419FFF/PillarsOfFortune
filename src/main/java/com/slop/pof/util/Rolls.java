package com.slop.pof.util;

/**
 * Weighted rolls copied from the Skript ({@code pofBlockAmount} and {@code pofGiveRandom}).
 */
public final class Rolls {
    private static final int[] BLOCK_CUTS = {8, 18, 30, 44, 58, 72, 84, 93};

    private Rolls() {
    }

    /** {@code percentile} is 1..100. Defaults min=4 max=12 match the Skript table exactly. */
    public static int blockAmount(int percentile, int min, int max) {
        int bucket = BLOCK_CUTS.length;
        for (int i = 0; i < BLOCK_CUTS.length; i++) {
            if (percentile <= BLOCK_CUTS[i]) {
                bucket = i;
                break;
            }
        }
        if (max <= min) {
            return min;
        }
        if (min == 4 && max == 12) {
            return min + bucket;
        }
        return min + (int) Math.round(bucket * ((max - min) / 8.0));
    }

    /** 0 weapons, 1 armor, 2 blocks, 3 chaos. */
    public static int poolIndex(int roll, int weapons, int armor, int blocks) {
        if (roll <= weapons) {
            return 0;
        }
        if (roll <= weapons + armor) {
            return 1;
        }
        if (roll <= weapons + armor + blocks) {
            return 2;
        }
        return 3;
    }
}
