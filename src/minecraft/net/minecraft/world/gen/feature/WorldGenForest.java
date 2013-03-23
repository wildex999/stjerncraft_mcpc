package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.world.World;

import org.bukkit.BlockChangeDelegate; // CraftBukkit

public class WorldGenForest extends WorldGenerator implements TreeGenerator   // CraftBukkit add interface
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

    public boolean generate(BlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit end
        int l = par2Random.nextInt(3) + 5;
        boolean flag = true;

        if (par4 >= 1 && par4 + l + 1 <= 256)
        {
            int i1;
            int j1;
            int k1;
            int l1;

            for (i1 = par4; i1 <= par4 + 1 + l; ++i1)
            {
                byte b0 = 1;

                if (i1 == par4)
                {
                    b0 = 0;
                }

                if (i1 >= par4 + 1 + l - 2)
                {
                    b0 = 2;
                }

                for (j1 = par3 - b0; j1 <= par3 + b0 && flag; ++j1)
                {
                    for (k1 = par5 - b0; k1 <= par5 + b0 && flag; ++k1)
                    {
                        if (i1 >= 0 && i1 < 256)
                        {
                            l1 = par1World.getTypeId(j1, i1, k1);

                            if (l1 != 0 && l1 != Block.leaves.blockID)
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
                i1 = par1World.getTypeId(par3, par4 - 1, par5);

                if ((i1 == Block.grass.blockID || i1 == Block.dirt.blockID) && par4 < 256 - l - 1)
                {
                    this.setType(par1World, par3, par4 - 1, par5, Block.dirt.blockID);
                    int i2;

                    for (i2 = par4 - 3 + l; i2 <= par4 + l; ++i2)
                    {
                        j1 = i2 - (par4 + l);
                        k1 = 1 - j1 / 2;

                        for (l1 = par3 - k1; l1 <= par3 + k1; ++l1)
                        {
                            int j2 = l1 - par3;

                            for (int k2 = par5 - k1; k2 <= par5 + k1; ++k2)
                            {
                                int l2 = k2 - par5;

                                if (Math.abs(j2) != k1 || Math.abs(l2) != k1 || par2Random.nextInt(2) != 0 && j1 != 0)
                                {
                                    int i3 = par1World.getTypeId(l1, i2, k2);

                                    if (i3 == 0 || i3 == Block.leaves.blockID)
                                    {
                                        this.setTypeAndData(par1World, l1, i2, k2, Block.leaves.blockID, 2);
                                    }
                                }
                            }
                        }
                    }

                    for (i2 = 0; i2 < l; ++i2)
                    {
                        j1 = par1World.getTypeId(par3, par4 + i2, par5);

                        if (j1 == 0 || j1 == Block.leaves.blockID)
                        {
                            this.setTypeAndData(par1World, par3, par4 + i2, par5, Block.wood.blockID, 2);
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
