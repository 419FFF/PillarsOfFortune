package com.slop.pof.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {
    @Test
    void boldGoesAfterALeadingColourCode() {
        // A colour code clears bold, so the bold code has to come after it.
        assertEquals("&6&lRush", Text.bold("&6Rush"));
        assertEquals("&c&lRush", Text.bold("&cRush"));
    }

    @Test
    void boldIsNotAddedTwice() {
        assertEquals("&6&lRush", Text.bold("&6&lRush"));
        assertEquals("&lRush", Text.bold("&lRush"));
    }

    @Test
    void boldWithoutAColourCode() {
        assertEquals("&lRush", Text.bold("Rush"));
        assertEquals("", Text.bold(null));
        assertEquals("", Text.bold(""));
    }
}
