package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

public class CraftCreature extends CraftLivingEntity implements Creature {
    public CraftCreature(CraftServer server, net.minecraft.entity.EntityCreature/*was:EntityCreature*/ entity) {
        super(server, entity);
    }

    public void setTarget(LivingEntity target) {
        net.minecraft.entity.EntityCreature/*was:EntityCreature*/ entity = getHandle();
        if (target == null) {
            entity.entityToAttack/*was:target*/ = null;
        } else if (target instanceof CraftLivingEntity) {
            entity.entityToAttack/*was:target*/ = ((CraftLivingEntity) target).getHandle();
            entity.pathToEntity/*was:pathEntity*/ = entity.worldObj/*was:world*/.getPathEntityToEntity/*was:findPath*/(entity, entity.entityToAttack/*was:target*/, 16.0F, true, false, false, true);
        }
    }

    public CraftLivingEntity getTarget() {
        if (getHandle().entityToAttack/*was:target*/ == null) return null;
        if (!(getHandle().entityToAttack/*was:target*/ instanceof net.minecraft.entity.EntityLiving/*was:EntityLiving*/)) return null;

        return (CraftLivingEntity) getHandle().entityToAttack/*was:target*/.getBukkitEntity();
    }

    @Override
    public net.minecraft.entity.EntityCreature/*was:EntityCreature*/ getHandle() {
        return (net.minecraft.entity.EntityCreature/*was:EntityCreature*/) entity;
    }

    @Override
    public String toString() {
        return "CraftCreature";
    }
}
