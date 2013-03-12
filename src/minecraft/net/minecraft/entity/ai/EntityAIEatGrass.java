package net.minecraft.entity.ai;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.Material;
// CraftBukkit end
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class EntityAIEatGrass extends EntityAIBase
{
    private EntityLiving theEntity;
    private World theWorld;

    /** A decrementing tick used for the sheep's head offset and animation. */
    int eatGrassTick = 0;

    public EntityAIEatGrass(EntityLiving par1EntityLiving)
    {
        this.theEntity = par1EntityLiving;
        this.theWorld = par1EntityLiving.worldObj;
        this.setMutexBits(7);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        if (this.theEntity.getRNG().nextInt(this.theEntity.isChild() ? 50 : 1000) != 0)
        {
            return false;
        }
        else
        {
            int i = MathHelper.floor_double(this.theEntity.posX);
            int j = MathHelper.floor_double(this.theEntity.posY);
            int k = MathHelper.floor_double(this.theEntity.posZ);
            return this.theWorld.getBlockId(i, j, k) == Block.tallGrass.blockID && this.theWorld.getBlockMetadata(i, j, k) == 1 ? true : this.theWorld.getBlockId(i, j - 1, k) == Block.grass.blockID;
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.eatGrassTick = 40;
        this.theWorld.setEntityState(this.theEntity, (byte)10);
        this.theEntity.getNavigator().clearPathEntity();
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
        this.eatGrassTick = 0;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        return this.eatGrassTick > 0;
    }

    public int getEatGrassTick()
    {
        return this.eatGrassTick;
    }

    /**
     * Updates the task
     */
    public void updateTask()
    {
        this.eatGrassTick = Math.max(0, this.eatGrassTick - 1);

        if (this.eatGrassTick == 4)
        {
            int i = MathHelper.floor_double(this.theEntity.posX);
            int j = MathHelper.floor_double(this.theEntity.posY);
            int k = MathHelper.floor_double(this.theEntity.posZ);

            if (this.theWorld.getBlockId(i, j, k) == Block.tallGrass.blockID)
            {
                // CraftBukkit start
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.theEntity.getBukkitEntity(), this.theEntity.worldObj.getWorld().getBlockAt(i, j, k), Material.AIR).isCancelled())
                {
                    this.theWorld.playAuxSFX(2001, i, j, k, Block.tallGrass.blockID + 4096);
                    this.theWorld.setBlockWithNotify(i, j, k, 0);
                    this.theEntity.eatGrassBonus();
                }

                // CraftBukkit end
            }
            else if (this.theWorld.getBlockId(i, j - 1, k) == Block.grass.blockID)
            {
                // CraftBukkit start
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.theEntity.getBukkitEntity(), this.theEntity.worldObj.getWorld().getBlockAt(i, j - 1, k), Material.DIRT).isCancelled())
                {
                    this.theWorld.playAuxSFX(2001, i, j - 1, k, Block.grass.blockID);
                    this.theWorld.setBlockWithNotify(i, j - 1, k, Block.dirt.blockID);
                    this.theEntity.eatGrassBonus();
                }
                // CraftBukkit end
            }
        }
    }
}
