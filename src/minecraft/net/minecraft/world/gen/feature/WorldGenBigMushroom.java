package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;

// CraftBukkit start
import net.minecraft.item.ItemStack;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
// CraftBukkit end

public class WorldGenBigMushroom extends WorldGenerator implements net.minecraft.block.BlockSapling.TreeGenerator   // CraftBukkit - add interface
{
    /** The mushroom type. 0 for brown, 1 for red. */
    private int mushroomType = -1;

    public WorldGenBigMushroom(int par1)
    {
        super(true);
        this.mushroomType = par1;
    }

    public WorldGenBigMushroom()
    {
        super(false);
    }

    public boolean generate(World par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit start - Moved to generate
        return grow((BlockChangeDelegate) par1World, par2Random, par3, par4, par5, null, null, null);
    }

    public boolean generate(BlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        return grow(par1World, par2Random, par3, par4, par5, null, null, null);
    }

    public boolean grow(BlockChangeDelegate world, Random random, int i, int j, int k, org.bukkit.event.world.StructureGrowEvent event, ItemStack itemstack, org.bukkit.craftbukkit.CraftWorld bukkitWorld)
    {
        // CraftBukkit end
        int l = random.nextInt(2);

        if (this.mushroomType >= 0)
        {
            l = this.mushroomType;
        }

        int i1 = random.nextInt(3) + 4;
        boolean flag = true;

        if (j >= 1 && j + i1 + 1 < 256)
        {
            int j1;
            int k1;
            int l1;
            int i2;

            for (j1 = j; j1 <= j + 1 + i1; ++j1)
            {
                byte b0 = 3;

                if (j1 <= j + 3)
                {
                    b0 = 0;
                }

                for (k1 = i - b0; k1 <= i + b0 && flag; ++k1)
                {
                    for (l1 = k - b0; l1 <= k + b0 && flag; ++l1)
                    {
                        if (j1 >= 0 && j1 < 256)
                        {
                            i2 = world.getTypeId(k1, j1, l1);

                            Block block = Block.blocksList[i2];
                            
                            if (block != null && !block.isAirBlock(world, k1, j1, l1) && !block.isLeaves(world, k1, j1, l1))
                            {
                                flag = false;
                            }
                        }
                        else
                        {
                            flag = false;
                        }
                    }
                }
            }

            if (!flag)
            {
                return false;
            }
            else
            {
                j1 = world.getTypeId(i, j - 1, k);

                if (j1 != Block.dirt.blockID && j1 != Block.grass.blockID && j1 != Block.mycelium.blockID)
                {
                    return false;
                }
                else
                {
                    // CraftBukkit start
                    if (event == null)
                    {
                        this.setTypeAndData(world, i, j - 1, k, Block.dirt.blockID, 0);
                    }
                    else
                    {
                        BlockState dirtState = bukkitWorld.getBlockAt(i, j - 1, k).getState();
                        dirtState.setTypeId(Block.dirt.blockID);
                        event.getBlocks().add(dirtState);
                    }

                    // CraftBukkit end
                    int j2 = j + i1;

                    if (l == 1)
                    {
                        j2 = j + i1 - 3;
                    }

                    for (k1 = j2; k1 <= j + i1; ++k1)
                    {
                        l1 = 1;

                        if (k1 < j + i1)
                        {
                            ++l1;
                        }

                        if (l == 0)
                        {
                            l1 = 3;
                        }

                        for (i2 = i - l1; i2 <= i + l1; ++i2)
                        {
                            for (int k2 = k - l1; k2 <= k + l1; ++k2)
                            {
                                int l2 = 5;

                                if (i2 == i - l1)
                                {
                                    --l2;
                                }

                                if (i2 == i + l1)
                                {
                                    ++l2;
                                }

                                if (k2 == k - l1)
                                {
                                    l2 -= 3;
                                }

                                if (k2 == k + l1)
                                {
                                    l2 += 3;
                                }

                                if (l == 0 || k1 < j + i1)
                                {
                                    if ((i2 == i - l1 || i2 == i + l1) && (k2 == k - l1 || k2 == k + l1))
                                    {
                                        continue;
                                    }

                                    if (i2 == i - (l1 - 1) && k2 == k - l1)
                                    {
                                        l2 = 1;
                                    }

                                    if (i2 == i - l1 && k2 == k - (l1 - 1))
                                    {
                                        l2 = 1;
                                    }

                                    if (i2 == i + (l1 - 1) && k2 == k - l1)
                                    {
                                        l2 = 3;
                                    }

                                    if (i2 == i + l1 && k2 == k - (l1 - 1))
                                    {
                                        l2 = 3;
                                    }

                                    if (i2 == i - (l1 - 1) && k2 == k + l1)
                                    {
                                        l2 = 7;
                                    }

                                    if (i2 == i - l1 && k2 == k + (l1 - 1))
                                    {
                                        l2 = 7;
                                    }

                                    if (i2 == i + (l1 - 1) && k2 == k + l1)
                                    {
                                        l2 = 9;
                                    }

                                    if (i2 == i + l1 && k2 == k + (l1 - 1))
                                    {
                                        l2 = 9;
                                    }
                                }

                                if (l2 == 5 && k1 < j + i1)
                                {
                                    l2 = 0;
                                }

                                Block block = Block.blocksList[world.getTypeId(i2, k1, k2)];

                                if ((l2 != 0 || j >= j + i1 - 1) && (block == null || block.canBeReplacedByLeaves(world, i2, k1, k2)))
                                {
                                    // CraftBukkit start
                                    if (event == null)
                                    {
                                        this.setTypeAndData(world, i2, k1, k2, Block.mushroomCapBrown.blockID + l, l2);
                                    }
                                    else
                                    {
                                        BlockState state = bukkitWorld.getBlockAt(i2, k1, k2).getState();
                                        state.setTypeId(Block.mushroomCapBrown.blockID + l);
                                        state.setData(new MaterialData(Block.mushroomCapBrown.blockID + l, (byte) l2));
                                        event.getBlocks().add(state);
                                    }

                                    // CraftBukkit end
                                }
                            }
                        }
                    }

                    for (k1 = 0; k1 < i1; ++k1)
                    {
                        l1 = world.getTypeId(i, j + k1, k);

                        Block block = Block.blocksList[l1];
                        if (block == null || block.canBeReplacedByLeaves(world, i, j + k1, k))
                        {
                            // CraftBukkit start
                            if (event == null)
                            {
                                this.setTypeAndData(world, i, j + k1, k, Block.mushroomCapBrown.blockID + l, 10);
                            }
                            else
                            {
                                BlockState state = bukkitWorld.getBlockAt(i, j + k1, k).getState();
                                state.setTypeId(Block.mushroomCapBrown.blockID + l);
                                state.setData(new MaterialData(Block.mushroomCapBrown.blockID + l, (byte) 10));
                                event.getBlocks().add(state);
                            }

                            // CraftBukkit end
                        }
                    }

                    // CraftBukkit start
                    if (event != null)
                    {
                        org.bukkit.Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            for (BlockState state : event.getBlocks())
                            {
                                state.update(true);
                            }
                        }
                    }

                    // CraftBukkit end
                    return true;
                }
            }
        }
        else
        {
            return false;
        }
    }
}
