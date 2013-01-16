package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;
// CraftBukkit start
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
// CraftBukkit end

public class BlockFire extends Block
{
    /** The chance this block will encourage nearby blocks to catch on fire */
    private int[] chanceToEncourageFire = new int[256];

    /**
     * This is an array indexed by block ID the larger the number in the array the more likely a block type will catch
     * fires
     */
    private int[] abilityToCatchFire = new int[256];

    protected BlockFire(int par1, int par2)
    {
        super(par1, par2, Material.fire);
        this.setTickRandomly(true);
    }

    /**
     * This method is called on a block after all other blocks gets already created. You can use it to reference and
     * configure something on the block that needs the others ones.
     */
    public void initializeBlock()
    {
        abilityToCatchFire = Block.blockFlammability;
        chanceToEncourageFire = Block.blockFireSpreadSpeed;
        this.setBurnRate(Block.planks.blockID, 5, 20);
        this.setBurnRate(Block.woodDoubleSlab.blockID, 5, 20);
        this.setBurnRate(Block.woodSingleSlab.blockID, 5, 20);
        this.setBurnRate(Block.fence.blockID, 5, 20);
        this.setBurnRate(Block.stairCompactPlanks.blockID, 5, 20);
        this.setBurnRate(Block.stairsWoodBirch.blockID, 5, 20);
        this.setBurnRate(Block.stairsWoodSpruce.blockID, 5, 20);
        this.setBurnRate(Block.stairsWoodJungle.blockID, 5, 20);
        this.setBurnRate(Block.wood.blockID, 5, 5);
        this.setBurnRate(Block.leaves.blockID, 30, 60);
        this.setBurnRate(Block.bookShelf.blockID, 30, 20);
        this.setBurnRate(Block.tnt.blockID, 15, 100);
        this.setBurnRate(Block.tallGrass.blockID, 60, 100);
        this.setBurnRate(Block.cloth.blockID, 30, 60);
        this.setBurnRate(Block.vine.blockID, 15, 100);
    }

    /**
     * Sets the burn rate for a block. The larger abilityToCatchFire the more easily it will catch. The larger
     * chanceToEncourageFire the faster it will burn and spread to other blocks. Args: blockID, chanceToEncourageFire,
     * abilityToCatchFire
     */
    private void setBurnRate(int par1, int par2, int par3)
    {
        Block.setBurnProperties(par1, par2, par3);
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        return null;
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 3;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random par1Random)
    {
        return 0;
    }

