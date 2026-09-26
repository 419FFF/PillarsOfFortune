package com.slop.pof.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PillarSlotsTest {
    @Test
    void twoPlayersStandOpposite() {
        assertEquals(0, PillarSlots.slot(0, 2, 12));
        assertEquals(6, PillarSlots.slot(1, 2, 12));
    }

    @Test
    void threePlayersLeaveGaps() {
        assertEquals(0, PillarSlots.slot(0, 3, 12));
        assertEquals(4, PillarSlots.slot(1, 3, 12));
        assertEquals(8, PillarSlots.slot(2, 3, 12));
    }

    @Test
    void sixPlayersSkipNeighbors() {
        int previous = -2;
        for (int i = 0; i < 6; i++) {
            int slot = PillarSlots.slot(i, 6, 12);
            assertTrue(slot - previous >= 2);
            previous = slot;
        }
    }

    @Test
    void fullQueueUsesEveryPillar() {
        boolean[] used = new boolean[12];
        for (int i = 0; i < 12; i++) {
            int slot = PillarSlots.slot(i, 12, 12);
            assertTrue(slot >= 0 && slot < 12);
            used[slot] = true;
        }
        for (boolean seat : used) {
            assertTrue(seat);
        }
    }
}
