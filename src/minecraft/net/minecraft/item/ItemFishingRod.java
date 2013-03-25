package net.minecraft.item;

import org.bukkit.event.player.PlayerFishEvent; // CraftBukkit
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.world.World;

public class ItemFishingRod extends Item
{
    public ItemFishingRod(int par1)
    {
        super(par1);
        this.setMaxDamage(64);
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.tabTools);
    }

    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        if (par3EntityPlayer.fishEntity != null)
        {
            int i = par3EntityPlayer.fishEntity.catchFish();
            par1ItemStack.damageItem(i, par3EntityPlayer);
            par3EntityPlayer.swingItem();
        }
        else
        {
            // CraftBukkit start
            EntityFishHook hook = new EntityFishHook(par2World, par3EntityPlayer);
            PlayerFishEvent playerFishEvent = new PlayerFishEvent((org.bukkit.entity.Player) par3EntityPlayer.getBukkitEntity(), null, (org.bukkit.entity.Fish) hook.getBukkitEntity(), PlayerFishEvent.State.FISHING);
            par2World.getServer().getPluginManager().callEvent(playerFishEvent);

            if (playerFishEvent.isCancelled())
            {
                return par1ItemStack;
            }

            // CraftBukkit end
            par2World.playSoundAtEntity(par3EntityPlayer, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

            if (!par2World.isRemote)
            {
                par2World.spawnEntityInWorld(hook); // CraftBukkit - moved creation up
            }

            par3EntityPlayer.swingItem();
        }

        return par1ItemStack;
    }
}
