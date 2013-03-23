package org.bukkit.craftbukkit.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


import org.apache.commons.lang.Validate;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftEntityEquipment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class CraftLivingEntity extends CraftEntity implements LivingEntity {
    private CraftEntityEquipment equipment;

    public CraftLivingEntity(final CraftServer server, final net.minecraft.entity.EntityLiving/*was:EntityLiving*/ entity) {
        super(server, entity);

        if (!(this instanceof HumanEntity)) {
            equipment = new CraftEntityEquipment(this);
        }
    }

    public int getHealth() {
        return Math.min(Math.max(0, getHandle().getHealth/*was:getHealth*/()), getMaxHealth());
    }

    public void setHealth(int health) {
        if ((health < 0) || (health > getMaxHealth())) {
            throw new IllegalArgumentException("Health must be between 0 and " + getMaxHealth());
        }

        if (entity instanceof net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ && health == 0) {
            ((net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) entity).onDeath/*was:die*/(net.minecraft.util.DamageSource/*was:DamageSource*/.generic/*was:GENERIC*/);
        }

        getHandle().setEntityHealth/*was:setHealth*/(health);
    }

    public int getMaxHealth() {
        return getHandle().maxHealth;
    }

    public void setMaxHealth(int amount) {
        Validate.isTrue(amount > 0, "Max health must be greater than 0");

        getHandle().maxHealth = amount;

        if (getHealth() > amount) {
            setHealth(amount);
        }
    }

    public void resetMaxHealth() {
        setMaxHealth(getHandle().getMaxHealth/*was:getMaxHealth*/());
    }

    @Deprecated
    public Egg throwEgg() {
        return launchProjectile(Egg.class);
    }

    @Deprecated
    public Snowball throwSnowball() {
        return launchProjectile(Snowball.class);
    }

    public double getEyeHeight() {
        return getHandle().getEyeHeight/*was:getHeadHeight*/();
    }

    public double getEyeHeight(boolean ignoreSneaking) {
        return getEyeHeight();
    }

    private List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance, int maxLength) {
        if (maxDistance > 120) {
            maxDistance = 120;
        }
        ArrayList<Block> blocks = new ArrayList<Block>();
        Iterator<Block> itr = new BlockIterator(this, maxDistance);
        while (itr.hasNext()) {
            Block block = itr.next();
            blocks.add(block);
            if (maxLength != 0 && blocks.size() > maxLength) {
                blocks.remove(0);
            }
            int id = block.getTypeId();
            if (transparent == null) {
                if (id != 0) {
                    break;
                }
            } else {
                if (!transparent.contains((byte) id)) {
                    break;
                }
            }
        }
        return blocks;
    }

    public List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 0);
    }

    public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
        List<Block> blocks = getLineOfSight(transparent, maxDistance, 1);
        return blocks.get(0);
    }

    public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 2);
    }

    @Deprecated
    public Arrow shootArrow() {
        return launchProjectile(Arrow.class);
    }

    public int getRemainingAir() {
        return getHandle().getAir/*was:getAirTicks*/();
    }

    public void setRemainingAir(int ticks) {
        getHandle().setAir/*was:setAirTicks*/(ticks);
    }

    public int getMaximumAir() {
        return getHandle().maxAirTicks;
    }

    public void setMaximumAir(int ticks) {
        getHandle().maxAirTicks = ticks;
    }

    public void damage(int amount) {
        damage(amount, null);
    }

    public void damage(int amount, org.bukkit.entity.Entity source) {
        net.minecraft.util.DamageSource/*was:DamageSource*/ reason = net.minecraft.util.DamageSource/*was:DamageSource*/.generic/*was:GENERIC*/;

        if (source instanceof HumanEntity) {
            reason = net.minecraft.util.DamageSource/*was:DamageSource*/.causePlayerDamage/*was:playerAttack*/(((CraftHumanEntity) source).getHandle());
        } else if (source instanceof LivingEntity) {
            reason = net.minecraft.util.DamageSource/*was:DamageSource*/.causeMobDamage/*was:mobAttack*/(((CraftLivingEntity) source).getHandle());
        }

        if (entity instanceof net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) {
            ((net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) entity).func_82195_e/*was:dealDamage*/(reason, amount);
        } else {
            entity.attackEntityFrom/*was:damageEntity*/(reason, amount);
        }
    }

    public Location getEyeLocation() {
        Location loc = getLocation();
        loc.setY(loc.getY() + getEyeHeight());
        return loc;
    }

    public int getMaximumNoDamageTicks() {
        return getHandle().maxHurtResistantTime/*was:maxNoDamageTicks*/;
    }

    public void setMaximumNoDamageTicks(int ticks) {
        getHandle().maxHurtResistantTime/*was:maxNoDamageTicks*/ = ticks;
    }

    public int getLastDamage() {
        return getHandle().lastDamage/*was:lastDamage*/;
    }

    public void setLastDamage(int damage) {
        getHandle().lastDamage/*was:lastDamage*/ = damage;
    }

    public int getNoDamageTicks() {
        return getHandle().hurtResistantTime/*was:noDamageTicks*/;
    }

    public void setNoDamageTicks(int ticks) {
        getHandle().hurtResistantTime/*was:noDamageTicks*/ = ticks;
    }

    @Override
    public net.minecraft.entity.EntityLiving/*was:EntityLiving*/ getHandle() {
        return (net.minecraft.entity.EntityLiving/*was:EntityLiving*/) entity;
    }

    public void setHandle(final net.minecraft.entity.EntityLiving/*was:EntityLiving*/ entity) {
        super.setHandle(entity);
    }

    @Override
    public String toString() {
        return "CraftLivingEntity{" + "id=" + getEntityId() + '}';
    }

    public Player getKiller() {
        return getHandle().attackingPlayer/*was:killer*/ == null ? null : (Player) getHandle().attackingPlayer/*was:killer*/.getBukkitEntity();
    }

    public boolean addPotionEffect(PotionEffect effect) {
        return addPotionEffect(effect, false);
    }

    public boolean addPotionEffect(PotionEffect effect, boolean force) {
        if (hasPotionEffect(effect.getType())) {
            if (!force) {
                return false;
            }
            removePotionEffect(effect.getType());
        }
        getHandle().addPotionEffect/*was:addEffect*/(new net.minecraft.potion.PotionEffect/*was:MobEffect*/(effect.getType().getId(), effect.getDuration(), effect.getAmplifier()));
        return true;
    }

    public boolean addPotionEffects(Collection<PotionEffect> effects) {
        boolean success = true;
        for (PotionEffect effect : effects) {
            success &= addPotionEffect(effect);
        }
        return success;
    }

    public boolean hasPotionEffect(PotionEffectType type) {
        return getHandle().isPotionActive/*was:hasEffect*/(net.minecraft.potion.Potion/*was:MobEffectList*/.potionTypes/*was:byId*/[type.getId()]);
    }

    public void removePotionEffect(PotionEffectType type) {
        getHandle().activePotionsMap/*was:effects*/.remove(type.getId());
        getHandle().potionsNeedUpdate/*was:updateEffects*/ = true;
        if (getHandle() instanceof net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) {
            if (((net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) getHandle()).playerNetServerHandler/*was:playerConnection*/ == null) return;
            ((net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) getHandle()).playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet42RemoveEntityEffect/*was:Packet42RemoveMobEffect*/(getHandle().entityId/*was:id*/, new net.minecraft.potion.PotionEffect/*was:MobEffect*/(type.getId(), 0, 0)));
        }
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (Object raw : getHandle().activePotionsMap/*was:effects*/.values()) {
            if (!(raw instanceof net.minecraft.potion.PotionEffect/*was:MobEffect*/))
                continue;
            net.minecraft.potion.PotionEffect/*was:MobEffect*/ handle = (net.minecraft.potion.PotionEffect/*was:MobEffect*/) raw;
            effects.add(new PotionEffect(PotionEffectType.getById(handle.getPotionID/*was:getEffectId*/()), handle.getDuration/*was:getDuration*/(), handle.getAmplifier/*was:getAmplifier*/()));
        }
        return effects;
    }

    @SuppressWarnings("unchecked")
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
        /*was:net.minecraft.server.*/net.minecraft.world.World/*was:World*/ world = ((CraftWorld) getWorld()).getHandle();
        /*was:net.minecraft.server.*/net.minecraft.entity.Entity/*was:Entity*/ launch = null;

        if (Snowball.class.isAssignableFrom(projectile)) {
            launch = new net.minecraft.entity.projectile.EntitySnowball/*was:EntitySnowball*/(world, getHandle());
        } else if (Egg.class.isAssignableFrom(projectile)) {
            launch = new net.minecraft.entity.projectile.EntityEgg/*was:EntityEgg*/(world, getHandle());
        } else if (EnderPearl.class.isAssignableFrom(projectile)) {
            launch = new net.minecraft.entity.item.EntityEnderPearl/*was:EntityEnderPearl*/(world, getHandle());
        } else if (Arrow.class.isAssignableFrom(projectile)) {
            launch = new net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/(world, getHandle(), 1);
        } else if (Fireball.class.isAssignableFrom(projectile)) {
            Location location = getEyeLocation();
            Vector direction = location.getDirection().multiply(10);

            if (SmallFireball.class.isAssignableFrom(projectile)) {
                launch = new net.minecraft.entity.projectile.EntitySmallFireball/*was:EntitySmallFireball*/(world, getHandle(), direction.getX(), direction.getY(), direction.getZ());
            } else if (WitherSkull.class.isAssignableFrom(projectile)) {
                launch = new net.minecraft.entity.projectile.EntityWitherSkull/*was:EntityWitherSkull*/(world, getHandle(), direction.getX(), direction.getY(), direction.getZ());
            } else {
                launch = new net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/(world, getHandle(), direction.getX(), direction.getY(), direction.getZ());
            }

            launch.setLocationAndAngles/*was:setPositionRotation*/(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        }

        Validate.notNull(launch, "Projectile not supported");

        world.spawnEntityInWorld/*was:addEntity*/(launch);
        return (T) launch.getBukkitEntity();
    }

    public EntityType getType() {
        return EntityType.UNKNOWN;
    }

    public boolean hasLineOfSight(Entity other) {
        return getHandle().getEntitySenses/*was:aA*/().canSee/*was:canSee*/(((CraftEntity) other).getHandle()); // az should be getEntitySenses
    }

    public boolean getRemoveWhenFarAway() {
        return !getHandle().persistenceRequired/*was:persistent*/;
    }

    public void setRemoveWhenFarAway(boolean remove) {
        getHandle().persistenceRequired/*was:persistent*/ = !remove;
    }

    public EntityEquipment getEquipment() {
        return equipment;
    }

    public void setCanPickupItems(boolean pickup) {
        getHandle().canPickUpLoot/*was:canPickUpLoot*/ = pickup;
    }

    public boolean getCanPickupItems() {
        return getHandle().canPickUpLoot/*was:canPickUpLoot*/;
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (getHealth() == 0) {
            return false;
        }

        return super.teleport(location, cause);
    }
}