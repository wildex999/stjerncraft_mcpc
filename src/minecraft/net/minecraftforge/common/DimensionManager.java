package net.minecraftforge.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.event.world.WorldEvent;
// MCPC+ start
import java.io.IOException;

import cpw.mods.fml.common.Loader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.Main;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
// MCPC+ end

public class DimensionManager
{
    private static Hashtable<Integer, Class<? extends WorldProvider>> providers = new Hashtable<Integer, Class<? extends WorldProvider>>();
    private static Hashtable<Integer, Boolean> spawnSettings = new Hashtable<Integer, Boolean>();
    private static Hashtable<Integer, WorldServer> worlds = new Hashtable<Integer, WorldServer>();
    private static boolean hasInit = false;
    private static Hashtable<Integer, Integer> dimensions = new Hashtable<Integer, Integer>();
    private static ArrayList<Integer> unloadQueue = new ArrayList<Integer>();
    private static BitSet dimensionMap = new BitSet(Long.SIZE << 4);
    private static ConcurrentMap<World, World> weakWorldMap = new MapMaker().weakKeys().weakValues().<World,World>makeMap();
    private static Multiset<Integer> leakedWorlds = HashMultiset.create();
    // MCPC+ start
    private static Hashtable<Class<? extends WorldProvider>, Integer> classToProviders = new Hashtable<Class<? extends WorldProvider>, Integer>();
    private static ArrayList<Integer> bukkitDims = new ArrayList<Integer>(); // used to keep track of Bukkit dimensions
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static Hashtable<String, Integer> bukkitAliasMap = new Hashtable<String, Integer>();
    private static org.bukkit.configuration.file.YamlConfiguration configuration = Main.configuration;
    // MCPC+ end

