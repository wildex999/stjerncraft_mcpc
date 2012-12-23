package net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import cpw.mods.fml.common.FMLLog;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.MinecraftForge;
import java.io.DataInput;
import java.io.DataOutput;
import net.minecraftforge.event.world.ChunkDataEvent.Load;
import net.minecraftforge.event.world.ChunkDataEvent.Save;

public class AnvilChunkLoader implements IThreadedFileIO, IChunkLoader
{
    private java.util.LinkedHashMap<ChunkCoordIntPair, AnvilChunkLoaderPending> pendingSaves = new java.util.LinkedHashMap<ChunkCoordIntPair, AnvilChunkLoaderPending>(); // Spigot
    private Object syncLockObject = new Object();

    /** Save directory for chunks using the Anvil format */
    public final File chunkSaveLocation;

    public AnvilChunkLoader(File par1File)
    {
        this.chunkSaveLocation = par1File;
    }

    // CraftBukkit start
    public boolean chunkExists(World world, int i, int j)
    {
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i, j);

        synchronized (this.syncLockObject)
        {
            // Spigot start
            if (pendingSaves.containsKey(chunkcoordintpair))
            {
                return true;
            }
        }

        // Spigot end
        return RegionFileCache.createOrLoadRegionFile(this.chunkSaveLocation, i, j).chunkExists(i & 31, j & 31);
    }
    // CraftBukkit end

    // CraftBukkit start - add async variant, provide compatibility
    public Chunk loadChunk(World par1World, int par2, int par3)
    {
        Object[] var4 = this.loadChunk__Async_CB(par1World, par2, par3);

        if (var4 != null)
        {
            Chunk var5 = (Chunk) var4[0];
            NBTTagCompound var6 = (NBTTagCompound) var4[1];
            this.loadEntities(var5, var6.getCompoundTag("Level"), par1World);
            return var5;
        }

        return null;
    }

    public Object[] loadChunk__Async_CB(World world, int i, int j)
    {
        // CraftBukkit end
        NBTTagCompound nbttagcompound = null;
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i, j);
        Object object = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            // Spigot start
            AnvilChunkLoaderPending pendingchunktosave = pendingSaves.get(chunkcoordintpair);

            if (pendingchunktosave != null)
            {
                nbttagcompound = pendingchunktosave.nbtTags;
            }

            /*
            if (this.b.contains(chunkcoordintpair)) {
                for (int k = 0; k < this.a.size(); ++k) {
                    if (((PendingChunkToSave) this.a.get(k)).a.equals(chunkcoordintpair)) {
                        nbttagcompound = ((PendingChunkToSave) this.a.get(k)).b;
                        break;
                    }
                }
            }
            */// Spigot end
        }

        if (nbttagcompound == null)
        {
            DataInputStream datainputstream = RegionFileCache.getChunkInputStream(this.chunkSaveLocation, i, j);

            if (datainputstream == null)
            {
                return null;
            }

            try
            {
                nbttagcompound = CompressedStreamTools.read((DataInput) datainputstream);
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }

        return this.checkedReadChunkFromNBT(world, i, j, nbttagcompound);
    }

    protected Object[] checkedReadChunkFromNBT(World world, int i, int j, NBTTagCompound nbttagcompound)   // CraftBukkit - return Chunk -> Object[]
    {
        if (!nbttagcompound.hasKey("Level"))
        {
            System.out.println("Chunk file at " + i + "," + j + " is missing level data, skipping");
            return null;
        }
        else if (!nbttagcompound.getCompoundTag("Level").hasKey("Sections"))
        {
            System.out.println("Chunk file at " + i + "," + j + " is missing block data, skipping");
            return null;
        }
        else
        {
            Chunk chunk = this.readChunkFromNBT(world, nbttagcompound.getCompoundTag("Level"));

            if (!chunk.isAtLocation(i, j))
            {
                System.out.println("Chunk file at " + i + "," + j + " is in the wrong location; relocating. (Expected " + i + ", " + j + ", got " + chunk.xPosition + ", " + chunk.zPosition + ")");
                nbttagcompound.getCompoundTag("Level").setInteger("xPos", i); // CraftBukkit - .getCompound("Level")
                nbttagcompound.getCompoundTag("Level").setInteger("zPos", j); // CraftBukkit - .getCompound("Level")
                chunk = this.readChunkFromNBT(world, nbttagcompound.getCompoundTag("Level"));
            }

            // CraftBukkit start
            Object[] data = new Object[2];
            data[0] = chunk;
            data[1] = nbttagcompound;
            MinecraftForge.EVENT_BUS.post(new Load(chunk, nbttagcompound));
            return data;
            // CraftBukkit end
        }
    }

    public void saveChunk(World par1World, Chunk par2Chunk)
    {
        // CraftBukkit start - "handle" exception
        try
        {
            par1World.checkSessionLock();
        }
        catch (MinecraftException ex)
        {
            // MCPC+ disable this for now.
            //ex.printStackTrace();
        }

        // CraftBukkit end

        try
        {
            NBTTagCompound var3 = new NBTTagCompound();
            NBTTagCompound var4 = new NBTTagCompound();
            var3.setTag("Level", var4);
            this.writeChunkToNBT(par2Chunk, par1World, var4);
            this.func_75824_a(par2Chunk.getChunkCoordIntPair(), var3);
            MinecraftForge.EVENT_BUS.post(new Save(par2Chunk, var3));
        }
        catch (Exception var5)
        {
            var5.printStackTrace();
        }
    }

    protected void func_75824_a(ChunkCoordIntPair par1ChunkCoordIntPair, NBTTagCompound par2NBTTagCompound)
    {
        Object var3 = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            // Spigot start
            if (this.pendingSaves.put(par1ChunkCoordIntPair, new AnvilChunkLoaderPending(par1ChunkCoordIntPair, par2NBTTagCompound)) != null)
            {
                return;
            }

            /*
            if (this.b.contains(chunkcoordintpair)) {
                for (int i = 0; i < this.a.size(); ++i) {
                    if (((PendingChunkToSave) this.a.get(i)).a.equals(chunkcoordintpair)) {
                        this.a.set(i, new PendingChunkToSave(chunkcoordintpair, nbttagcompound));
                        return;
                    }
                }
            }

            this.a.add(new PendingChunkToSave(chunkcoordintpair, nbttagcompound));
            this.b.add(chunkcoordintpair);
            */// Spigot end
            ThreadedFileIOBase.threadedIOInstance.queueIO(this);
        }
    }

    /**
     * Returns a boolean stating if the write was unsuccessful.
     */
    public boolean writeNextIO()
    {
        AnvilChunkLoaderPending var1 = null;
        Object var2 = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            // Spigot start
            if (this.pendingSaves.isEmpty())
            {
                return false;
            }

            var1 = this.pendingSaves.values().iterator().next();
            this.pendingSaves.remove(var1.chunkCoordinate);
            /*
            if (this.a.isEmpty()) {
                return false;
            }

            pendingchunktosave = (PendingChunkToSave) this.a.remove(0);
            this.b.remove(pendingchunktosave.a);
            */// Spigot end
        }

        if (var1 != null)
        {
            try
            {
                this.writeChunkNBTTags(var1);
            }
            catch (Exception var4)
            {
                var4.printStackTrace();
            }
        }

        return true;
    }

    public void writeChunkNBTTags(AnvilChunkLoaderPending par1AnvilChunkLoaderPending) throws java.io.IOException   // CraftBukkit - public -> private, added throws
    {
        DataOutputStream var2 = RegionFileCache.getChunkOutputStream(this.chunkSaveLocation, par1AnvilChunkLoaderPending.chunkCoordinate.chunkXPos, par1AnvilChunkLoaderPending.chunkCoordinate.chunkZPos);
        CompressedStreamTools.write(par1AnvilChunkLoaderPending.nbtTags, var2);
        var2.close();
    }

    /**
     * Save extra data associated with this Chunk not normally saved during autosave, only during chunk unload.
     * Currently unused.
     */
    public void saveExtraChunkData(World par1World, Chunk par2Chunk) {}

    /**
     * Called every World.tick()
     */
    public void chunkTick() {}

    /**
     * Save extra data not associated with any Chunk.  Not saved during autosave, only during world unload.  Currently
     * unused.
     */
    public void saveExtraData() {}

    /**
     * Writes the Chunk passed as an argument to the NBTTagCompound also passed, using the World argument to retrieve
     * the Chunk's last update time.
     */
    private void writeChunkToNBT(Chunk par1Chunk, World par2World, NBTTagCompound par3NBTTagCompound)
    {
        par3NBTTagCompound.setInteger("xPos", par1Chunk.xPosition);
        par3NBTTagCompound.setInteger("zPos", par1Chunk.zPosition);
        par3NBTTagCompound.setLong("LastUpdate", par2World.getTotalWorldTime());
        par3NBTTagCompound.setIntArray("HeightMap", par1Chunk.heightMap);
        par3NBTTagCompound.setBoolean("TerrainPopulated", par1Chunk.isTerrainPopulated);
        ExtendedBlockStorage[] var4 = par1Chunk.getBlockStorageArray();
        NBTTagList var5 = new NBTTagList("Sections");
        boolean var6 = !par2World.provider.hasNoSky;
        ExtendedBlockStorage[] var7 = var4;
        int var8 = var4.length;
        NBTTagCompound var11;

        for (int var9 = 0; var9 < var8; ++var9)
        {
            ExtendedBlockStorage var10 = var7[var9];

            if (var10 != null)
            {
                var11 = new NBTTagCompound();
                var11.setByte("Y", (byte)(var10.getYLocation() >> 4 & 255));
                var11.setByteArray("Blocks", var10.getBlockLSBArray());

                if (var10.getBlockMSBArray() != null)
                {
                    var11.setByteArray("Add", var10.getBlockMSBArray().getValueArray()); // Spigot
                }

                var11.setByteArray("Data", var10.getMetadataArray().getValueArray()); // Spigot
                var11.setByteArray("BlockLight", var10.getBlocklightArray().getValueArray()); // Spigot

                if (var6)
                {
                    var11.setByteArray("SkyLight", var10.getSkylightArray().getValueArray()); // Spigot
                }
                else
                {
                    var11.setByteArray("SkyLight", new byte[var10.getBlocklightArray().getValueArray().length]); // Spigot
                }

                var5.appendTag(var11);
            }
        }

        par3NBTTagCompound.setTag("Sections", var5);
        par3NBTTagCompound.setByteArray("Biomes", par1Chunk.getBiomeArray());
        par1Chunk.hasEntities = false;
        NBTTagList var16 = new NBTTagList();
        Iterator var18;

        for (var8 = 0; var8 < par1Chunk.entityLists.length; ++var8)
        {
            var18 = par1Chunk.entityLists[var8].iterator();

            while (var18.hasNext())
            {
                Entity var21 = (Entity)var18.next();
                par1Chunk.hasEntities = true;
                var11 = new NBTTagCompound();


                try
                {
                    if (var21.addEntityID(var11))
                    {
                        var16.appendTag(var11);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.SEVERE, e,
                            "An Entity type %s has thrown an exception trying to write state. It will not persist. Report this to the mod author",
                            var21.getClass().getName());
                 }
            }
        }

        par3NBTTagCompound.setTag("Entities", var16);
        NBTTagList var17 = new NBTTagList();
        var18 = par1Chunk.chunkTileEntityMap.values().iterator();

        while (var18.hasNext())
        {
            TileEntity var22 = (TileEntity)var18.next();
            var11 = new NBTTagCompound();
            try
            {
                var22.writeToNBT(var11);
                var17.appendTag(var11);
            }
            catch (Exception e)
            {
                FMLLog.log(Level.SEVERE, e,
                        "A TileEntity type %s has throw an exception trying to write state. It will not persist. Report this to the mod author",
                        var22.getClass().getName());
            }
        }

        par3NBTTagCompound.setTag("TileEntities", var17);
        List var20 = par2World.getPendingBlockUpdates(par1Chunk, false);

        if (var20 != null)
        {
            long var19 = par2World.getTotalWorldTime();
            NBTTagList var12 = new NBTTagList();
            Iterator var13 = var20.iterator();

            while (var13.hasNext())
            {
                NextTickListEntry var14 = (NextTickListEntry)var13.next();
                NBTTagCompound var15 = new NBTTagCompound();
                var15.setInteger("i", var14.blockID);
                var15.setInteger("x", var14.xCoord);
                var15.setInteger("y", var14.yCoord);
                var15.setInteger("z", var14.zCoord);
                var15.setInteger("t", (int)(var14.scheduledTime - var19));
                var12.appendTag(var15);
            }

            par3NBTTagCompound.setTag("TileTicks", var12);
        }
    }

    /**
     * Reads the data stored in the passed NBTTagCompound and creates a Chunk with that data in the passed World.
     * Returns the created Chunk.
     */
    private Chunk readChunkFromNBT(World par1World, NBTTagCompound par2NBTTagCompound)
    {
        int var3 = par2NBTTagCompound.getInteger("xPos");
        int var4 = par2NBTTagCompound.getInteger("zPos");
        Chunk var5 = new Chunk(par1World, var3, var4);
        var5.heightMap = par2NBTTagCompound.getIntArray("HeightMap");
        var5.isTerrainPopulated = par2NBTTagCompound.getBoolean("TerrainPopulated");
        NBTTagList var6 = par2NBTTagCompound.getTagList("Sections");
        byte var7 = 16;
        ExtendedBlockStorage[] var8 = new ExtendedBlockStorage[var7];
        boolean var9 = !par1World.provider.hasNoSky;

        for (int var10 = 0; var10 < var6.tagCount(); ++var10)
        {
            NBTTagCompound var11 = (NBTTagCompound)var6.tagAt(var10);
            byte var12 = var11.getByte("Y");
            ExtendedBlockStorage var13 = new ExtendedBlockStorage(var12 << 4, var9);
            var13.setBlockLSBArray(var11.getByteArray("Blocks"));

            if (var11.hasKey("Add"))
            {
                var13.setBlockMSBArray(new NibbleArray(var11.getByteArray("Add"), 4));
            }

            var13.setBlockMetadataArray(new NibbleArray(var11.getByteArray("Data"), 4));
            var13.setBlocklightArray(new NibbleArray(var11.getByteArray("BlockLight"), 4));

            if (var9)
            {
                var13.setSkylightArray(new NibbleArray(var11.getByteArray("SkyLight"), 4));
            }

            var13.removeInvalidBlocks();
            var8[var12] = var13;
        }

        var5.setStorageArrays(var8);

        if (par2NBTTagCompound.hasKey("Biomes"))
        {
            var5.setBiomeArray(par2NBTTagCompound.getByteArray("Biomes"));
        }

        // CraftBukkit start - end this method here and split off entity loading to another method
        return var5;
    }

    public void loadEntities(Chunk var5, NBTTagCompound par2NBTTagCompound, World par1World)
    {
        // CraftBukkit end
        NBTTagList var16 = par2NBTTagCompound.getTagList("Entities");

        if (var16 != null)
        {
            for (int var15 = 0; var15 < var16.tagCount(); ++var15)
            {
                NBTTagCompound var17 = (NBTTagCompound)var16.tagAt(var15);
                Entity var22 = EntityList.createEntityFromNBT(var17, par1World);
                var5.hasEntities = true;

                if (var22 != null)
                {
                    var5.addEntity(var22);
                }
            }
        }

        NBTTagList var19 = par2NBTTagCompound.getTagList("TileEntities");

        if (var19 != null)
        {
            for (int var18 = 0; var18 < var19.tagCount(); ++var18)
            {
                NBTTagCompound var20 = (NBTTagCompound)var19.tagAt(var18);
                TileEntity var14 = TileEntity.createAndLoadEntity(var20);

                if (var14 != null)
                {
                    var5.addTileEntity(var14);
                }
            }
        }

        if (par2NBTTagCompound.hasKey("TileTicks"))
        {
            NBTTagList var23 = par2NBTTagCompound.getTagList("TileTicks");

            if (var23 != null)
            {
                for (int var21 = 0; var21 < var23.tagCount(); ++var21)
                {
                    NBTTagCompound var24 = (NBTTagCompound)var23.tagAt(var21);
                    par1World.scheduleBlockUpdateFromLoad(var24.getInteger("x"), var24.getInteger("y"), var24.getInteger("z"), var24.getInteger("i"), var24.getInteger("t"));
                }
            }
        }

        //return var5; // CraftBukkit
    }
}
