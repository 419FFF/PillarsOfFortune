package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * One queueable ruleset. Blank overrides use the global config value.
 */
public final class Gamemode {
    public final String id;
    public final boolean enabled;
    public final String name;
    public final String icon;
    private final Integer minPlayers;
    private final Integer maxPlayers;
    private final Integer countdown;
    private final Integer queueSeconds;
    private final Integer itemDelaySeconds;

    public Gamemode(String id, boolean enabled, String name, String icon,
                    Integer minPlayers, Integer maxPlayers, Integer countdown,
                    Integer queueSeconds, Integer itemDelaySeconds) {
        this.id = id;
        this.enabled = enabled;
        this.name = name == null || name.isBlank() ? id : name;
        this.icon = icon == null || icon.isBlank() ? "NETHER_STAR" : icon;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.countdown = countdown;
        this.queueSeconds = queueSeconds;
        this.itemDelaySeconds = itemDelaySeconds;
    }

    public static Gamemode read(String id, ConfigurationSection section) {
        return new Gamemode(
                id,
                section.getBoolean("enabled", true),
                section.getString("name", id),
                section.getString("icon", "NETHER_STAR"),
                optionalInt(section, "min-players"),
                optionalInt(section, "max-players"),
                optionalInt(section, "countdown"),
                optionalInt(section, "queue-seconds"),
                optionalInt(section, "item-delay-seconds")
        );
    }

    /** Used when the config has no gamemodes section, so join still works with no menu. */
    public static Gamemode classic() {
        return new Gamemode("classic", true, "&6Classic", "NETHER_STAR", null, null, null, null, null);
    }

    public int minPlayers(Settings settings) {
        return minPlayers == null ? settings.minPlayers() : Math.max(1, minPlayers);
    }

    public int maxPlayers(Settings settings) {
        return maxPlayers == null ? settings.maxPlayers() : Math.max(minPlayers(settings), maxPlayers);
    }

    public int countdown(Settings settings) {
        return countdown == null ? settings.countdown() : Math.max(1, countdown);
    }

    public int queueSeconds(Settings settings) {
        return queueSeconds == null ? settings.queueSeconds() : Math.max(1, queueSeconds);
    }

    public int itemDelay(Settings settings) {
        return itemDelaySeconds == null ? settings.itemDelaySeconds() : Math.max(0, itemDelaySeconds);
    }

    private static Integer optionalInt(ConfigurationSection section, String path) {
        if (!section.contains(path)) {
            return null;
        }
        return section.getInt(path);
    }
}
