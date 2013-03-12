package net.minecraft.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.INpc;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet38EntityStatus;
import net.minecraft.network.packet.Packet54PlayNoteBlock;
import net.minecraft.network.packet.Packet60Explosion;
import net.minecraft.network.packet.Packet70GameEvent;
import net.minecraft.network.packet.Packet71Weather;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.SpawnListEntry;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.WorldGeneratorBonusChest;
import net.minecraft.world.storage.ISaveHandler;

import net.minecraftforge.common.ChestGenHooks;
import static net.minecraftforge.common.ChestGenHooks.*;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
// CraftBukkit start
import gnu.trove.iterator.TLongShortIterator;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.TileEntityRecordPlayer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.tileentity.TileEntitySign;

import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.entity.CraftLightningStrike;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldServer extends World implements org.bukkit.BlockChangeDelegate // CraftBukkit
{
    private final MinecraftServer mcServer;
    public EntityTracker theEntityTracker; // CraftBukkit - private final -> public
    private final PlayerManager thePlayerManager;
    private LongObjectHashMap<Set<NextTickListEntry>> field_73064_N; // CraftBukkit - change to something chunk friendly

    /** All work to do in future ticks. */
    private TreeSet pendingTickListEntries;
    public ChunkProviderServer theChunkProviderServer;

    /** set by CommandServerSave{all,Off,On} */
    public boolean canNotSave;

    /** is false if there are no players */
    public boolean allPlayersSleeping;
    private int updateEntityTick = 0;
    private final Teleporter field_85177_Q;

    /**
     * Double buffer of ServerBlockEventList[] for holding pending BlockEventData's
     */
    private ServerBlockEventList[] blockEventCache = new ServerBlockEventList[] {new ServerBlockEventList((ServerBlockEvent)null), new ServerBlockEventList((ServerBlockEvent)null)};

    /**
     * The index into the blockEventCache; either 0, or 1, toggled in sendBlockEventPackets  where all BlockEvent are
     * applied locally and send to clients.
     */
    private int blockEventCacheIndex = 0;
    public static final WeightedRandomChestContent[] bonusChestContent = new WeightedRandomChestContent[] {new WeightedRandomChestContent(Item.stick.itemID, 0, 1, 3, 10), new WeightedRandomChestContent(Block.planks.blockID, 0, 1, 3, 10), new WeightedRandomChestContent(Block.wood.blockID, 0, 1, 3, 10), new WeightedRandomChestContent(Item.axeStone.itemID, 0, 1, 1, 3), new WeightedRandomChestContent(Item.axeWood.itemID, 0, 1, 1, 5), new WeightedRandomChestContent(Item.pickaxeStone.itemID, 0, 1, 1, 3), new WeightedRandomChestContent(Item.pickaxeWood.itemID, 0, 1, 1, 5), new WeightedRandomChestContent(Item.appleRed.itemID, 0, 2, 3, 5), new WeightedRandomChestContent(Item.bread.itemID, 0, 2, 3, 3)};

    /** An IntHashMap of entity IDs (integers) to their Entity objects. */
    private IntHashMap entityIdMap;

    public List<Teleporter> customTeleporters = new ArrayList<Teleporter>();

    // CraftBukkit start
    public final int dimension;

    public WorldServer(MinecraftServer minecraftserver, ISaveHandler idatamanager, String s, int i, WorldSettings worldsettings, Profiler methodprofiler, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen)
    {
        super(idatamanager, s, worldsettings, env.name().equals("MYST") ? WorldProvider.getProviderForDimension(i) : WorldProvider.getProviderForDimension(env.getId()), methodprofiler, gen, env);
        this.dimension = i;
        this.pvpMode = minecraftserver.isPVPEnabled();
        // CraftBukkit end
        this.mcServer = minecraftserver;
        this.theEntityTracker = new EntityTracker(this);
        this.thePlayerManager = new PlayerManager(this, getWorld().viewDistance); // Spigot

        if (this.entityIdMap == null)
        {
            this.entityIdMap = new IntHashMap();
        }

        if (this.field_73064_N == null)
        {
            this.field_73064_N = new LongObjectHashMap<Set<NextTickListEntry>>(); // CraftBukkit
        }

        if (this.pendingTickListEntries == null)
        {
            this.pendingTickListEntries = new TreeSet();
        }

        this.field_85177_Q = new org.bukkit.craftbukkit.CraftTravelAgent(this); // CraftBukkit
        DimensionManager.setWorld(i, this);
    }

    public WorldServer(MinecraftServer par1MinecraftServer, ISaveHandler par2ISaveHandler, String par3Str, int par4, WorldSettings par5WorldSettings, Profiler par6Profiler)
    {
        this(par1MinecraftServer, par2ISaveHandler, par3Str, par4, par5WorldSettings, par6Profiler, null, null); // MCPC+ wrapper to get CB support
    }

    // CraftBukkit start
    @Override
    public TileEntity getBlockTileEntity(int i, int j, int k)
    {
        TileEntity result = super.getBlockTileEntity(i, j, k);
        int type = getBlockId(i, j, k);

        if (type == Block.chest.blockID)
        {
            if (!(result instanceof TileEntityChest))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.stoneOvenIdle.blockID)
        {
            if (!(result instanceof TileEntityFurnace))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.dispenser.blockID)
        {
            if (!(result instanceof TileEntityDispenser))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.jukebox.blockID)
        {
            if (!(result instanceof TileEntityRecordPlayer))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.music.blockID)
        {
            if (!(result instanceof TileEntityNote))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.mobSpawner.blockID)
        {
            if (!(result instanceof TileEntityMobSpawner))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if ((type == Block.signPost.blockID) || (type == Block.signWall.blockID))
        {
            if (!(result instanceof TileEntitySign))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }
        else if (type == Block.enderChest.blockID)
        {
            if (!(result instanceof TileEntityEnderChest))
            {
                result = fixTileEntity(i, j, k, type, result);
            }
        }

        return result;
    }

    private TileEntity fixTileEntity(int x, int y, int z, int type, TileEntity found)
    {
        getServer().getLogger().severe("Block at " + x + "," + y + "," + z + " is " + org.bukkit.Material.getMaterial(type).toString() + " but has " + found + ". " + "Bukkit will attempt to fix this, but there may be additional damage that we cannot recover.");

        if (Block.blocksList[type] instanceof BlockContainer)
        {
            TileEntity replacement = ((BlockContainer) Block.blocksList[type]).createNewTileEntity(this);
            setBlockTileEntity(x, y, z, replacement);
            return replacement;
        }
        else
        {
            getServer().getLogger().severe("Don't know how to fix for this type... Can't do anything! :(");
            return found;
        }
    }

    private boolean canSpawn(int x, int z)
    {
        if (this.generator != null)
        {
            return this.generator.canSpawn(this.getWorld(), x, z);
        }
        else
        {
            return this.provider.canCoordinateBeSpawn(x, z);
        }
    }
    // CraftBukkit end

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
        super.tick();

        if (this.getWorldInfo().isHardcoreModeEnabled() && this.difficultySetting < 3)
        {
            this.difficultySetting = 3;
        }

        this.provider.worldChunkMgr.cleanupCache();

        if (this.areAllPlayersAsleep())
        {
            boolean flag = false;

            if (this.spawnHostileMobs && this.difficultySetting >= 1)
            {
                ;
            }

            if (!flag)
            {
                long i = this.worldInfo.getWorldTime() + 24000L;
                this.worldInfo.setWorldTime(i - i % 24000L);
                this.wakeAllPlayers();
            }
        }

        this.theProfiler.startSection("mobSpawner");
        // CraftBukkit start - Only call spawner if we have players online and the world allows for mobs or animals
        long time = this.worldInfo.getWorldTotalTime();

        if (this.getGameRules().getGameRuleBooleanValue("doMobSpawning") && (this.spawnHostileMobs || this.spawnPeacefulMobs) && (this instanceof WorldServer && this.playerEntities.size() > 0))
        {
            SpawnerAnimals.findChunksForSpawning(this, this.spawnHostileMobs && (this.ticksPerMonsterSpawns != 0 && time % this.ticksPerMonsterSpawns == 0L), this.spawnPeacefulMobs && (this.ticksPerAnimalSpawns != 0 && time % this.ticksPerAnimalSpawns == 0L), this.worldInfo.getWorldTotalTime() % 400L == 0L);
        }

        // CraftBukkit end
        if (this.getWorld() != null) // MCPC+
            this.getWorld().processChunkGC(); // Spigot
        this.theProfiler.endStartSection("chunkSource");
        this.chunkProvider.unload100OldestChunks();
        int j = this.calculateSkylightSubtracted(1.0F);

        if (j != this.skylightSubtracted)
        {
            this.skylightSubtracted = j;
        }

        this.sendAndApplyBlockEvents();
        this.worldInfo.incrementTotalWorldTime(this.worldInfo.getWorldTotalTime() + 1L);
        this.worldInfo.setWorldTime(this.worldInfo.getWorldTime() + 1L);
        this.theProfiler.endStartSection("tickPending");
        this.tickUpdates(false);
        this.theProfiler.endStartSection("tickTiles");
        this.tickBlocksAndAmbiance();
        this.theProfiler.endStartSection("chunkMap");
        this.thePlayerManager.updatePlayerInstances();
        this.theProfiler.endStartSection("village");
        this.villageCollectionObj.tick();
        this.villageSiegeObj.tick();
        this.theProfiler.endStartSection("portalForcer");
        this.field_85177_Q.func_85189_a(this.getTotalWorldTime());
        for (Teleporter tele : customTeleporters)
        {
            tele.func_85189_a(getTotalWorldTime());
        }
        this.theProfiler.endSection();
        this.sendAndApplyBlockEvents();
        if (this.getWorld() != null) // MCPC+
            this.getWorld().processChunkGC(); // CraftBukkit
    }

    /**
     * only spawns creatures allowed by the chunkProvider
     */
    public SpawnListEntry spawnRandomCreature(EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4)
    {
        List list = this.getChunkProvider().getPossibleCreatures(par1EnumCreatureType, par2, par3, par4);
        return list != null && !list.isEmpty() ? (SpawnListEntry)WeightedRandom.getRandomItem(this.rand, list) : null;
    }

    /**
     * Updates the flag that indicates whether or not all players in the world are sleeping.
     */
    public void updateAllPlayersSleepingFlag()
    {
        this.allPlayersSleeping = !this.playerEntities.isEmpty();
        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer)iterator.next();

            if (!entityplayer.isPlayerSleeping() && !entityplayer.fauxSleeping)   // CraftBukkit
            {
                this.allPlayersSleeping = false;
                break;
            }
        }
    }

    protected void wakeAllPlayers()
    {
        this.allPlayersSleeping = false;
        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer)iterator.next();

            if (entityplayer.isPlayerSleeping())
            {
                entityplayer.wakeUpPlayer(false, false, true);
            }
        }

        this.resetRainAndThunder();
    }

    private void resetRainAndThunder()
    {
        // CraftBukkit start
        WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(weather);
        ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(thunder);

        if (!weather.isCancelled())
        {
            this.worldInfo.setRainTime(0);
            this.worldInfo.setRaining(false);
        }

        if (!thunder.isCancelled())
        {
            this.worldInfo.setThunderTime(0);
            this.worldInfo.setThundering(false);
        }

        // CraftBukkit end
    }

    public boolean areAllPlayersAsleep()
    {
        if (this.allPlayersSleeping && !this.isRemote)
        {
            Iterator iterator = this.playerEntities.iterator();
            // CraftBukkit - This allows us to assume that some people are in bed but not really, allowing time to pass in spite of AFKers
            boolean flag = false;
            EntityPlayer entityhuman;

            do
            {
                if (!iterator.hasNext())
                {
                    return flag; // CraftBukkit
                }

                entityhuman = (EntityPlayer) iterator.next();

                // CraftBukkit start
                if (entityhuman.isPlayerFullyAsleep())
                {
                    flag = true;
                }
            }
            while (entityhuman.isPlayerFullyAsleep() || entityhuman.fauxSleeping);

            // CraftBukkit end
            return false;
        }
        else
        {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * Sets a new spawn location by finding an uncovered block at a random (x,z) location in the chunk.
     */
    public void setSpawnLocation()
    {
        if (this.worldInfo.getSpawnY() <= 0)
        {
            this.worldInfo.setSpawnY(64);
        }

        int i = this.worldInfo.getSpawnX();
        int j = this.worldInfo.getSpawnZ();
        int k = 0;

        while (this.getFirstUncoveredBlock(i, j) == 0)
        {
            i += this.rand.nextInt(8) - this.rand.nextInt(8);
            j += this.rand.nextInt(8) - this.rand.nextInt(8);
            ++k;

            if (k == 10000)
            {
                break;
            }
        }

        this.worldInfo.setSpawnX(i);
        this.worldInfo.setSpawnZ(j);
    }

    /**
     * plays random cave ambient sounds and runs updateTick on random blocks within each chunk in the vacinity of a
     * player
     */
    protected void tickBlocksAndAmbiance()
    {
        // Spigot start
        this.aggregateTicks--;

        if (this.aggregateTicks != 0)
        {
            return;
        }

        aggregateTicks = this.getWorld().aggregateTicks;
        // Spigot end
        super.tickBlocksAndAmbiance();
        int i = 0;
        int j = 0;
        //Iterator iterator = this.activeChunkSet.iterator();

        // Spigot start
        for (TLongShortIterator iter = activeChunkSet.iterator(); iter.hasNext();)
        {
            iter.advance();
            long chunkCoord = iter.key();
            int chunkX = World.keyToX(chunkCoord);
            int chunkZ = World.keyToZ(chunkCoord);

            // If unloaded, or in procedd of being unloaded, drop it
            if ((!this.chunkExists(chunkX,  chunkZ)) || (this.theChunkProviderServer.chunksToUnload.contains(chunkX, chunkZ)))
            {
                iter.remove();
                continue;
            }

            int players = iter.value();
            // Spigot end
            int k = chunkX * 16;
            int l = chunkZ * 16;
            this.theProfiler.startSection("getChunk");
            Chunk chunk = this.getChunkFromChunkCoords(chunkX, chunkZ);
            this.moodSoundAndLightCheck(k, l, chunk);
            this.theProfiler.endStartSection("tickChunk");
            chunk.updateSkylight();
            this.theProfiler.endStartSection("thunder");
            int i1;
            int j1;
            int k1;
            int l1;

            if (provider.canDoLightning(chunk) && this.rand.nextInt(100000) == 0 && this.isRaining() && this.isThundering())
            {
                this.updateLCG = this.updateLCG * 3 + 1013904223;
                i1 = this.updateLCG >> 2;
                j1 = k + (i1 & 15);
                k1 = l + (i1 >> 8 & 15);
                l1 = this.getPrecipitationHeight(j1, k1);

                if (this.canLightningStrikeAt(j1, l1, k1))
                {
                    this.addWeatherEffect(new EntityLightningBolt(this, (double)j1, (double)l1, (double)k1));
                }
            }

            this.theProfiler.endStartSection("iceandsnow");
            int i2;

            if (provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0)
            {
                this.updateLCG = this.updateLCG * 3 + 1013904223;
                i1 = this.updateLCG >> 2;
                j1 = i1 & 15;
                k1 = i1 >> 8 & 15;
                l1 = this.getPrecipitationHeight(j1 + k, k1 + l);

                if (this.isBlockFreezableNaturally(j1 + k, l1 - 1, k1 + l))
                {
                    this.setBlockWithNotify(j1 + k, l1 - 1, k1 + l, Block.ice.blockID);
                }

                if (this.isRaining() && this.canSnowAt(j1 + k, l1, k1 + l))
                {
                    this.setBlockWithNotify(j1 + k, l1, k1 + l, Block.snow.blockID);
                }

                if (this.isRaining())
                {
                    BiomeGenBase biomegenbase = this.getBiomeGenForCoords(j1 + k, k1 + l);

                    if (biomegenbase.canSpawnLightningBolt())
                    {
                        i2 = this.getBlockId(j1 + k, l1 - 1, k1 + l);

                        if (i2 != 0)
                        {
                            Block.blocksList[i2].fillWithRain(this, j1 + k, l1 - 1, k1 + l);
                        }
                    }
                }
            }

            this.theProfiler.endStartSection("tickTiles");
            ExtendedBlockStorage[] aextendedblockstorage = chunk.getBlockStorageArray();
            j1 = aextendedblockstorage.length;

            for (k1 = 0; k1 < j1; ++k1)
            {
                ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[k1];

                if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick())
                {
                    for (int j2 = 0; j2 < 3; ++j2)
                    {
                        this.updateLCG = this.updateLCG * 3 + 1013904223;
                        i2 = this.updateLCG >> 2;
                        int k2 = i2 & 15;
                        int l2 = i2 >> 8 & 15;
                        int i3 = i2 >> 16 & 15;
                        int j3 = extendedblockstorage.getExtBlockID(k2, i3, l2);
                        ++j;
                        Block block = Block.blocksList[j3];

                        if (block != null && block.getTickRandomly())
                        {
                            ++i;
                            if (players < 1)
                            {
                                //grow fast if no players are in this chunk
                                this.growthOdds = modifiedOdds;
                            }
                            else
                            {
                                this.growthOdds = 100;
                            }
                            // Spigot end
                            block.updateTick(this, k2 + k, i3 + extendedblockstorage.getYLocation(), l2 + l, this.rand);
                        }
                    }
                }
            }

            this.theProfiler.endSection();
        }
    }

    /**
     * Schedules a tick to a block with a delay (Most commonly the tick rate)
     */
    public void scheduleBlockUpdate(int par1, int par2, int par3, int par4, int par5)
    {
        this.func_82740_a(par1, par2, par3, par4, par5, 0);
    }

    public void func_82740_a(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        NextTickListEntry nextticklistentry = new NextTickListEntry(par1, par2, par3, par4);
        boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
        byte b0 = isForced ? (byte)0 : 8;

        if (this.scheduledUpdatesAreImmediate && par4 > 0)
        {
            if (Block.blocksList[par4].func_82506_l())
            {
                if (this.checkChunksExist(nextticklistentry.xCoord - b0, nextticklistentry.yCoord - b0, nextticklistentry.zCoord - b0, nextticklistentry.xCoord + b0, nextticklistentry.yCoord + b0, nextticklistentry.zCoord + b0))
                {
                    int k1 = this.getBlockId(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                    if (k1 == nextticklistentry.blockID && k1 > 0)
                    {
                        Block.blocksList[k1].updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                    }
                }

                return;
            }

            par5 = 1;
        }

        if (this.checkChunksExist(par1 - b0, par2 - b0, par3 - b0, par1 + b0, par2 + b0, par3 + b0))
        {
            if (par4 > 0)
            {
                nextticklistentry.setScheduledTime((long)par5 + this.worldInfo.getWorldTotalTime());
                nextticklistentry.func_82753_a(par6);
            }

            /*if (!this.field_73064_N.contains(nextticklistentry))
            {
                this.field_73064_N.add(nextticklistentry);
                this.pendingTickListEntries.add(nextticklistentry);
            }*/
            addNextTickIfNeeded(nextticklistentry); // CraftBukkit
        }
    }

    /**
     * Schedules a block update from the saved information in a chunk. Called when the chunk is loaded.
     */
    public void scheduleBlockUpdateFromLoad(int par1, int par2, int par3, int par4, int par5)
    {
        NextTickListEntry nextticklistentry = new NextTickListEntry(par1, par2, par3, par4);

        if (par4 > 0)
        {
            nextticklistentry.setScheduledTime((long)par5 + this.worldInfo.getWorldTotalTime());
        }

        /*if (!this.field_73064_N.contains(nextticklistentry))
        {
            this.field_73064_N.add(nextticklistentry);
            this.pendingTickListEntries.add(nextticklistentry);
        }*/
        addNextTickIfNeeded(nextticklistentry); // CraftBukkit
    }

    /**
     * Updates (and cleans up) entities and tile entities
     */
    public void updateEntities()
    {
        if (this.playerEntities.isEmpty() && getPersistentChunks().isEmpty())
        {
            if (this.updateEntityTick++ >= 1200)
            {
                return;
            }
        }
        else
        {
            this.resetUpdateEntityTick();
        }

        super.updateEntities();
    }

    /**
     * Resets the updateEntityTick field to 0
     */
    public void resetUpdateEntityTick()
    {
        this.updateEntityTick = 0;
    }

    /**
     * Runs through the list of updates to run and ticks them
     */
    public boolean tickUpdates(boolean par1)
    {
        int i = this.pendingTickListEntries.size();

        //if (i != this.L.size()) { // Spigot
        //    throw new IllegalStateException("TickNextTick list out of synch"); // Spigot
        //} else { // Spigot
        if (i > 1000)
        {
            // CraftBukkit start - if the server has too much to process over time, try to alleviate that
            if (i > 20 * 1000)
            {
                i = i / 20;
            }
            else
            {
                i = 1000;
            }

            // CraftBukkit end
        }

        for (int j = 0; j < i; ++j)
        {
            NextTickListEntry nextticklistentry = (NextTickListEntry) this.pendingTickListEntries.first();

            if (!par1 && nextticklistentry.scheduledTime > this.worldInfo.getWorldTotalTime())
            {
                break;
            }

            // Spigot start
            //this.pendingTickListEntries.remove(nextticklistentry);
            //this.field_73064_N.remove(nextticklistentry);
            this.removeNextTickIfNeeded(nextticklistentry);
            // Spigot end
            boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
            byte b0 = isForced ? (byte)0 : 8;

            if (this.checkChunksExist(nextticklistentry.xCoord - b0, nextticklistentry.yCoord - b0, nextticklistentry.zCoord - b0, nextticklistentry.xCoord + b0, nextticklistentry.yCoord + b0, nextticklistentry.zCoord + b0))
            {
                int k = this.getBlockId(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                if (k == nextticklistentry.blockID && k > 0)
                {
                    try
                    {
                            Block.blocksList[k].updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                    }
                    catch (Throwable throwable)
                    {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while ticking a block");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being ticked");
                        int l;

                        try
                        {
                            l = this.getBlockMetadata(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);
                        }
                        catch (Throwable throwable1)
                        {
                            l = -1;
                        }

                        CrashReportCategory.func_85068_a(crashreportcategory, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, k, l);
                        throw new ReportedException(crashreport);
                    }
                }
            }
        }

        return !this.pendingTickListEntries.isEmpty();
    }

    public List getPendingBlockUpdates(Chunk par1Chunk, boolean par2)
    {
        return this.getNextTickEntriesForChunk(par1Chunk, par2); // Spigot
        /* Spigot start
        ArrayList arraylist = null;
        ChunkCoordIntPair chunkcoordintpair = par1Chunk.getChunkCoordIntPair();
        int i = chunkcoordintpair.chunkXPos << 4;
        int j = i + 16;
        int k = chunkcoordintpair.chunkZPos << 4;
        int l = k + 16;
        Iterator iterator = this.pendingTickListEntries.iterator();

        while (iterator.hasNext())
        {
            NextTickListEntry nextticklistentry = (NextTickListEntry)iterator.next();

            if (nextticklistentry.xCoord >= i && nextticklistentry.xCoord < j && nextticklistentry.zCoord >= k && nextticklistentry.zCoord < l)
            {
                if (par2)
                {
                    this.field_73064_N.remove(nextticklistentry);
                    iterator.remove();
                }

                if (arraylist == null)
                {
                    arraylist = new ArrayList();
                }

                arraylist.add(nextticklistentry);
            }
        }

        return arraylist;
        // Spigot end */
    }

    /**
     * Will update the entity in the world if the chunk the entity is in is currently loaded or its forced to update.
     * Args: entity, forceUpdate
     */
    public void updateEntityWithOptionalForce(Entity par1Entity, boolean par2)
    {
        /* CraftBukkit start - We prevent spawning in general, so this butchering is not needed
        if (!this.server.getSpawnAnimals() && (entity instanceof EntityAnimal || entity instanceof EntityWaterAnimal)) {
            entity.die();
        }
        // CraftBukkit end */
        if (!this.mcServer.getCanSpawnNPCs() && par1Entity instanceof INpc)
        {
            par1Entity.setDead();
        }

        if (!(par1Entity.riddenByEntity instanceof EntityPlayer))
        {
            super.updateEntityWithOptionalForce(par1Entity, par2);
        }
    }

    /**
     * direct call to super.updateEntityWithOptionalForce
     */
    public void uncheckedUpdateEntity(Entity par1Entity, boolean par2)
    {
        super.updateEntityWithOptionalForce(par1Entity, par2);
    }

    /**
     * Creates the chunk provider for this world. Called in the constructor. Retrieves provider from worldProvider?
     */
    protected IChunkProvider createChunkProvider()
    {
        IChunkLoader ichunkloader = this.saveHandler.getChunkLoader(this.provider);
        // CraftBukkit start
        org.bukkit.craftbukkit.generator.InternalChunkGenerator gen;

        if (this.generator != null)
        {
            gen = new org.bukkit.craftbukkit.generator.CustomChunkGenerator(this, this.getSeed(), this.generator);
        }
        else if (this.provider instanceof WorldProviderHell)
        {
            gen = new org.bukkit.craftbukkit.generator.NetherChunkGenerator(this, this.getSeed());
        }
        else if (this.provider instanceof WorldProviderEnd)
        {
            gen = new org.bukkit.craftbukkit.generator.SkyLandsChunkGenerator(this, this.getSeed());
        }
        else
        {
            gen = new org.bukkit.craftbukkit.generator.NormalChunkGenerator(this, this.getSeed());
        }

        this.theChunkProviderServer = new ChunkProviderServer(this, ichunkloader, gen);
        // CraftBukkit end
        return this.theChunkProviderServer;
    }

    /**
     * pars: min x,y,z , max x,y,z
     */
    public List getAllTileEntityInBox(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        ArrayList arraylist = new ArrayList();

        for (int x = (par1 >> 4); x <= (par4 >> 4); x++)
        {
            for (int z = (par3 >> 4); z <= (par6 >> 4); z++)
            {
                Chunk chunk = getChunkFromChunkCoords(x, z);
                if (chunk != null)
                {
                    for (Object obj : chunk.chunkTileEntityMap.values())
                    {
                        TileEntity entity = (TileEntity)obj;
                        if (!entity.isInvalid())
                        {
                            if (entity.xCoord >= par1 && entity.yCoord >= par2 && entity.zCoord >= par3 &&
                                    entity.xCoord <= par4 && entity.yCoord <= par5 && entity.zCoord <= par6)
                            {
                                arraylist.add(entity);
                            }
                        }
                    }
                }
            }
        }
        return arraylist;
    }

    /**
     * Called when checking if a certain block can be mined or not. The 'spawn safe zone' check is located here.
     */
    public boolean canMineBlock(EntityPlayer par1EntityPlayer, int par2, int par3, int par4)
    {
        return super.canMineBlock(par1EntityPlayer, par2, par3, par4);
    }

    public boolean canMineBlockBody(EntityPlayer par1EntityPlayer, int par2, int par3, int par4)
    {
        int l = MathHelper.abs_int(par2 - this.worldInfo.getSpawnX());
        int i1 = MathHelper.abs_int(par4 - this.worldInfo.getSpawnZ());

        if (l > i1)
        {
            i1 = l;
        }

        // CraftBukkit - Configurable spawn protection
        return i1 > this.getServer().getSpawnRadius() || this.mcServer.getConfigurationManager().areCommandsAllowed(par1EntityPlayer.username) || this.mcServer.isSinglePlayer();
    }

    protected void initialize(WorldSettings par1WorldSettings)
    {
        if (this.entityIdMap == null)
        {
            this.entityIdMap = new IntHashMap();
        }

        if (this.field_73064_N == null)
        {
            this.field_73064_N = new LongObjectHashMap<Set<NextTickListEntry>>();
        }

        if (this.pendingTickListEntries == null)
        {
            this.pendingTickListEntries = new TreeSet();
        }

        this.createSpawnPosition(par1WorldSettings);
        super.initialize(par1WorldSettings);
    }

    /**
     * creates a spawn position at random within 256 blocks of 0,0
     */
    protected void createSpawnPosition(WorldSettings par1WorldSettings)
    {
        if (!this.provider.canRespawnHere())
        {
            this.worldInfo.setSpawnPosition(0, this.provider.getAverageGroundLevel(), 0);
        }
        else
        {
            this.findingSpawnPoint = true;
            WorldChunkManager worldchunkmanager = this.provider.worldChunkMgr;
            List list = worldchunkmanager.getBiomesToSpawnIn();
            Random random = new Random(this.getSeed());
            ChunkPosition chunkposition = worldchunkmanager.findBiomePosition(0, 0, 256, list, random);
            int i = 0;
            int j = this.provider.getAverageGroundLevel();
            int k = 0;

            // CraftBukkit start
            if (this.generator != null)
            {
                Random rand = new Random(this.getSeed());
                org.bukkit.Location spawn = this.generator.getFixedSpawnLocation(((WorldServer) this).getWorld(), rand);

                if (spawn != null)
                {
                    if (spawn.getWorld() != ((WorldServer) this).getWorld())
                    {
                        throw new IllegalStateException("Cannot set spawn point for " + this.worldInfo.getWorldName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                    }
                    else
                    {
                        this.worldInfo.setSpawnPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
                        this.findingSpawnPoint = false;
                        return;
                    }
                }
            }

            // CraftBukkit end

            if (chunkposition != null)
            {
                i = chunkposition.x;
                k = chunkposition.z;
            }
            else
            {
                System.out.println("Unable to find spawn biome");
            }

            int l = 0;

            while (!this.canSpawn(i, k))   // CraftBukkit - use our own canSpawn
            {
                i += random.nextInt(64) - random.nextInt(64);
                k += random.nextInt(64) - random.nextInt(64);
                ++l;

                if (l == 1000)
                {
                    break;
                }
            }

            this.worldInfo.setSpawnPosition(i, j, k);
            this.findingSpawnPoint = false;

            if (par1WorldSettings.isBonusChestEnabled())
            {
                this.createBonusChest();
            }
        }
    }

    /**
     * Creates the bonus chest in the world.
     */
    protected void createBonusChest()
    {
        WorldGeneratorBonusChest worldgeneratorbonuschest = new WorldGeneratorBonusChest(ChestGenHooks.getItems(BONUS_CHEST, rand), ChestGenHooks.getCount(BONUS_CHEST, rand));

        for (int i = 0; i < 10; ++i)
        {
            int j = this.worldInfo.getSpawnX() + this.rand.nextInt(6) - this.rand.nextInt(6);
            int k = this.worldInfo.getSpawnZ() + this.rand.nextInt(6) - this.rand.nextInt(6);
            int l = this.getTopSolidOrLiquidBlock(j, k) + 1;

            if (worldgeneratorbonuschest.generate(this, this.rand, j, l, k))
            {
                break;
            }
        }
    }

    /**
     * Gets the hard-coded portal location to use when entering this dimension.
     */
    public ChunkCoordinates getEntrancePortalLocation()
    {
        return this.provider.getEntrancePortalLocation();
    }

    /**
     * Saves all chunks to disk while updating progress bar.
     */
    public void saveAllChunks(boolean par1, IProgressUpdate par2IProgressUpdate) throws MinecraftException
    {
        if (this.chunkProvider.canSave())
        {
            if (par2IProgressUpdate != null)
            {
                par2IProgressUpdate.displayProgressMessage("Saving level");
            }

            this.saveLevel();

            if (par2IProgressUpdate != null)
            {
                par2IProgressUpdate.resetProgresAndWorkingMessage("Saving chunks");
            }

            this.chunkProvider.saveChunks(par1, par2IProgressUpdate);
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Save(this));
        }
    }

    /**
     * Saves the chunks to disk.
     */
    protected void saveLevel() throws MinecraftException
    {
        this.checkSessionLock();
        this.saveHandler.saveWorldInfoWithPlayer(this.worldInfo, this.mcServer.getConfigurationManager().getTagsFromLastWrite());
        this.mapStorage.saveAllData();
        this.perWorldStorage.saveAllData();
    }

    /**
     * Start the skin for this entity downloading, if necessary, and increment its reference counter
     */
    protected void obtainEntitySkin(Entity par1Entity)
    {
        super.obtainEntitySkin(par1Entity);
        this.entityIdMap.addKey(par1Entity.entityId, par1Entity);
        Entity[] aentity = par1Entity.getParts();

        if (aentity != null)
        {
            for (int i = 0; i < aentity.length; ++i)
            {
                this.entityIdMap.addKey(aentity[i].entityId, aentity[i]);
            }
        }
    }

    /**
     * Decrement the reference counter for this entity's skin image data
     */
    public void releaseEntitySkin(Entity par1Entity)
    {
        super.releaseEntitySkin(par1Entity);
        this.entityIdMap.removeObject(par1Entity.entityId);
        Entity[] aentity = par1Entity.getParts();

        if (aentity != null)
        {
            for (int i = 0; i < aentity.length; ++i)
            {
                this.entityIdMap.removeObject(aentity[i].entityId);
            }
        }
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this World.
     */
    public Entity getEntityByID(int par1)
    {
        return (Entity)this.entityIdMap.lookup(par1);
    }

    /**
     * adds a lightning bolt to the list of lightning bolts in this world.
     */
    public boolean addWeatherEffect(Entity par1Entity)
    {
        // MCPC+ start - vanilla compatibility
        if (par1Entity instanceof net.minecraft.entity.effect.EntityLightningBolt) 
        { 
            // CraftBukkit start
            LightningStrikeEvent lightning = new LightningStrikeEvent(this.getWorld(), (org.bukkit.entity.LightningStrike) par1Entity.getBukkitEntity());
            this.getServer().getPluginManager().callEvent(lightning);
    
            if (lightning.isCancelled())
            {
                return false;
            }
        }
        // MCPC+ end
        if (super.addWeatherEffect(par1Entity))
        {
            this.mcServer.getConfigurationManager().sendToAllNear(par1Entity.posX, par1Entity.posY, par1Entity.posZ, 512.0D, this.dimension, new Packet71Weather(par1Entity));
            // CraftBukkit end
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * sends a Packet 38 (Entity Status) to all tracked players of that entity
     */
    public void setEntityState(Entity par1Entity, byte par2)
    {
        Packet38EntityStatus packet38entitystatus = new Packet38EntityStatus(par1Entity.entityId, par2);
        this.getEntityTracker().sendPacketToAllAssociatedPlayers(par1Entity, packet38entitystatus);
    }

    /**
     * returns a new explosion. Does initiation (at time of writing Explosion is not finished)
     */
    public Explosion newExplosion(Entity par1Entity, double par2, double par4, double par6, float par8, boolean par9, boolean par10)
    {
        // CraftBukkit start
        Explosion explosion = super.newExplosion(par1Entity, par2, par4, par6, par8, par9, par10);

        if (explosion.wasCanceled)
        {
            return explosion;
        }

        /* Remove
        explosion.a = flag;
        explosion.b = flag1;
        explosion.a();
        explosion.a(false);
        */
        // CraftBukkit end - TODO: Check if explosions are still properly implemented

        if (!par10)
        {
            explosion.affectedBlockPositions.clear();
        }

        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer.getDistanceSq(par2, par4, par6) < 4096.0D)
            {
                ((EntityPlayerMP) entityplayer).playerNetServerHandler.sendPacketToPlayer(new Packet60Explosion(par2, par4, par6, par8, explosion.affectedBlockPositions, (Vec3) explosion.func_77277_b().get(entityplayer)));
            }
        }

        return explosion;
    }

    /**
     * Adds a block event with the given Args to the blockEventCache. During the next tick(), the block specified will
     * have its onBlockEvent handler called with the given parameters. Args: X,Y,Z, BlockID, EventID, EventParameter
     */
    public void addBlockEvent(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        BlockEventData blockeventdata = new BlockEventData(par1, par2, par3, par4, par5, par6);
        Iterator iterator = this.blockEventCache[this.blockEventCacheIndex].iterator();
        BlockEventData blockeventdata1;

        do
        {
            if (!iterator.hasNext())
            {
                this.blockEventCache[this.blockEventCacheIndex].add(blockeventdata);
                return;
            }

            blockeventdata1 = (BlockEventData)iterator.next();
        }
        while (!blockeventdata1.equals(blockeventdata));
    }

    /**
     * Send and apply locally all pending BlockEvents to each player with 64m radius of the event.
     */
    private void sendAndApplyBlockEvents()
    {
        while (!this.blockEventCache[this.blockEventCacheIndex].isEmpty())
        {
            int i = this.blockEventCacheIndex;
            this.blockEventCacheIndex ^= 1;
            Iterator iterator = this.blockEventCache[i].iterator();

            while (iterator.hasNext())
            {
                BlockEventData blockeventdata = (BlockEventData)iterator.next();

                if (this.onBlockEventReceived(blockeventdata))
                {
                    // CraftBukkit - this.worldProvider.dimension -> this.dimension
                    this.mcServer.getConfigurationManager().sendToAllNear((double) blockeventdata.getX(), (double) blockeventdata.getY(), (double) blockeventdata.getZ(), 64.0D, this.dimension, new Packet54PlayNoteBlock(blockeventdata.getX(), blockeventdata.getY(), blockeventdata.getZ(), blockeventdata.getBlockID(), blockeventdata.getEventID(), blockeventdata.getEventParameter()));
                }
            }

            this.blockEventCache[i].clear();
        }
    }

    /**
     * Called to apply a pending BlockEvent to apply to the current world.
     */
    private boolean onBlockEventReceived(BlockEventData par1BlockEventData)
    {
        int i = this.getBlockId(par1BlockEventData.getX(), par1BlockEventData.getY(), par1BlockEventData.getZ());

        if (i == par1BlockEventData.getBlockID())
        {
            Block.blocksList[i].onBlockEventReceived(this, par1BlockEventData.getX(), par1BlockEventData.getY(), par1BlockEventData.getZ(), par1BlockEventData.getEventID(), par1BlockEventData.getEventParameter());
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Syncs all changes to disk and wait for completion.
     */
    public void flush()
    {
        this.saveHandler.flush();
    }

    /**
     * Updates all weather states.
     */
    protected void updateWeather()
    {
        boolean flag = this.isRaining();
        super.updateWeather();

        if (flag != this.isRaining())
        {
            // CraftBukkit start - only sending weather packets to those affected
            for (int i = 0; i < this.playerEntities.size(); ++i)
            {
                if (((EntityPlayerMP) this.playerEntities.get(i)).worldObj == this)
                {
                    ((EntityPlayerMP) this.playerEntities.get(i)).playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(flag ? 2 : 1, 0));
                }
            }

            // CraftBukkit end
        }
    }

    /**
     * Gets the MinecraftServer.
     */
    public MinecraftServer getMinecraftServer()
    {
        return this.mcServer;
    }

    /**
     * Gets the EntityTracker
     */
    public EntityTracker getEntityTracker()
    {
        return this.theEntityTracker;
    }

    public PlayerManager getPlayerManager()
    {
        return this.thePlayerManager;
    }

    public Teleporter func_85176_s()
    {
        return this.field_85177_Q;
    }

    // Spigot start
    private void addNextTickIfNeeded(NextTickListEntry ent)
    {
        long coord = LongHash.toLong(ent.xCoord >> 4, ent.zCoord >> 4);
        Set<NextTickListEntry> chunkset = field_73064_N.get(coord);

        if (chunkset == null)
        {
            chunkset = new HashSet<NextTickListEntry>();
            field_73064_N.put(coord, chunkset);
        }
        else if (chunkset.contains(ent))
        {
            return;
        }

        chunkset.add(ent);
        pendingTickListEntries.add(ent);
    }

    private void removeNextTickIfNeeded(NextTickListEntry ent)
    {
        long coord = LongHash.toLong(ent.xCoord >> 4, ent.zCoord >> 4);
        Set<NextTickListEntry> chunkset = field_73064_N.get(coord);

        if (chunkset == null)
        {
            return;
        }

        if (chunkset.remove(ent))
        {
            pendingTickListEntries.remove(ent);

            if (chunkset.isEmpty())
            {
                field_73064_N.remove(coord);
            }
        }
    }

    private List<NextTickListEntry> getNextTickEntriesForChunk(Chunk chunk, boolean remove)
    {
        long coord = LongHash.toLong(chunk.xPosition, chunk.zPosition);
        Set<NextTickListEntry> chunkset = field_73064_N.get(coord);

        if (chunkset == null)
        {
            return null;
        }

        List<NextTickListEntry> list = new ArrayList<NextTickListEntry>(chunkset);

        if (remove)
        {
            field_73064_N.remove(coord);
            pendingTickListEntries.removeAll(list);
           chunkset.clear();
        }

        return list;
    }
    // Spigot end

    public File getChunkSaveLocation()
    {
        return ((AnvilChunkLoader)theChunkProviderServer.currentChunkLoader).chunkSaveLocation;
    }
    // MCPC start
    @Override
    public boolean setRawTypeId(int x, int y, int z, int typeId) {
        // TODO Auto-generated method stub
        return setBlock(x, y, z, typeId);
    }

    @Override
    public boolean setRawTypeIdAndData(int x, int y, int z, int typeId, int data) {
        // TODO Auto-generated method stub
        return setBlockAndMetadata(x, y, z, typeId, data);
    }

    @Override
    public boolean setTypeId(int x, int y, int z, int typeId) {
        // TODO Auto-generated method stub
        return setBlockWithNotify(x, y, z, typeId);
    }

    @Override
    public boolean setTypeIdAndData(int x, int y, int z, int typeId, int data) {
        // TODO Auto-generated method stub
        return setBlockAndMetadataWithNotify(x, y, z, typeId, data);
    }

    @Override
    public int getTypeId(int x, int y, int z) {
        // TODO Auto-generated method stub
        return getBlockId(x, y, z);
    }

    @Override
    public boolean isEmpty(int x, int y, int z) {
        // TODO Auto-generated method stub
        return isAirBlock(x, y, z);
    }
    // MCPC end
}
