package com.slop.pof.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {
    @TempDir
    Path temp;

    @Test
    void offlinePlayerAppearsOnTopAndSurvivesReopen() {
        Database db = open();
        UUID winner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        db.ensure(winner, "OfflineGuy");
        db.addWin(winner);
        db.addWin(winner);
        db.addDeath(winner);
        db.ensure(other, "Aaa");
        db.addWin(other);
        db.addKill(other);
        db.addKill(other);

        Stats stats = db.get(winner);
        assertEquals(2, stats.wins);
        assertEquals(0, stats.streak);
        assertEquals(2, stats.bestStreak);
        assertEquals(1, stats.deaths);
        assertEquals(1, stats.level, "levels start at 1");
        assertEquals(0, stats.xp);

        List<TopEntry> top = db.top(10);
        assertEquals(2, top.size());
        assertEquals("OfflineGuy", top.get(0).name);
        assertEquals(2, top.get(0).value);
        assertEquals("Aaa", top.get(1).name);

        List<TopEntry> byKills = db.top(Leaderboard.KILLS, 10);
        assertEquals("Aaa", byKills.get(0).name);
        assertEquals(2, byKills.get(0).value);

        List<TopEntry> byStreak = db.top(Leaderboard.STREAK, 10);
        assertEquals("OfflineGuy", byStreak.get(0).name);
        assertEquals(2, byStreak.get(0).value);

        db.setLevelXp(other, 7, 0);
        List<TopEntry> byLevel = db.top(Leaderboard.LEVEL, 10);
        assertEquals("Aaa", byLevel.get(0).name);
        assertEquals(7, byLevel.get(0).value);
        db.close();

        Database again = open();
        List<TopEntry> still = again.top(10);
        assertEquals("OfflineGuy", still.get(0).name);
        assertEquals(2, still.get(0).value);
        assertTrue(again.findByName("offlineguy").wins == 2);
        again.ensure(winner, "OfflineGuy");
        assertEquals(2, again.get(winner).wins);
        again.close();
    }

    private Database open() {
        Database db = new Database(temp.toFile(), "h2", "data", "127.0.0.1", 3306, "minecraft", "root", "", null);
        db.start();
        return db;
    }
}
