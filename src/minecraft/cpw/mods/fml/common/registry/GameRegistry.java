/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.common.registry;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
// MCPC+ start
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.Main;

import net.minecraft.entity.player.EntityPlayerMP;
// MCPC+ end

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.IPickupNotifier;
import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.Mod.Block;
import cpw.mods.fml.common.ModContainer;

public class GameRegistry
{
    private static Multimap<ModContainer, BlockProxy> blockRegistry = ArrayListMultimap.create();
    private static Set<IWorldGenerator> worldGenerators = Sets.newHashSet();
    private static List<IFuelHandler> fuelHandlers = Lists.newArrayList();
    private static List<ICraftingHandler> craftingHandlers = Lists.newArrayList();
    private static List<IPickupNotifier> pickupHandlers = Lists.newArrayList();
    private static List<IPlayerTracker> playerTrackers = Lists.newArrayList();
    private static org.bukkit.configuration.file.YamlConfiguration configuration = Main.configuration; // MCPC+
    private static Map<Integer, BannedBlock> bannedItemCache = new HashMap(); 
    /**
     * Register a world generator - something that inserts new block types into the world
     *
     * @param generator
     */
    static 
    {
        // init banned items
        if (configuration.getBoolean("mcpc.enable-banned-items"))
        {
            for (String bannedData : configuration.getStringList("mcpc.banned-item-IDs")) {
                int seperator = bannedData.indexOf(':');
                if (seperator > 0 && (bannedData.length() - 1) > seperator)
                {
                    int id = Integer.parseInt(bannedData.substring(0, seperator));
                    int meta = Integer.parseInt(bannedData.substring(seperator + 1, bannedData.length()));
                    bannedItemCache.put(id, new BannedBlock(id, meta));
                    FMLLog.info("Banning" + " item ID " +id);
                }
                else
                {
                    int id = Integer.parseInt(bannedData);
                    bannedItemCache.put(id, new BannedBlock(id, -1));
                    FMLLog.info("Banning" + " item ID " +id);
                }
            }
        }
    }

