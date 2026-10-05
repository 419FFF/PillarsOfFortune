package com.slop.pof.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisibilityTest {
    @Test
    void cyclesAllThenQueueThenNone() {
        assertEquals(Visibility.QUEUE, Visibility.ALL.next());
        assertEquals(Visibility.NONE, Visibility.QUEUE.next());
        assertEquals(Visibility.ALL, Visibility.NONE.next());
    }

    @Test
    void parsesNamesWithAFallback() {
        assertEquals(Visibility.QUEUE, Visibility.of("queue", Visibility.ALL));
        assertEquals(Visibility.QUEUE, Visibility.of("QUEUE", Visibility.ALL));
        assertEquals(Visibility.ALL, Visibility.of("nonsense", Visibility.ALL));
        assertEquals(Visibility.ALL, Visibility.of(null, Visibility.ALL));
    }

    @Test
    void keysMatchTheMessageNames() {
        assertEquals("all", Visibility.ALL.key());
        assertEquals("queue", Visibility.QUEUE.key());
        assertEquals("none", Visibility.NONE.key());
    }
}
