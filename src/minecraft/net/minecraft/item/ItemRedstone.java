package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.block.CraftBlockState; // CraftBukkit

public class ItemRedstone extends Item
{
    public ItemRedstone(int par1)
    {
        super(par1);
        this.setCreativeTab(CreativeTabs.tabRedstone);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int clickedX = par4, clickedY = par5, clickedZ = par6; // CraftBukkit

        if (par3World.getBlockId(par4, par5, par6) != Block.snow.blockID)
        {
            if (par7 == 0)
            {
                --par5;
            }

            if (par7 == 1)
            {
                ++par5;
            }

            if (par7 == 2)
            {
                --par6;
            }

            if (par7 == 3)
            {
                ++par6;
            }

            if (par7 == 4)
            {
                --par4;
            }

            if (par7 == 5)
            {
                ++par4;
            }

            if (!par3World.isAirBlock(par4, par5, par6))
            {
                return false;
            }
        }

        if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else
        {
            if (Block.redstoneWire.canPlaceBlockAt(par3World, par4, par5, par6))
            {
                // CraftBukkit start
                CraftBlockState blockState = CraftBlockState.getBlockState(par3World, par4, par5, par6);
                par3World.editingBlocks = true;
                par3World.setBlock(par4, par5, par6, Block.redstoneWire.blockID); // We update after the event
                org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(par3World, par2EntityPlayer, blockState, clickedX, clickedY, clickedZ);
                blockState.update(true);
                par3World.editingBlocks = false;

                if (event.isCancelled() || !event.canBuild())
                {
                    return false;
                }

                // CraftBukkit end
                --par1ItemStack.stackSize;
                par3World.setBlockWithNotify(par4, par5, par6, Block.redstoneWire.blockID);
            }

            return true;
        }
    }
}
