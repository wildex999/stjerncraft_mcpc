package net.minecraft.item;

// CraftBukkit start
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
// CraftBukkit end

public class ItemFlintAndSteel extends Item
{
    public ItemFlintAndSteel(int par1)
    {
        super(par1);
        this.maxStackSize = 1;
        this.setMaxDamage(64);
        this.setCreativeTab(CreativeTabs.tabTools);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int clickedX = par4, clickedY = par5, clickedZ = par6; // CraftBukkit

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

        if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else
        {
            if (par3World.isAirBlock(par4, par5, par6))
            {
                // CraftBukkit start - Store the clicked block
                if (CraftEventFactory.callBlockIgniteEvent(par3World, par4, par5, par6, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, par2EntityPlayer).isCancelled())
                {
                    par1ItemStack.damageItem(1, par2EntityPlayer);
                    return false;
                }

                CraftBlockState blockState = CraftBlockState.getBlockState(par3World, par4, par5, par6);
                // CraftBukkit end
                par3World.playSoundEffect((double)par4 + 0.5D, (double)par5 + 0.5D, (double)par6 + 0.5D, "fire.ignite", 1.0F, itemRand.nextFloat() * 0.4F + 0.8F);
                par3World.setBlock(par4, par5, par6, Block.fire.blockID);
                // CraftBukkit start
                org.bukkit.event.block.BlockPlaceEvent placeEvent = CraftEventFactory.callBlockPlaceEvent(par3World, par2EntityPlayer, blockState, clickedX, clickedY, clickedZ);

                if (placeEvent.isCancelled() || !placeEvent.canBuild())
                {
                    placeEvent.getBlockPlaced().setTypeIdAndData(0, (byte) 0, false);
                    return false;
                }

                // CraftBukkit end
            }

            par1ItemStack.damageItem(1, par2EntityPlayer);
            return true;
        }
    }
}
