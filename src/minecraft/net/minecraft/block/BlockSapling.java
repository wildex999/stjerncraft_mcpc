package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.*;

import net.minecraftforge.event.terraingen.TerrainGen;
// CraftBukkit start
import net.minecraft.block.BlockSapling.TreeGenerator;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.util.StructureGrowDelegate;
import org.bukkit.entity.Player;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public class BlockSapling extends BlockFlower
{
    public static final String[] WOOD_TYPES = new String[] {"oak", "spruce", "birch", "jungle"};
    private static final String[] field_94370_b = new String[] {"sapling", "sapling_spruce", "sapling_birch", "sapling_jungle"};

    protected BlockSapling(int par1)
    {
        super(par1);
        float f = 0.4F;
        this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (!par1World.isRemote)
        {
            super.updateTick(par1World, par2, par3, par4, par5Random);

            if (par1World.getBlockLightValue(par2, par3 + 1, par4) >= 9 && (par5Random.nextInt(Math.max(2, (int)((par1World.growthOdds / par1World.getWorld().aggregateTicks / par1World.getWorld().treeGrowthModifier * 7) + 0.5F))) == 0))    // Spigot
            {
                this.growTree(par1World, par2, par3, par4, par5Random, false, null, null); // CraftBukkit - added bonemeal, player and itemstack
            }
        }
    }
    
    // MCPC+ start - added for vanilla compatibility - copy vanilla method
    /**
     * Attempts to grow a sapling into a tree
     */
    public void growTree(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (!TerrainGen.saplingGrowTree(par1World, par5Random, par2, par3, par4)) return;

        int l = par1World.getBlockMetadata(par2, par3, par4) & 3;
        Object object = null;
        int i1 = 0;
        int j1 = 0;
        boolean flag = false;

        if (l == 1)
        {
            object = new WorldGenTaiga2(true);
        }
        else if (l == 2)
        {
            object = new WorldGenForest(true);
        }
        else if (l == 3)
        {
            for (i1 = 0; i1 >= -1; --i1)
            {
                for (j1 = 0; j1 >= -1; --j1)
                {
                    if (this.isSameSapling(par1World, par2 + i1, par3, par4 + j1, 3) && this.isSameSapling(par1World, par2 + i1 + 1, par3, par4 + j1, 3) && this.isSameSapling(par1World, par2 + i1, par3, par4 + j1 + 1, 3) && this.isSameSapling(par1World, par2 + i1 + 1, par3, par4 + j1 + 1, 3))
                    {
                        object = new WorldGenHugeTrees(true, 10 + par5Random.nextInt(20), 3, 3);
                        flag = true;
                        break;
                    }
                }

                if (object != null)
                {
                    break;
                }
            }

            if (object == null)
            {
                j1 = 0;
                i1 = 0;
                object = new WorldGenTrees(true, 4 + par5Random.nextInt(7), 3, 3, false);
            }
        }
        else
        {
            object = new WorldGenTrees(true);

            if (par5Random.nextInt(10) == 0)
            {
                object = new WorldGenBigTree(true);
            }
        }

        if (flag)
        {
            par1World.setBlock(par2 + i1, par3, par4 + j1, 0, 0, 4);
            par1World.setBlock(par2 + i1 + 1, par3, par4 + j1, 0, 0, 4);
            par1World.setBlock(par2 + i1, par3, par4 + j1 + 1, 0, 0, 4);
            par1World.setBlock(par2 + i1 + 1, par3, par4 + j1 + 1, 0, 0, 4);
        }
        else
        {
            par1World.setBlock(par2, par3, par4, 0, 0, 4);
        }

        if (!((WorldGenerator)object).generate(par1World, par5Random, par2 + i1, par3, par4 + j1))
        {
            if (flag)
            {
                par1World.setBlock(par2 + i1, par3, par4 + j1, this.blockID, l, 4);
                par1World.setBlock(par2 + i1 + 1, par3, par4 + j1, this.blockID, l, 4);
                par1World.setBlock(par2 + i1, par3, par4 + j1 + 1, this.blockID, l, 4);
                par1World.setBlock(par2 + i1 + 1, par3, par4 + j1 + 1, this.blockID, l, 4);
            }
            else
            {
                par1World.setBlock(par2, par3, par4, this.blockID, l, 4);
            }
        }
    }
    // MCPC+ end 

    // CraftBukkit - added bonemeal, player and itemstack

    /**
     * Attempts to grow a sapling into a tree
     */
    public void growTree(World par1World, int par2, int par3, int par4, Random par5Random, boolean bonemeal, Player player, ItemStack itemstack)
    {
        // MCPC+ start - delegate growth to subclass if overridden, mods @Override growTree() e.g. MFR rubber trees
        if (this.getClass() != BlockSapling.class) {
            this.growTree(par1World, par2, par3, par4, par5Random);
            return;
        }
        // MCPC+ end
        if (!TerrainGen.saplingGrowTree(par1World, par5Random, par2, par3, par4)) return;
    
        int l = par1World.getBlockMetadata(par2, par3, par4);

        if ((l & 8) == 0)
        {
            par1World.setBlockMetadataWithNotify(par2, par3, par4, l | 8, 4);
        }
        else
        {
            this.d(par1World, par2, par3, par4, par5Random, bonemeal, player, itemstack); // CraftBukkit
        }
    }

    // CraftBukkit - Added bonemeal, player and itemstack
    public void d(World world, int i, int j, int k, Random random, boolean bonemeal, Player player, ItemStack itemstack)
    {
        int l = world.getBlockMetadata(i, j, k) & 3;
        Object object = null;
        int i1 = 0;
        int j1 = 0;
        boolean flag = false;
        // CraftBukkit start - Records tree generation and calls StructureGrowEvent
        StructureGrowDelegate delegate = new StructureGrowDelegate(world);
        TreeType treeType = null;
        TreeGenerator gen = null;
        boolean grownTree = false;

        if (l == 1)
        {
            treeType = TreeType.REDWOOD;
            gen = new WorldGenTaiga2(false);
        }
        else if (l == 2)
        {
            treeType = TreeType.BIRCH;
            gen = new WorldGenForest(false);
        }
        else if (l == 3)
        {
            for (i1 = 0; i1 >= -1; --i1)
            {
                for (j1 = 0; j1 >= -1; --j1)
                {
                    if (this.isSameSapling(world, i + i1, j, k + j1, 3) && this.isSameSapling(world, i + i1 + 1, j, k + j1, 3) && this.isSameSapling(world, i + i1, j, k + j1 + 1, 3) && this.isSameSapling(world, i + i1 + 1, j, k + j1 + 1, 3))
                    {
                        treeType = TreeType.JUNGLE;
                        gen = new WorldGenHugeTrees(false, 10 + random.nextInt(20), 3, 3);
                        flag = true;
                        break;
                    }
                }

                if (gen != null)
                {
                    break;
                }
            }

            if (gen == null)
            {
                j1 = 0;
                i1 = 0;
                treeType = TreeType.SMALL_JUNGLE;
                gen = new WorldGenTrees(false, 4 + random.nextInt(7), 3, 3, false);
            }
        }
        else
        {
            treeType = TreeType.TREE;
            gen = new WorldGenTrees(false);

            if (random.nextInt(10) == 0)
            {
                treeType = TreeType.BIG_TREE;
                gen = new WorldGenBigTree(false);
            }
        }

        if (flag)
        {
            world.setBlock(i + i1, j, k + j1, 0, 0, 4);
            world.setBlock(i + i1 + 1, j, k + j1, 0, 0, 4);
            world.setBlock(i + i1, j, k + j1 + 1, 0, 0, 4);
            world.setBlock(i + i1 + 1, j, k + j1 + 1, 0, 0, 4);
        }
        else
        {
            world.setBlock(i, j, k, 0, 0, 4);
        }

        grownTree = gen.generate(delegate, random, i + i1, j, k + j1);

        if (grownTree)
        {
            Location location = new Location(world.getWorld(), i, j, k);
            StructureGrowEvent event = new StructureGrowEvent(location, treeType, bonemeal, player, delegate.getBlocks());
            org.bukkit.Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                grownTree = false;
            }
            else
            {
                for (org.bukkit.block.BlockState state : event.getBlocks())
                {
                    state.update(true);
                }

                if (event.isFromBonemeal() && itemstack != null)
                {
                    --itemstack.stackSize;
                }
            }
        }
        else if (bonemeal && itemstack != null)
        {
            // We always consume bonemeal when trying to grow
            --itemstack.stackSize;
        }

        if (!grownTree)
        {
            if (flag)
            {
                world.setBlock(i + i1, j, k + j1, this.blockID, l, 4);
                world.setBlock(i + i1 + 1, j, k + j1, this.blockID, l, 4);
                world.setBlock(i + i1, j, k + j1 + 1, this.blockID, l, 4);
                world.setBlock(i + i1 + 1, j, k + j1 + 1, this.blockID, l, 4);
            }
            else
            {
                world.setBlock(i, j, k, this.blockID, l, 4);
            }
        }

        // CraftBukkit end
    }

    /**
     * Determines if the same sapling is present at the given location.
     */
    public boolean isSameSapling(World par1World, int par2, int par3, int par4, int par5)
    {
        return par1World.getBlockId(par2, par3, par4) == this.blockID && (par1World.getBlockMetadata(par2, par3, par4) & 3) == par5;
    }

    /**
     * Determines the damage on the item the block drops. Used in cloth and wood.
     */
    public int damageDropped(int par1)
    {
        return par1 & 3;
    }

    // CraftBukkit start
    public interface TreeGenerator
    {
        public boolean generate(World world, Random random, int i, int j, int k);

        public boolean generate(org.bukkit.BlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5);
    }
    // CraftBukkit end
}
