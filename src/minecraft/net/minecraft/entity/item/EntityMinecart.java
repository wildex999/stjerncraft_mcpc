package net.minecraft.entity.item;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityMinecartMobSpawner;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
// CraftBukkit end
import net.minecraftforge.common.IMinecartCollisionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartCollisionEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;

public abstract class EntityMinecart extends Entity
{
    protected boolean isInReverse;
    protected final IUpdatePlayerListBox field_82344_g;
    protected String field_94102_c;

    /** Minecart rotational logic matrix */
    protected static final int[][][] matrix = new int[][][] {{{0, 0, -1}, {0, 0, 1}}, {{ -1, 0, 0}, {1, 0, 0}}, {{ -1, -1, 0}, {1, 0, 0}}, {{ -1, 0, 0}, {1, -1, 0}}, {{0, 0, -1}, {0, -1, 1}}, {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, { -1, 0, 0}}, {{0, 0, -1}, { -1, 0, 0}}, {{0, 0, -1}, {1, 0, 0}}};

    /** appears to be the progress of the turn */
    protected int turnProgress;
    protected double minecartX;
    protected double minecartY;
    protected double minecartZ;
    protected double minecartYaw;
    protected double minecartPitch;

    // CraftBukkit start
    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public double maxSpeed = 0.4D;
    // CraftBukkit end
    
    /* Forge: Minecart Compatibility Layer Integration. */
    public static float defaultMaxSpeedAirLateral = 0.4f;
    public static float defaultMaxSpeedAirVertical = -1f;
    public static double defaultDragAir = 0.94999998807907104D;
    protected boolean canUseRail = true;
    protected boolean canBePushed = true;
    private static IMinecartCollisionHandler collisionHandler = null;

    /* Instance versions of the above physics properties */
    private float currentSpeedRail = getMaxCartSpeedOnRail();
    protected float maxSpeedAirLateral = defaultMaxSpeedAirLateral;
    protected float maxSpeedAirVertical = defaultMaxSpeedAirVertical;
    protected double dragAir = defaultDragAir;    

    public EntityMinecart(World par1World)
    {
        super(par1World);
        this.isInReverse = false;
        this.preventEntitySpawning = true;
        this.setSize(0.98F, 0.7F);
        this.yOffset = this.height / 2.0F;
        this.field_82344_g = par1World != null ? par1World.func_82735_a(this) : null;
    }

