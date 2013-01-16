package net.minecraft.potion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import net.minecraft.entity.projectile.EntityPotion;
// CraftBukkit end
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StringUtils;

public class Potion
{
    /** The array of potion types. */
    public static final Potion[] potionTypes = new Potion[32];
    public static final Potion field_76423_b = null;
    public static final Potion moveSpeed = (new Potion(1, false, 8171462)).setPotionName("potion.moveSpeed").setIconIndex(0, 0);
    public static final Potion moveSlowdown = (new Potion(2, true, 5926017)).setPotionName("potion.moveSlowdown").setIconIndex(1, 0);
    public static final Potion digSpeed = (new Potion(3, false, 14270531)).setPotionName("potion.digSpeed").setIconIndex(2, 0).setEffectiveness(1.5D);
    public static final Potion digSlowdown = (new Potion(4, true, 4866583)).setPotionName("potion.digSlowDown").setIconIndex(3, 0);
    public static final Potion damageBoost = (new Potion(5, false, 9643043)).setPotionName("potion.damageBoost").setIconIndex(4, 0);
    public static final Potion heal = (new PotionHealth(6, false, 16262179)).setPotionName("potion.heal");
    public static final Potion harm = (new PotionHealth(7, true, 4393481)).setPotionName("potion.harm");
    public static final Potion jump = (new Potion(8, false, 7889559)).setPotionName("potion.jump").setIconIndex(2, 1);
    public static final Potion confusion = (new Potion(9, true, 5578058)).setPotionName("potion.confusion").setIconIndex(3, 1).setEffectiveness(0.25D);

    /** The regeneration Potion object. */
    public static final Potion regeneration = (new Potion(10, false, 13458603)).setPotionName("potion.regeneration").setIconIndex(7, 0).setEffectiveness(0.25D);
    public static final Potion resistance = (new Potion(11, false, 10044730)).setPotionName("potion.resistance").setIconIndex(6, 1);

    /** The fire resistance Potion object. */
    public static final Potion fireResistance = (new Potion(12, false, 14981690)).setPotionName("potion.fireResistance").setIconIndex(7, 1);

    /** The water breathing Potion object. */
    public static final Potion waterBreathing = (new Potion(13, false, 3035801)).setPotionName("potion.waterBreathing").setIconIndex(0, 2);

    /** The invisibility Potion object. */
    public static final Potion invisibility = (new Potion(14, false, 8356754)).setPotionName("potion.invisibility").setIconIndex(0, 1);

    /** The blindness Potion object. */
    public static final Potion blindness = (new Potion(15, true, 2039587)).setPotionName("potion.blindness").setIconIndex(5, 1).setEffectiveness(0.25D);

    /** The night vision Potion object. */
    public static final Potion nightVision = (new Potion(16, false, 2039713)).setPotionName("potion.nightVision").setIconIndex(4, 1);

    /** The hunger Potion object. */
    public static final Potion hunger = (new Potion(17, true, 5797459)).setPotionName("potion.hunger").setIconIndex(1, 1);

    /** The weakness Potion object. */
    public static final Potion weakness = (new Potion(18, true, 4738376)).setPotionName("potion.weakness").setIconIndex(5, 0);

    /** The poison Potion object. */
    public static final Potion poison = (new Potion(19, true, 5149489)).setPotionName("potion.poison").setIconIndex(6, 0).setEffectiveness(0.25D);

    /** The wither Potion object. */
    public static final Potion wither = (new Potion(20, true, 3484199)).setPotionName("potion.wither").setIconIndex(1, 2).setEffectiveness(0.25D);
    public static final Potion field_76434_w = null;
    public static final Potion field_76444_x = null;
    public static final Potion field_76443_y = null;
    public static final Potion field_76442_z = null;
    public static final Potion field_76409_A = null;
    public static final Potion field_76410_B = null;
    public static final Potion field_76411_C = null;
    public static final Potion field_76405_D = null;
    public static final Potion field_76406_E = null;
    public static final Potion field_76407_F = null;
    public static final Potion field_76408_G = null;

    /** The Id of a Potion object. */
    public final int id;

    /** The name of the Potion. */
    private String name = "";

    /** The index for the icon displayed when the potion effect is active. */
    private int statusIconIndex = -1;

    /**
     * This field indicated if the effect is 'bad' - negative - for the entity.
     */
    private final boolean isBadEffect;
    private double effectiveness;
    private boolean usable;

    /** Is the color of the liquid for this potion. */
    private final int liquidColor;

    protected Potion(int par1, boolean par2, int par3)
    {
        this.id = par1;
        potionTypes[par1] = this;
        this.isBadEffect = par2;

        if (par2)
        {
            this.effectiveness = 0.5D;
        }
        else
        {
            this.effectiveness = 1.0D;
        }

        this.liquidColor = par3;
        org.bukkit.potion.PotionEffectType.registerPotionEffectType(new org.bukkit.craftbukkit.potion.CraftPotionEffectType(this)); // CraftBukkit
    }

    /**
     * Sets the index for the icon displayed in the player's inventory when the status is active.
     */
    protected Potion setIconIndex(int par1, int par2)
    {
        this.statusIconIndex = par1 + par2 * 8;
        return this;
    }

    /**
     * returns the ID of the potion
     */
    public int getId()
    {
        return this.id;
    }

