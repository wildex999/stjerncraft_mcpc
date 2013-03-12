package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.world.World;
import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockCommandBlock extends BlockContainer
{
    public BlockCommandBlock(int par1)
    {
        super(par1, 184, Material.iron);
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    public TileEntity createNewTileEntity(World par1World)
    {
        return new TileEntityCommandBlock();
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World par1World, int par2, int par3, int par4, int par5)
    {
        if (!par1World.isRemote)
        {
            boolean flag = par1World.isBlockIndirectlyGettingPowered(par2, par3, par4);
            int i1 = par1World.getBlockMetadata(par2, par3, par4);
            boolean flag1 = (i1 & 1) != 0;
            // CraftBukkit start
            org.bukkit.block.Block block = par1World.getWorld().getBlockAt(par2, par3, par4);
            int old = flag1 ? 1 : 0;
            int current = flag ? 1 : 0;
            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, old, current);
            par1World.getServer().getPluginManager().callEvent(eventRedstone);
            // CraftBukkit end

            if (eventRedstone.getNewCurrent() > 0 && !(eventRedstone.getOldCurrent() > 0))   // CraftBukkit
            {
                par1World.setBlockMetadata(par2, par3, par4, i1 | 1);
                par1World.scheduleBlockUpdate(par2, par3, par4, this.blockID, this.tickRate());
            }
            else if (!(eventRedstone.getNewCurrent() > 0) && eventRedstone.getOldCurrent() > 0)     // CraftBukkit
            {
                par1World.setBlockMetadata(par2, par3, par4, i1 & -2);
            }
        }
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        TileEntity tileentity = par1World.getBlockTileEntity(par2, par3, par4);

        if (tileentity != null && tileentity instanceof TileEntityCommandBlock)
        {
            ((TileEntityCommandBlock)tileentity).executeCommandOnPowered(par1World);
        }
    }

    /**
     * How many world ticks before ticking
     */
    public int tickRate()
    {
        return 1;
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9)
    {
        TileEntityCommandBlock tileentitycommandblock = (TileEntityCommandBlock)par1World.getBlockTileEntity(par2, par3, par4);

        if (tileentitycommandblock != null)
        {
            par5EntityPlayer.displayGUIEditSign(tileentitycommandblock);
        }

        return true;
    }
}
