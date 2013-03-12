package net.minecraft.entity.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.IMinecartCollisionHandler;
import net.minecraftforge.common.MinecartRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.*;
// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
// CraftBukkit end

public class EntityMinecart extends Entity implements IInventory
{
    /** Array of item stacks stored in minecart (for storage minecarts). */
    protected ItemStack[] cargoItems;
    protected int fuel;
    protected boolean isInReverse;

    /** The type of minecart, 2 for powered, 1 for storage. */
    public int minecartType;
    public double pushX;
    public double pushZ;
    protected final IUpdatePlayerListBox field_82344_g;
    protected boolean field_82345_h;

    /** Minecart rotational logic matrix */
    protected static final int[][][] matrix = new int[][][] {{{0, 0, -1}, {0, 0, 1}}, {{ -1, 0, 0}, {1, 0, 0}}, {{ -1, -1, 0}, {1, 0, 0}}, {{ -1, 0, 0}, {1, -1, 0}}, {{0, 0, -1}, {0, -1, 1}}, {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, { -1, 0, 0}}, {{0, 0, -1}, { -1, 0, 0}}, {{0, 0, -1}, {1, 0, 0}}};

    /** appears to be the progress of the turn */
    protected int turnProgress;
    protected double minecartX;
    protected double minecartY;
    protected double minecartZ;
    protected double minecartYaw;
    protected double minecartPitch;
    @SideOnly(Side.CLIENT)
    protected double velocityX;
    @SideOnly(Side.CLIENT)
    protected double velocityY;
    @SideOnly(Side.CLIENT)
    protected double velocityZ;

    // CraftBukkit start
    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public double maxSpeed = 0.4D;
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    public ItemStack[] getContents()
    {
        return this.cargoItems;
    }

    public void onOpen(CraftHumanEntity who)
    {
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return transaction;
    }

