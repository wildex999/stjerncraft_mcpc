package net.minecraft.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.ArgsWrapper;
import cpw.mods.fml.relauncher.FMLRelauncher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.BlockDispenser;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.dispenser.BehaviorArrowDispense;
import net.minecraft.dispenser.BehaviorBucketEmptyDispense;
import net.minecraft.dispenser.BehaviorBucketFullDispense;
import net.minecraft.dispenser.BehaviorDispenseBoat;
import net.minecraft.dispenser.BehaviorDispenseFireball;
import net.minecraft.dispenser.BehaviorDispenseFirework;
import net.minecraft.dispenser.BehaviorDispenseMinecart;
import net.minecraft.dispenser.BehaviorEggDispense;
import net.minecraft.dispenser.BehaviorExpBottleDispense;
import net.minecraft.dispenser.BehaviorMobEggDispense;
import net.minecraft.dispenser.BehaviorPotionDispense;
import net.minecraft.dispenser.BehaviorSnowballDispense;
import net.minecraft.item.Item;
import net.minecraft.network.NetworkListenThread;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet4UpdateTime;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.StringTranslate;
import net.minecraft.util.StringUtils;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
// CraftBukkit start
import java.util.concurrent.ExecutionException;
import java.io.IOException;
import net.minecraft.command.ServerCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.world.World;
import com.google.common.io.Files;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.WorldSaveEvent;
// CraftBukkit end
// MCPC+ start
import net.minecraftforge.common.Configuration;
import java.io.FilenameFilter;
// MCPC+ end

public abstract class MinecraftServer implements ICommandSender, Runnable, IPlayerUsage
{
    /** The logging system. */
    public static Logger logger = Logger.getLogger("Minecraft");

    /** Instance of Minecraft Server. */
    private static MinecraftServer mcServer = null;
    public ISaveFormat anvilConverterForAnvilFile; // CraftBukkit - private final -> public

    /** The PlayerUsageSnooper instance. */
    private final PlayerUsageSnooper usageSnooper = new PlayerUsageSnooper("server", this);
    public File anvilFile; // CraftBukkit - private final -> public

    /**
     * Collection of objects to update every tick. Type: List<IUpdatePlayerListBox>
     */
    private final List tickables = new ArrayList();
    private final ICommandManager commandManager;
    public final Profiler theProfiler = new Profiler();

    /** The server's hostname. */
    private String hostname;

    /** The server's port. */
    private int serverPort = -1;
    public WorldServer[] worldServers; // MCPC+ - vanilla compatibility

    /** The ServerConfigurationManager instance. */
    private ServerConfigurationManager serverConfigManager;

    /**
     * Indicates whether the server is running or not. Set to false to initiate a shutdown.
     */
    private boolean serverRunning = true;

    /** Indicates to other classes that the server is safely stopped. */
    private boolean serverStopped = false;

    /** Incremented every tick. */
    private int tickCounter = 0;

    /**
     * The task the server is currently working on(and will output on outputPercentRemaining).
     */
    public String currentTask;

    /** The percentage of the current task finished so far. */
    public int percentDone;

    /** True if the server is in online mode. */
    private boolean onlineMode;

    /** True if the server has animals turned on. */
    private boolean canSpawnAnimals;
    private boolean canSpawnNPCs;

    /** Indicates whether PvP is active on the server or not. */
    private boolean pvpEnabled;

    /** Determines if flight is allowed or not. */
    private boolean allowFlight;

    /** The server MOTD string. */
    private String motd;

    /** Maximum build height. */
    private int buildLimit;
    private long lastSentPacketID;
    private long lastSentPacketSize;
    private long lastReceivedID;
    private long lastReceivedSize;
    public final long[] sentPacketCountArray = new long[100];
    public final long[] sentPacketSizeArray = new long[100];
    public final long[] receivedPacketCountArray = new long[100];
    public final long[] receivedPacketSizeArray = new long[100];
    public final long[] tickTimeArray = new long[100];

    /** Stats are [dimension][tick%100] system.nanoTime is stored. */
    //public long[][] timeOfLastDimensionTick;
    public Hashtable<Integer, long[]> worldTickTimes = new Hashtable<Integer, long[]>();
    private KeyPair serverKeyPair;

    /** Username of the server owner (for integrated servers) */
    private String serverOwner;
    private String folderName;
    @SideOnly(Side.CLIENT)
    private String worldName;
    private boolean isDemo;
    private boolean enableBonusChest;

    /**
     * If true, there is no need to save chunks or stop the server, because that is already being done.
     */
    private boolean worldIsBeingDeleted;
    private String texturePack = "";
    private boolean serverIsRunning = false;

    /**
     * Set when warned for "Can't keep up", which triggers again after 15 seconds.
     */
    private long timeOfLastWarning;
    private String userMessage;
    private boolean startProfiling;

    // CraftBukkit start
    public List<WorldServer> worlds = new ArrayList<WorldServer>();
    public org.bukkit.craftbukkit.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    public ConsoleReader reader;
    public static int currentTick;
    public final Thread primaryThread;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
    public int autosavePeriod;
    // CraftBukkit end
    // Spigot start
    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    public static double currentTPS = 0;
    // Spigot end

    public MinecraftServer(File par1File)
    {
        mcServer = this;
        this.anvilFile = par1File;
        this.commandManager = new ServerCommandManager();
        this.anvilConverterForAnvilFile = new AnvilSaveConverter(par1File);
        this.registerDispenseBehaviors();
        primaryThread = null;
    }

