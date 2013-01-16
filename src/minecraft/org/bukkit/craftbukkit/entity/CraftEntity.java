package org.bukkit.craftbukkit.entity;

import java.util.List;
import java.util.UUID;


import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public abstract class CraftEntity implements org.bukkit.entity.Entity {
    protected final CraftServer server;
    protected net.minecraft.entity.Entity/*was:Entity*/ entity;
    private EntityDamageEvent lastDamageEvent;
    private static CraftEntity instance; // MCPC

    public CraftEntity(final CraftServer server, final net.minecraft.entity.Entity/*was:Entity*/ entity) {
        this.server = server;
        this.entity = entity;
        instance = this; // MCPC
    }

    public static CraftEntity getEntity(CraftServer server, net.minecraft.entity.Entity/*was:Entity*/ entity) {
        /**
         * Order is *EXTREMELY* important -- keep it right! =D
         */
        if (entity instanceof net.minecraft.entity.EntityLiving/*was:EntityLiving*/) {
            // Players
            if (entity instanceof net.minecraft.entity.player.EntityPlayer/*was:EntityHuman*/) {
                if (entity instanceof net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) { return new CraftPlayer(server, (net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) entity); }
                else { return new CraftHumanEntity(server, (net.minecraft.entity.player.EntityPlayer/*was:EntityHuman*/) entity); }
            }
            else if (entity instanceof net.minecraft.entity.EntityCreature/*was:EntityCreature*/) {
                // Animals
                if (entity instanceof net.minecraft.entity.passive.EntityAnimal/*was:EntityAnimal*/) {
                    if (entity instanceof net.minecraft.entity.passive.EntityChicken/*was:EntityChicken*/) { return new CraftChicken(server, (net.minecraft.entity.passive.EntityChicken/*was:EntityChicken*/) entity); }
                    else if (entity instanceof net.minecraft.entity.passive.EntityCow/*was:EntityCow*/) {
                        if (entity instanceof net.minecraft.entity.passive.EntityMooshroom/*was:EntityMushroomCow*/) { return new CraftMushroomCow(server, (net.minecraft.entity.passive.EntityMooshroom/*was:EntityMushroomCow*/) entity); }
                        else { return new CraftCow(server, (net.minecraft.entity.passive.EntityCow/*was:EntityCow*/) entity); }
                    }
                    else if (entity instanceof net.minecraft.entity.passive.EntityPig/*was:EntityPig*/) { return new CraftPig(server, (net.minecraft.entity.passive.EntityPig/*was:EntityPig*/) entity); }
                    else if (entity instanceof net.minecraft.entity.passive.EntityTameable/*was:EntityTameableAnimal*/) {
                        if (entity instanceof net.minecraft.entity.passive.EntityWolf/*was:EntityWolf*/) { return new CraftWolf(server, (net.minecraft.entity.passive.EntityWolf/*was:EntityWolf*/) entity); }
                        else if (entity instanceof net.minecraft.entity.passive.EntityOcelot/*was:EntityOcelot*/) { return new CraftOcelot(server, (net.minecraft.entity.passive.EntityOcelot/*was:EntityOcelot*/) entity); }
                        // MCPC - add support for Pixelmon
                        else { return new CraftTameableAnimal(server, (net.minecraft.entity.passive.EntityTameable/*was:EntityTameableAnimal*/) entity); }
                    }
                    else if (entity instanceof net.minecraft.entity.passive.EntitySheep/*was:EntitySheep*/) { return new CraftSheep(server, (net.minecraft.entity.passive.EntitySheep/*was:EntitySheep*/) entity); }
                    else  { return new CraftAnimals(server, (net.minecraft.entity.passive.EntityAnimal/*was:EntityAnimal*/) entity); }
                }
                // Monsters
                else if (entity instanceof net.minecraft.entity.monster.EntityMob/*was:EntityMonster*/) {
                    if (entity instanceof net.minecraft.entity.monster.EntityZombie/*was:EntityZombie*/) {
                        if (entity instanceof net.minecraft.entity.monster.EntityPigZombie/*was:EntityPigZombie*/) { return new CraftPigZombie(server, (net.minecraft.entity.monster.EntityPigZombie/*was:EntityPigZombie*/) entity); }
                        else { return new CraftZombie(server, (net.minecraft.entity.monster.EntityZombie/*was:EntityZombie*/) entity); }
                    }
                    else if (entity instanceof net.minecraft.entity.monster.EntityCreeper/*was:EntityCreeper*/) { return new CraftCreeper(server, (net.minecraft.entity.monster.EntityCreeper/*was:EntityCreeper*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntityEnderman/*was:EntityEnderman*/) { return new CraftEnderman(server, (net.minecraft.entity.monster.EntityEnderman/*was:EntityEnderman*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntitySilverfish/*was:EntitySilverfish*/) { return new CraftSilverfish(server, (net.minecraft.entity.monster.EntitySilverfish/*was:EntitySilverfish*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntityGiantZombie/*was:EntityGiantZombie*/) { return new CraftGiant(server, (net.minecraft.entity.monster.EntityGiantZombie/*was:EntityGiantZombie*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntitySkeleton/*was:EntitySkeleton*/) { return new CraftSkeleton(server, (net.minecraft.entity.monster.EntitySkeleton/*was:EntitySkeleton*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntityBlaze/*was:EntityBlaze*/) { return new CraftBlaze(server, (net.minecraft.entity.monster.EntityBlaze/*was:EntityBlaze*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntityWitch/*was:EntityWitch*/) { return new CraftWitch(server, (net.minecraft.entity.monster.EntityWitch/*was:EntityWitch*/) entity); }
                    else if (entity instanceof net.minecraft.entity.boss.EntityWither/*was:EntityWither*/) { return new CraftWither(server, (net.minecraft.entity.boss.EntityWither/*was:EntityWither*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntitySpider/*was:EntitySpider*/) {
                        if (entity instanceof net.minecraft.entity.monster.EntityCaveSpider/*was:EntityCaveSpider*/) { return new CraftCaveSpider(server, (net.minecraft.entity.monster.EntityCaveSpider/*was:EntityCaveSpider*/) entity); }
                        else { return new CraftSpider(server, (net.minecraft.entity.monster.EntitySpider/*was:EntitySpider*/) entity); }
                    }

                    else  { return new CraftMonster(server, (net.minecraft.entity.monster.EntityMob/*was:EntityMonster*/) entity); }
                }
                // Water Animals
                else if (entity instanceof net.minecraft.entity.passive.EntityWaterMob/*was:EntityWaterAnimal*/) {
                    if (entity instanceof net.minecraft.entity.passive.EntitySquid/*was:EntitySquid*/) { return new CraftSquid(server, (net.minecraft.entity.passive.EntitySquid/*was:EntitySquid*/) entity); }
                    else { return new CraftWaterMob(server, (net.minecraft.entity.passive.EntityWaterMob/*was:EntityWaterAnimal*/) entity); }
                }
                else if (entity instanceof net.minecraft.entity.monster.EntityGolem/*was:EntityGolem*/) {
                    if (entity instanceof net.minecraft.entity.monster.EntitySnowman/*was:EntitySnowman*/) { return new CraftSnowman(server, (net.minecraft.entity.monster.EntitySnowman/*was:EntitySnowman*/) entity); }
                    else if (entity instanceof net.minecraft.entity.monster.EntityIronGolem/*was:EntityIronGolem*/) { return new CraftIronGolem(server, (net.minecraft.entity.monster.EntityIronGolem/*was:EntityIronGolem*/) entity); }
                }
                else if (entity instanceof net.minecraft.entity.passive.EntityVillager/*was:EntityVillager*/) { return new CraftVillager(server, (net.minecraft.entity.passive.EntityVillager/*was:EntityVillager*/) entity); }
                else { return new CraftCreature(server, (net.minecraft.entity.EntityCreature/*was:EntityCreature*/) entity); }
            }
            // Slimes are a special (and broken) case
            else if (entity instanceof net.minecraft.entity.monster.EntitySlime/*was:EntitySlime*/) {
                if (entity instanceof net.minecraft.entity.monster.EntityMagmaCube/*was:EntityMagmaCube*/) { return new CraftMagmaCube(server, (net.minecraft.entity.monster.EntityMagmaCube/*was:EntityMagmaCube*/) entity); }
                else { return new CraftSlime(server, (net.minecraft.entity.monster.EntitySlime/*was:EntitySlime*/) entity); }
            }
            // Flying
            else if (entity instanceof net.minecraft.entity.EntityFlying/*was:EntityFlying*/) {
                if (entity instanceof net.minecraft.entity.monster.EntityGhast/*was:EntityGhast*/) { return new CraftGhast(server, (net.minecraft.entity.monster.EntityGhast/*was:EntityGhast*/) entity); }
                else { return new CraftFlying(server, (net.minecraft.entity.EntityFlying/*was:EntityFlying*/) entity); }
            }
            else if (entity instanceof net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) {
                return new CraftEnderDragon(server, (net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) entity);
            }
            // Ambient
            else if (entity instanceof net.minecraft.entity.passive.EntityAmbientCreature/*was:EntityAmbient*/) {
                if (entity instanceof net.minecraft.entity.passive.EntityBat/*was:EntityBat*/) { return new CraftBat(server, (net.minecraft.entity.passive.EntityBat/*was:EntityBat*/) entity); }
                else { return new CraftAmbient(server, (net.minecraft.entity.passive.EntityAmbientCreature/*was:EntityAmbient*/) entity); }
            }
            else  { return new CraftLivingEntity(server, (net.minecraft.entity.EntityLiving/*was:EntityLiving*/) entity); }
        }
        else if (entity instanceof net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/) {
            net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/ part = (net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/) entity;
            if (part.entityDragonObj/*was:owner*/ instanceof net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) { return new CraftEnderDragonPart(server, (net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/) entity); }
            else { return new CraftComplexPart(server, (net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/) entity); }
        }
        else if (entity instanceof net.minecraft.entity.item.EntityXPOrb/*was:EntityExperienceOrb*/) { return new CraftExperienceOrb(server, (net.minecraft.entity.item.EntityXPOrb/*was:EntityExperienceOrb*/) entity); }
        else if (entity instanceof net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/) { return new CraftArrow(server, (net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/) entity); }
        else if (entity instanceof net.minecraft.entity.item.EntityBoat/*was:EntityBoat*/) { return new CraftBoat(server, (net.minecraft.entity.item.EntityBoat/*was:EntityBoat*/) entity); }
        else if (entity instanceof net.minecraft.entity.projectile.EntityThrowable/*was:EntityProjectile*/) {
            if (entity instanceof net.minecraft.entity.projectile.EntityEgg/*was:EntityEgg*/) { return new CraftEgg(server, (net.minecraft.entity.projectile.EntityEgg/*was:EntityEgg*/) entity); }
            else if (entity instanceof net.minecraft.entity.projectile.EntitySnowball/*was:EntitySnowball*/) { return new CraftSnowball(server, (net.minecraft.entity.projectile.EntitySnowball/*was:EntitySnowball*/) entity); }
            else if (entity instanceof net.minecraft.entity.projectile.EntityPotion/*was:EntityPotion*/) { return new CraftThrownPotion(server, (net.minecraft.entity.projectile.EntityPotion/*was:EntityPotion*/) entity); }
            else if (entity instanceof net.minecraft.entity.item.EntityEnderPearl/*was:EntityEnderPearl*/) { return new CraftEnderPearl(server, (net.minecraft.entity.item.EntityEnderPearl/*was:EntityEnderPearl*/) entity); }
            else if (entity instanceof net.minecraft.entity.item.EntityExpBottle/*was:EntityThrownExpBottle*/) { return new CraftThrownExpBottle(server, (net.minecraft.entity.item.EntityExpBottle/*was:EntityThrownExpBottle*/) entity); }
            else { return new CraftProjectile(server, (net.minecraft.entity.projectile.EntityThrowable/*was:EntityProjectile*/) entity); } // MCPC
        }
        else if (entity instanceof net.minecraft.entity.item.EntityFallingSand/*was:EntityFallingBlock*/) { return new CraftFallingSand(server, (net.minecraft.entity.item.EntityFallingSand/*was:EntityFallingBlock*/) entity); }
        else if (entity instanceof net.minecraft.entity.projectile.EntityFireball/*was:EntityFireball*/) {
            if (entity instanceof net.minecraft.entity.projectile.EntitySmallFireball/*was:EntitySmallFireball*/) { return new CraftSmallFireball(server, (net.minecraft.entity.projectile.EntitySmallFireball/*was:EntitySmallFireball*/) entity); }
            else if (entity instanceof net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/) { return new CraftLargeFireball(server, (net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/) entity); }
            else if (entity instanceof net.minecraft.entity.projectile.EntityWitherSkull/*was:EntityWitherSkull*/) { return new CraftWitherSkull(server, (net.minecraft.entity.projectile.EntityWitherSkull/*was:EntityWitherSkull*/) entity); }
            else { return new CraftFireball(server, (net.minecraft.entity.projectile.EntityFireball/*was:EntityFireball*/) entity); }
        }
        else if (entity instanceof net.minecraft.entity.item.EntityEnderEye/*was:EntityEnderSignal*/) { return new CraftEnderSignal(server, (net.minecraft.entity.item.EntityEnderEye/*was:EntityEnderSignal*/) entity); }
        else if (entity instanceof net.minecraft.entity.item.EntityEnderCrystal/*was:EntityEnderCrystal*/) { return new CraftEnderCrystal(server, (net.minecraft.entity.item.EntityEnderCrystal/*was:EntityEnderCrystal*/) entity); }
        else if (entity instanceof net.minecraft.entity.projectile.EntityFishHook/*was:EntityFishingHook*/) { return new CraftFish(server, (net.minecraft.entity.projectile.EntityFishHook/*was:EntityFishingHook*/) entity); }
        else if (entity instanceof net.minecraft.entity.item.EntityItem/*was:EntityItem*/) { return new CraftItem(server, (net.minecraft.entity.item.EntityItem/*was:EntityItem*/) entity); }
        else if (entity instanceof net.minecraft.entity.effect.EntityWeatherEffect/*was:EntityWeather*/) {
            if (entity instanceof net.minecraft.entity.effect.EntityLightningBolt/*was:EntityLightning*/) { return new CraftLightningStrike(server, (net.minecraft.entity.effect.EntityLightningBolt/*was:EntityLightning*/) entity); }
            else { return new CraftWeather(server, (net.minecraft.entity.effect.EntityWeatherEffect/*was:EntityWeather*/) entity); }
        }
        else if (entity instanceof net.minecraft.entity.item.EntityMinecart/*was:EntityMinecart*/) {
            net.minecraft.entity.item.EntityMinecart/*was:EntityMinecart*/ mc = (net.minecraft.entity.item.EntityMinecart/*was:EntityMinecart*/) entity;
            if (mc.minecartType/*was:type*/ == CraftMinecart.Type.StorageMinecart.getId()) { return new CraftStorageMinecart(server, mc); }
            else if (mc.minecartType/*was:type*/ == CraftMinecart.Type.PoweredMinecart.getId()) { return new CraftPoweredMinecart(server, mc); }
            else { return new CraftMinecart(server, mc); }
        }
        else if (entity instanceof net.minecraft.entity.EntityHanging/*was:EntityHanging*/) {
            if (entity instanceof net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/) { return new CraftPainting(server, (net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/) entity); }
            else if (entity instanceof net.minecraft.entity.item.EntityItemFrame/*was:EntityItemFrame*/) { return new CraftItemFrame(server, (net.minecraft.entity.item.EntityItemFrame/*was:EntityItemFrame*/) entity); }
            else { return new CraftHanging(server, (net.minecraft.entity.EntityHanging/*was:EntityHanging*/) entity); }
        }
        else if (entity instanceof net.minecraft.entity.item.EntityTNTPrimed/*was:EntityTNTPrimed*/) { return new CraftTNTPrimed(server, (net.minecraft.entity.item.EntityTNTPrimed/*was:EntityTNTPrimed*/) entity); }
        else if (entity instanceof net.minecraft.entity.item.EntityFireworkRocket/*was:EntityFireworks*/) { return new CraftFirework(server, (net.minecraft.entity.item.EntityFireworkRocket/*was:EntityFireworks*/) entity); }
        // MCPC - used for custom entities that extend Entity
        else if (entity instanceof net.minecraft.entity.Entity/*was:Entity*/) { return instance; }

        throw new IllegalArgumentException("Unknown entity " + entity); // MCPC - show the entity that caused exception
    }

    public Location getLocation() {
        return new Location(getWorld(), entity.posX/*was:locX*/, entity.posY/*was:locY*/, entity.posZ/*was:locZ*/, entity.rotationYaw/*was:yaw*/, entity.rotationPitch/*was:pitch*/);
    }

    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setWorld(getWorld());
            loc.setX(entity.posX/*was:locX*/);
            loc.setY(entity.posY/*was:locY*/);
            loc.setZ(entity.posZ/*was:locZ*/);
            loc.setYaw(entity.rotationYaw/*was:yaw*/);
            loc.setPitch(entity.rotationPitch/*was:pitch*/);
        }

        return loc;
    }

    public Vector getVelocity() {
        return new Vector(entity.motionX/*was:motX*/, entity.motionY/*was:motY*/, entity.motionZ/*was:motZ*/);
    }

    public void setVelocity(Vector vel) {
        entity.motionX/*was:motX*/ = vel.getX();
        entity.motionY/*was:motY*/ = vel.getY();
        entity.motionZ/*was:motZ*/ = vel.getZ();
        entity.velocityChanged/*was:velocityChanged*/ = true;
    }

    public World getWorld() {
        return ((net.minecraft.world.WorldServer/*was:WorldServer*/) entity.worldObj/*was:world*/).getWorld();
    }

    public boolean teleport(Location location) {
        return teleport(location, TeleportCause.PLUGIN);
    }

    public boolean teleport(Location location, TeleportCause cause) {
        if (entity.ridingEntity/*was:vehicle*/ != null || entity.riddenByEntity/*was:passenger*/ != null || entity.isDead/*was:dead*/) {
            return false;
        }

        entity.worldObj/*was:world*/ = ((CraftWorld) location.getWorld()).getHandle();
        entity.setPositionAndRotation/*was:setLocation*/(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        // entity.setLocation() throws no event, and so cannot be cancelled
        return true;
    }

    public boolean teleport(org.bukkit.entity.Entity destination) {
        return teleport(destination.getLocation());
    }

    public boolean teleport(org.bukkit.entity.Entity destination, TeleportCause cause) {
        return teleport(destination.getLocation(), cause);
    }

    public List<org.bukkit.entity.Entity> getNearbyEntities(double x, double y, double z) {
        @SuppressWarnings("unchecked")
        List<net.minecraft.entity.Entity/*was:Entity*/> notchEntityList = entity.worldObj/*was:world*/.getEntitiesWithinAABBExcludingEntity/*was:getEntities*/(entity, entity.boundingBox/*was:boundingBox*/.expand/*was:grow*/(x, y, z));
        List<org.bukkit.entity.Entity> bukkitEntityList = new java.util.ArrayList<org.bukkit.entity.Entity>(notchEntityList.size());

        for (net.minecraft.entity.Entity/*was:Entity*/ e : notchEntityList) {
            bukkitEntityList.add(e.getBukkitEntity());
        }
        return bukkitEntityList;
    }

    public int getEntityId() {
        return entity.entityId/*was:id*/;
    }

    public int getFireTicks() {
        return entity.fire/*was:fireTicks*/;
    }

    public int getMaxFireTicks() {
        return entity.fireResistance/*was:maxFireTicks*/;
    }

    public void setFireTicks(int ticks) {
        entity.fire/*was:fireTicks*/ = ticks;
    }

    public void remove() {
        entity.isDead/*was:dead*/ = true;
    }

    public boolean isDead() {
        return !entity.isEntityAlive/*was:isAlive*/();
    }

    public boolean isValid() {
        return entity.isEntityAlive/*was:isAlive*/() && entity.valid;
    }

    public Server getServer() {
        return server;
    }

    public Vector getMomentum() {
        return getVelocity();
    }

    public void setMomentum(Vector value) {
        setVelocity(value);
    }

    public org.bukkit.entity.Entity getPassenger() {
        return isEmpty() ? null : (CraftEntity) getHandle().riddenByEntity/*was:passenger*/.getBukkitEntity();
    }

    public boolean setPassenger(org.bukkit.entity.Entity passenger) {
        if (passenger instanceof CraftEntity) {
            ((CraftEntity) passenger).getHandle().setPassengerOf(getHandle());
            return true;
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return getHandle().riddenByEntity/*was:passenger*/ == null;
    }

    public boolean eject() {
        if (getHandle().riddenByEntity/*was:passenger*/ == null) {
            return false;
        }

        getHandle().riddenByEntity/*was:passenger*/.setPassengerOf(null);
        return true;
    }

    public float getFallDistance() {
        return getHandle().fallDistance/*was:fallDistance*/;
    }

    public void setFallDistance(float distance) {
        getHandle().fallDistance/*was:fallDistance*/ = distance;
    }

    public void setLastDamageCause(EntityDamageEvent event) {
        lastDamageEvent = event;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageEvent;
    }

    public UUID getUniqueId() {
        return getHandle().persistentID;
    }

    public int getTicksLived() {
        return getHandle().ticksExisted/*was:ticksLived*/;
    }

    public void setTicksLived(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Age must be at least 1 tick");
        }
        getHandle().ticksExisted/*was:ticksLived*/ = value;
    }

    public net.minecraft.entity.Entity/*was:Entity*/ getHandle() {
        return entity;
    }

    public void playEffect(EntityEffect type) {
        this.getHandle().worldObj/*was:world*/.setEntityState/*was:broadcastEntityEffect*/(getHandle(), type.getData());
    }

    public void setHandle(final net.minecraft.entity.Entity/*was:Entity*/ entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return "CraftEntity{" + "id=" + getEntityId() + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CraftEntity other = (CraftEntity) obj;
        return (this.getEntityId() == other.getEntityId());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.getEntityId();
        return hash;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        server.getEntityMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return server.getEntityMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return server.getEntityMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        server.getEntityMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    public boolean isInsideVehicle() {
        return getHandle().ridingEntity/*was:vehicle*/ != null;
    }

    public boolean leaveVehicle() {
        if (getHandle().ridingEntity/*was:vehicle*/ == null) {
            return false;
        }

        getHandle().setPassengerOf(null);
        return true;
    }

    public org.bukkit.entity.Entity getVehicle() {
        if (getHandle().ridingEntity/*was:vehicle*/ == null) {
            return null;
        }

        return getHandle().ridingEntity/*was:vehicle*/.getBukkitEntity();
    }
}