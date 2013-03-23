package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
public class EntityFireworkRocket extends Entity
{
    /** The age of the firework in ticks. */
    private int fireworkAge;

    /**
     * The lifetime of the firework in ticks. When the age reaches the lifetime the firework explodes.
     */
    public int lifetime; // CraftBukkit - private -> public

    public EntityFireworkRocket(World par1World)
    {
        super(par1World);
        this.setSize(0.25F, 0.25F);
    }

    protected void entityInit()
    {
        this.dataWatcher.addObjectByDataType(8, 5);
    }

    public EntityFireworkRocket(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack)
    {
        super(par1World);
        this.fireworkAge = 0;
        this.setSize(0.25F, 0.25F);
        this.setPosition(par2, par4, par6);
        this.yOffset = 0.0F;
        int i = 1;

        if (par8ItemStack != null && par8ItemStack.hasTagCompound())
        {
            this.dataWatcher.updateObject(8, par8ItemStack);
            NBTTagCompound nbttagcompound = par8ItemStack.getTagCompound();
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("Fireworks");

            if (nbttagcompound1 != null)
            {
                i += nbttagcompound1.getByte("Flight");
            }
        }

        this.motionX = this.rand.nextGaussian() * 0.001D;
        this.motionZ = this.rand.nextGaussian() * 0.001D;
        this.motionY = 0.05D;
        this.lifetime = 10 * i + this.rand.nextInt(6) + this.rand.nextInt(7);
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        super.onUpdate();
        this.motionX *= 1.15D;
        this.motionZ *= 1.15D;
        this.motionY += 0.04D;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

        for (this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
        {
            ;
        }

        while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
        {
            this.prevRotationPitch += 360.0F;
        }

        while (this.rotationYaw - this.prevRotationYaw < -180.0F)
        {
            this.prevRotationYaw -= 360.0F;
        }

        while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
        {
            this.prevRotationYaw += 360.0F;
        }

        this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
        this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;

        if (this.fireworkAge == 0)
        {
            this.worldObj.playSoundAtEntity(this, "fireworks.launch", 3.0F, 1.0F);
        }

        ++this.fireworkAge;

        if (this.worldObj.isRemote && this.fireworkAge % 2 < 2)
        {
            this.worldObj.spawnParticle("fireworksSpark", this.posX, this.posY - 0.3D, this.posZ, this.rand.nextGaussian() * 0.05D, -this.motionY * 0.5D, this.rand.nextGaussian() * 0.05D);
        }

        if (!this.worldObj.isRemote && this.fireworkAge > this.lifetime)
        {
            this.worldObj.setEntityState(this, (byte)17);
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        par1NBTTagCompound.setInteger("Life", this.fireworkAge);
        par1NBTTagCompound.setInteger("LifeTime", this.lifetime);
        ItemStack itemstack = this.dataWatcher.getWatchableObjectItemStack(8);

        if (itemstack != null)
        {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            itemstack.writeToNBT(nbttagcompound1);
            par1NBTTagCompound.setCompoundTag("FireworksItem", nbttagcompound1);
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        this.fireworkAge = par1NBTTagCompound.getInteger("Life");
        this.lifetime = par1NBTTagCompound.getInteger("LifeTime");
        NBTTagCompound nbttagcompound1 = par1NBTTagCompound.getCompoundTag("FireworksItem");

        if (nbttagcompound1 != null)
        {
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttagcompound1);

            if (itemstack != null)
            {
                this.dataWatcher.updateObject(8, itemstack);
            }
        }
    }

    /**
     * Gets how bright this entity is.
     */
    public float getBrightness(float par1)
    {
        return super.getBrightness(par1);
    }

    /**
     * If returns false, the item will not inflict any damage against entities.
     */
    public boolean canAttackWithItem()
    {
        return false;
    }
}
