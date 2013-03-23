package net.minecraft.entity.effect;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class EntityLightningBolt extends EntityWeatherEffect
{
    /**
     * Declares which state the lightning bolt is in. Whether it's in the air, hit the ground, etc.
     */
    private int lightningState;

    /**
     * A random long that is used to change the vertex of the lightning rendered in RenderLightningBolt
     */
    public long boltVertex = 0L;

    /**
     * Determines the time before the EntityLightningBolt is destroyed. It is a random integer decremented over time.
     */
    private int boltLivingTime;

    // CraftBukkit start
    public boolean isEffect = false;

    public EntityLightningBolt(World par1World, double par2, double par4, double par6)
    {
        this(par1World, par2, par4, par6, false);
    }

    public EntityLightningBolt(World par1World, double par2, double par4, double par6, boolean isEffect)
    {
        // CraftBukkit end
        super(par1World);
        // CraftBukkit start
        this.isEffect = isEffect;
        // CraftBukkit end
        this.setLocationAndAngles(par2, par4, par6, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltVertex = this.rand.nextLong();
        this.boltLivingTime = this.rand.nextInt(3) + 1;

        // CraftBukkit
        if (!isEffect && !par1World.isRemote && par1World.difficultySetting >= 2 && par1World.doChunksNearChunkExist(MathHelper.floor_double(par2), MathHelper.floor_double(par4), MathHelper.floor_double(par6), 10))
        {
            int i = MathHelper.floor_double(par2);
            int j = MathHelper.floor_double(par4);
            int k = MathHelper.floor_double(par6);

            if (par1World.getBlockId(i, j, k) == 0 && Block.fire.canPlaceBlockAt(par1World, i, j, k))
            {
                // CraftBukkit start
                if (!CraftEventFactory.callBlockIgniteEvent(par1World, i, j, k, this).isCancelled())
                {
                    par1World.setBlock(i, j, k, Block.fire.blockID);
                }

                // CraftBukkit end
            }

            for (i = 0; i < 4; ++i)
            {
                j = MathHelper.floor_double(par2) + this.rand.nextInt(3) - 1;
                k = MathHelper.floor_double(par4) + this.rand.nextInt(3) - 1;
                int l = MathHelper.floor_double(par6) + this.rand.nextInt(3) - 1;

                if (par1World.getBlockId(j, k, l) == 0 && Block.fire.canPlaceBlockAt(par1World, j, k, l))
                {
                    // CraftBukkit start
                    if (!CraftEventFactory.callBlockIgniteEvent(par1World, j, k, l, this).isCancelled())
                    {
                        par1World.setBlock(i, j, k, Block.fire.blockID);
                    }

                    // CraftBukkit end
                }
            }
        }
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        super.onUpdate();

        if (this.lightningState == 2)
        {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "ambient.weather.thunder", 10000.0F, 0.8F + this.rand.nextFloat() * 0.2F);
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.explode", 2.0F, 0.5F + this.rand.nextFloat() * 0.2F);
        }

        --this.lightningState;

        if (this.lightningState < 0)
        {
            if (this.boltLivingTime == 0)
            {
                this.setDead();
            }
            else if (this.lightningState < -this.rand.nextInt(10))
            {
                --this.boltLivingTime;
                this.lightningState = 1;
                this.boltVertex = this.rand.nextLong();

                // CraftBukkit
                if (!this.isEffect && !this.worldObj.isRemote && this.worldObj.doChunksNearChunkExist(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ), 10))
                {
                    int i = MathHelper.floor_double(this.posX);
                    int j = MathHelper.floor_double(this.posY);
                    int k = MathHelper.floor_double(this.posZ);

                    if (this.worldObj.getBlockId(i, j, k) == 0 && Block.fire.canPlaceBlockAt(this.worldObj, i, j, k))
                    {
                        // CraftBukkit start
                        if (!CraftEventFactory.callBlockIgniteEvent(worldObj, i, j, k, this).isCancelled())
                        {
                            worldObj.setBlock(i, j, k, Block.fire.blockID);
                        }

                        // CraftBukkit end
                    }
                }
            }
        }

        if (this.lightningState >= 0 && !this.isEffect)   // CraftBukkit - add !this.isEffect
        {
            if (this.worldObj.isRemote)
            {
                this.worldObj.lastLightningBolt = 2;
            }
            else
            {
                double d0 = 3.0D;
                List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getAABBPool().getAABB(this.posX - d0, this.posY - d0, this.posZ - d0, this.posX + d0, this.posY + 6.0D + d0, this.posZ + d0));

                for (int l = 0; l < list.size(); ++l)
                {
                    Entity entity = (Entity)list.get(l);
                    entity.onStruckByLightning(this);
                }
            }
        }
    }

    protected void entityInit() {}

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {}

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {}
}
