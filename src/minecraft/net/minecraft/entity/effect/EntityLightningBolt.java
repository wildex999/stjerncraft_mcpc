package net.minecraft.entity.effect;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.bukkit.event.block.BlockIgniteEvent; // CraftBukkit

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
    private org.bukkit.craftbukkit.CraftWorld cworld;
    public boolean isEffect = false;

    public EntityLightningBolt(World par1World, double par2, double par4, double par6)
    {
        this(par1World, par2, par4, par6, false);
    }

    public EntityLightningBolt(World world, double d0, double d1, double d2, boolean isEffect)
    {
        // CraftBukkit end
        super(world);
        // CraftBukkit start
        this.isEffect = isEffect;
        this.cworld = world.getWorld();
        // CraftBukkit end
        this.setLocationAndAngles(d0, d1, d2, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltVertex = this.rand.nextLong();
        this.boltLivingTime = this.rand.nextInt(3) + 1;

        // CraftBukkit
        if (!isEffect && !world.isRemote && world.difficultySetting >= 2 && world.doChunksNearChunkExist(MathHelper.floor_double(d0), MathHelper.floor_double(d1), MathHelper.floor_double(d2), 10))
        {
            int i = MathHelper.floor_double(d0);
            int j = MathHelper.floor_double(d1);
            int k = MathHelper.floor_double(d2);

            if (world.getBlockId(i, j, k) == 0 && Block.fire.canPlaceBlockAt(world, i, j, k))
            {
                // CraftBukkit start
                BlockIgniteEvent event = new BlockIgniteEvent(this.cworld.getBlockAt(i, j, k), BlockIgniteEvent.IgniteCause.LIGHTNING, null);
                world.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    world.setBlockWithNotify(i, j, k, Block.fire.blockID);
                }

                // CraftBukkit end
            }

            for (i = 0; i < 4; ++i)
            {
                j = MathHelper.floor_double(d0) + this.rand.nextInt(3) - 1;
                k = MathHelper.floor_double(d1) + this.rand.nextInt(3) - 1;
                int l = MathHelper.floor_double(d2) + this.rand.nextInt(3) - 1;

                if (world.getBlockId(j, k, l) == 0 && Block.fire.canPlaceBlockAt(world, j, k, l))
                {
                    // CraftBukkit start
                    BlockIgniteEvent event = new BlockIgniteEvent(this.cworld.getBlockAt(j, k, l), BlockIgniteEvent.IgniteCause.LIGHTNING, null);
                    world.getServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled())
                    {
                        world.setBlockWithNotify(j, k, l, Block.fire.blockID);
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
                    int var1 = MathHelper.floor_double(this.posX);
                    int var2 = MathHelper.floor_double(this.posY);
                    int var3 = MathHelper.floor_double(this.posZ);

                    if (this.worldObj.getBlockId(var1, var2, var3) == 0 && Block.fire.canPlaceBlockAt(this.worldObj, var1, var2, var3))
                    {
                        // CraftBukkit start
                        BlockIgniteEvent event = new BlockIgniteEvent(this.cworld.getBlockAt(var1, var2, var3), BlockIgniteEvent.IgniteCause.LIGHTNING, null);
                        this.worldObj.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            this.worldObj.setBlockWithNotify(var1, var2, var3, Block.fire.blockID);
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
                double var6 = 3.0D;
                List var7 = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getAABBPool().addOrModifyAABBInPool(this.posX - var6, this.posY - var6, this.posZ - var6, this.posX + var6, this.posY + 6.0D + var6, this.posZ + var6));

                for (int var4 = 0; var4 < var7.size(); ++var4)
                {
                    Entity var5 = (Entity)var7.get(var4);
                    var5.onStruckByLightning(this);
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

    @SideOnly(Side.CLIENT)

    /**
     * Checks using a Vec3d to determine if this entity is within range of that vector to be rendered. Args: vec3D
     */
    public boolean isInRangeToRenderVec3D(Vec3 par1Vec3)
    {
        return this.lightningState >= 0;
    }
}
