package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

public class CraftProjectile extends AbstractProjectile implements Projectile { // MCPC
    public CraftProjectile(CraftServer server, /*was:net.minecraft.server.*/net.minecraft.entity.Entity/*was:Entity*/ entity) {
        super(server, entity);
    }

    public LivingEntity getShooter() {
        if (getHandle().getThrower/*was:getShooter*/() instanceof net.minecraft.entity.EntityLiving/*was:EntityLiving*/) {
            return (LivingEntity) getHandle().getThrower/*was:getShooter*/().getBukkitEntity();
        }

        return null;
    }

    public void setShooter(LivingEntity shooter) {
        if (shooter instanceof CraftLivingEntity) {
            getHandle().thrower/*was:shooter*/ = (net.minecraft.entity.EntityLiving/*was:EntityLiving*/) ((CraftLivingEntity) shooter).entity;
            if (shooter instanceof CraftHumanEntity) {
                getHandle().throwerName/*was:shooterName*/ = ((CraftHumanEntity) shooter).getName();
            }
        }
    }

    @Override
    public net.minecraft.entity.projectile.EntityThrowable/*was:EntityProjectile*/ getHandle() {
        return (net.minecraft.entity.projectile.EntityThrowable/*was:EntityProjectile*/) entity;
    }

    @Override
    public String toString() {
        return "CraftProjectile";
    }

    // MCPC start
    @Override
    public EntityType getType() {
        return EntityType.UNKNOWN;
    }
    // MCPC end
}
