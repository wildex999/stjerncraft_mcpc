package org.bukkit.craftbukkit.generator;

import java.util.List;
import java.util.Random;


import net.minecraft.entity.EnumCreatureType;

import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.craftbukkit.block.CraftBlock;

public class CustomChunkGenerator extends InternalChunkGenerator {
    private final ChunkGenerator generator;
    private final net.minecraft.world.WorldServer/*was:WorldServer*/ world;
    private final Random random;
    private final net.minecraft.world.gen.structure.MapGenStronghold/*was:WorldGenStronghold*/ strongholdGen = new net.minecraft.world.gen.structure.MapGenStronghold/*was:WorldGenStronghold*/();

    private static class CustomBiomeGrid implements BiomeGrid {
        net.minecraft.world.biome.BiomeGenBase/*was:BiomeBase*/[] biome;

        public Biome getBiome(int x, int z) {
            return CraftBlock.biomeBaseToBiome(biome[(z << 4) | x]);
        }

        public void setBiome(int x, int z, Biome bio) {
           biome[(z << 4) | x] = CraftBlock.biomeToBiomeBase(bio);
        }
    }

    public CustomChunkGenerator(net.minecraft.world.World/*was:World*/ world, long seed, ChunkGenerator generator) {
        this.world = (net.minecraft.world.WorldServer/*was:WorldServer*/) world;
        this.generator = generator;

        this.random = new Random(seed);
    }

    public boolean chunkExists/*was:isChunkLoaded*/(int x, int z) {
        return true;
    }

    public net.minecraft.world.chunk.Chunk/*was:Chunk*/ provideChunk/*was:getOrCreateChunk*/(int x, int z) {
        random.setSeed((long) x * 341873128712L + (long) z * 132897987541L);

        net.minecraft.world.chunk.Chunk/*was:Chunk*/ chunk;

        // Get default biome data for chunk
        CustomBiomeGrid biomegrid = new CustomBiomeGrid();
        biomegrid.biome = new net.minecraft.world.biome.BiomeGenBase/*was:BiomeBase*/[256];
        world.getWorldChunkManager/*was:getWorldChunkManager*/().loadBlockGeneratorData/*was:getBiomeBlock*/(biomegrid.biome, x << 4, z << 4, 16, 16);

        // Try extended block method (1.2+)
        short[][] xbtypes = generator.generateExtBlockSections(this.world.getWorld(), this.random, x, z, biomegrid);
        if (xbtypes != null) {
            chunk = new net.minecraft.world.chunk.Chunk/*was:Chunk*/(this.world, x, z);

            net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/[] csect = chunk.getBlockStorageArray/*was:i*/();
            int scnt = Math.min(csect.length, xbtypes.length);

            // Loop through returned sections
            for (int sec = 0; sec < scnt; sec++) {
                if (xbtypes[sec] == null) {
                    continue;
                }
                byte[] secBlkID = new byte[4096]; // Allocate blk ID bytes
                byte[] secExtBlkID = (byte[]) null; // Delay getting extended ID nibbles
                short[] bdata = xbtypes[sec];
                // Loop through data, 2 blocks at a time
                for (int i = 0, j = 0; i < bdata.length; i += 2, j++) {
                    short b1 = bdata[i];
                    short b2 = bdata[i + 1];
                    byte extb = (byte) ((b1 >> 8) | ((b2 >> 4) & 0xF0));

                    secBlkID[i] = (byte) b1;
                    secBlkID[(i + 1)] = (byte) b2;

                    if (extb != 0) { // If extended block ID data
                        if (secExtBlkID == null) { // Allocate if needed
                            secExtBlkID = new byte[2048];
                        }
                        secExtBlkID[j] = extb;
                    }
                }
                // Build chunk section
                csect[sec] = new net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/(sec << 4, true, secBlkID, secExtBlkID);
            }
        }
        else { // Else check for byte-per-block section data
            byte[][] btypes = generator.generateBlockSections(this.world.getWorld(), this.random, x, z, biomegrid);

            if (btypes != null) {
                chunk = new net.minecraft.world.chunk.Chunk/*was:Chunk*/(this.world, x, z);

                net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/[] csect = chunk.getBlockStorageArray/*was:i*/();
                int scnt = Math.min(csect.length, btypes.length);

                for (int sec = 0; sec < scnt; sec++) {
                    if (btypes[sec] == null) {
                        continue;
                    }
                    csect[sec] = new net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/(sec << 4, true, btypes[sec], null);
                }
            }
            else { // Else, fall back to pre 1.2 method
                @SuppressWarnings("deprecation")
                byte[] types = generator.generate(this.world.getWorld(), this.random, x, z);
                int ydim = types.length / 256;
                int scnt = ydim / 16;

                chunk = new net.minecraft.world.chunk.Chunk/*was:Chunk*/(this.world, x, z); // Create empty chunk

                net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/[] csect = chunk.getBlockStorageArray/*was:i*/();

                scnt = Math.min(scnt, csect.length);
                // Loop through sections
                for (int sec = 0; sec < scnt; sec++) {
                    net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/ cs = null; // Add sections when needed
                    byte[] csbytes = (byte[]) null;

                    for (int cy = 0; cy < 16; cy++) {
                        int cyoff = cy | (sec << 4);

                        for (int cx = 0; cx < 16; cx++) {
                            int cxyoff = (cx * ydim * 16) + cyoff;

                            for (int cz = 0; cz < 16; cz++) {
                                byte blk = types[cxyoff + (cz * ydim)];

                                if (blk != 0) { // If non-empty
                                    if (cs == null) { // If no section yet, get one
                                        cs = csect[sec] = new net.minecraft.world.chunk.storage.ExtendedBlockStorage/*was:ChunkSection*/(sec << 4, true);
                                        csbytes = cs.getBlockLSBArray/*was:g*/();
                                    }
                                    csbytes[(cy << 8) | (cz << 4) | cx] = blk;
                                }
                            }
                        }
                    }
                    // If section built, finish prepping its state
                    if (cs != null) {
                        cs.getYLocation/*was:d*/();
                    }
                }
            }
        }
        // Set biome grid
        byte[] biomeIndex = chunk.getBiomeArray/*was:m*/();
        for (int i = 0; i < biomeIndex.length; i++) {
            biomeIndex[i] = (byte) (biomegrid.biome[i].biomeID/*was:id*/ & 0xFF);
        }
        // Initialize lighting
        chunk.generateSkylightMap/*was:initLighting*/();

        return chunk;
    }

