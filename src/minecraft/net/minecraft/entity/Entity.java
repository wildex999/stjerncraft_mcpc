package net.minecraft.entity;

import cpw.mods.fml.common.FMLLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFluid;
import net.minecraft.block.StepSound;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;





// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.TravelAgent;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Vehicle;
import org.spigotmc.CustomTimingsHandler; // Spigot
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.plugin.PluginManager;

import w999.baseprotect.IWorldInteract;
import w999.baseprotect.PlayerData;
// CraftBukkit end
// MCPC+ start
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.world.Teleporter;
import net.minecraftforge.common.EnumHelper;
// MCPC+ end
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;

public abstract class Entity implements IWorldInteract
{
    // CraftBukkit start
    private static final int CURRENT_LEVEL = 2;
    static boolean isLevelAtLeast(NBTTagCompound tag, int level)
    {
        return tag.hasKey("Bukkit.updateLevel") && tag.getInteger("Bukkit.updateLevel") >= level;
    }
    // CraftBukkit end

    private static int nextEntityID = 0;
    public int entityId;
    public double renderDistanceWeight;

    /**
     * Blocks entities from spawning when they do their AABB check to make sure the spot is clear of entities that can
     * prevent spawning.
     */
    public boolean preventEntitySpawning;

    /** The entity that is riding this entity */
    public Entity riddenByEntity;

    /** The entity we are currently riding */
    public Entity ridingEntity;
    public boolean field_98038_p;

    /** Reference to the World object. */
    public World worldObj;
    public double prevPosX;
    public double prevPosY;
    public double prevPosZ;

    /** Entity position X */
    public double posX;

    /** Entity position Y */
    public double posY;

    /** Entity position Z */
    public double posZ;

    /** Entity motion X */
    public double motionX;

    /** Entity motion Y */
    public double motionY;

    /** Entity motion Z */
    public double motionZ;

    /** Entity rotation Yaw */
    public float rotationYaw;

    /** Entity rotation Pitch */
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;

    /** Axis aligned bounding box. */
    public final AxisAlignedBB boundingBox;
    public boolean onGround;

    /**
     * True if after a move this entity has collided with something on X- or Z-axis
     */
    public boolean isCollidedHorizontally;

    /**
     * True if after a move this entity has collided with something on Y-axis
     */
    public boolean isCollidedVertically;

    /**
     * True if after a move this entity has collided with something either vertically or horizontally
     */
    public boolean isCollided;
    public boolean velocityChanged;
    protected boolean isInWeb;
    public boolean field_70135_K;

    /**
     * Gets set by setDead, so this must be the flag whether an Entity is dead (inactive may be better term)
     */
    public boolean isDead;
    public float yOffset;

    /** How wide this entity is considered to be */
    public float width;

    /** How high this entity is considered to be */
    public float height;

    /** The previous ticks distance walked multiplied by 0.6 */
    public float prevDistanceWalkedModified;

    /** The distance walked multiplied by 0.6 */
    public float distanceWalkedModified;
    public float distanceWalkedOnStepModified;
    public float fallDistance;

    /**
     * The distance that has to be exceeded in order to triger a new step sound and an onEntityWalking event on a block
     */
    private int nextStepDistance;

    /**
     * The entity's X coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosX;

    /**
     * The entity's Y coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosY;

    /**
     * The entity's Z coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosZ;
    public float ySize;

    /**
     * How high this entity can step up when running into a block to try to get over it (currently make note the entity
     * will always step up this amount and not just the amount needed)
     */
    public float stepHeight;

    /**
     * Whether this entity won't clip with collision or not (make note it won't disable gravity)
     */
    public boolean noClip;

    /**
     * Reduces the velocity applied by entity collisions by the specified percent.
     */
    public float entityCollisionReduction;
    protected Random rand;

    /** How many ticks has this entity had ran since being alive */
    public int ticksExisted;

    /**
     * The amount of ticks you have to stand inside of fire before be set on fire
     */
    public int fireResistance;
    public int fire; // CraftBukkit - private -> public

    /**
     * Whether this entity is currently inside of water (if it handles water movement that is)
     */
    public boolean inWater; // Spigot - protected -> public

    /**
     * Remaining time an entity will be "immune" to further damage after being hurt.
     */
    public int hurtResistantTime;
    private boolean firstUpdate;
    protected boolean isImmuneToFire;
    protected DataWatcher dataWatcher;
    private double entityRiderPitchDelta;
    private double entityRiderYawDelta;

    /** Has this entity been added to the chunk its within */
    public boolean addedToChunk;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;

    /**
     * Render entity even if it is outside the camera frustum. Only true in EntityFish for now. Used in RenderGlobal:
     * render if ignoreFrustumCheck or in frustum.
     */
    public boolean ignoreFrustumCheck;
    public boolean isAirBorne;
    public int timeUntilPortal;

    /** Whether the entity is inside a Portal */
    protected boolean inPortal;
    protected int timeInPortal;

    /** Which dimension the player is in (-1 = the Nether, 0 = normal world) */
    public int dimension;
    protected int teleportDirection;
    private boolean invulnerable;
    public UUID entityUniqueID; // CraftBukkit - private -> public
    public EnumEntitySize myEntitySize;
    public boolean valid = false; // CraftBukkit
    
    // Spigot start
    public CustomTimingsHandler tickTimer = org.bukkit.craftbukkit.SpigotTimings.getEntityTimings(this); // Spigot
        
    public final byte activationType = org.bukkit.craftbukkit.Spigot.initializeEntityActivationType(this);
    public final boolean defaultActivationState;
    public long activatedTick = 0;
    // Spigot end
            
    /** Forge: Used to store custom data for each entity. */
    private NBTTagCompound customEntityData;
    public boolean captureDrops = false;
    public ArrayList<EntityItem> capturedDrops = new ArrayList<EntityItem>();

    private HashMap<String, IExtendedEntityProperties> extendedProperties;

    public Entity(World par1World)
    {
        this.entityId = nextEntityID++;
        this.renderDistanceWeight = 1.0D;
        this.preventEntitySpawning = false;
        this.boundingBox = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        this.onGround = false;
        this.isCollided = false;
        this.velocityChanged = false;
        this.field_70135_K = true;
        this.isDead = false;
        this.yOffset = 0.0F;
        this.width = 0.6F;
        this.height = 1.8F;
        this.prevDistanceWalkedModified = 0.0F;
        this.distanceWalkedModified = 0.0F;
        this.distanceWalkedOnStepModified = 0.0F;
        this.fallDistance = 0.0F;
        this.nextStepDistance = 1;
        this.ySize = 0.0F;
        this.stepHeight = 0.0F;
        this.noClip = false;
        this.entityCollisionReduction = 0.0F;
        this.rand = new Random();
        this.ticksExisted = 0;
        this.fireResistance = 1;
        this.fire = 0;
        this.inWater = false;
        this.hurtResistantTime = 0;
        this.firstUpdate = true;
        this.isImmuneToFire = false;
        this.dataWatcher = new DataWatcher();
        this.addedToChunk = false;
        this.teleportDirection = 0;
        this.invulnerable = false;
        this.entityUniqueID = new UUID(rand.nextLong(), rand.nextLong()); // Spigot
        this.myEntitySize = EnumEntitySize.SIZE_2;
        this.worldObj = par1World;
        this.setPosition(0.0D, 0.0D, 0.0D);

        if (par1World != null && par1World.getWorld() != null) // MCPC+ - add second null check for Worlds without CraftWorld
        {
            this.dimension = par1World.provider.dimensionId;
            // Spigot start
            this.defaultActivationState = org.bukkit.craftbukkit.Spigot.initializeEntityActivationState(this, par1World.getWorld());
        }
        else
        {
            this.defaultActivationState = false;
        }

        // Spigot end
        this.dataWatcher.addObject(0, Byte.valueOf((byte)0));
        this.dataWatcher.addObject(1, Short.valueOf((short)300));
        this.entityInit();

        extendedProperties = new HashMap<String, IExtendedEntityProperties>();

        MinecraftForge.EVENT_BUS.post(new EntityEvent.EntityConstructing(this));

        for (IExtendedEntityProperties props : this.extendedProperties.values())
        {
            props.init(this, par1World);
        }
    }

    protected abstract void entityInit();

    public DataWatcher getDataWatcher()
    {
        return this.dataWatcher;
    }

    public boolean equals(Object par1Obj)
    {
        return par1Obj instanceof Entity ? ((Entity)par1Obj).entityId == this.entityId : false;
    }

    public int hashCode()
    {
        return this.entityId;
    }

    /**
     * Will get destroyed next tick.
     */
    public void setDead()
    {
        //System.out.println("Removing Entity: " + par1Entity.getClass().getName());
        String className = this.getClass().getName();
        if(className.equals("vswe.stevescarts.Carts.MinecartModular"))
        {
        	System.out.println("Removing Minecart with stack(SETDEAD):");
        	Thread.currentThread().dumpStack();
        }
        this.isDead = true;
    }

    /**
     * Sets the width and height of the entity. Args: width, height
     */
    protected void setSize(float par1, float par2)
    {
        if (par1 != this.width || par2 != this.height)
        {
            this.width = par1;
            this.height = par2;
            this.boundingBox.maxX = this.boundingBox.minX + (double)this.width;
            this.boundingBox.maxZ = this.boundingBox.minZ + (double)this.width;
            this.boundingBox.maxY = this.boundingBox.minY + (double)this.height;
        }

        float f2 = par1 % 2.0F;

        if ((double)f2 < 0.375D)
        {
            this.myEntitySize = EnumEntitySize.SIZE_1;
        }
        else if ((double)f2 < 0.75D)
        {
            this.myEntitySize = EnumEntitySize.SIZE_2;
        }
        else if ((double)f2 < 1.0D)
        {
            this.myEntitySize = EnumEntitySize.SIZE_3;
        }
        else if ((double)f2 < 1.375D)
        {
            this.myEntitySize = EnumEntitySize.SIZE_4;
        }
        else if ((double)f2 < 1.75D)
        {
            this.myEntitySize = EnumEntitySize.SIZE_5;
        }
        else
        {
            this.myEntitySize = EnumEntitySize.SIZE_6;
        }
    }

