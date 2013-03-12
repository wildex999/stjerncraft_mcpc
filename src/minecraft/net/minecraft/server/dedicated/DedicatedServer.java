package net.minecraft.server.dedicated;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommand;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.NetworkListenThread;
import net.minecraft.network.rcon.IServer;
import net.minecraft.network.rcon.RConThreadMain;
import net.minecraft.network.rcon.RConThreadQuery;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.ServerGUI;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.CryptManager;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
// CraftBukkit start
import java.io.PrintStream;
import java.util.concurrent.Callable;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;

import org.bukkit.craftbukkit.LoggerOutputStream;
import org.bukkit.event.server.ServerCommandEvent;
// CraftBukkit end

public class DedicatedServer extends MinecraftServer implements IServer
{
    private final List pendingCommandList = Collections.synchronizedList(new ArrayList());
    private RConThreadQuery theRConThreadQuery;
    private RConThreadMain theRConThreadMain;
    public PropertyManager settings; // CraftBukkit - private -> public
    private boolean canSpawnStructures;
    private EnumGameType gameType;
    private NetworkListenThread networkThread;
    private boolean guiIsEnabled = false;

    // MCPC+ start - vanilla compatibility
    public DedicatedServer(File par1File)
    {
        super(par1File);
        new DedicatedServerSleepThread(this);
    }
    // MCPC+ end

    // CraftBukkit start - Signature changed
    public DedicatedServer(joptsimple.OptionSet options)
    {
        super(options);
        // CraftBukkit end
        new DedicatedServerSleepThread(this);
    }