    public static void registerWorldGenerator(IWorldGenerator generator)
    {
        // MCPC+ start - add config options to enable/disable mod world generators
        String modId = Loader.instance().activeModContainer().getModId();
        modId = modId.replaceAll("[^A-Za-z0-9]", ""); // remove all non-digits/alphanumeric
        modId.replace(" ", "_");
        String generatorName = modId + "-" + generator.getClass().getSimpleName();
        if (!configuration.isBoolean("world-settings.default.worldgen-" + generatorName))
                configuration.set("world-settings.default.worldgen-" + generatorName, true);
        boolean generatorEnabled = configuration.getBoolean("world-settings.default.worldgen-" + generatorName);
        try {
            configuration.save((File) Main.configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!generatorEnabled)
        {
            FMLLog.info(Loader.instance().activeModContainer().getModId() + " world generator " + generator + " is DISABLED. Skipping registration.");
        }
        else 
        {
            FMLLog.info(Loader.instance().activeModContainer().getModId() + " registered world generator " + generator);
            worldGenerators.add(generator);
        }
        // MCPC+ end
    }

    /**
     * Callback hook for world gen - if your mod wishes to add extra mod related generation to the world
     * call this
     *
     * @param chunkX
     * @param chunkZ
     * @param world
     * @param chunkGenerator
     * @param chunkProvider
     */
    public static void generateWorld(int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        long worldSeed = world.getSeed();
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        fmlRandom.setSeed((xSeed * chunkX + zSeed * chunkZ) ^ worldSeed);

        for (IWorldGenerator generator : worldGenerators)
        {
            generator.generate(fmlRandom, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        }
    }

    /**
     * Internal method for creating an @Block instance
     * @param container
     * @param type
     * @param annotation
     * @throws Exception
     */
    public static Object buildBlock(ModContainer container, Class<?> type, Block annotation) throws Exception
    {
        Object o = type.getConstructor(int.class).newInstance(findSpareBlockId());
        registerBlock((net.minecraft.block.Block) o);
        return o;
    }

    /**
     * Private and not yet working properly
     *
     * @return a block id
     */
    private static int findSpareBlockId()
    {
        return BlockTracker.nextBlockId();
    }

    /**
     * Register an item with the item registry with a custom name : this allows for easier server->client resolution
     *
     * @param item The item to register
     * @param name The mod-unique name of the item
     */
    public static void registerItem(net.minecraft.item.Item item, String name)
    {
        registerItem(item, name, null);
    }

    /**
     * Register the specified Item with a mod specific name : overrides the standard type based name
     * @param item The item to register
     * @param name The mod-unique name to register it as - null will remove a custom name
     * @param modId An optional modId that will "own" this block - generally used by multi-mod systems
     * where one mod should "own" all the blocks of all the mods, null defaults to the active mod
     */
    public static void registerItem(net.minecraft.item.Item item, String name, String modId)
    {
        GameRegistry.registerMaterial(item, name, modId); // MCPC+ - register bukkit material
        GameData.setName(item, name, modId);
    }

    /**
     * Register a block with the world
     *
     */
    @Deprecated
    public static void registerBlock(net.minecraft.block.Block block)
    {
        registerBlock(block, ItemBlock.class);
    }


    /**
     * Register a block with the specified mod specific name : overrides the standard type based name
     * @param block The block to register
     * @param name The mod-unique name to register it as
     */
    public static void registerBlock(net.minecraft.block.Block block, String name)
    {
        registerBlock(block, ItemBlock.class, name);
    }

    /**
     * Register a block with the world, with the specified item class
     *
     * Deprecated in favour of named versions
     *
     * @param block The block to register
     * @param itemclass The item type to register with it
     */
    @Deprecated
    public static void registerBlock(net.minecraft.block.Block block, Class<? extends ItemBlock> itemclass)
    {
        registerBlock(block, itemclass, null);
    }
    /**
     * Register a block with the world, with the specified item class and block name
     * @param block The block to register
     * @param itemclass The item type to register with it
     * @param name The mod-unique name to register it with
     */
    public static void registerBlock(net.minecraft.block.Block block, Class<? extends ItemBlock> itemclass, String name)
    {
        registerBlock(block, itemclass, name, null);
    }
    /**
     * Register a block with the world, with the specified item class, block name and owning modId
     * @param block The block to register
     * @param itemclass The iterm type to register with it
     * @param name The mod-unique name to register it with
     * @param modId The modId that will own the block name. null defaults to the active modId
     */
    public static void registerBlock(net.minecraft.block.Block block, Class<? extends ItemBlock> itemclass, String name, String modId)
    {
        if (Loader.instance().isInState(LoaderState.CONSTRUCTING))
        {
            FMLLog.warning("The mod %s is attempting to register a block whilst it it being constructed. This is bad modding practice - please use a proper mod lifecycle event.", Loader.instance().activeModContainer());
        }
        try
        {
            assert block != null : "registerBlock: block cannot be null";
            assert itemclass != null : "registerBlock: itemclass cannot be null";
            int blockItemId = block.blockID - 256;
            Constructor<? extends ItemBlock> itemCtor;
            Item i;
            try
            {
                itemCtor = itemclass.getConstructor(int.class);
                i = itemCtor.newInstance(blockItemId);
            }
            catch (NoSuchMethodException e)
            {
                itemCtor = itemclass.getConstructor(int.class, net.minecraft.block.Block.class);
                i = itemCtor.newInstance(blockItemId, block);
            }
            GameRegistry.registerItem(i,name, modId);
        }
        catch (Exception e)
        {
            FMLLog.log(Level.SEVERE, e, "Caught an exception during block registration");
            throw new LoaderException(e);
        }
        blockRegistry.put(Loader.instance().activeModContainer(), (BlockProxy) block);
    }

    // MCPC+ start - register bukkit material names for modded items/blocks
    /**
     * Register the specified Material with a mod specific name : overrides the standard type XID name
     * @param item The material to register
     * @param name The material-unique name to register it as - null will default to modId_itemId
     * @param modId An optional modId that will "own" this block - generally used by multi-mod systems
     * where one mod should "own" all the blocks of all the mods, null defaults to the active mod
     */
    public static void registerMaterial(net.minecraft.item.Item item, String name, String modId)
    {
        if (name != null)
        {
            if (modId == null)
               modId = Loader.instance().activeModContainer().getModId();
            String materialName = modId + "_" + name;
            org.bukkit.Material.setMaterialName(item.itemID, materialName);
        }
        else 
        {
            if (modId == null)
                modId = Loader.instance().activeModContainer().getModId();
            String materialName = modId + "_" + String.valueOf(item.itemID);
            org.bukkit.Material.setMaterialName(item.itemID, materialName);
        }
    }

    // Check to see if the item ID is banned before registering it
    public static void addRecipe(ItemStack output, Object... params)
    {
        if (output != null && !isItemBanned(output))
            addShapedRecipe(output, params);
    }

    public static IRecipe addShapedRecipe(ItemStack output, Object... params)
    {
        return CraftingManager.getInstance().addRecipe(output, params);
    }

    public static void addShapelessRecipe(ItemStack output, Object... params)
    {
        if (output != null && !isItemBanned(output))
            CraftingManager.getInstance().addShapelessRecipe(output, params);
    }

    public static void addRecipe(IRecipe recipe)
    {
        CraftingManager.getInstance().getRecipeList().add(recipe);
    }

    public static void addSmelting(int input, ItemStack output, float xp)
    {
        if (!isItemBanned(output))
            FurnaceRecipes.smelting().addSmelting(input, output, xp);
    }

    public static boolean isItemBanned(ItemStack itemstack) {
        if (configuration.getBoolean("mcpc.enable-banned-items") && itemstack != null)
        {
            if (bannedItemCache.containsKey(itemstack.itemID))
            {
                BannedBlock block = bannedItemCache.get(itemstack.itemID);
                if (block.blockID == itemstack.itemID && (block.meta == itemstack.getItemDamage() || block.meta == -1))
                {
                    return true;
                }
            }
        }
        return false;
    }
    // MCPC+ end

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id)
    {
        TileEntity.addMapping(tileEntityClass, id);
    }

    /**
     * Register a tile entity, with alternative TileEntity identifiers. Use with caution!
     * This method allows for you to "rename" the 'id' of the tile entity.
     *
     * @param tileEntityClass The tileEntity class to register
     * @param id The primary ID, this will be the ID that the tileentity saves as
     * @param alternatives A list of alternative IDs that will also map to this class. These will never save, but they will load
     */
    public static void registerTileEntityWithAlternatives(Class<? extends TileEntity> tileEntityClass, String id, String... alternatives)
    {
        TileEntity.addMapping(tileEntityClass, id);
        Map<String,Class> teMappings = ObfuscationReflectionHelper.getPrivateValue(TileEntity.class, null, "field_" + "70326_a", "nameToClassMap", "a");
        for (String s: alternatives)
        {
            if (!teMappings.containsKey(s))
            {
                teMappings.put(s, tileEntityClass);
            }
        }
    }

    public static void addBiome(BiomeGenBase biome)
    {
        WorldType.DEFAULT.addNewBiome(biome);
    }

    public static void removeBiome(BiomeGenBase biome)
    {
        WorldType.DEFAULT.removeBiome(biome);
    }

    public static void registerFuelHandler(IFuelHandler handler)
    {
        fuelHandlers.add(handler);
    }
    public static int getFuelValue(ItemStack itemStack)
    {
        int fuelValue = 0;
        for (IFuelHandler handler : fuelHandlers)
        {
            fuelValue = Math.max(fuelValue, handler.getBurnTime(itemStack));
        }
        return fuelValue;
    }

    public static void registerCraftingHandler(ICraftingHandler handler)
    {
        craftingHandlers.add(handler);
    }

    public static void onItemCrafted(EntityPlayer player, ItemStack item, IInventory craftMatrix)
    {
        for (ICraftingHandler handler : craftingHandlers)
        {
            handler.onCrafting(player, item, craftMatrix);
        }
    }

    public static void onItemSmelted(EntityPlayer player, ItemStack item)
    {
        for (ICraftingHandler handler : craftingHandlers)
        {
            handler.onSmelting(player, item);
        }
    }

    public static void registerPickupHandler(IPickupNotifier handler)
    {
        pickupHandlers.add(handler);
    }

    public static void onPickupNotification(EntityPlayer player, EntityItem item)
    {
        for (IPickupNotifier notify : pickupHandlers)
        {
            notify.notifyPickup(item, player);
        }
    }

    public static void registerPlayerTracker(IPlayerTracker tracker)
	{
		playerTrackers.add(tracker);
	}

	public static void onPlayerLogin(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerLogin(player);
	}

	public static void onPlayerLogout(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerLogout(player);
	}

	public static void onPlayerChangedDimension(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerChangedDimension(player);
        // MCPC+ start - update compassTarget to new world when changing dimensions or it will leave a reference to the last world object causing a memory leak
        // This is required for mods that implement their own dimension transfer methods which bypass ServerConfigurationManager
        EntityPlayerMP playermp = (EntityPlayerMP)player;
        playermp.compassTarget = new Location(playermp.worldObj.getWorld(), playermp.posX, playermp.posY, playermp.posZ);
        // MCPC+ end
	}

	public static void onPlayerRespawn(EntityPlayer player)
	{
		for(IPlayerTracker tracker : playerTrackers)
			tracker.onPlayerRespawn(player);
	}


	/**
	 * Look up a mod block in the global "named item list"
	 * @param modId The modid owning the block
	 * @param name The name of the block itself
	 * @return The block or null if not found
	 */
	public static net.minecraft.block.Block findBlock(String modId, String name)
	{
	    return GameData.findBlock(modId, name);
	}

	/**
	 * Look up a mod item in the global "named item list"
	 * @param modId The modid owning the item
	 * @param name The name of the item itself
	 * @return The item or null if not found
	 */
	public static net.minecraft.item.Item findItem(String modId, String name)
    {
        return GameData.findItem(modId, name);
    }

	/**
	 * Manually register a custom item stack with FML for later tracking. It is automatically scoped with the active modid
	 *
	 * @param name The name to register it under
	 * @param itemStack The itemstack to register
	 */
	public static void registerCustomItemStack(String name, ItemStack itemStack)
	{
	    if (!isItemBanned(itemStack)) // MCPC+ - check if item is banned
	        GameData.registerCustomItemStack(name, itemStack);
	}
	/**
	 * Lookup an itemstack based on mod and name. It will create "default" itemstacks from blocks and items if no
	 * explicit itemstack is found.
	 *
	 * If it is built from a block, the metadata is by default the "wildcard" value.
	 *
	 * Custom itemstacks can be dumped from minecraft by setting the system property fml.dumpRegistry to true
	 * (-Dfml.dumpRegistry=true on the command line will work)
	 *
	 * @param modId The modid of the stack owner
	 * @param name The name of the stack
	 * @param stackSize The size of the stack returned
	 * @return The custom itemstack or null if no such itemstack was found
	 */
	public static ItemStack findItemStack(String modId, String name, int stackSize)
	{
	    ItemStack foundStack = GameData.findItemStack(modId, name);
	    if (foundStack != null)
	    {
            ItemStack is = foundStack.copy();
    	    is.stackSize = Math.min(stackSize, is.getMaxStackSize());
    	    return is;
	    }
	    return null;
	}
}
