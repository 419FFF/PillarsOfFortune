package com.slop.pof.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sidebar layout resolution for an older config that still uses the per-line {@code board-*} keys.
 * These are the keys from before {@code board-lobby}/{@code board-queued}/... existed.
 */
class BoardConfigTest {
    private static YamlConfiguration jarDefaults() {
        YamlConfiguration jar = new YamlConfiguration();
        jar.set("messages.board-lobby",
                "&6&lPILLARS\n&6/pof join\n&fWins: &e{wins}\n&8\n&e{ip}");
        jar.set("messages.board-queued",
                "&6&lPILLARS\n{start}\n&fQueue: &e{queue}&7/&f{max}\n&8\n&e{ip}");
        return jar;
    }

    @Test
    void shippedLayoutWinsWhenOnlyLegacyKeysExist() {
        YamlConfiguration user = new YamlConfiguration();
        user.setDefaults(jarDefaults());
        // An old config has no board-lobby, only the per-line keys it was written with.
        user.set("messages.board-title-lobby", "&a&lMY TITLE");
        user.set("messages.board-ip", "&7my.server.net");

        Settings settings = Settings.from(user);
        String rendered = settings.board(null, "lobby");

        // The layout string wins over the old per-line keys: the composed legacy form has no
        // {title}, {level_name} or bar, so it must not be used.
        assertTrue(rendered.contains("/pof join"), rendered);
        assertTrue(rendered.contains("Wins"), rendered);
        assertFalse(rendered.contains("MY TITLE"), rendered);
    }

    @Test
    void editedBoardStringWinsOverLegacyKeys() {
        YamlConfiguration user = new YamlConfiguration();
        user.setDefaults(jarDefaults());
        user.set("messages.board-lobby", "&b&lCUSTOM\n&fWins: &e{wins}");
        user.set("messages.board-title-lobby", "&a&lMY TITLE");

        Settings settings = Settings.from(user);
        String rendered = settings.board(null, "lobby");

        assertTrue(rendered.contains("CUSTOM"), rendered);
        assertFalse(rendered.contains("MY TITLE"), rendered);
    }
}
