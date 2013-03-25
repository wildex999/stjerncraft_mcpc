package net.minecraft.entity.ai;

// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
// CraftBukkit end

public abstract class EntityAITarget extends EntityAIBase
{
    /** The entity that this task belongs to */
    protected EntityLiving taskOwner;
    protected float targetDistance;

    /**
     * If true, EntityAI targets must be able to be seen (cannot be blocked by walls) to be suitable targets.
     */
    protected boolean shouldCheckSight;
    private boolean field_75303_a;
    private int field_75301_b;
    private int field_75302_c;
    private int field_75298_g;

    public EntityAITarget(EntityLiving par1EntityLiving, float par2, boolean par3)
    {
        this(par1EntityLiving, par2, par3, false);
    }

    public EntityAITarget(EntityLiving par1EntityLiving, float par2, boolean par3, boolean par4)
    {
        this.field_75301_b = 0;
        this.field_75302_c = 0;
        this.field_75298_g = 0;
        this.taskOwner = par1EntityLiving;
        this.targetDistance = par2;
        this.shouldCheckSight = par3;
        this.field_75303_a = par4;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        EntityLiving entityliving = this.taskOwner.getAttackTarget();

        if (entityliving == null)
        {
            return false;
        }
        else if (!entityliving.isEntityAlive())
        {
            return false;
        }
        else if (this.taskOwner.getDistanceSqToEntity(entityliving) > (double)(this.targetDistance * this.targetDistance))
        {
            return false;
        }
        else
        {
            if (this.shouldCheckSight)
            {
                if (this.taskOwner.getEntitySenses().canSee(entityliving))
                {
                    this.field_75298_g = 0;
                }
                else if (++this.field_75298_g > 60)
                {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.field_75301_b = 0;
        this.field_75302_c = 0;
        this.field_75298_g = 0;
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
        this.taskOwner.setAttackTarget((EntityLiving)null);
    }

    /**
     * A method used to see if an entity is a suitable target through a number of checks.
     */
    protected boolean isSuitableTarget(EntityLiving par1EntityLiving, boolean par2)
    {
        if (par1EntityLiving == null)
        {
            return false;
        }
        else if (par1EntityLiving == this.taskOwner)
        {
            return false;
        }
        else if (!par1EntityLiving.isEntityAlive())
        {
            return false;
        }
        else if (!this.taskOwner.canAttackClass(par1EntityLiving.getClass()))
        {
            return false;
        }
        else
        {
            if (this.taskOwner instanceof EntityTameable && ((EntityTameable)this.taskOwner).isTamed())
            {
                if (par1EntityLiving instanceof EntityTameable && ((EntityTameable)par1EntityLiving).isTamed())
                {
                    return false;
                }

                if (par1EntityLiving == ((EntityTameable)this.taskOwner).getOwner())
                {
                    return false;
                }
            }
            else if (par1EntityLiving instanceof EntityPlayer && !par2 && ((EntityPlayer)par1EntityLiving).capabilities.disableDamage)
            {
                return false;
            }

            if (!this.taskOwner.isWithinHomeDistance(MathHelper.floor_double(par1EntityLiving.posX), MathHelper.floor_double(par1EntityLiving.posY), MathHelper.floor_double(par1EntityLiving.posZ)))
            {
                return false;
            }
            else if (this.shouldCheckSight && !this.taskOwner.getEntitySenses().canSee(par1EntityLiving))
            {
                return false;
            }
            else
            {
                if (this.field_75303_a)
                {
                    if (--this.field_75302_c <= 0)
                    {
                        this.field_75301_b = 0;
                    }

                    if (this.field_75301_b == 0)
                    {
                        this.field_75301_b = this.func_75295_a(par1EntityLiving) ? 1 : 2;
                    }

                    if (this.field_75301_b == 2)
                    {
                        return false;
                    }
                }

                // CraftBukkit start - Check all the different target goals for the reason, default to RANDOM_TARGET
                EntityTargetEvent.TargetReason reason = EntityTargetEvent.TargetReason.RANDOM_TARGET;

                if (this instanceof EntityAIDefendVillage)
                {
                    reason = EntityTargetEvent.TargetReason.DEFEND_VILLAGE;
                }
                else if (this instanceof EntityAIHurtByTarget)
                {
                    reason = EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY;
                }
                else if (this instanceof EntityAINearestAttackableTarget)
                {
                    if (par1EntityLiving instanceof EntityPlayer)
                    {
                        reason = EntityTargetEvent.TargetReason.CLOSEST_PLAYER;
                    }
                }
                else if (this instanceof EntityAIOwnerHurtByTarget)
                {
                    reason = EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER;
                }
                else if (this instanceof EntityAIOwnerHurtTarget)
                {
                    reason = EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET;
                }

                org.bukkit.event.entity.EntityTargetLivingEntityEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityTargetLivingEvent(this.taskOwner, par1EntityLiving, reason);

                if (event.isCancelled() || event.getTarget() == null)
                {
                    this.taskOwner.setAttackTarget(null);
                    return false;
                }
                else if (par1EntityLiving.getBukkitEntity() != event.getTarget())
                {
                    this.taskOwner.setAttackTarget((EntityLiving)((CraftEntity) event.getTarget()).getHandle());
                }

                if (this.taskOwner instanceof EntityCreature)
                {
                    ((EntityCreature) this.taskOwner).entityToAttack = ((CraftEntity) event.getTarget()).getHandle();
                }

                // CraftBukkit end
                return true;
            }
        }
    }

    private boolean func_75295_a(EntityLiving par1EntityLiving)
    {
        this.field_75302_c = 10 + this.taskOwner.getRNG().nextInt(5);
        PathEntity pathentity = this.taskOwner.getNavigator().getPathToEntityLiving(par1EntityLiving);

        if (pathentity == null)
        {
            return false;
        }
        else
        {
            PathPoint pathpoint = pathentity.getFinalPathPoint();

            if (pathpoint == null)
            {
                return false;
            }
            else
            {
                int i = pathpoint.xCoord - MathHelper.floor_double(par1EntityLiving.posX);
                int j = pathpoint.zCoord - MathHelper.floor_double(par1EntityLiving.posZ);
                return (double)(i * i + j * j) <= 2.25D;
            }
        }
    }
}
