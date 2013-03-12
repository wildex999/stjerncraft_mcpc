package net.minecraft.block;

import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;

import net.minecraftforge.common.ForgeDirection;
// CraftBukkit start
import java.util.ArrayList;
import net.minecraft.item.ItemStack;

import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public class BlockMushroom extends BlockFlower
{
    protected BlockMushroom(int par1, int par2)
    {
        super(par1, par2);
        float f = 0.2F;
        this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        this.setTickRandomly(true);
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        final int sourceX = par2, sourceY = par3, sourceZ = par4; // CraftBukkit

        if (par5Random.nextInt(Math.max(1, (int) par1World.growthOdds / par1World.getWorld().mushroomGrowthModifier * 25)) == 0)   // Spigot
        {
            byte b0 = 4;
            int l = 5;
            int i1;
            int j1;
            int k1;

            for (i1 = par2 - b0; i1 <= par2 + b0; ++i1)
            {
                for (j1 = par4 - b0; j1 <= par4 + b0; ++j1)
                {
                    for (k1 = par3 - 1; k1 <= par3 + 1; ++k1)
                    {
                        if (par1World.getBlockId(i1, k1, j1) == this.blockID)
                        {
                            --l;

                            if (l <= 0)
                            {
                                return;
                            }
                        }
                    }
                }
            }

            i1 = par2 + par5Random.nextInt(3) - 1;
            j1 = par3 + par5Random.nextInt(2) - par5Random.nextInt(2);
            k1 = par4 + par5Random.nextInt(3) - 1;

            for (int l1 = 0; l1 < 4; ++l1)
            {
                if (par1World.isAirBlock(i1, j1, k1) && this.canBlockStay(par1World, i1, j1, k1))
                {
                    par2 = i1;
                    par3 = j1;
                    par4 = k1;
                }

                i1 = par2 + par5Random.nextInt(3) - 1;
                j1 = par3 + par5Random.nextInt(2) - par5Random.nextInt(2);
                k1 = par4 + par5Random.nextInt(3) - 1;
            }

            if (par1World.isAirBlock(i1, j1, k1) && this.canBlockStay(par1World, i1, j1, k1))
            {
                // CraftBukkit start
                org.bukkit.World bworld = par1World.getWorld();
                BlockState blockState = bworld.getBlockAt(i1, j1, k1).getState();
                blockState.setTypeId(this.blockID);
                BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(sourceX, sourceY, sourceZ), blockState);
                par1World.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    blockState.update(true);
                }

                // CraftBukkit end
            }
        }
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        return super.canPlaceBlockAt(par1World, par2, par3, par4) && this.canBlockStay(par1World, par2, par3, par4);
    }

    /**
     * Gets passed in the blockID of the block below and supposed to return true if its allowed to grow on the type of
     * blockID passed in. Args: blockID
     */
    protected boolean canThisPlantGrowOnThisBlockID(int par1)
    {
        return Block.opaqueCubeLookup[par1];
    }

    /**
     * Can this block stay at this position.  Similar to canPlaceBlockAt except gets checked often with plants.
     */
    public boolean canBlockStay(World par1World, int par2, int par3, int par4)
    {
        if (par3 >= 0 && par3 < 256)
        {
            int l = par1World.getBlockId(par2, par3 - 1, par4);
            Block soil = Block.blocksList[l];
            return (l == Block.mycelium.blockID || par1World.getFullBlockLightValue(par2, par3, par4) < 13) &&
                   (soil != null && soil.canSustainPlant(par1World, par2, par3 - 1, par4, ForgeDirection.UP, this));
        }
        else
        {
            return false;
        }
    }

    // MCPC+ start - wrapper for vanilla compatibility
    public boolean fertilizeMushroom(World world, int i, int j, int k, Random random)
    {
        return this.fertilizeMushroom(world, i, j, k, random, false, null, null);
    }
    // MCPC+ end

    /**
     * Fertilize the mushroom.
     */
    // CraftBukkit - added bonemeal, player and itemstack
    public boolean fertilizeMushroom(World world, int i, int j, int k, Random random, boolean bonemeal, org.bukkit.entity.Player player, ItemStack itemstack)
    {
        int l = world.getBlockMetadata(i, j, k);
        world.setBlock(i, j, k, 0);
        // CraftBukkit start
        boolean grown = false;
        StructureGrowEvent event = null;
        Location location = new Location(world.getWorld(), i, j, k);
        WorldGenBigMushroom worldgenhugemushroom = null;

        // MCPC+ start - add support for Twilight Forest
        if (player != null)
        {
            if (this.blockID == Block.mushroomBrown.blockID)
            {
                event = new StructureGrowEvent(location, TreeType.BROWN_MUSHROOM, bonemeal, player, new ArrayList<BlockState>());
                worldgenhugemushroom = new WorldGenBigMushroom(0);
            }
            else if (this.blockID == Block.mushroomRed.blockID)
            {
                event = new StructureGrowEvent(location, TreeType.RED_MUSHROOM, bonemeal, player, new ArrayList<BlockState>());
                worldgenhugemushroom = new WorldGenBigMushroom(1);
            }
    
            if (worldgenhugemushroom != null && event != null)
            {
                grown = worldgenhugemushroom.grow((org.bukkit.BlockChangeDelegate)world, random, i, j, k, event, itemstack, world.getWorld());
    
                if (event.isFromBonemeal() && itemstack != null)
                {
                    --itemstack.stackSize;
                }
            }
    
            if (!grown || event.isCancelled())
            {
                world.setBlockAndMetadata(i, j, k, this.blockID, l);
                return false;
            }
        }
        else { // do vanilla
            if (this.blockID == Block.mushroomBrown.blockID)
            {
                worldgenhugemushroom = new WorldGenBigMushroom(0);
            }
            else if (this.blockID == Block.mushroomRed.blockID)
            {
                worldgenhugemushroom = new WorldGenBigMushroom(1);
            }

            if (worldgenhugemushroom != null && worldgenhugemushroom.generate(world, random, i, j, k))
            {
                return true;
            }
            else
            {
                world.setBlockAndMetadata(i, j, k, this.blockID, l);
                return false;
            }
        }
        // MCPC+ end

        return true;
        // CraftBukkit end
    }
}
