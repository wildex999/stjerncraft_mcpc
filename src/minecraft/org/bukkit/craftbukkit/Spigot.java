package org.bukkit.craftbukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.spigotmc.Metrics;

public class Spigot {

    static net.minecraft.util.AxisAlignedBB maxBB = net.minecraft.util.AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    static net.minecraft.util.AxisAlignedBB miscBB = net.minecraft.util.AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    static net.minecraft.util.AxisAlignedBB animalBB = net.minecraft.util.AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    static net.minecraft.util.AxisAlignedBB monsterBB = net.minecraft.util.AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    public static boolean tabPing = false;
    private static Metrics metrics;

    public static void initialize(CraftServer server, SimpleCommandMap commandMap, YamlConfiguration configuration) {
        if (configuration.getBoolean("settings.tps-command", true)) { // MCPC+ - config option to allow mods to replace command
            commandMap.register("bukkit", new org.bukkit.craftbukkit.command.TicksPerSecondCommand("tps"));
        }

        if (configuration.getBoolean("settings.restart-command", true)) { // MCPC+ - config option to allow mods to replace command
            //commandMap.register("bukkit", new RestartCommand("restart")); // TODO: moved into org.spigotmc
        }

        int timeout = configuration.getInt("settings.timeout-time", 300);
        if (timeout == 180) {
            timeout = 300;
            server.getLogger().info("Migrating to new timeout time of 300");
            configuration.set("settings.timeout-time", timeout);
            server.saveConfig();
        }
        org.bukkit.craftbukkit.util.WatchdogThread.startThread(timeout, configuration.getBoolean("settings.restart-on-crash", false));

        server.whitelistMessage = configuration.getString("settings.whitelist-message", server.whitelistMessage);
        server.stopMessage = configuration.getString("settings.stop-message", server.stopMessage);
        server.logCommands = configuration.getBoolean("settings.log-commands", true);
        server.ipFilter = configuration.getBoolean("settings.filter-unsafe-ips", false);
        server.commandComplete = configuration.getBoolean("settings.command-complete", true);
        server.spamGuardExclusions = configuration.getStringList("settings.spam-exclusions");

        server.orebfuscatorEnabled = configuration.getBoolean("orebfuscator.enable", false);
        server.orebfuscatorEngineMode = configuration.getInt("orebfuscator.engine-mode", 1);
        server.orebfuscatorUpdateRadius = configuration.getInt("orebfuscator.update-radius", 2);
        server.orebfuscatorDisabledWorlds = configuration.getStringList("orebfuscator.disabled-worlds");
        server.orebfuscatorBlocks = configuration.getShortList("orebfuscator.blocks");
        if (server.orebfuscatorEngineMode != 1 && server.orebfuscatorEngineMode != 2) {
            server.orebfuscatorEngineMode = 1;
        }
        server.orebfuscatorForgeOredictBlocks = configuration.getBoolean("orebfuscator.forge-oredict-blocks", true); // MCPC+

        if (server.chunkGCPeriod == 0) {
            server.getLogger().severe("[Spigot] You should not disable chunk-gc, unexpected behaviour may occur!");
        }
        
        tabPing = configuration.getBoolean("settings.tab-ping", tabPing);

        if (metrics == null) {
            try {
                metrics = new Metrics();
                metrics.start();
            } catch (IOException ex) {
                Bukkit.getServer().getLogger().log(Level.SEVERE, "Could not start metrics service", ex);
            }
        }        
    }

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity
     * @return group id
     */
    public static byte initializeEntityActivationType(net.minecraft.entity.Entity entity) {
        if (entity instanceof net.minecraft.entity.monster.EntityMob || entity instanceof net.minecraft.entity.monster.EntitySlime) {
            return 1; // Monster
        } else if (entity instanceof net.minecraft.entity.EntityCreature || entity instanceof net.minecraft.entity.passive.EntityAmbientCreature) {
            return 2; // Animal
        } else {
            return 3; // Misc
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity
     * @param world
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(net.minecraft.entity.Entity entity, CraftWorld world) {
        if ((entity.activationType == 3 && world.miscEntityActivationRange == 0)
                || (entity.activationType == 2 && world.animalEntityActivationRange == 0)
                || (entity.activationType == 1 && world.monsterEntityActivationRange == 0)
                || entity instanceof net.minecraft.entity.player.EntityPlayer
                || entity instanceof net.minecraft.entity.item.EntityItemFrame
                || entity instanceof net.minecraft.entity.projectile.EntityThrowable
                || entity instanceof net.minecraft.entity.boss.EntityDragon
                || entity instanceof net.minecraft.entity.boss.EntityDragonPart
                || entity instanceof net.minecraft.entity.boss.EntityWither
                || entity instanceof net.minecraft.entity.projectile.EntityFireball
                || entity instanceof net.minecraft.entity.effect.EntityWeatherEffect
                || entity instanceof net.minecraft.entity.item.EntityTNTPrimed
                || entity instanceof net.minecraft.entity.item.EntityEnderCrystal
                || entity instanceof net.minecraft.entity.item.EntityFireworkRocket) {
            return true;
        }

        return false;
    }

    /**
     * Utility method to grow an AABB without creating a new AABB or touching
     * the pool, so we can re-use ones we have.
     *
     * @param target
     * @param source
     * @param x
     * @param y
     * @param z
     */
    public static void growBB(net.minecraft.util.AxisAlignedBB target, net.minecraft.util.AxisAlignedBB source, int x, int y, int z) {
        target.minX = source.minX - x;
        target.minY = source.minY - y;
        target.minZ = source.minZ - z;
        target.maxX = source.maxX + x;
        target.maxY = source.maxY + y;
        target.maxZ = source.maxZ + z;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(net.minecraft.world.World world) {
        SpigotTimings.entityActivationCheckTimer.startTiming();
        final int miscActivationRange = world.getWorld().miscEntityActivationRange;
        final int animalActivationRange = world.getWorld().animalEntityActivationRange;
        final int monsterActivationRange = world.getWorld().monsterEntityActivationRange;

        int maxRange = Math.max(monsterActivationRange, animalActivationRange);
        maxRange = Math.max(maxRange, miscActivationRange);
        maxRange = Math.min((world.getWorld().viewDistance << 4) - 8, maxRange);

        for (net.minecraft.entity.Entity player : new ArrayList<net.minecraft.entity.Entity>(world.playerEntities)) {

            player.activatedTick = net.minecraft.server.MinecraftServer.currentTick;
            growBB(maxBB, player.boundingBox, maxRange, 256, maxRange);
            growBB(miscBB, player.boundingBox, miscActivationRange, 256, miscActivationRange);
            growBB(animalBB, player.boundingBox, animalActivationRange, 256, animalActivationRange);
            growBB(monsterBB, player.boundingBox, monsterActivationRange, 256, monsterActivationRange);

            int i = net.minecraft.util.MathHelper.floor_double(maxBB.minX / 16.0D);
            int j = net.minecraft.util.MathHelper.floor_double(maxBB.maxX / 16.0D);
            int k = net.minecraft.util.MathHelper.floor_double(maxBB.minZ / 16.0D);
            int l = net.minecraft.util.MathHelper.floor_double(maxBB.maxZ / 16.0D);

            for (int i1 = i; i1 <= j; ++i1) {
                for (int j1 = k; j1 <= l; ++j1) {
                    if (world.getWorld().isChunkLoaded(i1, j1)) {
                        activateChunkEntities(world.getChunkFromChunkCoords(i1, j1));
                    }
                }
            }
        }
        SpigotTimings.entityActivationCheckTimer.stopTiming();
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk
     */
    private static void activateChunkEntities(net.minecraft.world.chunk.Chunk chunk) {
        for (List<net.minecraft.entity.Entity> slice : chunk.entityLists) {
            for (net.minecraft.entity.Entity entity : slice) {
                if (net.minecraft.server.MinecraftServer.currentTick > entity.activatedTick) {
                    if (entity.defaultActivationState) {
                        entity.activatedTick = net.minecraft.server.MinecraftServer.currentTick;
                        continue;
                    }
                    switch (entity.activationType) {
                        case 1:
                            if (monsterBB.intersectsWith(entity.boundingBox)) {
                                entity.activatedTick = net.minecraft.server.MinecraftServer.currentTick;
                            }
                            break;
                        case 2:
                            if (animalBB.intersectsWith(entity.boundingBox)) {
                                entity.activatedTick = net.minecraft.server.MinecraftServer.currentTick;
                            }
                            break;
                        case 3:
                        default:
                            if (miscBB.intersectsWith(entity.boundingBox)) {
                                entity.activatedTick = net.minecraft.server.MinecraftServer.currentTick;
                            }
                    }
                }
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity
     * @return
     */
    public static boolean checkEntityImmunities(net.minecraft.entity.Entity entity) {
        // quick checks.
        if (entity.inWater /* isInWater */ || entity.fire > 0) {
            return true;
        }
        if (!(entity instanceof net.minecraft.entity.projectile.EntityArrow)) {
            if (!entity.onGround || entity.riddenByEntity != null
                    || entity.ridingEntity != null) {
                return true;
            }
        } else if (!((net.minecraft.entity.projectile.EntityArrow) entity).inGround) {
            return true;
        }
        // special cases.
        if (entity instanceof net.minecraft.entity.EntityLiving) {
            net.minecraft.entity.EntityLiving living = (net.minecraft.entity.EntityLiving) entity;
            if (living.attackTime > 0 || living.hurtTime > 0 || living.activePotionsMap.size() > 0) {
                return true;
            }
            if (entity instanceof net.minecraft.entity.EntityCreature && ((net.minecraft.entity.EntityCreature) entity).entityToAttack != null) {
                return true;
            }
            if (entity instanceof net.minecraft.entity.passive.EntityAnimal) {
                net.minecraft.entity.passive.EntityAnimal animal = (net.minecraft.entity.passive.EntityAnimal) entity;
                if (animal.isChild() || animal.isInLove() /*love*/) {
                    return true;
                }
                if (entity instanceof net.minecraft.entity.passive.EntitySheep && ((net.minecraft.entity.passive.EntitySheep) entity).getSheared()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(net.minecraft.entity.Entity entity) {
        SpigotTimings.checkIfActiveTimer.startTiming();
        boolean isActive = entity.activatedTick >= net.minecraft.server.MinecraftServer.currentTick || entity.defaultActivationState;

        // Should this entity tick?
        if (!isActive) {
            if ((net.minecraft.server.MinecraftServer.currentTick - entity.activatedTick - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                if (checkEntityImmunities(entity)) {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    entity.activatedTick = net.minecraft.server.MinecraftServer.currentTick + 20;
                }
                isActive = true;
            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (!entity.defaultActivationState && entity.ticksExisted % 4 == 0 && !checkEntityImmunities(entity)) {
            isActive = false;
        }
        int x = net.minecraft.util.MathHelper.floor_double(entity.posX);
        int z = net.minecraft.util.MathHelper.floor_double(entity.posZ);
        // Make sure not on edge of unloaded chunk
        if (isActive && !entity.worldObj.doChunksNearChunkExist(x, 0, z, 16)) {
            isActive = false;
        }
        SpigotTimings.checkIfActiveTimer.stopTiming();
        return isActive;
    }
}
