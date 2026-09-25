package com.slop.pof.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RollsTest {
    @Test
    void blockAmountsMatchSkriptTable() {
        assertEquals(4, Rolls.blockAmount(1, 4, 12));
        assertEquals(4, Rolls.blockAmount(8, 4, 12));
        assertEquals(5, Rolls.blockAmount(9, 4, 12));
        assertEquals(8, Rolls.blockAmount(58, 4, 12));
        assertEquals(9, Rolls.blockAmount(59, 4, 12));
        assertEquals(12, Rolls.blockAmount(94, 4, 12));
        assertEquals(12, Rolls.blockAmount(100, 4, 12));
    }

    @Test
    void poolsUseSkriptWeights() {
        assertEquals(0, Rolls.poolIndex(8, 8, 7, 72));
        assertEquals(1, Rolls.poolIndex(9, 8, 7, 72));
        assertEquals(1, Rolls.poolIndex(15, 8, 7, 72));
        assertEquals(2, Rolls.poolIndex(16, 8, 7, 72));
        assertEquals(2, Rolls.poolIndex(87, 8, 7, 72));
        assertEquals(3, Rolls.poolIndex(88, 8, 7, 72));
    }
}
