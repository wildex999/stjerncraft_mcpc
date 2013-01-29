package net.minecraft.world.gen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
// CraftBukkit start
import java.util.Random;

import net.minecraft.block.BlockSand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.bukkit.Server;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongHashSet;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.world.ChunkUnloadEvent;
// CraftBukkit end

public class ChunkProviderServer implements IChunkProvider
{
    // CraftBukkit start
    public LongHashSet chunksToUnload = new LongHashSet();
    public Chunk defaultEmptyChunk;
    public IChunkProvider currentChunkProvider; // CraftBukkit
    public IChunkLoader currentChunkLoader; // Spigot
    public boolean loadChunkOnProvideRequest = false; // MCPC+ - when using this flag, it must be turned off after use or will cause issues with chunk unloading
    public LongObjectHashMap<Chunk> loadedChunkHashMap = new LongObjectHashMap<Chunk>();
    public List loadedChunks = new ArrayList(); // MCPC+  vanilla compatibility
    public WorldServer worldObj;
    // CraftBukkit end

    public ChunkProviderServer(WorldServer par1WorldServer, IChunkLoader par2IChunkLoader, IChunkProvider par3IChunkProvider)
    {
        this.defaultEmptyChunk = new EmptyChunk(par1WorldServer, 0, 0);
        this.worldObj = par1WorldServer;
        this.currentChunkLoader = par2IChunkLoader;
        this.currentChunkProvider = par3IChunkProvider;
    }

    /**
     * Checks to see if a chunk exists at x, y
     */
    public boolean chunkExists(int par1, int par2)
    {
        return this.loadedChunkHashMap.containsKey(LongHash.toLong(par1, par2)); // CraftBukkit
    }

    /**
     * marks chunk for unload by "unload100OldestChunks"  if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    public void unloadChunksIfNotNearSpawn(int par1, int par2)
    {
        if (this.worldObj.provider.canRespawnHere() && DimensionManager.shouldLoadSpawn(this.worldObj.dimension))   // Forge
        {
            ChunkCoordinates var3 = this.worldObj.getSpawnPoint();
            int var4 = par1 * 16 + 8 - var3.posX;
            int var5 = par2 * 16 + 8 - var3.posZ;
            short var6 = 128;

            // CraftBukkit start
            if (var4 < -var6 || var4 > var6 || var5 < -var6 || var5 > var6 || !(this.worldObj.keepSpawnInMemory))   // Added 'this.world.keepSpawnInMemory'
            {
                this.chunksToUnload.add(par1, par2);
                Chunk c = this.loadedChunkHashMap.get(LongHash.toLong(par1, par2));

                if (c != null)
                {
                    c.mustSave = true;
                }
            }

            // CraftBukkit end
        }
        else
        {
            // CraftBukkit start
            this.chunksToUnload.add(par1, par2);
            Chunk c = this.loadedChunkHashMap.get(LongHash.toLong(par1, par2));

            if (c != null)
            {
                c.mustSave = true;
            }

            // CraftBukkit end
        }
    }

    /**
     * marks all chunks for unload, ignoring those near the spawn
     */
    public void unloadAllChunks()
    {
        Iterator var1 = this.loadedChunkHashMap.values().iterator(); // CraftBukkit

        while (var1.hasNext())
        {
            Chunk var2 = (Chunk)var1.next();
            this.unloadChunksIfNotNearSpawn(var2.xPosition, var2.zPosition);
        }
    }

