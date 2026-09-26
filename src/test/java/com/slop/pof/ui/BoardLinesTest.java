package com.slop.pof.ui;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardLinesTest {
    @Test
    void newlineSplitsTitleFromBody() {
        String[] rows = Boards.sidebarLines("&6&lPILLARS\n&6/pof join\n&8\n&e{ip}");
        assertEquals(4, rows.length);
        assertEquals("&6&lPILLARS", rows[0]);
        assertEquals("&6/pof join", rows[1]);
        assertEquals("&8", rows[2]);
        assertEquals("&e{ip}", rows[3]);
    }

    @Test
    void realLineBreaksAndBlankRows() {
        String[] rows = Boards.sidebarLines("TITLE\n\nsecond");
        assertEquals("TITLE", rows[0]);
        assertEquals(ChatColor.RESET.toString(), rows[1]);
        assertEquals("second", rows[2]);
    }

    @Test
    void titleIsCappedAndExtraLinesDrop() {
        StringBuilder text = new StringBuilder("123456789012345678901234567890EXTRA");
        for (int i = 0; i < 20; i++) {
            text.append('\n').append("line").append(i);
        }
        String[] rows = Boards.sidebarLines(text.toString());
        assertEquals(16, rows.length);
        assertEquals(32, rows[0].length());
        assertTrue(rows[0].startsWith("123456789012345678901234567890"));
        assertEquals("line14", rows[15]);
    }
}
