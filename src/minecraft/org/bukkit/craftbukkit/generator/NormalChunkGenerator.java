package org.bukkit.craftbukkit.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.chunk.IChunkProvider; // MCPC+

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;

public class NormalChunkGenerator extends InternalChunkGenerator {
    private final net.minecraft.world.chunk.IChunkProvider provider;

    public NormalChunkGenerator(net.minecraft.world.World world, long seed) {
        provider = world.provider.createChunkGenerator();
    }

    public byte[] generate(org.bukkit.World world, Random random, int x, int z) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public boolean canSpawn(org.bukkit.World world, int x, int z) {
        return ((CraftWorld) world).getHandle().provider.canCoordinateBeSpawn(x, z);
    }

    public List<BlockPopulator> getDefaultPopulators(org.bukkit.World world) {
        return new ArrayList<BlockPopulator>();
    }

    public boolean chunkExists(int i, int i1) {
        return provider.chunkExists(i, i1);
    }

    public net.minecraft.world.chunk.Chunk provideChunk(int i, int i1) {
        return provider.provideChunk(i, i1);
    }

    public net.minecraft.world.chunk.Chunk loadChunk(int i, int i1) {
        return provider.loadChunk(i, i1);
    }

    public void populate(net.minecraft.world.chunk.IChunkProvider icp, int i, int i1) {
        provider.populate(icp, i, i1);
    }

    public boolean saveChunks(boolean bln, net.minecraft.util.IProgressUpdate ipu) {
        return provider.saveChunks(bln, ipu);
    }

    public boolean unload100OldestChunks() {
        return provider.unload100OldestChunks();
    }

    public boolean canSave() {
        return provider.canSave();
    }

    public List<?> getMobsFor(net.minecraft.entity.EnumCreatureType ect, int i, int i1, int i2) {
        return provider.getPossibleCreatures(ect, i, i1, i2);
    }

    public net.minecraft.world.ChunkPosition findClosestStructure(net.minecraft.world.World world, String string, int i, int i1, int i2) {
        return provider.findClosestStructure(world, string, i, i1, i2);
    }

    public void recreateStructures(int i, int j) {
        provider.recreateStructures(i, j);
    }

    // n.m.s implementations always return 0. (The true implementation is in ChunkProviderServer)
    public int getLoadedChunkCount() {
        return 0;
    }

    public String makeString() {
        return "NormalWorldGenerator";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
        return provider.getPossibleCreatures(var1, var2, var3, var4);
    }

    // MCPC+ start - return vanilla compatible IChunkProvider for forge
    public IChunkProvider getForgeChunkProvider()
    {
        return this.provider;
    }
    // MCPC+ end
}
