package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.world.World;

import org.bukkit.BlockChangeDelegate; // CraftBukkit

public abstract class WorldGenerator
{
    /**
     * Sets wither or not the generator should notify blocks of blocks it changes. When the world is first generated,
     * this is false, when saplings grow, this is true.
     */
    private final boolean doBlockNotify;

    public WorldGenerator()
    {
        this.doBlockNotify = false;
    }

    public WorldGenerator(boolean par1)
    {
        this.doBlockNotify = par1;
    }

    public abstract boolean generate(World world, Random random, int i, int j, int k);

    /**
     * Rescales the generator settings, only used in WorldGenBigTree
     */
    public void setScale(double par1, double par3, double par5) {}

    // CraftBukkit - change signature
    protected void setType(BlockChangeDelegate world, int i, int j, int k, int l)
    {
        this.setTypeAndData(world, i, j, k, l, 0);
    }

    // CraftBukkit - change signature
    protected void setTypeAndData(BlockChangeDelegate world, int i, int j, int k, int l, int i1)
    {
        if (this.doBlockNotify)
        {
            world.setTypeIdAndData(i, j, k, l, i1);
        }
        else
        {
            // CraftBukkit start - Layering violation :(
            if (world instanceof World)
            {
                ((World) world).setBlock(i, j, k, l, i1, 2);
            }
            else
            {
                world.setRawTypeIdAndData(i, j, k, l, i1);
            }

            // CraftBukkit end
        }
    }
    
    // MCPC+ start - vanilla compatibility
    /**
     * Sets the block without metadata in the world, notifying neighbors if enabled.
     */
    protected void setBlock(World par1World, int par2, int par3, int par4, int par5)
    {
        this.setBlockAndMetadata(par1World, par2, par3, par4, par5, 0);
    }

    /**
     * Sets the block in the world, notifying neighbors if enabled.
     */
    protected void setBlockAndMetadata(World par1World, int par2, int par3, int par4, int par5, int par6)
    {
        if (this.doBlockNotify)
        {
            par1World.setBlock(par2, par3, par4, par5, par6, 3);
        }
        else
        {
            par1World.setBlock(par2, par3, par4, par5, par6, 2);
        }
    }
    // MCPC+ end    
}