    public MinecraftServer(OptionSet options)   // CraftBukkit - signature file -> OptionSet
    {
        mcServer = this;
        // this.universe = file1; // CraftBukkit
        this.commandManager = new ServerCommandManager();
        // this.convertable = new WorldLoaderServer(server.getWorldContainer()); // CraftBukkit - moved to DedicatedServer.init
        this.registerDispenseBehaviors();
        // CraftBukkit start
        this.options = options;

        try
        {
            this.reader = new ConsoleReader(System.in, System.out);
            this.reader.setExpandEvents(false); // Avoid parsing exceptions for uncommonly used event designators
        }
        catch (Exception e)
        {
            try
            {
                // Try again with jline disabled for Windows users without C++ 2008 Redistributable
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                org.bukkit.craftbukkit.Main.useJline = false;
                this.reader = new ConsoleReader(System.in, System.out);
                this.reader.setExpandEvents(false);
            }
            catch (java.io.IOException ex)
            {
                Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (org.bukkit.craftbukkit.Main.useJline)
        {
            net.minecraft.server.FMLLogJLineBreakProxy.reader = this.reader;
        }

        Runtime.getRuntime().addShutdownHook(new org.bukkit.craftbukkit.util.ServerShutdownThread(this));
        primaryThread = new ThreadMinecraftServer(this, "Server thread"); // Moved from main
    }

    public abstract PropertyManager getPropertyManager();
    // CraftBukkit end

    /**
     * Register all dispense behaviors.
     */
    private void registerDispenseBehaviors()
    {
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.arrow, new BehaviorArrowDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.egg, new BehaviorEggDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.snowball, new BehaviorSnowballDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.expBottle, new BehaviorExpBottleDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.potion, new BehaviorPotionDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.monsterPlacer, new BehaviorMobEggDispense(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.firework, new BehaviorDispenseFirework(this));
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.fireballCharge, new BehaviorDispenseFireball(this));
        BehaviorDispenseMinecart var1 = new BehaviorDispenseMinecart(this);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartEmpty, var1);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartCrate, var1);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartPowered, var1);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.boat, new BehaviorDispenseBoat(this));
        BehaviorBucketFullDispense var2 = new BehaviorBucketFullDispense(this);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketLava, var2);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketWater, var2);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketEmpty, new BehaviorBucketEmptyDispense(this));
    }

    /**
     * Initialises the server and starts it.
     */
    protected abstract boolean startServer() throws java.net.UnknownHostException; // CraftBukkit - throws UnknownHostException

    protected void convertMapIfNeeded(String par1Str)
    {
        if (this.getActiveAnvilConverter().isOldMapFormat(par1Str))
        {
            logger.info("Converting map!");
            this.setUserMessage("menu.convertingLevel");
            this.getActiveAnvilConverter().convertMapFormat(par1Str, new ConvertingProgressUpdate(this));
        }
    }

    /**
     * Typically "menu.convertingLevel", "menu.loadingLevel" or others.
     */
    protected synchronized void setUserMessage(String par1Str)
    {
        this.userMessage = par1Str;
    }

    @SideOnly(Side.CLIENT)

    public synchronized String getUserMessage()
    {
        return this.userMessage;
    }

    protected void loadAllWorlds(String par1Str, String par2Str, long par3, WorldType par5WorldType, String par6Str)
    {
        // MCPC+ start - register vanilla server commands
        ServerCommandManager vanillaCommandManager = (ServerCommandManager)this.getCommandManager();
        vanillaCommandManager.registerVanillaCommands();
        // MCPC+ end
        this.convertMapIfNeeded(par1Str);
        this.setUserMessage("menu.loadingLevel");
        // CraftBukkit - removed world and ticktime arrays
        ISaveHandler var7 = this.anvilConverterForAnvilFile.getSaveLoader(par1Str, true);
        WorldInfo var9 = var7.loadWorldInfo();
        // CraftBukkit start - removed worldsettings
        
        WorldSettings worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
        worldsettings.func_82750_a(par6Str);
        WorldServer world;
        WorldServer overWorld = initOverWorld(par1Str, par2Str, worldsettings);

        for (int dimension : DimensionManager.getStaticDimensionIDs())
        {
            String worldType = "";
            String name = "";
            // MCPC+ start
            Environment env = Environment.getEnvironment(dimension);
            if (dimension >= -1 && dimension <= 1)
            {
                if (dimension == 0 || (dimension == -1 && !this.getAllowNether()) || (dimension == 1 && !this.server.getAllowEnd()))
                    continue;
                worldType = env.toString().toLowerCase();
                name = par1Str + "_" + worldType;
            }
            else
            {
                WorldProvider provider = WorldProvider.getProviderForDimension(dimension);
                worldType = provider.getClass().getSimpleName();
                env = DimensionManager.registerBukkitEnvironment(provider.dimensionId, provider.getClass().getSimpleName());
                if (worldType.contains("WorldProvider"))
                    worldType = worldType.replace("WorldProvider", "");
                name = "world_" + worldType.toLowerCase();
            }
            org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(name);
            worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
            worldsettings.func_82750_a(par6Str);
            String dim = "DIM" + dimension;
            File newWorld = new File(new File(name), dim);
            File oldWorld = new File(new File(par1Str), dim);

            if ((!newWorld.isDirectory()) && (oldWorld.isDirectory()))
            {
                logger.info("---- Migration of old " + worldType + " folder required ----");
                logger.info("Unfortunately due to the way that Minecraft implemented multiworld support in 1.6, Bukkit requires that you move your " + worldType + " folder to a new location in order to operate correctly.");
                logger.info("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using Bukkit in the future.");
                logger.info("Attempting to move " + oldWorld + " to " + newWorld + "...");

                if (newWorld.exists())
                {
                    logger.severe("A file or folder already exists at " + newWorld + "!");
                    logger.info("---- Migration of old " + worldType + " folder failed ----");
                }
                else if (newWorld.getParentFile().mkdirs())
                {
                    if (oldWorld.renameTo(newWorld))
                    {
                        logger.info("Success! To restore " + worldType + " in the future, simply move " + newWorld + " to " + oldWorld);

                        // Migrate world data too.
                        try
                        {
                            Files.copy(new File(new File(par1Str), "level.dat"), new File(new File(name), "level.dat"));
                        }
                        catch (IOException exception)
                        {
                            logger.severe("Unable to migrate world data.");
                        }

                        logger.info("---- Migration of old " + worldType + " folder complete ----");
                    }
                    else
                    {
                        logger.severe("Could not move folder " + oldWorld + " to " + newWorld + "!");
                        logger.info("---- Migration of old " + worldType + " folder failed ----");
                    }
                }
                else
                {
                    logger.severe("Could not create path for " + newWorld + "!");
                    logger.info("---- Migration of old " + worldType + " folder failed ----");
                }
            }

            this.setUserMessage(name);
            // CraftBukkit
            world = new WorldServerMulti(this, new AnvilSaveHandler(server.getWorldContainer(), name, true), name, dimension, worldsettings, overWorld, this.theProfiler, env, gen);
            // MCPC+ end
            if (gen != null)
            {
                world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
            }

            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));
            world.addWorldAccess(new WorldManager(this, world));

            if (!this.isSinglePlayer())
            {
                world.getWorldInfo().setGameType(this.getGameType());
            }

            this.worlds.add(world);

            this.serverConfigManager.setPlayerManager(this.worlds.toArray(new WorldServer[this.worlds.size()]));
            // CraftBukkit end
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load((World)world)); // Forge
        }
        boolean mystLoaded = this.initMystWorld(worldsettings);
        if (mystLoaded)
            System.out.println("Successfully initialized Mystcraft support.");
        this.setDifficultyForAllWorlds(this.getDifficulty());
        this.initialWorldChunkLoad();
    }

    // MCPC+ start - move overWorld initialization to it's own method for easier use above.
    protected WorldServer initOverWorld(String par1Str, String par2Str, WorldSettings worldsettings)
    {
        org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(par1Str);
        WorldServer overWorld = (isDemo() ? new DemoWorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, theProfiler) : new WorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, worldsettings, theProfiler,  Environment.getEnvironment(0), gen));
        if (gen != null)
        {
            overWorld.getWorld().getPopulators().addAll(gen.getDefaultPopulators(overWorld.getWorld()));
        }

        this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(overWorld.getWorld()));
        overWorld.addWorldAccess(new WorldManager(this, overWorld));

        if (!this.isSinglePlayer())
        {
            overWorld.getWorldInfo().setGameType(this.getGameType());
        }
        
        this.worlds.add(overWorld);
        this.serverConfigManager.setPlayerManager(this.worlds.toArray(new WorldServer[this.worlds.size()]));
        // CraftBukkit end
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load((World)overWorld)); // Forge
        return overWorld;
    }
    // MCPC+ end

    // MCPC+ start - used to create an isolated myst dimension
    protected boolean initMystWorld(WorldSettings worldsettings)
    {
        //  search for myst dimensions and load them
        File mystconfig = new File("./config/mystcraft_config.txt");
        boolean initMyst = false;
        boolean mystLoaded = false;
        int mystProviderType = -999;

        if (mystconfig.exists())
        {
            Configuration config = new Configuration(mystconfig);
            config.load();
            mystProviderType = config.get(Configuration.CATEGORY_GENERAL, "options.providerId", -999).getInt();
            System.out.println("MinecraftServer mystProvider = " + mystProviderType);
            initMyst = true;
        }
        if (initMyst)
        {
            File file = new File("world_myst");
            String[] directories = file.list(new FilenameFilter() {
              @Override
              public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
              }
            });
            if (directories != null)
            {
                for (int i = 0; i < directories.length; i++)
                {
                    String dim = "0";
                    if (directories[i].contains("age"))
                        dim = directories[i].replace("age", "");
                    if (Integer.parseInt(dim) != 0)
                    {
                        DimensionManager.registerDimension(Integer.parseInt(dim), mystProviderType);
                        WorldServer mystWorld = DimensionManager.initMystWorld("world_myst", worldsettings, Integer.parseInt(dim));
                        mystLoaded = true;
                    }
                }
            }
        }
        return mystLoaded;
    }
    // MCPC+ end

    protected void initialWorldChunkLoad()
    {
        short var5 = 196;
        long var6 = System.currentTimeMillis();
        this.setUserMessage("menu.generatingTerrain");
        byte var7 = 0;

        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            WorldServer worldserver = this.worlds.get(j);
            logger.info("Preparing start region for level " + j + " (Dimension: " + worldserver.provider.dimensionId + ", Seed: " + worldserver.getSeed() + ")");

            if (!worldserver.getWorld().getKeepSpawnInMemory())
            {
                continue;
            }

            // CraftBukkit end
            ChunkCoordinates var8 = worldserver.getSpawnPoint();

            for (int var9 = -var5; var9 <= var5 && this.isServerRunning(); var9 += 16)
            {
                for (int var11 = -var5; var11 <= var5 && this.isServerRunning(); var11 += 16)
                {
                    long var12 = System.currentTimeMillis();

                    if (var12 < var6)
                    {
                        var6 = var12;
                    }

                    if (var12 > var6 + 1000L)
                    {
                        int var13 = (var5 * 2 + 1) * (var5 * 2 + 1);
                        int k1 = (var9 + var5) * (var5 * 2 + 1) + var11 + 1;
                        this.outputPercentRemaining("Preparing spawn area", k1 * 100 / var13);
                        var6 = var12;
                    }

                    worldserver.theChunkProviderServer.loadChunk(var8.posX + var9 >> 4, var8.posZ + var11 >> 4);
                }
            }
        }

        this.clearCurrentTask();
    }

    public abstract boolean canStructuresSpawn();

    public abstract EnumGameType getGameType();

    /**
     * Defaults to "1" (Easy) for the dedicated server, defaults to "2" (Normal) on the client.
     */
    public abstract int getDifficulty();

    /**
     * Defaults to false.
     */
    public abstract boolean isHardcore();

    /**
     * Used to display a percent remaining given text and the percentage.
     */
    protected void outputPercentRemaining(String par1Str, int par2)
    {
        this.currentTask = par1Str;
        this.percentDone = par2;
        logger.info(par1Str + ": " + par2 + "%");
    }

    /**
     * Set current task to null and set its percentage to 0.
     */
    protected void clearCurrentTask()
    {
        this.currentTask = null;
        this.percentDone = 0;
        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD); // CraftBukkit
    }

    protected void saveAllWorlds(boolean par1) throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            // CraftBukkit start
            for (int j = 0; j < this.worlds.size(); ++j)
            {
                WorldServer worldserver = this.worlds.get(j);

                if (worldserver != null)
                {
                    if (!par1)
                    {
                        logger.info("Saving chunks for level \'" + worldserver.getWorldInfo().getWorldName() + "\'/" + worldserver.provider.getDimensionName());
                    }

                    worldserver.saveAllChunks(true, (IProgressUpdate) null);
                    worldserver.flush();
                    WorldSaveEvent event = new WorldSaveEvent(worldserver.getWorld());
                    this.server.getPluginManager().callEvent(event);
                }
            }

            // CraftBukkit end
        }
    }

    public void stopServer() //throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            logger.info("Stopping server");

            // CraftBukkit start
            if (this.server != null)
            {
                this.server.disablePlugins();
            }

            // CraftBukkit end

            if (this.getNetworkThread() != null)
            {
                this.getNetworkThread().stopListening();
            }

            if (this.serverConfigManager != null)
            {
                logger.info("Saving players");
                this.serverConfigManager.saveAllPlayerData();
                this.serverConfigManager.removeAllPlayers();
            }

            logger.info("Saving worlds");
            try {
                this.saveAllWorlds(false);
            } catch (MinecraftException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            /* CraftBukkit start - handled in saveChunks
            for (int i = 0; i < this.worldServer.length; ++i) {
                WorldServer worldserver = this.worldServer[i];

                worldserver.saveLevel();
            }
            // CraftBukkit end */
            for (int var1 = 0; var1 < this.worlds.size(); ++var1)
            {
                WorldServer var2 = this.worlds.get(var1);
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(var2)); // Forge
            }

            List<WorldServer> tmp = this.worlds;

            for (WorldServer world : tmp)
            {
                DimensionManager.setWorld(world.dimension, (WorldServer)null);
            }

            // Forge end
            if (this.usageSnooper != null && this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.stopSnooper();
            }
        }
    }

    /**
     * "getHostname" is already taken, but both return the hostname.
     */
    public String getServerHostname()
    {
        return this.hostname;
    }

    public void setHostname(String par1Str)
    {
        this.hostname = par1Str;
    }

    public boolean isServerRunning()
    {
        return this.serverRunning;
    }

    /**
     * Sets the serverRunning variable to false, in order to get the server to shut down.
     */
    public void initiateShutdown()
    {
        this.serverRunning = false;
    }

    public void run()
    {
        try
        {
            if (this.startServer())
            {
                FMLCommonHandler.instance().handleServerStarted();
                FMLCommonHandler.instance().onWorldLoadTick(this.worlds.toArray(new WorldServer[this.worlds.size()]));

                // Spigot start
                for (long lastTick = 0L; this.serverRunning; this.serverIsRunning = true)
                {
                    long curTime = System.nanoTime();
                    long wait = TICK_TIME - (curTime - lastTick);

                    if (wait > 0)
                    {
                        Thread.sleep(wait / 1000000);
                        continue;
                    }

                    currentTPS = (currentTPS * 0.95) + (1E9 / (curTime - lastTick) * 0.05);
                    lastTick = curTime;
                    MinecraftServer.currentTick++;
                    this.tick();
                }

                // Spigot end
                FMLCommonHandler.instance().handleServerStopping();
            }
            else
            {
                this.finalTick((CrashReport)null);
            }
        }
        catch (Throwable var1)
        {
            if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
            {
                return;    // Forge
            }

            var1.printStackTrace();
            logger.log(Level.SEVERE, "Encountered an unexpected exception " + var1.getClass().getSimpleName(), var1);
            CrashReport var50 = null;

            if (var1 instanceof ReportedException)
            {
                var50 = this.addServerInfoToCrashReport(((ReportedException) var1).getCrashReport());
            }
            else
            {
                var50 = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", var1));
            }

            File var5 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (var50.saveToFile(var5))
            {
                logger.severe("This crash report has been saved to: " + var5.getAbsolutePath());
            }
            else
            {
                logger.severe("We were unable to save this crash report to disk.");
            }

            this.finalTick(var50);
        }
        finally
        {
            org.bukkit.craftbukkit.util.WatchdogThread.stopping(); // Spigot

            try
            {
                if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
                {
                    return;    // Forge
                }

                this.stopServer();
                this.serverStopped = true;
            }
            catch (Throwable var7)
            {
                var7.printStackTrace();
            }
            finally
            {
                // CraftBukkit start - restore terminal to original settings
                try
                {
                    this.reader.getTerminal().restore();
                }
                catch (Exception e)
                {
                }

                // CraftBukkit end
                this.systemExitNow();
            }
        }
    }

    protected File getDataDirectory()
    {
        return new File(".");
    }

    /**
     * Called on exit from the main run() loop.
     */
    protected void finalTick(CrashReport par1CrashReport) {}

    /**
     * Directly calls System.exit(0), instantly killing the program.
     */
    protected void systemExitNow() {}

    /**
     * Main function called by run() every loop.
     */
    protected void tick() throws MinecraftException   // CraftBukkit - added throws
    {
        FMLCommonHandler.instance().rescheduleTicks(Side.SERVER); // Forge
        long var1 = System.nanoTime();
        AxisAlignedBB.getAABBPool().cleanPool();
        FMLCommonHandler.instance().onPreServerTick(); // Forge
        ++this.tickCounter;

        if (this.startProfiling)
        {
            this.startProfiling = false;
            this.theProfiler.profilingEnabled = true;
            this.theProfiler.clearProfiling();
        }

        this.theProfiler.startSection("root");
        this.updateTimeLightAndEntities();

        if ((this.autosavePeriod > 0) && ((this.tickCounter % this.autosavePeriod) == 0))   // CraftBukkit
        {
            this.theProfiler.startSection("save");
            this.serverConfigManager.saveAllPlayerData();
            this.saveAllWorlds(true);
            this.theProfiler.endSection();
        }

        this.theProfiler.startSection("tallying");
        this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - var1;
        this.sentPacketCountArray[this.tickCounter % 100] = Packet.sentID - this.lastSentPacketID;
        this.lastSentPacketID = Packet.sentID;
        this.sentPacketSizeArray[this.tickCounter % 100] = Packet.sentSize - this.lastSentPacketSize;
        this.lastSentPacketSize = Packet.sentSize;
        this.receivedPacketCountArray[this.tickCounter % 100] = Packet.receivedID - this.lastReceivedID;
        this.lastReceivedID = Packet.receivedID;
        this.receivedPacketSizeArray[this.tickCounter % 100] = Packet.receivedSize - this.lastReceivedSize;
        this.lastReceivedSize = Packet.receivedSize;
        this.theProfiler.endSection();
        this.theProfiler.startSection("snooper");

        if (!this.usageSnooper.isSnooperRunning() && this.tickCounter > 100)
        {
            this.usageSnooper.startSnooper();
        }

        if (this.tickCounter % 6000 == 0)
        {
            this.usageSnooper.addMemoryStatsToSnooper();
        }

        this.theProfiler.endSection();
        this.theProfiler.endSection();
        FMLCommonHandler.instance().onPostServerTick();
    }

    public void updateTimeLightAndEntities()
    {
        this.theProfiler.startSection("levels");
        // CraftBukkit start - only send timeupdates to the people in that world
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);

        // Run tasks that are waiting on processing
        while (!processQueue.isEmpty())
        {
            processQueue.remove().run();
        }

        org.bukkit.craftbukkit.chunkio.ChunkIOExecutor.tick();

        // Send timeupdates to everyone, it will get the right time from the world the player is in.
        if (this.tickCounter % 20 == 0)
        {
            for (int i = 0; i < this.getConfigurationManager().playerEntityList.size(); ++i)
            {
                EntityPlayerMP entityplayer = (EntityPlayerMP) this.getConfigurationManager().playerEntityList.get(i);
                entityplayer.playerNetServerHandler.sendPacketToPlayer(new Packet4UpdateTime(entityplayer.worldObj.getTotalWorldTime(), entityplayer.getPlayerTime())); // Add support for per player time
            }
        }

        int i;

        Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);
        for (int x = 0; x < ids.length; x++)
        {
            int id = ids[x];
            long j = System.nanoTime();
            // if (i == 0 || this.getAllowNether()) {
            WorldServer worldserver = DimensionManager.getWorld(id);
            this.theProfiler.startSection(worldserver.getWorldInfo().getWorldName());
            this.theProfiler.startSection("pools");
            worldserver.getWorldVec3Pool().clear();
            this.theProfiler.endSection();
            /* Drop global time updates
            if (this.ticks % 20 == 0) {
                this.methodProfiler.a("timeSync");
                this.t.a(new Packet4UpdateTime(worldserver.getTime(), worldserver.getDayTime()), worldserver.worldProvider.dimension);
                this.methodProfiler.b();
            }
            // CraftBukkit end */
            this.theProfiler.startSection("tick");
            FMLCommonHandler.instance().onPreWorldTick(worldserver);
            CrashReport crashreport;

            try
            {
                worldserver.tick();
            }
            catch (Throwable throwable)
            {
                crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world");
                worldserver.addWorldInfoToCrashReport(crashreport);
                throw new ReportedException(crashreport);
            }

            try
            {
                worldserver.updateEntities();
            }
            catch (Throwable throwable1)
            {
                crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world entities");
                worldserver.addWorldInfoToCrashReport(crashreport);
                throw new ReportedException(crashreport);
            }

            FMLCommonHandler.instance().onPostWorldTick(worldserver);
            this.theProfiler.endSection();
            this.theProfiler.startSection("tracker");
            worldserver.getEntityTracker().updateTrackedEntities();
            this.theProfiler.endSection();
            this.theProfiler.endSection();
            // } // CraftBukkit
            // this.k[i][this.ticks % 100] = System.nanoTime() - j; // CraftBukkit
            // Forge start
            //((long[]) this.worldTickTimes.get(id))[this.tickCounter % 100] = System.nanoTime() - j;
        }

        this.theProfiler.endStartSection("dim_unloading");
        //DimensionManager.unloadWorlds(this.worldTickTimes);
        // Forge end
        this.theProfiler.endStartSection("connection");
        this.getNetworkThread().networkTick();
        this.theProfiler.endStartSection("players");
        this.serverConfigManager.sendPlayerInfoToAllPlayers();
        this.theProfiler.endStartSection("tickables");

        for (i = 0; i < this.tickables.size(); ++i)
        {
            ((IUpdatePlayerListBox) this.tickables.get(i)).update();
        }

        this.theProfiler.endSection();
        org.bukkit.craftbukkit.util.WatchdogThread.tick(); // Spigot
    }

    public boolean getAllowNether()
    {
        return true;
    }

    @SideOnly(Side.CLIENT)  // MCPC
    public void startServerThread()
    {
        (new ThreadMinecraftServer(this, "Server thread")).start();
        // (new ThreadServerApplication(this, "Server thread")).start(); // CraftBukkit - prevent abuse
    }

    /**
     * Returns a File object from the specified string.
     */
    public File getFile(String par1Str)
    {
        return new File(this.getDataDirectory(), par1Str);
    }

    /**
     * Logs the message with a level of INFO.
     */
    public void logInfo(String par1Str)
    {
        logger.info(par1Str);
    }

    /**
     * Logs the message with a level of WARN.
     */
    public void logWarning(String par1Str)
    {
        logger.warning(par1Str);
    }

    /**
     * Gets the worldServer by the given dimension.
     */
    public WorldServer worldServerForDimension(int par1)
    {
        // MCPC+ start - this is required for MystCraft agebooks to teleport correctly
        // verify the nether or the end is allowed, and if not return overworld
        if ((par1 == -1 && !this.getAllowNether()) || (par1 == 1 && !this.server.getAllowEnd()))
        {
            return DimensionManager.getWorld(0);
        }

        WorldServer ret = DimensionManager.getWorld(par1);
        if (ret == null)
        {
            DimensionManager.initDimension(par1);
            ret = DimensionManager.getWorld(par1);
        }
        return ret;
        // MCPC+ end
    }

    @SideOnly(Side.SERVER)
    public void func_82010_a(IUpdatePlayerListBox par1IUpdatePlayerListBox)
    {
        this.tickables.add(par1IUpdatePlayerListBox);
    }

    /**
     * Returns the server's hostname.
     */
    public String getHostname()
    {
        return this.hostname;
    }

    /**
     * Never used, but "getServerPort" is already taken.
     */
    public int getPort()
    {
        return this.serverPort;
    }

    /**
     * Returns the server message of the day
     */
    public String getServerMOTD()
    {
        return this.motd;
    }

    /**
     * Returns the server's Minecraft version as string.
     */
    public String getMinecraftVersion()
    {
        return "1.4.7";
    }

    /**
     * Returns the number of players currently on the server.
     */
    public int getCurrentPlayerCount()
    {
        return this.serverConfigManager.getCurrentPlayerCount();
    }

    /**
     * Returns the maximum number of players allowed on the server.
     */
    public int getMaxPlayers()
    {
        return this.serverConfigManager.getMaxPlayers();
    }

    /**
     * Returns an array of the usernames of all the connected players.
     */
    public String[] getAllUsernames()
    {
        return this.serverConfigManager.getAllUsernames();
    }

    /**
     * Used by RCon's Query in the form of "MajorServerMod 1.2.3: MyPlugin 1.3; AnotherPlugin 2.1; AndSoForth 1.0".
     */
    public String getPlugins()
    {
        // CraftBukkit start - whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();
        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && this.server.getQueryPlugins())
        {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++)
            {
                if (i > 0)
                {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    // CraftBukkit start
    public String executeCommand(final String par1Str)   // CraftBukkit - final parameter
    {
        Waitable<String> waitable = new Waitable<String>()
        {
            @Override
            protected String evaluate()
            {
                RConConsoleSource.consoleBuffer.resetLog();
                // Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(MinecraftServer.this.remoteConsole, par1Str);
                MinecraftServer.this.server.getPluginManager().callEvent(event);
                // Event changes end
                ServerCommand servercommand = new ServerCommand(event.getCommand(), RConConsoleSource.consoleBuffer);
                // this.q.a(RemoteControlCommandListener.instance, s);
                MinecraftServer.this.server.dispatchServerCommand(MinecraftServer.this.remoteConsole, servercommand); // CraftBukkit
                return RConConsoleSource.consoleBuffer.getChatBuffer();
            }
        };
        processQueue.add(waitable);

        try
        {
            return waitable.get();
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException("Exception processing rcon command " + par1Str, e.getCause());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt(); // Maintain interrupted state
            throw new RuntimeException("Interrupted processing rcon command " + par1Str, e);
        }

        // CraftBukkit end
    }

    /**
     * Returns true if debugging is enabled, false otherwise.
     */
    public boolean isDebuggingEnabled()
    {
        return this.getPropertyManager().getBooleanProperty("debug", false); // CraftBukkit - don't hardcode
    }

    /**
     * Logs the error message with a level of SEVERE.
     */
    public void logSevere(String par1Str)
    {
        logger.log(Level.SEVERE, par1Str);
    }

    /**
     * If isDebuggingEnabled(), logs the message with a level of INFO.
     */
    public void logDebug(String par1Str)
    {
        if (this.isDebuggingEnabled())
        {
            logger.log(Level.INFO, par1Str);
        }
    }

    public String getServerModName()
    {
        return "mcpc,craftbukkit,forge,fml"; // MCPC+
    }

    /**
     * Adds the server info, including from theWorldServer, to the crash report.
     */
    public CrashReport addServerInfoToCrashReport(CrashReport par1CrashReport)
    {
        par1CrashReport.func_85056_g().addCrashSectionCallable("Profiler Position", new CallableIsServerModded(this));

        if (this.worlds != null && this.worlds.size() > 0 && this.worlds.get(0) != null)
        {
            par1CrashReport.func_85056_g().addCrashSectionCallable("Vec3 Pool Size", new CallableServerProfiler(this));
        }

        if (this.serverConfigManager != null)
        {
            par1CrashReport.func_85056_g().addCrashSectionCallable("Player Count", new CallableServerMemoryStats(this));
        }

        return par1CrashReport;
    }

    /**
     * If par2Str begins with /, then it searches for commands, otherwise it returns players.
     */
    public List getPossibleCompletions(ICommandSender par1ICommandSender, String par2Str)
    {
        // CraftBukkit start - Allow tab-completion of Bukkit commands
        /*
        ArrayList arraylist = new ArrayList();

        if (s.startsWith("/")) {
            s = s.substring(1);
            boolean flag = !s.contains(" ");
            List list = this.q.b(icommandlistener, s);

            if (list != null) {
                Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    String s1 = (String) iterator.next();

                    if (flag) {
                        arraylist.add("/" + s1);
                    } else {
                        arraylist.add(s1);
                    }
                }
            }

            return arraylist;
        } else {
            String[] astring = s.split(" ", -1);
            String s2 = astring[astring.length - 1];
            String[] astring1 = this.t.d();
            int i = astring1.length;

            for (int j = 0; j < i; ++j) {
                String s3 = astring1[j];

                if (CommandAbstract.a(s2, s3)) {
                    arraylist.add(s3);
                }
            }

            return arraylist;
        }
        */
        return this.server.tabComplete(par1ICommandSender, par2Str);
        // CraftBukkit end
    }

    /**
     * Gets mcServer.
     */
    public static MinecraftServer getServer()
    {
        return mcServer;
    }

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getCommandSenderName()
    {
        return "Server";
    }

    public void sendChatToPlayer(String par1Str)
    {
        logger.info(StringUtils.stripControlCodes(par1Str));
    }

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        return true;
    }

    /**
     * Translates and formats the given string key with the given arguments.
     */
    public String translateString(String par1Str, Object ... par2ArrayOfObj)
    {
        return StringTranslate.getInstance().translateKeyFormat(par1Str, par2ArrayOfObj);
    }

    public ICommandManager getCommandManager()
    {
        return this.commandManager;
    }

    /**
     * Gets KeyPair instanced in MinecraftServer.
     */
    public KeyPair getKeyPair()
    {
        return this.serverKeyPair;
    }

    /**
     * Gets serverPort.
     */
    public int getServerPort()
    {
        return this.serverPort;
    }

    public void setServerPort(int par1)
    {
        this.serverPort = par1;
    }

    /**
     * Returns the username of the server owner (for integrated servers)
     */
    public String getServerOwner()
    {
        return this.serverOwner;
    }

    /**
     * Sets the username of the owner of this server (in the case of an integrated server)
     */
    public void setServerOwner(String par1Str)
    {
        this.serverOwner = par1Str;
    }

    public boolean isSinglePlayer()
    {
        return this.serverOwner != null;
    }

    public String getFolderName()
    {
        return this.folderName;
    }

    public void setFolderName(String par1Str)
    {
        this.folderName = par1Str;
    }

    @SideOnly(Side.CLIENT)
    public void setWorldName(String par1Str)
    {
        this.worldName = par1Str;
    }

    @SideOnly(Side.CLIENT)
    public String getWorldName()
    {
        return this.worldName;
    }

    public void setKeyPair(KeyPair par1KeyPair)
    {
        this.serverKeyPair = par1KeyPair;
    }

    public void setDifficultyForAllWorlds(int par1)
    {
        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            WorldServer worldserver = this.worlds.get(j);
            // CraftBukkit end

            if (worldserver != null)
            {
                if (worldserver.getWorldInfo().isHardcoreModeEnabled())
                {
                    worldserver.difficultySetting = 3;
                    worldserver.setAllowedSpawnTypes(true, true);
                }
                else if (this.isSinglePlayer())
                {
                    worldserver.difficultySetting = par1;
                    worldserver.setAllowedSpawnTypes(worldserver.difficultySetting > 0, true);
                }
                else
                {
                    worldserver.difficultySetting = par1;
                    worldserver.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
                }
            }
        }
    }

    protected boolean allowSpawnMonsters()
    {
        return true;
    }

    /**
     * Gets whether this is a demo or not.
     */
    public boolean isDemo()
    {
        return this.isDemo;
    }

    /**
     * Sets whether this is a demo or not.
     */
    public void setDemo(boolean par1)
    {
        this.isDemo = par1;
    }

    public void canCreateBonusChest(boolean par1)
    {
        this.enableBonusChest = par1;
    }

    public ISaveFormat getActiveAnvilConverter()
    {
        return this.anvilConverterForAnvilFile;
    }

    /**
     * WARNING : directly calls
     * getActiveAnvilConverter().deleteWorldDirectory(theWorldServer[0].getSaveHandler().getSaveDirectoryName());
     */
    public void deleteWorldAndStopServer()
    {
        this.worldIsBeingDeleted = true;
        this.getActiveAnvilConverter().flushCache();

        // CraftBukkit start - This needs review, what does it do? (it's new)
        for (int i = 0; i < this.worlds.size(); ++i)
        {
            WorldServer worldserver = this.worlds.get(i);
            // CraftBukkit end

            if (worldserver != null)
            {
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
                worldserver.flush();
            }
        }

        this.getActiveAnvilConverter().deleteWorldDirectory(this.worlds.get(0).getSaveHandler().getSaveDirectoryName()); // CraftBukkit
        this.initiateShutdown();
    }

    public String getTexturePack()
    {
        return this.texturePack;
    }

    public void setTexturePack(String par1Str)
    {
        this.texturePack = par1Str;
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper)
    {
        par1PlayerUsageSnooper.addData("whitelist_enabled", Boolean.valueOf(false));
        par1PlayerUsageSnooper.addData("whitelist_count", Integer.valueOf(0));
        par1PlayerUsageSnooper.addData("players_current", Integer.valueOf(this.getCurrentPlayerCount()));
        par1PlayerUsageSnooper.addData("players_max", Integer.valueOf(this.getMaxPlayers()));
        par1PlayerUsageSnooper.addData("players_seen", Integer.valueOf(this.serverConfigManager.getAvailablePlayerDat().length));
        par1PlayerUsageSnooper.addData("uses_auth", Boolean.valueOf(this.onlineMode));
        par1PlayerUsageSnooper.addData("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
        par1PlayerUsageSnooper.addData("avg_tick_ms", Integer.valueOf((int)(MathHelper.average(this.tickTimeArray) * 1.0E-6D)));
        par1PlayerUsageSnooper.addData("avg_sent_packet_count", Integer.valueOf((int)MathHelper.average(this.sentPacketCountArray)));
        par1PlayerUsageSnooper.addData("avg_sent_packet_size", Integer.valueOf((int)MathHelper.average(this.sentPacketSizeArray)));
        par1PlayerUsageSnooper.addData("avg_rec_packet_count", Integer.valueOf((int)MathHelper.average(this.receivedPacketCountArray)));
        par1PlayerUsageSnooper.addData("avg_rec_packet_size", Integer.valueOf((int)MathHelper.average(this.receivedPacketSizeArray)));
        int var2 = 0;

        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            // if (this.worldServer[j] != null) {
            WorldServer worldserver = this.worlds.get(j);
            // CraftBukkit end
            WorldInfo var3 = worldserver.getWorldInfo();
            par1PlayerUsageSnooper.addData("world[" + var2 + "][dimension]", Integer.valueOf(worldserver.provider.dimensionId));
            par1PlayerUsageSnooper.addData("world[" + var2 + "][mode]", var3.getGameType());
            par1PlayerUsageSnooper.addData("world[" + var2 + "][difficulty]", Integer.valueOf(worldserver.difficultySetting));
            par1PlayerUsageSnooper.addData("world[" + var2 + "][hardcore]", Boolean.valueOf(var3.isHardcoreModeEnabled()));
            par1PlayerUsageSnooper.addData("world[" + var2 + "][generator_name]", var3.getTerrainType().getWorldTypeName());
            par1PlayerUsageSnooper.addData("world[" + var2 + "][generator_version]", Integer.valueOf(var3.getTerrainType().getGeneratorVersion()));
            par1PlayerUsageSnooper.addData("world[" + var2 + "][height]", Integer.valueOf(this.buildLimit));
            par1PlayerUsageSnooper.addData("world[" + var2 + "][chunks_loaded]", Integer.valueOf(worldserver.getChunkProvider().getLoadedChunkCount()));
            ++var2;
            // } // CraftBukkit
        }

        par1PlayerUsageSnooper.addData("worlds", Integer.valueOf(var2));
    }

    public void addServerTypeToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper)
    {
        par1PlayerUsageSnooper.addData("singleplayer", Boolean.valueOf(this.isSinglePlayer()));
        par1PlayerUsageSnooper.addData("server_brand", this.getServerModName());
        par1PlayerUsageSnooper.addData("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
        par1PlayerUsageSnooper.addData("dedicated", Boolean.valueOf(this.isDedicatedServer()));
    }

    /**
     * Returns whether snooping is enabled or not.
     */
    public boolean isSnooperEnabled()
    {
        return true;
    }

    /**
     * This is checked to be 16 upon receiving the packet, otherwise the packet is ignored.
     */
    public int textureSize()
    {
        return 16;
    }

    public abstract boolean isDedicatedServer();

    public boolean isServerInOnlineMode()
    {
        return this.server.getOnlineMode(); // CraftBukkit
    }

    public void setOnlineMode(boolean par1)
    {
        this.onlineMode = par1;
    }

    public boolean getCanSpawnAnimals()
    {
        return this.canSpawnAnimals;
    }

    public void setCanSpawnAnimals(boolean par1)
    {
        this.canSpawnAnimals = par1;
    }

    public boolean getCanSpawnNPCs()
    {
        return this.canSpawnNPCs;
    }

    public void setCanSpawnNPCs(boolean par1)
    {
        this.canSpawnNPCs = par1;
    }

    public boolean isPVPEnabled()
    {
        return this.pvpEnabled;
    }

    public void setAllowPvp(boolean par1)
    {
        this.pvpEnabled = par1;
    }

    public boolean isFlightAllowed()
    {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean par1)
    {
        this.allowFlight = par1;
    }

    /**
     * Return whether command blocks are enabled.
     */
    public abstract boolean isCommandBlockEnabled();

    public String getMOTD()
    {
        return this.motd;
    }

    public void setMOTD(String par1Str)
    {
        this.motd = par1Str;
    }

    public int getBuildLimit()
    {
        return this.buildLimit;
    }

    public void setBuildLimit(int par1)
    {
        this.buildLimit = par1;
    }

    public boolean isServerStopped()
    {
        return this.serverStopped;
    }

    public ServerConfigurationManager getConfigurationManager()
    {
        return this.serverConfigManager;
    }

    public void setConfigurationManager(ServerConfigurationManager par1ServerConfigurationManager)
    {
        this.serverConfigManager = par1ServerConfigurationManager;
    }

    /**
     * Sets the game type for all worlds.
     */
    public void setGameType(EnumGameType par1EnumGameType)
    {
        // CraftBukkit start
        for (int i = 0; i < this.worlds.size(); ++i)
        {
            getServer().worlds.get(i).getWorldInfo().setGameType(par1EnumGameType);
            // CraftBukkit end
        }
    }

    public abstract NetworkListenThread getNetworkThread();

    @SideOnly(Side.CLIENT)
    public boolean serverIsInRunLoop()
    {
        return this.serverIsRunning;
    }

    public boolean getGuiEnabled()
    {
        return false;
    }

    /**
     * On dedicated does nothing. On integrated, sets commandsAllowedForAll, gameType and allows external connections.
     */
    public abstract String shareToLAN(EnumGameType var1, boolean var2);

    public int getTickCounter()
    {
        return this.tickCounter;
    }

    public void enableProfiling()
    {
        this.startProfiling = true;
    }

    @SideOnly(Side.CLIENT)
    public PlayerUsageSnooper getPlayerUsageSnooper()
    {
        return this.usageSnooper;
    }

    /**
     * Return the position for this command sender.
     */
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(0, 0, 0);
    }

    /**
     * Return the spawn protection area's size.
     */
    public int getSpawnProtectionSize()
    {
        return 16;
    }

    /**
     * Gets the current player count, maximum player count, and player entity list.
     */
    public static ServerConfigurationManager getServerConfigurationManager(MinecraftServer par0MinecraftServer)
    {
        return par0MinecraftServer.serverConfigManager;
    }

    @SideOnly(Side.SERVER)
    public static void main(String[] par0ArrayOfStr)
    {
        FMLRelauncher.handleServerRelaunch(new ArgsWrapper(par0ArrayOfStr));
    }

    @SideOnly(Side.SERVER)
    public static void fmlReentry(ArgsWrapper var1)   // CraftBukkit - replaces main(String[] astring)
    {

        logger.severe(var1.args.getClass().getName());
        OptionSet options = org.bukkit.craftbukkit.Main.loadOptions(var1.args);

        if (options == null)
        {
            return;
        }

        cpw.mods.fml.relauncher.FMLLogFormatter.setFormat(options.has("nojline"), options.has("date-format") ? (SimpleDateFormat)options.valueOf("date-format") : null);
        StatList.nopInit();

        try
        {
            DedicatedServer dedicatedserver = new DedicatedServer(options);

            if (options.has("port"))
            {
                int port = (Integer) options.valueOf("port");

                if (port > 0)
                {
                    dedicatedserver.setServerPort(port);
                }
            }

            if (options.has("universe"))
            {
                dedicatedserver.anvilFile = (File) options.valueOf("universe");
            }

            if (options.has("world"))
            {
                dedicatedserver.setFolderName((String) options.valueOf("world"));
            }

            dedicatedserver.primaryThread.setUncaughtExceptionHandler(new org.bukkit.craftbukkit.util.ExceptionHandler()); // Spigot
            dedicatedserver.primaryThread.start();
            // CraftBukkit end
        }
        catch (Exception exception)
        {
            logger.log(Level.SEVERE, "Failed to start the minecraft server", exception);
        }
    }
}
