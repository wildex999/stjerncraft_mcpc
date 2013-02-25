package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.world.World;
import net.minecraft.block.BlockSapling.TreeGenerator;
import org.bukkit.BlockChangeDelegate; // CraftBukkit
import net.minecraftforge.common.ForgeDirection;

public class WorldGenForest extends WorldGenerator implements net.minecraft.block.BlockSapling.TreeGenerator   // CraftBukkit add interface
{
    public WorldGenForest(boolean par1)
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
        int l = random.nextInt(3) + 5;
        boolean flag = true;
        World w = world instanceof World ? (World)world : null; // MCPC

        if (j >= 1 && j + l + 1 <= 256)
        {
            int i1;
            int j1;
            int k1;
            int l1;

            for (i1 = j; i1 <= j + 1 + l; ++i1)
            {
                byte b0 = 1;

                if (i1 == j)
                {
                    b0 = 0;
                }

                if (i1 >= j + 1 + l - 2)
                {
                    b0 = 2;
                }

                for (j1 = i - b0; j1 <= i + b0 && flag; ++j1)
                {
                    for (k1 = k - b0; k1 <= k + b0 && flag; ++k1)
                    {
                        if (i1 >= 0 && i1 < 256)
                        {
                            l1 = world.getTypeId(j1, i1, k1);
                            Block block = Block.blocksList[l1]; // Forge

                            if (l1 != 0 && block != null && !block.isLeaves(w, j1, i1, k1))   // Forge
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
                i1 = world.getTypeId(i, j - 1, k);
                Block soil = Block.blocksList[i1];
                // MCPC+ start - BlockChangeDelegate vs. Forge
                boolean isValidSoil;
                if (world instanceof World) {
                    isValidSoil = soil != null && soil.canSustainPlant((World) world, i, j - 1, k, ForgeDirection.UP, (BlockSapling)Block.sapling);
                } else {
                    isValidSoil = i1 == Block.grass.blockID || i1 == Block.dirt.blockID;
                }
                // MCPC+ end

                if (isValidSoil && j < 256 - l - 1)
                {
                    // MCPC+ start - BlockChangeDelegate vs. Forge
                    if (world instanceof World) {
                        soil.onPlantGrow((World) world, i, j - 1, k, i, j, k);
                    } else {
                        this.setType(world, i, j - 1, k, Block.dirt.blockID);
                    }
                    // MCPC+ end
                    int i2;

                    for (i2 = j - 3 + l; i2 <= j + l; ++i2)
                    {
                        j1 = i2 - (j + l);
                        k1 = 1 - j1 / 2;

                        for (l1 = i - k1; l1 <= i + k1; ++l1)
                        {
                            int j2 = l1 - i;

                            for (int k2 = k - k1; k2 <= k + k1; ++k2)
                            {
                                int l2 = k2 - k;
                                Block block = Block.blocksList[world.getTypeId(l1, i2, k2)]; // Forge

                                if ((Math.abs(j2) != k1 || Math.abs(l2) != k1 || random.nextInt(2) != 0 && j1 != 0) && (block == null || block.canBeReplacedByLeaves(w, l1, i2, k2)))   // Forge
                                {
                                    this.setTypeAndData(world, l1, i2, k2, Block.leaves.blockID, 2);
                                }
                            }
                        }
                    }

                    for (i2 = 0; i2 < l; ++i2)
                    {
                        j1 = world.getTypeId(i, j + i2, k);
                        Block block = Block.blocksList[j1]; // Forge

                        if (j1 == 0 || block == null || block.isLeaves(w, i, j + i2, k))   // Forge
                        {
                            this.setTypeAndData(world, i, j + i2, k, Block.wood.blockID, 2);
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