    public void performEffect(EntityLiving par1EntityLiving, int par2)
    {
        if (this.id == regeneration.id)
        {
            if (par1EntityLiving.getHealth() < par1EntityLiving.maxHealth)   // CraftBukkit - .getMaxHealth() -> .maxHealth
            {
                par1EntityLiving.heal(1, RegainReason.MAGIC_REGEN); // CraftBukkit
            }
        }
        else if (this.id == poison.id)
        {
            if (par1EntityLiving.getHealth() > 1)
            {
                // CraftBukkit start
                EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(null, par1EntityLiving, EntityDamageEvent.DamageCause.POISON, 1);

                if (!event.isCancelled() && event.getDamage() > 0)
                {
                    par1EntityLiving.attackEntityFrom(DamageSource.magic, event.getDamage());
                }

                // CraftBukkit end
            }
        }
        else if (this.id == wither.id)
        {
            // CraftBukkit start
            EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(null, par1EntityLiving, EntityDamageEvent.DamageCause.WITHER, 1);

            if (!event.isCancelled() && event.getDamage() > 0)
            {
                par1EntityLiving.attackEntityFrom(DamageSource.wither, event.getDamage());
            }

            // CraftBukkit end
        }
        else if (this.id == hunger.id && par1EntityLiving instanceof EntityPlayer)
        {
            ((EntityPlayer)par1EntityLiving).addExhaustion(0.025F * (float)(par2 + 1));
        }
        else if ((this.id != heal.id || par1EntityLiving.isEntityUndead()) && (this.id != harm.id || !par1EntityLiving.isEntityUndead()))
        {
            if (this.id == harm.id && !par1EntityLiving.isEntityUndead() || this.id == heal.id && par1EntityLiving.isEntityUndead())
            {
                // CraftBukkit start
                EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(null, par1EntityLiving, EntityDamageEvent.DamageCause.MAGIC, 6 << par2);

                if (!event.isCancelled() && event.getDamage() > 0)
                {
                    par1EntityLiving.attackEntityFrom(DamageSource.magic, event.getDamage());
                }

                // CraftBukkit end
            }
        }
        else
        {
            par1EntityLiving.heal(6 << par2, RegainReason.MAGIC); // CraftBukkit
        }
    }

    /**
     * Hits the provided entity with this potion's instant effect.
     */
    public void affectEntity(EntityLiving par1EntityLiving, EntityLiving par2EntityLiving, int par3, double par4)
    {
        // CraftBukkit start - delegate; we need EntityPotion
        applyInstantEffect(par1EntityLiving, par2EntityLiving, par3, par4, null);
    }

    public void applyInstantEffect(EntityLiving entityliving, EntityLiving entityliving1, int i, double d0, EntityPotion potion)
    {
        // CraftBukkit end
        int j;

        if ((this.id != heal.id || entityliving1.isEntityUndead()) && (this.id != harm.id || !entityliving1.isEntityUndead()))
        {
            if (this.id == harm.id && !entityliving1.isEntityUndead() || this.id == heal.id && entityliving1.isEntityUndead())
            {
                j = (int)(d0 * (double)(6 << i) + 0.5D);

                if (entityliving == null)
                {
                    entityliving1.attackEntityFrom(DamageSource.magic, j);
                }
                else
                {
                    // CraftBukkit - The "damager" needs to be the potion
                    entityliving1.attackEntityFrom(DamageSource.causeIndirectMagicDamage(potion != null ? potion : entityliving1, entityliving), j);
                }
            }
        }
        else
        {
            j = (int)(d0 * (double)(6 << i) + 0.5D);
            entityliving1.heal(j, RegainReason.MAGIC); // CraftBukkit
        }
    }

    /**
     * Returns true if the potion has an instant effect instead of a continuous one (eg Harming)
     */
    public boolean isInstant()
    {
        return false;
    }

    /**
     * checks if Potion effect is ready to be applied this tick.
     */
    public boolean isReady(int par1, int par2)
    {
        int var3;

        if (this.id != regeneration.id && this.id != poison.id)
        {
            if (this.id == wither.id)
            {
                var3 = 40 >> par2;
                return var3 > 0 ? par1 % var3 == 0 : true;
            }
            else
            {
                return this.id == hunger.id;
            }
        }
        else
        {
            var3 = 25 >> par2;
            return var3 > 0 ? par1 % var3 == 0 : true;
        }
    }

    /**
     * Set the potion name.
     */
    public Potion setPotionName(String par1Str)
    {
        this.name = par1Str;
        return this;
    }

    /**
     * returns the name of the potion
     */
    public String getName()
    {
        return this.name;
    }

    protected Potion setEffectiveness(double par1)
    {
        this.effectiveness = par1;
        return this;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns true if the potion has a associated status icon to display in then inventory when active.
     */
    public boolean hasStatusIcon()
    {
        return this.statusIconIndex >= 0;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns the index for the icon to display when the potion is active.
     */
    public int getStatusIconIndex()
    {
        return this.statusIconIndex;
    }

    @SideOnly(Side.CLIENT)

    /**
     * This method returns true if the potion effect is bad - negative - for the entity.
     */
    public boolean isBadEffect()
    {
        return this.isBadEffect;
    }

    @SideOnly(Side.CLIENT)
    public static String getDurationString(PotionEffect par0PotionEffect)
    {
        int var1 = par0PotionEffect.getDuration();
        return StringUtils.ticksToElapsedTime(var1);
    }

    public double getEffectiveness()
    {
        return this.effectiveness;
    }

    public boolean isUsable()
    {
        return this.usable;
    }

    /**
     * Returns the color of the potion liquid.
     */
    public int getLiquidColor()
    {
        return this.liquidColor;
    }
}
