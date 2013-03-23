package org.bukkit.craftbukkit.entity;


import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class CraftArrow extends AbstractProjectile implements Arrow {

    public CraftArrow(CraftServer server, net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/ entity) {
        super(server, entity);
    }

    public LivingEntity getShooter() {
        if (getHandle().shootingEntity/*was:shooter*/ != null) {
            return (LivingEntity) getHandle().shootingEntity/*was:shooter*/.getBukkitEntity();
        }

        return null;
    }

    public void setShooter(LivingEntity shooter) {
        if (shooter instanceof CraftLivingEntity) {
            getHandle().shootingEntity/*was:shooter*/ = ((CraftLivingEntity) shooter).getHandle();
        }
    }

    @Override
    public net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/ getHandle() {
        return (net.minecraft.entity.projectile.EntityArrow/*was:EntityArrow*/) entity;
    }

    @Override
    public String toString() {
        return "CraftArrow";
    }

    public EntityType getType() {
        return EntityType.ARROW;
    }
}
