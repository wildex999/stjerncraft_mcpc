package net.minecraft.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.Location;
// CraftBukkit end

public class Explosion
{
    /** whether or not the explosion sets fire to blocks around it */
    public boolean isFlaming = false;

    /** whether or not this explosion spawns smoke particles */
    public boolean isSmoking = true;
    private int field_77289_h = 16;
    private Random explosionRNG = new Random();
    private World worldObj;
    public double explosionX;
    public double explosionY;
    public double explosionZ;
    public Entity exploder;
    public float explosionSize;

    /** A list of ChunkPositions of blocks affected by this explosion */
    public List affectedBlockPositions = new ArrayList();
    private Map field_77288_k = new HashMap();
    public boolean wasCanceled = false; // CraftBukkit

    public Explosion(World par1World, Entity par2Entity, double par3, double par5, double par7, float par9)
    {
        this.worldObj = par1World;
        this.exploder = par2Entity;
        this.explosionSize = (float) Math.max(par9, 0.0); // CraftBukkit - clamp bad values
        this.explosionX = par3;
        this.explosionY = par5;
        this.explosionZ = par7;
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    public void doExplosionA()
    {
        // CraftBukkit start
        if (this.explosionSize < 0.1F)
        {
            return;
        }

        // CraftBukkit end
        float f = this.explosionSize;
        HashSet hashset = new HashSet();
        int i;
        int j;
        int k;
        double d0;
        double d1;
        double d2;

        for (i = 0; i < this.field_77289_h; ++i)
        {
            for (j = 0; j < this.field_77289_h; ++j)
            {
                for (k = 0; k < this.field_77289_h; ++k)
                {
                    if (i == 0 || i == this.field_77289_h - 1 || j == 0 || j == this.field_77289_h - 1 || k == 0 || k == this.field_77289_h - 1)
                    {
                        double d3 = (double)((float)i / ((float)this.field_77289_h - 1.0F) * 2.0F - 1.0F);
                        double d4 = (double)((float)j / ((float)this.field_77289_h - 1.0F) * 2.0F - 1.0F);
                        double d5 = (double)((float)k / ((float)this.field_77289_h - 1.0F) * 2.0F - 1.0F);
                        double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                        d3 /= d6;
                        d4 /= d6;
                        d5 /= d6;
                        float f1 = this.explosionSize * (0.7F + this.worldObj.rand.nextFloat() * 0.6F);
                        d0 = this.explosionX;
                        d1 = this.explosionY;
                        d2 = this.explosionZ;

                        for (float f2 = 0.3F; f1 > 0.0F; f1 -= f2 * 0.75F)
                        {
                            int l = MathHelper.floor_double(d0);
                            int i1 = MathHelper.floor_double(d1);
                            int j1 = MathHelper.floor_double(d2);
                            int k1 = this.worldObj.getBlockId(l, i1, j1);

                            if (k1 > 0)
                            {
                                Block block = Block.blocksList[k1];
                                float f3 = this.exploder != null ? this.exploder.func_82146_a(this, block, l, i1, j1) : block.getExplosionResistance(this.exploder);
                                f1 -= (f3 + 0.3F) * f2;
                            }

                            if (f1 > 0.0F && i1 < 256 && i1 >= 0)   // CraftBukkit - don't wrap explosions
                            {
                                hashset.add(new ChunkPosition(l, i1, j1));
                            }

                            d0 += d3 * (double)f2;
                            d1 += d4 * (double)f2;
                            d2 += d5 * (double)f2;
                        }
                    }
                }
            }
        }

        this.affectedBlockPositions.addAll(hashset);
        this.explosionSize *= 2.0F;
        i = MathHelper.floor_double(this.explosionX - (double)this.explosionSize - 1.0D);
        j = MathHelper.floor_double(this.explosionX + (double)this.explosionSize + 1.0D);
        k = MathHelper.floor_double(this.explosionY - (double)this.explosionSize - 1.0D);
        int l1 = MathHelper.floor_double(this.explosionY + (double)this.explosionSize + 1.0D);
        int i2 = MathHelper.floor_double(this.explosionZ - (double)this.explosionSize - 1.0D);
        int j2 = MathHelper.floor_double(this.explosionZ + (double)this.explosionSize + 1.0D);
        List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this.exploder, AxisAlignedBB.getAABBPool().addOrModifyAABBInPool((double)i, (double)k, (double)i2, (double)j, (double)l1, (double)j2));
        Vec3 vec3 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.explosionX, this.explosionY, this.explosionZ);

