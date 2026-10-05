package com.slop.pof.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The commented config.yml from the jar must survive a rewrite: the notes stay and the admin's
 * values win. This is what stopped the notes from disappearing after an update.
 */
class ConfigLayoutTest {
    private static final String TEMPLATE = String.join("\n",
            "# Pillars of Fortune",
            "# Leading note.",
            "prefix: \"&8[&6PoF&8]&r\"",
            "# Note above the section.",
            "arenas:",
            "  # How many arenas.",
            "  count: 6",
            "  spacing: 2000",
            "");

    @Test
    void mergeKeepsCommentsAndUsesUserValues() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("prefix", "&cCustom");
        user.set("arenas.count", 9);

        String merged = ConfigLayout.merge(TEMPLATE, user);

        assertTrue(merged.contains("# Pillars of Fortune"), merged);
        assertTrue(merged.contains("# Note above the section."), merged);
        assertTrue(merged.contains("# How many arenas."), merged);
        assertTrue(merged.contains("&cCustom"), merged);
        assertTrue(merged.contains("count: 9"), merged);
        assertTrue(merged.contains("spacing: 2000"), merged);
    }

    @Test
    void commentsAreDetectedAsMissing() {
        String stripped = "prefix: \"&8[&6PoF&8]&r\"\narenas:\n  count: 6\n  spacing: 2000\n";
        assertTrue(ConfigLayout.needsCommentRestore(stripped, TEMPLATE));
        assertFalse(ConfigLayout.needsCommentRestore(TEMPLATE, TEMPLATE));
    }
}
