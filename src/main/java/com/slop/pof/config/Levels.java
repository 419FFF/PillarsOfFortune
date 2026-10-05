package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Leveling curve and XP rewards, modeled on BedWars1058's levels config.
 * An exact {@code leveling.rankups.<n>} entry wins over a {@code <from>-<to>} range,
 * and {@code leveling.rankups.others} is the fallback.
 */
public final class Levels {
    /** The level, the xp left inside it, and the xp that was actually added. */
    public record Roll(int level, int xp, int gained) {
    }

    private record Step(int from, int to, int cost) {
    }

    private final boolean enabled;
    private final String levelFormat;
    private final int xpPerWin;
    private final int xpPerKill;
    private final int xpPerGame;
    private final int xpPerMinute;
    private final int winstreakStep;
    private final int winstreakCap;
    private final int rankupBase;
    private final double rankupGrowth;
    private final int rankupCap;
    private final int barLength;
    private final String barSymbol;
    private final String barUnlocked;
    private final String barLocked;
    private final List<Step> steps;

    private Levels(FileConfiguration c) {
        enabled = c.getBoolean("leveling.enabled", true);
        levelFormat = c.getString("leveling.level-format", "&e{level}");
        xpPerWin = Math.max(0, c.getInt("leveling.xp-per-win", 100));
        xpPerKill = Math.max(0, c.getInt("leveling.xp-per-kill", 10));
        xpPerGame = Math.max(0, c.getInt("leveling.xp-per-game", 5));
        xpPerMinute = Math.max(0, c.getInt("leveling.xp-per-minute", 2));
        winstreakStep = Math.max(0, c.getInt("leveling.winstreak-step", 10));
        winstreakCap = Math.max(0, c.getInt("leveling.winstreak-cap", 100));
        rankupBase = Math.max(1, c.getInt("leveling.rankup-base", 500));
        rankupGrowth = Math.max(1.0D, c.getDouble("leveling.rankup-growth", 1.15D));
        rankupCap = Math.max(rankupBase, c.getInt("leveling.rankup-cap", 20000));
        barLength = Math.max(1, Math.min(40, c.getInt("leveling.progress-bar.length", 20)));
        barSymbol = c.getString("leveling.progress-bar.symbol", "|");
        barUnlocked = c.getString("leveling.progress-bar.unlocked-color", "&b");
        barLocked = c.getString("leveling.progress-bar.locked-color", "&7");
        steps = readSteps(c.getConfigurationSection("leveling.rankups"));
    }

    public static Levels from(FileConfiguration c) {
        return new Levels(c);
    }

    public boolean enabled() {
        return enabled;
    }

    public int xpPerKill() {
        return xpPerKill;
    }

    public int xpPerGame() {
        return xpPerGame;
    }

    public int xpPerMinute() {
        return xpPerMinute;
    }

    /** XP a win is worth. A longer winstreak adds {@code winstreak-step} each time, up to the cap. */
    public int winXp(int streak) {
        int extra = winstreakStep * Math.max(0, Math.max(1, streak) - 1);
        return xpPerWin + Math.min(winstreakCap, extra);
    }

    /** XP a win is worth before any winstreak bonus. */
    public int baseWinXp() {
        return xpPerWin;
    }

    /**
     * XP needed to leave this level. An explicit {@code rankups} entry wins, then a range, and
     * otherwise each level costs {@code rankup-growth} times the previous one, capped so the curve
     * keeps getting harder without ever becoming impossible.
     */
    public int nextCost(int level) {
        int safe = Math.max(1, level);
        for (Step step : steps) {
            if (step.from == step.to && step.from == safe) {
                return step.cost;
            }
        }
        for (Step step : steps) {
            if (step.from != step.to && safe >= step.from && safe <= step.to) {
                return step.cost;
            }
        }
        double grown = rankupBase * Math.pow(rankupGrowth, safe - 1);
        if (grown >= rankupCap) {
            return rankupCap;
        }
        return (int) Math.max(1L, Math.round(grown));
    }

    /** The level shown in the sidebar, with the format applied. Colors are left for the caller. */
    public String name(int level) {
        return levelFormat.replace("{level}", String.valueOf(Math.max(1, level)));
    }

    public double progress(int level, int xp) {
        int cost = nextCost(level);
        if (cost <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) Math.max(0, xp) / cost));
    }

    /** A solid progress bar for the sidebar, already colored with the configured codes. */
    public String bar(int level, int xp) {
        int filled = (int) Math.round(progress(level, xp) * barLength);
        StringBuilder out = new StringBuilder(barUnlocked);
        for (int i = 0; i < barLength; i++) {
            if (i == filled) {
                out.append(barLocked);
            }
            out.append(barSymbol);
        }
        return out.toString();
    }

    /** Adds XP and rolls any spill into the next level, like BedWars1058's upgradeLevel. */
    public Roll roll(int level, int xp, int gained) {
        int nowLevel = Math.max(1, level);
        int nowXp = Math.max(0, xp) + Math.max(0, gained);
        int guard = 0;
        while (nowXp >= nextCost(nowLevel) && guard++ < 100_000) {
            nowXp -= nextCost(nowLevel);
            nowLevel++;
        }
        return new Roll(nowLevel, nowXp, Math.max(0, gained));
    }

    private static List<Step> readSteps(ConfigurationSection section) {
        List<Step> steps = new ArrayList<>();
        if (section == null) {
            return steps;
        }
        for (String key : section.getKeys(false)) {
            if ("others".equalsIgnoreCase(key)) {
                continue;
            }
            int[] range = parseRange(key);
            if (range == null) {
                continue;
            }
            steps.add(new Step(range[0], range[1], Math.max(1, section.getInt(key))));
        }
        return steps;
    }

    private static int[] parseRange(String key) {
        String trimmed = key.trim();
        int dash = trimmed.indexOf('-');
        try {
            if (dash < 0) {
                int level = Integer.parseInt(trimmed);
                return new int[]{level, level};
            }
            int from = Integer.parseInt(trimmed.substring(0, dash).trim());
            int to = Integer.parseInt(trimmed.substring(dash + 1).trim());
            return from <= to ? new int[]{from, to} : new int[]{to, from};
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
