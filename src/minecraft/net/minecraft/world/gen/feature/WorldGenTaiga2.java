package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.world.World;
import net.minecraft.block.BlockSapling.TreeGenerator;
import org.bukkit.BlockChangeDelegate; // CraftBukkit
import net.minecraftforge.common.ForgeDirection;

public class WorldGenTaiga2 extends WorldGenerator implements net.minecraft.block.BlockSapling.TreeGenerator   // CraftBukkit add interface
{
    public WorldGenTaiga2(boolean par1)
    {
        super(par1);
    }

    public boolean generate(World par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit start - moved to generate
        return this.generate((BlockChangeDelegate) par1World, par2Random, par3, par4, par5);
    }

    public boolean generate(BlockChangeDelegate world, Random random, int i, int j, int k)
    {
        // CraftBukkit end
        int l = random.nextInt(4) + 6;
        int i1 = 1 + random.nextInt(2);
        int j1 = l - i1;
        int k1 = 2 + random.nextInt(2);
        boolean flag = true;
        World w = world instanceof World ? (World)world : null; // MCPC

        if (j >= 1 && j + l + 1 <= 256)
        {
            int l1;
            int i2;
            int j2;
            int k2;

            for (l1 = j; l1 <= j + 1 + l && flag; ++l1)
            {
                boolean flag1 = true;

                if (l1 - j < i1)
                {
                    k2 = 0;
                }
                else
                {
                    k2 = k1;
                }

                for (i2 = i - k2; i2 <= i + k2 && flag; ++i2)
                {
                    for (int l2 = k - k2; l2 <= k + k2 && flag; ++l2)
                    {
                        if (l1 >= 0 && l1 < 256)
                        {
                            j2 = world.getTypeId(i2, l1, l2);
                            // Forge start
                            Block block = Block.blocksList[j2];

                            if (j2 != 0 && block != null && !block.isLeaves(w, i2, l1, l2))
                            {
                                // Forge end
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
                l1 = world.getTypeId(i, j - 1, k);
                Block soil = Block.blocksList[l1];
                boolean isValidSoil = soil != null && soil.canSustainPlant(w, i, j - 1, k, ForgeDirection.UP, (BlockSapling)Block.sapling);

                if (isValidSoil && j < 256 - l - 1)
                {
                    // MCPC+ start - BlockChangeDelegate vs. Forge
                    if (world instanceof World) {
                        soil.onPlantGrow((World) world, i, j - 1, k, i, j, k);
                    } else {
                        this.setType(world, i, j - 1, k, Block.dirt.blockID);
                    }
                    // MCPC+ end
                    k2 = random.nextInt(2);
                    i2 = 1;
                    byte b0 = 0;
                    int i3;
                    int j3;

                    for (j2 = 0; j2 <= j1; ++j2)
                    {
                        j3 = j + l - j2;

                        for (i3 = i - k2; i3 <= i + k2; ++i3)
                        {
                            int k3 = i3 - i;

                            for (int l3 = k - k2; l3 <= k + k2; ++l3)
                            {
                                int i4 = l3 - k;
                                // Forge start
                                Block block = Block.blocksList[world.getTypeId(i3, j3, l3)];

                                if ((Math.abs(k3) != k2 || Math.abs(i4) != k2 || k2 <= 0) && (block == null || block.canBeReplacedByLeaves(w, i3, j3, l3)))
                                {
                                    // Forge end
                                    this.setTypeAndData(world, i3, j3, l3, Block.leaves.blockID, 1);
                                }
                            }
                        }

                        if (k2 >= i2)
                        {
                            k2 = b0;
                            b0 = 1;
                            ++i2;

                            if (i2 > k1)
                            {
                                i2 = k1;
                            }
                        }
                        else
                        {
                            ++k2;
                        }
                    }

                    j2 = random.nextInt(3);

                    for (j3 = 0; j3 < l - j2; ++j3)
                    {
                        i3 = world.getTypeId(i, j + j3, k);
                        Block block = Block.blocksList[i3];

                        if (i3 == 0 || block == null || block.isLeaves(w, i, j + j3, k))
                        {
                            this.setTypeAndData(world, i, j + j3, k, Block.wood.blockID, 1);
                        }
                    }

                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }
}
