package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling.TreeGenerator;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.bukkit.BlockChangeDelegate; // CraftBukkit

public class WorldGenSwamp extends WorldGenerator implements TreeGenerator   // CraftBukkit add interface
{
    public WorldGenSwamp() {}

    public boolean generate(World par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit start - Moved to generate
        return this.generate((BlockChangeDelegate) par1World, par2Random, par3, par4, par5);
    }

    public boolean generate(BlockChangeDelegate par1World, Random par2Random, int par3, int par4, int par5)
    {
        // CraftBukkit end
        int l;

        for (l = par2Random.nextInt(4) + 5; par1World.getTypeId(par3, par4 - 1, par5) != 0 && Block.blocksList[par1World.getTypeId(par3, par4 - 1, par5)].blockMaterial == Material.water; --par4)   // CraftBukkit - bypass world.getMaterial
        {
            ;
        }

        boolean flag = true;

        if (par4 >= 1 && par4 + l + 1 <= 128)
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
                    b0 = 3;
                }

                for (j1 = par3 - b0; j1 <= par3 + b0 && flag; ++j1)
                {
                    for (k1 = par5 - b0; k1 <= par5 + b0 && flag; ++k1)
                    {
                        if (i1 >= 0 && i1 < 128)
                        {
                            l1 = par1World.getTypeId(j1, i1, k1);

                            if (l1 != 0 && l1 != Block.leaves.blockID)
                            {
                                if (l1 != Block.waterStill.blockID && l1 != Block.waterMoving.blockID)
                                {
                                    flag = false;
                                }
                                else if (i1 > par4)
                                {
                                    flag = false;
                                }
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

                if ((i1 == Block.grass.blockID || i1 == Block.dirt.blockID) && par4 < 128 - l - 1)
                {
                    this.setType(par1World, par3, par4 - 1, par5, Block.dirt.blockID);
                    int i2;
                    int j2;

                    for (j2 = par4 - 3 + l; j2 <= par4 + l; ++j2)
                    {
                        j1 = j2 - (par4 + l);
                        k1 = 2 - j1 / 2;

                        for (l1 = par3 - k1; l1 <= par3 + k1; ++l1)
                        {
                            i2 = l1 - par3;

                            for (int k2 = par5 - k1; k2 <= par5 + k1; ++k2)
                            {
                                int l2 = k2 - par5;

                                if ((Math.abs(i2) != k1 || Math.abs(l2) != k1 || par2Random.nextInt(2) != 0 && j1 != 0) && !Block.opaqueCubeLookup[par1World.getTypeId(l1, j2, k2)])
                                {
                                    this.setType(par1World, l1, j2, k2, Block.leaves.blockID);
                                }
                            }
                        }
                    }

                    for (j2 = 0; j2 < l; ++j2)
                    {
                        j1 = par1World.getTypeId(par3, par4 + j2, par5);

                        if (j1 == 0 || j1 == Block.leaves.blockID || j1 == Block.waterMoving.blockID || j1 == Block.waterStill.blockID)
                        {
                            this.setType(par1World, par3, par4 + j2, par5, Block.wood.blockID);
                        }
                    }

                    for (j2 = par4 - 3 + l; j2 <= par4 + l; ++j2)
                    {
                        j1 = j2 - (par4 + l);
                        k1 = 2 - j1 / 2;

                        for (l1 = par3 - k1; l1 <= par3 + k1; ++l1)
                        {
                            for (i2 = par5 - k1; i2 <= par5 + k1; ++i2)
                            {
                                if (par1World.getTypeId(l1, j2, i2) == Block.leaves.blockID)
                                {
                                    if (par2Random.nextInt(4) == 0 && par1World.getTypeId(l1 - 1, j2, i2) == 0)
                                    {
                                        this.b(par1World, l1 - 1, j2, i2, 8);
                                    }

                                    if (par2Random.nextInt(4) == 0 && par1World.getTypeId(l1 + 1, j2, i2) == 0)
                                    {
                                        this.b(par1World, l1 + 1, j2, i2, 2);
                                    }

                                    if (par2Random.nextInt(4) == 0 && par1World.getTypeId(l1, j2, i2 - 1) == 0)
                                    {
                                        this.b(par1World, l1, j2, i2 - 1, 1);
                                    }

                                    if (par2Random.nextInt(4) == 0 && par1World.getTypeId(l1, j2, i2 + 1) == 0)
                                    {
                                        this.b(par1World, l1, j2, i2 + 1, 4);
                                    }
                                }
                            }
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

    // CraftBukkit - change signature
    private void b(BlockChangeDelegate world, int i, int j, int k, int l)
    {
        this.setTypeAndData(world, i, j, k, Block.vine.blockID, l);
        int i1 = 4;

        while (true)
        {
            --j;

            if (world.getTypeId(i, j, k) != 0 || i1 <= 0)
            {
                return;
            }

            this.setTypeAndData(world, i, j, k, Block.vine.blockID, l);
            --i1;
        }
    }
}
