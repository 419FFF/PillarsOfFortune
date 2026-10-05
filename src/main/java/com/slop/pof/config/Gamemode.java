package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

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
    private final Boolean randomItems;
    private final List<String> startItems;
    private final List<String> description;

    public Gamemode(String id, boolean enabled, String name, String icon,
                    Integer minPlayers, Integer maxPlayers, Integer countdown,
                    Integer queueSeconds, Integer itemDelaySeconds,
                    Boolean randomItems, List<String> startItems, List<String> description) {
        this.id = id;
        this.enabled = enabled;
        this.name = name == null || name.isBlank() ? id : name;
        this.icon = icon == null || icon.isBlank() ? "NETHER_STAR" : icon;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.countdown = countdown;
        this.queueSeconds = queueSeconds;
        this.itemDelaySeconds = itemDelaySeconds;
        this.randomItems = randomItems;
        this.startItems = startItems == null ? List.of() : List.copyOf(startItems);
        this.description = description == null ? List.of() : List.copyOf(description);
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
                optionalInt(section, "item-delay-seconds"),
                optionalBool(section, "random-items"),
                section.getStringList("start-items"),
                section.getStringList("description")
        );
    }

    /** Used when the config has no gamemodes section, so join still works with no menu. */
    public static Gamemode classic() {
        return new Gamemode("classic", true, "&6Classic", "BEDROCK", null, null, null, null, null,
                null, List.of(), List.of());
    }

    /** True when random item drops are given. A mode with a start kit usually turns these off. */
    public boolean randomItems() {
        return randomItems == null || randomItems;
    }

    /** The kit given when the cages close. Empty means the mode relies on random drops. */
    public List<String> startItems() {
        return startItems;
    }

    /** The GUI lore shown in the mode menu, straight from config. */
    public List<String> description() {
        return description;
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

    private static Boolean optionalBool(ConfigurationSection section, String path) {
        if (!section.contains(path)) {
            return null;
        }
        return section.getBoolean(path);
    }
}