    public static boolean registerProviderType(int id, Class<? extends WorldProvider> provider, boolean keepLoaded)
    {
        if (providers.containsKey(id))
        {
            return false;
        }
        // MCPC+ start - register provider with bukkit and add appropriate config option
        String worldType = "unknown";
        if (id != -1 && id != 0 && id != 1) // ignore vanilla
        {
            worldType = provider.getSimpleName().toLowerCase();
            worldType = worldType.replace("worldprovider", "");
            worldType = worldType.replace("provider", "");
            registerBukkitEnvironment(id, worldType);
        }
        else
        {
            worldType = Environment.getEnvironment(id).name().toLowerCase();
        }
        if (!configuration.isBoolean("world-settings.default.keeploaded-environment-" + worldType))
            configuration.set("world-settings.default.keeploaded-environment-" + worldType, keepLoaded);
        keepLoaded = configuration.getBoolean("world-settings.default.keeploaded-environment-" + worldType);
        try {
            configuration.save(Main.configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // MCPC+ end
        providers.put(id, provider);
        classToProviders.put(provider, id);
        spawnSettings.put(id, keepLoaded);
        return true;
    }

    /**
     * Unregisters a Provider type, and returns a array of all dimensions that are 
     * registered to this provider type.
     * If the return size is greater then 0, it is required that the caller either 
     * change those dimensions's registered type, or replace this type before the 
     * world is attempted to load, else the loader will throw an exception.
     * 
     * @param id The provider type ID to unreigster
     * @return An array containing all dimension IDs still registered to this provider type.
     */
    public static int[] unregisterProviderType(int id)
    {
        if (!providers.containsKey(id))
        {
            return new int[0];
        }
        providers.remove(id);
        spawnSettings.remove(id);

        int[] ret = new int[dimensions.size()];
        int x = 0;
        for (Map.Entry<Integer, Integer> ent : dimensions.entrySet())
        {
            if (ent.getValue() == id)
            {
                ret[x++] = ent.getKey();
            }
        }

        return Arrays.copyOf(ret, x);
    }

    public static void init()
    {
        if (hasInit)
        {
            return;
        }

        hasInit = true;

        registerProviderType( 0, WorldProviderSurface.class, true);
        registerProviderType(-1, WorldProviderHell.class,    true);
        registerProviderType( 1, WorldProviderEnd.class,     false);
        registerDimension( 0,  0);
        registerDimension(-1, -1);
        registerDimension( 1,  1);
    }

    public static void registerDimension(int id, int providerType)
    {
        if (!providers.containsKey(providerType))
        {
            throw new IllegalArgumentException(String.format("Failed to register dimension for id %d, provider type %d does not exist", id, providerType));
        }
        // MCPC+ start - avoid throwing an exception to support Mystcraft.
        if (dimensions.containsKey(id))
        {
            FMLLog.warning(String.format("Failed to register dimension for id %d, One is already registered", id));
        }
        else 
        {
            dimensions.put(id, providerType);
            if (id >= 0)
            {
                dimensionMap.set(id);

                // MCPC+ start - set up map for world name to dimension
                if (id > 1) {
                    WorldProvider provider = WorldProvider.getProviderForDimension(id);
                    String name = provider.getSaveFolder();
                    bukkitAliasMap.put(name.toLowerCase(), id);
                }
                // MCPC+ end
            }
        }
        // MCPC+ end
    }

    /**
     * For unregistering a dimension when the save is changed (disconnected from a server or loaded a new save
     */
    public static void unregisterDimension(int id)
    {
        if (!dimensions.containsKey(id))
        {
            throw new IllegalArgumentException(String.format("Failed to unregister dimension for id %d; No provider registered", id));
        }
        dimensions.remove(id);
    }

    public static boolean isDimensionRegistered(int dim)
    {
        return dimensions.containsKey(dim);
    }

    public static int getProviderType(int dim)
    {
        if (!dimensions.containsKey(dim))
        {
            throw new IllegalArgumentException(String.format("Could not get provider type for dimension %d, does not exist", dim));
        }
        return dimensions.get(dim);
    }

    public static WorldProvider getProvider(int dim)
    {
        return getWorld(dim).provider;
    }

    public static Integer[] getIDs(boolean check)
    {
        // MCPC+ start - check config option and only log world leak messages if enabled
        if (MinecraftServer.getServer().server.getWorldLeakDebug())
        {
            if (check)
            {
                List<World> allWorlds = Lists.newArrayList(weakWorldMap.keySet());
                allWorlds.removeAll(worlds.values());
                for (ListIterator<World> li = allWorlds.listIterator(); li.hasNext(); )
                {
                    World w = li.next();
                    leakedWorlds.add(System.identityHashCode(w));
                }
                for (World w : allWorlds)
                {
                    int leakCount = leakedWorlds.count(System.identityHashCode(w));
                    if (leakCount == 5)
                    {
                        FMLLog.fine("The world %x (%s) may have leaked: first encounter (5 occurences). Note: This may be a caused by a mod, plugin, or just a false-positive(No memory leak). If server crashes due to OOM, report to MCPC.\n", System.identityHashCode(w), w.getWorldInfo().getWorldName());
                    }
                    else if (leakCount % 5 == 0)
                    {
                        FMLLog.fine("The world %x (%s) may have leaked: seen %d times. Note: This may be a caused by a mod, plugin, or just a false-positive(No memory leak). If server crashes due to OOM, report to MCPC.\n", System.identityHashCode(w), w.getWorldInfo().getWorldName(), leakCount);
                    }
                }
            }
        }
        // MCPC+ end
        return getIDs();
    }
    public static Integer[] getIDs()
    {
        return worlds.keySet().toArray(new Integer[worlds.size()]); //Only loaded dims, since usually used to cycle through loaded worlds
    }

    public static void setWorld(int id, WorldServer world)
    {
        if (world != null) {
            worlds.put(id, world);
            // MCPC+ start - check config option and only log world leak messages if enabled
            if (MinecraftServer.getServer().server.getWorldLeakDebug())
            {
                weakWorldMap.put(world, world);
            }
            // MCPC+ end
            MinecraftServer.getServer().worldTickTimes.put(id, new long[100]);
            FMLLog.info("Loading dimension %d (%s) (%s)", id, world.getWorldInfo().getWorldName(), world.getMinecraftServer());
        } else {
            MinecraftServer.getServer().worlds.remove(getWorld(id)); // MCPC+ - remove world from our new world arraylist
            worlds.remove(id);
            MinecraftServer.getServer().worldTickTimes.remove(id);
            FMLLog.info("Unloading dimension %d", id);
        }

        ArrayList<WorldServer> tmp = new ArrayList<WorldServer>();
        if (worlds.get( 0) != null)
            tmp.add(worlds.get( 0));
        if (worlds.get(-1) != null)
        {
            tmp.add(worlds.get(-1));
        }
        if (worlds.get( 1) != null)
        {
            tmp.add(worlds.get( 1));
        }

        for (Entry<Integer, WorldServer> entry : worlds.entrySet())
        {
            int dim = entry.getKey();
            if (dim >= -1 && dim <= 1)
            {
                continue;
            }
            tmp.add(entry.getValue());
            // MCPC+ start - add world if it does not exist
            if (!MinecraftServer.getServer().worlds.contains(entry.getValue()))
            {
                MinecraftServer.getServer().worlds.add(entry.getValue());
            }
            // MCPC+ end
        }

        MinecraftServer.getServer().worldServers = tmp.toArray(new WorldServer[tmp.size()]);
    }

    public static void initDimension(int dim) {
        if (dim == 0) return; // MCPC+ - overworld
        WorldServer overworld = getWorld(0);
        if (overworld == null) {
            throw new RuntimeException("Cannot Hotload Dim: Overworld is not Loaded!");
        }
        try {
            // MCPC+ start - Fixes MultiVerse issue when mods such as Twilight Forest try to hotload their dimension when using its WorldProvider
            if(overworld.getMinecraftServer().server.craftWorldLoading)
            {
                return;
            }
            // MCPC+ end
            DimensionManager.getProviderType(dim);
        } catch (Exception e) {
            System.err.println("Cannot Hotload Dim: " + e.getMessage());
            return; //If a provider hasn't been registered then we can't hotload the dim
        }

        MinecraftServer mcServer = overworld.getMinecraftServer();
        WorldSettings worldSettings = new WorldSettings(overworld.getWorldInfo());

        // MCPC+ start - handles hotloading dimensions
        String worldType;
        String name;
        String oldName = "";
        Environment env = Environment.getEnvironment(getProviderType(dim));
        if (dim >= -1 && dim <= 1)
        {
            if ((dim == -1 && !mcServer.getAllowNether()) || (dim == 1 && !mcServer.server.getAllowEnd()))
                return;
            worldType = env.toString().toLowerCase();
            name = "DIM" + dim;
        }
        else
        {
            WorldProvider provider = WorldProvider.getProviderForDimension(dim);
            worldType = provider.getClass().getSimpleName().toLowerCase();
            worldType = worldType.replace("worldprovider", "");
            oldName = "world_" + worldType;
            worldType = worldType.replace("provider", "");

            if (Environment.getEnvironment(DimensionManager.getProviderType(dim)) == null)
                    env = DimensionManager.registerBukkitEnvironment(DimensionManager.getProviderType(provider.getClass()), worldType);

            name = provider.getSaveFolder();
        }

        mcServer.migrateWorlds(worldType, oldName, overworld.getWorldInfo().getWorldName(), name); // MCPC+
        ChunkGenerator gen = mcServer.server.getGenerator(name);
        if (mcServer instanceof DedicatedServer) {
            worldSettings.func_82750_a(((DedicatedServer) mcServer).getStringProperty("generator-settings", ""));
        }
        WorldServer world = new WorldServerMulti(mcServer, new AnvilSaveHandler(mcServer.server.getWorldContainer(), name, true), name, dim, worldSettings, overworld, mcServer.theProfiler, overworld.getWorldLogAgent(), env, gen);

        if (gen != null)
        {
            world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
        }
        //mcServer.worlds.add(world); world is added to this collection in setWorld. since  it is a list and not a set, it will be added twice
        mcServer.getConfigurationManager().setPlayerManager(mcServer.worlds.toArray(new WorldServer[mcServer.worlds.size()]));
        // MCPC+ end
        world.addWorldAccess(new WorldManager(mcServer, world));
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
        mcServer.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(world.getWorld()));
        if (!mcServer.isSinglePlayer())
        {
            world.getWorldInfo().setGameType(mcServer.getGameType());
        }

        mcServer.setDifficultyForAllWorlds(mcServer.getDifficulty());
    }

    // MCPC+ start - new method for handling creation of Bukkit dimensions. Currently supports MultiVerse
    public static WorldServer initDimension(WorldCreator creator, WorldSettings worldSettings) {
        WorldServer overworld = getWorld(0);
        if (overworld == null) {
            throw new RuntimeException("Cannot Hotload Dim: Overworld is not Loaded!");
        }

        MinecraftServer mcServer = overworld.getMinecraftServer();

        String worldType;
        String name;
        int dim = DimensionManager.getNextFreeDimId();
        if (bukkitAliasMap.containsKey(creator.name().toLowerCase()))
        {
            dim = bukkitAliasMap.get(creator.name().toLowerCase());
        }
        else { 
            bukkitAliasMap.put(creator.name().toLowerCase(), dim);
        }

        int providerId = 0;
        if (creator.environment() != null)
            providerId = creator.environment().getId();
        try {
            providerId = getProviderType(providerId);
        }
        catch (IllegalArgumentException e)
        {
            // do nothing
        }

        registerDimension(dim, providerId);
        addBukkitDimension(dim);
        Environment env = creator.environment();
        worldType = env.name().toLowerCase();
        name = creator.name();
        ChunkGenerator gen = creator.generator();

        if (mcServer instanceof DedicatedServer) {
            worldSettings.func_82750_a(((DedicatedServer) mcServer).getStringProperty("generator-settings", ""));
        }

        WorldServer world = new WorldServerMulti(mcServer, new AnvilSaveHandler(mcServer.server.getWorldContainer(), name, true), name, dim, worldSettings, overworld, mcServer.theProfiler, overworld.getWorldLogAgent(), env, gen);

        if (gen != null)
        {
            world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
        }
        //mcServer.worlds.add(world); world is added to this collection in setWorld. since  it is a list and not a set, it will be added twice
        mcServer.getConfigurationManager().setPlayerManager(mcServer.worlds.toArray(new WorldServer[mcServer.worlds.size()]));
        // MCPC+ end
        world.addWorldAccess(new WorldManager(mcServer, world));
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
        if (!mcServer.isSinglePlayer())
        {
            world.getWorldInfo().setGameType(mcServer.getGameType());
        }

        mcServer.setDifficultyForAllWorlds(mcServer.getDifficulty());

        return world;
    }

    public static WorldServer getWorld(int id)
    {
        return worlds.get(id);
    }

    public static WorldServer[] getWorlds()
    {
        return worlds.values().toArray(new WorldServer[worlds.size()]);
    }

    public static boolean shouldLoadSpawn(int dim)
    {
        int id = getProviderType(dim);
        return spawnSettings.containsKey(id) && spawnSettings.get(id);
    }

    static
    {
        init();
    }

    /**
     * Not public API: used internally to get dimensions that should load at
     * server startup
     */
    public static Integer[] getStaticDimensionIDs()
    {
        return dimensions.keySet().toArray(new Integer[dimensions.keySet().size()]);
    }
    public static WorldProvider createProviderFor(int dim)
    {
        try
        {
            if (dimensions.containsKey(dim))
            {
                WorldProvider provider = providers.get(getProviderType(dim)).newInstance();
                provider.setDimension(dim);
                return provider;
            }
            else
            {
                throw new RuntimeException(String.format("No WorldProvider bound for dimension %d", dim)); //It's going to crash anyway at this point.  Might as well be informative
            }
        }
        catch (Exception e)
        {
            FMLCommonHandler.instance().getFMLLogger().log(Level.SEVERE,String.format("An error occured trying to create an instance of WorldProvider %d (%s)",
                    dim, providers.get(getProviderType(dim)).getSimpleName()),e);
            throw new RuntimeException(e);
        }
    }

    public static void unloadWorld(int id) {
        if (!shouldLoadSpawn(id)) // MCPC+ - prevent mods from force unloading if we have it disabled
            unloadQueue.add(id);
    }

    /*
    * To be called by the server at the appropriate time, do not call from mod code.
    */
    public static void unloadWorlds(Hashtable<Integer, long[]> worldTickTimes) {
        for (int id : unloadQueue) {
            WorldServer w = worlds.get(id);
            if (w != null)
            {
                MinecraftServer.getServer().server.unloadWorld(w.getWorld(), true); // MCPC+ - unload through our new method for simplicity
            }
        }
        unloadQueue.clear();
    }

    /**
     * Return the next free dimension ID. Note: you are not guaranteed a contiguous
     * block of free ids. Always call for each individual ID you wish to get.
     * @return the next free dimension ID
     */
    public static int getNextFreeDimId() {
        int next = 0;
        while (true)
        {
            next = dimensionMap.nextClearBit(next);
            if (dimensions.containsKey(next))
            {
                dimensionMap.set(next);
            }
            else
            {
                return next;
            }
        }
    }

    public static NBTTagCompound saveDimensionDataMap()
    {
        int[] data = new int[(dimensionMap.length() + Integer.SIZE - 1 )/ Integer.SIZE];
        NBTTagCompound dimMap = new NBTTagCompound();
        for (int i = 0; i < data.length; i++)
        {
            int val = 0;
            for (int j = 0; j < Integer.SIZE; j++)
            {
                val |= dimensionMap.get(i * Integer.SIZE + j) ? (1 << j) : 0;
            }
            data[i] = val;
        }
        dimMap.setIntArray("DimensionArray", data);
        return dimMap;
    }

    public static void loadDimensionDataMap(NBTTagCompound compoundTag)
    {
        if (compoundTag == null)
        {
            dimensionMap.clear();
            for (Integer id : dimensions.keySet())
            {
                if (id >= 0)
                {
                    dimensionMap.set(id);
                }
            }
        }
        else
        {
            int[] intArray = compoundTag.getIntArray("DimensionArray");
            for (int i = 0; i < intArray.length; i++)
            {
                for (int j = 0; j < Integer.SIZE; j++)
                {
                    dimensionMap.set(i * Integer.SIZE + j, (intArray[i] & (1 << j)) != 0);
                }
            }
        }
    }

    /**
     * Return the current root directory for the world save. Accesses getSaveHandler from the overworld
     * @return the root directory of the save
     */
    public static File getCurrentSaveRootDirectory()
    {
        if (DimensionManager.getWorld(0) != null)
        {
            return ((SaveHandler)DimensionManager.getWorld(0).getSaveHandler()).getWorldDirectory();
        }
        else if (MinecraftServer.getServer() != null)
        {
            MinecraftServer srv = MinecraftServer.getServer();
            SaveHandler saveHandler = (SaveHandler) srv.getActiveAnvilConverter().getSaveLoader(srv.getFolderName(), false);
            return saveHandler.getWorldDirectory();
        }
        else
        {
            return null;
        }
    }

    // MCPC+ start - add registration for Bukkit Environments
    public static Environment registerBukkitEnvironment(int dim, String providerName)
    {
        Environment env = Environment.getEnvironment(dim);
        if (env == null) // MCPC+  if environment not found, register one
        {
            providerName = providerName.replace("WorldProvider", "");
            env = EnumHelper.addBukkitEnvironment(dim, providerName.toUpperCase());
            Environment.registerEnvironment(env);
        }
        return env;
    }

    public static int getProviderType(Class<? extends WorldProvider> provider)
    {
        return classToProviders.get(provider);
    }

    public static void addBukkitDimension(int dim)
    {
        if (!bukkitDims.contains(dim))
            bukkitDims.add(dim);
    }

    public static void removeBukkitDimension(int dim)
    {
        if (bukkitDims.contains(dim))
            bukkitDims.remove(bukkitDims.indexOf(dim));
    }

    public static ArrayList<Integer> getBukkitDimensionIDs()
    {
        return bukkitDims;
    }

    public static boolean isBukkitDimension(int dim)
    {
        return bukkitDims.contains(dim);
    }

    public static void addBukkitAlias(String name, int dim)
    {
        bukkitAliasMap.put(name.toLowerCase(), dim);
    }

    public static Integer getBukkitAliasDim(String name) {
        return bukkitAliasMap.get(name.toLowerCase());
    }
    // MCPC+ end
}
