package net.minecraft.server;

import cpw.mods.fml.common.FMLCommonHandler;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.command.CommandChunkSampling;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.dispenser.DispenserBehaviors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.logging.ILogAgent;
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
import net.minecraft.command.ServerCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.world.World;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.WorldSaveEvent;


// CraftBukkit end
// MCPC+ start
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.EnumHelper;
import w999.baseprotect.BaseProtect;
import za.co.mcportcentral.FMLLogJLineBreakProxy;
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
    public WorldServer[] worldServers = new WorldServer[0]; // MCPC+ - vanilla compatibility

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
    private boolean field_104057_T = false;

    // CraftBukkit start
    public List<WorldServer> worlds = new ArrayList<WorldServer>();
    public org.bukkit.craftbukkit.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    public ConsoleReader reader;
    public static int currentTick = (int)(System.currentTimeMillis() / 50);
    public final Thread primaryThread;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
    public int autosavePeriod;
    // CraftBukkit end
    // Spigot start
    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    public static double currentTPS = 0;
    private static long catchupTime = 0;
    // Spigot end
    public static boolean callingForgeTick = false; // MCPC+ handle loadOnRequest during forge tick events
    // MCPC+ start - vanilla compatibility
    public MinecraftServer(File par1File)
    {
        mcServer = this;
        this.anvilFile = par1File;
        this.commandManager = new ServerCommandManager();
        this.anvilConverterForAnvilFile = new AnvilSaveConverter(par1File);
        this.registerDispenseBehaviors();
        primaryThread = null;
    }
    // MCPC+ end

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
            catch (IOException ex)
            {
                Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (org.bukkit.craftbukkit.Main.useJline)
        {
            FMLLogJLineBreakProxy.reader = this.reader;
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
        DispenserBehaviors.func_96467_a();
    }

    /**
     * Initialises the server and starts it.
     */
    protected abstract boolean startServer() throws java.net.UnknownHostException; // CraftBukkit - throws UnknownHostException

    protected void convertMapIfNeeded(String par1Str)
    {
        if (this.getActiveAnvilConverter().isOldMapFormat(par1Str))
        {
            this.getLogAgent().logInfo("Converting map!");
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
        // CraftBukkit - Removed ticktime arrays
        ISaveHandler isavehandler = this.anvilConverterForAnvilFile.getSaveLoader(par1Str, true);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        // CraftBukkit start - Removed worldsettings
        
        WorldSettings worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
        worldsettings.func_82750_a(par6Str);
        WorldServer world;

        org.bukkit.generator.ChunkGenerator overWorldGen = this.server.getGenerator(par1Str);
        WorldServer overWorld = (isDemo() ? new DemoWorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, theProfiler, this.getLogAgent()) : new WorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, worldsettings, theProfiler, this.getLogAgent(), Environment.getEnvironment(0), overWorldGen));
        if (overWorldGen != null)
        {
            overWorld.getWorld().getPopulators().addAll(overWorldGen.getDefaultPopulators(overWorld.getWorld()));
        }

        for (int dimension : DimensionManager.getStaticDimensionIDs())
        {
            String worldType = "";
            String name = "";
            String oldName = "";
            org.bukkit.generator.ChunkGenerator gen = null;
            // MCPC+ start
            Environment env = Environment.getEnvironment(dimension);
            if (dimension != 0)
            {
                if ((dimension == -1 && !this.getAllowNether()) || (dimension == 1 && !this.server.getAllowEnd()))
                    continue;
                if (env == null)
                {
                    WorldProvider provider = WorldProvider.getProviderForDimension(dimension);
                    worldType = provider.getClass().getSimpleName().toLowerCase();
                    worldType = worldType.replace("worldprovider", "");
                    oldName = "world_" + worldType.toLowerCase();
                    worldType = worldType.replace("provider", "");
                    env = Environment.getEnvironment(DimensionManager.getProviderType(provider.getClass()));
                    name = provider.getSaveFolder();
                }
                else 
                {
                    worldType = env.toString().toLowerCase();
                    name = "DIM" + dimension;
                    oldName = par1Str + "_" + worldType;
                    oldName = oldName.replaceAll(" ", "_");
                }
                gen = this.server.getGenerator(name);
                worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
                worldsettings.func_82750_a(par6Str);

                migrateWorlds(worldType, oldName, par1Str, name);

                this.setUserMessage(name);
            }

            world = (dimension == 0 ? overWorld : new WorldServerMulti(this, new AnvilSaveHandler(server.getWorldContainer(), name, true), name, dimension, worldsettings, overWorld, this.theProfiler, this.getLogAgent(), env, gen));
            // MCPC+ end
            if (gen != null)
            {
                world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
            }

            this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));
            world.addWorldAccess(new WorldManager(this, world));

            if (!this.isSinglePlayer())
            {
                world.getWorldInfo().setGameType(this.getGameType());
            }

            this.serverConfigManager.setPlayerManager(this.worlds.toArray(new WorldServer[this.worlds.size()]));
            // CraftBukkit end
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load((World)world)); // Forge
        }
        this.setDifficultyForAllWorlds(this.getDifficulty());
        this.initialWorldChunkLoad();
        CraftBlock.dumpMaterials(); // MCPC+
        // MCPC+ start - register TE's for inventory events
        for (Object obj : TileEntity.classToNameMap.entrySet())
        {
            Map.Entry<Class<? extends TileEntity>, String> tileEntry = (Map.Entry<Class<? extends TileEntity>, String>)obj;
            if (tileEntry.getKey() == null)
                continue;
            EnumHelper.addInventoryType(tileEntry.getKey(), tileEntry.getValue());
        }
        // MCPC+ end
    }

    protected void initialWorldChunkLoad()
    {
        short short1 = 196;
        long i = System.currentTimeMillis();
        this.setUserMessage("menu.generatingTerrain");
        byte b0 = 0;

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
            ChunkCoordinates chunkcoordinates = worldserver.getSpawnPoint();
            boolean before = worldserver.theChunkProviderServer.loadChunkOnProvideRequest; // MCPC+ remember previous value
            worldserver.theChunkProviderServer.loadChunkOnProvideRequest = true; // MCPC+ force chunks to load
            for (int k = -short1; k <= short1 && this.isServerRunning(); k += 16)
            {
                for (int l = -short1; l <= short1 && this.isServerRunning(); l += 16)
                {
                    long i1 = System.currentTimeMillis();

                    if (i1 < i)
                    {
                        i = i1;
                    }

                    if (i1 > i + 1000L)
                    {
                        int j1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int k1 = (k + short1) * (short1 * 2 + 1) + l + 1;
                        this.outputPercentRemaining("Preparing spawn area", k1 * 100 / j1);
                        i = i1;
                    }

                    worldserver.theChunkProviderServer.loadChunk(chunkcoordinates.posX + j >> 4, chunkcoordinates.posZ + k >> 4);
                }
            }
            worldserver.theChunkProviderServer.loadChunkOnProvideRequest = before; // MCPC+ force chunks to load
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
        this.getLogAgent().logInfo(par1Str + ": " + par2 + "%");
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

    /**
     * par1 indicates if a log message should be output.
     */
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
                        this.getLogAgent().logInfo("Saving chunks for level \'" + worldserver.getWorldInfo().getWorldName() + "\'/" + worldserver.provider.getDimensionName());
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

    /**
     * Saves all necessary data as preparation for stopping the server.
     */
    public void stopServer() throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            this.getLogAgent().logInfo("Stopping server");

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
                this.getLogAgent().logInfo("Saving players");
                this.serverConfigManager.saveAllPlayerData();
                this.serverConfigManager.removeAllPlayers();
            }

            this.getLogAgent().logInfo("Saving worlds");
            this.saveAllWorlds(false);

            for (int i = 0; i < this.worlds.size(); ++i)
            {
                WorldServer worldserver = this.worlds.get(i);
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver)); // Forge
                DimensionManager.setWorld(worldserver.provider.dimensionId, (WorldServer)null);
            }

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

                server.getCommandMap().register("chunksampling2", new CommandChunkSampling()); //MCPC+ - Register ChunkSampling Command
                new BaseProtect(server); //MCPC+ - Initialize BaseProtect(Must be done before world loads)
               
                // Spigot start
                for (long lastTick = 0L; this.serverRunning; this.serverIsRunning = true)
                {
                    long curTime = System.nanoTime();
                    long wait = TICK_TIME - (curTime - lastTick) - catchupTime;

                    if (wait > 0)
                    {
                        Thread.sleep(wait / 1000000);
                        catchupTime = 0;
                        continue;
                    }
                    else
                    {
                        catchupTime = Math.min(TICK_TIME * TPS, Math.abs(wait));
                    }

                    currentTPS = (currentTPS * 0.95) + (1E9 / (curTime - lastTick) * 0.05);
                    lastTick = curTime;
                    MinecraftServer.currentTick++;
                    SpigotTimings.serverTickTimer.startTiming(); // Spigot
                    this.tick();
                    SpigotTimings.serverTickTimer.stopTiming(); // Spigot
                    org.bukkit.CustomTimingsHandler.tick(); // Spigot
                    org.spigotmc.WatchdogThread.tick();
                }

                // Spigot end
                FMLCommonHandler.instance().handleServerStopping();
            }
            else
            {
                this.finalTick((CrashReport)null);
            }
        }
        catch (Throwable throwable)
        {
        	w999.baseprotect.IWorldInteract current = World.currentTickItem;
        	if(current != null)
        	{
        		System.err.println("CRASH, item: " + current.getClass().getName() + " (X: " + current.getX() + " Y: " + current.getY() + " Z: " + current.getZ() + " )");
        		w999.baseprotect.PlayerData player = current.getItemOwner();
        		
        		if(player != null)
        		{
        			System.err.println("Owner: " + player.getPlayer());
        		}
        	}
        	else
        		System.err.println("Crashed with NULL item");
        	
            if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
            {
                return;
            }
            throwable.printStackTrace();
            this.getLogAgent().logSevereException("Encountered an unexpected exception " + throwable.getClass().getSimpleName(), throwable);
            CrashReport crashreport = null;

            if (throwable instanceof ReportedException)
            {
                crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable).getCrashReport());
            }
            else
            {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file1, this.getLogAgent()))
            {
                this.getLogAgent().logSevere("This crash report has been saved to: " + file1.getAbsolutePath());
            }
            else
            {
                this.getLogAgent().logSevere("We were unable to save this crash report to disk.");
            }

            this.finalTick(crashreport);
        }
        finally
        {
            org.spigotmc.WatchdogThread.doStop(); // Spigot

            try
            {
                if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
                {
                    return;
                }
                this.stopServer();
                this.serverStopped = true;
            }
            catch (Throwable throwable1)
            {
                throwable1.printStackTrace();
            }
            finally
            {
                // CraftBukkit start - Restore terminal to original settings
                try
                {
                    this.reader.getTerminal().restore();
                }
                catch (Exception e)
                {
                }

                // CraftBukkit end
                FMLCommonHandler.instance().handleServerStopped();
                this.serverStopped = true;
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
    public void tick() throws MinecraftException   // CraftBukkit - added throws // MCPC+ - protected -> public for Forge
    {
    	//MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample();
    	//MCPC+ End
    	
        FMLCommonHandler.instance().rescheduleTicks(Side.SERVER); // Forge
        long i = System.nanoTime();
        AxisAlignedBB.getAABBPool().cleanPool();
        callingForgeTick = true; // MCPC+ start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPreServerTick(); // Forge
        callingForgeTick = false; // MCPC+ end
        ++this.tickCounter;

        if (this.startProfiling)
        {
            this.startProfiling = false;
            this.theProfiler.profilingEnabled = true;
            this.theProfiler.clearProfiling();
        }
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("PreTick");
    	//MCPC+ End

        this.theProfiler.startSection("root");
        this.updateTimeLightAndEntities();
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("PostTick");
    	//MCPC+ End

        if ((this.autosavePeriod > 0) && ((this.tickCounter % this.autosavePeriod) == 0))   // CraftBukkit
        {
            this.theProfiler.startSection("save");
            this.serverConfigManager.saveAllPlayerData();
            this.saveAllWorlds(true);
            this.theProfiler.endSection();
        }

        this.theProfiler.startSection("tallying");
        this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
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
        callingForgeTick = true; // MCPC+ start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPostServerTick();
        callingForgeTick = false; // MCPC+ end
        
        //MCPC+ Start
        if(ChunkSampler.sampling)
        {
        	ChunkSampler.preSample("endTick");
        	ChunkSampler.nextTick();
        }
        //MCPC+ End
    }

    public void updateTimeLightAndEntities()
    {
        this.theProfiler.startSection("levels");
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
        // CraftBukkit start
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);

        // Run tasks that are waiting on processing
        while (!processQueue.isEmpty())
        {
            processQueue.remove().run();
        }
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("queuedTasks");
    	//MCPC+ End

        SpigotTimings.schedulerTimer.stopTiming(); // Spigot
        SpigotTimings.chunkIOTickTimer.startTiming(); // Spigot
        org.bukkit.craftbukkit.chunkio.ChunkIOExecutor.tick();
        SpigotTimings.chunkIOTickTimer.stopTiming(); // Spigot
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("chunkIO");
    	//MCPC+ End

        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCounter % 20 == 0)
        {
            for (int i = 0; i < this.getConfigurationManager().playerEntityList.size(); ++i)
            {
                EntityPlayerMP entityplayer = (EntityPlayerMP) this.getConfigurationManager().playerEntityList.get(i);
                entityplayer.playerNetServerHandler.sendPacketToPlayer(new Packet4UpdateTime(entityplayer.worldObj.getTotalWorldTime(), entityplayer.getPlayerTime())); // Add support for per player time
            }
        }

        int i;
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("preUpdateTimeLightAndEntities");
    	//MCPC+ End

        
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
            if (this.tickCounter % 20 == 0)
            {
                this.theProfiler.startSection("timeSync");
                this.serverConfigManager.sendPacketToAllPlayersInDimension(new Packet4UpdateTime(worldserver.getTotalWorldTime(), worldserver.getWorldTime()), worldserver.provider.dimensionId);
                this.theProfiler.endSection();
            }
            // CraftBukkit end */
            this.theProfiler.startSection("tick");
            FMLCommonHandler.instance().onPreWorldTick(worldserver);
            
            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("onPreWorldTick");
        	//MCPC+ End
            
            
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
            
            
            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("onPostWorldtick");
        	//MCPC+ End
            
            worldserver.timings.tracker.startTiming(); // Spigot
            worldserver.getEntityTracker().updateTrackedEntities();
            worldserver.timings.tracker.stopTiming(); // Spigot
            this.theProfiler.endSection();
            this.theProfiler.endSection();
            
            //MCPC+ Start
        	if(ChunkSampler.sampling)
        		ChunkSampler.preSample("updateTrackedEntities");
        	//MCPC+ End

            // Forge start
            ((long[]) this.worldTickTimes.get(id))[this.tickCounter % 100] = System.nanoTime() - j;
        }

        this.theProfiler.endStartSection("dim_unloading");
        DimensionManager.unloadWorlds(this.worldTickTimes);
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("unloadWorlds");
    	//MCPC+ End
        
        // Forge end
        this.theProfiler.endStartSection("connection");
        SpigotTimings.connectionTimer.startTiming(); // Spigot
        this.getNetworkThread().networkTick();
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("networkTick");
    	//MCPC+ End
        
        SpigotTimings.connectionTimer.stopTiming(); // Spigot
        this.theProfiler.endStartSection("players");
        SpigotTimings.playerListTimer.startTiming(); // Spigot
        this.serverConfigManager.sendPlayerInfoToAllPlayers();
        SpigotTimings.playerListTimer.stopTiming(); // Spigot
        this.theProfiler.endStartSection("tickables");
        SpigotTimings.tickablesTimer.startTiming(); // Spigot

        for (i = 0; i < this.tickables.size(); ++i)
        {
            ((IUpdatePlayerListBox)this.tickables.get(i)).update();
        }

        SpigotTimings.tickablesTimer.stopTiming(); // Spigot
        this.theProfiler.endSection();
        
        //MCPC+ Start
    	if(ChunkSampler.sampling)
    		ChunkSampler.preSample("postUpdateTimeLightAndEntities");
    	//MCPC+ End
        
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
        this.getLogAgent().logInfo(par1Str);
    }

    /**
     * Logs the message with a level of WARN.
     */
    public void logWarning(String par1Str)
    {
        this.getLogAgent().logWarning(par1Str);
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
        if (cpw.mods.fml.relauncher.FMLInjectionData.obf151()) return "1.5.1"; // MCPC+
        return "1.5.2";
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
        // CraftBukkit start - Whole method
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
        catch (java.util.concurrent.ExecutionException e)
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
        this.getLogAgent().logSevere(par1Str);
    }

    /**
     * If isDebuggingEnabled(), logs the message with a level of INFO.
     */
    public void logDebug(String par1Str)
    {
        if (this.isDebuggingEnabled())
        {
            this.getLogAgent().logInfo(par1Str);
        }
    }

    public String getServerModName()
    {
        return FMLCommonHandler.instance().getModName();
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
        //return this.server.tabComplete(par1ICommandSender, par2Str);
        // CraftBukkit end

        // MCPC+ start -- allow vanilla and bukkit command completion
        java.util.HashSet arraylist = new java.util.HashSet(); // use a set here to avoid duplicates

        if (par2Str.startsWith("/")) {
            par2Str = par2Str.substring(1);
            boolean flag = !par2Str.contains(" ");
            List list = this.commandManager.getPossibleCommands(par1ICommandSender, par2Str);

            if (list != null) {
                java.util.Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    String s1 = (String) iterator.next();

                    if (flag) {
                        arraylist.add("/" + s1);
                    } else {
                        arraylist.add(s1);
                    }
                }
            }
        } else {
            String[] astring = par2Str.split(" ", -1);
            String s2 = astring[astring.length - 1];
            String[] astring1 = this.serverConfigManager.getAllUsernames();
            int i = astring1.length;

            for (int j = 0; j < i; ++j) {
                String s3 = astring1[j];

                if (net.minecraft.command.CommandBase.doesStringStartWith(s2, s3)) {
                    arraylist.add(s3);
                }
            }
        }
        arraylist.addAll(this.server.tabComplete(par1ICommandSender, par2Str)); // Add craftbukkit commands
        return new ArrayList(arraylist);
        // MCPC+ end
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
        this.getLogAgent().logInfo(StringUtils.stripControlCodes(par1Str));
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
     * getActiveAnvilConverter().deleteWorldDirectory(theWorldServer[0].getSaveHandler().getWorldDirectoryName());
     */
    public void deleteWorldAndStopServer()
    {
        this.worldIsBeingDeleted = true;
        this.getActiveAnvilConverter().flushCache();

        for (int i = 0; i < this.worlds.size(); ++i)
        {
            WorldServer worldserver = this.worlds.get(i);

            if (worldserver != null)
            {
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
                worldserver.flush();
            }
        }

        this.getActiveAnvilConverter().deleteWorldDirectory(this.worlds.get(0).getSaveHandler().getWorldDirectoryName()); // CraftBukkit
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
        int i = 0;

        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            // if (this.worldServer[j] != null) {
            WorldServer worldserver = this.worlds.get(j);
            // CraftBukkit end
            WorldInfo worldinfo = worldserver.getWorldInfo();
            par1PlayerUsageSnooper.addData("world[" + i + "][dimension]", Integer.valueOf(worldserver.provider.dimensionId));
            par1PlayerUsageSnooper.addData("world[" + i + "][mode]", worldinfo.getGameType());
            par1PlayerUsageSnooper.addData("world[" + i + "][difficulty]", Integer.valueOf(worldserver.difficultySetting));
            par1PlayerUsageSnooper.addData("world[" + i + "][hardcore]", Boolean.valueOf(worldinfo.isHardcoreModeEnabled()));
            par1PlayerUsageSnooper.addData("world[" + i + "][generator_name]", worldinfo.getTerrainType().getWorldTypeName());
            par1PlayerUsageSnooper.addData("world[" + i + "][generator_version]", Integer.valueOf(worldinfo.getTerrainType().getGeneratorVersion()));
            par1PlayerUsageSnooper.addData("world[" + i + "][height]", Integer.valueOf(this.buildLimit));
            par1PlayerUsageSnooper.addData("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.getChunkProvider().getLoadedChunkCount()));
            ++i;
            // } // CraftBukkit
        }

        par1PlayerUsageSnooper.addData("worlds", Integer.valueOf(i));
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
        return org.bukkit.craftbukkit.Spigot.textureResolution; // Spigot
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
    public abstract String shareToLAN(EnumGameType enumgamemode, boolean flag);

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

    public boolean func_96290_a(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer)
    {
        return false;
    }

    public abstract ILogAgent getLogAgent();

    public void func_104055_i(boolean par1)
    {
        this.field_104057_T = par1;
    }

    public boolean func_104056_am()
    {
        return this.field_104057_T;
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
    public static void fmlReentry(ArgsWrapper wrap)   // CraftBukkit - replaces main(String[] astring)
    {
        logger.severe(wrap.args.getClass().getName());
        OptionSet options = org.bukkit.craftbukkit.Main.loadOptions(wrap.args);

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

    // MCPC+ start - moved world migrations to its own method
    public boolean migrateWorlds(String worldType, String oldWorldContainer, String newWorldContainer, String worldName)
    {
        boolean result = true;
        File newWorld = new File(new File(newWorldContainer), worldName);
        File oldWorld = new File(new File(oldWorldContainer), worldName);

        if ((!newWorld.isDirectory()) && (oldWorld.isDirectory()))
        {
            final ILogAgent log = this.getLogAgent();
            log.logInfo("---- Migration of old " + worldType + " folder required ----");
            log.logInfo("MCPC has moved back to using the Forge World structure, your " + worldType + " folder will be moved to a new location in order to operate correctly.");
            log.logInfo("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using MCPC in the future.");
            log.logInfo("Attempting to move " + oldWorld + " to " + newWorld + "...");

            if (newWorld.exists())
            {
                log.logSevere("A file or folder already exists at " + newWorld + "!");
                log.logInfo("---- Migration of old " + worldType + " folder failed ----");
                result = false;
            }
            else if (newWorld.getParentFile().mkdirs() || newWorld.getParentFile().exists())
            {
                log.logInfo("Success! To restore " + worldType + " in the future, simply move " + newWorld + " to " + oldWorld);

                // Migrate world data
                try
                {
                    com.google.common.io.Files.move(oldWorld, newWorld);
                }
                catch (IOException exception)
                {
                    log.logSevere("Unable to move world data.");
                    exception.printStackTrace();
                    result = false;
                }
                try
                {
                    com.google.common.io.Files.copy(new File(oldWorld.getParent(), "level.dat"), new File(newWorld, "level.dat"));
                }
                catch (IOException exception)
                {
                    log.logSevere("Unable to migrate world level.dat.");
                }

                log.logInfo("---- Migration of old " + worldType + " folder complete ----");
            }
            else result = false;
        }
        return result;
    }
    // MCPC+ end
}