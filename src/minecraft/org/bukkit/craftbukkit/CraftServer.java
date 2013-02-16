package org.bukkit.craftbukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Warning.WarningState;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.conversations.Conversable;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.craftbukkit.inventory.RecipeIterator;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.craftbukkit.metadata.EntityMetadataStore;
import org.bukkit.craftbukkit.metadata.PlayerMetadataStore;
import org.bukkit.craftbukkit.metadata.WorldMetadataStore;
import org.bukkit.craftbukkit.potion.CraftPotionBrewer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.updater.AutoUpdater;
import org.bukkit.craftbukkit.updater.BukkitDLUpdaterService;
import org.bukkit.craftbukkit.util.DatFileFilter;
import org.bukkit.craftbukkit.util.Versioning;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.scheduler.BukkitWorker;
import org.bukkit.util.StringUtil;
import org.bukkit.util.permissions.DefaultPermissions;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import org.apache.commons.lang.Validate;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import guava10.com.google.common.collect.ImmutableList;
import guava10.com.google.common.collect.MapMaker;
import org.bukkit.craftbukkit.command.CraftSimpleCommandMap; // MCPC+
// Forge start
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Unload;
// Forge end

import jline.console.ConsoleReader;

public final class CraftServer implements Server {
    private final String serverName = "CraftBukkit";
    private final String serverVersion;
    private final String bukkitVersion = Versioning.getBukkitVersion();
    private final ServicesManager servicesManager = new SimpleServicesManager();
    private final CraftScheduler scheduler = new CraftScheduler();
    private final CraftSimpleCommandMap craftCommandMap = new CraftSimpleCommandMap(this); // MCPC+
    private final SimpleCommandMap commandMap = new SimpleCommandMap(this);
    private final SimpleHelpMap helpMap = new SimpleHelpMap(this);
    private final StandardMessenger messenger = new StandardMessenger();
    private final PluginManager pluginManager = new SimplePluginManager(this, commandMap);
    protected final net.minecraft.server.MinecraftServer/*was:MinecraftServer*/ console;
    protected final net.minecraft.server.dedicated.DedicatedPlayerList/*was:DedicatedPlayerList*/ playerList;
    private final Map<String, World> worlds = new LinkedHashMap<String, World>();
    public YamlConfiguration configuration; // Spigot private -> protected // MCPC+ - public for JavaPluginLoader
    private final Yaml yaml = new Yaml(new SafeConstructor());
    private final Map<String, OfflinePlayer> offlinePlayers = new MapMaker().softValues().makeMap();
    private final AutoUpdater updater;
    private final EntityMetadataStore entityMetadata = new EntityMetadataStore();
    private final PlayerMetadataStore playerMetadata = new PlayerMetadataStore();
    private final WorldMetadataStore worldMetadata = new WorldMetadataStore();
    private int monsterSpawn = -1;
    private int animalSpawn = -1;
    private int waterAnimalSpawn = -1;
    private int ambientSpawn = -1;
    public int chunkGCPeriod = -1;
    public int chunkGCLoadThresh = 0;
    private File container;
    private WarningState warningState = WarningState.DEFAULT;
    // Spigot start
    public String whitelistMessage = "You are not white-listed on this server!";
    public String stopMessage = "Server restarting. Brb";
    public boolean logCommands = true;
    public boolean ipFilter = false;
    public boolean commandComplete = true;
    public List<String> spamGuardExclusions;
    // Spigot end
    private boolean dumpMaterials = false; // MCPC+

    private final BooleanWrapper online = new BooleanWrapper();

    // Orebfuscator use
    public boolean orebfuscatorEnabled = false;
    public int orebfuscatorUpdateRadius = 2;
    public List<String> orebfuscatorDisabledWorlds;

    private final class BooleanWrapper {
        private boolean value = true;
    }

    static {
        ConfigurationSerialization.registerClass(CraftOfflinePlayer.class);
        CraftItemFactory.instance();
    }

    public CraftServer(net.minecraft.server.MinecraftServer/*was:MinecraftServer*/ console, net.minecraft.server.management.ServerConfigurationManager/*was:PlayerList*/ playerList) {
        this.console = console;
        this.playerList = (net.minecraft.server.dedicated.DedicatedPlayerList/*was:DedicatedPlayerList*/) playerList;
        this.serverVersion = CraftServer.class.getPackage().getImplementationVersion();
        online.value = console.getPropertyManager().getBooleanProperty/*was:getBoolean*/("online-mode", true);

        Bukkit.setServer(this);

        // Register all the Enchantments and PotionTypes now so we can stop new registration immediately after
        net.minecraft.enchantment.Enchantment/*was:Enchantment*/.sharpness/*was:DAMAGE_ALL*/.getClass();
        //org.bukkit.enchantments.Enchantment.stopAcceptingRegistrations(); // MCPC - allow registrations

        Potion.setPotionBrewer(new CraftPotionBrewer());
        net.minecraft.potion.Potion/*was:MobEffectList*/.blindness/*was:BLINDNESS*/.getClass();
        //PotionEffectType.stopAcceptingRegistrations(); // MCPC - allow registrations
        // Ugly hack :(

        if (!Main.useConsole) {
            getLogger().info("Console input is disabled due to --noconsole command argument");
        }

        configuration = YamlConfiguration.loadConfiguration(getConfigFile());
        configuration.options().copyDefaults(true);
        configuration.setDefaults(YamlConfiguration.loadConfiguration(getClass().getClassLoader().getResourceAsStream("configurations/bukkit.yml")));
        saveConfig();
        ((SimplePluginManager) pluginManager).useTimings(configuration.getBoolean("settings.plugin-profiling"));
        monsterSpawn = configuration.getInt("spawn-limits.monsters");
        animalSpawn = configuration.getInt("spawn-limits.animals");
        waterAnimalSpawn = configuration.getInt("spawn-limits.water-animals");
        ambientSpawn = configuration.getInt("spawn-limits.ambient");
        console.autosavePeriod = configuration.getInt("ticks-per.autosave");
        warningState = WarningState.value(configuration.getString("settings.deprecated-verbose"));
        chunkGCPeriod = configuration.getInt("chunk-gc.period-in-ticks");
        chunkGCLoadThresh = configuration.getInt("chunk-gc.load-threshold");
        dumpMaterials = configuration.getBoolean("mcpc.dump-materials"); // MCPC+ - dumps all materials with their corresponding id's

        updater = new AutoUpdater(new BukkitDLUpdaterService(configuration.getString("auto-updater.host")), getLogger(), configuration.getString("auto-updater.preferred-channel"));
        updater.setEnabled(false);
        updater.setSuggestChannels(configuration.getBoolean("auto-updater.suggest-channels"));
        updater.getOnBroken().addAll(configuration.getStringList("auto-updater.on-broken"));
        updater.getOnUpdate().addAll(configuration.getStringList("auto-updater.on-update"));
        updater.check(serverVersion);

        // Spigot start
        Spigot.initialize(this, commandMap, configuration);

        try {
            configuration.save(getConfigFile());
        } catch (IOException e) {
        }
        try {
            new org.bukkit.craftbukkit.util.Metrics().start();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not start metrics", e);
        }
        // Spigot end

        loadPlugins();
        enablePlugins(PluginLoadOrder.STARTUP);
    }

