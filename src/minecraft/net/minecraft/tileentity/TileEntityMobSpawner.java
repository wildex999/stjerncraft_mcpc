package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;

public class TileEntityMobSpawner extends TileEntity
{
    /** The stored delay before a new spawn. */
    public int delay = -1;

    /**
     * The string ID of the mobs being spawned from this spawner. Defaults to pig, apparently.
     */
    public String mobID = "Pig"; // CraftBukkit - private -> public
    private List field_92060_e = null;

    /** The extra NBT data to add to spawned entities */
    private TileEntityMobSpawnerSpawnData spawnerTags = null;
    public double yaw;
    public double yaw2 = 0.0D;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private Entity spawnedMob;

    /** Maximum number of entities for limiting mob spawning */
    private int maxNearbyEntities = 6;

    /** Required player range for mob spawning to occur */
    private int requiredPlayerRange = 16;

    /** Range for spawning new entities with mob spawners */
    private int spawnRange = 4;

    public TileEntityMobSpawner()
    {
        this.delay = 20;
    }

    public String getMobID()
    {
        return this.spawnerTags == null ? this.mobID : this.spawnerTags.field_92084_c;
    }

    public void setMobID(String par1Str)
    {
        this.mobID = par1Str;
    }

    /**
     * Returns true if there is a player in range (using World.getClosestPlayer)
     */
    public boolean anyPlayerInRange()
    {
        return this.worldObj.getClosestPlayer((double)this.xCoord + 0.5D, (double)this.yCoord + 0.5D, (double)this.zCoord + 0.5D, (double)this.requiredPlayerRange) != null;
    }

    /**
     * Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count
     * ticks and creates a new spawn inside its implementation.
     */
    public void updateEntity()
    {
        if (this.anyPlayerInRange())
        {
            double d0;

            if (this.worldObj.isRemote)
            {
                double d1 = (double)((float)this.xCoord + this.worldObj.rand.nextFloat());
                double d2 = (double)((float)this.yCoord + this.worldObj.rand.nextFloat());
                d0 = (double)((float)this.zCoord + this.worldObj.rand.nextFloat());
                this.worldObj.spawnParticle("smoke", d1, d2, d0, 0.0D, 0.0D, 0.0D);
                this.worldObj.spawnParticle("flame", d1, d2, d0, 0.0D, 0.0D, 0.0D);

                if (this.delay > 0)
                {
                    --this.delay;
                }

                this.yaw2 = this.yaw;
                this.yaw = (this.yaw + (double)(1000.0F / ((float)this.delay + 200.0F))) % 360.0D;
            }
            else
            {
                if (this.delay == -1)
                {
                    this.updateDelay();
                }

                if (this.delay > 0)
                {
                    --this.delay;
                    return;
                }

                boolean flag = false;

                for (int i = 0; i < this.spawnCount; ++i)
                {
                    Entity entity = EntityList.createEntityByName(this.getMobID(), this.worldObj);

                    if (entity == null)
                    {
                        return;
                    }

                    int j = this.worldObj.getEntitiesWithinAABB(entity.getClass(), AxisAlignedBB.getAABBPool().addOrModifyAABBInPool((double)this.xCoord, (double)this.yCoord, (double)this.zCoord, (double)(this.xCoord + 1), (double)(this.yCoord + 1), (double)(this.zCoord + 1)).expand((double)(this.spawnRange * 2), 4.0D, (double)(this.spawnRange * 2))).size();

                    if (j >= this.maxNearbyEntities)
                    {
                        this.updateDelay();
                        return;
                    }

                    if (entity != null)
                    {
                        d0 = (double)this.xCoord + (this.worldObj.rand.nextDouble() - this.worldObj.rand.nextDouble()) * (double)this.spawnRange;
                        double d3 = (double)(this.yCoord + this.worldObj.rand.nextInt(3) - 1);
                        double d4 = (double)this.zCoord + (this.worldObj.rand.nextDouble() - this.worldObj.rand.nextDouble()) * (double)this.spawnRange;
                        EntityLiving entityliving = entity instanceof EntityLiving ? (EntityLiving)entity : null;
                        entity.setLocationAndAngles(d0, d3, d4, this.worldObj.rand.nextFloat() * 360.0F, 0.0F);

                        if (entityliving == null || entityliving.getCanSpawnHere())
                        {
                            this.writeNBTTagsTo(entity);
                            this.worldObj.addEntity(entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER); // CraftBukkit
                            this.worldObj.playAuxSFX(2004, this.xCoord, this.yCoord, this.zCoord, 0);

                            if (entityliving != null)
                            {
                                entityliving.spawnExplosionParticle();
                            }

                            flag = true;
                        }
                    }
                }

                if (flag)
                {
                    this.updateDelay();
                }
            }

            super.updateEntity();
        }
    }