    /**
     * How many world ticks before ticking
     */
    public int tickRate()
    {
        return 30;
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (par1World.getGameRules().getGameRuleBooleanValue("doFireTick"))
        {
            Block base = Block.blocksList[par1World.getBlockId(par2, par3 - 1, par4)];
            boolean flag = (base != null && base.isFireSource(par1World, par2, par3 - 1, par4, par1World.getBlockMetadata(par2, par3 - 1, par4), UP));

            if (!this.canPlaceBlockAt(par1World, par2, par3, par4))
            {
                fireExtinguished(par1World, par2, par3, par4); // CraftBukkit - invalid place location
            }

            if (!flag && par1World.isRaining() && (par1World.canLightningStrikeAt(par2, par3, par4) || par1World.canLightningStrikeAt(par2 - 1, par3, par4) || par1World.canLightningStrikeAt(par2 + 1, par3, par4) || par1World.canLightningStrikeAt(par2, par3, par4 - 1) || par1World.canLightningStrikeAt(par2, par3, par4 + 1)))
            {
                fireExtinguished(par1World, par2, par3, par4); // CraftBukkit - extinguished by rain
            }
            else
            {
                int var6 = par1World.getBlockMetadata(par2, par3, par4);

                if (var6 < 15)
                {
                    par1World.setBlockMetadata(par2, par3, par4, var6 + par5Random.nextInt(3) / 2);
                }

                par1World.scheduleBlockUpdate(par2, par3, par4, this.blockID, this.tickRate() + par5Random.nextInt(10));

                if (!flag && !this.canNeighborBurn(par1World, par2, par3, par4))
                {
                    if (!par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) || var6 > 3)
                    {
                        par1World.setBlockWithNotify(par2, par3, par4, 0);
                    }
                }
                else if (!flag && !this.canBlockCatchFire((IBlockAccess) par1World, par2, par3 - 1, par4, UP) && var6 == 15 && par5Random.nextInt(4) == 0)     // Forge
                {
                    fireExtinguished(par1World, par2, par3, par4); // CraftBukkit - burn out
                }
                else
                {
                    boolean var7 = par1World.isBlockHighHumidity(par2, par3, par4);
                    byte var8 = 0;

                    if (var7)
                    {
                        var8 = -50;
                    }

                    this.tryToCatchBlockOnFire(par1World, par2 + 1, par3, par4, 300 + var8, par5Random, var6, WEST);
                    this.tryToCatchBlockOnFire(par1World, par2 - 1, par3, par4, 300 + var8, par5Random, var6, EAST);
                    this.tryToCatchBlockOnFire(par1World, par2, par3 - 1, par4, 250 + var8, par5Random, var6, UP);
                    this.tryToCatchBlockOnFire(par1World, par2, par3 + 1, par4, 250 + var8, par5Random, var6, DOWN);
                    this.tryToCatchBlockOnFire(par1World, par2, par3, par4 - 1, 300 + var8, par5Random, var6, SOUTH);
                    this.tryToCatchBlockOnFire(par1World, par2, par3, par4 + 1, 300 + var8, par5Random, var6, NORTH);
                    // CraftBukkit start - call to stop spread of fire
                    org.bukkit.Server server = par1World.getServer();
                    org.bukkit.World bworld = par1World.getWorld();
                    BlockIgniteEvent.IgniteCause igniteCause = BlockIgniteEvent.IgniteCause.SPREAD;
                    org.bukkit.block.Block fromBlock = bworld.getBlockAt(par2, par3, par4);
                    // CraftBukkit end

                    for (int var9 = par2 - 1; var9 <= par2 + 1; ++var9)
                    {
                        for (int var10 = par4 - 1; var10 <= par4 + 1; ++var10)
                        {
                            for (int var11 = par3 - 1; var11 <= par3 + 4; ++var11)
                            {
                                if (var9 != par2 || var11 != par3 || var10 != par4)
                                {
                                    int var12 = 100;

                                    if (var11 > par3 + 1)
                                    {
                                        var12 += (var11 - (par3 + 1)) * 100;
                                    }

                                    int var13 = this.getChanceOfNeighborsEncouragingFire(par1World, var9, var11, var10);

                                    if (var13 > 0)
                                    {
                                        int var14 = (var13 + 40 + par1World.difficultySetting * 7) / (var6 + 30);

                                        if (var7)
                                        {
                                            var14 /= 2;
                                        }

                                        if (var14 > 0 && par5Random.nextInt(var12) <= var14 && (!par1World.isRaining() || !par1World.canLightningStrikeAt(var9, var11, var10)) && !par1World.canLightningStrikeAt(var9 - 1, var11, par4) && !par1World.canLightningStrikeAt(var9 + 1, var11, var10) && !par1World.canLightningStrikeAt(var9, var11, var10 - 1) && !par1World.canLightningStrikeAt(var9, var11, var10 + 1))
                                        {
                                            int var15 = var6 + par5Random.nextInt(5) / 4;

                                            if (var15 > 15)
                                            {
                                                var15 = 15;
                                            }

                                            // CraftBukkit start - call to stop spread of fire
                                            org.bukkit.block.Block block = bworld.getBlockAt(var9, var11, var10);

                                            if (block.getTypeId() != Block.fire.blockID)
                                            {
                                                BlockIgniteEvent event = new BlockIgniteEvent(block, igniteCause, null);
                                                server.getPluginManager().callEvent(event);

                                                if (event.isCancelled())
                                                {
                                                    continue;
                                                }

                                                org.bukkit.block.BlockState blockState = bworld.getBlockAt(var9, var11, var10).getState();
                                                blockState.setTypeId(this.blockID);
                                                blockState.setData(new org.bukkit.material.MaterialData(this.blockID, (byte) var15));
                                                BlockSpreadEvent spreadEvent = new BlockSpreadEvent(blockState.getBlock(), fromBlock, blockState);
                                                server.getPluginManager().callEvent(spreadEvent);

                                                if (!spreadEvent.isCancelled())
                                                {
                                                    blockState.update(true);
                                                }
                                            }

                                            // CraftBukkit end
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean func_82506_l()
    {
        return false;
    }

    @Deprecated
    private void tryToCatchBlockOnFire(World par1World, int par2, int par3, int par4, int par5, Random par6Random, int par7)
    {
        this.tryToCatchBlockOnFire(par1World, par2, par3, par4, par5, par6Random, par7, UP); // Forge
    }
    // Forge start
    private void tryToCatchBlockOnFire(World world, int i, int j, int k, int l, Random random, int i1, ForgeDirection face)
    {
        int j1 = 0;
        Block block = Block.blocksList[world.getBlockId(i, j, k)];

        if (block != null)
        {
            j1 = block.getFlammability(world, i, j, k, world.getBlockMetadata(i, j, k), face);
        }

        // Forge end
        if (random.nextInt(l) < j1)
        {
            boolean flag = world.getBlockId(i, j, k) == Block.tnt.blockID;
            // CraftBukkit start
            org.bukkit.block.Block theBlock = world.getWorld().getBlockAt(i, j, k);
            BlockBurnEvent event = new BlockBurnEvent(theBlock);
            world.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return;
            }

            // CraftBukkit end

            if (random.nextInt(i1 + 10) < 5 && !world.canLightningStrikeAt(i, j, k))
            {
                int k1 = i1 + random.nextInt(5) / 4;

                if (k1 > 15)
                {
                    k1 = 15;
                }

                world.setBlockAndMetadataWithNotify(i, j, k, this.blockID, k1);
            }
            else
            {
                world.setBlockWithNotify(i, j, k, 0);
            }

            if (flag)
            {
                Block.tnt.onBlockDestroyedByPlayer(world, i, j, k, 1);
            }
        }
    }

    /**
     * Returns true if at least one block next to this one can burn.
     */
    private boolean canNeighborBurn(World par1World, int par2, int par3, int par4)
    {
        return canBlockCatchFire(par1World, par2 + 1, par3, par4, WEST ) ||
               canBlockCatchFire(par1World, par2 - 1, par3, par4, EAST ) ||
               canBlockCatchFire(par1World, par2, par3 - 1, par4, UP   ) ||
               canBlockCatchFire(par1World, par2, par3 + 1, par4, DOWN ) ||
               canBlockCatchFire(par1World, par2, par3, par4 - 1, SOUTH) ||
               canBlockCatchFire(par1World, par2, par3, par4 + 1, NORTH);
    }

    /**
     * Gets the highest chance of a neighbor block encouraging this block to catch fire
     */
    private int getChanceOfNeighborsEncouragingFire(World par1World, int par2, int par3, int par4)
    {
        byte var5 = 0;

        if (!par1World.isAirBlock(par2, par3, par4))
        {
            return 0;
        }
        else
        {
            int var6 = this.getChanceToEncourageFire(par1World, par2 + 1, par3, par4, var5, WEST);
            var6 = this.getChanceToEncourageFire(par1World, par2 - 1, par3, par4, var6, EAST);
            var6 = this.getChanceToEncourageFire(par1World, par2, par3 - 1, par4, var6, UP);
            var6 = this.getChanceToEncourageFire(par1World, par2, par3 + 1, par4, var6, DOWN);
            var6 = this.getChanceToEncourageFire(par1World, par2, par3, par4 - 1, var6, SOUTH);
            var6 = this.getChanceToEncourageFire(par1World, par2, par3, par4 + 1, var6, NORTH);
            return var6;
        }
    }

    /**
     * Returns if this block is collidable (only used by Fire). Args: x, y, z
     */
    public boolean isCollidable()
    {
        return false;
    }

    /**
     * Checks the specified block coordinate to see if it can catch fire.  Args: blockAccess, x, y, z
     * Deprecated for a side-sensitive version
     */
    @Deprecated
    public boolean canBlockCatchFire(IBlockAccess par1IBlockAccess, int par2, int par3, int par4)
    {
        return canBlockCatchFire(par1IBlockAccess, par2, par3, par4, UP);
    }

    /**
     * Retrieves a specified block's chance to encourage their neighbors to burn and if the number is greater than the
     * current number passed in it will return its number instead of the passed in one.  Args: world, x, y, z,
     * curChanceToEncourageFire
     * Deprecated for a side-sensitive version
     */
    @Deprecated
    public int getChanceToEncourageFire(World par1World, int par2, int par3, int par4, int par5)
    {
        return getChanceToEncourageFire(par1World, par2, par3, par4, par5, UP);
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        return par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) || this.canNeighborBurn(par1World, par2, par3, par4);
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World par1World, int par2, int par3, int par4, int par5)
    {
        if (!par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) && !this.canNeighborBurn(par1World, par2, par3, par4))
        {
            fireExtinguished(par1World, par2, par3, par4); // CraftBukkit - fuel block gone
        }
    }

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int par2, int par3, int par4)
    {
        if (par1World.provider.dimensionId > 0 || par1World.getBlockId(par2, par3 - 1, par4) != Block.obsidian.blockID || !Block.portal.tryToCreatePortal(par1World, par2, par3, par4))
        {
            if (!par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) && !this.canNeighborBurn(par1World, par2, par3, par4))
            {
                fireExtinguished(par1World, par2, par3, par4); // CraftBukkit - fuel block broke
            }
            else
            {
                par1World.scheduleBlockUpdate(par2, par3, par4, this.blockID, this.tickRate() + par1World.rand.nextInt(10));
            }
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    public void randomDisplayTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (par5Random.nextInt(24) == 0)
        {
            par1World.playSound((double)((float)par2 + 0.5F), (double)((float)par3 + 0.5F), (double)((float)par4 + 0.5F), "fire.fire", 1.0F + par5Random.nextFloat(), par5Random.nextFloat() * 0.7F + 0.3F, false);
        }

        int var6;
        float var7;
        float var8;
        float var9;

        if (!par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) && !Block.fire.canBlockCatchFire(par1World, par2, par3 - 1, par4, UP))
        {
            if (Block.fire.canBlockCatchFire(par1World, par2 - 1, par3, par4, EAST))
            {
                for (var6 = 0; var6 < 2; ++var6)
                {
                    var7 = (float)par2 + par5Random.nextFloat() * 0.1F;
                    var8 = (float)par3 + par5Random.nextFloat();
                    var9 = (float)par4 + par5Random.nextFloat();
                    par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Block.fire.canBlockCatchFire(par1World, par2 + 1, par3, par4, WEST))
            {
                for (var6 = 0; var6 < 2; ++var6)
                {
                    var7 = (float)(par2 + 1) - par5Random.nextFloat() * 0.1F;
                    var8 = (float)par3 + par5Random.nextFloat();
                    var9 = (float)par4 + par5Random.nextFloat();
                    par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Block.fire.canBlockCatchFire(par1World, par2, par3, par4 - 1, SOUTH))
            {
                for (var6 = 0; var6 < 2; ++var6)
                {
                    var7 = (float)par2 + par5Random.nextFloat();
                    var8 = (float)par3 + par5Random.nextFloat();
                    var9 = (float)par4 + par5Random.nextFloat() * 0.1F;
                    par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Block.fire.canBlockCatchFire(par1World, par2, par3, par4 + 1, NORTH))
            {
                for (var6 = 0; var6 < 2; ++var6)
                {
                    var7 = (float)par2 + par5Random.nextFloat();
                    var8 = (float)par3 + par5Random.nextFloat();
                    var9 = (float)(par4 + 1) - par5Random.nextFloat() * 0.1F;
                    par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Block.fire.canBlockCatchFire(par1World, par2, par3 + 1, par4, DOWN))
            {
                for (var6 = 0; var6 < 2; ++var6)
                {
                    var7 = (float)par2 + par5Random.nextFloat();
                    var8 = (float)(par3 + 1) - par5Random.nextFloat() * 0.1F;
                    var9 = (float)par4 + par5Random.nextFloat();
                    par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        else
        {
            for (var6 = 0; var6 < 3; ++var6)
            {
                var7 = (float)par2 + par5Random.nextFloat();
                var8 = (float)par3 + par5Random.nextFloat() * 0.5F + 0.5F;
                var9 = (float)par4 + par5Random.nextFloat();
                par1World.spawnParticle("largesmoke", (double)var7, (double)var8, (double)var9, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // CraftBukkit start
    private void fireExtinguished(World world, int x, int y, int z)
    {
        if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockFadeEvent(world.getWorld().getBlockAt(x, y, z), 0).isCancelled() == false)
        {
            world.setBlockWithNotify(x, y, z, 0);
        }
    }
    // CraftBukkit end

    /**
     * Side sensitive version that calls the block function.
     *
     * @param world The current world
     * @param x X Position
     * @param y Y Position
     * @param z Z Position
     * @param face The side the fire is coming from
     * @return True if the face can catch fire.
     */
    public boolean canBlockCatchFire(IBlockAccess world, int x, int y, int z, ForgeDirection face)
    {
        Block block = Block.blocksList[world.getBlockId(x, y, z)];
        if (block != null)
        {
            return block.isFlammable(world, x, y, z, world.getBlockMetadata(x, y, z), face);
        }
        return false;
    }

    /**
     * Side sensitive version that calls the block function.
     *
     * @param world The current world
     * @param x X Position
     * @param y Y Position
     * @param z Z Position
     * @param oldChance The previous maximum chance.
     * @param face The side the fire is coming from
     * @return The chance of the block catching fire, or oldChance if it is higher
     */
    public int getChanceToEncourageFire(World world, int x, int y, int z, int oldChance, ForgeDirection face)
    {
        int newChance = 0;
        Block block = Block.blocksList[world.getBlockId(x, y, z)];
        if (block != null)
        {
            newChance = block.getFireSpreadSpeed(world, x, y, z, world.getBlockMetadata(x, y, z), face);
        }
        return (newChance > oldChance ? newChance : oldChance);
    }
}
