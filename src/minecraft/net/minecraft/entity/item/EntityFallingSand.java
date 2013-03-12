package net.minecraft.entity.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSand;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
//CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
// CraftBukkit end

public class EntityFallingSand extends Entity
{
    public int blockID;
    public int metadata;

    /** How long the block has been falling for. */
    public int fallTime;
    public boolean shouldDropItem;
    private boolean isBreakingAnvil;
    private boolean isAnvil;

    /** Maximum amount of damage dealt to entities hit by falling block */
    private int fallHurtMax;

    /** Actual damage dealt to entities hit by falling block */
    private float fallHurtAmount;
    private NBTTagCompound tileEntityData; // CraftBukkit

    public EntityFallingSand(World par1World)
    {
        super(par1World);
        this.fallTime = 0;
        this.shouldDropItem = true;
        this.isBreakingAnvil = false;
        this.isAnvil = false;
        this.fallHurtMax = 40;
        this.fallHurtAmount = 2.0F;
    }

    public EntityFallingSand(World par1World, double par2, double par4, double par6, int par8)
    {
        this(par1World, par2, par4, par6, par8, 0);
    }

    public EntityFallingSand(World par1World, double par2, double par4, double par6, int par8, int par9)
    {
        super(par1World);
        this.fallTime = 0;
        this.shouldDropItem = true;
        this.isBreakingAnvil = false;
        this.isAnvil = false;
        this.fallHurtMax = 40;
        this.fallHurtAmount = 2.0F;
        this.blockID = par8;
        this.metadata = par9;
        this.preventEntitySpawning = true;
        this.setSize(0.98F, 0.98F);
        this.yOffset = this.height / 2.0F;
        this.setPosition(par2, par4, par6);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
    }

    /**
     * returns if this entity triggers Block.onEntityWalking on the blocks they walk on. used for spiders and wolves to
     * prevent them from trampling crops
     */
    protected boolean canTriggerWalking()
    {
        return false;
    }

    protected void entityInit() {}

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith()
    {
        return !this.isDead;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        if (this.blockID == 0)
        {
            this.setDead();
        }
        else
        {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            ++this.fallTime;
            this.motionY -= 0.03999999910593033D;
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9800000190734863D;
            this.motionY *= 0.9800000190734863D;
            this.motionZ *= 0.9800000190734863D;

            if (!this.worldObj.isRemote)
            {
                int i = MathHelper.floor_double(this.posX);
                int j = MathHelper.floor_double(this.posY);
                int k = MathHelper.floor_double(this.posZ);

                if (this.fallTime == 1)
                {
                    if (this.fallTime != 1 || this.worldObj.getBlockId(i, j, k) != this.blockID || this.worldObj.getBlockMetadata(i, j, k) != this.metadata || CraftEventFactory.callEntityChangeBlockEvent(this, i, j, k, 0, 0).isCancelled()) // CraftBukkit - compare data and call event
                    {
                        this.setDead();
                        return;
                    }

                    // CraftBukkit start - Store the block tile entity with this entity
                    TileEntity tile = this.worldObj.getBlockTileEntity(i, j, k);

                    if (tile != null)
                    {
                        tileEntityData = new NBTTagCompound();
                        // Save the data
                        tile.writeToNBT(tileEntityData);
                        // Remove the existing tile entity
                        this.worldObj.removeBlockTileEntity(i, j, k);
                    }

                    // CraftBukkit end
                    this.worldObj.setBlockWithNotify(i, j, k, 0);
                }

                if (this.onGround)
                {
                    this.motionX *= 0.699999988079071D;
                    this.motionZ *= 0.699999988079071D;
                    this.motionY *= -0.5D;

                    if (this.worldObj.getBlockId(i, j, k) != Block.pistonMoving.blockID)
                    {
                        this.setDead();

                        // CraftBukkit start
                        if (!this.isBreakingAnvil && this.worldObj.canPlaceEntityOnSide(this.blockID, i, j, k, true, 1, (Entity) null) && !BlockSand.canFallBelow(this.worldObj, i, j - 1, k) /* mimic the false conditions of setTypeIdAndData */ && i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000 && j > 0 && j < 256 && !(this.worldObj.getBlockId(i, j, k) == this.blockID && this.worldObj.getBlockMetadata(i, j, k) == this.metadata))
                        {
                            if (CraftEventFactory.callEntityChangeBlockEvent(this, i, j, k, this.blockID, this.metadata).isCancelled())
                            {
                                return;
                            }

                            this.worldObj.setBlockAndMetadataWithNotify(i, j, k, this.blockID, this.metadata);

                            if (this.tileEntityData != null)
                            {
                                this.worldObj.setBlockTileEntity(i, j, k, TileEntity.createAndLoadEntity(this.tileEntityData));
                            }

                            // CraftBukkit end
                            if (Block.blocksList[this.blockID] instanceof BlockSand)
                            {
                                ((BlockSand)Block.blocksList[this.blockID]).onFinishFalling(this.worldObj, i, j, k, this.metadata);
                            }
                        }
                        else if (this.shouldDropItem && !this.isBreakingAnvil)
                        {
                            this.entityDropItem(new ItemStack(this.blockID, 1, Block.blocksList[this.blockID].damageDropped(this.metadata)), 0.0F);
                        }
                    }
                }
                else if (this.fallTime > 100 && !this.worldObj.isRemote && (j < 1 || j > 256) || this.fallTime > 600)
                {
                    if (this.shouldDropItem)
                    {
                        this.entityDropItem(new ItemStack(this.blockID, 1, Block.blocksList[this.blockID].damageDropped(this.metadata)), 0.0F);
                    }

                    this.setDead();
                }
            }
        }
    }

