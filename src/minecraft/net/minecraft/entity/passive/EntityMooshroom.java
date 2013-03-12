package net.minecraft.entity.passive;

import org.bukkit.event.player.PlayerShearEntityEvent; // CraftBukkit
import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;

import net.minecraftforge.common.IShearable;

public class EntityMooshroom extends EntityCow implements IShearable
{
    EntityPlayer shearer = null; // MCPC

    public EntityMooshroom(World par1World)
    {
        super(par1World);
        this.texture = "/mob/redcow.png";
        this.setSize(0.9F, 1.3F);
    }

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer par1EntityPlayer)
    {
        ItemStack itemstack = par1EntityPlayer.inventory.getCurrentItem();

        if (itemstack != null && itemstack.itemID == Item.bowlEmpty.itemID && this.getGrowingAge() >= 0)
        {
            if (itemstack.stackSize == 1)
            {
                par1EntityPlayer.inventory.setInventorySlotContents(par1EntityPlayer.inventory.currentItem, new ItemStack(Item.bowlSoup));
                return true;
            }

            if (par1EntityPlayer.inventory.addItemStackToInventory(new ItemStack(Item.bowlSoup)) && !par1EntityPlayer.capabilities.isCreativeMode)
            {
                par1EntityPlayer.inventory.decrStackSize(par1EntityPlayer.inventory.currentItem, 1);
                return true;
            }
        }

        shearer = par1EntityPlayer; // MCPC
        return super.interact(par1EntityPlayer); // Forge
    }

    /**
     * This function is used when two same-species animals in 'love mode' breed to generate the new baby animal.
     */
    public EntityMooshroom spawnBabyAnimal(EntityAgeable par1EntityAgeable)
    {
        return new EntityMooshroom(this.worldObj);
    }

    public EntityAgeable createChild(EntityAgeable par1EntityAgeable)
    {
        return this.spawnBabyAnimal(par1EntityAgeable);
    }

    @Override
    public boolean isShearable(ItemStack item, World world, int X, int Y, int Z)
    {
        return getGrowingAge() >= 0;
    }

    @Override
    public ArrayList<ItemStack> onSheared(ItemStack item, World world, int X, int Y, int Z, int fortune)
    {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        // CraftBukkit start
        PlayerShearEntityEvent event = new PlayerShearEntityEvent((org.bukkit.entity.Player) shearer.getBukkitEntity(), this.getBukkitEntity());
        this.worldObj.getServer().getPluginManager().callEvent(event);
        shearer = null;

        if (event.isCancelled())
        {
            return ret;
        }
        // CraftBukkit end
        setDead();
        EntityCow entitycow = new EntityCow(worldObj);
        entitycow.setLocationAndAngles(posX, posY, posZ, rotationYaw, rotationPitch);
        entitycow.setEntityHealth(getHealth());
        entitycow.renderYawOffset = renderYawOffset;
        worldObj.spawnEntityInWorld(entitycow);
        worldObj.spawnParticle("largeexplode", posX, posY + (double)(height / 2.0F), posZ, 0.0D, 0.0D, 0.0D);

        for (int x = 0; x < 5; x++)
        {
            ret.add(new ItemStack(Block.mushroomRed));
        }
        return ret;
    }
}
