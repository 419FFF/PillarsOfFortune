package com.slop.pof.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCopyTest {
    @Test
    void nestedGamemodeIsNotSavedAsAnEmptyMap() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("gamemodes.classic.enabled", true);
        defaults.set("gamemodes.classic.name", "&6Classic");
        defaults.set("gamemodes.classic.icon", "NETHER_STAR");
        defaults.set("gamemodes.custom.enabled", false);

        YamlConfiguration config = new YamlConfiguration();
        ConfigUpdater.copy(config, "gamemodes", defaults.get("gamemodes"));

        assertTrue(config.getBoolean("gamemodes.classic.enabled"));
        assertEquals("&6Classic", config.getString("gamemodes.classic.name"));
        assertEquals("NETHER_STAR", config.getString("gamemodes.classic.icon"));
        assertFalse(config.getBoolean("gamemodes.custom.enabled"));
        assertFalse(config.saveToString().contains("{}"));
    }
}
