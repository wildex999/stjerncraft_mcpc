package net.minecraft.world;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFluid;
import net.minecraft.block.BlockHalfSlab;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.command.IEntitySelector;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.logging.ILogAgent;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.ChunkSampler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Direction;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3Pool;
import net.minecraft.village.VillageCollection;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;

import com.google.common.collect.ImmutableSetMultimap;

import net.minecraftforge.common.*;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraft.entity.EnumCreatureType;
// CraftBukkit start
import net.minecraft.entity.item.EntityXPOrb;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.Spigot; // Spigot
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
// CraftBukkit end


// MCPC+ start
import w999.baseprotect.BaseProtect;
import w999.baseprotect.WorldInteract;
import w999.baseprotect.PlayerData;
import w999.thatlag.TimeWatch;
import net.minecraft.nbt.NBTTagCompound;
import za.co.mcportcentral.MCPCWorldConfig;
import za.co.mcportcentral.entity.CraftFakePlayer;
// MCPC+ end


public abstract class World implements IBlockAccess
{
    /**
     * Used in the getEntitiesWithinAABB functions to expand the search area for entities.
     * Modders should change this variable to a higher value if it is less then the radius
     * of one of there entities.
     */
    public static double MAX_ENTITY_RADIUS = 2.0D;

    public final MapStorage perWorldStorage;
    
    public static WorldInteract currentTickItem; //MCPC+ - Current ticking item(Block, Entity, tileentity) TODO:Make NOT static
    public static Player mcFakePlayer; //Cache of [MineCraft] fake player
    /**
     * boolean; if true updates scheduled by scheduleBlockUpdate happen immediately
     */
    public boolean scheduledUpdatesAreImmediate = false;

    /** A list of all Entities in all currently-loaded chunks */
    public List loadedEntityList = new ArrayList();
    protected List unloadedEntityList = new ArrayList();

    /** A list of all TileEntities in all currently-loaded chunks */
    public List loadedTileEntityList = new ArrayList(); // CraftBukkit - ArrayList -> HashSet // MCPC+ - keep vanilla for mod compatibility
    private List addedTileEntityList = new ArrayList();

    /** Entities marked for removal. */
    private List entityRemoval = new ArrayList();

    /** Array list of players in the world. */
    public List playerEntities = new ArrayList();

    /** a list of all the lightning entities */
    public List weatherEffects = new ArrayList();
    private long cloudColour = 16777215L;

    /** How much light is subtracted from full daylight */
    public int skylightSubtracted = 0;

    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C
     * value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a
     * 16x128x16 field.
     */
    protected int updateLCG = (new Random()).nextInt();

    /**
     * magic number used to generate fast random numbers for 3d distribution within a chunk
     */
    protected final int DIST_HASH_MAGIC = 1013904223;
    public float prevRainingStrength;
    public float rainingStrength;
    public float prevThunderingStrength;
    public float thunderingStrength;

    /**
     * Set to 2 whenever a lightning bolt is generated in SSP. Decrements if > 0 in updateWeather(). Value appears to be
     * unused.
     */
    public int lastLightningBolt = 0;
    public boolean callingPlaceEvent = false; // CraftBukkit

    /** Option > Difficulty setting (0 - 3) */
    public int difficultySetting;

    /** RNG for World. */
    public Random rand = new Random();

    /** The WorldProvider instance that World uses. */
    public WorldProvider provider; // CraftBukkit - remove final
    protected List worldAccesses = new ArrayList();

    /** Handles chunk operations and caching */
    public IChunkProvider chunkProvider; // CraftBukkit - protected -> public
    protected final ISaveHandler saveHandler;

    /**
     * holds information about a world (size on disk, time, spawn point, seed, ...)
     */
    public WorldInfo worldInfo; // CraftBukkit - protected -> public
    
    /** ThatLag, remaining and max to update */
    public double remainingTileEntityCount = 0; //How many left to update this tick
    public double remainingEntityCount = 0;
    public double actualTileEntityCount = 0; //Set by world to the actual number of tileentities updated(Used in next time calculation)
    public double actualEntityCount = 0;
    public int currentTileEntityIndex = 0; //Current entity index, used for setting where to continue on each tick
    public int currentEntityIndex = 0;
    

    /** Boolean that is set to true when trying to find a spawn point */
    public boolean findingSpawnPoint;
    public MapStorage mapStorage;
    public VillageCollection villageCollectionObj;
    protected final VillageSiege villageSiegeObj = new VillageSiege(this);
    public final Profiler theProfiler;

    /** The world-local pool of vectors */
    private final Vec3Pool vecPool = new Vec3Pool(300, 2000);
    private final Calendar theCalendar = Calendar.getInstance();
    public Scoreboard worldScoreboard = new Scoreboard(); // CraftBukkit - protected -> public

    /** The log agent for this world. */
    private final ILogAgent worldLogAgent;
    private ArrayList collidingBoundingBoxes = new ArrayList();    
    private boolean scanningTileEntities;
    // CraftBukkit start - public, longhashset

    /** indicates if enemies are spawned or not */
    public boolean spawnHostileMobs = true;

    /** A flag indicating whether we should spawn peaceful mobs. */
    public boolean spawnPeacefulMobs = true;

    /** Positions to update */
    protected gnu.trove.map.hash.TLongShortHashMap activeChunkSet_CB; // Spigot
    public Set activeChunkSet = new HashSet(); // vanilla compatibility
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    // CraftBukkit end

    /** number of ticks until the next random ambients play */
    private int ambientTickCountdown;

    /**
     * is a temporary list of blocks and light values used when updating light levels. Holds up to 32x32x32 blocks (the
     * maximum influence of a light source.) Every element is a packed bit value: 0000000000LLLLzzzzzzyyyyyyxxxxxx. The
     * 4-bit L is a light level used when darkening blocks. 6-bit numbers x, y and z represent the block's offset from
     * the original block, plus 32 (i.e. value of 31 would mean a -1 offset
     */
    int[] lightUpdateBlockList;

    /** This is set to true for client worlds, and false for server worlds. */
    public boolean isRemote;
    // MCPC+ start - block place
    /** These hit coords are set by ItemBlock.onItemUse and are only used during a forge block place event in canPlaceEntityOnSide */
    public float curPlacedItemHitX = 0;
    public float curPlacedItemHitY = 0;
    public float curPlacedItemHitZ = 0;
    // MCPC+ end

    // MCPC+ start - preload world crash report classes to fix NCDFE masking StackOverflow/memory error, see #721
    private static boolean preloadedCrashClasses = false;
    {
        if (!preloadedCrashClasses)
        {
            // generate a temporary crash report
            Throwable throwable = new Throwable();
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");

            // loads all the required classes - including net.minecraft.crash.CallableBlockType (package private)
            crashreportcategory.addCrashSectionCallable("Source block type", (Callable)(new CallableLvl1(this, 0)));
            CrashReportCategory.func_85068_a(crashreportcategory, 0, 0, 0, 0, -1);

            preloadedCrashClasses = true;
        }
    }
    // MCPC+ end
    // Spigot start

    public static long chunkToKey(int x, int z)
    {
        long k = ((((long)x) & 0xFFFF0000L) << 16) | ((((long)x) & 0x0000FFFFL) << 0);
        k |= ((((long)z) & 0xFFFF0000L) << 32) | ((((long)z) & 0x0000FFFFL) << 16);
        return k;
    }
    public static int keyToX(long k)
    {
        return (int)(((k >> 16) & 0xFFFF0000) | (k & 0x0000FFFF));
    }
    public static int keyToZ(long k)
    {
        return (int)(((k >> 32) & 0xFFFF0000L) | ((k >> 16) & 0x0000FFFF));
    }
    // Spigot end

    /**
     * Gets the biome for a given set of x/z coordinates
     */
    public BiomeGenBase getBiomeGenForCoords(int par1, int par2)
    {
        return provider.getBiomeGenForCoords(par1, par2);
    }

    public BiomeGenBase getBiomeGenForCoordsBody(int par1, int par2)
    {
        if (this.blockExists(par1, 0, par2))
        {
            Chunk chunk = this.getChunkFromBlockCoords(par1, par2);

            if (chunk != null)
            {
                return chunk.getBiomeGenForWorldCoords(par1 & 15, par2 & 15, this.provider.worldChunkMgr);
            }
        }

        return this.provider.worldChunkMgr.getBiomeGenAt(par1, par2);
    }

    public WorldChunkManager getWorldChunkManager()
    {
        return this.provider.worldChunkMgr;
    }

    // CraftBukkit start
    private final CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public ChunkGenerator generator;
    Chunk lastChunkAccessed;
    int lastXAccessed = Integer.MIN_VALUE;
    int lastZAccessed = Integer.MIN_VALUE;
    final Object chunkLock = new Object();
    private byte chunkTickRadius; // Spigot
    public final za.co.mcportcentral.MCPCWorldConfig mcpcConfig; // MCPC+

    public final SpigotTimings.WorldTimingsHandler timings; // Spigot

    public CraftWorld getWorld()
    {
        return this.world;
    }

    public CraftServer getServer()
    {
        return (CraftServer) Bukkit.getServer();
    }

