package com.slop.pof.config;

import java.util.Locale;

/** Lobby visibility states, cycled by the lobby dye item. */
public enum Visibility {
    /** Show everyone in the lobby. */
    ALL,
    /** Show only players in the same queue. */
    QUEUE,
    /** Hide every other player. */
    NONE;

    public Visibility next() {
        return switch (this) {
            case ALL -> QUEUE;
            case QUEUE -> NONE;
            case NONE -> ALL;
        };
    }

    /** Lower-case name, used for config keys such as {@code visibility-name-all}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Visibility of(String raw, Visibility fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