    /**
     * Sets the rotation of the entity
     */
    protected void setRotation(float par1, float par2)
    {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(par1))
        {
            par1 = 0;
        }

        if ((par1 == Float.POSITIVE_INFINITY) || (par1 == Float.NEGATIVE_INFINITY))
        {
            if (this instanceof EntityPlayerMP)
            {
                this.worldObj.getServer().getLogger().warning(((CraftPlayer) this.getBukkitEntity()).getName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Nope");
            }

            par1 = 0;
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0.
        if (Float.isNaN(par2))
        {
            par2 = 0;
        }

        if ((par2 == Float.POSITIVE_INFINITY) || (par2 == Float.NEGATIVE_INFINITY))
        {
            if (this instanceof EntityPlayerMP)
            {
                this.worldObj.getServer().getLogger().warning(((CraftPlayer) this.getBukkitEntity()).getName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Nope");
            }

            par2 = 0;
        }

        // CraftBukkit end
        this.rotationYaw = par1 % 360.0F;
        this.rotationPitch = par2 % 360.0F;
    }

    /**
     * Sets the x,y,z of the entity from the given parameters. Also seems to set up a bounding box.
     */
    public void setPosition(double par1, double par3, double par5)
    {
        this.posX = par1;
        this.posY = par3;
        this.posZ = par5;
        float f = this.width / 2.0F;
        float f1 = this.height;
        this.boundingBox.setBounds(par1 - (double)f, par3 - (double)this.yOffset + (double)this.ySize, par5 - (double)f, par1 + (double)f, par3 - (double)this.yOffset + (double)this.ySize + (double)f1, par5 + (double)f);
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        this.onEntityUpdate();
    }

    /**
     * Gets called every tick from main Entity class
     */
    public void onEntityUpdate()
    {
        this.worldObj.theProfiler.startSection("entityBaseTick");

        if (this.ridingEntity != null && this.ridingEntity.isDead)
        {
            this.ridingEntity = null;
        }

        this.prevDistanceWalkedModified = this.distanceWalkedModified;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        int i;

        if (!this.worldObj.isRemote && this.worldObj instanceof WorldServer)
        {
            this.worldObj.theProfiler.startSection("portal");
            MinecraftServer minecraftserver = ((WorldServer)this.worldObj).getMinecraftServer();
            i = this.getMaxInPortalTime();

            if (this.inPortal)
            {
                if (true || minecraftserver.getAllowNether())   // CraftBukkit
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

        if (this.isSprinting() && !this.isInWater())
        {
            int j = MathHelper.floor_double(this.posX);
            i = MathHelper.floor_double(this.posY - 0.20000000298023224D - (double)this.yOffset);
            int k = MathHelper.floor_double(this.posZ);
            int l = this.worldObj.getBlockId(j, i, k);

            if (l > 0)
            {
                this.worldObj.spawnParticle("tilecrack_" + l + "_" + this.worldObj.getBlockMetadata(j, i, k), this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D, this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, -this.motionX * 4.0D, 1.5D, -this.motionZ * 4.0D);
            }
        }

        this.handleWaterMovement();

        if (this.worldObj.isRemote)
        {
            this.fire = 0;
        }
        else if (this.fire > 0)
        {
            if (this.isImmuneToFire)
            {
                this.fire -= 4;

                if (this.fire < 0)
                {
                    this.fire = 0;
                }
            }
            else
            {
                if (this.fire % 20 == 0)
                {
                    this.attackEntityFrom(DamageSource.onFire, 1);
                }

                --this.fire;
            }
        }

        if (this.handleLavaMovement())
        {
            this.setOnFireFromLava();
            this.fallDistance *= 0.5F;
        }

        if (this.posY < -64.0D)
        {
            this.kill();
        }

        if (!this.worldObj.isRemote)
        {
            this.setFlag(0, this.fire > 0);
            this.setFlag(2, this.ridingEntity != null && ridingEntity.shouldRiderSit());
        }

        this.firstUpdate = false;
        this.worldObj.theProfiler.endSection();
    }

    /**
     * Return the amount of time this entity should stay in a portal before being transported.
     */
    public int getMaxInPortalTime()
    {
        return 0;
    }

    /**
     * Called whenever the entity is walking inside of lava.
     */
    protected void setOnFireFromLava()
    {
        if (!this.isImmuneToFire)
        {
            // CraftBukkit start - Fallen in lava TODO: this event spams!
            if (this instanceof EntityLiving)
            {
                Server server = this.worldObj.getServer();
                // TODO: shouldn't be sending null for the block.
                org.bukkit.block.Block damager = null; // ((WorldServer) this.l).getWorld().getBlockAt(i, j, k);
                org.bukkit.entity.Entity damagee = this.getBukkitEntity();
                EntityDamageByBlockEvent event = new EntityDamageByBlockEvent(damager, damagee, EntityDamageEvent.DamageCause.LAVA, 4);
                server.getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    damagee.setLastDamageCause(event);
                    this.attackEntityFrom(DamageSource.lava, event.getDamage());
                }

                if (this.fire <= 0)
                {
                    // not on fire yet
                    EntityCombustEvent combustEvent = new org.bukkit.event.entity.EntityCombustByBlockEvent(damager, damagee, 15);
                    server.getPluginManager().callEvent(combustEvent);

                    if (!combustEvent.isCancelled())
                    {
                        this.setFire(combustEvent.getDuration());
                    }
                }
                else
                {
                    // This will be called every single tick the entity is in lava, so don't throw an event
                    this.setFire(15);
                }

                return;
            }

            // CraftBukkit end - we also don't throw an event unless the object in lava is living, to save on some event calls
            this.attackEntityFrom(DamageSource.lava, 4);
            this.setFire(15);
        }
    }

    /**
     * Sets entity to burn for x amount of seconds, cannot lower amount of existing fire.
     */
    public void setFire(int par1)
    {
        int j = par1 * 20;
        j = EnchantmentProtection.func_92093_a(this, j);

        if (this.fire < j)
        {
            this.fire = j;
        }
    }

    /**
     * Removes fire from entity.
     */
    public void extinguish()
    {
        this.fire = 0;
    }

    /**
     * sets the dead flag. Used when you fall off the bottom of the world.
     */
    protected void kill()
    {
        this.setDead();
    }

    /**
     * Checks if the offset position from the entity's current position is inside of liquid. Args: x, y, z
     */
    public boolean isOffsetPositionInLiquid(double par1, double par3, double par5)
    {
        AxisAlignedBB axisalignedbb = this.boundingBox.getOffsetBoundingBox(par1, par3, par5);
        List list = this.worldObj.getCollidingBoundingBoxes(this, axisalignedbb);
        return !list.isEmpty() ? false : !this.worldObj.isAnyLiquid(axisalignedbb);
    }

    /**
     * Tries to moves the entity by the passed in displacement. Args: x, y, z
     */
    public void moveEntity(double par1, double par3, double par5)
    {
        // CraftBukkit start - Don't do anything if we aren't moving
        if (par1 == 0 && par3 == 0 && par5 == 0 && this.ridingEntity == null && this.riddenByEntity == null)
        {
            return;
        }

        // CraftBukkit end

        org.bukkit.craftbukkit.SpigotTimings.entityMoveTimer.startTiming(); // Spigot

        if (this.noClip)
        {
            this.boundingBox.offset(par1, par3, par5);
            this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
            this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
            this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;
        }
        else
        {
            this.worldObj.theProfiler.startSection("move");
            this.ySize *= 0.4F;
            double d3 = this.posX;
            double d4 = this.posY;
            double d5 = this.posZ;

            if (this.isInWeb)
            {
                this.isInWeb = false;
                par1 *= 0.25D;
                par3 *= 0.05000000074505806D;
                par5 *= 0.25D;
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }

            double d6 = par1;
            double d7 = par3;
            double d8 = par5;
            AxisAlignedBB axisalignedbb = this.boundingBox.copy();
            boolean flag = this.onGround && this.isSneaking() && this instanceof EntityPlayer;

            if (flag)
            {
                double d9;

                for (d9 = 0.05D; par1 != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(par1, -1.0D, 0.0D)).isEmpty(); d6 = par1)
                {
                    if (par1 < d9 && par1 >= -d9)
                    {
                        par1 = 0.0D;
                    }
                    else if (par1 > 0.0D)
                    {
                        par1 -= d9;
                    }
                    else
                    {
                        par1 += d9;
                    }
                }

                for (; par5 != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(0.0D, -1.0D, par5)).isEmpty(); d8 = par5)
                {
                    if (par5 < d9 && par5 >= -d9)
                    {
                        par5 = 0.0D;
                    }
                    else if (par5 > 0.0D)
                    {
                        par5 -= d9;
                    }
                    else
                    {
                        par5 += d9;
                    }
                }

                while (par1 != 0.0D && par5 != 0.0D && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(par1, -1.0D, par5)).isEmpty())
                {
                    if (par1 < d9 && par1 >= -d9)
                    {
                        par1 = 0.0D;
                    }
                    else if (par1 > 0.0D)
                    {
                        par1 -= d9;
                    }
                    else
                    {
                        par1 += d9;
                    }

                    if (par5 < d9 && par5 >= -d9)
                    {
                        par5 = 0.0D;
                    }
                    else if (par5 > 0.0D)
                    {
                        par5 -= d9;
                    }
                    else
                    {
                        par5 += d9;
                    }

                    d6 = par1;
                    d8 = par5;
                }
            }

            List list = this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.addCoord(par1, par3, par5));

            for (int i = 0; i < list.size(); ++i)
            {
                par3 = ((AxisAlignedBB)list.get(i)).calculateYOffset(this.boundingBox, par3);
            }

            this.boundingBox.offset(0.0D, par3, 0.0D);

            if (!this.field_70135_K && d7 != par3)
            {
                par5 = 0.0D;
                par3 = 0.0D;
                par1 = 0.0D;
            }

            boolean flag1 = this.onGround || d7 != par3 && d7 < 0.0D;
            int j;

            for (j = 0; j < list.size(); ++j)
            {
                par1 = ((AxisAlignedBB)list.get(j)).calculateXOffset(this.boundingBox, par1);
            }

            this.boundingBox.offset(par1, 0.0D, 0.0D);

            if (!this.field_70135_K && d6 != par1)
            {
                par5 = 0.0D;
                par3 = 0.0D;
                par1 = 0.0D;
            }

            for (j = 0; j < list.size(); ++j)
            {
                par5 = ((AxisAlignedBB)list.get(j)).calculateZOffset(this.boundingBox, par5);
            }

            this.boundingBox.offset(0.0D, 0.0D, par5);

            if (!this.field_70135_K && d8 != par5)
            {
                par5 = 0.0D;
                par3 = 0.0D;
                par1 = 0.0D;
            }

            double d10;
            double d11;
            int k;
            double d12;

            if (this.stepHeight > 0.0F && flag1 && (flag || this.ySize < 0.05F) && (d6 != par1 || d8 != par5))
            {
                d12 = par1;
                d10 = par3;
                d11 = par5;
                par1 = d6;
                par3 = (double)this.stepHeight;
                par5 = d8;
                AxisAlignedBB axisalignedbb1 = this.boundingBox.copy();
                this.boundingBox.setBB(axisalignedbb);
                list = this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.addCoord(d6, par3, d8));

                for (k = 0; k < list.size(); ++k)
                {
                    par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(this.boundingBox, par3);
                }

                this.boundingBox.offset(0.0D, par3, 0.0D);

                if (!this.field_70135_K && d7 != par3)
                {
                    par5 = 0.0D;
                    par3 = 0.0D;
                    par1 = 0.0D;
                }

                for (k = 0; k < list.size(); ++k)
                {
                    par1 = ((AxisAlignedBB)list.get(k)).calculateXOffset(this.boundingBox, par1);
                }

                this.boundingBox.offset(par1, 0.0D, 0.0D);

                if (!this.field_70135_K && d6 != par1)
                {
                    par5 = 0.0D;
                    par3 = 0.0D;
                    par1 = 0.0D;
                }

                for (k = 0; k < list.size(); ++k)
                {
                    par5 = ((AxisAlignedBB)list.get(k)).calculateZOffset(this.boundingBox, par5);
                }

                this.boundingBox.offset(0.0D, 0.0D, par5);

                if (!this.field_70135_K && d8 != par5)
                {
                    par5 = 0.0D;
                    par3 = 0.0D;
                    par1 = 0.0D;
                }

                if (!this.field_70135_K && d7 != par3)
                {
                    par5 = 0.0D;
                    par3 = 0.0D;
                    par1 = 0.0D;
                }
                else
                {
                    par3 = (double)(-this.stepHeight);

                    for (k = 0; k < list.size(); ++k)
                    {
                        par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(this.boundingBox, par3);
                    }

                    this.boundingBox.offset(0.0D, par3, 0.0D);
                }

                if (d12 * d12 + d11 * d11 >= par1 * par1 + par5 * par5)
                {
                    par1 = d12;
                    par3 = d10;
                    par5 = d11;
                    this.boundingBox.setBB(axisalignedbb1);
                }
            }