    // Changed signature
    public World(ISaveHandler idatamanager, String s, WorldSettings worldsettings, WorldProvider worldprovider, Profiler profiler, ILogAgent ilogagent, ChunkGenerator gen, org.bukkit.World.Environment env)
    {
        this.mcpcConfig = new za.co.mcportcentral.MCPCWorldConfig( s ); // MCPC+;
        this.generator = gen;
        this.worldInfo = idatamanager.loadWorldInfo(); // Spigot
        this.world = new CraftWorld((WorldServer) this, gen, env);
        this.ticksPerAnimalSpawns = this.getServer().getTicksPerAnimalSpawns(); // CraftBukkit
        this.ticksPerMonsterSpawns = this.getServer().getTicksPerMonsterSpawns(); // CraftBukkit
        this.chunkTickRadius = (byte)((this.getServer().getViewDistance() < 7) ? this.getServer().getViewDistance() : 7); // CraftBukkit - don't tick chunks we don't load for player
        // CraftBukkit end
        // Spigot start
        activeChunkSet_CB = new gnu.trove.map.hash.TLongShortHashMap(world.growthPerTick * 5, 0.7f, Long.MIN_VALUE, Short.MIN_VALUE);
        activeChunkSet_CB.setAutoCompactionFactor(0);
        // Spigot end
        this.ambientTickCountdown = this.rand.nextInt(12000);
        this.lightUpdateBlockList = new int[32768];
        this.isRemote = false;
        this.saveHandler = idatamanager;
        this.theProfiler = profiler;
        // MCPC+ start
        // Provides a solution for different worlds getting different copies of the same data, potentially rewriting the data or causing race conditions/stale data
        // Buildcraft has suffered from the issue this fixes.  If you load the same data from two different worlds they can get two different copies of the same object, thus the last saved gets final say.
        if (DimensionManager.getWorld(0) != null) // if overworld has loaded, use its mapstorage
        {
            this.mapStorage = DimensionManager.getWorld(0).mapStorage;
        }
        else // if we are loading overworld, create a new mapstorage
        {
            this.mapStorage = new MapStorage(idatamanager);
        }
        // MCPC+ end
        this.worldLogAgent = ilogagent;
        // this.worldInfo = idatamanager.loadWorldInfo(); // Spigot - Moved up

        if (worldprovider != null)
        {
            this.provider = worldprovider;
        }
        else if (this.worldInfo != null && this.worldInfo.getDimension() != 0)
        {
            this.provider = WorldProvider.getProviderForDimension(this.worldInfo.getDimension());
        }
        else
        {
            this.provider = WorldProvider.getProviderForDimension(0);
        }

        if (this.worldInfo == null)
        {
            this.worldInfo = new WorldInfo(worldsettings, s);
            this.worldInfo.setDimension(this.provider.dimensionId); // MCPC+ - Save dimension to level.dat
        }
        else
        {
            this.worldInfo.setWorldName(s);
            // MCPC+ start - Use saved dimension from level.dat. Fixes issues with MultiVerse
            if (this.worldInfo.getDimension() != 0)
                this.provider.dimensionId = this.worldInfo.getDimension();
            else
            {
                this.worldInfo.setDimension(this.provider.dimensionId);
            }
            // MCPC+ end
        }

        // MCPC+ start - Guarantee provider dimension is not reset. This is required for mods that rely on the provider ID to match the client dimension. Without this, IC2 will send the wrong ID to clients.
        int providerId = this.provider.dimensionId;
        this.provider.registerWorld(this);
        this.provider.dimensionId = providerId;
        // MCPC+ end
        this.chunkProvider = this.createChunkProvider();

        if (!this.worldInfo.isInitialized())
        {
            try
            {
                this.initialize(worldsettings);
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception initializing level");

                try
                {
                    this.addWorldInfoToCrashReport(crashreport);
                }
                catch (Throwable throwable1)
                {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            this.worldInfo.setServerInitialized(true);
        }

        if (this instanceof WorldServer)
        {
            this.perWorldStorage = new MapStorage(new WorldSpecificSaveHandler((WorldServer)this, idatamanager));
        }
        else
        {
            this.perWorldStorage = new MapStorage((ISaveHandler)null);
        }
        VillageCollection villagecollection = (VillageCollection)perWorldStorage.loadData(VillageCollection.class, "villages");

        if (villagecollection == null)
        {
            this.villageCollectionObj = new VillageCollection(this);
            this.perWorldStorage.setData("villages", this.villageCollectionObj);
        }
        else
        {
            this.villageCollectionObj = villagecollection;
            this.villageCollectionObj.func_82566_a(this);
        }

        this.calculateInitialSkylight();
        this.calculateInitialWeather();
        this.getServer().addWorld(this.world); // CraftBukkit
        timings = new SpigotTimings.WorldTimingsHandler(this); // Spigot
    }
    
    public World(ISaveHandler par1ISaveHandler, String par2Str, WorldSettings par3WorldSettings, WorldProvider par4WorldProvider, Profiler par5Profiler, ILogAgent par6ILogAgent)
    {
        this.mcpcConfig = new za.co.mcportcentral.MCPCWorldConfig( par2Str ); // MCPC+
        this.world = null; // CraftWorld not used
        this.ambientTickCountdown = this.rand.nextInt(12000);
        this.lightUpdateBlockList = new int[32768];
        this.isRemote = false;
        this.saveHandler = par1ISaveHandler;
        this.theProfiler = par5Profiler;
        // MCPC+ start
        // Provides a solution for different worlds getting different copies of the same data, potentially rewriting the data or causing race conditions/stale data
        // Buildcraft has suffered from the issue this fixes.  If you load the same data from two different worlds they can get two different copies of the same object, thus the last saved gets final say.
        if (DimensionManager.getWorld(0) != null) // if overworld has loaded, use its mapstorage
        {
            this.mapStorage = DimensionManager.getWorld(0).mapStorage;
        }
        else // if we are loading overworld, create a new mapstorage
        {
            this.mapStorage = new MapStorage(par1ISaveHandler);
        }
        // MCPC+ end
        this.worldLogAgent = par6ILogAgent;
        this.worldInfo = par1ISaveHandler.loadWorldInfo();

        if (par4WorldProvider != null)
        {
            this.provider = par4WorldProvider;
        }
        else if (this.worldInfo != null && this.worldInfo.getDimension() != 0)
        {
            this.provider = WorldProvider.getProviderForDimension(this.worldInfo.getDimension());
        }
        else
        {
            this.provider = WorldProvider.getProviderForDimension(0);
        }

        if (this.worldInfo == null)
        {
            this.worldInfo = new WorldInfo(par3WorldSettings, par2Str);
        }
        else
        {
            this.worldInfo.setWorldName(par2Str);
        }

        this.provider.registerWorld(this);
        this.chunkProvider = this.createChunkProvider();

        if (!this.worldInfo.isInitialized())
        {
            try
            {
                this.initialize(par3WorldSettings);
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception initializing level");

                try
                {
                    this.addWorldInfoToCrashReport(crashreport);
                }
                catch (Throwable throwable1)
                {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            this.worldInfo.setServerInitialized(true);
        }

        if (this instanceof WorldServer)
        {
            this.perWorldStorage = new MapStorage(new WorldSpecificSaveHandler((WorldServer)this, par1ISaveHandler));
        }
        else
        {
            this.perWorldStorage = new MapStorage((ISaveHandler)null);
        }
        VillageCollection villagecollection = (VillageCollection)perWorldStorage.loadData(VillageCollection.class, "villages");

        if (villagecollection == null)
        {
            this.villageCollectionObj = new VillageCollection(this);
            this.perWorldStorage.setData("villages", this.villageCollectionObj);
        }
        else
        {
            this.villageCollectionObj = villagecollection;
            this.villageCollectionObj.func_82566_a(this);
        }

        this.calculateInitialSkylight();
        this.calculateInitialWeather();
        timings = new SpigotTimings.WorldTimingsHandler(this); // Spigot
    }
    // MCPC+ end    

    private static MapStorage s_mapStorage;
    private static ISaveHandler s_savehandler;
    //Provides a solution for different worlds getting different copies of the same data, potentially rewriting the data or causing race conditions/stale data
    //Buildcraft has suffered from the issue this fixes.  If you load the same data from two different worlds they can get two different copies of the same object, thus the last saved gets final say.
    private MapStorage getMapStorage(ISaveHandler savehandler)
    {
        if (s_savehandler != savehandler || s_mapStorage == null) {
            s_mapStorage = new MapStorage(savehandler);
            s_savehandler = savehandler;
        }
        return s_mapStorage;
    }

    /**
     * Creates the chunk provider for this world. Called in the constructor. Retrieves provider from worldProvider?
     */
    protected abstract IChunkProvider createChunkProvider();

    protected void initialize(WorldSettings par1WorldSettings)
    {
        this.worldInfo.setServerInitialized(true);
    }

    /**
     * Returns the block ID of the first block at this (x,z) location with air above it, searching from sea level
     * upwards.
     */
    public int getFirstUncoveredBlock(int par1, int par2)
    {
        int k;

        for (k = 63; !this.isAirBlock(par1, k + 1, par2); ++k)
        {
            ;
        }

        return this.getBlockId(par1, k, par2);
    }

    /**
     * Returns the block ID at coords x,y,z
     */
    private int prevX = 0;
    private int prevZ = 0;
    private Chunk prevChunk = null;
    
    public int getBlockId(int par1, int par2, int par3)
    {
    	//Quick out(Avoid the other if's and method invocation)
    	if(par1 == prevX && par3 == prevZ)
    		return prevChunk.getBlockID(par1 & 15, par2, par3 & 15);
    	
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (par2 < 0 || par2 >= 256)
            {
                return 0;
            }
            else
            {
                Chunk chunk = null;

                try
                {
                    chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
                    prevX = par1;
                    prevZ = par3;
                    prevChunk = chunk;
                    return chunk.getBlockID(par1 & 15, par2, par3 & 15);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception getting block type in world");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Requested block coordinates");
                    crashreportcategory.addCrashSection("Found chunk", Boolean.valueOf(chunk == null));
                    crashreportcategory.addCrashSection("Location", CrashReportCategory.getLocationInfo(par1, par2, par3));
                    throw new ReportedException(crashreport);
                }
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns true if the block at the specified coordinates is empty
     */
    public boolean isAirBlock(int par1, int par2, int par3)
    {
        int id = getBlockId(par1, par2, par3);
        return id == 0 || Block.blocksList[id] == null || Block.blocksList[id].isAirBlock(this, par1, par2, par3);
    }

    /**
     * Checks if a block at a given position should have a tile entity.
     */
    public boolean blockHasTileEntity(int par1, int par2, int par3)
    {
        int l = this.getBlockId(par1, par2, par3);
        int meta = this.getBlockMetadata(par1, par2, par3);
        return Block.blocksList[l] != null && Block.blocksList[l].hasTileEntity(meta);
    }

    /**
     * Returns the render type of the block at the given coordinate.
     */
    public int blockGetRenderType(int par1, int par2, int par3)
    {
        int l = this.getBlockId(par1, par2, par3);
        return Block.blocksList[l] != null ? Block.blocksList[l].getRenderType() : -1;
    }

    /**
     * Returns whether a block exists at world coordinates x, y, z
     */
    public boolean blockExists(int par1, int par2, int par3)
    {
        return par2 >= 0 && par2 < 256 ? this.chunkExists(par1 >> 4, par3 >> 4) : false;
    }

    /**
     * Checks if any of the chunks within distance (argument 4) blocks of the given block exist
     */
    public boolean doChunksNearChunkExist(int par1, int par2, int par3, int par4)
    {
        return this.checkChunksExist(par1 - par4, par2 - par4, par3 - par4, par1 + par4, par2 + par4, par3 + par4);
    }

    /**
     * Checks between a min and max all the chunks inbetween actually exist. Args: minX, minY, minZ, maxX, maxY, maxZ
     */
    public boolean checkChunksExist(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        if (par5 >= 0 && par2 < 256)
        {
            par1 >>= 4;
            par3 >>= 4;
            par4 >>= 4;
            par6 >>= 4;

            for (int k1 = par1; k1 <= par4; ++k1)
            {
                for (int l1 = par3; l1 <= par6; ++l1)
                {
                    // CraftBukkit - check unload queue too so we don't leak a chunk
                    if (!this.chunkExists(k1, l1) || ((WorldServer) this).theChunkProviderServer.chunksToUnload.contains(k1, l1))
                    {
                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns whether a chunk exists at chunk coordinates x, y
     */
    public boolean chunkExists(int par1, int par2) // MCPC+ - protected -> public for repackaging
    {
        return this.chunkProvider.chunkExists(par1, par2);
    }

    /**
     * Returns a chunk looked up by block coordinates. Args: x, z
     */
    public Chunk getChunkFromBlockCoords(int par1, int par2)
    {
        return this.getChunkFromChunkCoords(par1 >> 4, par2 >> 4);
    }

    // CraftBukkit start

    /**
     * Returns back a chunk looked up by chunk coordinates Args: x, y
     */
    public Chunk getChunkFromChunkCoords(int par1, int par2)
    {
        // Spigot start - Alternate, sync-free-but-safe chunk reference cache
        //synchronized (this.chunkLock) {
        Chunk result = this.lastChunkAccessed; // Exploit fact that read is atomic

        if (result == null || result.xPosition != par1 || result.zPosition != par2)
        {
            result = this.chunkProvider.provideChunk(par1, par2);
            this.lastChunkAccessed = result; // Exploit fact that write is atomic
        }

        //}
        // Spigot end
        return result;
    }
    // CraftBukkit end

    /**
     * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block ID, new metadata, flags. Flag 1 will
     * cause a block update. Flag 2 will send the change to clients (you almost always want this). Flag 4 prevents the
     * block from being re-rendered, if this is a client world. Flags can be added together.
     */
    public boolean setBlock(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (par2 < 0)
            {
                return false;
            }
            else if (par2 >= 256)
            {
                return false;
            }
            else
            {
            	
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
                
                WorldInteract currentInteractor = World.currentTickItem;
                
                int k1 = 0;
                if ((par6 & 1) != 0 || currentInteractor != null)
                {
                    k1 = chunk.getBlockID(par1 & 15, par2, par3 & 15);
                }
                
                //MCPC+ start, BaseProtect, send out event and skip set if cancelled
                if(currentInteractor != null)
                {
	                //First, We don't need to check for build permission if we got anything other than Bedrock on k1 since getBlockID will check
	                //If we got bedrock, if the interactor is a player we will allow it to continue(Players in creative can break bedrock)
                	if(k1 == Block.bedrock.blockID && !(currentInteractor instanceof EntityPlayerMP)) 
                		return false;
                	
                	//Are we interested in this interactor?
                	BaseProtect bp = BaseProtect.instance; //TODO: Store on world
                	if(bp.isRelevant(currentInteractor))
                	{
	                	//Get owner
	                	PlayerData playerData = currentInteractor.getItemOwner();
	                	Player player = null;
	                	if(playerData != null)
	                		player = playerData.getBukkitPlayer();
	                	if(player == null) //If no owner, set Minecraft as owner
	                	{
	                		if(mcFakePlayer == null)
	                			mcFakePlayer = (Player) FakePlayerFactory.getMinecraft(this).getBukkitEntity();
	                		player = mcFakePlayer;
	                	}
		                
		                //Then, tell ClaimHandler to skip the next event
	                	BaseProtect.claimIgnoreEvents(true);
	                	
		                //Send events for other plugins(Logging) if we're allowed to build
		                if(par4 == 0) //Set to Air(Break)
		                {
		                	//New Break event
		                	BlockBreakEvent event = new BlockBreakEvent(getWorld().getBlockAt(par1, par2, par3), player);
		                	getServer().getPluginManager().callEvent(event);
		                	if(event.isCancelled())
		                	{
		                		BaseProtect.claimIgnoreEvents(false);
		                		return false;
		                	}
		                }
		                else //Set to anything else(Place)
		                {
		                	//TODO: For now it seems everything is forced to send an event anyway
		                	//New Place event
		                	//BlockPlaceEvent event = new BlockPlaceEvent(getWorld().getBlockAt(par1, par2, par3), player);
		                }
                	}
	                
	                //Reset ignore(TODO: What happens if there is an exception between set and unset ignore?)
	                BaseProtect.claimIgnoreEvents(false);
                	
                }
                //MCPC+ end

                boolean flag = chunk.setBlockIDWithMetadata(par1 & 15, par2, par3 & 15, par4, par5);
                this.theProfiler.startSection("checkLight");
                this.updateAllLightTypes(par1, par2, par3);
                this.theProfiler.endSection();

                if (flag)
                {
                    if ((par6 & 2) != 0 && (!this.isRemote || (par6 & 4) == 0))
                    {
                        this.markBlockForUpdate(par1, par2, par3);
                    }

                    if (!this.isRemote && (par6 & 1) != 0)
                    {
                        this.notifyBlockChange(par1, par2, par3, k1);
                        Block block = Block.blocksList[par4];

                        if (block != null && block.hasComparatorInputOverride())
                        {
                            this.func_96440_m(par1, par2, par3, par4);
                        }
                    }
                }

                return flag;
            }
        }
        else
        {
            return false;
        }
    }

    // MCPC+ start - new helper method for adding mod events

    public boolean trySetBlockAndMetadata(int x, int y, int z, int blockID, int metadata, int flags, String username, boolean doLogin) // original name
    {
        return trySetBlock(x, y, z, blockID, metadata, flags, username, doLogin);
    }

    /**
     * Attempt to set a block in the world, sending required events and cancelling if necessary.
     * @param x
     * @param y
     * @param z
     * @param blockID
     * @param metadata
     * @param flags
     * @param username A real player's username (possibly offline) or fake player name enclosed in [brackets]
     * @param doLogin If true, sends join events for fake players (should be configurable in the mod)
     * @return true if successful, false if denied. Mods MUST not continue if denied.
     */
    public boolean trySetBlock(int x, int y, int z, int blockID, int metadata, int flags, String username, boolean doLogin)
    {
        int oldBlockID = this.getBlockId(x, y, z);
        int oldMetadata = this.getBlockMetadata(x, y, z);

        if (blockID == oldBlockID && metadata == oldMetadata) {
            // no change
            return false;
        }

        org.bukkit.entity.Player player = CraftFakePlayer.getPossiblyRealPlayerBukkitEntity(this, username, doLogin);

        if (blockID == 0)
        {
            // Block break - modeled after ItemInWorldManager#tryHarvestBlock
            org.bukkit.block.Block block = this.getWorld().getBlockAt(x, y, z);
            org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(block, player);

            this.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return false;
            }
            return setBlock(x, y, z, blockID, metadata, flags);
        }
        else
        {
            // Block place - delegate to ItemBlock#processBlockPlace
            int clickedX = x, clickedY = y, clickedZ = z;
            ItemStack itemstack = null;

            return ItemBlock.processBlockPlace(this,
                    ((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle(),
                    itemstack,
                    x, y, z,
                    blockID,
                    metadata,
                    clickedX, clickedY, clickedZ);
        }
    }
    // MCPC+ end


    /**
     * Returns the block's material.
     */
    public Material getBlockMaterial(int par1, int par2, int par3)
    {
        int l = this.getBlockId(par1, par2, par3);
        return l == 0 || Block.blocksList[l] == null ? Material.air : Block.blocksList[l].blockMaterial; // MCPC+
    }

    /**
     * Returns the block metadata at coords x,y,z
     */
    public int getBlockMetadata(int par1, int par2, int par3)
    {
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (par2 < 0)
            {
                return 0;
            }
            else if (par2 >= 256)
            {
                return 0;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
                par1 &= 15;
                par3 &= 15;
                return chunk.getBlockMetadata(par1, par2, par3);
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Sets the blocks metadata and if set will then notify blocks that this block changed, depending on the flag. Args:
     * x, y, z, metadata, flag. See setBlock for flag description
     */
    public boolean setBlockMetadataWithNotify(int par1, int par2, int par3, int par4, int par5)
    {
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (par2 < 0)
            {
                return false;
            }
            else if (par2 >= 256)
            {
                return false;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
                int j1 = par1 & 15;
                int k1 = par3 & 15;
                boolean flag = chunk.setBlockMetadata(j1, par2, k1, par4);

                if (flag)
                {
                    int l1 = chunk.getBlockID(j1, par2, k1);

                    if ((par5 & 2) != 0 && (!this.isRemote || (par5 & 4) == 0))
                    {
                        this.markBlockForUpdate(par1, par2, par3);
                    }

                    if (!this.isRemote && (par5 & 1) != 0)
                    {
                        this.notifyBlockChange(par1, par2, par3, l1);
                        Block block = Block.blocksList[l1];

                        if (block != null && block.hasComparatorInputOverride())
                        {
                            this.func_96440_m(par1, par2, par3, l1);
                        }
                    }
                }

                return flag;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets a block to 0 and notifies relevant systems with the block change  Args: x, y, z
     */
    public boolean setBlockToAir(int par1, int par2, int par3)
    {
        return this.setBlock(par1, par2, par3, 0, 0, 3);
    }

    /**
     * Destroys a block and optionally drops items. Args: X, Y, Z, dropItems
     */
    public boolean destroyBlock(int par1, int par2, int par3, boolean par4)
    {
        int l = this.getBlockId(par1, par2, par3);

        if (l > 0)
        {
            int i1 = this.getBlockMetadata(par1, par2, par3);
            this.playAuxSFX(2001, par1, par2, par3, l + (i1 << 12));

            if (par4)
            {
                Block.blocksList[l].dropBlockAsItem(this, par1, par2, par3, i1, 0);
            }

            return this.setBlock(par1, par2, par3, 0, 0, 3);
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets a block and notifies relevant systems with the block change  Args: x, y, z, blockID
     */
    public boolean setBlock(int par1, int par2, int par3, int par4)
    {
        return this.setBlock(par1, par2, par3, par4, 0, 3);
    }

    /**
     * On the client, re-renders the block. On the server, sends the block to the client (which will re-render it),
     * including the tile entity description packet if applicable. Args: x, y, z
     */
    public void markBlockForUpdate(int par1, int par2, int par3)
    {
        for (int l = 0; l < this.worldAccesses.size(); ++l)
        {
            ((IWorldAccess)this.worldAccesses.get(l)).markBlockForUpdate(par1, par2, par3);
        }
    }

    /**
     * The block type change and need to notify other systems  Args: x, y, z, blockID
     */
    public void notifyBlockChange(int par1, int par2, int par3, int par4)
    {
        this.notifyBlocksOfNeighborChange(par1, par2, par3, par4);
    }

    /**
     * marks a vertical line of blocks as dirty
     */
    public void markBlocksDirtyVertical(int par1, int par2, int par3, int par4)
    {
        int i1;

        if (par3 > par4)
        {
            i1 = par4;
            par4 = par3;
            par3 = i1;
        }

        if (!this.provider.hasNoSky)
        {
            for (i1 = par3; i1 <= par4; ++i1)
            {
                this.updateLightByType(EnumSkyBlock.Sky, par1, i1, par2);
            }
        }

        this.markBlockRangeForRenderUpdate(par1, par3, par2, par1, par4, par2);
    }

    /**
     * On the client, re-renders all blocks in this range, inclusive. On the server, does nothing. Args: min x, min y,
     * min z, max x, max y, max z
     */
    public void markBlockRangeForRenderUpdate(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        for (int k1 = 0; k1 < this.worldAccesses.size(); ++k1)
        {
            ((IWorldAccess)this.worldAccesses.get(k1)).markBlockRangeForRenderUpdate(par1, par2, par3, par4, par5, par6);
        }
    }

    /**
     * Notifies neighboring blocks that this specified block changed  Args: x, y, z, blockID
     */
    public void notifyBlocksOfNeighborChange(int par1, int par2, int par3, int par4)
    {
        this.notifyBlockOfNeighborChange(par1 - 1, par2, par3, par4);
        this.notifyBlockOfNeighborChange(par1 + 1, par2, par3, par4);
        this.notifyBlockOfNeighborChange(par1, par2 - 1, par3, par4);
        this.notifyBlockOfNeighborChange(par1, par2 + 1, par3, par4);
        this.notifyBlockOfNeighborChange(par1, par2, par3 - 1, par4);
        this.notifyBlockOfNeighborChange(par1, par2, par3 + 1, par4);
    }

    /**
     * Calls notifyBlockOfNeighborChange on adjacent blocks, except the one on the given side. Args: X, Y, Z,
     * changingBlockID, side
     */
    public void notifyBlocksOfNeighborChange(int par1, int par2, int par3, int par4, int par5)
    {
        if (par5 != 4)
        {
            this.notifyBlockOfNeighborChange(par1 - 1, par2, par3, par4);
        }

        if (par5 != 5)
        {
            this.notifyBlockOfNeighborChange(par1 + 1, par2, par3, par4);
        }

        if (par5 != 0)
        {
            this.notifyBlockOfNeighborChange(par1, par2 - 1, par3, par4);
        }

        if (par5 != 1)
        {
            this.notifyBlockOfNeighborChange(par1, par2 + 1, par3, par4);
        }

        if (par5 != 2)
        {
            this.notifyBlockOfNeighborChange(par1, par2, par3 - 1, par4);
        }

        if (par5 != 3)
        {
            this.notifyBlockOfNeighborChange(par1, par2, par3 + 1, par4);
        }
    }

    /**
     * Notifies a block that one of its neighbor change to the specified type Args: x, y, z, blockID
     */
    public void notifyBlockOfNeighborChange(int par1, int par2, int par3, int par4)
    {
        if (!this.isRemote)
        {
            int i1 = this.getBlockId(par1, par2, par3);
            Block block = Block.blocksList[i1];

            if (block != null)
            {
                try
                {
                    // CraftBukkit start
                    CraftWorld world = ((WorldServer) this).getWorld();

                    if (world != null)
                    {
                        BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(par1, par2, par3), par4);
                        this.getServer().getPluginManager().callEvent(event);

                        if (event.isCancelled())
                        {
                            return;
                        }
                    }

                    // CraftBukkit end
                    block.onNeighborBlockChange(this, par1, par2, par3, par4);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");
                    int j1;

                    try
                    {
                        j1 = this.getBlockMetadata(par1, par2, par3);
                    }
                    catch (Throwable throwable1)
                    {
                        j1 = -1;
                    }

                    crashreportcategory.addCrashSectionCallable("Source block type", new CallableLvl1(this, par4));
                    CrashReportCategory.func_85068_a(crashreportcategory, par1, par2, par3, i1, j1);
                    throw new ReportedException(crashreport);
                }
            }
        }
    }

    /**
     * Returns true if the given block will receive a scheduled tick in the future. Args: X, Y, Z, blockID
     */
    public boolean isBlockTickScheduled(int par1, int par2, int par3, int par4)
    {
        return false;
    }

    /**
     * Checks if the specified block is able to see the sky
     */
    public boolean canBlockSeeTheSky(int par1, int par2, int par3)
    {
        return this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4).canBlockSeeTheSky(par1 & 15, par2, par3 & 15);
    }

    /**
     * Does the same as getBlockLightValue_do but without checking if its not a normal block
     */
    public int getFullBlockLightValue(int par1, int par2, int par3)
    {
        if (par2 < 0)
        {
            return 0;
        }
        else
        {
            if (par2 >= 256)
            {
                par2 = 255;
            }

            return this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4).getBlockLightValue(par1 & 15, par2, par3 & 15, 0);
        }
    }

    /**
     * Gets the light value of a block location
     */
    public int getBlockLightValue(int par1, int par2, int par3)
    {
        return this.getBlockLightValue_do(par1, par2, par3, true);
    }

    /**
     * Gets the light value of a block location. This is the actual function that gets the value and has a bool flag
     * that indicates if its a half step block to get the maximum light value of a direct neighboring block (left,
     * right, forward, back, and up)
     */
    public int getBlockLightValue_do(int par1, int par2, int par3, boolean par4)
    {
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (par4)
            {
                int l = this.getBlockId(par1, par2, par3);

                if (Block.useNeighborBrightness[l])
                {
                    int i1 = this.getBlockLightValue_do(par1, par2 + 1, par3, false);
                    int j1 = this.getBlockLightValue_do(par1 + 1, par2, par3, false);
                    int k1 = this.getBlockLightValue_do(par1 - 1, par2, par3, false);
                    int l1 = this.getBlockLightValue_do(par1, par2, par3 + 1, false);
                    int i2 = this.getBlockLightValue_do(par1, par2, par3 - 1, false);

                    if (j1 > i1)
                    {
                        i1 = j1;
                    }

                    if (k1 > i1)
                    {
                        i1 = k1;
                    }

                    if (l1 > i1)
                    {
                        i1 = l1;
                    }

                    if (i2 > i1)
                    {
                        i1 = i2;
                    }

                    return i1;
                }
            }

            if (par2 < 0)
            {
                return 0;
            }
            else
            {
                if (par2 >= 256)
                {
                    par2 = 255;
                }

                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
                par1 &= 15;
                par3 &= 15;
                return chunk.getBlockLightValue(par1, par2, par3, this.skylightSubtracted);
            }
        }
        else
        {
            return 15;
        }
    }

    /**
     * Returns the y coordinate with a block in it at this x, z coordinate
     */
    public int getHeightValue(int par1, int par2)
    {
        if (par1 >= -30000000 && par2 >= -30000000 && par1 < 30000000 && par2 < 30000000)
        {
            if (!this.chunkExists(par1 >> 4, par2 >> 4))
            {
                return 0;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par2 >> 4);
                return chunk.getHeightValue(par1 & 15, par2 & 15);
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Gets the heightMapMinimum field of the given chunk, or 0 if the chunk is not loaded. Coords are in blocks. Args:
     * X, Z
     */
    public int getChunkHeightMapMinimum(int par1, int par2)
    {
        if (par1 >= -30000000 && par2 >= -30000000 && par1 < 30000000 && par2 < 30000000)
        {
            if (!this.chunkExists(par1 >> 4, par2 >> 4))
            {
                return 0;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par2 >> 4);
                return chunk.heightMapMinimum;
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns saved light value without taking into account the time of day.  Either looks in the sky light map or
     * block light map based on the enumSkyBlock arg.
     */
    public int getSavedLightValue(EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4)
    {
        if (par3 < 0)
        {
            par3 = 0;
        }

        if (par3 >= 256)
        {
            par3 = 255;
        }

        if (par2 >= -30000000 && par4 >= -30000000 && par2 < 30000000 && par4 < 30000000)
        {
            int l = par2 >> 4;
            int i1 = par4 >> 4;

            if (!this.chunkExists(l, i1))
            {
                return par1EnumSkyBlock.defaultLightValue;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(l, i1);
                return chunk.getSavedLightValue(par1EnumSkyBlock, par2 & 15, par3, par4 & 15);
            }
        }
        else
        {
            return par1EnumSkyBlock.defaultLightValue;
        }
    }

    /**
     * Sets the light value either into the sky map or block map depending on if enumSkyBlock is set to sky or block.
     * Args: enumSkyBlock, x, y, z, lightValue
     */
    public void setLightValue(EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4, int par5)
    {
        if (par2 >= -30000000 && par4 >= -30000000 && par2 < 30000000 && par4 < 30000000)
        {
            if (par3 >= 0)
            {
                if (par3 < 256)
                {
                    if (this.chunkExists(par2 >> 4, par4 >> 4))
                    {
                        Chunk chunk = this.getChunkFromChunkCoords(par2 >> 4, par4 >> 4);
                        chunk.setLightValue(par1EnumSkyBlock, par2 & 15, par3, par4 & 15, par5);

                        for (int i1 = 0; i1 < this.worldAccesses.size(); ++i1)
                        {
                            ((IWorldAccess)this.worldAccesses.get(i1)).markBlockForRenderUpdate(par2, par3, par4);
                        }
                    }
                }
            }
        }
    }

    /**
     * On the client, re-renders this block. On the server, does nothing. Used for lighting updates.
     */
    public void markBlockForRenderUpdate(int par1, int par2, int par3)
    {
        for (int l = 0; l < this.worldAccesses.size(); ++l)
        {
            ((IWorldAccess)this.worldAccesses.get(l)).markBlockForRenderUpdate(par1, par2, par3);
        }
    }

    /**
     * Returns how bright the block is shown as which is the block's light value looked up in a lookup table (light
     * values aren't linear for brightness). Args: x, y, z
     */
    public float getLightBrightness(int par1, int par2, int par3)
    {
        return this.provider.lightBrightnessTable[this.getBlockLightValue(par1, par2, par3)];
    }

    /**
     * Checks whether its daytime by seeing if the light subtracted from the skylight is less than 4
     */
    public boolean isDaytime()
    {
        return provider.isDaytime();
    }

    /**
     * ray traces all blocks, including non-collideable ones
     */
    public MovingObjectPosition rayTraceBlocks(Vec3 par1Vec3, Vec3 par2Vec3)
    {
        return this.rayTraceBlocks_do_do(par1Vec3, par2Vec3, false, false);
    }

    public MovingObjectPosition rayTraceBlocks_do(Vec3 par1Vec3, Vec3 par2Vec3, boolean par3)
    {
        return this.rayTraceBlocks_do_do(par1Vec3, par2Vec3, par3, false);
    }

    public MovingObjectPosition rayTraceBlocks_do_do(Vec3 par1Vec3, Vec3 par2Vec3, boolean par3, boolean par4)
    {
        if (!Double.isNaN(par1Vec3.xCoord) && !Double.isNaN(par1Vec3.yCoord) && !Double.isNaN(par1Vec3.zCoord))
        {
            if (!Double.isNaN(par2Vec3.xCoord) && !Double.isNaN(par2Vec3.yCoord) && !Double.isNaN(par2Vec3.zCoord))
            {
                int i = MathHelper.floor_double(par2Vec3.xCoord);
                int j = MathHelper.floor_double(par2Vec3.yCoord);
                int k = MathHelper.floor_double(par2Vec3.zCoord);
                int l = MathHelper.floor_double(par1Vec3.xCoord);
                int i1 = MathHelper.floor_double(par1Vec3.yCoord);
                int j1 = MathHelper.floor_double(par1Vec3.zCoord);
                int k1 = this.getBlockId(l, i1, j1);
                int l1 = this.getBlockMetadata(l, i1, j1);
                Block block = Block.blocksList[k1];

                if (block != null && (!par4 || block == null || block.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null) && k1 > 0 && block.canCollideCheck(l1, par3))
                {
                    MovingObjectPosition movingobjectposition = block.collisionRayTrace(this, l, i1, j1, par1Vec3, par2Vec3);

                    if (movingobjectposition != null)
                    {
                        return movingobjectposition;
                    }
                }

                k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(par1Vec3.xCoord) || Double.isNaN(par1Vec3.yCoord) || Double.isNaN(par1Vec3.zCoord))
                    {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k)
                    {
                        return null;
                    }

                    boolean flag2 = true;
                    boolean flag3 = true;
                    boolean flag4 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l)
                    {
                        d0 = (double)l + 1.0D;
                    }
                    else if (i < l)
                    {
                        d0 = (double)l + 0.0D;
                    }
                    else
                    {
                        flag2 = false;
                    }

                    if (j > i1)
                    {
                        d1 = (double)i1 + 1.0D;
                    }
                    else if (j < i1)
                    {
                        d1 = (double)i1 + 0.0D;
                    }
                    else
                    {
                        flag3 = false;
                    }

                    if (k > j1)
                    {
                        d2 = (double)j1 + 1.0D;
                    }
                    else if (k < j1)
                    {
                        d2 = (double)j1 + 0.0D;
                    }
                    else
                    {
                        flag4 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = par2Vec3.xCoord - par1Vec3.xCoord;
                    double d7 = par2Vec3.yCoord - par1Vec3.yCoord;
                    double d8 = par2Vec3.zCoord - par1Vec3.zCoord;

                    if (flag2)
                    {
                        d3 = (d0 - par1Vec3.xCoord) / d6;
                    }

                    if (flag3)
                    {
                        d4 = (d1 - par1Vec3.yCoord) / d7;
                    }

                    if (flag4)
                    {
                        d5 = (d2 - par1Vec3.zCoord) / d8;
                    }

                    boolean flag5 = false;
                    byte b0;

                    if (d3 < d4 && d3 < d5)
                    {
                        if (i > l)
                        {
                            b0 = 4;
                        }
                        else
                        {
                            b0 = 5;
                        }

                        par1Vec3.xCoord = d0;
                        par1Vec3.yCoord += d7 * d3;
                        par1Vec3.zCoord += d8 * d3;
                    }
                    else if (d4 < d5)
                    {
                        if (j > i1)
                        {
                            b0 = 0;
                        }
                        else
                        {
                            b0 = 1;
                        }

                        par1Vec3.xCoord += d6 * d4;
                        par1Vec3.yCoord = d1;
                        par1Vec3.zCoord += d8 * d4;
                    }
                    else
                    {
                        if (k > j1)
                        {
                            b0 = 2;
                        }
                        else
                        {
                            b0 = 3;
                        }

                        par1Vec3.xCoord += d6 * d5;
                        par1Vec3.yCoord += d7 * d5;
                        par1Vec3.zCoord = d2;
                    }

                    Vec3 vec32 = this.getWorldVec3Pool().getVecFromPool(par1Vec3.xCoord, par1Vec3.yCoord, par1Vec3.zCoord);
                    l = (int)(vec32.xCoord = (double)MathHelper.floor_double(par1Vec3.xCoord));

                    if (b0 == 5)
                    {
                        --l;
                        ++vec32.xCoord;
                    }

                    i1 = (int)(vec32.yCoord = (double)MathHelper.floor_double(par1Vec3.yCoord));

                    if (b0 == 1)
                    {
                        --i1;
                        ++vec32.yCoord;
                    }

                    j1 = (int)(vec32.zCoord = (double)MathHelper.floor_double(par1Vec3.zCoord));

                    if (b0 == 3)
                    {
                        --j1;
                        ++vec32.zCoord;
                    }

                    int i2 = this.getBlockId(l, i1, j1);
                    int j2 = this.getBlockMetadata(l, i1, j1);
                    Block block1 = Block.blocksList[i2];

                    if ((!par4 || block1 == null || block1.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null) && i2 > 0 && block1.canCollideCheck(j2, par3))
                    {
                        MovingObjectPosition movingobjectposition1 = block1.collisionRayTrace(this, l, i1, j1, par1Vec3, par2Vec3);

                        if (movingobjectposition1 != null)
                        {
                            vec32.myVec3LocalPool.release(vec32); // CraftBukkit
                            return movingobjectposition1;
                        }
                    }

                    vec32.myVec3LocalPool.release(vec32); // CraftBukkit
                }

                return null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Plays a sound at the entity's position. Args: entity, sound, volume (relative to 1.0), and frequency (or pitch,
     * also relative to 1.0).
     */
    public void playSoundAtEntity(Entity par1Entity, String par2Str, float par3, float par4)
    {
        PlaySoundAtEntityEvent event = new PlaySoundAtEntityEvent(par1Entity, par2Str, par3, par4);
        if (MinecraftForge.EVENT_BUS.post(event))
        {
            return;
        }
        par2Str = event.name;
        if (par1Entity != null && par2Str != null)
        {
            for (int i = 0; i < this.worldAccesses.size(); ++i)
            {
                ((IWorldAccess)this.worldAccesses.get(i)).playSound(par2Str, par1Entity.posX, par1Entity.posY - (double)par1Entity.yOffset, par1Entity.posZ, par3, par4);
            }
        }
    }

    /**
     * Plays sound to all near players except the player reference given
     */
    public void playSoundToNearExcept(EntityPlayer par1EntityPlayer, String par2Str, float par3, float par4)
    {
        PlaySoundAtEntityEvent event = new PlaySoundAtEntityEvent(par1EntityPlayer, par2Str, par3, par4);
        if (MinecraftForge.EVENT_BUS.post(event))
        {
            return;
        }
        par2Str = event.name;
        if (par1EntityPlayer != null && par2Str != null)
        {
            for (int i = 0; i < this.worldAccesses.size(); ++i)
            {
                ((IWorldAccess)this.worldAccesses.get(i)).playSoundToNearExcept(par1EntityPlayer, par2Str, par1EntityPlayer.posX, par1EntityPlayer.posY - (double)par1EntityPlayer.yOffset, par1EntityPlayer.posZ, par3, par4);
            }
        }
    }

    /**
     * Play a sound effect. Many many parameters for this function. Not sure what they do, but a classic call is :
     * (double)i + 0.5D, (double)j + 0.5D, (double)k + 0.5D, 'random.door_open', 1.0F, world.rand.nextFloat() * 0.1F +
     * 0.9F with i,j,k position of the block.
     */
    public void playSoundEffect(double par1, double par3, double par5, String par7Str, float par8, float par9)
    {
        if (par7Str != null)
        {
            for (int i = 0; i < this.worldAccesses.size(); ++i)
            {
                ((IWorldAccess)this.worldAccesses.get(i)).playSound(par7Str, par1, par3, par5, par8, par9);
            }
        }
    }

    /**
     * par8 is loudness, all pars passed to minecraftInstance.sndManager.playSound
     */
    public void playSound(double par1, double par3, double par5, String par7Str, float par8, float par9, boolean par10) {}

    /**
     * Plays a record at the specified coordinates of the specified name. Args: recordName, x, y, z
     */
    public void playRecord(String par1Str, int par2, int par3, int par4)
    {
        for (int l = 0; l < this.worldAccesses.size(); ++l)
        {
            ((IWorldAccess)this.worldAccesses.get(l)).playRecord(par1Str, par2, par3, par4);
        }
    }

    /**
     * Spawns a particle.  Args particleName, x, y, z, velX, velY, velZ
     */
    public void spawnParticle(String par1Str, double par2, double par4, double par6, double par8, double par10, double par12)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).spawnParticle(par1Str, par2, par4, par6, par8, par10, par12);
        }
    }

    /**
     * adds a lightning bolt to the list of lightning bolts in this world.
     */
    public boolean addWeatherEffect(Entity par1Entity)
    {
        this.weatherEffects.add(par1Entity);
        return true;
    }

    // CraftBukkit start - Used for entities other than creatures

    /**
     * Called to place all entities as part of a world
     */
    public boolean spawnEntityInWorld(Entity par1Entity)
    {
        // MCPC+ start - do not drop any items during a place event. Fixes dupes in mods such as Flans
        if (par1Entity instanceof EntityItem && this.callingPlaceEvent)
            return false;
        // MCPC+ end
        return this.addEntity(par1Entity, CreatureSpawnEvent.SpawnReason.DEFAULT); // Set reason as DEFAULT
    }

    public boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason spawnReason)   // Changed signature, added SpawnReason
    {
        if (Thread.currentThread() != MinecraftServer.getServer().primaryThread)
        {
            throw new IllegalStateException("Asynchronous entity add!");    // Spigot
        }

        if (entity == null)
        {
            return false;
        }

        // CraftBukkit end
        int i = MathHelper.floor_double(entity.posX / 16.0D);
        int j = MathHelper.floor_double(entity.posZ / 16.0D);
        boolean flag = entity.field_98038_p;

        if (entity instanceof EntityPlayer)
        {
            flag = true;
        }

        // CraftBukkit start
        org.bukkit.event.Cancellable event = null;

        if (entity instanceof EntityLiving && !(entity instanceof EntityPlayerMP))
        {
            boolean isAnimal = entity instanceof EntityAnimal || entity instanceof EntityWaterMob || entity instanceof EntityGolem;
            boolean isMonster = entity instanceof EntityMob || entity instanceof EntityGhast || entity instanceof EntitySlime;

            if (spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM)
            {
                if (isAnimal && !spawnPeacefulMobs || isMonster && !spawnHostileMobs)
                {
                    entity.isDead = true;
                    return false;
                }
            }

            event = CraftEventFactory.callCreatureSpawnEvent((EntityLiving) entity, spawnReason);
        }
        else if (entity instanceof EntityItem)
        {
            event = CraftEventFactory.callItemSpawnEvent((EntityItem) entity);
        }
        else if (entity.getBukkitEntity() instanceof org.bukkit.entity.Projectile)
        {
            // Not all projectiles extend EntityProjectile, so check for Bukkit interface instead
            event = CraftEventFactory.callProjectileLaunchEvent(entity);
        }
        // Spigot start
        else if (entity instanceof EntityXPOrb)
        {
            EntityXPOrb xp = (EntityXPOrb) entity;
            double radius = this.getWorld().expMergeRadius;

            if (radius > 0)
            {
                List<Entity> entities = this.getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.expand(radius, radius, radius));

                for (Entity e : entities)
                {
                    if (e instanceof EntityXPOrb)
                    {
                        EntityXPOrb loopItem = (EntityXPOrb) e;

                        if (!loopItem.isDead)
                        {
                            xp.xpValue += loopItem.xpValue;
                            loopItem.setDead();
                        }
                    }
                }
            }
        } // Spigot end

        if (event != null && (event.isCancelled() || entity.isDead))
        {
            entity.isDead = true;
            return false;
        }

        // CraftBukkit end

        if (!flag && !this.chunkExists(i, j))
        {
            entity.isDead = true; // CraftBukkit
            return false;
        }
        else
        {
            if (entity instanceof EntityPlayer)
            {
                EntityPlayer entityplayer = (EntityPlayer) entity;
                this.playerEntities.add(entityplayer);
                this.updateAllPlayersSleepingFlag();
            }

            if (MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, this)) && !flag)
            {
                return false;
            }

            //MCPC+ start - BaseProtect, Any new entity created will inherit the same owner as the currently ticking item
            WorldInteract entityParent = this.currentTickItem;
            if(entityParent != null)
            {
            	//System.out.println(entity.getClass().getName() + " Parent: " + entityParent + "(" + entityParent.getItemOwner() + ")");
            	entity.setItemOwner(entityParent.getItemOwner());
            }
            //MCPC+ End
            
            this.getChunkFromChunkCoords(i, j).addEntity(entity);
            this.loadedEntityList.add(entity);
            this.obtainEntitySkin(entity);
            return true;
        }
    }

    /**
     * Start the skin for this entity downloading, if necessary, and increment its reference counter
     */
    protected void obtainEntitySkin(Entity par1Entity)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).onEntityCreate(par1Entity);
        }

        par1Entity.valid = true; // CraftBukkit
    }

    /**
     * Decrement the reference counter for this entity's skin image data
     */
    public void releaseEntitySkin(Entity par1Entity)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).onEntityDestroy(par1Entity);
        }

        par1Entity.valid = false; // CraftBukkit
    }

    /**
     * Schedule the entity for removal during the next tick. Marks the entity dead in anticipation.
     */
    public void removeEntity(Entity par1Entity)
    {
        if (par1Entity.riddenByEntity != null)
        {
            par1Entity.riddenByEntity.mountEntity((Entity)null);
        }

        if (par1Entity.ridingEntity != null)
        {
            par1Entity.mountEntity((Entity)null);
        }
        
        par1Entity.setDead();

        if (par1Entity instanceof EntityPlayer)
        {
            this.playerEntities.remove(par1Entity);
            this.updateAllPlayersSleepingFlag();
        }
    }

    /**
     * Do NOT use this method to remove normal entities- use normal removeEntity
     */
    public void removePlayerEntityDangerously(Entity par1Entity)
    {
        if (Thread.currentThread() != MinecraftServer.getServer().primaryThread)
        {
            throw new IllegalStateException("Asynchronous entity remove!");    // Spigot
        }

        par1Entity.setDead();

        if (par1Entity instanceof EntityPlayer)
        {
            this.playerEntities.remove(par1Entity);
            this.updateAllPlayersSleepingFlag();
        }

        int i = par1Entity.chunkCoordX;
        int j = par1Entity.chunkCoordZ;

        if (par1Entity.addedToChunk && this.chunkExists(i, j))
        {
            this.getChunkFromChunkCoords(i, j).removeEntity(par1Entity);
        }

        this.loadedEntityList.remove(par1Entity);
        this.releaseEntitySkin(par1Entity);
    }

    /**
     * Adds a IWorldAccess to the list of worldAccesses
     */
    public void addWorldAccess(IWorldAccess par1IWorldAccess)
    {
        this.worldAccesses.add(par1IWorldAccess);
    }

    /**
     * Returns a list of bounding boxes that collide with aabb excluding the passed in entity's collision. Args: entity,
     * aabb
     */
    public List getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB)
    {
        this.collidingBoundingBoxes.clear();
        int i = MathHelper.floor_double(par2AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par2AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par2AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par2AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par2AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par2AxisAlignedBB.maxZ + 1.0D);
        // Spigot start
        int ystart = ((k - 1) < 0) ? 0 : (k - 1);

        for (int chunkx = (i >> 4); chunkx <= ((j - 1) >> 4); chunkx++)
        {
            int cx = chunkx << 4;

            for (int chunkz = (i1 >> 4); chunkz <= ((j1 - 1) >> 4); chunkz++)
            {
                if (!this.chunkExists(chunkx, chunkz))
                {
                    continue;
                }

                int cz = chunkz << 4;
                Chunk chunk = this.getChunkFromChunkCoords(chunkx, chunkz);
                // Compute ranges within chunk
                int xstart = (i < cx) ? cx : i;
                int xend = (j < (cx + 16)) ? j : (cx + 16);
                int zstart = (i1 < cz) ? cz : i1;
                int zend = (j1 < (cz + 16)) ? j1 : (cz + 16);

                // Loop through blocks within chunk
                for (int x = xstart; x < xend; x++)
                {
                    for (int z = zstart; z < zend; z++)
                    {
                        for (int y = ystart; y < l; y++)
                        {
                            int blkid = chunk.getBlockID(x - cx, y, z - cz);

                            if (blkid > 0)
                            {
                                Block block = Block.blocksList[blkid];

                                if (block != null)
                                {
                                    block.addCollisionBoxesToList(this, x, y, z, par2AxisAlignedBB, this.collidingBoundingBoxes, par1Entity);
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (this.isLoaded(k1, 64, l1)) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        Block block = Block.byId[this.getTypeId(k1, i2, l1)];

                        if (block != null) {
                            block.a(this, k1, i2, l1, axisalignedbb, this.M, entity);
                        }
                    }
                }
            }
        }
        */// Spigot end
        double d0 = 0.25D;
        List list = this.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB.expand(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2)
        {
            AxisAlignedBB axisalignedbb1 = ((Entity)list.get(j2)).getBoundingBox();

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB))
            {
                this.collidingBoundingBoxes.add(axisalignedbb1);
            }

            axisalignedbb1 = par1Entity.getCollisionBox((Entity)list.get(j2));

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB))
            {
                this.collidingBoundingBoxes.add(axisalignedbb1);
            }
        }

        return this.collidingBoundingBoxes;
    }

    /**
     * calculates and returns a list of colliding bounding boxes within a given AABB
     */
    public List getCollidingBlockBounds(AxisAlignedBB par1AxisAlignedBB)
    {
        this.collidingBoundingBoxes.clear();
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.blockExists(k1, 64, l1))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        Block block = Block.blocksList[this.getBlockId(k1, i2, l1)];

                        if (block != null)
                        {
                            block.addCollisionBoxesToList(this, k1, i2, l1, par1AxisAlignedBB, this.collidingBoundingBoxes, (Entity)null);
                        }
                    }
                }
            }
        }

        return this.collidingBoundingBoxes;
    }

    /**
     * Returns the amount of skylight subtracted for the current time
     */
    public int calculateSkylightSubtracted(float par1)
    {
        float f1 = this.getCelestialAngle(par1);
        float f2 = 1.0F - (MathHelper.cos(f1 * (float)Math.PI * 2.0F) * 2.0F + 0.5F);

        if (f2 < 0.0F)
        {
            f2 = 0.0F;
        }

        if (f2 > 1.0F)
        {
            f2 = 1.0F;
        }

        f2 = 1.0F - f2;
        f2 = (float)((double)f2 * (1.0D - (double)(this.getRainStrength(par1) * 5.0F) / 16.0D));
        f2 = (float)((double)f2 * (1.0D - (double)(this.getWeightedThunderStrength(par1) * 5.0F) / 16.0D));
        f2 = 1.0F - f2;
        return (int)(f2 * 11.0F);
    }

    /**
     * calls calculateCelestialAngle
     */
    public float getCelestialAngle(float par1)
    {
        return this.provider.calculateCelestialAngle(this.worldInfo.getWorldTime(), par1);
    }

    public int getMoonPhase()
    {
        return this.provider.getMoonPhase(this.worldInfo.getWorldTime());
    }

    /**
     * Return getCelestialAngle()*2*PI
     */
    public float getCelestialAngleRadians(float par1)
    {
        float f1 = this.getCelestialAngle(par1);
        return f1 * (float)Math.PI * 2.0F;
    }

    /**
     * Gets the height to which rain/snow will fall. Calculates it if not already stored.
     */
    public int getPrecipitationHeight(int par1, int par2)
    {
        return this.getChunkFromBlockCoords(par1, par2).getPrecipitationHeight(par1 & 15, par2 & 15);
    }

    /**
     * Finds the highest block on the x, z coordinate that is solid and returns its y coord. Args x, z
     */
    public int getTopSolidOrLiquidBlock(int par1, int par2)
    {
        Chunk chunk = this.getChunkFromBlockCoords(par1, par2);
        int x = par1;
        int z = par2;
        int k = chunk.getTopFilledSegment() + 15;
        par1 &= 15;

        for (par2 &= 15; k > 0; --k)
        {
            int l = chunk.getBlockID(par1, k, par2);

            if (l != 0 && Block.blocksList[l].blockMaterial.blocksMovement() && Block.blocksList[l].blockMaterial != Material.leaves && !Block.blocksList[l].isBlockFoliage(this, x, k, z))
            {
                return k + 1;
            }
        }

        return -1;
    }

    /**
     * Schedules a tick to a block with a delay (Most commonly the tick rate)
     */
    public void scheduleBlockUpdate(int par1, int par2, int par3, int par4, int par5) {}

    public void func_82740_a(int par1, int par2, int par3, int par4, int par5, int par6) {}

    /**
     * Schedules a block update from the saved information in a chunk. Called when the chunk is loaded.
     */
    public void scheduleBlockUpdateFromLoad(int par1, int par2, int par3, int par4, int par5, int par6) {}

    /**
     * Updates (and cleans up) entities and tile entities
     */
    public void updateEntities()
    {
        this.theProfiler.startSection("entities");
        this.theProfiler.startSection("global");
        int i;
        Entity entity;
        CrashReport crashreport;
        CrashReportCategory crashreportcategory;

        long lastChunk = Long.MIN_VALUE; // Spigot - cache chunk x, z cords for unload queue
        for (i = 0; i < this.weatherEffects.size(); ++i)
        {
            entity = (Entity)this.weatherEffects.get(i);

            // CraftBukkit start - Fixed an NPE, don't process entities in chunks queued for unload
            if (entity == null)
            {
                continue;
            }

            ChunkProviderServer chunkProviderServer = ((WorldServer) this).theChunkProviderServer;

            // Spigot start - check last chunk to see if this loaded (fast cache)
            long chunk = org.bukkit.craftbukkit.util.LongHash.toLong(MathHelper.floor_double(entity.posX) >> 4, MathHelper.floor_double(entity.posZ) >> 4);
            if (lastChunk != chunk) {
                if (chunkProviderServer.chunksToUnload.contains(chunk)) { // Spigot end
                    continue;
                }
            }

            // CraftBukkit end
            lastChunk = chunk; // Spigot

            
            try
            {
                ++entity.ticksExisted;
                entity.onUpdate();
            }
            catch (Throwable throwable)
            {
            	//Print currently ticking item(AND POSITION) when crashing
            	WorldInteract item = currentTickItem;
        		if(item == null)
        		{
        			System.out.println("ITEM WHEN CRASH: NULL");
        		}
        		else
        		{
        			String className = item.getClass().getName();
        			int last = className.lastIndexOf(".");
        			if(last != -1)
        				className = className.substring(last);
        			
        			System.out.println("ITEM WHEN CRASH: (" + className + " [X: " + item.getX() + " Y: " + item.getY() + " Z: " + item.getZ() +"]) ");
        		}
        		
                crashreport = CrashReport.makeCrashReport(throwable, "Ticking entity");
                crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (entity == null)
                {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                }
                else
                {
                    entity.func_85029_a(crashreportcategory);
                }

                if (ForgeDummyContainer.removeErroringEntities)
                {
                    FMLLog.severe(crashreport.getCompleteReport());
                    removeEntity(entity);
                }
                else
                {
                	//Remove entity in hope of a clean restart
                	removeEntity(entity);
                    throw new ReportedException(crashreport);
                }
            }

            if (entity.isDead)
            {
                this.weatherEffects.remove(i--);
            }
        }
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("WeatherTick");
    	//MCPC+ End

        lastChunk = Long.MIN_VALUE; // Spigot
        this.theProfiler.endStartSection("remove");
        this.loadedEntityList.removeAll(this.unloadedEntityList);
        int j;
        int k;

        for (i = 0; i < this.unloadedEntityList.size(); ++i)
        {
            entity = (Entity)this.unloadedEntityList.get(i);
            j = entity.chunkCoordX;
            k = entity.chunkCoordZ;

            if (entity.addedToChunk && this.chunkExists(j, k))
            {
                this.getChunkFromChunkCoords(j, k).removeEntity(entity);
            }
        }

        for (i = 0; i < this.unloadedEntityList.size(); ++i)
        {
            this.releaseEntitySkin((Entity)this.unloadedEntityList.get(i));
        }

        this.unloadedEntityList.clear();
        this.theProfiler.endStartSection("regular");
        org.bukkit.craftbukkit.Spigot.activateEntities(this); // Spigot
        timings.entityTick.startTiming(); // Spigot
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("UnloadEntities");
    	//MCPC+ End

        for (i = 0; i < this.loadedEntityList.size(); ++i)
        {
            entity = (Entity)this.loadedEntityList.get(i);
            // CraftBukkit start - Don't tick entities in chunks queued for unload
            ChunkProviderServer chunkProviderServer = ((WorldServer) this).theChunkProviderServer;

            // Spigot start - check last chunk to see if this loaded (fast cache)
            long chunk = org.bukkit.craftbukkit.util.LongHash.toLong(MathHelper.floor_double(entity.posX) >> 4, MathHelper.floor_double(entity.posZ) >> 4);
            if (lastChunk != chunk) {
                if (chunkProviderServer.chunksToUnload.contains(chunk)) { // Spigot end
                    continue;
                }
            }

            // CraftBukkit end
            lastChunk = Long.MIN_VALUE; // Spigot

            if (entity.ridingEntity != null)
            {
                if (!entity.ridingEntity.isDead && entity.ridingEntity.riddenByEntity == entity)
                {
                    continue;
                }

                entity.ridingEntity.riddenByEntity = null;
                entity.ridingEntity = null;
            }

            this.theProfiler.startSection("tick");

            if (!entity.isDead)
            {
                try
                {
                    SpigotTimings.tickEntityTimer.startTiming(); // Spigot
                    
                    //MCPC+ Start
                    if(ChunkSampler.sampling)
                    {
                    	ChunkSampler.preSample("preEntityTick");
                    	ChunkSampler.tickedEntity(entity.worldObj, entity.chunkCoordX, entity.chunkCoordZ);
                    }
                    //MCPC+ End
                    
                    currentTickItem = entity; //MCPC+
                    
                    this.updateEntity(entity);
                    
                    currentTickItem = null; //MCPC+
                    
                    //MCPC+ Start
                    if(ChunkSampler.sampling)
                    	ChunkSampler.postSampleEntity(entity.worldObj, entity.chunkCoordX, entity.chunkCoordZ, entity);
                    //MCPC+ End
                    
                    SpigotTimings.tickEntityTimer.stopTiming(); // Spigot
                }
                catch (Throwable throwable1)
                {
                    crashreport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    crashreportcategory = crashreport.makeCategory("Entity being ticked");
                    entity.func_85029_a(crashreportcategory);
                    
                    if (ForgeDummyContainer.removeErroringEntities)
                    {
                        FMLLog.severe(crashreport.getCompleteReport());
                        removeEntity(entity);
                    }
                    else
                    {
                        throw new ReportedException(crashreport);
                    }
                }
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("remove");

            if (entity.isDead)
            {
                j = entity.chunkCoordX;
                k = entity.chunkCoordZ;
                
                if (entity.addedToChunk && this.chunkExists(j, k))
                {
                    this.getChunkFromChunkCoords(j, k).removeEntity(entity);
                }

                this.loadedEntityList.remove(i--);
                this.releaseEntitySkin(entity);
            }

            this.theProfiler.endSection();
            
            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("PostEntityTick");
        	//MCPC+ End
        }

        timings.entityTick.stopTiming(); // Spigot
        this.theProfiler.endStartSection("tileEntities");
        timings.tileEntityTick.startTiming(); // Spigot
        this.scanningTileEntities = true;
        
        ListIterator iterator;
        
        if(currentTileEntityIndex < loadedTileEntityList.size() && currentTileEntityIndex >= 0)
        {
        	iterator = loadedTileEntityList.listIterator(currentTileEntityIndex);
        }
        else
        {
        	iterator = loadedTileEntityList.listIterator();
        	currentTileEntityIndex = 0;
        }
        
        if(remainingTileEntityCount > loadedTileEntityList.size())
        	remainingTileEntityCount = loadedTileEntityList.size();
        
        if(remainingTileEntityCount == 0)
        	iterator = loadedTileEntityList.listIterator();
        else if(remainingTileEntityCount < 1)
        	iterator = loadedTileEntityList.listIterator(loadedTileEntityList.size());
        
        actualTileEntityCount = remainingTileEntityCount;
        
        //ThatLag, Time Tile Entities
        TimeWatch.timeResume(TimeWatch.TimeType.TileEntity);
        while (iterator.hasNext())
        {
            TileEntity tileentity = (TileEntity)iterator.next();

            // Spigot start
            if (tileentity == null)
            {
                getServer().getLogger().severe("Spigot has detected a null entity and has removed it, preventing a crash");
                iterator.remove();
                continue;
            }

            // Spigot end
            // CraftBukkit start - Don't tick entities in chunks queued for unload
            ChunkProviderServer chunkProviderServer = ((WorldServer) this).theChunkProviderServer;

            if (chunkProviderServer.chunksToUnload.contains(tileentity.xCoord >> 4, tileentity.zCoord >> 4))
            {
                continue;
            }

            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("tileEntityChunkUnloadCheck");
        	//MCPC+ End
            
            // CraftBukkit end

            if (!tileentity.isInvalid() && tileentity.func_70309_m() && this.blockExists(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord))
            {
                try
                {
                	//MCPC+ Start
                    if(ChunkSampler.sampling)
                    {
                    	ChunkSampler.tickedTileEntity(tileentity.worldObj, tileentity.xCoord >> 4, tileentity.zCoord >> 4);
                    	ChunkSampler.preSample("preTileEntityTick");
                    }
                    //MCPC+ End
                	
                    currentTickItem = tileentity; //MCPC+
                    
                    tileentity.tickTimer.startTiming(); // Spigot
                    tileentity.updateEntity();
                    tileentity.tickTimer.stopTiming(); // Spigot
                    
                    currentTickItem = null; //MCPC+
                    
                    //MCPC+ Start
                    if(ChunkSampler.sampling)
                    	ChunkSampler.postSampleTileEntity(tileentity.worldObj, tileentity.xCoord >> 4, tileentity.zCoord >> 4, tileentity);
                    //MCPC+ End
                }
                catch (Throwable throwable2)
                {
                	//Print currently ticking item(AND POSITION) when crashing
                	WorldInteract item = currentTickItem;
            		if(item == null)
            		{
            			System.out.println("ITEM WHEN CRASH: NULL");
            		}
            		else
            		{
            			String className = item.getClass().getName();
            			int last = className.lastIndexOf(".");
            			if(last != -1)
            				className = className.substring(last);
            			
            			System.out.println("ITEM WHEN CRASH: (" + className + " [X: " + item.getX() + " Y: " + item.getY() + " Z: " + item.getZ() +"]) ");
            		}
                	
                    tileentity.tickTimer.stopTiming(); // Spigot
                    crashreport = CrashReport.makeCrashReport(throwable2, "Ticking tile entity");
                    crashreportcategory = crashreport.makeCategory("Tile entity being ticked");
                    tileentity.func_85027_a(crashreportcategory);
                    if (ForgeDummyContainer.removeErroringTileEntities)
                    {
                        FMLLog.severe(crashreport.getCompleteReport());
                        tileentity.invalidate();
                        setBlockToAir(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
                    }
                    else
                    {
                    	//Remove the crashing block, hopefully allowing a clean restart
                        tileentity.invalidate();
                        setBlockToAir(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
                        throw new ReportedException(crashreport);
                    }
                }
            }

            if (tileentity.isInvalid())
            {
                iterator.remove();
                currentTileEntityIndex--; //Move index back when deleting(i.e, not move it forward)

                if (this.chunkExists(tileentity.xCoord >> 4, tileentity.zCoord >> 4))
                {
                    Chunk chunk = this.getChunkFromChunkCoords(tileentity.xCoord >> 4, tileentity.zCoord >> 4);

                    if (chunk != null)
                    {
                        chunk.cleanChunkBlockTileEntity(tileentity.xCoord & 15, tileentity.yCoord, tileentity.zCoord & 15);
                    }
                }
            }
            
            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("postTileEntityTick");
        	//MCPC+ End
            
        	currentTileEntityIndex++;
        	
        	if(--remainingTileEntityCount < 1.0)
        		break;
        	
        	//Wrap if at the end and still have remaining
        	if(!iterator.hasNext() && remainingTileEntityCount > 0)
        	{
        		iterator = loadedTileEntityList.listIterator();
        		currentTileEntityIndex = 0;
        	}
        }
        
        //ThatLag, Time Tile Entities
        TimeWatch.timePause(TimeWatch.TimeType.TileEntity);

        timings.tileEntityTick.stopTiming(); // Spigot
        timings.tileEntityPending.startTiming(); // Spigot

        if (!this.entityRemoval.isEmpty())
        {
            for (Object tile : entityRemoval)
            {
               ((TileEntity)tile).onChunkUnload();
            }
            this.loadedTileEntityList.removeAll(this.entityRemoval);
            this.entityRemoval.clear();
        }
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("TileEntityCleanup");
    	//MCPC+ End

        this.scanningTileEntities = false;

        this.theProfiler.endStartSection("pendingTileEntities");
        
        if (!this.addedTileEntityList.isEmpty())
        {
            for (int l = 0; l < this.addedTileEntityList.size(); ++l)
            {
                TileEntity tileentity1 = (TileEntity)this.addedTileEntityList.get(l);

                if (!tileentity1.isInvalid())
                {
                    /* CraftBukkit start - Order matters, moved down
                    if (!this.tileEntityList.contains(tileentity1)) {
                        this.tileEntityList.add(tileentity1);
                    }
                    // CraftBukkit end */
                    if (this.chunkExists(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4))
                    {
                        Chunk chunk1 = this.getChunkFromChunkCoords(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4);

                        if (chunk1 != null)
                        {
                            chunk1.cleanChunkBlockTileEntity(tileentity1.xCoord & 15, tileentity1.yCoord, tileentity1.zCoord & 15);

                            // CraftBukkit start - Moved down from above
                            if (!this.loadedTileEntityList.contains(tileentity1))
                            {
                                this.loadedTileEntityList.add(tileentity1);
                            }

                            // CraftBukkit end
                        }
                    }
                }
            }

            this.addedTileEntityList.clear();
        }
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("addedTileEntityList");
    	//MCPC+ End

        timings.tileEntityPending.stopTiming(); // Spigot
        this.theProfiler.endSection();
        this.theProfiler.endSection();
    }

    public void addTileEntity(Collection par1Collection)
    {
        Collection dest = scanningTileEntities ? addedTileEntityList : loadedTileEntityList; // MCPC+ - List -> Collection for CB loadedTileEntityList type change
        for(Object entity : par1Collection)
        {
            if(((TileEntity)entity).canUpdate())
            {
                dest.add(entity);
            }
        }
    }

    /**
     * Will update the entity in the world if the chunk the entity is in is currently loaded. Args: entity
     */
    public void updateEntity(Entity par1Entity)
    {
        this.updateEntityWithOptionalForce(par1Entity, true);
    }

    /**
     * Will update the entity in the world if the chunk the entity is in is currently loaded or its forced to update.
     * Args: entity, forceUpdate
     */
    public void updateEntityWithOptionalForce(Entity par1Entity, boolean par2)
    {
        int i = MathHelper.floor_double(par1Entity.posX);
        int j = MathHelper.floor_double(par1Entity.posZ);

        // Spigot start
        if (!Spigot.checkIfActive(par1Entity))
        {
            par1Entity.ticksExisted++;
            return;
        }
        par1Entity.tickTimer.startTiming();
        // Spigot end        
            
        boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(i >> 4, j >> 4));
        byte b0 = isForced ? (byte)0 : 32;
        boolean canUpdate = !par2 || this.checkChunksExist(i - b0, 0, j - b0, i + b0, 0, j + b0);
        if (!canUpdate)
        {
            EntityEvent.CanUpdate event = new EntityEvent.CanUpdate(par1Entity);
            MinecraftForge.EVENT_BUS.post(event);
            canUpdate = event.canUpdate;
        }
        if (canUpdate)
        {
            par1Entity.lastTickPosX = par1Entity.posX;
            par1Entity.lastTickPosY = par1Entity.posY;
            par1Entity.lastTickPosZ = par1Entity.posZ;
            par1Entity.prevRotationYaw = par1Entity.rotationYaw;
            par1Entity.prevRotationPitch = par1Entity.rotationPitch;

            if (par2 && par1Entity.addedToChunk)
            {
                if (par1Entity.ridingEntity != null)
                {
                    par1Entity.updateRidden();
                }
                else
                {
                    ++par1Entity.ticksExisted;
                    par1Entity.onUpdate();
                }
            }

            this.theProfiler.startSection("chunkCheck");

            if (Double.isNaN(par1Entity.posX) || Double.isInfinite(par1Entity.posX))
            {
                par1Entity.posX = par1Entity.lastTickPosX;
            }

            if (Double.isNaN(par1Entity.posY) || Double.isInfinite(par1Entity.posY))
            {
                par1Entity.posY = par1Entity.lastTickPosY;
            }

            if (Double.isNaN(par1Entity.posZ) || Double.isInfinite(par1Entity.posZ))
            {
                par1Entity.posZ = par1Entity.lastTickPosZ;
            }

            if (Double.isNaN((double)par1Entity.rotationPitch) || Double.isInfinite((double)par1Entity.rotationPitch))
            {
                par1Entity.rotationPitch = par1Entity.prevRotationPitch;
            }

            if (Double.isNaN((double)par1Entity.rotationYaw) || Double.isInfinite((double)par1Entity.rotationYaw))
            {
                par1Entity.rotationYaw = par1Entity.prevRotationYaw;
            }

            int k = MathHelper.floor_double(par1Entity.posX / 16.0D);
            int l = MathHelper.floor_double(par1Entity.posY / 16.0D);
            int i1 = MathHelper.floor_double(par1Entity.posZ / 16.0D);

            if (!par1Entity.addedToChunk || par1Entity.chunkCoordX != k || par1Entity.chunkCoordY != l || par1Entity.chunkCoordZ != i1)
            {
                if (par1Entity.addedToChunk && this.chunkExists(par1Entity.chunkCoordX, par1Entity.chunkCoordZ))
                {
                    this.getChunkFromChunkCoords(par1Entity.chunkCoordX, par1Entity.chunkCoordZ).removeEntityAtIndex(par1Entity, par1Entity.chunkCoordY);
                }

                if (this.chunkExists(k, i1))
                {
                    par1Entity.addedToChunk = true;
                    this.getChunkFromChunkCoords(k, i1).addEntity(par1Entity);
                }
                else
                {
                    par1Entity.addedToChunk = false;
                }
            }

            this.theProfiler.endSection();

            if (par2 && par1Entity.addedToChunk && par1Entity.riddenByEntity != null)
            {
                if (!par1Entity.riddenByEntity.isDead && par1Entity.riddenByEntity.ridingEntity == par1Entity)
                {
                    this.updateEntity(par1Entity.riddenByEntity);
                }
                else
                {
                    par1Entity.riddenByEntity.ridingEntity = null;
                    par1Entity.riddenByEntity = null;
                }
            }

            par1Entity.tickTimer.stopTiming(); // Spigot
        }
    }

    /**
     * Returns true if there are no solid, live entities in the specified AxisAlignedBB
     */
    public boolean checkNoEntityCollision(AxisAlignedBB par1AxisAlignedBB)
    {
        return this.checkNoEntityCollision(par1AxisAlignedBB, (Entity)null);
    }

    /**
     * Returns true if there are no solid, live entities in the specified AxisAlignedBB, excluding the given entity
     */
    public boolean checkNoEntityCollision(AxisAlignedBB par1AxisAlignedBB, Entity par2Entity)
    {
        List list = this.getEntitiesWithinAABBExcludingEntity((Entity)null, par1AxisAlignedBB);

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity1 = (Entity)list.get(i);

            if (!entity1.isDead && entity1.preventEntitySpawning && entity1 != par2Entity)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if there are any blocks in the region constrained by an AxisAlignedBB
     */
    public boolean checkBlockCollision(AxisAlignedBB par1AxisAlignedBB)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        if (par1AxisAlignedBB.minX < 0.0D)
        {
            --i;
        }

        if (par1AxisAlignedBB.minY < 0.0D)
        {
            --k;
        }

        if (par1AxisAlignedBB.minZ < 0.0D)
        {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    Block block = Block.blocksList[this.getBlockId(k1, l1, i2)];

                    if (block != null)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns if any of the blocks within the aabb are liquids. Args: aabb
     */
    public boolean isAnyLiquid(AxisAlignedBB par1AxisAlignedBB)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        if (par1AxisAlignedBB.minX < 0.0D)
        {
            --i;
        }

        if (par1AxisAlignedBB.minY < 0.0D)
        {
            --k;
        }

        if (par1AxisAlignedBB.minZ < 0.0D)
        {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    Block block = Block.blocksList[this.getBlockId(k1, l1, i2)];

                    if (block != null && block.blockMaterial.isLiquid())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns whether or not the given bounding box is on fire or not
     */
    public boolean isBoundingBoxBurning(AxisAlignedBB par1AxisAlignedBB)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        if (this.checkChunksExist(i, k, i1, j, l, j1))
        {
            for (int k1 = i; k1 < j; ++k1)
            {
                for (int l1 = k; l1 < l; ++l1)
                {
                    for (int i2 = i1; i2 < j1; ++i2)
                    {
                        int j2 = this.getBlockId(k1, l1, i2);

                        if (j2 == Block.fire.blockID || j2 == Block.lavaMoving.blockID || j2 == Block.lavaStill.blockID)
                        {
                            return true;
                        }
                        else
                        {
                            Block block = Block.blocksList[j2];
                            if (block != null && block.isBlockBurning(this, k1, l1, i2))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * handles the acceleration of an object whilst in water. Not sure if it is used elsewhere.
     */
    public boolean handleMaterialAcceleration(AxisAlignedBB par1AxisAlignedBB, Material par2Material, Entity par3Entity)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        if (!this.checkChunksExist(i, k, i1, j, l, j1))
        {
            return false;
        }
        else
        {
            boolean flag = false;
            Vec3 vec3 = this.getWorldVec3Pool().getVecFromPool(0.0D, 0.0D, 0.0D);

            for (int k1 = i; k1 < j; ++k1)
            {
                for (int l1 = k; l1 < l; ++l1)
                {
                    for (int i2 = i1; i2 < j1; ++i2)
                    {
                        Block block = Block.blocksList[this.getBlockId(k1, l1, i2)];

                        if (block != null && block.blockMaterial == par2Material)
                        {
                            double d0 = (double)((float)(l1 + 1) - BlockFluid.getFluidHeightPercent(this.getBlockMetadata(k1, l1, i2)));

                            if ((double)l >= d0)
                            {
                                flag = true;
                                block.velocityToAddToEntity(this, k1, l1, i2, par3Entity, vec3);
                            }
                        }
                    }
                }
            }

            if (vec3.lengthVector() > 0.0D && par3Entity.func_96092_aw())
            {
                vec3 = vec3.normalize();
                double d1 = 0.014D;
                par3Entity.motionX += vec3.xCoord * d1;
                par3Entity.motionY += vec3.yCoord * d1;
                par3Entity.motionZ += vec3.zCoord * d1;
            }

            vec3.myVec3LocalPool.release(vec3); // CraftBukkit - pop it - we're done
            return flag;
        }
    }

    /**
     * Returns true if the given bounding box contains the given material
     */
    public boolean isMaterialInBB(AxisAlignedBB par1AxisAlignedBB, Material par2Material)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    Block block = Block.blocksList[this.getBlockId(k1, l1, i2)];

                    if (block != null && block.blockMaterial == par2Material)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * checks if the given AABB is in the material given. Used while swimming.
     */
    public boolean isAABBInMaterial(AxisAlignedBB par1AxisAlignedBB, Material par2Material)
    {
        int i = MathHelper.floor_double(par1AxisAlignedBB.minX);
        int j = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0D);
        int k = MathHelper.floor_double(par1AxisAlignedBB.minY);
        int l = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0D);
        int i1 = MathHelper.floor_double(par1AxisAlignedBB.minZ);
        int j1 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    Block block = Block.blocksList[this.getBlockId(k1, l1, i2)];

                    if (block != null && block.blockMaterial == par2Material)
                    {
                        int j2 = this.getBlockMetadata(k1, l1, i2);
                        double d0 = (double)(l1 + 1);

                        if (j2 < 8)
                        {
                            d0 = (double)(l1 + 1) - (double)j2 / 8.0D;
                        }

                        if (d0 >= par1AxisAlignedBB.minY)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Creates an explosion. Args: entity, x, y, z, strength
     */
    public Explosion createExplosion(Entity par1Entity, double par2, double par4, double par6, float par8, boolean par9)
    {
        return this.newExplosion(par1Entity, par2, par4, par6, par8, false, par9);
    }

    /**
     * returns a new explosion. Does initiation (at time of writing Explosion is not finished)
     */
    public Explosion newExplosion(Entity par1Entity, double par2, double par4, double par6, float par8, boolean par9, boolean par10)
    {
        Explosion explosion = new Explosion(this, par1Entity, par2, par4, par6, par8);
        explosion.isFlaming = par9;
        explosion.isSmoking = par10;
        explosion.doExplosionA();
        explosion.doExplosionB(true);
        return explosion;
    }

    /**
     * Gets the percentage of real blocks within within a bounding box, along a specified vector.
     */
    public float getBlockDensity(Vec3 par1Vec3, AxisAlignedBB par2AxisAlignedBB)
    {
        double d0 = 1.0D / ((par2AxisAlignedBB.maxX - par2AxisAlignedBB.minX) * 2.0D + 1.0D);
        double d1 = 1.0D / ((par2AxisAlignedBB.maxY - par2AxisAlignedBB.minY) * 2.0D + 1.0D);
        double d2 = 1.0D / ((par2AxisAlignedBB.maxZ - par2AxisAlignedBB.minZ) * 2.0D + 1.0D);
        int i = 0;
        int j = 0;
        Vec3 vec32 = par1Vec3.myVec3LocalPool.getVecFromPool(0, 0, 0); // CraftBukkit

        for (float f = 0.0F; f <= 1.0F; f = (float)((double)f + d0))
        {
            for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float)((double)f1 + d1))
            {
                for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float)((double)f2 + d2))
                {
                    double d3 = par2AxisAlignedBB.minX + (par2AxisAlignedBB.maxX - par2AxisAlignedBB.minX) * (double)f;
                    double d4 = par2AxisAlignedBB.minY + (par2AxisAlignedBB.maxY - par2AxisAlignedBB.minY) * (double)f1;
                    double d5 = par2AxisAlignedBB.minZ + (par2AxisAlignedBB.maxZ - par2AxisAlignedBB.minZ) * (double)f2;

                    if (this.rayTraceBlocks(vec32.func_72439_b_CodeFix_Public(d3, d4, d5), par1Vec3) == null)   // CraftBukkit
                    {
                        ++i;
                    }

                    ++j;
                }
            }
        }

        vec32.myVec3LocalPool.release(vec32); // CraftBukkit
        return (float)i / (float)j;
    }

    /**
     * If the block in the given direction of the given coordinate is fire, extinguish it. Args: Player, X,Y,Z,
     * blockDirection
     */
    public boolean extinguishFire(EntityPlayer par1EntityPlayer, int par2, int par3, int par4, int par5)
    {
        if (par5 == 0)
        {
            --par3;
        }

        if (par5 == 1)
        {
            ++par3;
        }

        if (par5 == 2)
        {
            --par4;
        }

        if (par5 == 3)
        {
            ++par4;
        }

        if (par5 == 4)
        {
            --par2;
        }

        if (par5 == 5)
        {
            ++par2;
        }

        if (this.getBlockId(par2, par3, par4) == Block.fire.blockID)
        {
            this.playAuxSFXAtEntity(par1EntityPlayer, 1004, par2, par3, par4, 0);
            this.setBlockToAir(par2, par3, par4);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns the TileEntity associated with a given block in X,Y,Z coordinates, or null if no TileEntity exists
     */
    public TileEntity getBlockTileEntity(int par1, int par2, int par3)
    {
        if (par2 >= 0 && par2 < 256)
        {
            TileEntity tileentity = null;
            int l;
            TileEntity tileentity1;
            
            
            //MCPC+ start - BaseProtect, see if current interactor is allowed to see tileentity
            if(!Chunk.canSeeBlock(this, par1, par2, par3))
            	return null;

            if (this.scanningTileEntities)
            {
                for (l = 0; l < this.addedTileEntityList.size(); ++l)
                {
                    tileentity1 = (TileEntity)this.addedTileEntityList.get(l);

                    if (!tileentity1.isInvalid() && tileentity1.xCoord == par1 && tileentity1.yCoord == par2 && tileentity1.zCoord == par3)
                    {
                        tileentity = tileentity1;
                        break;
                    }
                }
            }

            if (tileentity == null)
            {
                Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);

                if (chunk != null)
                {
                    tileentity = chunk.getChunkBlockTileEntity(par1 & 15, par2, par3 & 15);
                }
            }

            if (tileentity == null)
            {
                for (l = 0; l < this.addedTileEntityList.size(); ++l)
                {
                    tileentity1 = (TileEntity)this.addedTileEntityList.get(l);

                    if (!tileentity1.isInvalid() && tileentity1.xCoord == par1 && tileentity1.yCoord == par2 && tileentity1.zCoord == par3)
                    {
                        tileentity = tileentity1;
                        break;
                    }
                }
            }

            return tileentity;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sets the TileEntity for a given block in X, Y, Z coordinates
     */
    public void setBlockTileEntity(int par1, int par2, int par3, TileEntity par4TileEntity)
    {
        if (par4TileEntity == null || par4TileEntity.isInvalid())
        {
            return;
        }
        
        //MCPC+ start - BaseProtect, Inherit owner from currently ticking item
        WorldInteract entityParent = this.currentTickItem;
        if(entityParent != null)
        {
        	//System.out.println(par4TileEntity.getClass().getName() + " TileParent: " + entityParent + "(" + entityParent.getItemOwner() + ")");
        	par4TileEntity.setItemOwner(entityParent.getItemOwner());
        }
        //MCPC+ end

        if (par4TileEntity.canUpdate())
        {
            if (scanningTileEntities)
            {
                Iterator iterator = addedTileEntityList.iterator();
                while (iterator.hasNext())
                {
                    TileEntity tileentity1 = (TileEntity)iterator.next();

                    if (tileentity1.xCoord == par1 && tileentity1.yCoord == par2 && tileentity1.zCoord == par3)
                    {
                        tileentity1.invalidate();
                        iterator.remove();
                    }
                }
                addedTileEntityList.add(par4TileEntity);
            }
            else
            {
                loadedTileEntityList.add(par4TileEntity);
            }
        }

        Chunk chunk = this.getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
        if (chunk != null)
        {
            chunk.setChunkBlockTileEntity(par1 & 15, par2, par3 & 15, par4TileEntity);
        }
    }

    /**
     * Removes the TileEntity for a given block in X,Y,Z coordinates
     */
    public void removeBlockTileEntity(int par1, int par2, int par3)
    {
        Chunk chunk = getChunkFromChunkCoords(par1 >> 4, par3 >> 4);
        if (chunk != null)
        {
            chunk.removeChunkBlockTileEntity(par1 & 15, par2, par3 & 15);
        }
    }

    /**
     * adds tile entity to despawn list (renamed from markEntityForDespawn)
     */
    public void markTileEntityForDespawn(TileEntity par1TileEntity)
    {
        this.entityRemoval.add(par1TileEntity);
    }

    /**
     * Returns true if the block at the specified coordinates is an opaque cube. Args: x, y, z
     */
    public boolean isBlockOpaqueCube(int par1, int par2, int par3)
    {
        Block block = Block.blocksList[this.getBlockId(par1, par2, par3)];
        return block == null ? false : block.isOpaqueCube();
    }

    /**
     * Indicate if a material is a normal solid opaque cube.
     */
    public boolean isBlockNormalCube(int par1, int par2, int par3)
    {
        Block block = Block.blocksList[getBlockId(par1, par2, par3)];
        return block != null && block.isBlockNormalCube(this, par1, par2, par3);
    }

    public boolean func_85174_u(int par1, int par2, int par3)
    {
        int l = this.getBlockId(par1, par2, par3);

        if (l != 0 && Block.blocksList[l] != null)
        {
            AxisAlignedBB axisalignedbb = Block.blocksList[l].getCollisionBoundingBoxFromPool(this, par1, par2, par3);
            return axisalignedbb != null && axisalignedbb.getAverageEdgeLength() >= 1.0D;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns true if the block at the given coordinate has a solid (buildable) top surface.
     */
    public boolean doesBlockHaveSolidTopSurface(int par1, int par2, int par3)
    {
        return isBlockSolidOnSide(par1, par2, par3, ForgeDirection.UP);
    }

    /**
     * Performs check to see if the block is a normal, solid block, or if the metadata of the block indicates that its
     * facing puts its solid side upwards. (inverted stairs, for example)
     */
    @Deprecated //DO NOT USE THIS!!! USE doesBlockHaveSolidTopSurface
    public boolean isBlockTopFacingSurfaceSolid(Block par1Block, int par2)
    {
        // -.-  Mojang PLEASE make this location sensitive, you have no reason not to.
        return par1Block == null ? false : (par1Block.blockMaterial.isOpaque() && par1Block.renderAsNormalBlock() ? true : (par1Block instanceof BlockStairs ? (par2 & 4) == 4 : (par1Block instanceof BlockHalfSlab ? (par2 & 8) == 8 : (par1Block instanceof BlockHopper ? true : (par1Block instanceof BlockSnow ? (par2 & 7) == 7 : false)))));
    }

    /**
     * Checks if the block is a solid, normal cube. If the chunk does not exist, or is not loaded, it returns the
     * boolean parameter.
     */
    public boolean isBlockNormalCubeDefault(int par1, int par2, int par3, boolean par4)
    {
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            Chunk chunk = this.chunkProvider.provideChunk(par1 >> 4, par3 >> 4);

            if (chunk != null && !chunk.isEmpty())
            {
                Block block = Block.blocksList[this.getBlockId(par1, par2, par3)];
                return block == null ? false : isBlockNormalCube(par1, par2, par3);
            }
            else
            {
                return par4;
            }
        }
        else
        {
            return par4;
        }
    }

    /**
     * Called on construction of the World class to setup the initial skylight values
     */
    public void calculateInitialSkylight()
    {
        int i = this.calculateSkylightSubtracted(1.0F);

        if (i != this.skylightSubtracted)
        {
            this.skylightSubtracted = i;
        }
    }

    /**
     * Set which types of mobs are allowed to spawn (peaceful vs hostile).
     */
    public void setAllowedSpawnTypes(boolean par1, boolean par2)
    {
        provider.setAllowedSpawnTypes(par1, par2);
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
        this.updateWeather();
    }

    /**
     * Called from World constructor to set rainingStrength and thunderingStrength
     */
    private void calculateInitialWeather()
    {
        provider.calculateInitialWeather();
    }

    public void calculateInitialWeatherBody()
    {
        if (this.worldInfo.isRaining())
        {
            this.rainingStrength = 1.0F;

            if (this.worldInfo.isThundering())
            {
                this.thunderingStrength = 1.0F;
            }
        }
    }

    /**
     * Updates all weather states.
     */
    protected void updateWeather()
    {
        provider.updateWeather();
    }

    public void updateWeatherBody()
    {
        if (!this.provider.hasNoSky)
        {
            int i = this.worldInfo.getThunderTime();

            if (i <= 0)
            {
                if (this.worldInfo.isThundering())
                {
                    this.worldInfo.setThunderTime(this.rand.nextInt(12000) + 3600);
                }
                else
                {
                    this.worldInfo.setThunderTime(this.rand.nextInt(168000) + 12000);
                }
            }
            else
            {
                --i;
                this.worldInfo.setThunderTime(i);

                if (i <= 0)
                {
                    // CraftBukkit start
                    ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), !this.worldInfo.isThundering());
                    this.getServer().getPluginManager().callEvent(thunder);

                    if (!thunder.isCancelled())
                    {
                        this.worldInfo.setThundering(!this.worldInfo.isThundering());
                    }

                    // CraftBukkit end
                }
            }

            int j = this.worldInfo.getRainTime();

            if (j <= 0)
            {
                if (this.worldInfo.isRaining())
                {
                    this.worldInfo.setRainTime(this.rand.nextInt(12000) + 12000);
                }
                else
                {
                    this.worldInfo.setRainTime(this.rand.nextInt(168000) + 12000);
                }
            }
            else
            {
                --j;
                this.worldInfo.setRainTime(j);

                if (j <= 0)
                {
                    // CraftBukkit start
                    WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), !this.worldInfo.isRaining());
                    this.getServer().getPluginManager().callEvent(weather);

                    if (!weather.isCancelled())
                    {
                        this.worldInfo.setRaining(!this.worldInfo.isRaining());
                    }

                    // CraftBukkit end
                }
            }

            this.prevRainingStrength = this.rainingStrength;

            if (this.worldInfo.isRaining())
            {
                this.rainingStrength = (float)((double)this.rainingStrength + 0.01D);
            }
            else
            {
                this.rainingStrength = (float)((double)this.rainingStrength - 0.01D);
            }

            if (this.rainingStrength < 0.0F)
            {
                this.rainingStrength = 0.0F;
            }

            if (this.rainingStrength > 1.0F)
            {
                this.rainingStrength = 1.0F;
            }

            this.prevThunderingStrength = this.thunderingStrength;

            if (this.worldInfo.isThundering())
            {
                this.thunderingStrength = (float)((double)this.thunderingStrength + 0.01D);
            }
            else
            {
                this.thunderingStrength = (float)((double)this.thunderingStrength - 0.01D);
            }

            if (this.thunderingStrength < 0.0F)
            {
                this.thunderingStrength = 0.0F;
            }

            if (this.thunderingStrength > 1.0F)
            {
                this.thunderingStrength = 1.0F;
            }
        }
    }

    public void toggleRain()
    {
        provider.toggleRain();
    }

    // Spigot start
    public int aggregateTicks = 1;
    protected float modifiedOdds = 100F;
    public float growthOdds = 100F;

    protected void setActivePlayerChunksAndCheckLight()
    {
        // MCPC+ start - add persistent chunks to be ticked for growth
        activeChunkSet.clear();
        activeChunkSet_CB.clear();
        for(ChunkCoordIntPair chunk : getPersistentChunks().keySet()) {
            this.activeChunkSet.add(chunk);
            long key = chunkToKey(chunk.chunkXPos, chunk.chunkZPos);
            activeChunkSet_CB.put(key, (short) 0);
            if (!this.chunkExists(chunk.chunkXPos, chunk.chunkZPos)) {
                ((WorldServer)this).theChunkProviderServer.loadChunk(chunk.chunkXPos, chunk.chunkZPos);
            }
        }
        // MCPC+ end

        // this.chunkTickList.clear(); // CraftBukkit - removed
        this.theProfiler.startSection("buildList");
        int i;
        EntityPlayer entityplayer;
        int j;
        int k;
        final int optimalChunks = this.getWorld().growthPerTick;

        if (optimalChunks <= 0)
        {
            return;
        }

        /*if (playerEntities.size() == 0) // MCPC+ tick chunks even if no players are logged in
        {
            return;
        }*/

        // Keep chunks with growth inside of the optimal chunk range
        int chunksPerPlayer = Math.min(200, Math.max(1, (int)(((optimalChunks - playerEntities.size()) / (double) playerEntities.size()) + 0.5)));
        int randRange = 3 + chunksPerPlayer / 30;

        if (randRange > chunkTickRadius)   // Limit to normal tick radius - including view distance
        {
            randRange = chunkTickRadius;
        }

        // odds of growth happening vs growth happening in vanilla
        final float modifiedOdds = Math.max(35, Math.min(100, ((chunksPerPlayer + 1) * 100F) / 15F));
        this.modifiedOdds = modifiedOdds;
        this.growthOdds = modifiedOdds;

        for (i = 0; i < this.playerEntities.size(); ++i)
        {
            entityplayer = (EntityPlayer)this.playerEntities.get(i);
            int chunkX = MathHelper.floor_double(entityplayer.posX / 16.0D);
            int chunkZ = MathHelper.floor_double(entityplayer.posZ / 16.0D);
            // Always update the chunk the player is on
            long key = chunkToKey(chunkX, chunkZ);
            int existingPlayers = Math.max(0, activeChunkSet_CB.get(key)); //filter out -1's
            activeChunkSet_CB.put(key, (short)(existingPlayers + 1));
            activeChunkSet.add(new ChunkCoordIntPair(chunkX, chunkZ)); // MCPC+ - vanilla compatibility

            // Check and see if we update the chunks surrounding the player this tick
            for (int chunk = 0; chunk < chunksPerPlayer; chunk++)
            {
                int dx = (rand.nextBoolean() ? 1 : -1) * rand.nextInt(randRange);
                int dz = (rand.nextBoolean() ? 1 : -1) * rand.nextInt(randRange);
                long hash = chunkToKey(dx + chunkX, dz + chunkZ);

                if (!activeChunkSet_CB.contains(hash) && this.chunkExists(dx + chunkX, dz + chunkZ))
                {
                    activeChunkSet_CB.put(hash, (short) - 1); //no players
                    activeChunkSet.add(new ChunkCoordIntPair(dx + chunkX, dz + chunkZ)); // MCPC+ - vanilla compatibility
                }
            }
        }

        // Spigot End
        this.theProfiler.endSection();

        if (this.ambientTickCountdown > 0)
        {
            --this.ambientTickCountdown;
        }

        this.theProfiler.startSection("playerCheckLight");

        if (!this.playerEntities.isEmpty() && this.getWorld().randomLightingUpdates)   // Spigot
        {
            i = this.rand.nextInt(this.playerEntities.size());
            entityplayer = (EntityPlayer)this.playerEntities.get(i);
            j = MathHelper.floor_double(entityplayer.posX) + this.rand.nextInt(11) - 5;
            k = MathHelper.floor_double(entityplayer.posY) + this.rand.nextInt(11) - 5;
            int j1 = MathHelper.floor_double(entityplayer.posZ) + this.rand.nextInt(11) - 5;
            this.updateAllLightTypes(j, k, j1);
        }

        this.theProfiler.endSection();
    }

    protected void moodSoundAndLightCheck(int par1, int par2, Chunk par3Chunk)
    {
        this.theProfiler.endStartSection("moodSound");

        if (this.ambientTickCountdown == 0 && !this.isRemote)
        {
            this.updateLCG = this.updateLCG * 3 + 1013904223;
            int k = this.updateLCG >> 2;
            int l = k & 15;
            int i1 = k >> 8 & 15;
            int j1 = k >> 16 & 255; // CraftBukkit - 127 -> 255
            int k1 = par3Chunk.getBlockID(l, j1, i1);
            l += par1;
            i1 += par2;

            if (k1 == 0 && this.getFullBlockLightValue(l, j1, i1) <= this.rand.nextInt(8) && this.getSavedLightValue(EnumSkyBlock.Sky, l, j1, i1) <= 0)
            {
                EntityPlayer entityplayer = this.getClosestPlayer((double)l + 0.5D, (double)j1 + 0.5D, (double)i1 + 0.5D, 8.0D);

                if (entityplayer != null && entityplayer.getDistanceSq((double)l + 0.5D, (double)j1 + 0.5D, (double)i1 + 0.5D) > 4.0D)
                {
                    this.playSoundEffect((double)l + 0.5D, (double)j1 + 0.5D, (double)i1 + 0.5D, "ambient.cave.cave", 0.7F, 0.8F + this.rand.nextFloat() * 0.2F);
                    this.ambientTickCountdown = this.rand.nextInt(12000) + 6000;
                }
            }
        }

        this.theProfiler.endStartSection("checkLight");
        par3Chunk.enqueueRelightChecks();
    }

    // Spigot start
    /**
     * plays random cave ambient sounds and runs updateTick on random blocks within each chunk in the vacinity of a
     * player
     */
    protected void tickBlocksAndAmbiance()
    {
        try
        {
            this.setActivePlayerChunksAndCheckLight();
        }
        catch (Exception e)
        {
            org.bukkit.craftbukkit.util.ExceptionReporter.handle(e, "Spigot has detected an unexpected exception while ticking chunks");
        }
    }
    // Spigot end

    /**
     * checks to see if a given block is both water and is cold enough to freeze
     */
    public boolean isBlockFreezable(int par1, int par2, int par3)
    {
        return this.canBlockFreeze(par1, par2, par3, false);
    }

    /**
     * checks to see if a given block is both water and has at least one immediately adjacent non-water block
     */
    public boolean isBlockFreezableNaturally(int par1, int par2, int par3)
    {
        return this.canBlockFreeze(par1, par2, par3, true);
    }

    /**
     * checks to see if a given block is both water, and cold enough to freeze - if the par4 boolean is set, this will
     * only return true if there is a non-water block immediately adjacent to the specified block
     */
    public boolean canBlockFreeze(int par1, int par2, int par3, boolean par4)
    {
        return provider.canBlockFreeze(par1, par2, par3, par4);
    }

    public boolean canBlockFreezeBody(int par1, int par2, int par3, boolean par4)
    {
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(par1, par3);
        float f = biomegenbase.getFloatTemperature();

        if (f > 0.15F)
        {
            return false;
        }
        else
        {
            if (par2 >= 0 && par2 < 256 && this.getSavedLightValue(EnumSkyBlock.Block, par1, par2, par3) < 10)
            {
                int l = this.getBlockId(par1, par2, par3);

                if ((l == Block.waterStill.blockID || l == Block.waterMoving.blockID) && this.getBlockMetadata(par1, par2, par3) == 0)
                {
                    if (!par4)
                    {
                        return true;
                    }

                    boolean flag1 = true;

                    if (flag1 && this.getBlockMaterial(par1 - 1, par2, par3) != Material.water)
                    {
                        flag1 = false;
                    }

                    if (flag1 && this.getBlockMaterial(par1 + 1, par2, par3) != Material.water)
                    {
                        flag1 = false;
                    }

                    if (flag1 && this.getBlockMaterial(par1, par2, par3 - 1) != Material.water)
                    {
                        flag1 = false;
                    }

                    if (flag1 && this.getBlockMaterial(par1, par2, par3 + 1) != Material.water)
                    {
                        flag1 = false;
                    }

                    if (!flag1)
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * Tests whether or not snow can be placed at a given location
     */
    public boolean canSnowAt(int par1, int par2, int par3)
    {
        return provider.canSnowAt(par1, par2, par3);
    }

    public boolean canSnowAtBody(int par1, int par2, int par3)
    {
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(par1, par3);
        float f = biomegenbase.getFloatTemperature();

        if (f > 0.15F)
        {
            return false;
        }
        else
        {
            if (par2 >= 0 && par2 < 256 && this.getSavedLightValue(EnumSkyBlock.Block, par1, par2, par3) < 10)
            {
                int l = this.getBlockId(par1, par2 - 1, par3);
                int i1 = this.getBlockId(par1, par2, par3);

                if (i1 == 0 && Block.snow.canPlaceBlockAt(this, par1, par2, par3) && l != 0 && l != Block.ice.blockID && Block.blocksList[l].blockMaterial.blocksMovement())
                {
                    return true;
                }
            }

            return false;
        }
    }

    public void updateAllLightTypes(int par1, int par2, int par3)
    {
        if (!this.provider.hasNoSky)
        {
            this.updateLightByType(EnumSkyBlock.Sky, par1, par2, par3);
        }

        this.updateLightByType(EnumSkyBlock.Block, par1, par2, par3);
    }

    private int computeLightValue(int par1, int par2, int par3, EnumSkyBlock par4EnumSkyBlock)
    {
        if (par4EnumSkyBlock == EnumSkyBlock.Sky && this.canBlockSeeTheSky(par1, par2, par3))
        {
            return 15;
        }
        else
        {
            int l = this.getBlockId(par1, par2, par3);
            Block block = Block.blocksList[l];
            int blockLight = (block == null ? 0 : block.getLightValue(this, par1, par2, par3));
            int i1 = par4EnumSkyBlock == EnumSkyBlock.Sky ? 0 : blockLight;
            int j1 = (block == null ? 0 : block.getLightOpacity(this, par1, par2, par3));

            if (j1 >= 15 && blockLight > 0)
            {
                j1 = 1;
            }

            if (j1 < 1)
            {
                j1 = 1;
            }

            if (j1 >= 15)
            {
                return 0;
            }
            else if (i1 >= 14)
            {
                return i1;
            }
            else
            {
                for (int k1 = 0; k1 < 6; ++k1)
                {
                    int l1 = par1 + Facing.offsetsXForSide[k1];
                    int i2 = par2 + Facing.offsetsYForSide[k1];
                    int j2 = par3 + Facing.offsetsZForSide[k1];
                    int k2 = this.getSavedLightValue(par4EnumSkyBlock, l1, i2, j2) - j1;

                    if (k2 > i1)
                    {
                        i1 = k2;
                    }

                    if (i1 >= 14)
                    {
                        return i1;
                    }
                }

                return i1;
            }
        }
    }

    public void updateLightByType(EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4)
    {
        if (this.doChunksNearChunkExist(par2, par3, par4, 17))
        {
            int l = 0;
            int i1 = 0;
            this.theProfiler.startSection("getBrightness");
            int j1 = this.getSavedLightValue(par1EnumSkyBlock, par2, par3, par4);
            int k1 = this.computeLightValue(par2, par3, par4, par1EnumSkyBlock);
            int l1;
            int i2;
            int j2;
            int k2;
            int l2;
            int i3;
            int j3;
            int k3;
            int l3;

            if (k1 > j1)
            {
                this.lightUpdateBlockList[i1++] = 133152;
            }
            else if (k1 < j1)
            {
                this.lightUpdateBlockList[i1++] = 133152 | j1 << 18;

                while (l < i1)
                {
                    l1 = this.lightUpdateBlockList[l++];
                    i2 = (l1 & 63) - 32 + par2;
                    j2 = (l1 >> 6 & 63) - 32 + par3;
                    k2 = (l1 >> 12 & 63) - 32 + par4;
                    l2 = l1 >> 18 & 15;
                    i3 = this.getSavedLightValue(par1EnumSkyBlock, i2, j2, k2);

                    if (i3 == l2)
                    {
                        this.setLightValue(par1EnumSkyBlock, i2, j2, k2, 0);

                        if (l2 > 0)
                        {
                            j3 = MathHelper.abs_int(i2 - par2);
                            l3 = MathHelper.abs_int(j2 - par3);
                            k3 = MathHelper.abs_int(k2 - par4);

                            if (j3 + l3 + k3 < 17)
                            {
                                for (int i4 = 0; i4 < 6; ++i4)
                                {
                                    int j4 = i2 + Facing.offsetsXForSide[i4];
                                    int k4 = j2 + Facing.offsetsYForSide[i4];
                                    int l4 = k2 + Facing.offsetsZForSide[i4];
                                    Block block = Block.blocksList[getBlockId(j4, k4, l4)];
                                    int blockOpacity = (block == null ? 0 : block.getLightOpacity(this, j4, k4, l4));
                                    int i5 = Math.max(1, blockOpacity);
                                    i3 = this.getSavedLightValue(par1EnumSkyBlock, j4, k4, l4);

                                    if (i3 == l2 - i5 && i1 < this.lightUpdateBlockList.length)
                                    {
                                        this.lightUpdateBlockList[i1++] = j4 - par2 + 32 | k4 - par3 + 32 << 6 | l4 - par4 + 32 << 12 | l2 - i5 << 18;
                                    }
                                }
                            }
                        }
                    }
                }

                l = 0;
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("checkedPosition < toCheckCount");

            while (l < i1)
            {
                l1 = this.lightUpdateBlockList[l++];
                i2 = (l1 & 63) - 32 + par2;
                j2 = (l1 >> 6 & 63) - 32 + par3;
                k2 = (l1 >> 12 & 63) - 32 + par4;
                l2 = this.getSavedLightValue(par1EnumSkyBlock, i2, j2, k2);
                i3 = this.computeLightValue(i2, j2, k2, par1EnumSkyBlock);

                if (i3 != l2)
                {
                    this.setLightValue(par1EnumSkyBlock, i2, j2, k2, i3);

                    if (i3 > l2)
                    {
                        j3 = Math.abs(i2 - par2);
                        l3 = Math.abs(j2 - par3);
                        k3 = Math.abs(k2 - par4);
                        boolean flag = i1 < this.lightUpdateBlockList.length - 6;

                        if (j3 + l3 + k3 < 17 && flag)
                        {
                            if (this.getSavedLightValue(par1EnumSkyBlock, i2 - 1, j2, k2) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 - 1 - par2 + 32 + (j2 - par3 + 32 << 6) + (k2 - par4 + 32 << 12);
                            }

                            if (this.getSavedLightValue(par1EnumSkyBlock, i2 + 1, j2, k2) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 + 1 - par2 + 32 + (j2 - par3 + 32 << 6) + (k2 - par4 + 32 << 12);
                            }

                            if (this.getSavedLightValue(par1EnumSkyBlock, i2, j2 - 1, k2) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 - par2 + 32 + (j2 - 1 - par3 + 32 << 6) + (k2 - par4 + 32 << 12);
                            }

                            if (this.getSavedLightValue(par1EnumSkyBlock, i2, j2 + 1, k2) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 - par2 + 32 + (j2 + 1 - par3 + 32 << 6) + (k2 - par4 + 32 << 12);
                            }

                            if (this.getSavedLightValue(par1EnumSkyBlock, i2, j2, k2 - 1) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 - par2 + 32 + (j2 - par3 + 32 << 6) + (k2 - 1 - par4 + 32 << 12);
                            }

                            if (this.getSavedLightValue(par1EnumSkyBlock, i2, j2, k2 + 1) < i3)
                            {
                                this.lightUpdateBlockList[i1++] = i2 - par2 + 32 + (j2 - par3 + 32 << 6) + (k2 + 1 - par4 + 32 << 12);
                            }
                        }
                    }
                }
            }

            this.theProfiler.endSection();
        }
    }

    /**
     * Runs through the list of updates to run and ticks them
     */
    public boolean tickUpdates(boolean par1)
    {
        return false;
    }

    public List getPendingBlockUpdates(Chunk par1Chunk, boolean par2)
    {
        return null;
    }

    /**
     * Will get all entities within the specified AABB excluding the one passed into it. Args: entityToExclude, aabb
     */
    public List getEntitiesWithinAABBExcludingEntity(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB)
    {
        return this.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB, (IEntitySelector)null);
    }

    public List getEntitiesWithinAABBExcludingEntity(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB, IEntitySelector par3IEntitySelector)
    {
        ArrayList arraylist = new ArrayList();
        int i = MathHelper.floor_double((par2AxisAlignedBB.minX - MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((par2AxisAlignedBB.maxX + MAX_ENTITY_RADIUS) / 16.0D);
        int k = MathHelper.floor_double((par2AxisAlignedBB.minZ - MAX_ENTITY_RADIUS) / 16.0D);
        int l = MathHelper.floor_double((par2AxisAlignedBB.maxZ + MAX_ENTITY_RADIUS) / 16.0D);

        for (int i1 = i; i1 <= j; ++i1)
        {
            for (int j1 = k; j1 <= l; ++j1)
            {
                if (this.chunkExists(i1, j1))
                {
                    this.getChunkFromChunkCoords(i1, j1).getEntitiesWithinAABBForEntity(par1Entity, par2AxisAlignedBB, arraylist, par3IEntitySelector);
                }
            }
        }

        return arraylist;
    }

    /**
     * Returns all entities of the specified class type which intersect with the AABB. Args: entityClass, aabb
     */
    public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB)
    {
        return this.selectEntitiesWithinAABB(par1Class, par2AxisAlignedBB, (IEntitySelector)null);
    }

    public List selectEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB, IEntitySelector par3IEntitySelector)
    {
        int i = MathHelper.floor_double((par2AxisAlignedBB.minX - MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((par2AxisAlignedBB.maxX + MAX_ENTITY_RADIUS) / 16.0D);
        int k = MathHelper.floor_double((par2AxisAlignedBB.minZ - MAX_ENTITY_RADIUS) / 16.0D);
        int l = MathHelper.floor_double((par2AxisAlignedBB.maxZ + MAX_ENTITY_RADIUS) / 16.0D);
        ArrayList arraylist = new ArrayList();

        for (int i1 = i; i1 <= j; ++i1)
        {
            for (int j1 = k; j1 <= l; ++j1)
            {
                if (this.chunkExists(i1, j1))
                {
                    this.getChunkFromChunkCoords(i1, j1).getEntitiesOfTypeWithinAAAB(par1Class, par2AxisAlignedBB, arraylist, par3IEntitySelector);
                }
            }
        }

        return arraylist;
    }

    public Entity findNearestEntityWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB, Entity par3Entity)
    {
        List list = this.getEntitiesWithinAABB(par1Class, par2AxisAlignedBB);
        Entity entity1 = null;
        double d0 = Double.MAX_VALUE;

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity2 = (Entity)list.get(i);

            if (entity2 != par3Entity)
            {
                double d1 = par3Entity.getDistanceSqToEntity(entity2);

                if (d1 <= d0)
                {
                    entity1 = entity2;
                    d0 = d1;
                }
            }
        }

        return entity1;
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this World.
     */
    public abstract Entity getEntityByID(int i);

    /**
     * Accessor for world Loaded Entity List
     */
    public List getLoadedEntityList()
    {
        return this.loadedEntityList;
    }

    /**
     * marks the chunk that contains this tilentity as modified and then calls worldAccesses.doNothingWithTileEntity
     */
    public void updateTileEntityChunkAndDoNothing(int par1, int par2, int par3, TileEntity par4TileEntity)
    {
        if (this.blockExists(par1, par2, par3))
        {
            this.getChunkFromBlockCoords(par1, par3).setChunkModified();
        }
    }

    /**
     * Counts how many entities of an entity class exist in the world. Args: entityClass
     */
    public int countEntities(Class par1Class)
    {
        int i = 0;

        for (int j = 0; j < this.loadedEntityList.size(); ++j)
        {
            Entity entity = (Entity)this.loadedEntityList.get(j);

            // CraftBukkit start - Split out persistent check, don't apply it to special persistent mobs
            if (entity instanceof EntityLiving)
            {
                EntityLiving entityliving = (EntityLiving) entity;

                if (entityliving.func_70692_ba_CodeFix_Public() && entityliving.func_104002_bU())   // Should be isPersistent
                {
                    continue;
                }
            }

            if (par1Class.isAssignableFrom(entity.getClass()))
            {
                ++i;
            }

            // CraftBukkit end
        }

        return i;
    }

    /**
     * adds entities to the loaded entities list, and loads thier skins.
     */
    public void addLoadedEntities(List par1List)
    {
        // CraftBukkit start
        Entity entity = null;

        for (int i = 0; i < par1List.size(); ++i)
        {
            entity = (Entity) par1List.get(i);

            if (entity == null)
            {
                continue;
            }

            // CraftBukkit end
            if (!MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, this)))
            {
                loadedEntityList.add(entity);
                this.obtainEntitySkin(entity);
            }
        }
    }

    /**
     * Adds a list of entities to be unloaded on the next pass of World.updateEntities()
     */
    public void unloadEntities(List par1List)
    {
        this.unloadedEntityList.addAll(par1List);
    }

    /**
     * Returns true if the given Entity can be placed on the given side of the given block position.
     */
    public boolean canPlaceEntityOnSide(int par1, int par2, int par3, int par4, boolean par5, int par6, Entity par7Entity, ItemStack par8ItemStack)
    {
        int j1 = this.getBlockId(par2, par3, par4);
        Block block = Block.blocksList[j1];
        Block block1 = Block.blocksList[par1];
        AxisAlignedBB axisalignedbb = block1.getCollisionBoundingBoxFromPool(this, par2, par3, par4);

        if (par5)
        {
            axisalignedbb = null;
        }

        boolean defaultReturn; // CraftBukkit - store the default action
        if (axisalignedbb != null && !this.checkNoEntityCollision(axisalignedbb, par7Entity))
        {
            defaultReturn = false; // CraftBukkit
        }
        else
        {
            if (block != null && (block == Block.waterMoving || block == Block.waterStill || block == Block.lavaMoving || block == Block.lavaStill || block == Block.fire || block.blockMaterial.isReplaceable()))
            {
                block = null;
            }

            // CraftBukkit
            defaultReturn = block != null && block.blockMaterial == Material.circuits && block1 == Block.anvil ? true : par1 > 0 && block == null && block1.canPlaceBlockOnSide(this, par2, par3, par4, par6, par8ItemStack);
        }

        // CraftBukkit start
        BlockCanBuildEvent event = new BlockCanBuildEvent(this.getWorld().getBlockAt(par2, par3, par4), par1, defaultReturn);
        this.getServer().getPluginManager().callEvent(event);
        boolean result = event.isBuildable();
        // MCPC+ start - all forge blocks will now send a BlockPlaceEvent here to allow events to occur with mods 
        //               that override ItemBlock.onItemUse and ItemBlock.placeBlockAt such as RP2's microblocks, BC pipes, etc.
        if (par7Entity != null && !this.callingPlaceEvent && block1 != null && block1.isForgeBlock && result)
        {
            if (par7Entity instanceof EntityPlayer)
            {
                EntityPlayer player = (EntityPlayer)par7Entity;
                ItemStack itemstack = (player.getCurrentEquippedItem() != null ? player.getCurrentEquippedItem() : null);
                NBTTagCompound savedCompound = null;
                if (itemstack != null && itemstack.getTagCompound() != null)
                {
                    savedCompound = itemstack.getTagCompound(); // save current itemstack NBT
                    itemstack.setTagCompound(new NBTTagCompound()); // dont use any itemstack NBT in our simulation, fixes MFR DSU dupe
                }
                org.bukkit.block.BlockState blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(this, par2, par3, par4);
                this.callingPlaceEvent = true;
                if (itemstack != null && itemstack.getItem() instanceof ItemBlock)
                {
                    ItemBlock itemblock = (ItemBlock)itemstack.getItem();
                    int itemData = itemblock.getMetadata(itemstack.getItemDamage());
                    int metadata = Block.blocksList[par1].onBlockPlaced(this, par2, par3, par4, par6, this.curPlacedItemHitX, this.curPlacedItemHitY, this.curPlacedItemHitZ, itemData);
                    if (itemblock.placeBlockAt(itemstack, player, this, par2, par3, par4, par6, this.curPlacedItemHitX, this.curPlacedItemHitY, this.curPlacedItemHitZ, metadata))
                    {
                        // since this is only a simulation, there is no need to play sound or decrement stacksize
                    }
                }
                this.curPlacedItemHitX = 0;
                this.curPlacedItemHitY = 0;
                this.curPlacedItemHitZ = 0;

                // send place event
                org.bukkit.event.block.BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(this, player, blockstate, par2, par3, par4);

                if (placeEvent.isCancelled() || !placeEvent.canBuild())
                {
                    result = false; // cancel placement
                }
                blockstate.update(true, false); // revert blockstate since this was only a simulation
                this.callingPlaceEvent = false;
                if (savedCompound != null)
                {
                    itemstack.setTagCompound(savedCompound); // restore itemstack NBT
                }
            }
        }
        return result;
        // MCPC+ end
    }

    public PathEntity getPathEntityToEntity(Entity par1Entity, Entity par2Entity, float par3, boolean par4, boolean par5, boolean par6, boolean par7)
    {
        this.theProfiler.startSection("pathfind");
        int i = MathHelper.floor_double(par1Entity.posX);
        int j = MathHelper.floor_double(par1Entity.posY + 1.0D);
        int k = MathHelper.floor_double(par1Entity.posZ);
        int l = (int)(par3 + 16.0F);
        int i1 = i - l;
        int j1 = j - l;
        int k1 = k - l;
        int l1 = i + l;
        int i2 = j + l;
        int j2 = k + l;
        ChunkCache chunkcache = new ChunkCache(this, i1, j1, k1, l1, i2, j2, 0);
        PathEntity pathentity = (new PathFinder(chunkcache, par4, par5, par6, par7)).createEntityPathTo(par1Entity, par2Entity, par3);
        this.theProfiler.endSection();
        return pathentity;
    }

    public PathEntity getEntityPathToXYZ(Entity par1Entity, int par2, int par3, int par4, float par5, boolean par6, boolean par7, boolean par8, boolean par9)
    {
        this.theProfiler.startSection("pathfind");
        int l = MathHelper.floor_double(par1Entity.posX);
        int i1 = MathHelper.floor_double(par1Entity.posY);
        int j1 = MathHelper.floor_double(par1Entity.posZ);
        int k1 = (int)(par5 + 8.0F);
        int l1 = l - k1;
        int i2 = i1 - k1;
        int j2 = j1 - k1;
        int k2 = l + k1;
        int l2 = i1 + k1;
        int i3 = j1 + k1;
        ChunkCache chunkcache = new ChunkCache(this, l1, i2, j2, k2, l2, i3, 0);
        PathEntity pathentity = (new PathFinder(chunkcache, par6, par7, par8, par9)).createEntityPathTo(par1Entity, par2, par3, par4, par5);
        this.theProfiler.endSection();
        return pathentity;
    }

    /**
     * Is this block powering in the specified direction Args: x, y, z, direction
     */
    public int isBlockProvidingPowerTo(int par1, int par2, int par3, int par4)
    {
        int i1 = this.getBlockId(par1, par2, par3);
        return i1 == 0 ? 0 : Block.blocksList[i1].isProvidingStrongPower(this, par1, par2, par3, par4);
    }

    /**
     * Returns the highest redstone signal strength powering the given block. Args: X, Y, Z.
     */
    public int getBlockPowerInput(int par1, int par2, int par3)
    {
        byte b0 = 0;
        int l = Math.max(b0, this.isBlockProvidingPowerTo(par1, par2 - 1, par3, 0));

        if (l >= 15)
        {
            return l;
        }
        else
        {
            l = Math.max(l, this.isBlockProvidingPowerTo(par1, par2 + 1, par3, 1));

            if (l >= 15)
            {
                return l;
            }
            else
            {
                l = Math.max(l, this.isBlockProvidingPowerTo(par1, par2, par3 - 1, 2));

                if (l >= 15)
                {
                    return l;
                }
                else
                {
                    l = Math.max(l, this.isBlockProvidingPowerTo(par1, par2, par3 + 1, 3));

                    if (l >= 15)
                    {
                        return l;
                    }
                    else
                    {
                        l = Math.max(l, this.isBlockProvidingPowerTo(par1 - 1, par2, par3, 4));

                        if (l >= 15)
                        {
                            return l;
                        }
                        else
                        {
                            l = Math.max(l, this.isBlockProvidingPowerTo(par1 + 1, par2, par3, 5));
                            return l >= 15 ? l : l;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the indirect signal strength being outputted by the given block in the *opposite* of the given direction.
     * Args: X, Y, Z, direction
     */
    public boolean getIndirectPowerOutput(int par1, int par2, int par3, int par4)
    {
        return this.getIndirectPowerLevelTo(par1, par2, par3, par4) > 0;
    }

    /**
     * Gets the power level from a certain block face.  Args: x, y, z, direction
     */
    public int getIndirectPowerLevelTo(int par1, int par2, int par3, int par4)
    {
        if (this.isBlockNormalCube(par1, par2, par3))
        {
            return this.getBlockPowerInput(par1, par2, par3);
        }
        else
        {
            int i1 = this.getBlockId(par1, par2, par3);
            return i1 == 0 ? 0 : Block.blocksList[i1].isProvidingWeakPower(this, par1, par2, par3, par4);
        }
    }

    /**
     * Used to see if one of the blocks next to you or your block is getting power from a neighboring block. Used by
     * items like TNT or Doors so they don't have redstone going straight into them.  Args: x, y, z
     */
    public boolean isBlockIndirectlyGettingPowered(int par1, int par2, int par3)
    {
        return this.getIndirectPowerLevelTo(par1, par2 - 1, par3, 0) > 0 ? true : (this.getIndirectPowerLevelTo(par1, par2 + 1, par3, 1) > 0 ? true : (this.getIndirectPowerLevelTo(par1, par2, par3 - 1, 2) > 0 ? true : (this.getIndirectPowerLevelTo(par1, par2, par3 + 1, 3) > 0 ? true : (this.getIndirectPowerLevelTo(par1 - 1, par2, par3, 4) > 0 ? true : this.getIndirectPowerLevelTo(par1 + 1, par2, par3, 5) > 0))));
    }

    public int getStrongestIndirectPower(int par1, int par2, int par3)
    {
        int l = 0;

        for (int i1 = 0; i1 < 6; ++i1)
        {
            int j1 = this.getIndirectPowerLevelTo(par1 + Facing.offsetsXForSide[i1], par2 + Facing.offsetsYForSide[i1], par3 + Facing.offsetsZForSide[i1], i1);

            if (j1 >= 15)
            {
                return 15;
            }

            if (j1 > l)
            {
                l = j1;
            }
        }

        return l;
    }

    /**
     * Gets the closest player to the entity within the specified distance (if distance is less than 0 then ignored).
     * Args: entity, dist
     */
    public EntityPlayer getClosestPlayerToEntity(Entity par1Entity, double par2)
    {
        return this.getClosestPlayer(par1Entity.posX, par1Entity.posY, par1Entity.posZ, par2);
    }

    /**
     * Gets the closest player to the point within the specified distance (distance can be set to less than 0 to not
     * limit the distance). Args: x, y, z, dist
     */
    public EntityPlayer getClosestPlayer(double par1, double par3, double par5, double par7)
    {
        double d4 = -1.0D;
        EntityPlayer entityplayer = null;

        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            EntityPlayer entityplayer1 = (EntityPlayer)this.playerEntities.get(i);

            // CraftBukkit start - Fixed an NPE
            if (entityplayer1 == null || entityplayer1.isDead)
            {
                continue;
            }

            // CraftBukkit end
            double d5 = entityplayer1.getDistanceSq(par1, par3, par5);

            if ((par7 < 0.0D || d5 < par7 * par7) && (d4 == -1.0D || d5 < d4))
            {
                d4 = d5;
                entityplayer = entityplayer1;
            }
        }

        return entityplayer;
    }

    /**
     * Returns the closest vulnerable player to this entity within the given radius, or null if none is found
     */
    public EntityPlayer getClosestVulnerablePlayerToEntity(Entity par1Entity, double par2)
    {
        return this.getClosestVulnerablePlayer(par1Entity.posX, par1Entity.posY, par1Entity.posZ, par2);
    }

    /**
     * Returns the closest vulnerable player within the given radius, or null if none is found.
     */
    public EntityPlayer getClosestVulnerablePlayer(double par1, double par3, double par5, double par7)
    {
        double d4 = -1.0D;
        EntityPlayer entityplayer = null;

        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            EntityPlayer entityplayer1 = (EntityPlayer)this.playerEntities.get(i);

            // CraftBukkit start - Fixed an NPE
            if (entityplayer1 == null || entityplayer1.isDead)
            {
                continue;
            }

            // CraftBukkit end

            if (!entityplayer1.capabilities.disableDamage && entityplayer1.isEntityAlive())
            {
                double d5 = entityplayer1.getDistanceSq(par1, par3, par5);
                double d6 = par7;

                if (entityplayer1.isSneaking())
                {
                    d6 = par7 * 0.800000011920929D;
                }

                if (entityplayer1.isInvisible())
                {
                    float f = entityplayer1.func_82243_bO();

                    if (f < 0.1F)
                    {
                        f = 0.1F;
                    }

                    d6 *= (double)(0.7F * f);
                }

                if ((par7 < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4))
                {
                    d4 = d5;
                    entityplayer = entityplayer1;
                }
            }
        }

        return entityplayer;
    }

    /**
     * Find a player by name in this world.
     */
    public EntityPlayer getPlayerEntityByName(String par1Str)
    {
        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            if (par1Str.equals(((EntityPlayer)this.playerEntities.get(i)).username))
            {
                return (EntityPlayer)this.playerEntities.get(i);
            }
        }

        return null;
    }

    /**
     * Checks whether the session lock file was modified by another process
     */
    public void checkSessionLock() throws MinecraftException   // CraftBukkit - added throws
    {
        this.saveHandler.checkSessionLock();
    }

    /**
     * Retrieve the world seed from level.dat
     */
    public long getSeed()
    {
        return provider.getSeed();
    }

    public long getTotalWorldTime()
    {
        return this.worldInfo.getWorldTotalTime();
    }

    public long getWorldTime()
    {
        return provider.getWorldTime();
    }

    /**
     * Sets the world time.
     */
    public void setWorldTime(long par1)
    {
        provider.setWorldTime(par1);
    }

    /**
     * Returns the coordinates of the spawn point
     */
    public ChunkCoordinates getSpawnPoint()
    {
        return provider.getSpawnPoint();
    }

    /**
     * Called when checking if a certain block can be mined or not. The 'spawn safe zone' check is located here.
     */
    public boolean canMineBlock(EntityPlayer par1EntityPlayer, int par2, int par3, int par4)
    {
        return provider.canMineBlock(par1EntityPlayer, par2, par3, par4);
    }

    public boolean canMineBlockBody(EntityPlayer par1EntityPlayer, int par2, int par3, int par4)
    {
        return true;
    }

    /**
     * sends a Packet 38 (Entity Status) to all tracked players of that entity
     */
    public void setEntityState(Entity par1Entity, byte par2) {}

    /**
     * gets the IChunkProvider this world uses.
     */
    public IChunkProvider getChunkProvider()
    {
        return this.chunkProvider;
    }

    /**
     * Adds a block event with the given Args to the blockEventCache. During the next tick(), the block specified will
     * have its onBlockEvent handler called with the given parameters. Args: X,Y,Z, BlockID, EventID, EventParameter
     */
    public void addBlockEvent(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        if (par4 > 0)
        {
            Block.blocksList[par4].onBlockEventReceived(this, par1, par2, par3, par5, par6);
        }
    }

    /**
     * Returns this world's current save handler
     */
    public ISaveHandler getSaveHandler()
    {
        return this.saveHandler;
    }

    /**
     * Gets the World's WorldInfo instance
     */
    public WorldInfo getWorldInfo()
    {
        return this.worldInfo;
    }

    /**
     * Gets the GameRules instance.
     */
    public GameRules getGameRules()
    {
        return this.worldInfo.getGameRulesInstance();
    }

    /**
     * Updates the flag that indicates whether or not all players in the world are sleeping.
     */
    public void updateAllPlayersSleepingFlag() {}

    // CraftBukkit start
    // Calls the method that checks to see if players are sleeping
    // Called by CraftPlayer.setPermanentSleeping()
    public void checkSleepStatus()
    {
        if (!this.isRemote)
        {
            this.updateAllPlayersSleepingFlag();
        }
    }
    // CraftBukkit end

    public float getWeightedThunderStrength(float par1)
    {
        return (this.prevThunderingStrength + (this.thunderingStrength - this.prevThunderingStrength) * par1) * this.getRainStrength(par1);
    }

    /**
     * Not sure about this actually. Reverting this one myself.
     */
    public float getRainStrength(float par1)
    {
        return this.prevRainingStrength + (this.rainingStrength - this.prevRainingStrength) * par1;
    }

    /**
     * Returns true if the current thunder strength (weighted with the rain strength) is greater than 0.9
     */
    public boolean isThundering()
    {
        return (double)this.getWeightedThunderStrength(1.0F) > 0.9D;
    }

    /**
     * Returns true if the current rain strength is greater than 0.2
     */
    public boolean isRaining()
    {
        return (double)this.getRainStrength(1.0F) > 0.2D;
    }

    public boolean canLightningStrikeAt(int par1, int par2, int par3)
    {
        if (!this.isRaining())
        {
            return false;
        }
        else if (!this.canBlockSeeTheSky(par1, par2, par3))
        {
            return false;
        }
        else if (this.getPrecipitationHeight(par1, par3) > par2)
        {
            return false;
        }
        else
        {
            BiomeGenBase biomegenbase = this.getBiomeGenForCoords(par1, par3);
            return biomegenbase.getEnableSnow() ? false : biomegenbase.canSpawnLightningBolt();
        }
    }

    /**
     * Checks to see if the biome rainfall values for a given x,y,z coordinate set are extremely high
     */
    public boolean isBlockHighHumidity(int par1, int par2, int par3)
    {
        return provider.isBlockHighHumidity(par1, par2, par3);
    }

    /**
     * Assigns the given String id to the given MapDataBase using the MapStorage, removing any existing ones of the same
     * id.
     */
    public void setItemData(String par1Str, WorldSavedData par2WorldSavedData)
    {
        this.mapStorage.setData(par1Str, par2WorldSavedData);
    }

    /**
     * Loads an existing MapDataBase corresponding to the given String id from disk using the MapStorage, instantiating
     * the given Class, or returns null if none such file exists. args: Class to instantiate, String dataid
     */
    public WorldSavedData loadItemData(Class par1Class, String par2Str)
    {
        return this.mapStorage.loadData(par1Class, par2Str);
    }

    /**
     * Returns an unique new data id from the MapStorage for the given prefix and saves the idCounts map to the
     * 'idcounts' file.
     */
    public int getUniqueDataId(String par1Str)
    {
        return this.mapStorage.getUniqueDataId(par1Str);
    }

    public void func_82739_e(int par1, int par2, int par3, int par4, int par5)
    {
        for (int j1 = 0; j1 < this.worldAccesses.size(); ++j1)
        {
            ((IWorldAccess)this.worldAccesses.get(j1)).broadcastSound(par1, par2, par3, par4, par5);
        }
    }

    /**
     * See description for playAuxSFX.
     */
    public void playAuxSFX(int par1, int par2, int par3, int par4, int par5)
    {
        this.playAuxSFXAtEntity((EntityPlayer)null, par1, par2, par3, par4, par5);
    }

    /**
     * See description for playAuxSFX.
     */
    public void playAuxSFXAtEntity(EntityPlayer par1EntityPlayer, int par2, int par3, int par4, int par5, int par6)
    {
        try
        {
            for (int j1 = 0; j1 < this.worldAccesses.size(); ++j1)
            {
                ((IWorldAccess)this.worldAccesses.get(j1)).playAuxSFX(par1EntityPlayer, par2, par3, par4, par5, par6);
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Playing level event");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Level event being played");
            crashreportcategory.addCrashSection("Block coordinates", CrashReportCategory.getLocationInfo(par3, par4, par5));
            crashreportcategory.addCrashSection("Event source", par1EntityPlayer);
            crashreportcategory.addCrashSection("Event type", Integer.valueOf(par2));
            crashreportcategory.addCrashSection("Event data", Integer.valueOf(par6));
            throw new ReportedException(crashreport);
        }
    }

    /**
     * Returns current world height.
     */
    public int getHeight()
    {
        return provider.getHeight();
    }

    /**
     * Returns current world height.
     */
    public int getActualHeight()
    {
        return provider.getActualHeight();
    }

    public IUpdatePlayerListBox func_82735_a(EntityMinecart par1EntityMinecart)
    {
        return null;
    }

    /**
     * puts the World Random seed to a specific state dependant on the inputs
     */
    public Random setRandomSeed(int par1, int par2, int par3)
    {
        long l = (long)par1 * 341873128712L + (long)par2 * 132897987541L + this.getWorldInfo().getSeed() + (long)par3;
        this.rand.setSeed(l);
        return this.rand;
    }

    /**
     * Returns the location of the closest structure of the specified type. If not found returns null.
     */
    public ChunkPosition findClosestStructure(String par1Str, int par2, int par3, int par4)
    {
        return this.getChunkProvider().findClosestStructure(this, par1Str, par2, par3, par4);
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    public CrashReportCategory addWorldInfoToCrashReport(CrashReport par1CrashReport)
    {
        CrashReportCategory crashreportcategory = par1CrashReport.makeCategoryDepth("Affected level", 1);
        crashreportcategory.addCrashSection("Level name", this.worldInfo == null ? "????" : this.worldInfo.getWorldName());
        crashreportcategory.addCrashSectionCallable("All players", new CallableLvl2(this));
        crashreportcategory.addCrashSectionCallable("Chunk stats", new CallableLvl3(this));

        try
        {
            this.worldInfo.addToCrashReport(crashreportcategory);
        }
        catch (Throwable throwable)
        {
            crashreportcategory.addCrashSectionThrowable("Level Data Unobtainable", throwable);
        }

        return crashreportcategory;
    }

    /**
     * Starts (or continues) destroying a block with given ID at the given coordinates for the given partially destroyed
     * value
     */
    public void destroyBlockInWorldPartially(int par1, int par2, int par3, int par4, int par5)
    {
        for (int j1 = 0; j1 < this.worldAccesses.size(); ++j1)
        {
            IWorldAccess iworldaccess = (IWorldAccess)this.worldAccesses.get(j1);
            iworldaccess.destroyBlockPartially(par1, par2, par3, par4, par5);
        }
    }

    /**
     * Return the Vec3Pool object for this world.
     */
    public Vec3Pool getWorldVec3Pool()
    {
        return this.vecPool;
    }

    /**
     * returns a calendar object containing the current date
     */
    public Calendar getCurrentDate()
    {
        if (this.getTotalWorldTime() % 600L == 0L)
        {
            this.theCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        return this.theCalendar;
    }

    public Scoreboard getScoreboard()
    {
        return this.worldScoreboard;
    }

    public void func_96440_m(int par1, int par2, int par3, int par4)
    {
        for (int i1 = 0; i1 < 4; ++i1)
        {
            int j1 = par1 + Direction.offsetX[i1];
            int k1 = par3 + Direction.offsetZ[i1];
            int l1 = this.getBlockId(j1, par2, k1);

            if (l1 != 0)
            {
                Block block = Block.blocksList[l1];

                if (Block.redstoneComparatorIdle.func_94487_f(l1))
                {
                    block.onNeighborBlockChange(this, j1, par2, k1, par4);
                }
                else if (Block.isNormalCube(l1))
                {
                    j1 += Direction.offsetX[i1];
                    k1 += Direction.offsetZ[i1];
                    l1 = this.getBlockId(j1, par2, k1);
                    block = Block.blocksList[l1];

                    if (Block.redstoneComparatorIdle.func_94487_f(l1))
                    {
                        block.onNeighborBlockChange(this, j1, par2, k1, par4);
                    }
                }
            }
        }
    }

    public ILogAgent getWorldLogAgent()
    {
        return this.worldLogAgent;
    }

    /**
     * Adds a single TileEntity to the world.
     * @param entity The TileEntity to be added.
     */
    public void addTileEntity(TileEntity entity)
    {
        Collection dest = scanningTileEntities ? addedTileEntityList : loadedTileEntityList; // MCPC+ - List -> Collection for CB loadedTileEntityList type change
        if(entity.canUpdate())
        {
            dest.add(entity);
        }
    }

    /**
     * Determine if the given block is considered solid on the
     * specified side.  Used by placement logic.
     *
     * @param x Block X Position
     * @param y Block Y Position
     * @param z Block Z Position
     * @param side The Side in question
     * @return True if the side is solid
     */
    public boolean isBlockSolidOnSide(int x, int y, int z, ForgeDirection side)
    {
        return isBlockSolidOnSide(x, y, z, side, false);
    }

    /**
     * Determine if the given block is considered solid on the
     * specified side.  Used by placement logic.
     *
     * @param x Block X Position
     * @param y Block Y Position
     * @param z Block Z Position
     * @param side The Side in question
     * @param _default The defult to return if the block doesn't exist.
     * @return True if the side is solid
     */
    public boolean isBlockSolidOnSide(int x, int y, int z, ForgeDirection side, boolean _default)
    {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000)
        {
            return _default;
        }

        Chunk chunk = this.chunkProvider.provideChunk(x >> 4, z >> 4);
        if (chunk == null || chunk.isEmpty())
        {
            return _default;
        }

        Block block = Block.blocksList[getBlockId(x, y, z)];
        if(block == null)
        {
            return false;
        }

        return block.isBlockSolidOnSide(this, x, y, z, side);
    }

    /**
     * Get the persistent chunks for this world
     *
     * @return
     */
    public ImmutableSetMultimap<ChunkCoordIntPair, Ticket> getPersistentChunks()
    {
        return ForgeChunkManager.getPersistentChunksFor(this);
    }

    /**
     * Readded as it was removed, very useful helper function
     *
     * @param x X position
     * @param y Y Position
     * @param z Z Position
     * @return The blocks light opacity
     */
    public int getBlockLightOpacity(int x, int y, int z)
    {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000)
        {
            return 0;
        }

        if (y < 0 || y >= 256)
        {
            return 0;
        }

        return getChunkFromChunkCoords(x >> 4, z >> 4).getBlockLightOpacity(x & 15, y, z & 15);
    }

    /**
     * Returns a count of entities that classify themselves as the specified creature type.
     */
    public int countEntities(EnumCreatureType type, boolean forSpawnCount)
    {
        int count = 0;
        for (int x = 0; x < loadedEntityList.size(); x++)
        {
            if (((Entity)loadedEntityList.get(x)).isCreatureType(type, forSpawnCount))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getBrightness(int arg0, int arg1, int arg2, int arg3) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int arg0, int arg1, int arg2,
            int arg3) {
        return 0;
    }
}
