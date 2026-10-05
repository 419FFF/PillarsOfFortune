package com.slop.pof.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the config.yml bundled in the jar, so the shipped defaults cannot silently regress. */
class BoardDefaultsTest {
    private static YamlConfiguration shipped() {
        try (InputStream in = BoardDefaultsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(in, "config.yml must be on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void everyBoardHasTitleDateLevelAndBarAboveTheIp() {
        YamlConfiguration yaml = shipped();
        for (String mode : BoardLayouts.MODES) {
            String layout = yaml.getString("messages.board-" + mode);
            assertNotNull(layout, "missing messages.board-" + mode);
            String[] lines = layout.replace("\\n", "\n").split("\n", -1);
            assertEquals("{title}", lines[0], mode + " should start with {title}");
            assertTrue(lines[1].contains("{date}"), mode + " should show the date under the title");
            assertTrue(layout.contains("{level_name}"), mode + " should show the level");
            assertTrue(lines[lines.length - 2].contains("{progress}"), mode + " should put the bar above the ip");
            assertTrue(lines[lines.length - 2].contains("{level_name}"),
                    mode + " should keep the level next to the bar");
            assertTrue(lines[lines.length - 1].contains("{ip}"), mode + " should end with the ip");
        }
    }

    @Test
    void levelingVisibilityAndHubDefaults() {
        YamlConfiguration yaml = shipped();
        assertTrue(yaml.getBoolean("leveling.enabled"));
        assertEquals(500, yaml.getInt("leveling.rankup-base"));
        assertTrue(yaml.getDouble("leveling.rankup-growth") > 1.0D, "each level must cost more than the last");
        assertTrue(yaml.getInt("leveling.rankup-cap") > yaml.getInt("leveling.rankup-base"),
                "the cost must be capped so it stays possible");
        assertEquals(10, yaml.getInt("leveling.winstreak-step"));
        assertEquals(100, yaml.getInt("leveling.winstreak-cap"));
        assertTrue(yaml.getBoolean("visibility.show-all-by-default"));
        assertEquals(7, yaml.getInt("visibility.slot"));
        assertFalse(yaml.getBoolean("lobby-items.hub-enabled"), "the hub bed is off by default");
        assertEquals("hub", yaml.getString("lobby-items.hub-command"));
    }
}
