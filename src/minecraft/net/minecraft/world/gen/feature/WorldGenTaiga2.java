package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.world.World;

import org.bukkit.BlockChangeDelegate; // CraftBukkit

public class WorldGenTaiga2 extends WorldGenerator implements TreeGenerator   // CraftBukkit add interface
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

    public boolean generate(BlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit end
        int l = par2Random.nextInt(4) + 6;
        int i1 = 1 + par2Random.nextInt(2);
        int j1 = l - i1;
        int k1 = 2 + par2Random.nextInt(2);
        boolean flag = true;

        if (par4 >= 1 && par4 + l + 1 <= 256)
        {
            int l1;
            int i2;
            int j2;
            int k2;

            for (l1 = par4; l1 <= par4 + 1 + l && flag; ++l1)
            {
                boolean flag1 = true;

                if (l1 - par4 < i1)
                {
                    k2 = 0;
                }
                else
                {
                    k2 = k1;
                }

                for (i2 = par3 - k2; i2 <= par3 + k2 && flag; ++i2)
                {
                    for (int l2 = par5 - k2; l2 <= par5 + k2 && flag; ++l2)
                    {
                        if (l1 >= 0 && l1 < 256)
                        {
                            j2 = par1World.getTypeId(i2, l1, l2);

                            if (j2 != 0 && j2 != Block.leaves.blockID)
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
                l1 = par1World.getTypeId(par3, par4 - 1, par5);

                if ((l1 == Block.grass.blockID || l1 == Block.dirt.blockID) && par4 < 256 - l - 1)
                {
                    this.setType(par1World, par3, par4 - 1, par5, Block.dirt.blockID);
                    k2 = par2Random.nextInt(2);
                    i2 = 1;
                    byte b0 = 0;
                    int i3;
                    int j3;

                    for (j2 = 0; j2 <= j1; ++j2)
                    {
                        j3 = par4 + l - j2;

                        for (i3 = par3 - k2; i3 <= par3 + k2; ++i3)
                        {
                            int k3 = i3 - par3;

                            for (int l3 = par5 - k2; l3 <= par5 + k2; ++l3)
                            {
                                int i4 = l3 - par5;

                                if ((Math.abs(k3) != k2 || Math.abs(i4) != k2 || k2 <= 0) && !Block.opaqueCubeLookup[par1World.getTypeId(i3, j3, l3)])
                                {
                                    this.setTypeAndData(par1World, i3, j3, l3, Block.leaves.blockID, 1);
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

                    j2 = par2Random.nextInt(3);

                    for (j3 = 0; j3 < l - j2; ++j3)
                    {
                        i3 = par1World.getTypeId(par3, par4 + j3, par5);

                        if (i3 == 0 || i3 == Block.leaves.blockID)
                        {
                            this.setTypeAndData(par1World, par3, par4 + j3, par5, Block.wood.blockID, 1);
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