    public void writeNBTTagsTo(Entity par1Entity)
    {
        if (this.spawnerTags != null)
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            par1Entity.addEntityID(nbttagcompound);
            Iterator iterator = this.spawnerTags.field_92083_b.getTags().iterator();

            while (iterator.hasNext())
            {
                NBTBase nbtbase = (NBTBase)iterator.next();
                nbttagcompound.setTag(nbtbase.getName(), nbtbase.copy());
            }

            par1Entity.readFromNBT(nbttagcompound);
        }
        else if (par1Entity instanceof EntityLiving && par1Entity.worldObj != null)
        {
            ((EntityLiving)par1Entity).initCreature();
        }
    }

    /**
     * Sets the delay before a new spawn (base delay of 200 + random number up to 600).
     */
    private void updateDelay()
    {
        if (this.maxSpawnDelay <= this.minSpawnDelay)
        {
            this.delay = this.minSpawnDelay;
        }
        else
        {
            this.delay = this.minSpawnDelay + this.worldObj.rand.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        if (this.field_92060_e != null && this.field_92060_e.size() > 0)
        {
            this.spawnerTags = (TileEntityMobSpawnerSpawnData)WeightedRandom.getRandomItem(this.worldObj.rand, this.field_92060_e);
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }

        this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID, 1, 0);
    }

    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        this.mobID = par1NBTTagCompound.getString("EntityId");
        this.delay = par1NBTTagCompound.getShort("Delay");

        if (par1NBTTagCompound.hasKey("SpawnPotentials"))
        {
            this.field_92060_e = new ArrayList();
            NBTTagList nbttaglist = par1NBTTagCompound.getTagList("SpawnPotentials");

            for (int i = 0; i < nbttaglist.tagCount(); ++i)
            {
                this.field_92060_e.add(new TileEntityMobSpawnerSpawnData(this, (NBTTagCompound)nbttaglist.tagAt(i)));
            }
        }
        else
        {
            this.field_92060_e = null;
        }

        if (par1NBTTagCompound.hasKey("SpawnData"))
        {
            this.spawnerTags = new TileEntityMobSpawnerSpawnData(this, par1NBTTagCompound.getCompoundTag("SpawnData"), this.mobID);
        }
        else
        {
            this.spawnerTags = null;
        }

        if (par1NBTTagCompound.hasKey("MinSpawnDelay"))
        {
            this.minSpawnDelay = par1NBTTagCompound.getShort("MinSpawnDelay");
            this.maxSpawnDelay = par1NBTTagCompound.getShort("MaxSpawnDelay");
            this.spawnCount = par1NBTTagCompound.getShort("SpawnCount");
        }

        if (par1NBTTagCompound.hasKey("MaxNearbyEntities"))
        {
            this.maxNearbyEntities = par1NBTTagCompound.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = par1NBTTagCompound.getShort("RequiredPlayerRange");
        }

        if (par1NBTTagCompound.hasKey("SpawnRange"))
        {
            this.spawnRange = par1NBTTagCompound.getShort("SpawnRange");
        }

        if (this.worldObj != null && this.worldObj.isRemote)
        {
            this.spawnedMob = null;
        }
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setString("EntityId", this.getMobID());
        par1NBTTagCompound.setShort("Delay", (short)this.delay);
        par1NBTTagCompound.setShort("MinSpawnDelay", (short)this.minSpawnDelay);
        par1NBTTagCompound.setShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        par1NBTTagCompound.setShort("SpawnCount", (short)this.spawnCount);
        par1NBTTagCompound.setShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        par1NBTTagCompound.setShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        par1NBTTagCompound.setShort("SpawnRange", (short)this.spawnRange);

        if (this.spawnerTags != null)
        {
            par1NBTTagCompound.setCompoundTag("SpawnData", (NBTTagCompound)this.spawnerTags.field_92083_b.copy());
        }

        if (this.spawnerTags != null || this.field_92060_e != null && this.field_92060_e.size() > 0)
        {
            NBTTagList nbttaglist = new NBTTagList();

            if (this.field_92060_e != null && this.field_92060_e.size() > 0)
            {
                Iterator iterator = this.field_92060_e.iterator();

                while (iterator.hasNext())
                {
                    TileEntityMobSpawnerSpawnData tileentitymobspawnerspawndata = (TileEntityMobSpawnerSpawnData)iterator.next();
                    nbttaglist.appendTag(tileentitymobspawnerspawndata.func_92081_a());
                }
            }
            else
            {
                nbttaglist.appendTag(this.spawnerTags.func_92081_a());
            }

            par1NBTTagCompound.setTag("SpawnPotentials", nbttaglist);
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * will create the entity from the internalID the first time it is accessed
     */
    public Entity getMobEntity()
    {
        if (this.spawnedMob == null)
        {
            Entity entity = EntityList.createEntityByName(this.getMobID(), (World)null);
            this.writeNBTTagsTo(entity);
            this.spawnedMob = entity;
        }

        return this.spawnedMob;
    }

    /**
     * Overriden in a sign to provide the text.
     */
    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        nbttagcompound.removeTag("SpawnPotentials");
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 1, nbttagcompound);
    }

    /**
     * Called when a client event is received with the event number and argument, see World.sendClientEvent
     */
    public void receiveClientEvent(int par1, int par2)
    {
        if (par1 == 1 && this.worldObj.isRemote)
        {
            this.delay = this.minSpawnDelay;
        }
    }
}
