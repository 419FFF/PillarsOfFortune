package com.slop.pof.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Empty chunks. No terrain, villages, or spawn island.
 */
public final class VoidChunkGenerator extends ChunkGenerator {
    @Override
    public byte[] generate(World world, Random random, int x, int z) {
        return new byte[32768];
    }

    @Override
    public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
        paint(biomes);
        int sections = Math.max(1, world.getMaxHeight() / 16);
        return new byte[sections][];
    }

    @Override
    public short[][] generateExtBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
        paint(biomes);
        int sections = Math.max(1, world.getMaxHeight() / 16);
        return new short[sections][];
    }

    private static void paint(BiomeGrid biomes) {
        if (biomes == null) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                biomes.setBiome(x, z, Biome.PLAINS);
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.emptyList();
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 64, 0.5);
    }
}
