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
        Object[] aobject = this.loadChunk__Async_CB(par1World, par2, par3);

        if (aobject != null)
        {
            Chunk chunk = (Chunk) aobject[0];
            NBTTagCompound nbttagcompound = (NBTTagCompound) aobject[1];
            this.loadEntities(chunk, nbttagcompound.getCompoundTag("Level"), par1World);
            return chunk;
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
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound.setTag("Level", nbttagcompound1);
            this.writeChunkToNBT(par2Chunk, par1World, nbttagcompound1);
            this.func_75824_a(par2Chunk.getChunkCoordIntPair(), nbttagcompound);
            MinecraftForge.EVENT_BUS.post(new Save(par2Chunk, nbttagcompound));
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    protected void func_75824_a(ChunkCoordIntPair par1ChunkCoordIntPair, NBTTagCompound par2NBTTagCompound)
    {
        Object object = this.syncLockObject;

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
        AnvilChunkLoaderPending anvilchunkloaderpending = null;
        Object object = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            // Spigot start
            if (this.pendingSaves.isEmpty())
            {
                return false;
            }

            anvilchunkloaderpending = this.pendingSaves.values().iterator().next();
            this.pendingSaves.remove(anvilchunkloaderpending.chunkCoordinate);
            /*
            if (this.a.isEmpty()) {
                return false;
            }

            pendingchunktosave = (PendingChunkToSave) this.a.remove(0);
            this.b.remove(pendingchunktosave.a);
            */// Spigot end
        }

        if (anvilchunkloaderpending != null)
        {
            try
            {
                this.writeChunkNBTTags(anvilchunkloaderpending);
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }

        return true;
    }

    public void writeChunkNBTTags(AnvilChunkLoaderPending par1AnvilChunkLoaderPending) throws java.io.IOException   // CraftBukkit - public -> private, added throws
    {
        DataOutputStream dataoutputstream = RegionFileCache.getChunkOutputStream(this.chunkSaveLocation, par1AnvilChunkLoaderPending.chunkCoordinate.chunkXPos, par1AnvilChunkLoaderPending.chunkCoordinate.chunkZPos);
        CompressedStreamTools.write(par1AnvilChunkLoaderPending.nbtTags, dataoutputstream);
        dataoutputstream.close();
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
        ExtendedBlockStorage[] aextendedblockstorage = par1Chunk.getBlockStorageArray();
        NBTTagList nbttaglist = new NBTTagList("Sections");
        boolean flag = !par2World.provider.hasNoSky;
        ExtendedBlockStorage[] aextendedblockstorage1 = aextendedblockstorage;
        int i = aextendedblockstorage.length;
        NBTTagCompound nbttagcompound1;

        for (int j = 0; j < i; ++j)
        {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage1[j];

            if (extendedblockstorage != null)
            {
                nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Y", (byte)(extendedblockstorage.getYLocation() >> 4 & 255));
                nbttagcompound1.setByteArray("Blocks", extendedblockstorage.getBlockLSBArray());

                if (extendedblockstorage.getBlockMSBArray() != null)
                {
                    nbttagcompound1.setByteArray("Add", extendedblockstorage.getBlockMSBArray().getValueArray()); // Spigot
                }

                nbttagcompound1.setByteArray("Data", extendedblockstorage.getMetadataArray().getValueArray()); // Spigot
                nbttagcompound1.setByteArray("BlockLight", extendedblockstorage.getBlocklightArray().getValueArray()); // Spigot

                if (flag)
                {
                    nbttagcompound1.setByteArray("SkyLight", extendedblockstorage.getSkylightArray().getValueArray()); // Spigot
                }
                else
                {
                    nbttagcompound1.setByteArray("SkyLight", new byte[extendedblockstorage.getBlocklightArray().getValueArray().length]); // Spigot
                }

                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        par3NBTTagCompound.setTag("Sections", nbttaglist);
        par3NBTTagCompound.setByteArray("Biomes", par1Chunk.getBiomeArray());
        par1Chunk.hasEntities = false;
        NBTTagList nbttaglist1 = new NBTTagList();
        Iterator iterator;

        for (i = 0; i < par1Chunk.entityLists.length; ++i)
        {
            iterator = par1Chunk.entityLists[i].iterator();

            while (iterator.hasNext())
            {
                Entity entity = (Entity)iterator.next();
                par1Chunk.hasEntities = true;
                nbttagcompound1 = new NBTTagCompound();


                try
                {
                    if (entity.addEntityID(nbttagcompound1))
                    {
                        nbttaglist1.appendTag(nbttagcompound1);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.SEVERE, e,
                            "An Entity type %s at %s,%f,%f,%f has thrown an exception trying to write state. It will not persist. Report this to the mod author",
                            entity.getClass().getName(),
                            entity.worldObj.getWorld().getName(),
                            entity.posX, entity.posY, entity.posZ);
                 }
            }
        }

        par3NBTTagCompound.setTag("Entities", nbttaglist1);
        NBTTagList nbttaglist2 = new NBTTagList();
        iterator = par1Chunk.chunkTileEntityMap.values().iterator();

        while (iterator.hasNext())
        {
            TileEntity tileentity = (TileEntity)iterator.next();
            nbttagcompound1 = new NBTTagCompound();
            try
            {
                tileentity.writeToNBT(nbttagcompound1);
                nbttaglist2.appendTag(nbttagcompound1);
            }
            catch (Exception e)
            {
                FMLLog.log(Level.SEVERE, e,
                        "A TileEntity type %s at %s,%d,%d,%d has throw an exception trying to write state. It will not persist. Report this to the mod author",
                        tileentity.getClass().getName(),
                        tileentity.worldObj.getWorld().getName(),
                        tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
            }
        }

        par3NBTTagCompound.setTag("TileEntities", nbttaglist2);
        List list = par2World.getPendingBlockUpdates(par1Chunk, false);

        if (list != null)
        {
            long k = par2World.getTotalWorldTime();
            NBTTagList nbttaglist3 = new NBTTagList();
            Iterator iterator1 = list.iterator();

            while (iterator1.hasNext())
            {
                NextTickListEntry nextticklistentry = (NextTickListEntry)iterator1.next();
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                nbttagcompound2.setInteger("i", nextticklistentry.blockID);
                nbttagcompound2.setInteger("x", nextticklistentry.xCoord);
                nbttagcompound2.setInteger("y", nextticklistentry.yCoord);
                nbttagcompound2.setInteger("z", nextticklistentry.zCoord);
                nbttagcompound2.setInteger("t", (int)(nextticklistentry.scheduledTime - k));
                nbttaglist3.appendTag(nbttagcompound2);
            }

            par3NBTTagCompound.setTag("TileTicks", nbttaglist3);
        }
    }

    /**
     * Reads the data stored in the passed NBTTagCompound and creates a Chunk with that data in the passed World.
     * Returns the created Chunk.
     */
    private Chunk readChunkFromNBT(World par1World, NBTTagCompound par2NBTTagCompound)
    {
        int i = par2NBTTagCompound.getInteger("xPos");
        int j = par2NBTTagCompound.getInteger("zPos");
        Chunk chunk = new Chunk(par1World, i, j);
        chunk.heightMap = par2NBTTagCompound.getIntArray("HeightMap");
        chunk.isTerrainPopulated = par2NBTTagCompound.getBoolean("TerrainPopulated");
        NBTTagList nbttaglist = par2NBTTagCompound.getTagList("Sections");
        byte b0 = 16;
        ExtendedBlockStorage[] aextendedblockstorage = new ExtendedBlockStorage[b0];
        boolean flag = !par1World.provider.hasNoSky;

        for (int k = 0; k < nbttaglist.tagCount(); ++k)
        {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(k);
            byte b1 = nbttagcompound1.getByte("Y");
            ExtendedBlockStorage extendedblockstorage = new ExtendedBlockStorage(b1 << 4, flag);
            extendedblockstorage.setBlockLSBArray(nbttagcompound1.getByteArray("Blocks"));

            if (nbttagcompound1.hasKey("Add"))
            {
                extendedblockstorage.setBlockMSBArray(new NibbleArray(nbttagcompound1.getByteArray("Add"), 4));
            }

            extendedblockstorage.setBlockMetadataArray(new NibbleArray(nbttagcompound1.getByteArray("Data"), 4));
            extendedblockstorage.setBlocklightArray(new NibbleArray(nbttagcompound1.getByteArray("BlockLight"), 4));

            if (flag)
            {
                extendedblockstorage.setSkylightArray(new NibbleArray(nbttagcompound1.getByteArray("SkyLight"), 4));
            }

            extendedblockstorage.removeInvalidBlocks();
            aextendedblockstorage[b1] = extendedblockstorage;
        }

        chunk.setStorageArrays(aextendedblockstorage);

        if (par2NBTTagCompound.hasKey("Biomes"))
        {
            chunk.setBiomeArray(par2NBTTagCompound.getByteArray("Biomes"));
        }

        // CraftBukkit start - end this method here and split off entity loading to another method
        return chunk;
    }

    public void loadEntities(Chunk chunk, NBTTagCompound par2NBTTagCompound, World par1World)
    {
        // CraftBukkit end
        NBTTagList nbttaglist = par2NBTTagCompound.getTagList("Entities");

        if (nbttaglist != null)
        {
            for (int i = 0; i < nbttaglist.tagCount(); ++i)
            {
                NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(i);
                Entity entity = EntityList.createEntityFromNBT(nbttagcompound1, par1World);
                chunk.hasEntities = true;

                if (entity != null)
                {
                    chunk.addEntity(entity);
                }
            }
        }

        NBTTagList nbttaglist1 = par2NBTTagCompound.getTagList("TileEntities");

        if (nbttaglist1 != null)
        {
            for (int j = 0; j < nbttaglist1.tagCount(); ++j)
            {
                NBTTagCompound nbttagcompound2 = (NBTTagCompound)nbttaglist1.tagAt(j);
                TileEntity tileentity = TileEntity.createAndLoadEntity(nbttagcompound2);

                if (tileentity != null)
                {
                    chunk.addTileEntity(tileentity);
                }
            }
        }

        if (par2NBTTagCompound.hasKey("TileTicks"))
        {
            NBTTagList nbttaglist2 = par2NBTTagCompound.getTagList("TileTicks");

            if (nbttaglist2 != null)
            {
                for (int k = 0; k < nbttaglist2.tagCount(); ++k)
                {
                    NBTTagCompound nbttagcompound3 = (NBTTagCompound)nbttaglist2.tagAt(k);
                    par1World.scheduleBlockUpdateFromLoad(nbttagcompound3.getInteger("x"), nbttagcompound3.getInteger("y"), nbttagcompound3.getInteger("z"), nbttagcompound3.getInteger("i"), nbttagcompound3.getInteger("t"));
                }
            }
        }

        //return chunk; // CraftBukkit
    }
}