    /**
     * Initialises the server and starts it.
     */
    protected boolean startServer() throws java.net.UnknownHostException   // CraftBukkit - throws UnknownHostException
    {
        DedicatedServerCommandThread dedicatedservercommandthread = new DedicatedServerCommandThread(this);
        dedicatedservercommandthread.setDaemon(true);
        dedicatedservercommandthread.start();
        ConsoleLogManager.init(this); // CraftBukkit
        // CraftBukkit start
        System.setOut(new PrintStream(new LoggerOutputStream(logger, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(logger, Level.SEVERE), true));
        // CraftBukkit end
        logger.info("Starting minecraft server version 1.4.7");

        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L)
        {
            logger.warning("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        FMLCommonHandler.instance().onServerStart(this); // Forge
        logger.info("Loading properties");
        this.settings = new PropertyManager(this.options); // CraftBukkit - CLI argument support

        if (this.isSinglePlayer())
        {
            this.setHostname("127.0.0.1");
        }
        else
        {
            this.setOnlineMode(this.settings.getBooleanProperty("online-mode", true));
            this.setHostname(this.settings.getProperty("server-ip", ""));
        }

        this.setCanSpawnAnimals(this.settings.getBooleanProperty("spawn-animals", true));
        this.setCanSpawnNPCs(this.settings.getBooleanProperty("spawn-npcs", true));
        this.setAllowPvp(this.settings.getBooleanProperty("pvp", true));
        this.setAllowFlight(this.settings.getBooleanProperty("allow-flight", false));
        this.setTexturePack(this.settings.getProperty("texture-pack", ""));
        this.setMOTD(this.settings.getProperty("motd", "A Minecraft Server"));

        if (this.settings.getIntProperty("difficulty", 1) < 0)
        {
            this.settings.setProperty("difficulty", Integer.valueOf(0));
        }
        else if (this.settings.getIntProperty("difficulty", 1) > 3)
        {
            this.settings.setProperty("difficulty", Integer.valueOf(3));
        }

        this.canSpawnStructures = this.settings.getBooleanProperty("generate-structures", true);
        int i = this.settings.getIntProperty("gamemode", EnumGameType.SURVIVAL.getID());
        this.gameType = WorldSettings.getGameTypeById(i);
        logger.info("Default game type: " + this.gameType);
        InetAddress inetaddress = null;

        if (this.getServerHostname().length() > 0)
        {
            inetaddress = InetAddress.getByName(this.getServerHostname());
        }

        if (this.getServerPort() < 0)
        {
            this.setServerPort(this.settings.getIntProperty("server-port", 25565));
        }

        logger.info("Generating keypair");
        this.setKeyPair(CryptManager.createNewKeyPair());
        logger.info("Starting Minecraft server on " + (this.getServerHostname().length() == 0 ? "*" : this.getServerHostname()) + ":" + this.getServerPort());

        try
        {
            this.networkThread = new DedicatedServerListenThread(this, inetaddress, this.getServerPort());
        }
        catch (Throwable throwable)     // CraftBukkit - IOException -> Throwable
        {
            logger.warning("**** FAILED TO BIND TO PORT!");
            logger.log(Level.WARNING, "The exception was: " + throwable.toString());
            logger.warning("Perhaps a server is already running on that port?");
            return false;
        }

        this.setConfigurationManager((ServerConfigurationManager)(new DedicatedPlayerList(this)));  // CraftBukkit

        if (!this.isServerInOnlineMode())
        {
            logger.warning("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            logger.warning("The server will make no attempt to authenticate usernames. Beware.");
            logger.warning("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            logger.warning("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }

        FMLCommonHandler.instance().onServerStarted();
        // this.a((PlayerList) (new DedicatedPlayerList(this))); // CraftBukkit - moved up
        this.anvilConverterForAnvilFile = new AnvilSaveConverter(server.getWorldContainer()); // CraftBukkit - moved from MinecraftServer constructor
        long j = System.nanoTime();

        if (this.getFolderName() == null)
        {
            this.setFolderName(this.settings.getProperty("level-name", "world"));
        }

        String s = this.settings.getProperty("level-seed", "");
        String s1 = this.settings.getProperty("level-type", "DEFAULT");
        String s2 = this.settings.getProperty("generator-settings", "");
        long k = (new Random()).nextLong();

        if (s.length() > 0)
        {
            try
            {
                long l = Long.parseLong(s);

                if (l != 0L)
                {
                    k = l;
                }
            }
            catch (NumberFormatException numberformatexception)
            {
                k = (long)s.hashCode();
            }
        }

        WorldType worldtype = WorldType.parseWorldType(s1);

        if (worldtype == null)
        {
            worldtype = WorldType.DEFAULT;
        }

        this.setBuildLimit(this.settings.getIntProperty("max-build-height", 256));
        this.setBuildLimit((this.getBuildLimit() + 8) / 16 * 16);
        this.setBuildLimit(MathHelper.clamp_int(this.getBuildLimit(), 64, 256));
        this.settings.setProperty("max-build-height", Integer.valueOf(this.getBuildLimit()));
        if (!FMLCommonHandler.instance().handleServerAboutToStart(this)) { return false; }
        logger.info("Preparing level \"" + this.getFolderName() + "\"");
        this.loadAllWorlds(this.getFolderName(), this.getFolderName(), k, worldtype, s2);
        long i1 = System.nanoTime() - j;
        String s3 = String.format("%.3fs", new Object[] {Double.valueOf((double)i1 / 1.0E9D)});
        logger.info("Done (" + s3 + ")! For help, type \"help\" or \"?\"");

        if (this.settings.getBooleanProperty("enable-query", false))
        {
            logger.info("Starting GS4 status listener");
            this.theRConThreadQuery = new RConThreadQuery(this);
            this.theRConThreadQuery.startThread();
        }

        if (this.settings.getBooleanProperty("enable-rcon", false))
        {
            logger.info("Starting remote control listener");
            this.theRConThreadMain = new RConThreadMain(this);
            this.theRConThreadMain.startThread();
            this.remoteConsole = new org.bukkit.craftbukkit.command.CraftRemoteConsoleCommandSender(); // CraftBukkit
        }

        // CraftBukkit start
        if (this.server.getBukkitSpawnRadius() > -1)
        {
            logger.info("'settings.spawn-radius' in bukkit.yml has been moved to 'spawn-protection' in server.properties. I will move your config for you.");
            this.settings.properties.remove("spawn-protection");
            this.settings.getIntProperty("spawn-protection", this.server.getBukkitSpawnRadius());
            this.server.removeBukkitSpawnRadius();
            this.settings.saveProperties();
        }

        return FMLCommonHandler.instance().handleServerStarting(this);
    }

    public PropertyManager getPropertyManager()
    {
        return this.settings;
    }
    // CraftBukkit end

    public boolean canStructuresSpawn()
    {
        return this.canSpawnStructures;
    }

    public EnumGameType getGameType()
    {
        return this.gameType;
    }

    /**
     * Defaults to "1" (Easy) for the dedicated server, defaults to "2" (Normal) on the client.
     */
    public int getDifficulty()
    {
        return Math.max(0, Math.min(3, this.settings.getIntProperty("difficulty", 1))); // CraftBukkit - clamp values
    }

    /**
     * Defaults to false.
     */
    public boolean isHardcore()
    {
        return this.settings.getBooleanProperty("hardcore", false);
    }

    /**
     * Called on exit from the main run() loop.
     */
    protected void finalTick(CrashReport par1CrashReport)
    {
        while (this.isServerRunning())
        {
            this.executePendingCommands();

            try
            {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedexception)
            {
                interruptedexception.printStackTrace();
            }
        }
    }

    /**
     * Adds the server info, including from theWorldServer, to the crash report.
     */
    public CrashReport addServerInfoToCrashReport(CrashReport par1CrashReport)
    {
        par1CrashReport = super.addServerInfoToCrashReport(par1CrashReport);
        par1CrashReport.func_85056_g().addCrashSectionCallable("Is Modded", new CallableType(this));
        par1CrashReport.func_85056_g().addCrashSectionCallable("Type", new CallableServerType(this));
        return par1CrashReport;
    }

    /**
     * Directly calls System.exit(0), instantly killing the program.
     */
    protected void systemExitNow()
    {
        System.exit(0);
    }

    public void updateTimeLightAndEntities()
    {
        super.updateTimeLightAndEntities();
        this.executePendingCommands();
    }

    public boolean getAllowNether()
    {
        return this.settings.getBooleanProperty("allow-nether", true);
    }

    public boolean allowSpawnMonsters()
    {
        return this.settings.getBooleanProperty("spawn-monsters", true);
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper)
    {
        par1PlayerUsageSnooper.addData("whitelist_enabled", Boolean.valueOf(this.getDedicatedPlayerList().isWhiteListEnabled()));
        par1PlayerUsageSnooper.addData("whitelist_count", Integer.valueOf(this.getDedicatedPlayerList().getWhiteListedPlayers().size()));
        super.addServerStatsToSnooper(par1PlayerUsageSnooper);
    }

    /**
     * Returns whether snooping is enabled or not.
     */
    public boolean isSnooperEnabled()
    {
        return this.settings.getBooleanProperty("snooper-enabled", true);
    }

    public void addPendingCommand(String par1Str, ICommandSender par2ICommandSender)
    {
        this.pendingCommandList.add(new ServerCommand(par1Str, par2ICommandSender));
    }

    public void executePendingCommands()
    {
        while (!this.pendingCommandList.isEmpty())
        {
            ServerCommand servercommand = (ServerCommand)this.pendingCommandList.remove(0);
            // CraftBukkit start - ServerCommand for preprocessing
            ServerCommandEvent event = new ServerCommandEvent(this.console, servercommand.command);
            this.server.getPluginManager().callEvent(event);
            servercommand = new ServerCommand(event.getCommand(), servercommand.sender);
            // this.getCommandManager().executeCommand(servercommand.sender, servercommand.command); // Called in dispatchServerCommand
            this.server.dispatchServerCommand(this.console, servercommand);
            // CraftBukkit end
        }
    }

    public boolean isDedicatedServer()
    {
        return true;
    }

    public DedicatedPlayerList getDedicatedPlayerList()
    {
        return (DedicatedPlayerList)super.getConfigurationManager();
    }

    public NetworkListenThread getNetworkThread()
    {
        return this.networkThread;
    }

    /**
     * Gets an integer property. If it does not exist, set it to the specified value.
     */
    public int getIntProperty(String par1Str, int par2)
    {
        return this.settings.getIntProperty(par1Str, par2);
    }

    /**
     * Gets a string property. If it does not exist, set it to the specified value.
     */
    public String getStringProperty(String par1Str, String par2Str)
    {
        return this.settings.getProperty(par1Str, par2Str);
    }

    /**
     * Gets a boolean property. If it does not exist, set it to the specified value.
     */
    public boolean getBooleanProperty(String par1Str, boolean par2)
    {
        return this.settings.getBooleanProperty(par1Str, par2);
    }

    /**
     * Saves an Object with the given property name.
     */
    public void setProperty(String par1Str, Object par2Obj)
    {
        this.settings.setProperty(par1Str, par2Obj);
    }

    /**
     * Saves all of the server properties to the properties file.
     */
    public void saveProperties()
    {
        this.settings.saveProperties();
    }

    /**
     * Returns the filename where server properties are stored
     */
    public String getSettingsFilename()
    {
        File file1 = this.settings.getPropertiesFile();
        return file1 != null ? file1.getAbsolutePath() : "No settings file";
    }

    public boolean getGuiEnabled()
    {
        return this.guiIsEnabled;
    }

    /**
     * On dedicated does nothing. On integrated, sets commandsAllowedForAll, gameType and allows external connections.
     */
    public String shareToLAN(EnumGameType par1EnumGameType, boolean par2)
    {
        return "";
    }

    /**
     * Return whether command blocks are enabled.
     */
    public boolean isCommandBlockEnabled()
    {
        return this.settings.getBooleanProperty("enable-command-block", false);
    }

    /**
     * Return the spawn protection area's size.
     */
    public int getSpawnProtectionSize()
    {
        return this.settings.getIntProperty("spawn-protection", super.getSpawnProtectionSize());
    }

    public ServerConfigurationManager getConfigurationManager()
    {
        return this.getDedicatedPlayerList();
    }

    @SideOnly(Side.SERVER)
    public void enableGui()
    {
        ServerGUI.initGUI(this);
        this.guiIsEnabled = true;
    }
}