    public static EntityMinecart func_94090_a(World par0World, double par1, double par3, double par5, int par7)
    {
        switch (par7)
        {
            case 1:
                return new EntityMinecartChest(par0World, par1, par3, par5);
            case 2:
                return new EntityMinecartFurnace(par0World, par1, par3, par5);
            case 3:
                return new EntityMinecartTNT(par0World, par1, par3, par5);
            case 4:
                return new EntityMinecartMobSpawner(par0World, par1, par3, par5);
            case 5:
                return new EntityMinecartHopper(par0World, par1, par3, par5);
            default:
                return new EntityMinecartEmpty(par0World, par1, par3, par5);
        }
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
        this.dataWatcher.addObject(17, new Integer(0));
        this.dataWatcher.addObject(18, new Integer(1));
        this.dataWatcher.addObject(19, new Integer(0));
        this.dataWatcher.addObject(20, new Integer(0));
        this.dataWatcher.addObject(21, new Integer(6));
        this.dataWatcher.addObject(22, Byte.valueOf((byte)0));
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

    public EntityMinecart(World par1World, double par2, double par4, double par6)
    {
        this(par1World);
        this.setPosition(par2, par4 + (double)this.yOffset, par6);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
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
        if (!this.worldObj.isRemote && !this.isDead)
        {
            if (this.isEntityInvulnerable())
            {
                return false;
            }
            else
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
                boolean flag = par1DamageSource.getEntity() instanceof EntityPlayer && ((EntityPlayer)par1DamageSource.getEntity()).capabilities.isCreativeMode;

                if (flag || this.getDamage() > 40)
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

                    if (flag && !this.isInvNameLocalized())
                    {
                        this.setDead();
                    }
                    else
                    {
                        this.func_94095_a(par1DamageSource);
                    }
                }

                return true;
            }
        }
        else
        {
            return true;
        }
    }

    public void func_94095_a(DamageSource par1DamageSource)
    {
        this.setDead();
        ItemStack itemstack = new ItemStack(Item.minecartEmpty, 1);

        if (this.field_94102_c != null)
        {
            itemstack.setItemName(this.field_94102_c);
        }

        this.entityDropItem(itemstack, 0.0F);
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
        super.setDead();

        if (this.field_82344_g != null)
        {
            this.field_82344_g.update();
        }
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

        int i;

        if (!this.worldObj.isRemote && this.worldObj instanceof WorldServer)
        {
            this.worldObj.theProfiler.startSection("portal");
            MinecraftServer minecraftserver = ((WorldServer)this.worldObj).getMinecraftServer();
            i = this.getMaxInPortalTime();

            if (this.inPortal)
            {
                if (true || minecraftserver.getAllowNether())   // CraftBukkit - multi-world should still allow teleport even if default vanilla nether disabled
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

            if (BlockRailBase.isRailBlockAt(this.worldObj, j, i - 1, k))
            {
                --i;
            }

            double d4 = this.maxSpeed; // CraftBukkit
            double d5 = 0.0078125D;
            int l = this.worldObj.getBlockId(j, i, k);

            if (canUseRail() && BlockRailBase.isRailBlock(l))
            {
                BlockRailBase rail = (BlockRailBase)Block.blocksList[l];
                float railMaxSpeed = rail.getRailMaxSpeed(worldObj, this, j, i, k);
                double maxSpeed = Math.min(railMaxSpeed, getCurrentCartSpeedCapOnRail());
                int i1 = rail.getBasicRailMetadata(worldObj, this, j, i, k);
                this.func_94091_a(j, i, k, maxSpeed, getSlopeAdjustment(), l, i1);

                if (l == Block.railActivator.blockID)
                {
                    this.func_96095_a(j, i, k, (worldObj.getBlockMetadata(j, i, k) & 8) != 0);
                }
            }
            else
            {
                this.func_94088_b(onGround ? d4 : getMaxSpeedAirLateral());
            }

            this.doBlockCollisions();
            this.rotationPitch = 0.0F;
            double d6 = this.prevPosX - this.posX;
            double d7 = this.prevPosZ - this.posZ;

            if (d6 * d6 + d7 * d7 > 0.001D)
            {
                this.rotationYaw = (float)(Math.atan2(d7, d6) * 180.0D / Math.PI);

                if (this.isInReverse)
                {
                    this.rotationYaw += 180.0F;
                }
            }

            double d8 = (double)MathHelper.wrapAngleTo180_float(this.rotationYaw - this.prevRotationYaw);

            if (d8 < -170.0D || d8 >= 170.0D)
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
            AxisAlignedBB box;
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
                for (int j1 = 0; j1 < list.size(); ++j1)
                {
                    Entity entity = (Entity)list.get(j1);

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

            MinecraftForge.EVENT_BUS.post(new MinecartUpdateEvent(this, j, i, k));
        }
    }

    public void func_96095_a(int par1, int par2, int par3, boolean par4) {}

    protected void func_94088_b(double par1)
    {
        if (this.motionX < -par1)
        {
            this.motionX = -par1;
        }

        if (this.motionX > par1)
        {
            this.motionX = par1;
        }

        if (this.motionZ < -par1)
        {
            this.motionZ = -par1;
        }

        if (this.motionZ > par1)
        {
            this.motionZ = par1;
        }
        
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
        
        if (this.onGround)
        {
            // CraftBukkit start
            this.motionX *= this.derailedX;
            this.motionY *= this.derailedY;
            this.motionZ *= this.derailedZ;
            // CraftBukkit end
        }

        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        if (!this.onGround)
        {
            // CraftBukkit start // MCPC+ - CB changed to flyingX but Forge changed to getDragAir() - prefer Forge in this case
            this.motionX *= getDragAir();
            this.motionY *= getDragAir();
            this.motionZ *= getDragAir();
            // CraftBukkit end
        }
    }

    protected void func_94091_a(int par1, int par2, int par3, double par4, double par6, int par8, int par9)
    {
        this.fallDistance = 0.0F;
        Vec3 vec3 = this.func_70489_a(this.posX, this.posY, this.posZ);
        this.posY = (double)par2;
        boolean flag = false;
        boolean flag1 = false;

        if (par8 == Block.railPowered.blockID)
        {
            flag = (worldObj.getBlockMetadata(par1, par2, par3) & 8) != 0;
            flag1 = !flag;
        }

        if (((BlockRailBase)Block.blocksList[par8]).isPowered())
        {
            par9 &= 7;
        }

        if (par9 >= 2 && par9 <= 5)
        {
            this.posY = (double)(par2 + 1);
        }

        if (par9 == 2)
        {
            this.motionX -= par6;
        }

        if (par9 == 3)
        {
            this.motionX += par6;
        }

        if (par9 == 4)
        {
            this.motionZ += par6;
        }

        if (par9 == 5)
        {
            this.motionZ -= par6;
        }

        int[][] aint = matrix[par9];
        double d2 = (double)(aint[1][0] - aint[0][0]);
        double d3 = (double)(aint[1][2] - aint[0][2]);
        double d4 = Math.sqrt(d2 * d2 + d3 * d3);
        double d5 = this.motionX * d2 + this.motionZ * d3;

        if (d5 < 0.0D)
        {
            d2 = -d2;
            d3 = -d3;
        }

        double d6 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

        if (d6 > 2.0D)
        {
            d6 = 2.0D;
        }

        this.motionX = d6 * d2 / d4;
        this.motionZ = d6 * d3 / d4;
        double d7;
        double d8;

        if (this.riddenByEntity != null)
        {
            d7 = this.riddenByEntity.motionX * this.riddenByEntity.motionX + this.riddenByEntity.motionZ * this.riddenByEntity.motionZ;
            d8 = this.motionX * this.motionX + this.motionZ * this.motionZ;

            if (d7 > 1.0E-4D && d8 < 0.01D)
            {
                this.motionX += this.riddenByEntity.motionX * 0.1D;
                this.motionZ += this.riddenByEntity.motionZ * 0.1D;
                flag1 = false;
            }
        }

        if (flag1 && shouldDoRailFunctions())
        {
            d7 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

            if (d7 < 0.03D)
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

        d7 = 0.0D;
        d8 = (double)par1 + 0.5D + (double)aint[0][0] * 0.5D;
        double d9 = (double)par3 + 0.5D + (double)aint[0][2] * 0.5D;
        double d10 = (double)par1 + 0.5D + (double)aint[1][0] * 0.5D;
        double d11 = (double)par3 + 0.5D + (double)aint[1][2] * 0.5D;
        d2 = d10 - d8;
        d3 = d11 - d9;
        double d12;
        double d13;

        if (d2 == 0.0D)
        {
            this.posX = (double)par1 + 0.5D;
            d7 = this.posZ - (double)par3;
        }
        else if (d3 == 0.0D)
        {
            this.posZ = (double)par3 + 0.5D;
            d7 = this.posX - (double)par1;
        }
        else
        {
            d12 = this.posX - d8;
            d13 = this.posZ - d9;
            d7 = (d12 * d2 + d13 * d3) * 2.0D;
        }

        this.posX = d8 + d2 * d7;
        this.posZ = d9 + d3 * d7;
        this.setPosition(this.posX, this.posY + (double)this.yOffset, this.posZ);

        moveMinecartOnRail(par1, par2, par3, par4);

        if (aint[0][1] != 0 && MathHelper.floor_double(this.posX) - par1 == aint[0][0] && MathHelper.floor_double(this.posZ) - par3 == aint[0][2])
        {
            this.setPosition(this.posX, this.posY + (double)aint[0][1], this.posZ);
        }
        else if (aint[1][1] != 0 && MathHelper.floor_double(this.posX) - par1 == aint[1][0] && MathHelper.floor_double(this.posZ) - par3 == aint[1][2])
        {
            this.setPosition(this.posX, this.posY + (double)aint[1][1], this.posZ);
        }

        this.func_94101_h();
        Vec3 vec31 = this.func_70489_a(this.posX, this.posY, this.posZ);

        if (vec31 != null && vec3 != null)
        {
            double d14 = (vec3.yCoord - vec31.yCoord) * 0.05D;
            d6 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

            if (d6 > 0.0D)
            {
                this.motionX = this.motionX / d6 * (d6 + d14);
                this.motionZ = this.motionZ / d6 * (d6 + d14);
            }

            this.setPosition(this.posX, vec31.yCoord, this.posZ);
        }

        int j1 = MathHelper.floor_double(this.posX);
        int k1 = MathHelper.floor_double(this.posZ);

        if (j1 != par1 || k1 != par3)
        {
            d6 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.motionX = d6 * (double)(j1 - par1);
            this.motionZ = d6 * (double)(k1 - par3);
        }

        if(shouldDoRailFunctions())
        {
            ((BlockRailBase)Block.blocksList[par8]).onMinecartPass(worldObj, this, par1, par2, par3);
        }

        if (flag && shouldDoRailFunctions())
        {
            double d15 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

            if (d15 > 0.01D)
            {
                double d16 = 0.06D;
                this.motionX += this.motionX / d15 * d16;
                this.motionZ += this.motionZ / d15 * d16;
            }
            else if (par9 == 1)
            {
                if (this.worldObj.isBlockNormalCube(par1 - 1, par2, par3))
                {
                    this.motionX = 0.02D;
                }
                else if (this.worldObj.isBlockNormalCube(par1 + 1, par2, par3))
                {
                    this.motionX = -0.02D;
                }
            }
            else if (par9 == 0)
            {
                if (this.worldObj.isBlockNormalCube(par1, par2, par3 - 1))
                {
                    this.motionZ = 0.02D;
                }
                else if (this.worldObj.isBlockNormalCube(par1, par2, par3 + 1))
                {
                    this.motionZ = -0.02D;
                }
            }
        }
    }

    protected void func_94101_h()
    {
        if (this.riddenByEntity != null || !this.slowWhenEmpty)   // CraftBukkit
        {
            this.motionX *= 0.996999979019165D;
            this.motionY *= 0.0D;
            this.motionZ *= 0.996999979019165D;
        }
        else
        {
            this.motionX *= 0.9599999785423279D;
            this.motionY *= 0.0D;
            this.motionZ *= 0.9599999785423279D;
        }
    }

    public Vec3 func_70489_a(double par1, double par3, double par5)
    {
        int i = MathHelper.floor_double(par1);
        int j = MathHelper.floor_double(par3);
        int k = MathHelper.floor_double(par5);

        if (BlockRailBase.isRailBlockAt(this.worldObj, i, j - 1, k))
        {
            --j;
        }

        int l = this.worldObj.getBlockId(i, j, k);

        if (BlockRailBase.isRailBlock(l))
        {
            int i1 = ((BlockRailBase)Block.blocksList[l]).getBasicRailMetadata(worldObj, this, i, j, k);
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
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        if (par1NBTTagCompound.getBoolean("CustomDisplayTile"))
        {
            this.func_94094_j(par1NBTTagCompound.getInteger("DisplayTile"));
            this.func_94092_k(par1NBTTagCompound.getInteger("DisplayData"));
            this.func_94086_l(par1NBTTagCompound.getInteger("DisplayOffset"));
        }

        if (par1NBTTagCompound.hasKey("CustomName") && par1NBTTagCompound.getString("CustomName").length() > 0)
        {
            this.field_94102_c = par1NBTTagCompound.getString("CustomName");
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        if (this.func_94100_s())
        {
            par1NBTTagCompound.setBoolean("CustomDisplayTile", true);
            par1NBTTagCompound.setInteger("DisplayTile", this.func_94089_m() == null ? 0 : this.func_94089_m().blockID);
            par1NBTTagCompound.setInteger("DisplayData", this.func_94098_o());
            par1NBTTagCompound.setInteger("DisplayOffset", this.func_94099_q());
        }

        if (this.field_94102_c != null && this.field_94102_c.length() > 0)
        {
            par1NBTTagCompound.setString("CustomName", this.field_94102_c);
        }
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

                if (par1Entity instanceof EntityLiving && !(par1Entity instanceof EntityPlayer) && !(par1Entity instanceof EntityIronGolem) && this.func_94087_l() == 0 && this.motionX * this.motionX + this.motionZ * this.motionZ > 0.01D && this.riddenByEntity == null && par1Entity.ridingEntity == null)
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

    public abstract int func_94087_l();

    public Block func_94089_m()
    {
        if (!this.func_94100_s())
        {
            return this.func_94093_n();
        }
        else
        {
            int i = this.getDataWatcher().getWatchableObjectInt(20) & 65535;
            return i > 0 && i < Block.blocksList.length ? Block.blocksList[i] : null;
        }
    }

    public Block func_94093_n()
    {
        return null;
    }

    public int func_94098_o()
    {
        return !this.func_94100_s() ? this.func_94097_p() : this.getDataWatcher().getWatchableObjectInt(20) >> 16;
    }

    public int func_94097_p()
    {
        return 0;
    }

    public int func_94099_q()
    {
        return !this.func_94100_s() ? this.func_94085_r() : this.getDataWatcher().getWatchableObjectInt(21);
    }

    public int func_94085_r()
    {
        return 6;
    }

    public void func_94094_j(int par1)
    {
        this.getDataWatcher().updateObject(20, Integer.valueOf(par1 & 65535 | this.func_94098_o() << 16));
        this.func_94096_e(true);
    }

    public void func_94092_k(int par1)
    {
        Block block = this.func_94089_m();
        int j = block == null ? 0 : block.blockID;
        this.getDataWatcher().updateObject(20, Integer.valueOf(j & 65535 | par1 << 16));
        this.func_94096_e(true);
    }

    public void func_94086_l(int par1)
    {
        this.getDataWatcher().updateObject(21, Integer.valueOf(par1));
        this.func_94096_e(true);
    }

    public boolean func_94100_s()
    {
        return this.getDataWatcher().getWatchableObjectByte(22) == 1;
    }

    public void func_94096_e(boolean par1)
    {
        this.getDataWatcher().updateObject(22, Byte.valueOf((byte)(par1 ? 1 : 0)));
    }

    public void func_96094_a(String par1Str)
    {
        this.field_94102_c = par1Str;
    }

    /**
     * Gets the username of the entity.
     */
    public String getEntityName()
    {
        return this.field_94102_c != null ? this.field_94102_c : super.getEntityName();
    }

    /**
     * If this returns false, the inventory name will be used as an unlocalized name, and translated into the player's
     * language. Otherwise it will be used directly.
     */
    public boolean isInvNameLocalized()
    {
        return this.field_94102_c != null;
    }

    public String func_95999_t()
    {
        return this.field_94102_c;
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
    
    /**
     * Moved to allow overrides.
     * This code handles minecart movement and speed capping when on a rail.
     */
    public void moveMinecartOnRail(int x, int y, int z, double par4){
        double d12 = this.motionX;
        double d13 = this.motionZ;

        if (this.riddenByEntity != null)
        {
            d12 *= 0.75D;
            d13 *= 0.75D;
        }

        if (d12 < -par4)
        {
            d12 = -par4;
        }

        if (d12 > par4)
        {
            d12 = par4;
        }

        if (d13 < -par4)
        {
            d13 = -par4;
        }

        if (d13 > par4)
        {
            d13 = par4;
        }

        this.moveEntity(d12, 0.0D, d13);
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
     * This function returns an ItemStack that represents this cart.
     * This should be an ItemStack that can be used by the player to place the cart,
     * but is not necessary the item the cart drops when destroyed.
     * @return An ItemStack that can be used to place the cart.
     */
    public ItemStack getCartItem()
    {
        if (this instanceof EntityMinecartChest)
        {
            return new ItemStack(Item.minecartCrate);
        }
        else if (this instanceof EntityMinecartTNT)
        {
            return new ItemStack(Item.tntMinecart);
        }
        else if (this instanceof EntityMinecartFurnace)
        {
            return new ItemStack(Item.minecartPowered);
        }
        else if (this instanceof EntityMinecartHopper)
        {
            return new ItemStack(Item.hopperMinecart);
        }
        return new ItemStack(Item.minecartEmpty);
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
     * Return false if this cart should not call onMinecartPass() and should ignore Powered Rails.
     * @return True if this cart should call onMinecartPass().
     */
    public boolean shouldDoRailFunctions()
    {
        return true;
    }

    /**
     * Returns true if this cart is self propelled.
     * @return True if powered.
     */
    public boolean isPoweredCart()
    {
        return func_94087_l() == 2;
    }

    /**
     * Returns true if this cart can be ridden by an Entity.
     * @return True if this cart can be ridden.
     */
    public boolean canBeRidden()
    {
        if(this instanceof EntityMinecartEmpty)
        {
            return true;
        }
        return false;
    }

    /**
     * Getters/setters for physics variables
     */

    /**
     * Returns the carts max speed when traveling on rails. Carts going faster
     * than 1.1 cause issues with chunk loading. Carts cant traverse slopes or
     * corners at greater than 0.5 - 0.6. This value is compared with the rails
     * max speed and the carts current speed cap to determine the carts current
     * max speed. A normal rail's max speed is 0.4.
     *
     * @return Carts max speed.
     */
    public float getMaxCartSpeedOnRail()
    {
        return 1.2f;
    }

    /**
     * Returns the current speed cap for the cart when traveling on rails. This
     * functions differs from getMaxCartSpeedOnRail() in that it controls
     * current movement and cannot be overridden. The value however can never be
     * higher than getMaxCartSpeedOnRail().
     *
     * @return
     */
    public final float getCurrentCartSpeedCapOnRail()
    {
        return currentSpeedRail;
    }

    public final void setCurrentCartSpeedCapOnRail(float value)
    {
        value = Math.min(value, getMaxCartSpeedOnRail());
        currentSpeedRail = value;
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

    public double getSlopeAdjustment()
    {
        return 0.0078125D;
    }    
}