    private File getConfigFile() {
        return (File) console.options.valueOf("bukkit-settings");
    }

    public void saveConfig() { // Spigot private -> public
        try {
            configuration.save(getConfigFile());
        } catch (IOException ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, "Could not save " + getConfigFile(), ex);
        }
    }

    public void loadPlugins() {
        pluginManager.registerInterface(JavaPluginLoader.class);

        File pluginFolder = (File) console.options.valueOf("plugins");

        if (pluginFolder.exists()) {
            Plugin[] plugins = pluginManager.loadPlugins(pluginFolder);
            for (Plugin plugin : plugins) {
                try {
                    String message = String.format("Loading %s", plugin.getDescription().getFullName());
                    plugin.getLogger().info(message);
                    plugin.onLoad();
                } catch (Throwable ex) {
                    Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                }
            }
        } else {
            pluginFolder.mkdir();
        }
    }

    public void enablePlugins(PluginLoadOrder type) {
        if (type == PluginLoadOrder.STARTUP) {
            helpMap.clear();
            helpMap.initializeGeneralTopics();
        }

        Plugin[] plugins = pluginManager.getPlugins();

        for (Plugin plugin : plugins) {
            if ((!plugin.isEnabled()) && (plugin.getDescription().getLoad() == type)) {
                loadPlugin(plugin);
            }
        }

        if (type == PluginLoadOrder.POSTWORLD) {
            commandMap.registerServerAliases();
            loadCustomPermissions();
            DefaultPermissions.registerCorePermissions();
            helpMap.initializeCommands();
        }
    }

    public void disablePlugins() {
        pluginManager.disablePlugins();
    }

    private void loadPlugin(Plugin plugin) {
        try {
            pluginManager.enablePlugin(plugin);

            List<Permission> perms = plugin.getDescription().getPermissions();

            for (Permission perm : perms) {
                try {
                    pluginManager.addPermission(perm);
                } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.WARNING, "Plugin " + plugin.getDescription().getFullName() + " tried to register permission '" + perm.getName() + "' but it's already registered", ex);
                }
            }
        } catch (Throwable ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
        }
    }

    public String getName() {
        return serverName;
    }

    public String getVersion() {
        return serverVersion + " (MC: " + console.getMinecraftVersion/*was:getVersion*/() + ")";
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    @SuppressWarnings("unchecked")
    public Player[] getOnlinePlayers() {
        List<net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/> online = playerList.playerEntityList/*was:players*/;
        Player[] players = new Player[online.size()];

        for (int i = 0; i < players.length; i++) {
            players[i] = online.get(i).playerNetServerHandler/*was:playerConnection*/.getPlayerB();
        }

        return players;
    }

    public Player getPlayer(final String name) {
        Player[] players = getOnlinePlayers();

        Player found = null;
        String lowerName = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : players) {
            if (player.getName().toLowerCase().startsWith(lowerName)) {
                int curDelta = player.getName().length() - lowerName.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) break;
            }
        }
        return found;
    }

    public Player getPlayerExact(String name) {
        String lname = name.toLowerCase();

        for (Player player : getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(lname)) {
                return player;
            }
        }

        return null;
    }

    public int broadcastMessage(String message) {
        return broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public Player getPlayer(final net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entity) {
        return entity.playerNetServerHandler/*was:playerConnection*/.getPlayerB();
    }

    public List<Player> matchPlayer(String partialName) {
        List<Player> matchedPlayers = new ArrayList<Player>();

        for (Player iterPlayer : this.getOnlinePlayers()) {
            String iterPlayerName = iterPlayer.getName();

            if (partialName.equalsIgnoreCase(iterPlayerName)) {
                // Exact match
                matchedPlayers.clear();
                matchedPlayers.add(iterPlayer);
                break;
            }
            if (iterPlayerName.toLowerCase().contains(partialName.toLowerCase())) {
                // Partial match
                matchedPlayers.add(iterPlayer);
            }
        }

        return matchedPlayers;
    }

    public int getMaxPlayers() {
        return playerList.getMaxPlayers/*was:getMaxPlayers*/();
    }

    // NOTE: These are dependent on the corrisponding call in MinecraftServer
    // so if that changes this will need to as well
    public int getPort() {
        return this.getConfigInt("server-port", 25565);
    }

    public int getViewDistance() {
        return this.getConfigInt("view-distance", 10);
    }

    public String getIp() {
        return this.getConfigString("server-ip", "");
    }

    public String getServerName() {
        return this.getConfigString("server-name", "Unknown Server");
    }

    public String getServerId() {
        return this.getConfigString("server-id", "unnamed");
    }

    public String getWorldType() {
        return this.getConfigString("level-type", "DEFAULT");
    }

    public boolean getGenerateStructures() {
        return this.getConfigBoolean("generate-structures", true);
    }

    public boolean getAllowEnd() {
        return this.configuration.getBoolean("settings.allow-end");
    }

    public boolean getAllowNether() {
        return this.getConfigBoolean("allow-nether", true);
    }

    public boolean getWarnOnOverload() {
        return this.configuration.getBoolean("settings.warn-on-overload");
    }

    public boolean getQueryPlugins() {
        return this.configuration.getBoolean("settings.query-plugins");
    }

    public boolean hasWhitelist() {
        return this.getConfigBoolean("white-list", false);
    }

    // NOTE: Temporary calls through to server.properies until its replaced
    private String getConfigString(String variable, String defaultValue) {
        return this.console.getPropertyManager().getProperty/*was:getString*/(variable, defaultValue);
    }

    private int getConfigInt(String variable, int defaultValue) {
        return this.console.getPropertyManager().getIntProperty/*was:getInt*/(variable, defaultValue);
    }

    private boolean getConfigBoolean(String variable, boolean defaultValue) {
        return this.console.getPropertyManager().getBooleanProperty/*was:getBoolean*/(variable, defaultValue);
    }

    // End Temporary calls

    public String getUpdateFolder() {
        return this.configuration.getString("settings.update-folder", "update");
    }

    public File getUpdateFolderFile() {
        return new File((File) console.options.valueOf("plugins"), this.configuration.getString("settings.update-folder", "update"));
    }

    public int getPingPacketLimit() {
        return this.configuration.getInt("settings.ping-packet-limit", 100);
    }

    public long getConnectionThrottle() {
        return this.configuration.getInt("settings.connection-throttle");
    }

    public int getTicksPerAnimalSpawns() {
        return this.configuration.getInt("ticks-per.animal-spawns");
    }

    public int getTicksPerMonsterSpawns() {
        return this.configuration.getInt("ticks-per.monster-spawns");
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public CraftScheduler getScheduler() {
        return scheduler;
    }

    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    public List<World> getWorlds() {
        return new ArrayList<World>(worlds.values());
    }

    public net.minecraft.server.dedicated.DedicatedPlayerList/*was:DedicatedPlayerList*/ getHandle() {
        return playerList;
    }

    // NOTE: Should only be called from DedicatedServer.ah()
    public boolean dispatchServerCommand(CommandSender sender, net.minecraft.command.ServerCommand/*was:ServerCommand*/ serverCommand) {
        if (sender instanceof Conversable) {
            Conversable conversable = (Conversable)sender;

            if (conversable.isConversing()) {
                conversable.acceptConversationInput(serverCommand.command/*was:command*/);
                return true;
            }
        }
        try {
            // MCPC+ start - handle bukkit/vanilla console commands
            int space = serverCommand.command.indexOf(" ");
            // if bukkit command exists then execute it over vanilla
            if (this.getCommandMap().getCommand(serverCommand.command.substring(0, space != -1 ? space : serverCommand.command.length())) != null)
            {
                return this.dispatchCommand(sender, serverCommand.command);
            }
            else { // process vanilla console command
                craftCommandMap.setVanillaConsoleSender(serverCommand.sender);
                return this.dispatchVanillaCommand(sender, serverCommand.command);
            }
            // MCPC+ end
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Unexpected exception while parsing console command \"" + serverCommand.command/*was:command*/ + '"', ex);
            return false;
        }
    }

    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        if (commandMap.dispatch(sender, commandLine)) {
            return true;
        }

        sender.sendMessage("Unknown command. Type \"help\" for help.");

        return false;
    }

    // MCPC+ start
    // used to process vanilla commands
    public boolean dispatchVanillaCommand(CommandSender sender, String commandLine) {
        if (craftCommandMap.dispatch(sender, commandLine)) {
            return true;
        }

        sender.sendMessage("Unknown command. Type \"help\" for help.");

        return false;
    }

    public boolean getConnectionLoggingEnabled() {
        return this.configuration.getBoolean("mcpc.connection-logging", false);
    }
    
    public boolean getInfiniteWaterSource() {
        return this.configuration.getBoolean("mcpc.infinite-water-source", true);
    }

    public boolean getDumpMaterials() {
        return this.configuration.getBoolean("mcpc.dump-materials", false);
    }
    // MCPC+ end

    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(getConfigFile());
        net.minecraft.server.dedicated.PropertyManager/*was:PropertyManager*/ config = new net.minecraft.server.dedicated.PropertyManager/*was:PropertyManager*/(console.options);

        ((net.minecraft.server.dedicated.DedicatedServer/*was:DedicatedServer*/) console).settings/*was:propertyManager*/ = config;

        ((SimplePluginManager) pluginManager).useTimings(configuration.getBoolean("settings.plugin-profiling")); // Spigot
        boolean animals = config.getBooleanProperty/*was:getBoolean*/("spawn-animals", console.getCanSpawnAnimals/*was:getSpawnAnimals*/());
        boolean monsters = config.getBooleanProperty/*was:getBoolean*/("spawn-monsters", console.worlds.get(0).difficultySetting/*was:difficulty*/ > 0);
        int difficulty = config.getIntProperty/*was:getInt*/("difficulty", console.worlds.get(0).difficultySetting/*was:difficulty*/);

        online.value = config.getBooleanProperty/*was:getBoolean*/("online-mode", console.isServerInOnlineMode/*was:getOnlineMode*/());
        console.setCanSpawnAnimals/*was:setSpawnAnimals*/(config.getBooleanProperty/*was:getBoolean*/("spawn-animals", console.getCanSpawnAnimals/*was:getSpawnAnimals*/()));
        console.setAllowPvp/*was:setPvP*/(config.getBooleanProperty/*was:getBoolean*/("pvp", console.isPVPEnabled/*was:getPvP*/()));
        console.setAllowFlight/*was:setAllowFlight*/(config.getBooleanProperty/*was:getBoolean*/("allow-flight", console.isFlightAllowed/*was:getAllowFlight*/()));
        console.setMOTD/*was:setMotd*/(config.getProperty/*was:getString*/("motd", console.getMOTD/*was:getMotd*/()));
        monsterSpawn = configuration.getInt("spawn-limits.monsters");
        animalSpawn = configuration.getInt("spawn-limits.animals");
        waterAnimalSpawn = configuration.getInt("spawn-limits.water-animals");
        ambientSpawn = configuration.getInt("spawn-limits.ambient");
        warningState = WarningState.value(configuration.getString("settings.deprecated-verbose"));
        console.autosavePeriod = configuration.getInt("ticks-per.autosave");
        chunkGCPeriod = configuration.getInt("chunk-gc.period-in-ticks");
        chunkGCLoadThresh = configuration.getInt("chunk-gc.load-threshold");

        playerList.getBannedIPs/*was:getIPBans*/().loadBanList/*was:load*/();
        playerList.getBannedPlayers/*was:getNameBans*/().loadBanList/*was:load*/();

        for (net.minecraft.world.WorldServer/*was:WorldServer*/ world : console.worlds) {
            world.difficultySetting/*was:difficulty*/ = difficulty;
            world.setAllowedSpawnTypes/*was:setSpawnFlags*/(monsters, animals);
            if (this.getTicksPerAnimalSpawns() < 0) {
                world.ticksPerAnimalSpawns = 400;
            } else {
                world.ticksPerAnimalSpawns = this.getTicksPerAnimalSpawns();
            }

            if (this.getTicksPerMonsterSpawns() < 0) {
                world.ticksPerMonsterSpawns = 1;
            } else {
                world.ticksPerMonsterSpawns = this.getTicksPerMonsterSpawns();
            }
        }

        pluginManager.clearPlugins();
        commandMap.clearCommands();
        resetRecipes();

        int pollCount = 0;

        // Wait for at most 2.5 seconds for plugins to close their threads
        while (pollCount < 50 && getScheduler().getActiveWorkers().size() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
            pollCount++;
        }

        List<BukkitWorker> overdueWorkers = getScheduler().getActiveWorkers();
        for (BukkitWorker worker : overdueWorkers) {
            Plugin plugin = worker.getOwner();
            String author = "<NoAuthorGiven>";
            if (plugin.getDescription().getAuthors().size() > 0) {
                author = plugin.getDescription().getAuthors().get(0);
            }
            getLogger().log(Level.SEVERE, String.format(
                "Nag author: '%s' of '%s' about the following: %s",
                author,
                plugin.getDescription().getName(),
                "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin"
            ));
        }
        Spigot.initialize(this, commandMap, configuration); // Spigot
        loadPlugins();
        enablePlugins(PluginLoadOrder.STARTUP);
        enablePlugins(PluginLoadOrder.POSTWORLD);
    }

    @SuppressWarnings({ "unchecked", "finally" })
    private void loadCustomPermissions() {
        File file = new File(configuration.getString("settings.permissions-file"));
        FileInputStream stream;

        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            try {
                file.createNewFile();
            } finally {
                return;
            }
        }

        Map<String, Map<String, Object>> perms;

        try {
            perms = (Map<String, Map<String, Object>>) yaml.load(stream);
        } catch (MarkedYAMLException ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML: " + ex.toString());
            return;
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML.", ex);
            return;
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {}
        }

        if (perms == null) {
            getLogger().log(Level.INFO, "Server permissions file " + file + " is empty, ignoring it");
            return;
        }

        List<Permission> permsList = Permission.loadPermissions(perms, "Permission node '%s' in " + file + " is invalid", Permission.DEFAULT_PERMISSION);

        for (Permission perm : permsList) {
            try {
                pluginManager.addPermission(perm);
            } catch (IllegalArgumentException ex) {
                getLogger().log(Level.SEVERE, "Permission in " + file + " was already defined", ex);
            }
        }
    }

    @Override
    public String toString() {
        return "CraftServer{" + "serverName=" + serverName + ",serverVersion=" + serverVersion + ",minecraftVersion=" + console.getMinecraftVersion/*was:getVersion*/() + '}';
    }

    public World createWorld(String name, World.Environment environment) {
        return WorldCreator.name(name).environment(environment).createWorld();
    }

    public World createWorld(String name, World.Environment environment, long seed) {
        return WorldCreator.name(name).environment(environment).seed(seed).createWorld();
    }

    public World createWorld(String name, Environment environment, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).generator(generator).createWorld();
    }

    public World createWorld(String name, Environment environment, long seed, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).seed(seed).generator(generator).createWorld();
    }

    public World createWorld(WorldCreator creator) {
        if (creator == null) {
            throw new IllegalArgumentException("Creator may not be null");
        }

        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(getWorldContainer(), name);
        World world = getWorld(name);
        net.minecraft.world.WorldType/*was:WorldType*/ type = net.minecraft.world.WorldType/*was:WorldType*/.parseWorldType/*was:getType*/(creator.type().getName());
        boolean generateStructures = creator.generateStructures();

        if (world != null) {
            return world;
        }

        if ((folder.exists()) && (!folder.isDirectory())) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        }

        if (generator == null) {
            generator = getGenerator(name);
        }

        net.minecraft.world.storage.ISaveFormat/*was:Convertable*/ converter = new net.minecraft.world.chunk.storage.AnvilSaveConverter/*was:WorldLoaderServer*/(getWorldContainer());
        if (converter.isOldMapFormat/*was:isConvertable*/(name)) {
            getLogger().info("Converting world '" + name + "'");
            converter.convertMapFormat/*was:convert*/(name, new net.minecraft.server.ConvertingProgressUpdate/*was:ConvertProgressUpdater*/(console));
        }

        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + console.worlds.size();
        boolean used = false;
        do {
            for (net.minecraft.world.WorldServer/*was:WorldServer*/ server : console.worlds) {
                used = server.dimension == dimension;
                if (used) {
                    dimension++;
                    break;
                }
            }
        } while(used);
        boolean hardcore = false;

        net.minecraft.world.WorldServer/*was:WorldServer*/ internal = new net.minecraft.world.WorldServer/*was:WorldServer*/(console, new net.minecraft.world.chunk.storage.AnvilSaveHandler/*was:ServerNBTManager*/(getWorldContainer(), name, true), name, dimension, new net.minecraft.world.WorldSettings/*was:WorldSettings*/(creator.seed(), net.minecraft.world.EnumGameType/*was:EnumGamemode*/.getByID/*was:a*/(getDefaultGameMode().getValue()), generateStructures, hardcore, type), console.theProfiler/*was:methodProfiler*/, creator.environment(), generator);
        DimensionManager.addMVDimension(dimension); // MCPC+ allows us to keep track of which dimensions belong to MV so we can avoid sending a Packet9Respawn.
        DimensionManager.registerDimension(dimension, internal.provider/*was:worldProvider*/.dimensionId/*was:dimension*/); // MCPC+ registers MultiVerse dimensions with forge
        if (!(worlds.containsKey(name.toLowerCase()))) {
            return null;
        }

        internal.mapStorage/*was:worldMaps*/ = console.worlds.get(0).mapStorage/*was:worldMaps*/;

        internal.theEntityTracker/*was:tracker*/ = new net.minecraft.entity.EntityTracker/*was:EntityTracker*/(internal); // CraftBukkit
        internal.addWorldAccess/*was:addIWorldAccess*/((net.minecraft.world.IWorldAccess/*was:IWorldAccess*/) new net.minecraft.world.WorldManager/*was:WorldManager*/(console, internal));
        internal.difficultySetting/*was:difficulty*/ = 1;
        internal.setAllowedSpawnTypes/*was:setSpawnFlags*/(true, true);
        console.worlds.add(internal);

        if (generator != null) {
            internal.getWorld().getPopulators().addAll(generator.getDefaultPopulators(internal.getWorld()));
        }

        pluginManager.callEvent(new WorldInitEvent(internal.getWorld()));
        System.out.print("Preparing start region for level " + (console.worlds.size() - 1) + " (Dimension: " + internal.dimension + ", Seed: " + internal.getSeed/*was:getSeed*/() + ")");

        if (internal.getWorld().getKeepSpawnInMemory()) {
            short short1 = 196;
            long i = System.currentTimeMillis();
            for (int j = -short1; j <= short1; j += 16) {
                for (int k = -short1; k <= short1; k += 16) {
                    long l = System.currentTimeMillis();

                    if (l < i) {
                        i = l;
                    }

                    if (l > i + 1000L) {
                        int i1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int j1 = (j + short1) * (short1 * 2 + 1) + k + 1;

                        System.out.println("Preparing spawn area for " + name + ", " + (j1 * 100 / i1) + "%");
                        i = l;
                    }

                    net.minecraft.util.ChunkCoordinates/*was:ChunkCoordinates*/ chunkcoordinates = internal.getSpawnPoint/*was:getSpawn*/();
                    internal.theChunkProviderServer/*was:chunkProviderServer*/.loadChunk/*was:getChunkAt*/(chunkcoordinates.posX/*was:x*/ + j >> 4, chunkcoordinates.posZ/*was:z*/ + k >> 4);
                }
            }
        }
        pluginManager.callEvent(new WorldLoadEvent(internal.getWorld()));
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(internal)); // Forge
        return internal.getWorld();
    }

    public boolean unloadWorld(String name, boolean save) {
        return unloadWorld(getWorld(name), save);
    }

    public boolean unloadWorld(World world, boolean save) {
        if (world == null) {
            return false;
        }

        net.minecraft.world.WorldServer/*was:WorldServer*/ handle = ((CraftWorld) world).getHandle();

        if (!(console.worlds.contains(handle))) {
            return false;
        }

        if (!(handle.dimension > 1)) {
            return false;
        }

        if (handle.playerEntities/*was:players*/.size() > 0) {
            return false;
        }

        WorldUnloadEvent e = new WorldUnloadEvent(handle.getWorld());
        pluginManager.callEvent(e);

        if (e.isCancelled()) {
            return false;
        }

        if (save) {
            try {
                handle.saveAllChunks/*was:save*/(true, (net.minecraft.util.IProgressUpdate/*was:IProgressUpdate*/) null);
                handle.flush/*was:saveLevel*/();
                WorldSaveEvent event = new WorldSaveEvent(handle.getWorld());
                getPluginManager().callEvent(event);
            } catch (net.minecraft.world.MinecraftException/*was:ExceptionWorldConflict*/ ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }

        // Forge start
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(handle));
        DimensionManager.setWorld(handle.dimension, null);
        DimensionManager.unregisterDimension(handle.dimension);
        // Forge end
        return true;
    }

    public net.minecraft.server.MinecraftServer/*was:MinecraftServer*/ getServer() {
        return console;
    }

    public World getWorld(String name) {
        return worlds.get(name.toLowerCase());
    }

    public World getWorld(UUID uid) {
        for (World world : worlds.values()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    public void addWorld(World world) {
        // Check if a World already exists with the UID.
        // MCPC+ disable warning for now
        /*if (getWorld(world.getUID()) != null) {
            System.out.println("World " + world.getName() + " is a duplicate of another world and has been prevented from loading. Please delete the uid.dat file from " + world.getName() + "'s world directory if you want to be able to load the duplicate world.");
            return;
        }*/
        worlds.put(world.getName().toLowerCase(), world);
    }

    public Logger getLogger() {
        return net.minecraft.server.MinecraftServer/*was:MinecraftServer*/.logger/*was:log*/;
    }

    public ConsoleReader getReader() {
        return console.reader;
    }

    public PluginCommand getPluginCommand(String name) {
        Command command = commandMap.getCommand(name);

        if (command instanceof PluginCommand) {
            return (PluginCommand) command;
        } else {
            return null;
        }
    }

    public void savePlayers() {
        playerList.saveAllPlayerData/*was:savePlayers*/();
    }

    public void configureDbConfig(ServerConfig config) {
        DataSourceConfig ds = new DataSourceConfig();
        ds.setDriver(configuration.getString("database.driver"));
        ds.setUrl(configuration.getString("database.url"));
        ds.setUsername(configuration.getString("database.username"));
        ds.setPassword(configuration.getString("database.password"));
        ds.setIsolationLevel(TransactionIsolation.getLevel(configuration.getString("database.isolation")));

        if (ds.getDriver().contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
        }

        config.setDataSourceConfig(ds);
    }

    public boolean addRecipe(Recipe recipe) {
        CraftRecipe toAdd;
        if (recipe instanceof CraftRecipe) {
            toAdd = (CraftRecipe) recipe;
        } else {
            if (recipe instanceof ShapedRecipe) {
                toAdd = CraftShapedRecipe.fromBukkitRecipe((ShapedRecipe) recipe);
            } else if (recipe instanceof ShapelessRecipe) {
                toAdd = CraftShapelessRecipe.fromBukkitRecipe((ShapelessRecipe) recipe);
            } else if (recipe instanceof FurnaceRecipe) {
                toAdd = CraftFurnaceRecipe.fromBukkitRecipe((FurnaceRecipe) recipe);
            } else {
                return false;
            }
        }
        toAdd.addToCraftingManager();
        //net.minecraft.item.crafting.CraftingManager/*was:CraftingManager*/.getInstance/*was:getInstance*/().sort(); // MCPC+ - mod recipes not necessarily sortable
        return true;
    }

    public List<Recipe> getRecipesFor(ItemStack result) {
        List<Recipe> results = new ArrayList<Recipe>();
        Iterator<Recipe> iter = recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();
            ItemStack stack = recipe.getResult();
            if (stack.getType() != result.getType()) {
                continue;
            }
            if (result.getDurability() == -1 || result.getDurability() == stack.getDurability()) {
                results.add(recipe);
            }
        }
        return results;
    }

    public Iterator<Recipe> recipeIterator() {
        return new RecipeIterator();
    }

    public void clearRecipes() {
        net.minecraft.item.crafting.CraftingManager/*was:CraftingManager*/.getInstance/*was:getInstance*/().recipes/*was:recipes*/.clear();
        net.minecraft.item.crafting.FurnaceRecipes/*was:RecipesFurnace*/.smelting/*was:getInstance*/().smeltingList/*was:recipes*/.clear();
    }

    public void resetRecipes() {
        net.minecraft.item.crafting.CraftingManager/*was:CraftingManager*/.getInstance/*was:getInstance*/().recipes/*was:recipes*/ = new net.minecraft.item.crafting.CraftingManager/*was:CraftingManager*/().recipes/*was:recipes*/;
        net.minecraft.item.crafting.FurnaceRecipes/*was:RecipesFurnace*/.smelting/*was:getInstance*/().smeltingList/*was:recipes*/ = new net.minecraft.item.crafting.FurnaceRecipes/*was:RecipesFurnace*/().smeltingList/*was:recipes*/;
    }

    public Map<String, String[]> getCommandAliases() {
        ConfigurationSection section = configuration.getConfigurationSection("aliases");
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> commands = null;

                if (section.isList(key)) {
                    commands = section.getStringList(key);
                } else {
                    commands = ImmutableList.<String>of(section.getString(key));
                }

                result.put(key, commands.toArray(new String[commands.size()]));
            }
        }

        return result;
    }

    public void removeBukkitSpawnRadius() {
        configuration.set("settings.spawn-radius", null);
        saveConfig();
    }

    public int getBukkitSpawnRadius() {
        return configuration.getInt("settings.spawn-radius", -1);
    }

    public String getShutdownMessage() {
        return configuration.getString("settings.shutdown-message");
    }

    public int getSpawnRadius() {
        return ((net.minecraft.server.dedicated.DedicatedServer/*was:DedicatedServer*/) console).settings/*was:propertyManager*/.getIntProperty/*was:getInt*/("spawn-protection", 16);
    }

    public void setSpawnRadius(int value) {
        configuration.set("settings.spawn-radius", value);
        saveConfig();
    }

    public boolean getOnlineMode() {
        return online.value;
    }

    public boolean getAllowFlight() {
        return console.isFlightAllowed/*was:getAllowFlight*/();
    }

    public boolean isHardcore() {
        return console.isHardcore/*was:isHardcore*/();
    }

    public boolean useExactLoginLocation() {
        return configuration.getBoolean("settings.use-exact-login-location");
    }

    public ChunkGenerator getGenerator(String world) {
        ConfigurationSection section = configuration.getConfigurationSection("worlds");
        ChunkGenerator result = null;

        if (section != null) {
            section = section.getConfigurationSection(world);

            if (section != null) {
                String name = section.getString("generator");

                if ((name != null) && (!name.equals(""))) {
                    String[] split = name.split(":", 2);
                    String id = (split.length > 1) ? split[1] : null;
                    Plugin plugin = pluginManager.getPlugin(split[0]);

                    if (plugin == null) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + split[0] + "' does not exist");
                    } else if (!plugin.isEnabled()) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + split[0] + "' is not enabled yet (is it load:STARTUP?)");
                    } else {
                        result = plugin.getDefaultWorldGenerator(world, id);
                    }
                }
            }
        }

        return result;
    }

    public CraftMapView getMap(short id) {
        net.minecraft.world.storage.MapStorage/*was:WorldMapCollection*/ collection = console.worlds.get(0).mapStorage/*was:worldMaps*/;
        net.minecraft.world.storage.MapData/*was:WorldMap*/ worldmap = (net.minecraft.world.storage.MapData/*was:WorldMap*/) collection.loadData/*was:get*/(net.minecraft.world.storage.MapData/*was:WorldMap*/.class, "map_" + id);
        if (worldmap == null) {
            return null;
        }
        return worldmap.mapView;
    }

    public CraftMapView createMap(World world) {
        /*was:net.minecraft.server.*/net.minecraft.item.ItemStack/*was:ItemStack*/ stack = new net.minecraft.item.ItemStack/*was:ItemStack*/(net.minecraft.item.Item/*was:Item*/.map/*was:MAP*/, 1, -1);
        net.minecraft.world.storage.MapData/*was:WorldMap*/ worldmap = net.minecraft.item.Item/*was:Item*/.map/*was:MAP*/.getMapData/*was:getSavedMap*/(stack, ((CraftWorld) world).getHandle());
        return worldmap.mapView;
    }

    public void shutdown() {
        console.initiateShutdown/*was:safeShutdown*/();
    }

    public int broadcast(String message, String permission) {
        int count = 0;
        Set<Permissible> permissibles = getPluginManager().getPermissionSubscriptions(permission);

        for (Permissible permissible : permissibles) {
            if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                CommandSender user = (CommandSender) permissible;
                user.sendMessage(message);
                count++;
            }
        }

        return count;
    }

    // Spigot start
    public OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer result = getPlayerExact(name);
        String lname = name.toLowerCase();

        if (result == null) {
            result = offlinePlayers.get(lname);

            if (result == null) {
                // Spigot end
                result = new CraftOfflinePlayer(this, name);
                offlinePlayers.put(lname, result);
            }
        } else {
            offlinePlayers.remove(lname);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getIPBans() {
        return playerList.getBannedIPs/*was:getIPBans*/().getBannedList/*was:getEntries*/().keySet();
    }

    public void banIP(String address) {
        net.minecraft.server.management.BanEntry/*was:BanEntry*/ entry = new net.minecraft.server.management.BanEntry/*was:BanEntry*/(address);
        playerList.getBannedIPs/*was:getIPBans*/().put/*was:add*/(entry);
        playerList.getBannedIPs/*was:getIPBans*/().saveToFileWithHeader/*was:save*/();
    }

    public void unbanIP(String address) {
        playerList.getBannedIPs/*was:getIPBans*/().remove/*was:remove*/(address);
        playerList.getBannedIPs/*was:getIPBans*/().saveToFileWithHeader/*was:save*/();
    }

    public Set<OfflinePlayer> getBannedPlayers() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (Object name : playerList.getBannedPlayers/*was:getNameBans*/().getBannedList/*was:getEntries*/().keySet()) {
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    public void setWhitelist(boolean value) {
        playerList.whiteListEnforced/*was:hasWhitelist*/ = value;
        console.getPropertyManager().setProperty/*was:a*/("white-list", value);
    }

    public Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> result = new LinkedHashSet<OfflinePlayer>();

        for (Object name : playerList.getWhiteListedPlayers/*was:getWhitelisted*/()) {
            if (((String)name).length() == 0 || ((String)name).startsWith("#")) {
                continue;
            }
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    public Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (Object name : playerList.getOps/*was:getOPs*/()) {
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    public void reloadWhitelist() {
        playerList.loadWhiteList/*was:reloadWhitelist*/();
    }

    public GameMode getDefaultGameMode() {
        return GameMode.getByValue(console.worlds.get(0).getWorldInfo/*was:getWorldData*/().getGameType/*was:getGameType*/().getID/*was:a*/());
    }

    public void setDefaultGameMode(GameMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        for (World world : getWorlds()) {
            ((CraftWorld) world).getHandle().worldInfo/*was:worldData*/.setGameType/*was:setGameType*/(net.minecraft.world.EnumGameType/*was:EnumGamemode*/.getByID/*was:a*/(mode.getValue()));
        }
    }

    public ConsoleCommandSender getConsoleSender() {
        return console.console;
    }

    public EntityMetadataStore getEntityMetadata() {
        return entityMetadata;
    }

    public PlayerMetadataStore getPlayerMetadata() {
        return playerMetadata;
    }

    public WorldMetadataStore getWorldMetadata() {
        return worldMetadata;
    }

    public void detectListNameConflict(net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entityPlayer) {
        // Collisions will make for invisible people
        for (int i = 0; i < getHandle().playerEntityList/*was:players*/.size(); ++i) {
            net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ testEntityPlayer = (net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) getHandle().playerEntityList/*was:players*/.get(i);

            // We have a problem!
            if (testEntityPlayer != entityPlayer && testEntityPlayer.listName.equals(entityPlayer.listName)) {
                String oldName = entityPlayer.listName;
                int spaceLeft = 16 - oldName.length();

                if (spaceLeft <= 1) { // We also hit the list name length limit!
                    entityPlayer.listName = oldName.subSequence(0, oldName.length() - 2 - spaceLeft) + String.valueOf(System.currentTimeMillis() % 99);
                } else {
                    entityPlayer.listName = oldName + String.valueOf(System.currentTimeMillis() % 99);
                }

                return;
            }
        }
    }

    public File getWorldContainer() {
        if (this.getServer().anvilFile/*was:universe*/ != null) {
            return this.getServer().anvilFile/*was:universe*/;
        }

        if (container == null) {
            container = new File(configuration.getString("settings.world-container", "."));
        }

        return container;
    }

    public OfflinePlayer[] getOfflinePlayers() {
        net.minecraft.world.storage.SaveHandler/*was:WorldNBTStorage*/ storage = (net.minecraft.world.storage.SaveHandler/*was:WorldNBTStorage*/) console.worlds.get(0).getSaveHandler/*was:getDataManager*/();
        String[] files = storage.getPlayerDir().list(new DatFileFilter());
        Set<OfflinePlayer> players = new HashSet<OfflinePlayer>();

        for (String file : files) {
            players.add(getOfflinePlayer(file.substring(0, file.length() - 4))); // Spigot
        }
        players.addAll(Arrays.asList(getOnlinePlayers()));

        return players.toArray(new OfflinePlayer[players.size()]);
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(getMessenger(), source, channel, message);

        for (Player player : getOnlinePlayers()) {
            player.sendPluginMessage(source, channel, message);
        }
    }

    public Set<String> getListeningPluginChannels() {
        Set<String> result = new HashSet<String>();

        for (Player player : getOnlinePlayers()) {
            result.addAll(player.getListeningPluginChannels());
        }

        return result;
    }

    public void onPlayerJoin(Player player) {
        if ((updater.isEnabled()) && (updater.getCurrent() != null) && (player.hasPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE))) {
            if ((updater.getCurrent().isBroken()) && (updater.getOnBroken().contains(updater.WARN_OPERATORS))) {
                player.sendMessage(ChatColor.DARK_RED + "The version of CraftBukkit that this server is running is known to be broken. Please consider updating to the latest version at dl.bukkit.org.");
            } else if ((updater.isUpdateAvailable()) && (updater.getOnUpdate().contains(updater.WARN_OPERATORS))) {
                player.sendMessage(ChatColor.DARK_PURPLE + "The version of CraftBukkit that this server is running is out of date. Please consider updating to the latest version at dl.bukkit.org.");
            }
        }
    }

    public Inventory createInventory(InventoryHolder owner, InventoryType type) {
        // TODO: Create the appropriate type, rather than Custom?
        return new CraftInventoryCustom(owner, type);
    }

    public Inventory createInventory(InventoryHolder owner, int size) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size);
    }

    public Inventory createInventory(InventoryHolder owner, int size, String title) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size, title);
    }

    public HelpMap getHelpMap() {
        return helpMap;
    }

    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }
    // MCPC+ start
    public CraftSimpleCommandMap getCraftCommandMap() {
        return craftCommandMap;
    }
    // MCPC+ end
    public int getMonsterSpawnLimit() {
        return monsterSpawn;
    }

    public int getAnimalSpawnLimit() {
        return animalSpawn;
    }

    public int getWaterAnimalSpawnLimit() {
        return waterAnimalSpawn;
    }

    public int getAmbientSpawnLimit() {
        return ambientSpawn;
    }

    public boolean isPrimaryThread() {
        return Thread.currentThread().equals(console.primaryThread);
    }

    public String getMotd() {
        return console.getMOTD/*was:getMotd*/();
    }

    public WarningState getWarningState() {
        return warningState;
    }

    public List<String> tabComplete(/*was:net.minecraft.server.*/net.minecraft.command.ICommandSender/*was:ICommandListener*/ sender, String message) {
        if (!(sender instanceof net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/)) {
            return ImmutableList.of();
        }

        Player player = ((net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) sender).getBukkitEntity();
        if (message.startsWith("/")) {
            return tabCompleteCommand(player, message);
        } else {
            return tabCompleteChat(player, message);
        }
    }

    public List<String> tabCompleteCommand(Player player, String message) {
        List<String> completions = null;
        try {
            completions = (commandComplete) ? getCommandMap().tabComplete(player, message.substring(1)) : null; // Spigot
        } catch (CommandException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to tab-complete this command");
            getLogger().log(Level.SEVERE, "Exception when " + player.getName() + " attempted to tab complete " + message, ex);
        }

        return completions == null ? ImmutableList.<String>of() : completions;
    }

    public List<String> tabCompleteChat(Player player, String message) {
        Player[] players = getOnlinePlayers();
        List<String> completions = new ArrayList<String>();
        PlayerChatTabCompleteEvent event = new PlayerChatTabCompleteEvent(player, message, completions);
        String token = event.getLastToken();
        for (Player p : players) {
            if (player.canSee(p) && StringUtil.startsWithIgnoreCase(p.getName(), token)) {
                completions.add(p.getName());
            }
        }
        pluginManager.callEvent(event);

        Iterator<?> it = completions.iterator();
        while (it.hasNext()) {
            Object current = it.next();
            if (!(current instanceof String)) {
                // Sanity
                it.remove();
            }
        }
        Collections.sort(completions, String.CASE_INSENSITIVE_ORDER);
        return completions;
    }

    public CraftItemFactory getItemFactory() {
        return CraftItemFactory.instance();
    }

    // Spigot start
    public void restart() {
        try {
            String startupScript = configuration.getString("settings.restart-script-location", "");
            File file = new File(startupScript);
            if (file.isFile()) {
                System.out.println("Attempting to restart with " + startupScript);

                // Kick all players
                for (Player p : this.getOnlinePlayers()) {
                   ((org.bukkit.craftbukkit.entity.CraftPlayer) p).kickPlayer("Server is restarting", true);
                }
                // Give the socket a chance to send the packets
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                // Close the socket so we can rebind with the new process
                this.getServer().getNetworkThread/*was:ae*/().stopListening/*was:a*/();

                // Give time for it to kick in
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }

                // Actually shutdown
                try {
                    this.getServer().stopServer/*was:stop*/();
                } catch (Throwable t) {
                }

                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("cmd /c start " + file.getPath());
                } else {
                    Runtime.getRuntime().exec(file.getPath());
                }
                System.exit(0);
            } else {
                System.out.println("Startup script '" + startupScript + "' does not exist!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // Spigot end
}
