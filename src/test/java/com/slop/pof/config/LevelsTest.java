package com.slop.pof.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelsTest {
    private YamlConfiguration config() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("leveling.xp-per-win", 100);
        c.set("leveling.winstreak-step", 10);
        c.set("leveling.winstreak-cap", 100);
        c.set("leveling.rankup-base", 500);
        c.set("leveling.rankup-growth", 1.15);
        c.set("leveling.rankup-cap", 20000);
        c.set("leveling.rankups.1", 500);
        c.set("leveling.rankups.4-19", 2000);
        return c;
    }

    @Test
    void explicitStepsWinOverTheCurve() {
        Levels levels = Levels.from(config());
        assertEquals(500, levels.nextCost(1));
        assertEquals(2000, levels.nextCost(4));
        assertEquals(2000, levels.nextCost(19));
    }

    @Test
    void levelsGetHarderButAreCapped() {
        Levels levels = Levels.from(config());
        // Levels 2 and 3 are not in the table, so the curve applies: 500 * 1.15^(level-1).
        assertEquals(575, levels.nextCost(2));
        assertEquals(661, levels.nextCost(3));
        assertTrue(levels.nextCost(3) > levels.nextCost(2), "each level costs more than the last");
        assertTrue(levels.nextCost(20) > levels.nextCost(19), "still growing past the table");
        assertEquals(20000, levels.nextCost(100), "the cost stops at the cap");
        assertEquals(levels.nextCost(100), levels.nextCost(200));
    }

    @Test
    void rollCarriesSpillIntoTheNextLevel() {
        Levels levels = Levels.from(config());
        Levels.Roll roll = levels.roll(1, 400, 300);
        assertEquals(2, roll.level());
        assertEquals(200, roll.xp());
    }

    @Test
    void winstreakAddsXpForEachExtraWin() {
        Levels levels = Levels.from(config());
        assertEquals(100, levels.baseWinXp());
        assertEquals(100, levels.winXp(1));
        assertEquals(110, levels.winXp(2));
        assertEquals(120, levels.winXp(3));
        assertEquals(200, levels.winXp(11), "the bonus is capped at winstreak-cap");
    }

    @Test
    void progressAndBarTrackTheLevel() {
        Levels levels = Levels.from(config());
        assertEquals(0.5, levels.progress(1, 250), 0.001);
        assertEquals(0.0, levels.progress(1, 0), 0.001);
        assertTrue(levels.bar(1, 250).contains("|"));
    }
}
