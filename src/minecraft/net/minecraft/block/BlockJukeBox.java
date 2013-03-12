package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockJukeBox extends BlockContainer
{
    protected BlockJukeBox(int par1, int par2)
    {
        super(par1, par2, Material.wood);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    /**
     * Returns the block texture based on the side being looked at.  Args: side
     */
    public int getBlockTextureFromSide(int par1)
    {
        return this.blockIndexInTexture + (par1 == 1 ? 1 : 0);
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9)
    {
        if (par1World.getBlockMetadata(par2, par3, par4) == 0)
        {
            return false;
        }
        else
        {
            this.ejectRecord(par1World, par2, par3, par4);
            return true;
        }
    }

    /**
     * Insert the specified music disc in the jukebox at the given coordinates
     */
    public void insertRecord(World par1World, int par2, int par3, int par4, ItemStack par5ItemStack)
    {
        if (!par1World.isRemote)
        {
            TileEntityRecordPlayer tileentityrecordplayer = (TileEntityRecordPlayer)par1World.getBlockTileEntity(par2, par3, par4);

            if (tileentityrecordplayer != null)
            {
                tileentityrecordplayer.record = par5ItemStack.copy();
                tileentityrecordplayer.record.stackSize = 1; // CraftBukkit - There can be only one
                tileentityrecordplayer.onInventoryChanged();
                par1World.setBlockMetadataWithNotify(par2, par3, par4, 1);
            }
        }
    }

    /**
     * Ejects the current record inside of the jukebox.
     */
    public void ejectRecord(World par1World, int par2, int par3, int par4)
    {
        if (!par1World.isRemote)
        {
            TileEntityRecordPlayer tileentityrecordplayer = (TileEntityRecordPlayer)par1World.getBlockTileEntity(par2, par3, par4);

            if (tileentityrecordplayer != null)
            {
                ItemStack itemstack = tileentityrecordplayer.record;

                if (itemstack != null)
                {
                    par1World.playAuxSFX(1005, par2, par3, par4, 0);
                    par1World.playRecord((String)null, par2, par3, par4);
                    tileentityrecordplayer.record = null;
                    tileentityrecordplayer.onInventoryChanged();
                    par1World.setBlockMetadataWithNotify(par2, par3, par4, 0);
                    float f = 0.7F;
                    double d0 = (double)(par1World.rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                    double d1 = (double)(par1World.rand.nextFloat() * f) + (double)(1.0F - f) * 0.2D + 0.6D;
                    double d2 = (double)(par1World.rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                    ItemStack itemstack1 = itemstack.copy();
                    EntityItem entityitem = new EntityItem(par1World, (double)par2 + d0, (double)par3 + d1, (double)par4 + d2, itemstack1);
                    entityitem.delayBeforeCanPickup = 10;
                    par1World.spawnEntityInWorld(entityitem);
                }
            }
        }
    }

    /**
     * ejects contained items into the world, and notifies neighbours of an update, as appropriate
     */
    public void breakBlock(World par1World, int par2, int par3, int par4, int par5, int par6)
    {
        this.ejectRecord(par1World, par2, par3, par4);
        super.breakBlock(par1World, par2, par3, par4, par5, par6);
    }

    /**
     * Drops the block items with a specified chance of dropping the specified items
     */
    public void dropBlockAsItemWithChance(World par1World, int par2, int par3, int par4, int par5, float par6, int par7)
    {
        if (!par1World.isRemote)
        {
            super.dropBlockAsItemWithChance(par1World, par2, par3, par4, par5, par6, 0);
        }
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    public TileEntity createNewTileEntity(World par1World)
    {
        return new TileEntityRecordPlayer();
    }
}
