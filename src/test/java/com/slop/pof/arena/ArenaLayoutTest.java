package com.slop.pof.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLayoutTest {
    @Test
    void spiralUsesPositiveAndNegativeXAndZ() {
        boolean posX = false;
        boolean negX = false;
        boolean posZ = false;
        boolean negZ = false;
        for (int i = 0; i < 40; i++) {
            int[] step = ArenaLayout.steps(i);
            if (step[0] > 0) {
                posX = true;
            }
            if (step[0] < 0) {
                negX = true;
            }
            if (step[1] > 0) {
                posZ = true;
            }
            if (step[1] < 0) {
                negZ = true;
            }
        }
        assertTrue(posX && negX && posZ && negZ);
    }

    @Test
    void defaultGridStaysInsideTheWorld() {
        int spacing = 2000;
        int radius = 160;
        for (int i = 0; i < 400; i++) {
            int[] step = ArenaLayout.steps(i);
            assertTrue(ArenaLayout.inside(step[0] * spacing, step[1] * spacing, radius));
        }
    }

    /**
     * Every arena id must land on its own spot. A repeated center is what made the arena list
     * look like it was duplicating arenas.
     */
    @Test
    void arenaCentersAreUnique() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int index = 0; index < 64; index++) {
            int[] step = ArenaLayout.steps(index);
            assertTrue(seen.add(step[0] + "," + step[1]), "duplicate center at index " + index);
        }
    }

    @Test
    void rejectsSpotsPastTheVanillaEdge() {
        assertFalse(ArenaLayout.inside(29_999_000, 0, 2000));
        assertFalse(ArenaLayout.inside(0, -29_999_000, 2000));
    }
}
