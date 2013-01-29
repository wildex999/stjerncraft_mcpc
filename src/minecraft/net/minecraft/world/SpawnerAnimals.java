package net.minecraft.world;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class SpawnerAnimals
{
    private static LongObjectHashMap<Boolean> eligibleChunksForSpawning = new LongObjectHashMap<Boolean>(); // CraftBukkit - HashMap -> LongObjectHashMap

    /** An array of entity classes that spawn at night. */
    protected static final Class[] nightSpawnEntities = new Class[] {EntitySpider.class, EntityZombie.class, EntitySkeleton.class};
    private static byte spawnRadius = 0; // Spigot

    /**
     * Given a chunk, find a random position in it.
     */
    protected static ChunkPosition getRandomSpawningPointInChunk(World par0World, int par1, int par2)
    {
        Chunk chunk = par0World.getChunkFromChunkCoords(par1, par2);
        int k = par1 * 16 + par0World.rand.nextInt(16);
        int l = par2 * 16 + par0World.rand.nextInt(16);
        int i1 = par0World.rand.nextInt(chunk == null ? par0World.getActualHeight() : chunk.getTopFilledSegment() + 16 - 1);
        return new ChunkPosition(k, i1, l);
    }

    // Spigot start - get entity count only from chunks being processed in b
    public static final int getEntityCount(WorldServer server, Class oClass)
    {
        int i = 0;

        for (Long coord : eligibleChunksForSpawning.keySet())
        {
            int x = LongHash.msw(coord);
            int z = LongHash.lsw(coord);

            if (!server.theChunkProviderServer.chunksToUnload.contains(x, z) && server.chunkExists(x, z))
            {
                for (List<Entity> entitySlice : server.getChunkFromChunkCoords(x, z).entityLists)
                {
                    for (Entity entity : entitySlice)
                    {
                        if (oClass.isAssignableFrom(entity.getClass()))
                        {
                            ++i;
                        }
                    }
                }
            }
        }

        return i;
    }
    // Spigot end

    /**
     * adds all chunks within the spawn radius of the players to eligibleChunksForSpawning. pars: the world,
     * hostileCreatures, passiveCreatures. returns number of eligible chunks.
     */
    public static final int findChunksForSpawning(WorldServer par0WorldServer, boolean par1, boolean par2, boolean par3)
    {
        if (!par1 && !par2)
        {
            return 0;
        }
        else
        {
            eligibleChunksForSpawning.clear();
            int i;
            int j;

            // Spigot start - limit radius to spawn distance (chunks aren't loaded)
            if (spawnRadius == 0)
            {
                spawnRadius = (byte) par0WorldServer.getWorld().mobSpawnRange;

                if (spawnRadius > (byte) par0WorldServer.getServer().getViewDistance())
                {
                    spawnRadius = (byte) par0WorldServer.getServer().getViewDistance();
                }

                if (spawnRadius > 8)
                {
                    spawnRadius = 8;
                }
            }

            // Spigot end

            for (i = 0; i < par0WorldServer.playerEntities.size(); ++i)
            {
                EntityPlayer entityplayer = (EntityPlayer)par0WorldServer.playerEntities.get(i);
                int k = MathHelper.floor_double(entityplayer.posX / 16.0D);
                j = MathHelper.floor_double(entityplayer.posZ / 16.0D);
                byte b0 = spawnRadius; // Spigot - replace 8 with view distance constrained value

                for (int l = -b0; l <= b0; ++l)
                {
                    for (int i1 = -b0; i1 <= b0; ++i1)
                    {
                        boolean flag3 = l == -b0 || l == b0 || i1 == -b0 || i1 == b0;
                        // CraftBukkit start
                        long chunkCoords = LongHash.toLong(l + k, i1 + j);

                        if (!flag3)
                        {
                            eligibleChunksForSpawning.put(chunkCoords, false);
                        }
                        else if (!eligibleChunksForSpawning.containsKey(chunkCoords))
                        {
                            eligibleChunksForSpawning.put(chunkCoords, true);
                        }

                        // CraftBukkit end
                    }
                }
            }

            i = 0;
            ChunkCoordinates chunkcoordinates = par0WorldServer.getSpawnPoint();
            EnumCreatureType[] aenumcreaturetype = EnumCreatureType.values();
            j = aenumcreaturetype.length;

            for (int j1 = 0; j1 < j; ++j1)
            {
                EnumCreatureType enumcreaturetype = aenumcreaturetype[j1];
                // CraftBukkit start - Use per-world spawn limits
                int limit = enumcreaturetype.getMaxNumberOfCreature();

                switch (enumcreaturetype)
                {
                    case monster:
                        limit = par0WorldServer.getWorld().getMonsterSpawnLimit();
                        break;
                    case creature:
                        limit = par0WorldServer.getWorld().getAnimalSpawnLimit();
                        break;
                    case waterCreature:
                        limit = par0WorldServer.getWorld().getWaterAnimalSpawnLimit();
                        break;
                    case ambient:
                        limit = par0WorldServer.getWorld().getAmbientSpawnLimit();
                        break;
                }

                if (limit == 0)
                {
                    continue;
                }

                int mobcnt = 0; // Spigot
                // CraftBukkit end

                if ((!enumcreaturetype.getPeacefulCreature() || par2) && (enumcreaturetype.getPeacefulCreature() || par1) && (!enumcreaturetype.getAnimal() || par3) && (mobcnt = getEntityCount(par0WorldServer, enumcreaturetype.getCreatureClass())) <= limit * eligibleChunksForSpawning.size() / 256)   // CraftBukkit - use per-world limits and use all loaded chunks
                {
                    Iterator iterator = eligibleChunksForSpawning.keySet().iterator();
                    int moblimit = (limit * eligibleChunksForSpawning.size() / 256) - mobcnt + 1; // CraftBukkit - up to 1 more than limit
                    label110:

                    while (iterator.hasNext() && (moblimit > 0))   // Spigot - while more allowed
                    {
                        // CraftBukkit start
                        long key = ((Long) iterator.next()).longValue();

                        if (!eligibleChunksForSpawning.get(key))
                        {
                            ChunkPosition chunkposition = getRandomSpawningPointInChunk(par0WorldServer, LongHash.msw(key), LongHash.lsw(key));
                            // CraftBukkit end
                            int k1 = chunkposition.x;
                            int l1 = chunkposition.y;
                            int i2 = chunkposition.z;

                            if (!par0WorldServer.isBlockNormalCube(k1, l1, i2) && par0WorldServer.getBlockMaterial(k1, l1, i2) == enumcreaturetype.getCreatureMaterial())
                            {
                                int j2 = 0;
                                int k2 = 0;

                                while (k2 < 3)
                                {
                                    int l2 = k1;
                                    int i3 = l1;
                                    int j3 = i2;
                                    byte b1 = 6;
                                    SpawnListEntry spawnlistentry = null;
                                    int k3 = 0;

                                    while (true)
                                    {
                                        if (k3 < 4)
                                        {
                                            label103:
                                            {
                                                l2 += par0WorldServer.rand.nextInt(b1) - par0WorldServer.rand.nextInt(b1);
                                                i3 += par0WorldServer.rand.nextInt(1) - par0WorldServer.rand.nextInt(1);
                                                j3 += par0WorldServer.rand.nextInt(b1) - par0WorldServer.rand.nextInt(b1);

                                                if (canCreatureTypeSpawnAtLocation(enumcreaturetype, par0WorldServer, l2, i3, j3))
                                                {
                                                    float f = (float)l2 + 0.5F;
                                                    float f1 = (float)i3;
                                                    float f2 = (float)j3 + 0.5F;

                                                    if (par0WorldServer.getClosestPlayer((double)f, (double)f1, (double)f2, 24.0D) == null)
                                                    {
                                                        float f3 = f - (float)chunkcoordinates.posX;
                                                        float f4 = f1 - (float)chunkcoordinates.posY;
                                                        float f5 = f2 - (float)chunkcoordinates.posZ;
                                                        float f6 = f3 * f3 + f4 * f4 + f5 * f5;

                                                        if (f6 >= 576.0F)
                                                        {
                                                            if (spawnlistentry == null)
                                                            {
                                                                spawnlistentry = par0WorldServer.spawnRandomCreature(enumcreaturetype, l2, i3, j3);

                                                                if (spawnlistentry == null)
                                                                {
                                                                    break label103;
                                                                }
                                                            }

                                                            EntityLiving entityliving;

                                                            try
                                                            {
                                                                entityliving = (EntityLiving)spawnlistentry.entityClass.getConstructor(new Class[] {World.class}).newInstance(new Object[] {par0WorldServer});
                                                            }
                                                            catch (Exception exception)
                                                            {
                                                                exception.printStackTrace();
                                                                return i;
                                                            }

                                                            entityliving.setLocationAndAngles((double)f, (double)f1, (double)f2, par0WorldServer.rand.nextFloat() * 360.0F, 0.0F);

                                                            Result canSpawn = ForgeEventFactory.canEntitySpawn(entityliving, par0WorldServer, f, f1, f2);
                                                            if (canSpawn == Result.ALLOW || (canSpawn == Result.DEFAULT && entityliving.getCanSpawnHere()))
                                                            {
                                                                ++j2;
                                                                // CraftBukkit start - Added a reason for spawning this creature, moved a(entityliving, world...) up
                                                                creatureSpecificInit(entityliving, par0WorldServer, f, f1, f2);
                                                                par0WorldServer.addEntity(entityliving, CreatureSpawnEvent.SpawnReason.NATURAL);

                                                                // CraftBukkit end
                                                                // Spigot start
                                                                moblimit--;

                                                                if (moblimit <= 0)   // If we're past limit, stop spawn
                                                                {
                                                                    continue label110;
                                                                }

                                                                // Spigot end                                                                
                                                                if (j2 >= ForgeEventFactory.getMaxSpawnPackSize(entityliving))
                                                                {
                                                                    continue label110;
                                                                }
                                                            }

                                                            i += j2;
                                                        }
                                                    }
                                                }

                                                ++k3;
                                                continue;
                                            }
                                        }

                                        ++k2;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return i;
        }
    }

    /**
     * Returns whether or not the specified creature type can spawn at the specified location.
     */
    public static boolean canCreatureTypeSpawnAtLocation(EnumCreatureType par0EnumCreatureType, World par1World, int par2, int par3, int par4)
    {
        if (par0EnumCreatureType.getCreatureMaterial() == Material.water)
        {
            return par1World.getBlockMaterial(par2, par3, par4).isLiquid() && par1World.getBlockMaterial(par2, par3 - 1, par4).isLiquid() && !par1World.isBlockNormalCube(par2, par3 + 1, par4);
        }
        else if (!par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4))
        {
            return false;
        }
        else
        {
            int l = par1World.getBlockId(par2, par3 - 1, par4);
            boolean spawnBlock = (Block.blocksList[l] != null && Block.blocksList[l].canCreatureSpawn(par0EnumCreatureType, par1World, par2, par3 - 1, par4));
            return spawnBlock && l != Block.bedrock.blockID && !par1World.isBlockNormalCube(par2, par3, par4) && !par1World.getBlockMaterial(par2, par3, par4).isLiquid() && !par1World.isBlockNormalCube(par2, par3 + 1, par4);
        }
    }

    /**
     * determines if a skeleton spawns on a spider, and if a sheep is a different color
     */
    private static void creatureSpecificInit(EntityLiving par0EntityLiving, World par1World, float par2, float par3, float par4)
    {
        if (ForgeEventFactory.doSpecialSpawn(par0EntityLiving, par1World, par2, par3, par4))
        {
            return;
        }

        par0EntityLiving.initCreature();
    }

    /**
     * Called during chunk generation to spawn initial creatures.
     */
    public static void performWorldGenSpawning(World par0World, BiomeGenBase par1BiomeGenBase, int par2, int par3, int par4, int par5, Random par6Random)
    {
        List list = par1BiomeGenBase.getSpawnableList(EnumCreatureType.creature);

        if (!list.isEmpty())
        {
            while (par6Random.nextFloat() < par1BiomeGenBase.getSpawningChance())
            {
                SpawnListEntry spawnlistentry = (SpawnListEntry) WeightedRandom.getRandomItem(par0World.rand, (Collection) list);
                int i1 = spawnlistentry.minGroupCount + par6Random.nextInt(1 + spawnlistentry.maxGroupCount - spawnlistentry.minGroupCount);
                int j1 = par2 + par6Random.nextInt(par4);
                int k1 = par3 + par6Random.nextInt(par5);
                int l1 = j1;
                int i2 = k1;

                for (int j2 = 0; j2 < i1; ++j2)
                {
                    boolean flag = false;

                    for (int k2 = 0; !flag && k2 < 4; ++k2)
                    {
                        int l2 = par0World.getTopSolidOrLiquidBlock(j1, k1);

                        if (canCreatureTypeSpawnAtLocation(EnumCreatureType.creature, par0World, j1, l2, k1))
                        {
                            float f = (float)j1 + 0.5F;
                            float f1 = (float)l2;
                            float f2 = (float)k1 + 0.5F;
                            EntityLiving entityliving;

                            try
                            {
                                entityliving = (EntityLiving)spawnlistentry.entityClass.getConstructor(new Class[] {World.class}).newInstance(new Object[] {par0World});
                            }
                            catch (Exception exception)
                            {
                                exception.printStackTrace();
                                continue;
                            }

                            entityliving.setLocationAndAngles((double)f, (double)f1, (double)f2, par6Random.nextFloat() * 360.0F, 0.0F);
                            // CraftBukkit start - Added a reason for spawning this creature, moved a(entity, world...) up
                            creatureSpecificInit(entityliving, par0World, f, f1, f2);
                            par0World.addEntity(entityliving, CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
                            // CraftBukkit end
                            flag = true;
                        }

                        j1 += par6Random.nextInt(5) - par6Random.nextInt(5);

                        for (k1 += par6Random.nextInt(5) - par6Random.nextInt(5); j1 < par2 || j1 >= par2 + par4 || k1 < par3 || k1 >= par3 + par4; k1 = i2 + par6Random.nextInt(5) - par6Random.nextInt(5))
                        {
                            j1 = l1 + par6Random.nextInt(5) - par6Random.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}