        for (int k2 = 0; k2 < list.size(); ++k2)
        {
            Entity entity = (Entity)list.get(k2);
            double d7 = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / (double)this.explosionSize;

            if (d7 <= 1.0D)
            {
                d0 = entity.posX - this.explosionX;
                d1 = entity.posY + (double)entity.getEyeHeight() - this.explosionY;
                d2 = entity.posZ - this.explosionZ;
                double d8 = (double)MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

                if (d8 != 0.0D)
                {
                    d0 /= d8;
                    d1 /= d8;
                    d2 /= d8;
                    double d9 = (double)this.worldObj.getBlockDensity(vec3, entity.boundingBox);
                    double d10 = (1.0D - d7) * d9;
                    // CraftBukkit start - explosion damage hook
                    org.bukkit.entity.Entity damagee = (entity == null) ? null : entity.getBukkitEntity();
                    int damageDone = (int)((d10 * d10 + d10) / 2.0D * 8.0D * (double) this.explosionSize + 1.0D);

                    if (damagee == null)
                    {
                        // nothing was hurt
                    }
                    else if (this.exploder == null)     // Block explosion (without an entity source; bed etc.)
                    {
                        EntityDamageByBlockEvent event = new EntityDamageByBlockEvent(null, damagee, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damageDone);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            damagee.setLastDamageCause(event);
                            entity.attackEntityFrom(DamageSource.explosion, event.getDamage());
                            double d11 = EnchantmentProtection.func_92092_a(entity, d10);
                            entity.motionX += d0 * d11;
                            entity.motionY += d1 * d11;
                            entity.motionZ += d2 * d11;

                            if (entity instanceof EntityPlayer)
                            {
                                this.field_77288_k.put((EntityPlayer) entity, this.worldObj.getWorldVec3Pool().getVecFromPool(d0 * d10, d1 * d10, d2 * d10));
                            }
                        }
                    }
                    else
                    {
                        final org.bukkit.entity.Entity damager = this.exploder.getBukkitEntity();
                        final EntityDamageEvent.DamageCause damageCause;

                        if (damager instanceof org.bukkit.entity.TNTPrimed)
                        {
                            damageCause = EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
                        }
                        else
                        {
                            damageCause = EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
                        }

                        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, damagee, damageCause, damageDone);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            entity.getBukkitEntity().setLastDamageCause(event);
                            entity.attackEntityFrom(DamageSource.explosion, event.getDamage());
                            entity.motionX += d0 * d10;
                            entity.motionY += d1 * d10;
                            entity.motionZ += d2 * d10;

                            if (entity instanceof EntityPlayer)
                            {
                                this.field_77288_k.put((EntityPlayer) entity, this.worldObj.getWorldVec3Pool().getVecFromPool(d0 * d10, d1 * d10, d2 * d10));
                            }
                        }
                    }