    // CraftBukkit start - add async variant, provide compatibility

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int par1, int par2)
    {
        return getChunkAt(par1, par2, null);
    }

    public Chunk getChunkAt(int i, int j, Runnable runnable)
    {
        this.chunksToUnload.remove(i, j);
        Chunk chunk = (Chunk) this.loadedChunkHashMap.get(LongHash.toLong(i, j));
        boolean newChunk = false;
        AnvilChunkLoader loader = null;

        if (this.currentChunkLoader instanceof AnvilChunkLoader)
        {
            loader = (AnvilChunkLoader) this.currentChunkLoader;
        }

        // If the chunk exists but isn't loaded do it async
        if (chunk == null && runnable != null && loader != null && loader.chunkExists(this.worldObj, i, j))
        {
            ChunkIOExecutor.queueChunkLoad(this.worldObj, loader, this, i, j, runnable);
            return null;
        }

        // CraftBukkit end

        if (chunk == null)
        {
            // Forge start
            chunk =  ForgeChunkManager.fetchDormantChunk(LongHash.toLong(i, j), this.worldObj);

            if (chunk == null)
            {
                chunk = this.safeLoadChunk(i, j);
            }

            // Forge end
            if (chunk == null)
            {
                if (this.currentChunkProvider == null)
                {
                    chunk = this.defaultEmptyChunk;
                }
                else
                {
                    try
                    {
                        chunk = this.currentChunkProvider.provideChunk(i, j);
                    }
                    catch (Throwable throwable)
                    {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception generating new chunk");
                        CrashReportCategory crashreportsystemdetails = crashreport.makeCategory("Chunk to be generated");
                        crashreportsystemdetails.addCrashSection("Location", String.format("%d,%d", new Object[] { Integer.valueOf(i), Integer.valueOf(j)}));
                        crashreportsystemdetails.addCrashSection("Position hash", Long.valueOf(LongHash.toLong(i, j)));
                        crashreportsystemdetails.addCrashSection("Generator", this.currentChunkProvider.makeString());
                        throw new ReportedException(crashreport);
                    }
                }

                newChunk = true; // CraftBukkit
            }

            this.loadedChunkHashMap.put(LongHash.toLong(i, j), chunk); // CraftBukkit
            loadedChunks.add(chunk); // MCPC+ - vanilla compatibility

            if (chunk != null)
            {
                chunk.onChunkLoad();
            }

            // CraftBukkit start
            Server server = this.worldObj.getServer();

            if (server != null)
            {
                /*
                 * If it's a new world, the first few chunks are generated inside
                 * the World constructor. We can't reliably alter that, so we have
                 * no way of creating a CraftWorld/CraftServer at that point.
                 */
                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, newChunk));
            }

            // CraftBukkit end
            chunk.populateChunk(this, this, i, j);
        }

        // CraftBukkit start - If we didn't need to load the chunk run the callback now
        if (runnable != null)
        {
            runnable.run();
        }
        // CraftBukkit end
        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int par1, int par2)
    {
        // CraftBukkit start
        Chunk chunk = (Chunk) this.loadedChunkHashMap.get(LongHash.toLong(par1, par2));
        chunk = chunk == null ? (!this.worldObj.findingSpawnPoint && !this.loadChunkOnProvideRequest ? this.defaultEmptyChunk : this.loadChunk(par1, par2)) : chunk;

        if (chunk == this.defaultEmptyChunk)
        {
            return chunk;
        }

        if (par1 != chunk.xPosition || par2 != chunk.zPosition)
        {
            MinecraftServer.logger.severe("Chunk (" + chunk.xPosition + ", " + chunk.zPosition + ") stored at  (" + par1 + ", " + par2 + ") in world '" + worldObj.getWorld().getName() + "'");
            MinecraftServer.logger.severe(chunk.getClass().getName());
            Throwable ex = new Throwable();
            ex.fillInStackTrace();
            ex.printStackTrace();
        }

        return chunk;
        // CraftBukkit end
    }

    public Chunk safeLoadChunk(int par1, int par2)   // CraftBukkit - private -> public
    {
        if (this.currentChunkLoader == null)
        {
            return null;
        }
        else
        {
            try
            {
                Chunk var3 = this.currentChunkLoader.loadChunk(this.worldObj, par1, par2);

                if (var3 != null)
                {
                    var3.lastSaveTime = this.worldObj.getTotalWorldTime();

                    if (this.currentChunkProvider != null)
                    {
                        this.currentChunkProvider.recreateStructures(par1, par2);
                    }
                }

                return var3;
            }
            catch (Exception var4)
            {
                var4.printStackTrace();
                return null;
            }
        }
    }

    public void safeSaveExtraChunkData(Chunk par1Chunk)   // CraftBukkit - private -> public
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                this.currentChunkLoader.saveExtraChunkData(this.worldObj, par1Chunk);
            }
            catch (Exception var3)
            {
                var3.printStackTrace();
            }
        }
    }

    public void safeSaveChunk(Chunk par1Chunk)   // CraftBukkit - private -> public
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                par1Chunk.lastSaveTime = this.worldObj.getTotalWorldTime();
                this.currentChunkLoader.saveChunk(this.worldObj, par1Chunk);
            }
            catch (Exception var3)     // CraftBukkit - IOException -> Exception
            {
                var3.printStackTrace();
                // CraftBukkit start - remove extra exception
            }

            // } catch (ExceptionWorldConflict exceptionworldconflict) {
            //     exceptionworldconflict.printStackTrace();
            // }
            // CraftBukkit end
        }
    }

    /**
     * Populates chunk with ores etc etc
     */
    public void populate(IChunkProvider par1IChunkProvider, int par2, int par3)
    {
        Chunk var4 = this.provideChunk(par2, par3);

        if (!var4.isTerrainPopulated)
        {
            var4.isTerrainPopulated = true;

            if (this.currentChunkProvider != null)
            {
                this.currentChunkProvider.populate(par1IChunkProvider, par2, par3);
                // CraftBukkit start
                BlockSand.fallInstantly = true;
                Random random = new Random();
                random.setSeed(worldObj.getSeed());
                long xRand = random.nextLong() / 2L * 2L + 1L;
                long zRand = random.nextLong() / 2L * 2L + 1L;
                random.setSeed((long) par2 * xRand + (long) par3 * zRand ^ worldObj.getSeed());
                org.bukkit.World world = this.worldObj.getWorld();

                if (world != null)
                {
                    for (org.bukkit.generator.BlockPopulator populator : world.getPopulators())
                    {
                        populator.populate(world, random, var4.bukkitChunk);
                    }
                }

                BlockSand.fallInstantly = false;
                this.worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.world.ChunkPopulateEvent(var4.bukkitChunk));
                // CraftBukkit end
                GameRegistry.generateWorld(par2, par3, this.worldObj, this.currentChunkProvider, par1IChunkProvider); // Forge
                var4.setChunkModified();
            }
        }
    }

    /**
     * Two modes of operation: if passed true, save all Chunks in one go.  If passed false, save up to two chunks.
     * Return true if all chunks have been saved.
     */
    public boolean saveChunks(boolean par1, IProgressUpdate par2IProgressUpdate)
    {
        int var3 = 0;
        // CraftBukkit start
        Iterator iterator = this.loadedChunkHashMap.values().iterator();

        while (iterator.hasNext())
        {
            Chunk chunk = (Chunk) iterator.next();
            // CraftBukkit end

            if (par1)
            {
                this.safeSaveExtraChunkData(chunk);
            }

            if (chunk.needsSaving(par1))
            {
                this.safeSaveChunk(chunk);
                chunk.isModified = false;
                ++var3;

                if (var3 == 24 && !par1)
                {
                    return false;
                }
            }
        }

        if (par1)
        {
            if (this.currentChunkLoader == null)
            {
                return true;
            }

            this.currentChunkLoader.saveExtraData();
        }

        return true;
    }

    /**
     * Unloads the 100 oldest chunks from memory, due to a bug with chunkSet.add() never being called it thinks the list
     * is always empty and will not remove any chunks.
     */
    public boolean unload100OldestChunks()
    {
        if (!this.worldObj.canNotSave)
        {
            // MCPC+ start - remove any chunk that has a ticket associated with it
            if (!this.chunksToUnload.isEmpty())
            {
                for (ChunkCoordIntPair forcedChunk : this.worldObj.getPersistentChunks().keys())
                {
                    this.chunksToUnload.remove(forcedChunk.chunkXPos, forcedChunk.chunkZPos);
                }
            }
            // MCPC+ end
            // CraftBukkit start
            Server server = this.worldObj.getServer();

            for (int i = 0; i < 100 && !this.chunksToUnload.isEmpty(); i++)
            {
                long chunkcoordinates = this.chunksToUnload.popFirst();
                Chunk chunk = this.loadedChunkHashMap.get(chunkcoordinates);

                if (chunk == null)
                {
                    continue;
                }
                ChunkUnloadEvent event = new ChunkUnloadEvent(chunk.bukkitChunk);
                server.getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    chunk.onChunkUnload();
                    this.safeSaveChunk(chunk);
                    this.safeSaveExtraChunkData(chunk);
                    this.loadedChunkHashMap.remove(chunkcoordinates); // CraftBukkit
                    loadedChunks.remove(chunk); // MCPC+ - vanilla compatibility
                    ForgeChunkManager.putDormantChunk(chunkcoordinates, chunk);

                    if (this.loadedChunkHashMap.size() == 0 && ForgeChunkManager.getPersistentChunksFor(this.worldObj).size() == 0 && !DimensionManager.shouldLoadSpawn(this.worldObj.provider.dimensionId))
                    {
                        if (this.currentChunkProvider.makeString().equals(MinecraftServer.getServer().worlds.get(0).chunkProvider.makeString()))
                        {
                            DimensionManager.unloadWorld(this.worldObj.dimension);
                            return this.currentChunkProvider.unload100OldestChunks();
                        }
                    }
                }
            }
            // CraftBukkit end

            if (this.currentChunkLoader != null)
            {
                this.currentChunkLoader.chunkTick();
            }
        }

        return this.currentChunkProvider.unload100OldestChunks();
    }

    /**
     * Returns if the IChunkProvider supports saving.
     */
    public boolean canSave()
    {
        return !this.worldObj.canNotSave;
    }

    /**
     * Converts the instance data to a readable string.
     */
    public String makeString()
    {
        return "ServerChunkCache: " + this.loadedChunkHashMap.values().size() + " Drop: " + this.chunksToUnload.size(); // CraftBukkit
    }

    /**
     * Returns a list of creatures of the specified type that can spawn at the given location.
     */
    public List getPossibleCreatures(EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4)
    {
        return this.currentChunkProvider.getPossibleCreatures(par1EnumCreatureType, par2, par3, par4);
    }

    /**
     * Returns the location of the closest structure of the specified type. If not found returns null.
     */
    public ChunkPosition findClosestStructure(World par1World, String par2Str, int par3, int par4, int par5)
    {
        return this.currentChunkProvider.findClosestStructure(par1World, par2Str, par3, par4, par5);
    }

    public int getLoadedChunkCount()
    {
        return this.loadedChunkHashMap.values().size(); // CraftBukkit
    }

    public void recreateStructures(int par1, int par2) {}
}
