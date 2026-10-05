package com.slop.pof.config;

import java.util.function.Function;

/**
 * Sidebar text for one mode. Old configs stored each line under its own key.
 * Those keys are folded into {@code board-lobby}, {@code board-queued}, and the match layouts.
 */
public final class BoardLayouts {
    public static final String[] MODES = {"lobby", "queued", "starting", "grace", "ingame", "ending"};

    /** Per-line keys from before the six layout strings. {@code board-start} is not in this list. */
    public static final String[] LEGACY = {
            "board-title-lobby", "board-title-starting", "board-title-finished", "board-title-fortune",
            "board-join", "board-wins", "board-kills", "board-streak", "board-queue",
            "board-arena", "board-starts", "board-alive", "board-item", "board-time", "board-grace",
            "board-pad", "board-ip"
    };

    private BoardLayouts() {
    }

    public static boolean hasLegacy(Function<String, String> messages) {
        for (String key : LEGACY) {
            String value = messages.apply(key);
            if (value != null && !value.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Layout built from the old per-line keys. Missing lines use the original wording. */
    public static String compose(String mode, Function<String, String> messages) {
        String pad = line(messages, "board-pad", "&8");
        String ip = line(messages, "board-ip", "&e{ip}");
        String titleLobby = line(messages, "board-title-lobby", "&6&lPILLARS");
        String titleStart = line(messages, "board-title-starting", "&6&lSTARTING");
        String titleEnd = line(messages, "board-title-finished", "&6&lFINISHED");
        String titleLive = line(messages, "board-title-fortune", "&6&lFORTUNE");
        String arena = token(line(messages, "board-arena", "&fArena: &e{value}"), "value", "id");
        String alive = token(line(messages, "board-alive", "&fAlive: &e{value}"), "value", "alive");
        String sessionKills = token(line(messages, "board-kills", "&fKills: &e{value}"), "value", "session_kills");
        return switch (mode) {
            case "queued" -> join(
                    titleLobby,
                    "{start}",
                    token(line(messages, "board-queue", "&fQueue: &e{count}&7/&f{max}"), "count", "queue"),
                    pad,
                    ip);
            case "starting" -> join(
                    titleStart,
                    arena,
                    token(line(messages, "board-starts", "&fStarts: &e{value}s"), "value", "starts"),
                    alive,
                    pad,
                    ip);
            case "grace" -> join(
                    titleLive,
                    arena,
                    alive,
                    sessionKills,
                    token(line(messages, "board-item", "&fItem: &e{value}s"), "value", "item"),
                    token(line(messages, "board-time", "&fTime: &e{value}s"), "value", "time"),
                    token(line(messages, "board-grace", "&aGrace: &f{value}s"), "value", "grace"),
                    pad,
                    ip);
            case "ingame" -> join(
                    titleLive,
                    arena,
                    alive,
                    sessionKills,
                    token(line(messages, "board-item", "&fItem: &e{value}s"), "value", "item"),
                    token(line(messages, "board-time", "&fTime: &e{value}s"), "value", "time"),
                    pad,
                    ip);
            case "ending" -> join(
                    titleEnd,
                    arena,
                    alive,
                    sessionKills,
                    pad,
                    ip);
            default -> join(
                    titleLobby,
                    line(messages, "board-join", "&6/pof join"),
                    token(line(messages, "board-wins", "&fWins: &e{value}"), "value", "wins"),
                    token(line(messages, "board-kills", "&fKills: &e{value}"), "value", "kills"),
                    token(line(messages, "board-streak", "&fStreak: &e{value}"), "value", "streak"),
                    pad,
                    ip);
        };
    }

    /** Layout used when the file has neither a board string nor the old per-line keys. */
    public static String builtin(String mode) {
        String head = "{title}\n&7{date}";
        String tail = "&fLevel: {level_name} {progress}\n&e{ip}";
        return switch (mode) {
            case "queued" -> head + "\n{start}\n&fQueue: &e{queue}&7/&f{max}\n" + tail;
            case "starting" -> "&6&lSTARTING\n&7{date}\n&fArena: &e{id}\n&fStarts: &e{starts}s\n&fAlive: &e{alive}\n" + tail;
            case "grace" -> "{title}\n&7{date}\n&fArena: &e{id}\n&fAlive: &e{alive}\n&fKills: &e{session_kills}\n&fItem: &e{item}s\n&fTime: &e{time}s\n&aGrace: &f{grace}s\n" + tail;
            case "ingame" -> "{title}\n&7{date}\n&fArena: &e{id}\n&fAlive: &e{alive}\n&fKills: &e{session_kills}\n&fItem: &e{item}s\n&fTime: &e{time}s\n" + tail;
            case "ending" -> "{title}\n&7{date}\n&fArena: &e{id}\n&fAlive: &e{alive}\n&fKills: &e{session_kills}\n" + tail;
            default -> head + "\n&fWins: &e{wins}\n&fKills: &e{kills}\n&fStreak: &e{streak}\n" + tail;
        };
    }

    public static boolean same(String left, String right) {
        return norm(left).equals(norm(right));
    }

    public static String norm(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace("\\n", "\n");
    }

    private static String join(String... lines) {
        return String.join("\n", lines);
    }

    private static String line(Function<String, String> messages, String key, String fallback) {
        String value = messages.apply(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String token(String text, String from, String to) {
        return text.replace("{" + from + "}", "{" + to + "}");
    }
}
