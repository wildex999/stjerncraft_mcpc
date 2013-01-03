package net.minecraft.entity.projectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
// CraftBukkit start
import java.util.HashMap;

import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
// CraftBukkit end

public class EntityPotion extends EntityThrowable
{
    /**
     * The damage value of the thrown potion that this EntityPotion represents.
     */
    private ItemStack potionDamage;

    public EntityPotion(World par1World)
    {
        super(par1World);
    }

    public EntityPotion(World par1World, EntityLiving par2EntityLiving, int par3)
    {
        this(par1World, par2EntityLiving, new ItemStack(Item.potion, 1, par3));
    }

    public EntityPotion(World par1World, EntityLiving par2EntityLiving, ItemStack par3ItemStack)
    {
        super(par1World, par2EntityLiving);
        this.potionDamage = par3ItemStack;
    }

    @SideOnly(Side.CLIENT)
    public EntityPotion(World par1World, double par2, double par4, double par6, int par8)
    {
        this(par1World, par2, par4, par6, new ItemStack(Item.potion, 1, par8));
    }

    public EntityPotion(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack)
    {
        super(par1World, par2, par4, par6);
        this.potionDamage = par8ItemStack;
    }

    /**
     * Gets the amount of gravity to apply to the thrown entity with each tick.
     */
    protected float getGravityVelocity()
    {
        return 0.05F;
    }

    protected float func_70182_d()
    {
        return 0.5F;
    }

    protected float func_70183_g()
    {
        return -20.0F;
    }

    public void setPotionDamage(int par1)
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        this.potionDamage.setItemDamage(par1);
    }

    /**
     * Returns the damage value of the thrown potion that this EntityPotion represents.
     */
    public int getPotionDamage()
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        return this.potionDamage.getItemDamage();
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    protected void onImpact(MovingObjectPosition par1MovingObjectPosition)
    {
        if (!this.worldObj.isRemote)
        {
            List var2 = Item.potion.getEffects(this.potionDamage);

            if (var2 != null && !var2.isEmpty())
            {
                AxisAlignedBB var3 = this.boundingBox.expand(4.0D, 2.0D, 4.0D);
                List var4 = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, var3);

                if (var4 != null)   // CraftBukkit - Run code even if there are no entities around
                {
                    Iterator var5 = var4.iterator();
                    // CraftBukkit
                    HashMap<LivingEntity, Double> var6 = new HashMap<LivingEntity, Double>();

                    while (var5.hasNext())
                    {
                        EntityLiving var7 = (EntityLiving) var5.next();
                        double var9 = this.getDistanceSqToEntity(var7);

                        if (var9 < 16.0D)
                        {
                            double var11 = 1.0D - Math.sqrt(var9) / 4.0D;

                            if (var7 == par1MovingObjectPosition.entityHit)
                            {
                                var11 = 1.0D;
                            }

                            // CraftBukkit start
                            var6.put((LivingEntity) var7.getBukkitEntity(), var11);
                        }
                    }

                    org.bukkit.event.entity.PotionSplashEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPotionSplashEvent(this, var6);

                    if (!event.isCancelled())
                    {
                        for (LivingEntity victim : event.getAffectedEntities())
                        {
                            if (!(victim instanceof CraftLivingEntity))
                            {
                                continue;
                            }

                            EntityLiving entityliving = ((CraftLivingEntity) victim).getHandle();
                            double d1 = event.getIntensity(victim);
                            // CraftBukkit end
                            Iterator var12 = var2.iterator();

                            while (var12.hasNext())
                            {
                                PotionEffect var13 = (PotionEffect) var12.next();
                                int var14 = var13.getPotionID();

                                // CraftBukkit start - abide by PVP settings - for players only!
                                if (!this.worldObj.pvpMode && this.getThrower() instanceof EntityPlayerMP && entityliving instanceof EntityPlayerMP && entityliving != this.getThrower())
                                {
                                    // Block SLOWER_MOVEMENT, SLOWER_DIG, HARM, BLINDNESS, HUNGER, WEAKNESS and POISON potions
                                    if (var14 == 2 || var14 == 4 || var14 == 7 || var14 == 15 || var14 == 17 || var14 == 18 || var14 == 19)
                                    {
                                        continue;
                                    }
                                }

                                // CraftBukkit end

                                if (Potion.potionTypes[var14].isInstant())
                                {
                                    // CraftBukkit - added 'this'
                                    Potion.potionTypes[var14].applyInstantEffect(this.getThrower(), entityliving, var13.getAmplifier(), d1, this);
                                }
                                else
                                {
                                    int j = (int)(d1 * (double) var13.getDuration() + 0.5D);

                                    if (j > 20)
                                    {
                                        entityliving.addPotionEffect(new PotionEffect(var14, j, var13.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.worldObj.playAuxSFX(2002, (int)Math.round(this.posX), (int)Math.round(this.posY), (int)Math.round(this.posZ), this.getPotionDamage());
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);

        if (par1NBTTagCompound.hasKey("Potion"))
        {
            this.potionDamage = ItemStack.loadItemStackFromNBT(par1NBTTagCompound.getCompoundTag("Potion"));
        }
        else
        {
            this.setPotionDamage(par1NBTTagCompound.getInteger("potionValue"));
        }

        if (this.potionDamage == null)
        {
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);

        if (this.potionDamage != null)
        {
            par1NBTTagCompound.setCompoundTag("Potion", this.potionDamage.writeToNBT(new NBTTagCompound()));
        }
    }
}