                    // CraftBukkit end
                }
            }
        }

        this.explosionSize = f;
    }

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    public void doExplosionB(boolean par1)
    {
        this.worldObj.playSoundEffect(this.explosionX, this.explosionY, this.explosionZ, "random.explode", 4.0F, (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

        if (this.explosionSize >= 2.0F && this.isSmoking)
        {
            this.worldObj.spawnParticle("hugeexplosion", this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D);
        }
        else
        {
            this.worldObj.spawnParticle("largeexplode", this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D);
        }

        Iterator iterator;
        ChunkPosition chunkposition;
        int i;
        int j;
        int k;
        int l;

        if (this.isSmoking)
        {
            // CraftBukkit start
            org.bukkit.World bworld = this.worldObj.getWorld();
            org.bukkit.entity.Entity explode = this.exploder == null ? null : this.exploder.getBukkitEntity();
            Location location = new Location(bworld, this.explosionX, this.explosionY, this.explosionZ);
            List<org.bukkit.block.Block> blockList = new ArrayList<org.bukkit.block.Block>();

            for (int i1 = this.affectedBlockPositions.size() - 1; i1 >= 0; i1--)
            {
                ChunkPosition cpos = (ChunkPosition) this.affectedBlockPositions.get(i1);
                org.bukkit.block.Block block = bworld.getBlockAt(cpos.x, cpos.y, cpos.z);

                if (block.getType() != org.bukkit.Material.AIR)
                {
                    blockList.add(block);
                }
            }

            EntityExplodeEvent event = new EntityExplodeEvent(explode, location, blockList, 0.3F);
            this.worldObj.getServer().getPluginManager().callEvent(event);
            this.affectedBlockPositions.clear();

            for (org.bukkit.block.Block block : event.blockList())
            {
                ChunkPosition coords = new ChunkPosition(block.getX(), block.getY(), block.getZ());
                affectedBlockPositions.add(coords);
            }

            if (event.isCancelled())
            {
                this.wasCanceled = true;
                return;
            }

            // CraftBukkit end
            iterator = this.affectedBlockPositions.iterator();

            while (iterator.hasNext())
            {
                chunkposition = (ChunkPosition)iterator.next();
                i = chunkposition.x;
                j = chunkposition.y;
                k = chunkposition.z;
                l = this.worldObj.getBlockId(i, j, k);
                org.bukkit.craftbukkit.OrebfuscatorManager.updateNearbyBlocks(worldObj, i, j, k); // Spigot (Orebfuscator)

                if (par1)
                {
                    double d0 = (double)((float)i + this.worldObj.rand.nextFloat());
                    double d1 = (double)((float)j + this.worldObj.rand.nextFloat());
                    double d2 = (double)((float)k + this.worldObj.rand.nextFloat());
                    double d3 = d0 - this.explosionX;
                    double d4 = d1 - this.explosionY;
                    double d5 = d2 - this.explosionZ;
                    double d6 = (double)MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
                    d3 /= d6;
                    d4 /= d6;
                    d5 /= d6;
                    double d7 = 0.5D / (d6 / (double)this.explosionSize + 0.1D);
                    d7 *= (double)(this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F);
                    d3 *= d7;
                    d4 *= d7;
                    d5 *= d7;
                    this.worldObj.spawnParticle("explode", (d0 + this.explosionX * 1.0D) / 2.0D, (d1 + this.explosionY * 1.0D) / 2.0D, (d2 + this.explosionZ * 1.0D) / 2.0D, d3, d4, d5);
                    this.worldObj.spawnParticle("smoke", d0, d1, d2, d3, d4, d5);
                }

                // CraftBukkit - stop explosions from putting out fire
                if (l > 0 && l != Block.fire.blockID)
                {
                    Block block = Block.blocksList[l];

                    if (block.canDropFromExplosion(this))
                    {
                        // CraftBukkit
                        block.dropBlockAsItemWithChance(this.worldObj, i, j, k, this.worldObj.getBlockMetadata(i, j, k), event.getYield(), 0);
                    }

                    if (this.worldObj.setBlockAndMetadataWithUpdate(i, j, k, 0, 0, this.worldObj.isRemote))
                    {
                        this.worldObj.notifyBlocksOfNeighborChange(i, j, k, 0);
                    }

                    block.onBlockDestroyedByExplosion(this.worldObj, i, j, k);
                }
            }
        }

        if (this.isFlaming)
        {
            iterator = this.affectedBlockPositions.iterator();

            while (iterator.hasNext())
            {
                chunkposition = (ChunkPosition)iterator.next();
                i = chunkposition.x;
                j = chunkposition.y;
                k = chunkposition.z;
                l = this.worldObj.getBlockId(i, j, k);
                int i1 = this.worldObj.getBlockId(i, j - 1, k);

                if (l == 0 && Block.opaqueCubeLookup[i1] && this.explosionRNG.nextInt(3) == 0)
                {
                    this.worldObj.setBlockWithNotify(i, j, k, Block.fire.blockID);
                }
            }
        }
    }

    public Map func_77277_b()
    {
        return this.field_77288_k;
    }
}
