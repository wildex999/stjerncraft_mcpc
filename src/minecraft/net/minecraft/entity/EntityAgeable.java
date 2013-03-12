package net.minecraft.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityAgeable extends EntityCreature
{
    public boolean ageLocked = false; // CraftBukkit

    public EntityAgeable(World par1World)
    {
        super(par1World);
    }

    public abstract EntityAgeable createChild(EntityAgeable entityageable);

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer par1EntityPlayer)
    {
        ItemStack itemstack = par1EntityPlayer.inventory.getCurrentItem();

        if (itemstack != null && itemstack.itemID == Item.monsterPlacer.itemID && !this.worldObj.isRemote)
        {
            Class oclass = EntityList.getClassFromID(itemstack.getItemDamage());

            if (oclass != null && oclass.isAssignableFrom(this.getClass()))
            {
                EntityAgeable entityageable = this.createChild(this);

                if (entityageable != null)
                {
                    entityageable.setGrowingAge(-24000);
                    entityageable.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
                    this.worldObj.addEntity(entityageable, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG); // CraftBukkit

                    if (!par1EntityPlayer.capabilities.isCreativeMode)
                    {
                        --itemstack.stackSize;

                        if (itemstack.stackSize == 0)   // CraftBukkit - allow less than 0 stacks as "infinit"
                        {
                            par1EntityPlayer.inventory.setInventorySlotContents(par1EntityPlayer.inventory.currentItem, (ItemStack)null);
                        }
                    }
                }
            }
        }

        return super.interact(par1EntityPlayer);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataWatcher.addObject(12, new Integer(0));
    }

    /**
     * The age value may be negative or positive or zero. If it's negative, it get's incremented on each tick, if it's
     * positive, it get's decremented each tick. Don't confuse this with EntityLiving.getAge. With a negative value the
     * Entity is considered a child.
     */
    public int getGrowingAge()
    {
        return this.dataWatcher.getWatchableObjectInt(12);
    }

    /**
     * The age value may be negative or positive or zero. If it's negative, it get's incremented on each tick, if it's
     * positive, it get's decremented each tick. With a negative value the Entity is considered a child.
     */
    public void setGrowingAge(int par1)
    {
        this.dataWatcher.updateObject(12, Integer.valueOf(par1));
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setInteger("Age", this.getGrowingAge());
        par1NBTTagCompound.setBoolean("AgeLocked", this.ageLocked); // CraftBukkit
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);
        this.setGrowingAge(par1NBTTagCompound.getInteger("Age"));
        this.ageLocked = par1NBTTagCompound.getBoolean("AgeLocked"); // CraftBukkit
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        int i = this.getGrowingAge();

        if (ageLocked)
        {
            return;    // CraftBukkit
        }

        if (i < 0)
        {
            ++i;
            this.setGrowingAge(i);
        }
        else if (i > 0)
        {
            --i;
            this.setGrowingAge(i);
        }
    }

    /**
     * If Animal, checks if the age timer is negative
     */
    public boolean isChild()
    {
        return this.getGrowingAge() < 0;
    }
}
