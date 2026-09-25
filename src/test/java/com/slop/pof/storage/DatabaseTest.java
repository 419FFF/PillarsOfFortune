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

        Stats stats = db.get(winner);
        assertEquals(2, stats.wins);
        assertEquals(0, stats.streak);
        assertEquals(2, stats.bestStreak);
        assertEquals(1, stats.deaths);

        List<TopEntry> top = db.top(10);
        assertEquals(2, top.size());
        assertEquals("OfflineGuy", top.get(0).name);
        assertEquals(2, top.get(0).wins);
        assertEquals("Aaa", top.get(1).name);
        db.close();

        Database again = open();
        List<TopEntry> still = again.top(10);
        assertEquals("OfflineGuy", still.get(0).name);
        assertEquals(2, still.get(0).wins);
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
