package com.slop.pof.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountsTest {
    @Test
    void parsesFixedAndRanges() {
        assertEquals(4, Amounts.parse("4")[0]);
        assertEquals(4, Amounts.parse("4")[1]);
        assertEquals(1, Amounts.parse("1-2")[0]);
        assertEquals(2, Amounts.parse("1-2")[1]);
        assertEquals(4, Amounts.parse("12-4")[0]);
        assertEquals(12, Amounts.parse("12-4")[1]);
        assertEquals(1, Amounts.parse("1..3")[0]);
        assertEquals(3, Amounts.parse("1..3")[1]);
    }

    @Test
    void rollStaysInsideTheRange() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 30; i++) {
            int rolled = Amounts.roll("2-5", random);
            assertTrue(rolled >= 2 && rolled <= 5);
        }
    }
}