    /**
     * Called when the mob is falling. Calculates and applies fall damage.
     */
    protected void fall(float par1)
    {
        if (this.isAnvil)
        {
            int i = MathHelper.ceiling_float_int(par1 - 1.0F);

            if (i > 0)
            {
                ArrayList arraylist = new ArrayList(this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox));
                DamageSource damagesource = this.blockID == Block.anvil.blockID ? DamageSource.anvil : DamageSource.fallingBlock;
                Iterator iterator = arraylist.iterator();

                while (iterator.hasNext())
                {
                    Entity entity = (Entity)iterator.next();
                    // CraftBukkit start
                    int damage = Math.min(MathHelper.floor_float((float) i * this.fallHurtAmount), this.fallHurtMax);
                    EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(this, entity, EntityDamageEvent.DamageCause.FALLING_BLOCK, damage);

                    if (event.isCancelled())
                    {
                        continue;
                    }

                    entity.attackEntityFrom(damagesource, event.getDamage());
                    // CraftBukkit end
                }

                if (this.blockID == Block.anvil.blockID && (double)this.rand.nextFloat() < 0.05000000074505806D + (double)i * 0.05D)
                {
                    int j = this.metadata >> 2;
                    int k = this.metadata & 3;
                    ++j;

                    if (j > 2)
                    {
                        this.isBreakingAnvil = true;
                    }
                    else
                    {
                        this.metadata = k | j << 2;
                    }
                }
            }
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        par1NBTTagCompound.setByte("Tile", (byte)this.blockID);
        par1NBTTagCompound.setByte("Data", (byte)this.metadata);
        par1NBTTagCompound.setByte("Time", (byte)this.fallTime);
        par1NBTTagCompound.setBoolean("DropItem", this.shouldDropItem);
        par1NBTTagCompound.setBoolean("HurtEntities", this.isAnvil);
        par1NBTTagCompound.setFloat("FallHurtAmount", this.fallHurtAmount);
        par1NBTTagCompound.setInteger("FallHurtMax", this.fallHurtMax);

        // CraftBukkit start - store the tile data
        if (this.tileEntityData != null)
        {
            par1NBTTagCompound.setTag("Bukkit.tileData", this.tileEntityData.copy());
        }

        // CraftBukkit end
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        this.blockID = par1NBTTagCompound.getByte("Tile") & 255;
        this.metadata = par1NBTTagCompound.getByte("Data") & 255;
        this.fallTime = par1NBTTagCompound.getByte("Time") & 255;

        if (par1NBTTagCompound.hasKey("HurtEntities"))
        {
            this.isAnvil = par1NBTTagCompound.getBoolean("HurtEntities");
            this.fallHurtAmount = par1NBTTagCompound.getFloat("FallHurtAmount");
            this.fallHurtMax = par1NBTTagCompound.getInteger("FallHurtMax");
        }
        else if (this.blockID == Block.anvil.blockID)
        {
            this.isAnvil = true;
        }

        // CraftBukkit start - load tileData
        if (par1NBTTagCompound.hasKey("Bukkit.tileData"))
        {
            this.tileEntityData = (NBTTagCompound) par1NBTTagCompound.getCompoundTag("Bukkit.tileData").copy();
        }

        // CraftBukkit end

        if (par1NBTTagCompound.hasKey("DropItem"))
        {
            this.shouldDropItem = par1NBTTagCompound.getBoolean("DropItem");
        }

        if (this.blockID == 0)
        {
            this.blockID = Block.sand.blockID;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize()
    {
        return 0.0F;
    }

    @SideOnly(Side.CLIENT)
    public World getWorld()
    {
        return this.worldObj;
    }

    public void setIsAnvil(boolean par1)
    {
        this.isAnvil = par1;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Return whether this entity should be rendered as on fire.
     */
    public boolean canRenderOnFire()
    {
        return false;
    }

    public void func_85029_a(CrashReportCategory par1CrashReportCategory)
    {
        super.func_85029_a(par1CrashReportCategory);
        par1CrashReportCategory.addCrashSection("Immitating block ID", Integer.valueOf(this.blockID));
        par1CrashReportCategory.addCrashSection("Immitating block data", Integer.valueOf(this.metadata));
    }
}