            this.worldObj.theProfiler.endSection();
            this.worldObj.theProfiler.startSection("rest");
            this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
            this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
            this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;
            this.isCollidedHorizontally = d6 != par1 || d8 != par5;
            this.isCollidedVertically = d7 != par3;
            this.onGround = d7 != par3 && d7 < 0.0D;
            this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
            this.updateFallState(par3, this.onGround);

            if (d6 != par1)
            {
                this.motionX = 0.0D;
            }

            if (d7 != par3)
            {
                this.motionY = 0.0D;
            }

            if (d8 != par5)
            {
                this.motionZ = 0.0D;
            }

            d12 = this.posX - d3;
            d10 = this.posY - d4;
            d11 = this.posZ - d5;

            // CraftBukkit start
            if ((this.isCollidedHorizontally) && (this.getBukkitEntity() instanceof Vehicle) && this.worldObj.getWorld() != null) // MCPC+ - fixes MFR NPE with grinder/slaughterhouse
            {
                Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                org.bukkit.block.Block block = this.worldObj.getWorld().getBlockAt(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY - (double) this.yOffset), MathHelper.floor_double(this.posZ));

                if (d6 > par1)
                {
                    block = block.getRelative(BlockFace.EAST);
                }
                else if (d6 < par1)
                {
                    block = block.getRelative(BlockFace.WEST);
                }
                else if (d8 > par5)
                {
                    block = block.getRelative(BlockFace.SOUTH);
                }
                else if (d8 < par5)
                {
                    block = block.getRelative(BlockFace.NORTH);
                }

                VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, block);
                this.worldObj.getServer().getPluginManager().callEvent(event);
            }

            // CraftBukkit end

            if (this.canTriggerWalking() && !flag && this.ridingEntity == null)
            {
                int l = MathHelper.floor_double(this.posX);
                k = MathHelper.floor_double(this.posY - 0.20000000298023224D - (double)this.yOffset);
                int i1 = MathHelper.floor_double(this.posZ);
                int j1 = this.worldObj.getBlockId(l, k, i1);

                if (j1 == 0)
                {
                    int k1 = this.worldObj.blockGetRenderType(l, k - 1, i1);

                    if (k1 == 11 || k1 == 32 || k1 == 21)
                    {
                        j1 = this.worldObj.getBlockId(l, k - 1, i1);
                    }
                }

                if (j1 != Block.ladder.blockID)
                {
                    d10 = 0.0D;
                }

                this.distanceWalkedModified = (float)((double)this.distanceWalkedModified + (double)MathHelper.sqrt_double(d12 * d12 + d11 * d11) * 0.6D);
                this.distanceWalkedOnStepModified = (float)((double)this.distanceWalkedOnStepModified + (double)MathHelper.sqrt_double(d12 * d12 + d10 * d10 + d11 * d11) * 0.6D);

                if (this.distanceWalkedOnStepModified > (float)this.nextStepDistance && j1 > 0)
                {
                    this.nextStepDistance = (int)this.distanceWalkedOnStepModified + 1;

                    if (this.isInWater())
                    {
                        float f = MathHelper.sqrt_double(this.motionX * this.motionX * 0.20000000298023224D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.20000000298023224D) * 0.35F;

                        if (f > 1.0F)
                        {
                            f = 1.0F;
                        }

                        this.playSound("liquid.swim", f, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                    }

                    this.playStepSound(l, k, i1, j1);
                    Block.blocksList[j1].onEntityWalking(this.worldObj, l, k, i1, this);
                }
            }

            this.doBlockCollisions();
            boolean flag2 = this.isWet();

            if (this.worldObj.isBoundingBoxBurning(this.boundingBox.contract(0.001D, 0.001D, 0.001D)))
            {
                this.dealFireDamage(1);

                if (!flag2)
                {
                    ++this.fire;

                    // CraftBukkit start - Not on fire yet
                    if (this.fire <= 0)   // Only throw events on the first combust, otherwise it spams
                    {
                        EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), 8);
                        this.worldObj.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            this.setFire(event.getDuration());
                        }
                    }
                    else
                    {
                        // CraftBukkit end
                        this.setFire(8);
                    }
                }
            }
            else if (this.fire <= 0)
            {
                this.fire = -this.fireResistance;
            }

            if (flag2 && this.fire > 0)
            {
                this.playSound("random.fizz", 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                this.fire = -this.fireResistance;
            }

            this.worldObj.theProfiler.endSection();
        }

        org.bukkit.craftbukkit.SpigotTimings.entityMoveTimer.stopTiming(); // Spigot
    }

    /**
     * Checks for block collisions, and calls the associated onBlockCollided method for the collided block.
     */
    protected void doBlockCollisions()
    {
        int i = MathHelper.floor_double(this.boundingBox.minX + 0.001D);
        int j = MathHelper.floor_double(this.boundingBox.minY + 0.001D);
        int k = MathHelper.floor_double(this.boundingBox.minZ + 0.001D);
        int l = MathHelper.floor_double(this.boundingBox.maxX - 0.001D);
        int i1 = MathHelper.floor_double(this.boundingBox.maxY - 0.001D);
        int j1 = MathHelper.floor_double(this.boundingBox.maxZ - 0.001D);

        if (this.worldObj.checkChunksExist(i, j, k, l, i1, j1))
        {
            for (int k1 = i; k1 <= l; ++k1)
            {
                for (int l1 = j; l1 <= i1; ++l1)
                {
                    for (int i2 = k; i2 <= j1; ++i2)
                    {
                        int j2 = this.worldObj.getBlockId(k1, l1, i2);

                        if (j2 > 0)
                        {
                            Block.blocksList[j2].onEntityCollidedWithBlock(this.worldObj, k1, l1, i2, this);
                        }
                    }
                }
            }
        }
    }

    /**
     * Plays step sound at given x, y, z for the entity
     */
    protected void playStepSound(int par1, int par2, int par3, int par4)
    {
        StepSound stepsound = Block.blocksList[par4].stepSound;

        if (this.worldObj.getBlockId(par1, par2 + 1, par3) == Block.snow.blockID)
        {
            stepsound = Block.snow.stepSound;
            this.playSound(stepsound.getStepSound(), stepsound.getVolume() * 0.15F, stepsound.getPitch());
        }
        else if (!Block.blocksList[par4].blockMaterial.isLiquid())
        {
            this.playSound(stepsound.getStepSound(), stepsound.getVolume() * 0.15F, stepsound.getPitch());
        }
    }

    public void playSound(String par1Str, float par2, float par3)
    {
        this.worldObj.playSoundAtEntity(this, par1Str, par2, par3);
    }

    /**
     * returns if this entity triggers Block.onEntityWalking on the blocks they walk on. used for spiders and wolves to
     * prevent them from trampling crops
     */
    protected boolean canTriggerWalking()
    {
        return true;
    }

    /**
     * Takes in the distance the entity has fallen this tick and whether its on the ground to update the fall distance
     * and deal fall damage if landing on the ground.  Args: distanceFallenThisTick, onGround
     */
    protected void updateFallState(double par1, boolean par3)
    {
        if (par3)
        {
            if (this.fallDistance > 0.0F)
            {
                this.fall(this.fallDistance);
                this.fallDistance = 0.0F;
            }
        }
        else if (par1 < 0.0D)
        {
            this.fallDistance = (float)((double)this.fallDistance - par1);
        }
    }

    /**
     * returns the bounding box for this entity
     */
    public AxisAlignedBB getBoundingBox()
    {
        return null;
    }

    /**
     * Will deal the specified amount of damage to the entity if the entity isn't immune to fire damage. Args:
     * amountDamage
     */
    protected void dealFireDamage(int par1)
    {
        if (!this.isImmuneToFire)
        {
            this.attackEntityFrom(DamageSource.inFire, par1);
        }
    }

    public final boolean isImmuneToFire()
    {
        return this.isImmuneToFire;
    }

    /**
     * Called when the mob is falling. Calculates and applies fall damage.
     */
    protected void fall(float par1)
    {
        if (this.riddenByEntity != null)
        {
            this.riddenByEntity.fall(par1);
        }
    }

    /**
     * Checks if this entity is either in water or on an open air block in rain (used in wolves).
     */
    public boolean isWet()
    {
        return this.inWater || this.worldObj.canLightningStrikeAt(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) || this.worldObj.canLightningStrikeAt(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY + (double)this.height), MathHelper.floor_double(this.posZ));
    }

    /**
     * Checks if this entity is inside water (if inWater field is true as a result of handleWaterMovement() returning
     * true)
     */
    public boolean isInWater()
    {
        return this.inWater;
    }

    /**
     * Returns if this entity is in water and will end up adding the waters velocity to the entity
     */
    public boolean handleWaterMovement()
    {
        if (this.worldObj.handleMaterialAcceleration(this.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D).contract(0.001D, 0.001D, 0.001D), Material.water, this))
        {
            if (!this.inWater && !this.firstUpdate)
            {
                float f = MathHelper.sqrt_double(this.motionX * this.motionX * 0.20000000298023224D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.20000000298023224D) * 0.2F;

                if (f > 1.0F)
                {
                    f = 1.0F;
                }

                this.playSound("liquid.splash", f, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                float f1 = (float)MathHelper.floor_double(this.boundingBox.minY);
                int i;
                float f2;
                float f3;

                for (i = 0; (float)i < 1.0F + this.width * 20.0F; ++i)
                {
                    f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    f3 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.worldObj.spawnParticle("bubble", this.posX + (double)f2, (double)(f1 + 1.0F), this.posZ + (double)f3, this.motionX, this.motionY - (double)(this.rand.nextFloat() * 0.2F), this.motionZ);
                }

                for (i = 0; (float)i < 1.0F + this.width * 20.0F; ++i)
                {
                    f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    f3 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.worldObj.spawnParticle("splash", this.posX + (double)f2, (double)(f1 + 1.0F), this.posZ + (double)f3, this.motionX, this.motionY, this.motionZ);
                }
            }

            this.fallDistance = 0.0F;
            this.inWater = true;
            this.fire = 0;
        }
        else
        {
            this.inWater = false;
        }

        return this.inWater;
    }

    /**
     * Checks if the current block the entity is within of the specified material type
     */
    public boolean isInsideOfMaterial(Material par1Material)
    {
        double d0 = this.posY + (double)this.getEyeHeight();
        int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_float((float)MathHelper.floor_double(d0));
        int k = MathHelper.floor_double(this.posZ);
        int l = this.worldObj.getBlockId(i, j, k);

        if (l != 0 && Block.blocksList[l].blockMaterial == par1Material)
        {
            float f = BlockFluid.getFluidHeightPercent(this.worldObj.getBlockMetadata(i, j, k)) - 0.11111111F;
            float f1 = (float)(j + 1) - f;
            return d0 < (double)f1;
        }
        else
        {
            return false;
        }
    }

    public float getEyeHeight()
    {
        return 0.0F;
    }

    /**
     * Whether or not the current entity is in lava
     */
    public boolean handleLavaMovement()
    {
        return this.worldObj.isMaterialInBB(this.boundingBox.expand(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D), Material.lava);
    }

    /**
     * Used in both water and by flying objects
     */
    public void moveFlying(float par1, float par2, float par3)
    {
        float f3 = par1 * par1 + par2 * par2;

        if (f3 >= 1.0E-4F)
        {
            f3 = MathHelper.sqrt_float(f3);

            if (f3 < 1.0F)
            {
                f3 = 1.0F;
            }

            f3 = par3 / f3;
            par1 *= f3;
            par2 *= f3;
            float f4 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
            float f5 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
            this.motionX += (double)(par1 * f5 - par2 * f4);
            this.motionZ += (double)(par2 * f5 + par1 * f4);
        }
    }

    /**
     * Gets how bright this entity is.
     */
    public float getBrightness(float par1)
    {
        int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_double(this.posZ);

        if (this.worldObj.blockExists(i, 0, j))
        {
            double d0 = (this.boundingBox.maxY - this.boundingBox.minY) * 0.66D;
            int k = MathHelper.floor_double(this.posY - (double)this.yOffset + d0);
            return this.worldObj.getLightBrightness(i, k, j);
        }
        else
        {
            return 0.0F;
        }
    }

    /**
     * Sets the reference to the World object.
     */
    public void setWorld(World par1World)
    {
        // CraftBukkit start
        if (par1World == null)
        {
            this.setDead();
            this.worldObj = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
            return;
        }

        // CraftBukkit end
        this.worldObj = par1World;
    }

    /**
     * Sets the entity's position and rotation. Args: posX, posY, posZ, yaw, pitch
     */
    public void setPositionAndRotation(double par1, double par3, double par5, float par7, float par8)
    {
        this.prevPosX = this.posX = par1;
        this.prevPosY = this.posY = par3;
        this.prevPosZ = this.posZ = par5;
        this.prevRotationYaw = this.rotationYaw = par7;
        this.prevRotationPitch = this.rotationPitch = par8;
        this.ySize = 0.0F;
        double d3 = (double)(this.prevRotationYaw - par7);

        if (d3 < -180.0D)
        {
            this.prevRotationYaw += 360.0F;
        }

        if (d3 >= 180.0D)
        {
            this.prevRotationYaw -= 360.0F;
        }

        this.setPosition(this.posX, this.posY, this.posZ);
        this.setRotation(par7, par8);
    }

    /**
     * Sets the location and Yaw/Pitch of an entity in the world
     */
    public void setLocationAndAngles(double par1, double par3, double par5, float par7, float par8)
    {
        this.lastTickPosX = this.prevPosX = this.posX = par1;
        this.lastTickPosY = this.prevPosY = this.posY = par3 + (double)this.yOffset;
        this.lastTickPosZ = this.prevPosZ = this.posZ = par5;
        this.rotationYaw = par7;
        this.rotationPitch = par8;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    /**
     * Returns the distance to the entity. Args: entity
     */
    public float getDistanceToEntity(Entity par1Entity)
    {
        float f = (float)(this.posX - par1Entity.posX);
        float f1 = (float)(this.posY - par1Entity.posY);
        float f2 = (float)(this.posZ - par1Entity.posZ);
        return MathHelper.sqrt_float(f * f + f1 * f1 + f2 * f2);
    }

    /**
     * Gets the squared distance to the position. Args: x, y, z
     */
    public double getDistanceSq(double par1, double par3, double par5)
    {
        double d3 = this.posX - par1;
        double d4 = this.posY - par3;
        double d5 = this.posZ - par5;
        return d3 * d3 + d4 * d4 + d5 * d5;
    }

    /**
     * Gets the distance to the position. Args: x, y, z
     */
    public double getDistance(double par1, double par3, double par5)
    {
        double d3 = this.posX - par1;
        double d4 = this.posY - par3;
        double d5 = this.posZ - par5;
        return (double)MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
    }

    /**
     * Returns the squared distance to the entity. Args: entity
     */
    public double getDistanceSqToEntity(Entity par1Entity)
    {
        double d0 = this.posX - par1Entity.posX;
        double d1 = this.posY - par1Entity.posY;
        double d2 = this.posZ - par1Entity.posZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    /**
     * Called by a player entity when they collide with an entity
     */
    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {}

    /**
     * Applies a velocity to each of the entities pushing them away from each other. Args: entity
     */
    public void applyEntityCollision(Entity par1Entity)
    {
        if (par1Entity.riddenByEntity != this && par1Entity.ridingEntity != this)
        {
            double d0 = par1Entity.posX - this.posX;
            double d1 = par1Entity.posZ - this.posZ;
            double d2 = MathHelper.abs_max(d0, d1);

            if (d2 >= 0.009999999776482582D)
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
                d0 *= 0.05000000074505806D;
                d1 *= 0.05000000074505806D;
                d0 *= (double)(1.0F - this.entityCollisionReduction);
                d1 *= (double)(1.0F - this.entityCollisionReduction);
                this.addVelocity(-d0, 0.0D, -d1);
                par1Entity.addVelocity(d0, 0.0D, d1);
            }
        }
    }

    /**
     * Adds to the current velocity of the entity. Args: x, y, z
     */
    public void addVelocity(double par1, double par3, double par5)
    {
        this.motionX += par1;
        this.motionY += par3;
        this.motionZ += par5;
        this.isAirBorne = true;
    }

    /**
     * Sets that this entity has been attacked.
     */
    protected void setBeenAttacked()
    {
        this.velocityChanged = true;
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
            this.setBeenAttacked();
            return false;
        }
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith()
    {
        return false;
    }

    /**
     * Returns true if this entity should push and be pushed by other entities when colliding.
     */
    public boolean canBePushed()
    {
        return false;
    }

    /**
     * Adds a value to the player score. Currently not actually used and the entity passed in does nothing. Args:
     * entity, scoreToAdd
     */
    public void addToPlayerScore(Entity par1Entity, int par2) {}

    public boolean addNotRiddenEntityID(NBTTagCompound par1NBTTagCompound)
    {
        String s = this.getEntityString();

        if (!this.isDead && s != null)
        {
            par1NBTTagCompound.setString("id", s);
            this.writeToNBT(par1NBTTagCompound);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * adds the ID of this entity to the NBT given
     */
    public boolean addEntityID(NBTTagCompound par1NBTTagCompound)
    {
        String s = this.getEntityString();

        if (!this.isDead && s != null && this.riddenByEntity == null)
        {
            par1NBTTagCompound.setString("id", s);
            this.writeToNBT(par1NBTTagCompound);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Save the entity to NBT (calls an abstract helper method to write extra data)
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        try
        {
            par1NBTTagCompound.setTag("Pos", this.newDoubleNBTList(new double[] {this.posX, this.posY + (double)this.ySize, this.posZ}));
            par1NBTTagCompound.setTag("Motion", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));

            // CraftBukkit start - Checking for NaN pitch/yaw and resetting to zero
            // TODO: make sure this is the best way to address this.
            if (Float.isNaN(this.rotationYaw))
            {
                this.rotationYaw = 0;
            }

            if (Float.isNaN(this.rotationPitch))
            {
                this.rotationPitch = 0;
            }

            // CraftBukkit end
            par1NBTTagCompound.setTag("Rotation", this.newFloatNBTList(new float[] {this.rotationYaw, this.rotationPitch}));
            par1NBTTagCompound.setFloat("FallDistance", this.fallDistance);
            par1NBTTagCompound.setShort("Fire", (short)this.fire);
            par1NBTTagCompound.setShort("Air", (short)this.getAir());
            par1NBTTagCompound.setBoolean("OnGround", this.onGround);
            par1NBTTagCompound.setInteger("Dimension", this.dimension);
            par1NBTTagCompound.setBoolean("Invulnerable", this.invulnerable);
            par1NBTTagCompound.setInteger("PortalCooldown", this.timeUntilPortal);
            par1NBTTagCompound.setLong("UUIDMost", this.entityUniqueID.getMostSignificantBits());
            par1NBTTagCompound.setLong("UUIDLeast", this.entityUniqueID.getLeastSignificantBits());
            // CraftBukkit start
            par1NBTTagCompound.setLong("WorldUUIDLeast", this.worldObj.getSaveHandler().getUUID().getLeastSignificantBits());
            par1NBTTagCompound.setLong("WorldUUIDMost", this.worldObj.getSaveHandler().getUUID().getMostSignificantBits());
            par1NBTTagCompound.setInteger("Bukkit.updateLevel", CURRENT_LEVEL);
            // CraftBukkit end
            if (customEntityData != null)
            {
                par1NBTTagCompound.setCompoundTag("ForgeData", customEntityData);
            }

            for (String identifier : this.extendedProperties.keySet()){
                try{
                    IExtendedEntityProperties props = this.extendedProperties.get(identifier);
                    props.saveNBTData(par1NBTTagCompound);
                }catch (Throwable t){
                    FMLLog.severe("Failed to save extended properties for %s.  This is a mod issue.", identifier);
                    t.printStackTrace();
                }
            }

            this.writeEntityToNBT(par1NBTTagCompound);

            if (this.ridingEntity != null)
            {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound("Riding");

                if (this.ridingEntity.addNotRiddenEntityID(nbttagcompound1))
                {
                    par1NBTTagCompound.setTag("Riding", nbttagcompound1);
                }
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Saving entity NBT");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being saved");
            this.func_85029_a(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * Reads the entity from NBT (calls an abstract helper method to read specialized data)
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        try
        {
            NBTTagList nbttaglist = par1NBTTagCompound.getTagList("Pos");
            NBTTagList nbttaglist1 = par1NBTTagCompound.getTagList("Motion");
            NBTTagList nbttaglist2 = par1NBTTagCompound.getTagList("Rotation");
            this.motionX = ((NBTTagDouble)nbttaglist1.tagAt(0)).data;
            this.motionY = ((NBTTagDouble)nbttaglist1.tagAt(1)).data;
            this.motionZ = ((NBTTagDouble)nbttaglist1.tagAt(2)).data;
            /* CraftBukkit start - Moved section down
            if (Math.abs(this.motX) > 10.0D) {
                this.motX = 0.0D;
            }

            if (Math.abs(this.motY) > 10.0D) {
                this.motY = 0.0D;
            }

            if (Math.abs(this.motZ) > 10.0D) {
                this.motZ = 0.0D;
            }
            // CraftBukkit end */
            this.prevPosX = this.lastTickPosX = this.posX = ((NBTTagDouble)nbttaglist.tagAt(0)).data;
            this.prevPosY = this.lastTickPosY = this.posY = ((NBTTagDouble)nbttaglist.tagAt(1)).data;
            this.prevPosZ = this.lastTickPosZ = this.posZ = ((NBTTagDouble)nbttaglist.tagAt(2)).data;
            this.prevRotationYaw = this.rotationYaw = ((NBTTagFloat)nbttaglist2.tagAt(0)).data;
            this.prevRotationPitch = this.rotationPitch = ((NBTTagFloat)nbttaglist2.tagAt(1)).data;
            this.fallDistance = par1NBTTagCompound.getFloat("FallDistance");
            this.fire = par1NBTTagCompound.getShort("Fire");
            this.setAir(par1NBTTagCompound.getShort("Air"));
            this.onGround = par1NBTTagCompound.getBoolean("OnGround");
            this.dimension = par1NBTTagCompound.getInteger("Dimension");
            this.invulnerable = par1NBTTagCompound.getBoolean("Invulnerable");
            this.timeUntilPortal = par1NBTTagCompound.getInteger("PortalCooldown");

            if (par1NBTTagCompound.hasKey("UUIDMost") && par1NBTTagCompound.hasKey("UUIDLeast"))
            {
                this.entityUniqueID = new UUID(par1NBTTagCompound.getLong("UUIDMost"), par1NBTTagCompound.getLong("UUIDLeast"));
            }

            this.setPosition(this.posX, this.posY, this.posZ);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (par1NBTTagCompound.hasKey("ForgeData"))
            {
                customEntityData = par1NBTTagCompound.getCompoundTag("ForgeData");
            }

            for (String identifier : this.extendedProperties.keySet()){
                try{
                    IExtendedEntityProperties props = this.extendedProperties.get(identifier);
                    props.loadNBTData(par1NBTTagCompound);
                }catch (Throwable t){
                    FMLLog.severe("Failed to load extended properties for %s.  This is a mod issue.", identifier);
                    t.printStackTrace();
                }
            }

            //Rawr, legacy code, Vanilla added a UUID, keep this so older maps will convert properly
            if (par1NBTTagCompound.hasKey("PersistentIDMSB") && par1NBTTagCompound.hasKey("PersistentIDLSB"))
            {
                this.entityUniqueID = new UUID(par1NBTTagCompound.getLong("PersistentIDMSB"), par1NBTTagCompound.getLong("PersistentIDLSB"));
            }
            this.readEntityFromNBT(par1NBTTagCompound);            

            // CraftBukkit start
            if (this instanceof EntityLiving)
            {
                EntityLiving entity = (EntityLiving) this;

                // If the entity does not have a max health set yet, update it (it may have changed after loading the entity)
                if (!par1NBTTagCompound.hasKey("Bukkit.MaxHealth"))
                {
                    entity.maxHealth = entity.getMaxHealth();
                }

                // Reset the persistence for tamed animals
                if (entity instanceof EntityTameable && !isLevelAtLeast(par1NBTTagCompound, 2) && !par1NBTTagCompound.getBoolean("PersistenceRequired"))
                {
                    entity.persistenceRequired = !entity.canDespawn();
                }
            }

            // CraftBukkit end

            // CraftBukkit start - Exempt Vehicles from notch's sanity check
            if (!(this.getBukkitEntity() instanceof Vehicle))
            {
                if (Math.abs(this.motionX) > 10.0D)
                {
                    this.motionX = 0.0D;
                }

                if (Math.abs(this.motionY) > 10.0D)
                {
                    this.motionY = 0.0D;
                }

                if (Math.abs(this.motionZ) > 10.0D)
                {
                    this.motionZ = 0.0D;
                }
            }

            // CraftBukkit end

            // CraftBukkit start - Reset world
            if (this instanceof EntityPlayerMP)
            {
                Server server = Bukkit.getServer();
                org.bukkit.World bworld = null;
                // TODO: Remove World related checks, replaced with WorldUID.
                String worldName = par1NBTTagCompound.getString("World");

                if (par1NBTTagCompound.hasKey("WorldUUIDMost") && par1NBTTagCompound.hasKey("WorldUUIDLeast"))
                {
                    UUID uid = new UUID(par1NBTTagCompound.getLong("WorldUUIDMost"), par1NBTTagCompound.getLong("WorldUUIDLeast"));
                    bworld = server.getWorld(uid);
                }
                else
                {
                    bworld = server.getWorld(worldName);
                }

                if (bworld == null)
                {
                    EntityPlayerMP entityPlayer = (EntityPlayerMP) this;
                    bworld = ((org.bukkit.craftbukkit.CraftServer) server).getServer().worldServerForDimension(entityPlayer.dimension).getWorld();
                }

                this.setWorld(bworld == null ? null : ((CraftWorld) bworld).getHandle());
            }

            // CraftBukkit end
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Loading entity NBT");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being loaded");
            this.func_85029_a(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * Returns the string that identifies this Entity's class
     */
    protected final String getEntityString()
    {
        return EntityList.getEntityString(this);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected abstract void readEntityFromNBT(NBTTagCompound nbttagcompound);

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected abstract void writeEntityToNBT(NBTTagCompound nbttagcompound);

    /**
     * creates a NBT list from the array of doubles passed to this function
     */
    protected NBTTagList newDoubleNBTList(double ... par1ArrayOfDouble)
    {
        NBTTagList nbttaglist = new NBTTagList();
        double[] adouble = par1ArrayOfDouble;
        int i = par1ArrayOfDouble.length;

        for (int j = 0; j < i; ++j)
        {
            double d1 = adouble[j];
            nbttaglist.appendTag(new NBTTagDouble((String)null, d1));
        }

        return nbttaglist;
    }

    /**
     * Returns a new NBTTagList filled with the specified floats
     */
    protected NBTTagList newFloatNBTList(float ... par1ArrayOfFloat)
    {
        NBTTagList nbttaglist = new NBTTagList();
        float[] afloat = par1ArrayOfFloat;
        int i = par1ArrayOfFloat.length;

        for (int j = 0; j < i; ++j)
        {
            float f1 = afloat[j];
            nbttaglist.appendTag(new NBTTagFloat((String)null, f1));
        }

        return nbttaglist;
    }

    /**
     * Drops an item stack at the entity's position. Args: itemID, count
     */
    public EntityItem dropItem(int par1, int par2)
    {
        return this.dropItemWithOffset(par1, par2, 0.0F);
    }

    /**
     * Drops an item stack with a specified y offset. Args: itemID, count, yOffset
     */
    public EntityItem dropItemWithOffset(int par1, int par2, float par3)
    {
        return this.entityDropItem(new ItemStack(par1, par2, 0), par3);
    }

    /**
     * Drops an item at the position of the entity.
     */
    public EntityItem entityDropItem(ItemStack par1ItemStack, float par2)
    {
        EntityItem entityitem = new EntityItem(this.worldObj, this.posX, this.posY + (double)par2, this.posZ, par1ItemStack);
        entityitem.delayBeforeCanPickup = 10;
        if (captureDrops)
        {
            capturedDrops.add(entityitem);
        }
        else
        {
            this.worldObj.spawnEntityInWorld(entityitem);
        }
        return entityitem;
    }

    /**
     * Checks whether target entity is alive.
     */
    public boolean isEntityAlive()
    {
        return !this.isDead;
    }

    /**
     * Checks if this entity is inside of an opaque block
     */
    public boolean isEntityInsideOpaqueBlock()
    {
        for (int i = 0; i < 8; ++i)
        {
            float f = ((float)((i >> 0) % 2) - 0.5F) * this.width * 0.8F;
            float f1 = ((float)((i >> 1) % 2) - 0.5F) * 0.1F;
            float f2 = ((float)((i >> 2) % 2) - 0.5F) * this.width * 0.8F;
            int j = MathHelper.floor_double(this.posX + (double)f);
            int k = MathHelper.floor_double(this.posY + (double)this.getEyeHeight() + (double)f1);
            int l = MathHelper.floor_double(this.posZ + (double)f2);

            if (this.worldObj.isBlockNormalCube(j, k, l))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer par1EntityPlayer)
    {
        return false;
    }

    /**
     * Returns a boundingBox used to collide the entity with other entities and blocks. This enables the entity to be
     * pushable on contact, like boats or minecarts.
     */
    public AxisAlignedBB getCollisionBox(Entity par1Entity)
    {
        return null;
    }

    /**
     * Handles updating while being ridden by an entity
     */
    public void updateRidden()
    {
        if (this.ridingEntity.isDead)
        {
            this.ridingEntity = null;
        }
        else
        {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            this.onUpdate();

            if (this.ridingEntity != null)
            {
                this.ridingEntity.updateRiderPosition();
                this.entityRiderYawDelta += (double)(this.ridingEntity.rotationYaw - this.ridingEntity.prevRotationYaw);

                for (this.entityRiderPitchDelta += (double)(this.ridingEntity.rotationPitch - this.ridingEntity.prevRotationPitch); this.entityRiderYawDelta >= 180.0D; this.entityRiderYawDelta -= 360.0D)
                {
                    ;
                }

                while (this.entityRiderYawDelta < -180.0D)
                {
                    this.entityRiderYawDelta += 360.0D;
                }

                while (this.entityRiderPitchDelta >= 180.0D)
                {
                    this.entityRiderPitchDelta -= 360.0D;
                }

                while (this.entityRiderPitchDelta < -180.0D)
                {
                    this.entityRiderPitchDelta += 360.0D;
                }

                double d0 = this.entityRiderYawDelta * 0.5D;
                double d1 = this.entityRiderPitchDelta * 0.5D;
                float f = 10.0F;

                if (d0 > (double)f)
                {
                    d0 = (double)f;
                }

                if (d0 < (double)(-f))
                {
                    d0 = (double)(-f);
                }

                if (d1 > (double)f)
                {
                    d1 = (double)f;
                }

                if (d1 < (double)(-f))
                {
                    d1 = (double)(-f);
                }

                this.entityRiderYawDelta -= d0;
                this.entityRiderPitchDelta -= d1;
                this.rotationYaw = (float)((double)this.rotationYaw + d0);
                this.rotationPitch = (float)((double)this.rotationPitch + d1);
            }
        }
    }

    public void updateRiderPosition()
    {
        if (this.riddenByEntity != null)
        {
            if (!(this.riddenByEntity instanceof EntityPlayer) || !((EntityPlayer)this.riddenByEntity).func_71066_bF())
            {
                this.riddenByEntity.lastTickPosX = this.lastTickPosX;
                this.riddenByEntity.lastTickPosY = this.lastTickPosY + this.getMountedYOffset() + this.riddenByEntity.getYOffset();
                this.riddenByEntity.lastTickPosZ = this.lastTickPosZ;
            }

            this.riddenByEntity.setPosition(this.posX, this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ);
        }
    }

    /**
     * Returns the Y Offset of this entity.
     */
    public double getYOffset()
    {
        return (double)this.yOffset;
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    public double getMountedYOffset()
    {
        return (double)this.height * 0.75D;
    }

    /**
     * Called when a player mounts an entity. e.g. mounts a pig, mounts a boat.
     */
    public void mountEntity(Entity par1Entity)
    {
        // CraftBukkit start
        this.setPassengerOf(par1Entity);
    }

    public CraftEntity bukkitEntity;

    public CraftEntity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = CraftEntity.getEntity(this.worldObj.getServer(), this);
        }

        return this.bukkitEntity;
    }

    public void setPassengerOf(Entity entity)
    {
        // b(null) doesn't really fly for overloaded methods,
        // so this method is needed
        PluginManager pluginManager = Bukkit.getPluginManager();
        this.getBukkitEntity(); // make sure bukkitEntity is initialised
        // CraftBukkit end
        this.entityRiderPitchDelta = 0.0D;
        this.entityRiderYawDelta = 0.0D;

        if (entity == null)
        {
            if (this.ridingEntity != null)
            {
                // CraftBukkit start
                if ((this.bukkitEntity instanceof LivingEntity) && (this.ridingEntity.getBukkitEntity() instanceof Vehicle))
                {
                    VehicleExitEvent event = new VehicleExitEvent((Vehicle) this.ridingEntity.getBukkitEntity(), (LivingEntity) this.bukkitEntity);
                    pluginManager.callEvent(event);
                }

                // CraftBukkit end
                this.setLocationAndAngles(this.ridingEntity.posX, this.ridingEntity.boundingBox.minY + (double)this.ridingEntity.height, this.ridingEntity.posZ, this.rotationYaw, this.rotationPitch);
                this.ridingEntity.riddenByEntity = null;
            }

            this.ridingEntity = null;
        }
        else
        {
            // CraftBukkit start
            if ((this.bukkitEntity instanceof LivingEntity) && (entity.getBukkitEntity() instanceof Vehicle) && entity.worldObj.chunkExists((int) entity.posX >> 4, (int) entity.posZ >> 4))
            {
                VehicleEnterEvent event = new VehicleEnterEvent((Vehicle) entity.getBukkitEntity(), this.bukkitEntity);
                pluginManager.callEvent(event);

                if (event.isCancelled())
                {
                    return;
                }
            }

            // CraftBukkit end

            if (this.ridingEntity != null)
            {
                this.ridingEntity.riddenByEntity = null;
            }

            this.ridingEntity = entity;
            entity.riddenByEntity = this;
        }
    }

    /**
     * Called when a player unounts an entity.
     */
    public void unmountEntity(Entity par1Entity)
    {
        double d0 = this.posX;
        double d1 = this.posY;
        double d2 = this.posZ;

        if (par1Entity != null)
        {
            d0 = par1Entity.posX;
            d1 = par1Entity.boundingBox.minY + (double)par1Entity.height;
            d2 = par1Entity.posZ;
        }

        for (double d3 = -1.5D; d3 < 2.0D; ++d3)
        {
            for (double d4 = -1.5D; d4 < 2.0D; ++d4)
            {
                if (d3 != 0.0D || d4 != 0.0D)
                {
                    int i = (int)(this.posX + d3);
                    int j = (int)(this.posZ + d4);
                    AxisAlignedBB axisalignedbb = this.boundingBox.getOffsetBoundingBox(d3, 1.0D, d4);

                    if (this.worldObj.getCollidingBlockBounds(axisalignedbb).isEmpty())
                    {
                        if (this.worldObj.doesBlockHaveSolidTopSurface(i, (int)this.posY, j))
                        {
                            this.setLocationAndAngles(this.posX + d3, this.posY + 1.0D, this.posZ + d4, this.rotationYaw, this.rotationPitch);
                            return;
                        }

                        if (this.worldObj.doesBlockHaveSolidTopSurface(i, (int)this.posY - 1, j) || this.worldObj.getBlockMaterial(i, (int)this.posY - 1, j) == Material.water)
                        {
                            d0 = this.posX + d3;
                            d1 = this.posY + 1.0D;
                            d2 = this.posZ + d4;
                        }
                    }
                }
            }
        }

        this.setLocationAndAngles(d0, d1, d2, this.rotationYaw, this.rotationPitch);
    }

    public float getCollisionBorderSize()
    {
        return 0.1F;
    }

    /**
     * returns a (normalized) vector of where this entity is looking
     */
    public Vec3 getLookVec()
    {
        return null;
    }

    /**
     * Called by portal blocks when an entity is within it.
     */
    public void setInPortal()
    {
        if (this.timeUntilPortal > 0)
        {
            this.timeUntilPortal = this.getPortalCooldown();
        }
        else
        {
            double d0 = this.prevPosX - this.posX;
            double d1 = this.prevPosZ - this.posZ;

            if (!this.worldObj.isRemote && !this.inPortal)
            {
                this.teleportDirection = Direction.getMovementDirection(d0, d1);
            }

            this.inPortal = true;
        }
    }

    /**
     * Return the amount of cooldown before this entity can use a portal again.
     */
    public int getPortalCooldown()
    {
        return 900;
    }

    public ItemStack[] getLastActiveItems()
    {
        return null;
    }

    /**
     * Sets the held item, or an armor slot. Slot 0 is held item. Slot 1-4 is armor. Params: Item, slot
     */
    public void setCurrentItemOrArmor(int par1, ItemStack par2ItemStack) {}

    /**
     * Returns true if the entity is on fire. Used by render to add the fire effect on rendering.
     */
    public boolean isBurning()
    {
        return this.fire > 0 || this.getFlag(0);
    }

    /**
     * Returns true if the entity is riding another entity, used by render to rotate the legs to be in 'sit' position
     * for players.
     */
    public boolean isRiding()
    {
        return (this.ridingEntity != null && ridingEntity.shouldRiderSit()) || this.getFlag(2);
    }

    /**
     * Returns if this entity is sneaking.
     */
    public boolean isSneaking()
    {
        return this.getFlag(1);
    }

    /**
     * Sets the sneaking flag.
     */
    public void setSneaking(boolean par1)
    {
        this.setFlag(1, par1);
    }

    /**
     * Get if the Entity is sprinting.
     */
    public boolean isSprinting()
    {
        return this.getFlag(3);
    }

    /**
     * Set sprinting switch for Entity.
     */
    public void setSprinting(boolean par1)
    {
        this.setFlag(3, par1);
    }

    public boolean isInvisible()
    {
        return this.getFlag(5);
    }

    public void setInvisible(boolean par1)
    {
        this.setFlag(5, par1);
    }

    public void setEating(boolean par1)
    {
        this.setFlag(4, par1);
    }

    /**
     * Returns true if the flag is active for the entity. Known flags: 0) is burning; 1) is sneaking; 2) is riding
     * something; 3) is sprinting; 4) is eating
     */
    protected boolean getFlag(int par1)
    {
        return (this.dataWatcher.getWatchableObjectByte(0) & 1 << par1) != 0;
    }

    /**
     * Enable or disable a entity flag, see getEntityFlag to read the know flags.
     */
    protected void setFlag(int par1, boolean par2)
    {
        byte b0 = this.dataWatcher.getWatchableObjectByte(0);

        if (par2)
        {
            this.dataWatcher.updateObject(0, Byte.valueOf((byte)(b0 | 1 << par1)));
        }
        else
        {
            this.dataWatcher.updateObject(0, Byte.valueOf((byte)(b0 & ~(1 << par1))));
        }
    }

    public int getAir()
    {
        return this.dataWatcher.getWatchableObjectShort(1);
    }

    public void setAir(int par1)
    {
        this.dataWatcher.updateObject(1, Short.valueOf((short)par1));
    }

    /**
     * Called when a lightning bolt hits the entity.
     */
    public void onStruckByLightning(EntityLightningBolt par1EntityLightningBolt)
    {
        // CraftBukkit start
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        if (thisBukkitEntity == null) return; // MCPC+ - skip mod entities with no wrapper (TODO: create a wrapper)
        if (par1EntityLightningBolt == null) return; // MCPC+ - skip null entities, see #392
        final org.bukkit.entity.Entity stormBukkitEntity = par1EntityLightningBolt.getBukkitEntity();
        if (stormBukkitEntity == null) return; // MCPC+ - skip mod entities with no wrapper (TODO: create a wrapper)
        final PluginManager pluginManager = Bukkit.getPluginManager();

        if (thisBukkitEntity instanceof Painting)
        {
            PaintingBreakByEntityEvent event = new PaintingBreakByEntityEvent((Painting) thisBukkitEntity, stormBukkitEntity);
            pluginManager.callEvent(event);

            if (event.isCancelled())
            {
                return;
            }
        }

        EntityDamageEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityDamageEvent(par1EntityLightningBolt, this, EntityDamageEvent.DamageCause.LIGHTNING, 5);

        if (event.isCancelled())
        {
            return;
        }

        this.dealFireDamage(event.getDamage());
        // CraftBukkit end
        ++this.fire;

        if (this.fire == 0)
        {
            // CraftBukkit start - Call a combust event when lightning strikes
            EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
            pluginManager.callEvent(entityCombustEvent);

            if (!entityCombustEvent.isCancelled())
            {
                this.setFire(entityCombustEvent.getDuration());
            }

            // CraftBukkit end
        }
    }

    /**
     * This method gets called when the entity kills another one.
     */
    public void onKillEntity(EntityLiving par1EntityLiving) {}

    /**
     * Adds velocity to push the entity out of blocks at the specified x, y, z position Args: x, y, z
     */
    protected boolean pushOutOfBlocks(double par1, double par3, double par5)
    {
        int i = MathHelper.floor_double(par1);
        int j = MathHelper.floor_double(par3);
        int k = MathHelper.floor_double(par5);
        double d3 = par1 - (double)i;
        double d4 = par3 - (double)j;
        double d5 = par5 - (double)k;
        List list = this.worldObj.getCollidingBlockBounds(this.boundingBox);

        if (list.isEmpty() && !this.worldObj.func_85174_u(i, j, k))
        {
            return false;
        }
        else
        {
            boolean flag = !this.worldObj.func_85174_u(i - 1, j, k);
            boolean flag1 = !this.worldObj.func_85174_u(i + 1, j, k);
            boolean flag2 = !this.worldObj.func_85174_u(i, j - 1, k);
            boolean flag3 = !this.worldObj.func_85174_u(i, j + 1, k);
            boolean flag4 = !this.worldObj.func_85174_u(i, j, k - 1);
            boolean flag5 = !this.worldObj.func_85174_u(i, j, k + 1);
            byte b0 = 3;
            double d6 = 9999.0D;

            if (flag && d3 < d6)
            {
                d6 = d3;
                b0 = 0;
            }

            if (flag1 && 1.0D - d3 < d6)
            {
                d6 = 1.0D - d3;
                b0 = 1;
            }

            if (flag3 && 1.0D - d4 < d6)
            {
                d6 = 1.0D - d4;
                b0 = 3;
            }

            if (flag4 && d5 < d6)
            {
                d6 = d5;
                b0 = 4;
            }

            if (flag5 && 1.0D - d5 < d6)
            {
                d6 = 1.0D - d5;
                b0 = 5;
            }

            float f = this.rand.nextFloat() * 0.2F + 0.1F;

            if (b0 == 0)
            {
                this.motionX = (double)(-f);
            }

            if (b0 == 1)
            {
                this.motionX = (double)f;
            }

            if (b0 == 2)
            {
                this.motionY = (double)(-f);
            }

            if (b0 == 3)
            {
                this.motionY = (double)f;
            }

            if (b0 == 4)
            {
                this.motionZ = (double)(-f);
            }

            if (b0 == 5)
            {
                this.motionZ = (double)f;
            }

            return true;
        }
    }

    /**
     * Sets the Entity inside a web block.
     */
    public void setInWeb()
    {
        this.isInWeb = true;
        this.fallDistance = 0.0F;
    }

    /**
     * Gets the username of the entity.
     */
    public String getEntityName()
    {
        String s = EntityList.getEntityString(this);

        if (s == null)
        {
            s = "generic";
        }

        return StatCollector.translateToLocal("entity." + s + ".name");
    }

    /**
     * Return the Entity parts making up this Entity (currently only for dragons)
     */
    public Entity[] getParts()
    {
        return null;
    }

    /**
     * Returns true if Entity argument is equal to this Entity
     */
    public boolean isEntityEqual(Entity par1Entity)
    {
        return this == par1Entity;
    }

    public float getRotationYawHead()
    {
        return 0.0F;
    }

    /**
     * If returns false, the item will not inflict any damage against entities.
     */
    public boolean canAttackWithItem()
    {
        return true;
    }

    public boolean func_85031_j(Entity par1Entity)
    {
        return false;
    }

    public String toString()
    {
        return String.format("%s[\'%s\'/%d, l=\'%s\', x=%.2f, y=%.2f, z=%.2f]", new Object[] {this.getClass().getSimpleName(), this.getEntityName(), Integer.valueOf(this.entityId), this.worldObj == null ? "~NULL~" : this.worldObj.getWorldInfo().getWorldName(), Double.valueOf(this.posX), Double.valueOf(this.posY), Double.valueOf(this.posZ)});
    }

    /**
     * Return whether this entity is invulnerable to damage.
     */
    public boolean isEntityInvulnerable()
    {
        return this.invulnerable;
    }

    public void func_82149_j(Entity par1Entity)
    {
        this.setLocationAndAngles(par1Entity.posX, par1Entity.posY, par1Entity.posZ, par1Entity.rotationYaw, par1Entity.rotationPitch);
    }

    /**
     * Copies important data from another entity to this entity. Used when teleporting entities between worlds, as this
     * actually deletes the teleporting entity and re-creates it on the other side. Params: Entity to copy from, unused
     * (always true)
     */
    public void copyDataFrom(Entity par1Entity, boolean par2)
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        par1Entity.writeToNBT(nbttagcompound);
        this.readFromNBT(nbttagcompound);
        this.timeUntilPortal = par1Entity.timeUntilPortal;
        this.teleportDirection = par1Entity.teleportDirection;
    }

    /**
     * Teleports the entity to another dimension. Params: Dimension number to teleport to
     */
    public void travelToDimension(int par1)
    {
        if (!this.worldObj.isRemote && !this.isDead)
        {
            this.worldObj.theProfiler.startSection("changeDimension");
            MinecraftServer minecraftserver = MinecraftServer.getServer();
            // CraftBukkit start - Move logic into new function "teleportToLocation"
            // int j = this.dimension;
            // MCPC+ start - Allow Forge hotloading on teleport
            WorldServer exitWorld = minecraftserver.worldServerForDimension(par1);

            Location enter = this.getBukkitEntity().getLocation();
            Location exit = exitWorld != null ? minecraftserver.getConfigurationManager().calculateTarget(enter, minecraftserver.worldServerForDimension(par1)) : null;
            boolean useTravelAgent = exitWorld != null && !(this.dimension == 1 && exitWorld.dimension == 1); // don't use agent for custom worlds or return from THE_END
            // MCPC+ start - check if teleporter is instance of TravelAgent before attempting to cast to it
            Teleporter teleporter = exit != null ? ((CraftWorld) exit.getWorld()).getHandle().getDefaultTeleporter() : null;
            TravelAgent agent = (teleporter != null && teleporter instanceof TravelAgent) ? (TravelAgent)teleporter : org.bukkit.craftbukkit.CraftTravelAgent.DEFAULT;  // return arbitrary TA to compensate for implementation dependent plugins
            // MCPC+ end
            EntityPortalEvent event = new EntityPortalEvent(this.getBukkitEntity(), enter, exit, agent);
            event.useTravelAgent(useTravelAgent);
            event.getEntity().getServer().getPluginManager().callEvent(event);

            if (event.isCancelled() || event.getTo() == null || !this.isEntityAlive())
            {
                return;
            }

            exit = event.useTravelAgent() ? event.getPortalTravelAgent().findOrCreate(event.getTo()) : event.getTo();
            this.teleportTo(exit, true);
        }
    }

    public void teleportTo(Location exit, boolean portal)
    {
        if (true)
        {
            WorldServer worldserver = ((CraftWorld) this.getBukkitEntity().getLocation().getWorld()).getHandle();
            WorldServer worldserver1 = ((CraftWorld) exit.getWorld()).getHandle();
            int i = worldserver1.dimension;
            // CraftBukkit end
            this.dimension = i;
            this.worldObj.removeEntity(this);
            this.isDead = false;
            this.worldObj.theProfiler.startSection("reposition");
            // CraftBukkit start - Ensure chunks are loaded in case TravelAgent is not used which would initially cause chunks to load during find/create
            // minecraftserver.getPlayerList().a(this, j, worldserver, worldserver1);
            // MCPC+ start - if we are force allowing all chunk requests, avoid access to loadChunkOnProvideRequest
            if (worldserver1.getServer().getLoadChunkOnRequest())
            {
                worldserver1.getMinecraftServer().getConfigurationManager().repositionEntity(this, exit, portal);
            }
            else 
            {
                boolean before = worldserver1.theChunkProviderServer.loadChunkOnProvideRequest;
                worldserver1.theChunkProviderServer.loadChunkOnProvideRequest = true;
                worldserver1.getMinecraftServer().getConfigurationManager().repositionEntity(this, exit, portal);
                worldserver1.theChunkProviderServer.loadChunkOnProvideRequest = before;
            }
            // MCPC+ end
            // CraftBukkit end
            this.worldObj.theProfiler.endStartSection("reloading");
            Entity entity = EntityList.createEntityByName(EntityList.getEntityString(this), worldserver1);

            if (entity != null)
            {
                entity.copyDataFrom(this, true);
                worldserver1.spawnEntityInWorld(entity);
                // CraftBukkit start - Forward the CraftEntity to the new entity
                this.getBukkitEntity().setHandle(entity);
                entity.bukkitEntity = this.getBukkitEntity();
                // CraftBukkit end
            }

            this.isDead = true;
            this.worldObj.theProfiler.endSection();
            worldserver.resetUpdateEntityTick();
            worldserver1.resetUpdateEntityTick();
            this.worldObj.theProfiler.endSection();
        }
    }

    public float func_82146_a(Explosion par1Explosion, World par2World, int par3, int par4, int par5, Block par6Block)
    {
        return par6Block.getExplosionResistance(this, par2World, par3, par4, par5, posX, posY + (double)getEyeHeight(), posZ);
    }

    public boolean func_96091_a(Explosion par1Explosion, World par2World, int par3, int par4, int par5, int par6, float par7)
    {
        return true;
    }

    public int func_82143_as()
    {
        return 3;
    }

    public int getTeleportDirection()
    {
        return this.teleportDirection;
    }

    /**
     * Return whether this entity should NOT trigger a pressure plate or a tripwire.
     */
    public boolean doesEntityNotTriggerPressurePlate()
    {
        return false;
    }

    public void func_85029_a(CrashReportCategory par1CrashReportCategory)
    {
        par1CrashReportCategory.addCrashSectionCallable("Entity Type", new CallableEntityType(this));
        par1CrashReportCategory.addCrashSection("Entity ID", Integer.valueOf(this.entityId));
        par1CrashReportCategory.addCrashSectionCallable("Entity Name", new CallableEntityName(this));
        par1CrashReportCategory.addCrashSection("Entity\'s Exact location", String.format("%.2f, %.2f, %.2f", new Object[] {Double.valueOf(this.posX), Double.valueOf(this.posY), Double.valueOf(this.posZ)}));
        par1CrashReportCategory.addCrashSection("Entity\'s Block location", CrashReportCategory.getLocationInfo(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)));
        par1CrashReportCategory.addCrashSection("Entity\'s Momentum", String.format("%.2f, %.2f, %.2f", new Object[] {Double.valueOf(this.motionX), Double.valueOf(this.motionY), Double.valueOf(this.motionZ)}));
    }

    public boolean func_96092_aw()
    {
        return true;
    }

    /**
     * Returns the translated name of the entity.
     */
    public String getTranslatedEntityName()
    {
        return this.getEntityName();
    }

    /* ================================== Forge Start =====================================*/
    /**
     * Returns a NBTTagCompound that can be used to store custom data for this entity.
     * It will be written, and read from disc, so it persists over world saves.
     * @return A NBTTagCompound
     */
    public NBTTagCompound getEntityData()
    {
        if (customEntityData == null)
        {
            customEntityData = new NBTTagCompound();
        }
        return customEntityData;
    }

    /**
     * Used in model rendering to determine if the entity riding this entity should be in the 'sitting' position.
     * @return false to prevent an entity that is mounted to this entity from displaying the 'sitting' animation.
     */
    public boolean shouldRiderSit()
    {
        return true;
    }

    /**
     * Called when a user uses the creative pick block button on this entity.
     *
     * @param target The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, Null if nothing should be added.
     */
    public ItemStack getPickedResult(MovingObjectPosition target)
    {
        if (this instanceof EntityPainting)
        {
            return new ItemStack(Item.painting);
        }
        else if (this instanceof EntityMinecart)
        {
            return ((EntityMinecart)this).getCartItem();
        }
        else if (this instanceof EntityBoat)
        {
            return new ItemStack(Item.boat);
        }
        else if (this instanceof EntityItemFrame)
        {
            ItemStack held = ((EntityItemFrame)this).getDisplayedItem();
            if (held == null)
            {
                return new ItemStack(Item.itemFrame);
            }
            else
            {
                return held.copy();
            }
        }
        else
        {
            int id = EntityList.getEntityID(this);
            if (id > 0 && EntityList.entityEggs.containsKey(id))
            {
                return new ItemStack(Item.monsterPlacer, 1, id);
            }
        }
        return null;
    }

    public UUID getPersistentID()
    {
        return entityUniqueID;
    }

    /**
     * Reset the entity ID to a new value. Not to be used from Mod code
     */
    public final void resetEntityId()
    {
        this.entityId = nextEntityID++;
    }

    public boolean shouldRenderInPass(int pass)
    {
        return pass == 0;
    }

    /**
     * Returns true if the entity is of the @link{EnumCreatureType} provided
     * @param type The EnumCreatureType type this entity is evaluating
     * @param forSpawnCount If this is being invoked to check spawn count caps.
     * @return If the creature is of the type provided
     */
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount)
    {
        return type.getCreatureClass().isAssignableFrom(this.getClass());
    }

    /**
     * Register the instance of IExtendedProperties into the entity's collection.
     * @param identifier The identifier which you can use to retrieve these properties for the entity.
     * @param properties The instanceof IExtendedProperties to register
     * @return The identifier that was used to register the extended properties.  Empty String indicates an error.  If your requested key already existed, this will return a modified one that is unique.
     */
    public String registerExtendedProperties(String identifier, IExtendedEntityProperties properties)
    {
        if (identifier == null)
        {
            FMLLog.warning("Someone is attempting to register extended properties using a null identifier.  This is not allowed.  Aborting.  This may have caused instability.");
            return "";
        }
        if (properties == null)
        {
            FMLLog.warning("Someone is attempting to register null extended properties.  This is not allowed.  Aborting.  This may have caused instability.");
            return "";
        }

        String baseIdentifier = identifier;
        int identifierModCount = 1;
        while (this.extendedProperties.containsKey(identifier))
        {
            identifier = String.format("%s%d", baseIdentifier, identifierModCount++);
        }

        if (baseIdentifier != identifier)
        {
            FMLLog.info("An attempt was made to register exended properties using an existing key.  The duplicate identifier (%s) has been remapped to %s.", baseIdentifier, identifier);
        }

        this.extendedProperties.put(identifier, properties);
        return identifier;
    }

    /**
     * Gets the extended properties identified by the passed in key
     * @param identifier The key that identifies the extended properties.
     * @return The instance of IExtendedProperties that was found, or null.
     */
    public IExtendedEntityProperties getExtendedProperties(String identifier)
    {
        return this.extendedProperties.get(identifier);
    }
    
    @Override
	public boolean setItemOwner(PlayerData owner) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PlayerData getItemOwner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getX() {
		return (long)this.posX;
	}

	@Override
	public long getY() {
		return (long)this.posY;
	}

	@Override
	public long getZ() {
		return (long)this.posZ;
	}
}
