package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockOre extends Block
{
    public BlockOre(int par1, int par2)
    {
        super(par1, par2, Material.rock);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return this.blockID == Block.oreCoal.blockID ? Item.coal.itemID : (this.blockID == Block.oreDiamond.blockID ? Item.diamond.itemID : (this.blockID == Block.oreLapis.blockID ? Item.dyePowder.itemID : (this.blockID == Block.oreEmerald.blockID ? Item.emerald.itemID : this.blockID)));
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random par1Random)
    {
        return this.blockID == Block.oreLapis.blockID ? 4 + par1Random.nextInt(5) : 1;
    }

    /**
     * Returns the usual quantity dropped by the block plus a bonus of 1 to 'i' (inclusive).
     */
    public int quantityDroppedWithBonus(int par1, Random par2Random)
    {
        if (par1 > 0 && this.blockID != this.idDropped(0, par2Random, par1))
        {
            int j = par2Random.nextInt(par1 + 2) - 1;

            if (j < 0)
            {
                j = 0;
            }

            return this.quantityDropped(par2Random) * (j + 1);
        }
        else
        {
            return this.quantityDropped(par2Random);
        }
    }

    /**
     * Drops the block items with a specified chance of dropping the specified items
     */
    public void dropBlockAsItemWithChance(World par1World, int par2, int par3, int par4, int par5, float par6, int par7)
    {
        super.dropBlockAsItemWithChance(par1World, par2, par3, par4, par5, par6, par7);
        /* CraftBukkit start - delegated getExpDrop
        if (this.getDropType(l, world.random, i1) != this.id) {
            int j1 = 0;

            if (this.id == Block.COAL_ORE.id) {
                j1 = MathHelper.nextInt(world.random, 0, 2);
            } else if (this.id == Block.DIAMOND_ORE.id) {
                j1 = MathHelper.nextInt(world.random, 3, 7);
            } else if (this.id == Block.EMERALD_ORE.id) {
                j1 = MathHelper.nextInt(world.random, 3, 7);
            } else if (this.id == Block.LAPIS_ORE.id) {
                j1 = MathHelper.nextInt(world.random, 2, 5);
            }

            this.f(world, i, j, k, j1);
        } */
    }

    public int getExpDrop(World world, int l, int i1)
    {
        if (this.idDropped(l, world.rand, i1) != this.blockID)
        {
            int j1 = 0;

            if (this.blockID == Block.oreCoal.blockID)
            {
                j1 = MathHelper.getRandomIntegerInRange(world.rand, 0, 2);
            }
            else if (this.blockID == Block.oreDiamond.blockID)
            {
                j1 = MathHelper.getRandomIntegerInRange(world.rand, 3, 7);
            }
            else if (this.blockID == Block.oreEmerald.blockID)
            {
                j1 = MathHelper.getRandomIntegerInRange(world.rand, 3, 7);
            }
            else if (this.blockID == Block.oreLapis.blockID)
            {
                j1 = MathHelper.getRandomIntegerInRange(world.rand, 2, 5);
            }

            return j1;
        }

        return 0;
        // CraftBukkit end
    }

    /**
     * Determines the damage on the item the block drops. Used in cloth and wood.
     */
    public int damageDropped(int par1)
    {
        return this.blockID == Block.oreLapis.blockID ? 4 : 0;
    }
}
