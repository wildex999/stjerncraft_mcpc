package net.minecraft.util;

import org.bukkit.event.entity.EntityDamageEvent; // CraftBukkit
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet8UpdateHealth;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.nbt.NBTTagCompound;

public class FoodStats
{
    // CraftBukkit start - all made public
    /** The player's food level. */
    public int foodLevel = 20;

    /** The player's food saturation. */
    public float foodSaturationLevel = 5.0F;


    /** The player's food exhaustion. */
    public float foodExhaustionLevel;

    /** The player's food timer value. */
    public int foodTimer = 0;
    // CraftBukkit end
    private int prevFoodLevel = 20;

    /**
     * Args: int foodLevel, float foodSaturationModifier
     */
    public void addStats(int par1, float par2)
    {
        this.foodLevel = Math.min(par1 + this.foodLevel, 20);
        this.foodSaturationLevel = Math.min(this.foodSaturationLevel + (float)par1 * par2 * 2.0F, (float)this.foodLevel);
    }

    /**
     * Eat some food.
     */
    public void addStats(ItemFood par1ItemFood)
    {
        this.addStats(par1ItemFood.getHealAmount(), par1ItemFood.getSaturationModifier());
    }

    /**
     * Handles the food game logic.
     */
    public void onUpdate(EntityPlayer par1EntityPlayer)
    {
        int i = par1EntityPlayer.worldObj.difficultySetting;
        this.prevFoodLevel = this.foodLevel;

        if (this.foodExhaustionLevel > 4.0F)
        {
            this.foodExhaustionLevel -= 4.0F;

            if (this.foodSaturationLevel > 0.0F)
            {
                this.foodSaturationLevel = Math.max(this.foodSaturationLevel - 1.0F, 0.0F);
            }
            else if (i > 0)
            {
                // CraftBukkit start
                org.bukkit.event.entity.FoodLevelChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callFoodLevelChangeEvent(par1EntityPlayer, Math.max(this.foodLevel - 1, 0));

                if (!event.isCancelled())
                {
                    this.foodLevel = event.getFoodLevel();
                }

                ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(new Packet8UpdateHealth(par1EntityPlayer.getHealth(), this.foodLevel, this.foodSaturationLevel));
                // CraftBukkit end
            }
        }

        if (this.foodLevel >= 18 && par1EntityPlayer.shouldHeal())
        {
            ++this.foodTimer;

            if (this.foodTimer >= 80)
            {
                // CraftBukkit - added RegainReason
                par1EntityPlayer.heal(1, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED);
                this.foodTimer = 0;
            }
        }
        else if (this.foodLevel <= 0)
        {
            ++this.foodTimer;

            if (this.foodTimer >= 80)
            {
                if (par1EntityPlayer.getHealth() > 10 || i >= 3 || par1EntityPlayer.getHealth() > 1 && i >= 2)
                {
                    // CraftBukkit start
                    EntityDamageEvent event = new EntityDamageEvent(par1EntityPlayer.getBukkitEntity(), EntityDamageEvent.DamageCause.STARVATION, 1);
                    par1EntityPlayer.worldObj.getServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled())
                    {
                        event.getEntity().setLastDamageCause(event);
                        par1EntityPlayer.attackEntityFrom(DamageSource.starve, event.getDamage());
                    }

                    // CraftBukkit end
                }

                this.foodTimer = 0;
            }
        }
        else
        {
            this.foodTimer = 0;
        }
    }

    /**
     * Reads food stats from an NBT object.
     */
    public void readNBT(NBTTagCompound par1NBTTagCompound)
    {
        if (par1NBTTagCompound.hasKey("foodLevel"))
        {
            this.foodLevel = par1NBTTagCompound.getInteger("foodLevel");
            this.foodTimer = par1NBTTagCompound.getInteger("foodTickTimer");
            this.foodSaturationLevel = par1NBTTagCompound.getFloat("foodSaturationLevel");
            this.foodExhaustionLevel = par1NBTTagCompound.getFloat("foodExhaustionLevel");
        }
    }

    /**
     * Writes food stats to an NBT object.
     */
    public void writeNBT(NBTTagCompound par1NBTTagCompound)
    {
        par1NBTTagCompound.setInteger("foodLevel", this.foodLevel);
        par1NBTTagCompound.setInteger("foodTickTimer", this.foodTimer);
        par1NBTTagCompound.setFloat("foodSaturationLevel", this.foodSaturationLevel);
        par1NBTTagCompound.setFloat("foodExhaustionLevel", this.foodExhaustionLevel);
    }

    /**
     * Get the player's food level.
     */
    public int getFoodLevel()
    {
        return this.foodLevel;
    }

    @SideOnly(Side.CLIENT)
    public int getPrevFoodLevel()
    {
        return this.prevFoodLevel;
    }

    /**
     * If foodLevel is not max.
     */
    public boolean needFood()
    {
        return this.foodLevel < 20;
    }

    /**
     * adds input to foodExhaustionLevel to a max of 40
     */
    public void addExhaustion(float par1)
    {
        this.foodExhaustionLevel = Math.min(this.foodExhaustionLevel + par1, 40.0F);
    }

    /**
     * Get the player's food saturation level.
     */
    public float getSaturationLevel()
    {
        return this.foodSaturationLevel;
    }

    @SideOnly(Side.CLIENT)
    public void setFoodLevel(int par1)
    {
        this.foodLevel = par1;
    }

    @SideOnly(Side.CLIENT)
    public void setFoodSaturationLevel(float par1)
    {
        this.foodSaturationLevel = par1;
    }
}
