package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamemodeTest {
    @Test
    void readsRandomItemsFlagAndStartKit() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("gamemodes.rush.enabled", true);
        yaml.set("gamemodes.rush.name", "&cRush");
        yaml.set("gamemodes.rush.icon", "DIAMOND_SWORD");
        yaml.set("gamemodes.rush.random-items", false);
        yaml.set("gamemodes.rush.start-items",
                List.of("DIAMOND_HELMET", "IRON_SWORD", "COBBLESTONE|amount:64"));
        yaml.set("gamemodes.rush.description", List.of("&7Pure PvP", "&7No random items"));

        ConfigurationSection section = yaml.getConfigurationSection("gamemodes.rush");
        Gamemode mode = Gamemode.read("rush", section);

        assertEquals("rush", mode.id);
        assertEquals("DIAMOND_SWORD", mode.icon);
        assertFalse(mode.randomItems());
        assertEquals(3, mode.startItems().size());
        assertTrue(mode.startItems().contains("COBBLESTONE|amount:64"));
        assertEquals(2, mode.description().size());
        assertEquals("&7Pure PvP", mode.description().get(0));
    }

    @Test
    void defaultsGiveRandomItemsAndNoKit() {
        Gamemode classic = Gamemode.classic();
        assertTrue(classic.randomItems());
        assertTrue(classic.startItems().isEmpty());
        assertTrue(classic.description().isEmpty());
        assertEquals("BEDROCK", classic.icon);
    }

    @Test
    void queueTimingsComeFromConfig() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("queue-broadcast-seconds", List.of(30, 20, 10, 5));

        YamlConfiguration user = new YamlConfiguration();
        user.setDefaults(defaults);
        Settings settings = Settings.from(user);

        assertTrue(settings.broadcastAt(30));
        assertTrue(settings.broadcastAt(5));
        assertFalse(settings.broadcastAt(17));
        assertEquals(20, settings.queueReminderSeconds());
    }
}