    public InventoryHolder getOwner()
    {
        org.bukkit.entity.Entity cart = getBukkitEntity();

        if (cart instanceof InventoryHolder)
        {
            return (InventoryHolder) cart;
        }

        return null;
    }

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }
    // CraftBukkit end

    /* Forge: Minecart Compatibility Layer Integration. */
    public static float defaultMaxSpeedRail = 0.4f;
    public static float defaultMaxSpeedGround = 0.4f;
    public static float defaultMaxSpeedAirLateral = 0.4f;
    public static float defaultMaxSpeedAirVertical = -1f;
    public static double defaultDragRidden = 0.996999979019165D;
    public static double defaultDragEmpty = 0.9599999785423279D;
    public static double defaultDragAir = 0.94999998807907104D;
    protected boolean canUseRail = true;
    protected boolean canBePushed = true;
    private static IMinecartCollisionHandler collisionHandler = null;

    /* Instance versions of the above physics properties */
    protected float maxSpeedRail;
    protected float maxSpeedGround;
    protected float maxSpeedAirLateral;
    protected float maxSpeedAirVertical;
    protected double dragAir;

    public EntityMinecart(World par1World)
    {
        super(par1World);
        this.cargoItems = new ItemStack[27]; // CraftBukkit
        this.fuel = 0;
        this.isInReverse = false;
        this.field_82345_h = true;
        this.preventEntitySpawning = true;
        this.setSize(0.98F, 0.7F);
        this.yOffset = this.height / 2.0F;
        this.field_82344_g = par1World != null ? par1World.func_82735_a(this) : null;

        maxSpeedRail = defaultMaxSpeedRail;
        maxSpeedGround = defaultMaxSpeedGround;
        maxSpeedAirLateral = defaultMaxSpeedAirLateral;
        maxSpeedAirVertical = defaultMaxSpeedAirVertical;
        dragAir = defaultDragAir;
        this.worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleCreateEvent((Vehicle) this.getBukkitEntity())); // CraftBukkit
    }

    public EntityMinecart(World world, int type)
    {
        this(world);
        minecartType = type;
    }

    /**
     * returns if this entity triggers Block.onEntityWalking on the blocks they walk on. used for spiders and wolves to
     * prevent them from trampling crops
     */
    protected boolean canTriggerWalking()
    {
        return false;
    }

    protected void entityInit()
    {
        this.dataWatcher.addObject(16, new Byte((byte)0));
        this.dataWatcher.addObject(17, new Integer(0));
        this.dataWatcher.addObject(18, new Integer(1));
        this.dataWatcher.addObject(19, new Integer(0));
    }

    /**
     * Returns a boundingBox used to collide the entity with other entities and blocks. This enables the entity to be
     * pushable on contact, like boats or minecarts.
     */
    public AxisAlignedBB getCollisionBox(Entity par1Entity)
    {
        if (getCollisionHandler() != null)
        {
            return getCollisionHandler().getCollisionBox(this, par1Entity);
        }
        return par1Entity.canBePushed() ? par1Entity.boundingBox : null;
    }

    /**
     * returns the bounding box for this entity
     */
    public AxisAlignedBB getBoundingBox()
    {
        if (getCollisionHandler() != null)
        {
            return getCollisionHandler().getBoundingBox(this);
        }
        return null;
    }

    /**
     * Returns true if this entity should push and be pushed by other entities when colliding.
     */
    public boolean canBePushed()
    {
        return canBePushed;
    }

    public EntityMinecart(World par1World, double par2, double par4, double par6, int par8)
    {
        this(par1World);
        this.setPosition(par2, par4 + (double)this.yOffset, par6);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
        this.minecartType = par8;
        this.worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleCreateEvent((Vehicle) this.getBukkitEntity())); // CraftBukkit
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    public double getMountedYOffset()
    {
        return (double)this.height * 0.0D - 0.30000001192092896D;
    }

    /**
     * Called when the entity is attacked.
     */
    public boolean attackEntityFrom(DamageSource par1DamageSource, int par2)
    {
        if (this.isEntityInvulnerable())
        {
            return false;
        }
        else
        {
            if (!this.worldObj.isRemote && !this.isDead)
            {
                // CraftBukkit start
                Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                org.bukkit.entity.Entity passenger = (par1DamageSource.getEntity() == null) ? null : par1DamageSource.getEntity().getBukkitEntity();
                VehicleDamageEvent event = new VehicleDamageEvent(vehicle, passenger, par2);
                this.worldObj.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return true;
                }

                par2 = event.getDamage();
                // CraftBukkit end
                this.setRollingDirection(-this.getRollingDirection());
                this.setRollingAmplitude(10);
                this.setBeenAttacked();
                this.setDamage(this.getDamage() + par2 * 10);

                if (par1DamageSource.getEntity() instanceof EntityPlayer && ((EntityPlayer)par1DamageSource.getEntity()).capabilities.isCreativeMode)
                {
                    this.setDamage(100);
                }

                if (this.getDamage() > 40)
                {
                    if (this.riddenByEntity != null)
                    {
                        this.riddenByEntity.mountEntity(this);
                    }

                    // CraftBukkit start
                    VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
                    this.worldObj.getServer().getPluginManager().callEvent(destroyEvent);

                    if (destroyEvent.isCancelled())
                    {
                        this.setDamage(40); // Maximize damage so this doesn't get triggered again right away
                        return true;
                    }

                    // CraftBukkit end
                    this.setDead();
                    dropCartAsItem();
                }

                return true;
            }
            else
            {
                return true;
            }
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * Setups the entity to do the hurt animation. Only used by packets in multiplayer.
     */
    public void performHurtAnimation()
    {
        this.setRollingDirection(-this.getRollingDirection());
        this.setRollingAmplitude(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10);
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith()
    {
        return !this.isDead;
    }

    /**
     * Will get destroyed next tick.
     */
    public void setDead()
    {
        if (this.field_82345_h)
        {
            for (int i = 0; i < this.getSizeInventory(); ++i)
            {
                ItemStack itemstack = this.getStackInSlot(i);

                if (itemstack != null)
                {
                    float f = this.rand.nextFloat() * 0.8F + 0.1F;
                    float f1 = this.rand.nextFloat() * 0.8F + 0.1F;
                    float f2 = this.rand.nextFloat() * 0.8F + 0.1F;

                    while (itemstack.stackSize > 0)
                    {
                        int j = this.rand.nextInt(21) + 10;

                        if (j > itemstack.stackSize)
                        {
                            j = itemstack.stackSize;
                        }

                        itemstack.stackSize -= j;
                        EntityItem entityitem = new EntityItem(this.worldObj, this.posX + (double)f, this.posY + (double)f1, this.posZ + (double)f2, new ItemStack(itemstack.itemID, j, itemstack.getItemDamage()));

                        if (itemstack.hasTagCompound())
                        {
                            entityitem.getEntityItem().setTagCompound((NBTTagCompound)itemstack.getTagCompound().copy());
                        }

                        float f3 = 0.05F;
                        entityitem.motionX = (double)((float)this.rand.nextGaussian() * f3);
                        entityitem.motionY = (double)((float)this.rand.nextGaussian() * f3 + 0.2F);
                        entityitem.motionZ = (double)((float)this.rand.nextGaussian() * f3);
                        this.worldObj.spawnEntityInWorld(entityitem);
                    }
                }
            }
        }

        super.setDead();

        if (this.field_82344_g != null)
        {
            this.field_82344_g.update();
        }
    }

    /**
     * Teleports the entity to another dimension. Params: Dimension number to teleport to
     */
    public void travelToDimension(int par1)
    {
        this.field_82345_h = false;
        super.travelToDimension(par1);
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        // CraftBukkit start
        double prevX = this.posX;
        double prevY = this.posY;
        double prevZ = this.posZ;
        float prevYaw = this.rotationYaw;
        float prevPitch = this.rotationPitch;
        // CraftBukkit end

        if (this.field_82344_g != null)
        {
            this.field_82344_g.update();
        }

        if (this.getRollingAmplitude() > 0)
        {
            this.setRollingAmplitude(this.getRollingAmplitude() - 1);
        }

        if (this.getDamage() > 0)
        {
            this.setDamage(this.getDamage() - 1);
        }

        if (this.posY < -64.0D)
        {
            this.kill();
        }

        if (this.isMinecartPowered() && this.rand.nextInt(4) == 0 && minecartType == 2 && getClass() == EntityMinecart.class)
        {
            this.worldObj.spawnParticle("largesmoke", this.posX, this.posY + 0.8D, this.posZ, 0.0D, 0.0D, 0.0D);
        }

        int i;

        if (!this.worldObj.isRemote && this.worldObj instanceof WorldServer)
        {
            this.worldObj.theProfiler.startSection("portal");
            MinecraftServer minecraftserver = ((WorldServer)this.worldObj).getMinecraftServer();
            i = this.getMaxInPortalTime();

            if (this.inPortal)
            {
                if (true || minecraftserver.getAllowNether()) // CraftBukkit - multi-world should still allow teleport even if default vanilla nether disabled
                {
                    if (this.ridingEntity == null && this.timeInPortal++ >= i)
                    {
                        this.timeInPortal = i;
                        this.timeUntilPortal = this.getPortalCooldown();
                        byte b0;

                        if (this.worldObj.provider.dimensionId == -1)
                        {
                            b0 = 0;
                        }
                        else
                        {
                            b0 = -1;
                        }

                        this.travelToDimension(b0);
                    }

                    this.inPortal = false;
                }
            }
            else
            {
                if (this.timeInPortal > 0)
                {
                    this.timeInPortal -= 4;
                }

                if (this.timeInPortal < 0)
                {
                    this.timeInPortal = 0;
                }
            }

            if (this.timeUntilPortal > 0)
            {
                --this.timeUntilPortal;
            }

            this.worldObj.theProfiler.endSection();
        }

        if (this.worldObj.isRemote)
        {
            if (this.turnProgress > 0)
            {
                double d0 = this.posX + (this.minecartX - this.posX) / (double)this.turnProgress;
                double d1 = this.posY + (this.minecartY - this.posY) / (double)this.turnProgress;
                double d2 = this.posZ + (this.minecartZ - this.posZ) / (double)this.turnProgress;
                double d3 = MathHelper.wrapAngleTo180_double(this.minecartYaw - (double)this.rotationYaw);
                this.rotationYaw = (float)((double)this.rotationYaw + d3 / (double)this.turnProgress);
                this.rotationPitch = (float)((double)this.rotationPitch + (this.minecartPitch - (double)this.rotationPitch) / (double)this.turnProgress);
                --this.turnProgress;
                this.setPosition(d0, d1, d2);
                this.setRotation(this.rotationYaw, this.rotationPitch);
            }
            else
            {
                this.setPosition(this.posX, this.posY, this.posZ);
                this.setRotation(this.rotationYaw, this.rotationPitch);
            }
        }
        else
        {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.motionY -= 0.03999999910593033D;
            int j = MathHelper.floor_double(this.posX);
            i = MathHelper.floor_double(this.posY);
            int k = MathHelper.floor_double(this.posZ);

            if (BlockRail.isRailBlockAt(this.worldObj, j, i - 1, k))
            {
                --i;
            }

            // CraftBukkit
            double d4 = this.maxSpeed;
            double d5 = 0.0078125D;
            int l = this.worldObj.getBlockId(j, i, k);

            if (canUseRail() && BlockRail.isRailBlock(l))
            {
                this.fallDistance = 0.0F;
                Vec3 vec3 = this.func_70489_a(this.posX, this.posY, this.posZ);
                int i1 = ((BlockRail)Block.blocksList[l]).getBasicRailMetadata(worldObj, this, j, i, k);
                this.posY = (double)i;
                boolean flag = false;
                boolean flag1 = false;

                if (l == Block.railPowered.blockID)
                {
                    flag = (worldObj.getBlockMetadata(j, i, k) & 8) != 0;
                    flag1 = !flag;
                }

                if (((BlockRail)Block.blocksList[l]).isPowered())
                {
                    i1 &= 7;
                }

                if (i1 >= 2 && i1 <= 5)
                {
                    this.posY = (double)(i + 1);
                }

                adjustSlopeVelocities(i1);

                int[][] aint = matrix[i1];
                double d6 = (double)(aint[1][0] - aint[0][0]);
                double d7 = (double)(aint[1][2] - aint[0][2]);
                double d8 = Math.sqrt(d6 * d6 + d7 * d7);
                double d9 = this.motionX * d6 + this.motionZ * d7;

                if (d9 < 0.0D)
                {
                    d6 = -d6;
                    d7 = -d7;
                }

                double d10 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
                this.motionX = d10 * d6 / d8;
                this.motionZ = d10 * d7 / d8;
                double d11;
                double d12;

                if (this.riddenByEntity != null)
                {
                    d11 = this.riddenByEntity.motionX * this.riddenByEntity.motionX + this.riddenByEntity.motionZ * this.riddenByEntity.motionZ;
                    d12 = this.motionX * this.motionX + this.motionZ * this.motionZ;

                    if (d11 > 1.0E-4D && d12 < 0.01D)
                    {
                        this.motionX += this.riddenByEntity.motionX * 0.1D;
                        this.motionZ += this.riddenByEntity.motionZ * 0.1D;
                        flag1 = false;
                    }
                }

                if (flag1 && shouldDoRailFunctions())
                {
                    d11 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

                    if (d11 < 0.03D)
                    {
                        this.motionX *= 0.0D;
                        this.motionY *= 0.0D;
                        this.motionZ *= 0.0D;
                    }
                    else
                    {
                        this.motionX *= 0.5D;
                        this.motionY *= 0.0D;
                        this.motionZ *= 0.5D;
                    }
                }

                d11 = 0.0D;
                d12 = (double)j + 0.5D + (double)aint[0][0] * 0.5D;
                double d13 = (double)k + 0.5D + (double)aint[0][2] * 0.5D;
                double d14 = (double)j + 0.5D + (double)aint[1][0] * 0.5D;
                double d15 = (double)k + 0.5D + (double)aint[1][2] * 0.5D;
                d6 = d14 - d12;
                d7 = d15 - d13;
                double d16;
                double d17;

                if (d6 == 0.0D)
                {
                    this.posX = (double)j + 0.5D;
                    d11 = this.posZ - (double)k;
                }
                else if (d7 == 0.0D)
                {
                    this.posZ = (double)k + 0.5D;
                    d11 = this.posX - (double)j;
                }
                else
                {
                    d16 = this.posX - d12;
                    d17 = this.posZ - d13;
                    d11 = (d16 * d6 + d17 * d7) * 2.0D;
                }

                this.posX = d12 + d6 * d11;
                this.posZ = d13 + d7 * d11;
                this.setPosition(this.posX, this.posY + (double)this.yOffset, this.posZ);

                moveMinecartOnRail(j, i, k);

                if (aint[0][1] != 0 && MathHelper.floor_double(this.posX) - j == aint[0][0] && MathHelper.floor_double(this.posZ) - k == aint[0][2])
                {
                    this.setPosition(this.posX, this.posY + (double)aint[0][1], this.posZ);
                }
                else if (aint[1][1] != 0 && MathHelper.floor_double(this.posX) - j == aint[1][0] && MathHelper.floor_double(this.posZ) - k == aint[1][2])
                {
                    this.setPosition(this.posX, this.posY + (double)aint[1][1], this.posZ);
                }

                applyDragAndPushForces();

                Vec3 vec31 = this.func_70489_a(this.posX, this.posY, this.posZ);

                if (vec31 != null && vec3 != null)
                {
                    double d18 = (vec3.yCoord - vec31.yCoord) * 0.05D;
                    d10 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

                    if (d10 > 0.0D)
                    {
                        this.motionX = this.motionX / d10 * (d10 + d18);
                        this.motionZ = this.motionZ / d10 * (d10 + d18);
                    }

                    this.setPosition(this.posX, vec31.yCoord, this.posZ);
                }

                int j1 = MathHelper.floor_double(this.posX);
                int k1 = MathHelper.floor_double(this.posZ);

                if (j1 != j || k1 != k)
                {
                    d10 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
                    this.motionX = d10 * (double)(j1 - j);
                    this.motionZ = d10 * (double)(k1 - k);
                }

                double d19;

                updatePushForces();

                if(shouldDoRailFunctions())
                {
                    ((BlockRail)Block.blocksList[l]).onMinecartPass(worldObj, this, j, i, k);
                }

                if (flag && shouldDoRailFunctions())
                {
                    d19 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

                    if (d19 > 0.01D)
                    {
                        double d20 = 0.06D;
                        this.motionX += this.motionX / d19 * d20;
                        this.motionZ += this.motionZ / d19 * d20;
                    }
                    else if (i1 == 1)
                    {
                        if (this.worldObj.isBlockNormalCube(j - 1, i, k))
                        {
                            this.motionX = 0.02D;
                        }
                        else if (this.worldObj.isBlockNormalCube(j + 1, i, k))
                        {
                            this.motionX = -0.02D;
                        }
                    }
                    else if (i1 == 0)
                    {
                        if (this.worldObj.isBlockNormalCube(j, i, k - 1))
                        {
                            this.motionZ = 0.02D;
                        }
                        else if (this.worldObj.isBlockNormalCube(j, i, k + 1))
                        {
                            this.motionZ = -0.02D;
                        }
                    }
                }
            }
            else
            {
                moveMinecartOffRail(j, i, k);
            }

            this.doBlockCollisions();
            this.rotationPitch = 0.0F;
            double d21 = this.prevPosX - this.posX;
            double d22 = this.prevPosZ - this.posZ;

            if (d21 * d21 + d22 * d22 > 0.001D)
            {
                this.rotationYaw = (float)(Math.atan2(d22, d21) * 180.0D / Math.PI);

                if (this.isInReverse)
                {
                    this.rotationYaw += 180.0F;
                }
            }

            double d23 = (double)MathHelper.wrapAngleTo180_float(this.rotationYaw - this.prevRotationYaw);

            if (d23 < -170.0D || d23 >= 170.0D)
            {
                this.rotationYaw += 180.0F;
                this.isInReverse = !this.isInReverse;
            }

            this.setRotation(this.rotationYaw, this.rotationPitch);
            // CraftBukkit start
            org.bukkit.World bworld = this.worldObj.getWorld();
            Location from = new Location(bworld, prevX, prevY, prevZ, prevYaw, prevPitch);
            Location to = new Location(bworld, this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            this.worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleUpdateEvent(vehicle));
            if (!from.equals(to))
            {
                this.worldObj.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleMoveEvent(vehicle, from, to));
            }
            // CraftBukkit end
            AxisAlignedBB box = null;
            if (getCollisionHandler() != null)
            {
                box = getCollisionHandler().getMinecartCollisionBox(this);
            }
            else
            {
                box = boundingBox.expand(0.2D, 0.0D, 0.2D);
            }

            List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, box);

            if (list != null && !list.isEmpty())
            {
                for (int l1 = 0; l1 < list.size(); ++l1)
                {
                    Entity entity = (Entity)list.get(l1);

                    if (entity != this.riddenByEntity && entity.canBePushed() && entity instanceof EntityMinecart)
                    {
                        entity.applyEntityCollision(this);
                    }
                }
            }

            if (this.riddenByEntity != null && this.riddenByEntity.isDead)
            {
                if (this.riddenByEntity.ridingEntity == this)
                {
                    this.riddenByEntity.ridingEntity = null;
                }

                this.riddenByEntity = null;
            }

            updateFuel();
            MinecraftForge.EVENT_BUS.post(new MinecartUpdateEvent(this, j, i, k));
        }
    }

    @SideOnly(Side.CLIENT)
    public Vec3 func_70495_a(double par1, double par3, double par5, double par7)
    {
        int i = MathHelper.floor_double(par1);
        int j = MathHelper.floor_double(par3);
        int k = MathHelper.floor_double(par5);

        if (BlockRail.isRailBlockAt(this.worldObj, i, j - 1, k))
        {
            --j;
        }

        int l = this.worldObj.getBlockId(i, j, k);

        if (!BlockRail.isRailBlock(l))
        {
            return null;
        }
        else
        {
            int i1 = ((BlockRail)Block.blocksList[l]).getBasicRailMetadata(worldObj, this, i, j, k);

            par3 = (double)j;

            if (i1 >= 2 && i1 <= 5)
            {
                par3 = (double)(j + 1);
            }

            int[][] aint = matrix[i1];
            double d4 = (double)(aint[1][0] - aint[0][0]);
            double d5 = (double)(aint[1][2] - aint[0][2]);
            double d6 = Math.sqrt(d4 * d4 + d5 * d5);
            d4 /= d6;
            d5 /= d6;
            par1 += d4 * par7;
            par5 += d5 * par7;

            if (aint[0][1] != 0 && MathHelper.floor_double(par1) - i == aint[0][0] && MathHelper.floor_double(par5) - k == aint[0][2])
            {
                par3 += (double)aint[0][1];
            }
            else if (aint[1][1] != 0 && MathHelper.floor_double(par1) - i == aint[1][0] && MathHelper.floor_double(par5) - k == aint[1][2])
            {
                par3 += (double)aint[1][1];
            }

            return this.func_70489_a(par1, par3, par5);
        }
    }

    public Vec3 func_70489_a(double par1, double par3, double par5)
    {
        int i = MathHelper.floor_double(par1);
        int j = MathHelper.floor_double(par3);
        int k = MathHelper.floor_double(par5);

        if (BlockRail.isRailBlockAt(this.worldObj, i, j - 1, k))
        {
            --j;
        }

        int l = this.worldObj.getBlockId(i, j, k);

        if (BlockRail.isRailBlock(l))
        {
            int i1 = ((BlockRail)Block.blocksList[l]).getBasicRailMetadata(worldObj, this, i, j, k);
            par3 = (double)j;

            if (i1 >= 2 && i1 <= 5)
            {
                par3 = (double)(j + 1);
            }

            int[][] aint = matrix[i1];
            double d3 = 0.0D;
            double d4 = (double)i + 0.5D + (double)aint[0][0] * 0.5D;
            double d5 = (double)j + 0.5D + (double)aint[0][1] * 0.5D;
            double d6 = (double)k + 0.5D + (double)aint[0][2] * 0.5D;
            double d7 = (double)i + 0.5D + (double)aint[1][0] * 0.5D;
            double d8 = (double)j + 0.5D + (double)aint[1][1] * 0.5D;
            double d9 = (double)k + 0.5D + (double)aint[1][2] * 0.5D;
            double d10 = d7 - d4;
            double d11 = (d8 - d5) * 2.0D;
            double d12 = d9 - d6;

            if (d10 == 0.0D)
            {
                par1 = (double)i + 0.5D;
                d3 = par5 - (double)k;
            }
            else if (d12 == 0.0D)
            {
                par5 = (double)k + 0.5D;
                d3 = par1 - (double)i;
            }
            else
            {
                double d13 = par1 - d4;
                double d14 = par5 - d6;
                d3 = (d13 * d10 + d14 * d12) * 2.0D;
            }

            par1 = d4 + d10 * d3;
            par3 = d5 + d11 * d3;
            par5 = d6 + d12 * d3;

            if (d11 < 0.0D)
            {
                ++par3;
            }

            if (d11 > 0.0D)
            {
                par3 += 0.5D;
            }

            return this.worldObj.getWorldVec3Pool().getVecFromPool(par1, par3, par5);
        }
        else
        {
            return null;
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        par1NBTTagCompound.setInteger("Type", this.minecartType);

        if (isPoweredCart())
        {
            par1NBTTagCompound.setDouble("PushX", this.pushX);
            par1NBTTagCompound.setDouble("PushZ", this.pushZ);
            par1NBTTagCompound.setInteger("Fuel", this.fuel);
        }

        if (getSizeInventory() > 0)
        {
            NBTTagList nbttaglist = new NBTTagList();

            for (int i = 0; i < this.cargoItems.length; ++i)
            {
                if (this.cargoItems[i] != null)
                {
                    NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                    nbttagcompound1.setByte("Slot", (byte)i);
                    this.cargoItems[i].writeToNBT(nbttagcompound1);
                    nbttaglist.appendTag(nbttagcompound1);
                }
            }

            par1NBTTagCompound.setTag("Items", nbttaglist);
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        this.minecartType = par1NBTTagCompound.getInteger("Type");

        if (isPoweredCart())
        {
            this.pushX = par1NBTTagCompound.getDouble("PushX");
            this.pushZ = par1NBTTagCompound.getDouble("PushZ");
            try
            {
                this.fuel = par1NBTTagCompound.getInteger("Fuel");
            }
            catch (ClassCastException e)
            {
                this.fuel = par1NBTTagCompound.getShort("Fuel");
            }
        }

        if (getSizeInventory() > 0)
        {
            NBTTagList nbttaglist = par1NBTTagCompound.getTagList("Items");
            this.cargoItems = new ItemStack[this.getSizeInventory()];

            for (int i = 0; i < nbttaglist.tagCount(); ++i)
            {
                NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(i);
                int j = nbttagcompound1.getByte("Slot") & 255;

                if (j >= 0 && j < this.cargoItems.length)
                {
                    this.cargoItems[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize()
    {
        return 0.0F;
    }

    /**
     * Applies a velocity to each of the entities pushing them away from each other. Args: entity
     */
    public void applyEntityCollision(Entity par1Entity)
    {
        MinecraftForge.EVENT_BUS.post(new MinecartCollisionEvent(this, par1Entity));
        if (getCollisionHandler() != null)
        {
            getCollisionHandler().onEntityCollision(this, par1Entity);
            return;
        }
        if (!this.worldObj.isRemote)
        {
            if (par1Entity != this.riddenByEntity)
            {
                // CraftBukkit start
                Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                org.bukkit.entity.Entity hitEntity = (par1Entity == null) ? null : par1Entity.getBukkitEntity();
                VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, hitEntity);
                this.worldObj.getServer().getPluginManager().callEvent(collisionEvent);

                if (collisionEvent.isCancelled())
                {
                    return;
                }

                // CraftBukkit end

                if (par1Entity instanceof EntityLiving && !(par1Entity instanceof EntityPlayer) && !(par1Entity instanceof EntityIronGolem) && canBeRidden() && this.motionX * this.motionX + this.motionZ * this.motionZ > 0.01D && this.riddenByEntity == null && par1Entity.ridingEntity == null)
                {
                    par1Entity.mountEntity(this);
                }

                double d0 = par1Entity.posX - this.posX;
                double d1 = par1Entity.posZ - this.posZ;
                double d2 = d0 * d0 + d1 * d1;

                // CraftBukkit - collision
                if (d2 >= 9.999999747378752E-5D && !collisionEvent.isCollisionCancelled())
                {
                    d2 = (double)MathHelper.sqrt_double(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D)
                    {
                        d3 = 1.0D;
                    }

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.10000000149011612D;
                    d1 *= 0.10000000149011612D;
                    d0 *= (double)(1.0F - this.entityCollisionReduction);
                    d1 *= (double)(1.0F - this.entityCollisionReduction);
                    d0 *= 0.5D;
                    d1 *= 0.5D;

                    if (par1Entity instanceof EntityMinecart)
                    {
                        double d4 = par1Entity.posX - this.posX;
                        double d5 = par1Entity.posZ - this.posZ;
                        Vec3 vec3 = this.worldObj.getWorldVec3Pool().getVecFromPool(d4, 0.0D, d5).normalize();
                        Vec3 vec31 = this.worldObj.getWorldVec3Pool().getVecFromPool((double)MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F), 0.0D, (double)MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F)).normalize();
                        double d6 = Math.abs(vec3.dotProduct(vec31));

                        if (d6 < 0.800000011920929D)
                        {
                            return;
                        }

                        double d7 = par1Entity.motionX + this.motionX;
                        double d8 = par1Entity.motionZ + this.motionZ;

                        if (((EntityMinecart)par1Entity).isPoweredCart() && !isPoweredCart())
                        {
                            this.motionX *= 0.20000000298023224D;
                            this.motionZ *= 0.20000000298023224D;
                            this.addVelocity(par1Entity.motionX - d0, 0.0D, par1Entity.motionZ - d1);
                            par1Entity.motionX *= 0.949999988079071D;
                            par1Entity.motionZ *= 0.949999988079071D;
                        }
                        else if (!((EntityMinecart)par1Entity).isPoweredCart() && isPoweredCart())
                        {
                            par1Entity.motionX *= 0.20000000298023224D;
                            par1Entity.motionZ *= 0.20000000298023224D;
                            par1Entity.addVelocity(this.motionX + d0, 0.0D, this.motionZ + d1);
                            this.motionX *= 0.949999988079071D;
                            this.motionZ *= 0.949999988079071D;
                        }
                        else
                        {
                            d7 /= 2.0D;
                            d8 /= 2.0D;
                            this.motionX *= 0.20000000298023224D;
                            this.motionZ *= 0.20000000298023224D;
                            this.addVelocity(d7 - d0, 0.0D, d8 - d1);
                            par1Entity.motionX *= 0.20000000298023224D;
                            par1Entity.motionZ *= 0.20000000298023224D;
                            par1Entity.addVelocity(d7 + d0, 0.0D, d8 + d1);
                        }
                    }
                    else
                    {
                        this.addVelocity(-d0, 0.0D, -d1);
                        par1Entity.addVelocity(d0 / 4.0D, 0.0D, d1 / 4.0D);
                    }
                }
            }
        }
    }

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return (minecartType == 1 && getClass() == EntityMinecart.class ? 27 : 0);
    }

    /**
     * Returns the stack in slot i
     */
    public ItemStack getStackInSlot(int par1)
    {
        return this.cargoItems[par1];
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.cargoItems[par1] != null)
        {
            ItemStack itemstack;

            if (this.cargoItems[par1].stackSize <= par2)
            {
                itemstack = this.cargoItems[par1];
                this.cargoItems[par1] = null;
                return itemstack;
            }
            else
            {
                itemstack = this.cargoItems[par1].splitStack(par2);

                if (this.cargoItems[par1].stackSize == 0)
                {
                    this.cargoItems[par1] = null;
                }

                return itemstack;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (this.cargoItems[par1] != null)
        {
            ItemStack itemstack = this.cargoItems[par1];
            this.cargoItems[par1] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        this.cargoItems[par1] = par2ItemStack;

        if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit())
        {
            par2ItemStack.stackSize = this.getInventoryStackLimit();
        }
    }

    /**
     * Returns the name of the inventory.
     */
    public String getInvName()
    {
        return "container.minecart";
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return maxStack; // CraftBukkit
    }

    /**
     * Called when an the contents of an Inventory change, usually
     */
    public void onInventoryChanged() {}

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer par1EntityPlayer)
    {
        if (MinecraftForge.EVENT_BUS.post(new MinecartInteractEvent(this, par1EntityPlayer)))
        {
            return true;
        }

        if (canBeRidden())
        {
            if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer && this.riddenByEntity != par1EntityPlayer)
            {
                return true;
            }

            if (!this.worldObj.isRemote)
            {
                par1EntityPlayer.mountEntity(this);
            }
        }
        else if (getSizeInventory() > 0)
        {
            if (!this.worldObj.isRemote)
            {
                par1EntityPlayer.displayGUIChest(this);
            }
        }
        else if (this.minecartType == 2 && getClass() == EntityMinecart.class)
        {
            ItemStack itemstack = par1EntityPlayer.inventory.getCurrentItem();

            if (itemstack != null && itemstack.itemID == Item.coal.itemID)
            {
                if (--itemstack.stackSize == 0)
                {
                    par1EntityPlayer.inventory.setInventorySlotContents(par1EntityPlayer.inventory.currentItem, (ItemStack)null);
                }

                this.fuel += 3600;
            }

            this.pushX = this.posX - par1EntityPlayer.posX;
            this.pushZ = this.posZ - par1EntityPlayer.posZ;
        }

        return true;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Sets the position and rotation. Only difference from the other one is no bounding on the rotation. Args: posX,
     * posY, posZ, yaw, pitch
     */
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9)
    {
        this.minecartX = par1;
        this.minecartY = par3;
        this.minecartZ = par5;
        this.minecartYaw = (double)par7;
        this.minecartPitch = (double)par8;
        this.turnProgress = par9 + 2;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Sets the velocity to the args. Args: x, y, z
     */
    public void setVelocity(double par1, double par3, double par5)
    {
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes with Container
     */
    public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
    {
        return this.isDead ? false : par1EntityPlayer.getDistanceSqToEntity(this) <= 64.0D;
    }

    /**
     * Is this minecart powered (Fuel > 0)
     */
    public boolean isMinecartPowered()
    {
        return (this.dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    /**
     * Set if this minecart is powered (Fuel > 0)
     */
    protected void setMinecartPowered(boolean par1)
    {
        if (par1)
        {
            this.dataWatcher.updateObject(16, Byte.valueOf((byte)(this.dataWatcher.getWatchableObjectByte(16) | 1)));
        }
        else
        {
            this.dataWatcher.updateObject(16, Byte.valueOf((byte)(this.dataWatcher.getWatchableObjectByte(16) & -2)));
        }
    }

    public void openChest() {}

    public void closeChest() {}

    /**
     * Sets the current amount of damage the minecart has taken. Decreases over time. The cart breaks when this is over
     * 40.
     */
    public void setDamage(int par1)
    {
        this.dataWatcher.updateObject(19, Integer.valueOf(par1));
    }

    /**
     * Gets the current amount of damage the minecart has taken. Decreases over time. The cart breaks when this is over
     * 40.
     */
    public int getDamage()
    {
        return this.dataWatcher.getWatchableObjectInt(19);
    }

    /**
     * Sets the rolling amplitude the cart rolls while being attacked.
     */
    public void setRollingAmplitude(int par1)
    {
        this.dataWatcher.updateObject(17, Integer.valueOf(par1));
    }

    /**
     * Gets the rolling amplitude the cart rolls while being attacked.
     */
    public int getRollingAmplitude()
    {
        return this.dataWatcher.getWatchableObjectInt(17);
    }

    /**
     * Sets the rolling direction the cart rolls while being attacked. Can be 1 or -1.
     */
    public void setRollingDirection(int par1)
    {
        this.dataWatcher.updateObject(18, Integer.valueOf(par1));
    }

    /**
     * Gets the rolling direction the cart rolls while being attacked. Can be 1 or -1.
     */
    public int getRollingDirection()
    {
        return this.dataWatcher.getWatchableObjectInt(18);
    }

    // CraftBukkit start - methods for getting and setting flying and derailed velocity modifiers
    public Vector getFlyingVelocityMod()
    {
        return new Vector(flyingX, flyingY, flyingZ);
    }

    public void setFlyingVelocityMod(Vector flying)
    {
        flyingX = flying.getX();
        flyingY = flying.getY();
        flyingZ = flying.getZ();
    }

    public Vector getDerailedVelocityMod()
    {
        return new Vector(derailedX, derailedY, derailedZ);
    }

    public void setDerailedVelocityMod(Vector derailed)
    {
        derailedX = derailed.getX();
        derailedY = derailed.getY();
        derailedZ = derailed.getZ();
    }
    // CraftBukkit end
    /* =================================================== FORGE START =====================================*/
    /**
     * Drops the cart as a item. The exact item dropped is defined by getItemDropped().
     */
    public void dropCartAsItem()
    {
        for(ItemStack item : getItemsDropped())
        {
            entityDropItem(item, 0);
        }
    }

    /**
     * Override this to define which items your cart drops when broken.
     * This does not include items contained in the inventory,
     * that is handled elsewhere.
     * @return A list of items dropped.
     */
    public List<ItemStack> getItemsDropped()
    {
        List<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(Item.minecartEmpty));

        switch(minecartType)
        {
            case 1:
                items.add(new ItemStack(Block.chest));
                break;
            case 2:
                items.add(new ItemStack(Block.stoneOvenIdle));
                break;
        }
        return items;
    }

    /**
     * This function returns an ItemStack that represents this cart.
     * This should be an ItemStack that can be used by the player to place the cart.
     * This is the item that was registered with the cart via the registerMinecart function,
     * but is not necessary the item the cart drops when destroyed.
     * @return An ItemStack that can be used to place the cart.
     */
    public ItemStack getCartItem()
    {
        return MinecartRegistry.getItemForCart(this);
    }

    /**
     * Returns true if this cart is self propelled.
     * @return True if powered.
     */
    public boolean isPoweredCart()
    {
        return minecartType == 2 && getClass() == EntityMinecart.class;
    }

    /**
     * Returns true if this cart is a storage cart
     * Some carts may have inventories but not be storage carts
     * and some carts without inventories may be storage carts.
     * @return True if this cart should be classified as a storage cart.
     */
    public boolean isStorageCart()
    {
        return minecartType == 1 && getClass() == EntityMinecart.class;
    }

    /**
     * Returns true if this cart can be ridden by an Entity.
     * @return True if this cart can be ridden.
     */
    public boolean canBeRidden()
    {
        if(minecartType == 0 && getClass() == EntityMinecart.class)
        {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this cart can currently use rails.
     * This function is mainly used to gracefully detach a minecart from a rail.
     * @return True if the minecart can use rails.
     */
    public boolean canUseRail()
    {
        return canUseRail;
    }

    /**
     * Set whether the minecart can use rails.
     * This function is mainly used to gracefully detach a minecart from a rail.
     * @param use Whether the minecart can currently use rails.
     */
    public void setCanUseRail(boolean use)
    {
        canUseRail = use;
    }

    /**
     * Return false if this cart should not call IRail.onMinecartPass() and should ignore Powered Rails.
     * @return True if this cart should call IRail.onMinecartPass().
     */
    public boolean shouldDoRailFunctions()
    {
        return true;
    }

    /**
     * Simply returns the minecartType variable.
     * @return minecartType
     */
    public int getMinecartType()
    {
        return minecartType;
    }

    /**
     * Gets the current global Minecart Collision handler if none
     * is registered, returns null
     * @return The collision handler or null
     */
    public static IMinecartCollisionHandler getCollisionHandler()
    {
        return collisionHandler;
    }

    /**
     * Sets the global Minecart Collision handler, overwrites any
     * that is currently set.
     * @param handler The new handler
     */
    public static void setCollisionHandler(IMinecartCollisionHandler handler)
    {
        collisionHandler = handler;
    }

    /**
     * Carts should return their drag factor here
     * @return The drag rate.
     */
    protected double getDrag()
    {
        return riddenByEntity != null ? defaultDragRidden : defaultDragEmpty;
    }

    /**
     * Moved to allow overrides.
     * This code applies drag and updates push forces.
     */
    protected void applyDragAndPushForces()
    {
        if(isPoweredCart())
        {
            double d27 = MathHelper.sqrt_double(pushX * pushX + pushZ * pushZ);
            if(d27 > 0.01D)
            {
                pushX /= d27;
                pushZ /= d27;
                double d29 = 0.04;
                motionX *= 0.8D;
                motionY *= 0.0D;
                motionZ *= 0.8D;
                motionX += pushX * d29;
                motionZ += pushZ * d29;
            }
            else
            {
                motionX *= 0.9D;
                motionY *= 0.0D;
                motionZ *= 0.9D;
            }
        }
        motionX *= getDrag();
        motionY *= 0.0D;
        motionZ *= getDrag();
    }

    /**
     * Moved to allow overrides.
     * This code updates push forces.
     */
    protected void updatePushForces()
    {
        if(isPoweredCart())
        {
            double push = MathHelper.sqrt_double(pushX * pushX + pushZ * pushZ);
            if(push > 0.01D && motionX * motionX + motionZ * motionZ > 0.001D)
            {
                pushX /= push;
                pushZ /= push;
                if(pushX * motionX + pushZ * motionZ < 0.0D)
                {
                    pushX = 0.0D;
                    pushZ = 0.0D;
                }
                else
                {
                    pushX = motionX;
                    pushZ = motionZ;
                }
            }
        }
    }

    /**
     * Moved to allow overrides.
     * This code handles minecart movement and speed capping when on a rail.
     */
    protected void moveMinecartOnRail(int i, int j, int k)
    {
        int id = worldObj.getBlockId(i, j, k);
        if (!BlockRail.isRailBlock(id))
        {
                return;
        }
        float railMaxSpeed = ((BlockRail)Block.blocksList[id]).getRailMaxSpeed(worldObj, this, i, j, k);

        double maxSpeed = Math.min(railMaxSpeed, getMaxSpeedRail());
        double mX = motionX;
        double mZ = motionZ;
        if(riddenByEntity != null)
        {
            mX *= 0.75D;
            mZ *= 0.75D;
        }
        if(mX < -maxSpeed) mX = -maxSpeed;
        if(mX >  maxSpeed) mX =  maxSpeed;
        if(mZ < -maxSpeed) mZ = -maxSpeed;
        if(mZ >  maxSpeed) mZ =  maxSpeed;
        moveEntity(mX, 0.0D, mZ);
    }

    /**
     * Moved to allow overrides.
     * This code handles minecart movement and speed capping when not on a rail.
     */
    protected void moveMinecartOffRail(int i, int j, int k)
    {
        double d2 = getMaxSpeedGround();
        if(!onGround)
        {
            d2 = getMaxSpeedAirLateral();
        }
        if(motionX < -d2) motionX = -d2;
        if(motionX >  d2) motionX =  d2;
        if(motionZ < -d2) motionZ = -d2;
        if(motionZ >  d2) motionZ =  d2;
        double moveY = motionY;
        if(getMaxSpeedAirVertical() > 0 && motionY > getMaxSpeedAirVertical())
        {
            moveY = getMaxSpeedAirVertical();
            if(Math.abs(motionX) < 0.3f && Math.abs(motionZ) < 0.3f)
            {
                moveY = 0.15f;
                motionY = moveY;
            }
        }
        if(onGround)
        {
            motionX *= 0.5D;
            motionY *= 0.5D;
            motionZ *= 0.5D;
        }
        moveEntity(motionX, moveY, motionZ);
        if(!onGround)
        {
            motionX *= getDragAir();
            motionY *= getDragAir();
            motionZ *= getDragAir();
        }
    }

    /**
     * Moved to allow overrides.
     * This code applies fuel consumption.
     */
    protected void updateFuel()
    {
        if (fuel > 0) fuel--;
        if (fuel <= 0) pushX = pushZ = 0.0D;
        setMinecartPowered(fuel > 0);
    }

    /**
     * Moved to allow overrides, This code handle slopes affecting velocity.
     * @param metadata The blocks position metadata
     */
    protected void adjustSlopeVelocities(int metadata)
    {
        double acceleration = 0.0078125D;
        if (metadata == 2)
        {
            motionX -= acceleration;
        }
        else if (metadata == 3)
        {
            motionX += acceleration;
        }
        else if (metadata == 4)
        {
            motionZ += acceleration;
        }
        else if (metadata == 5)
        {
            motionZ -= acceleration;
        }
    }

    /**
     * Getters/setters for physics variables
     */

    /**
     * Returns the carts max speed.
     * Carts going faster than 1.1 cause issues with chunk loading.
     * Carts cant traverse slopes or corners at greater than 0.5 - 0.6.
     * This value is compared with the rails max speed to determine
     * the carts current max speed. A normal rails max speed is 0.4.
     * @return Carts max speed.
     */
    public float getMaxSpeedRail()
    {
        return maxSpeedRail;
    }

    public void setMaxSpeedRail(float value)
    {
        maxSpeedRail = value;
    }

    public float getMaxSpeedGround()
    {
        return maxSpeedGround;
    }

    public void setMaxSpeedGround(float value)
    {
        maxSpeedGround = value;
    }

    public float getMaxSpeedAirLateral()
    {
        return maxSpeedAirLateral;
    }

    public void setMaxSpeedAirLateral(float value)
    {
        maxSpeedAirLateral = value;
    }

    public float getMaxSpeedAirVertical()
    {
        return maxSpeedAirVertical;
    }

    public void setMaxSpeedAirVertical(float value)
    {
        maxSpeedAirVertical = value;
    }

    public double getDragAir()
    {
        return dragAir;
    }

    public void setDragAir(double value)
    {
        dragAir = value;
    }
}