    public void populate/*was:getChunkAt*/(net.minecraft.world.chunk.IChunkProvider/*was:IChunkProvider*/ icp, int i, int i1) {
        // Nothing!
    }

    public boolean saveChunks/*was:saveChunks*/(boolean bln, net.minecraft.util.IProgressUpdate/*was:IProgressUpdate*/ ipu) {
        return true;
    }

    public boolean unload100OldestChunks/*was:unloadChunks*/() {
        return false;
    }

    public boolean canSave/*was:canSave*/() {
        return true;
    }

    @SuppressWarnings("deprecation")
    public byte[] generate(org.bukkit.World world, Random random, int x, int z) {
        return generator.generate(world, random, x, z);
    }

    public byte[][] generateBlockSections(org.bukkit.World world, Random random, int x, int z, BiomeGrid biomes) {
        return generator.generateBlockSections(world, random, x, z, biomes);
    }

    public short[][] generateExtBlockSections(org.bukkit.World world, Random random, int x, int z, BiomeGrid biomes) {
        return generator.generateExtBlockSections(world, random, x, z, biomes);
    }

    public net.minecraft.world.chunk.Chunk/*was:Chunk*/ loadChunk/*was:getChunkAt*/(int x, int z) {
        return provideChunk/*was:getOrCreateChunk*/(x, z);
    }

    @Override
    public boolean canSpawn(org.bukkit.World world, int x, int z) {
        return generator.canSpawn(world, x, z);
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(org.bukkit.World world) {
        return generator.getDefaultPopulators(world);
    }

    public List<?> getMobsFor(net.minecraft.entity.EnumCreatureType/*was:EnumCreatureType*/ type, int x, int y, int z) {
        net.minecraft.world.biome.BiomeGenBase/*was:BiomeBase*/ biomebase = world.getBiomeGenForCoords/*was:getBiome*/(x, z);

        return biomebase == null ? null : biomebase.getSpawnableList/*was:getMobs*/(type);
    }

    public net.minecraft.world.ChunkPosition/*was:ChunkPosition*/ findClosestStructure/*was:findNearestMapFeature*/(net.minecraft.world.World/*was:World*/ world, String type, int x, int y, int z) {
        return "Stronghold".equals(type) && this.strongholdGen != null ? this.strongholdGen.getNearestInstance/*was:getNearestGeneratedFeature*/(world, x, y, z) : null;
    }

    public void recreateStructures/*was:recreateStructures*/(int i, int j) {}

    public int getLoadedChunkCount/*was:getLoadedChunks*/() {
        return 0;
    }

    public String makeString/*was:getName*/() {
        return "CustomChunkGenerator";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
        // TODO Auto-generated method stub
        return null;
    }
}
