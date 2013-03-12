package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockFadeEvent;
// CraftBukkit end

public class BlockGrass extends Block
{
    protected BlockGrass(int par1)
    {
        super(par1, Material.grass);
        this.blockIndexInTexture = 3;
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public int getBlockTextureFromSideAndMetadata(int par1, int par2)
    {
        return par1 == 1 ? 0 : (par1 == 0 ? 2 : 3);
    }

    @SideOnly(Side.CLIENT)

    /**
     * Retrieves the block texture to use based on the display side. Args: iBlockAccess, x, y, z, side
     */
    public int getBlockTexture(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        if (par5 == 1)
        {
            return 0;
        }
        else if (par5 == 0)
        {
            return 2;
        }
        else
        {
            Material material = par1IBlockAccess.getBlockMaterial(par2, par3 + 1, par4);
            return material != Material.snow && material != Material.craftedSnow ? 3 : 68;
        }
    }

    @SideOnly(Side.CLIENT)
    public int getBlockColor()
    {
        double d0 = 0.5D;
        double d1 = 1.0D;
        return ColorizerGrass.getGrassColor(d0, d1);
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns the color this block should be rendered. Used by leaves.
     */
    public int getRenderColor(int par1)
    {
        return this.getBlockColor();
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(IBlockAccess par1IBlockAccess, int par2, int par3, int par4)
    {
        int l = 0;
        int i1 = 0;
        int j1 = 0;

        for (int k1 = -1; k1 <= 1; ++k1)
        {
            for (int l1 = -1; l1 <= 1; ++l1)
            {
                int i2 = par1IBlockAccess.getBiomeGenForCoords(par2 + l1, par4 + k1).getBiomeGrassColor();
                l += (i2 & 16711680) >> 16;
                i1 += (i2 & 65280) >> 8;
                j1 += i2 & 255;
            }
        }

        return (l / 9 & 255) << 16 | (i1 / 9 & 255) << 8 | j1 / 9 & 255;
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (!par1World.isRemote)
        {
            if (par1World.getBlockLightValue(par2, par3 + 1, par4) < 4 && par1World.getBlockLightOpacity(par2, par3 + 1, par4) > 2)
            {
                // CraftBukkit start
                org.bukkit.World bworld = par1World.getWorld();
                BlockState blockState = bworld.getBlockAt(par2, par3, par4).getState();
                blockState.setTypeId(Block.dirt.blockID);
                BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
                par1World.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    blockState.update(true);
                }

                // CraftBukkit end
            }
            else if (par1World.getBlockLightValue(par2, par3 + 1, par4) >= 9)
            {
                int l = Math.min(4, Math.max(20, (int)(4 * 100F / par1World.growthOdds)));  // Spigot

                for (int i1 = 0; i1 < l; ++i1)   // Spigot
                {
                    int j1 = par2 + par5Random.nextInt(3) - 1;
                    int k1 = par3 + par5Random.nextInt(5) - 3;
                    int l1 = par4 + par5Random.nextInt(3) - 1;
                    int l1 = par1World.getBlockId(j1, k1 + 1, l1);

                    if (par1World.getBlockId(j1, k1, l1) == Block.dirt.blockID && par1World.getBlockLightValue(j1, k1 + 1, l1) >= 4 && par1World.getBlockLightOpacity(j1, k1 + 1, l1) <= 2)   // Forge
                    {
                        // CraftBukkit start
                        org.bukkit.World bworld = par1World.getWorld();
                        BlockState blockState = bworld.getBlockAt(j1, k1, l1).getState();
                        blockState.setTypeId(Block.grass.blockID);
                        BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(par2, par3, par4), blockState);
                        par1World.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            blockState.update(true);
                        }

                        // CraftBukkit end
                    }
                }
            }
        }
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return Block.dirt.idDropped(0, par2Random, par3);
    }
}
