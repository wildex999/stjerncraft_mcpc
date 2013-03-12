package net.minecraft.item;

// CraftBukkit start
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockIgniteEvent;
// CraftBukkit end
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemFireball extends Item
{
    public ItemFireball(int par1)
    {
        super(par1);
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        if (par3World.isRemote)
        {
            return true;
        }
        else
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

            if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
            {
                return false;
            }
            else
            {
                int i1 = par3World.getBlockId(par4, par5, par6);

                if (i1 == 0)
                {
                    // CraftBukkit start
                    org.bukkit.block.Block blockClicked = par3World.getWorld().getBlockAt(par4, par5, par6);
                    Player thePlayer = (Player) par2EntityPlayer.getBukkitEntity();
                    BlockIgniteEvent eventIgnite = new BlockIgniteEvent(blockClicked, BlockIgniteEvent.IgniteCause.FIREBALL, thePlayer);
                    par3World.getServer().getPluginManager().callEvent(eventIgnite);

                    if (eventIgnite.isCancelled())
                    {
                        if (!par2EntityPlayer.capabilities.isCreativeMode)
                        {
                            --par1ItemStack.stackSize;
                        }

                        return false;
                    }

                    // CraftBukkit end
                    par3World.playSoundEffect((double)par4 + 0.5D, (double)par5 + 0.5D, (double)par6 + 0.5D, "fire.ignite", 1.0F, itemRand.nextFloat() * 0.4F + 0.8F);
                    par3World.setBlockWithNotify(par4, par5, par6, Block.fire.blockID);
                }

                if (!par2EntityPlayer.capabilities.isCreativeMode)
                {
                    --par1ItemStack.stackSize;
                }

                return true;
            }
        }
    }
}
