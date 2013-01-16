package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LargeFireball;

public class CraftLargeFireball extends CraftFireball implements LargeFireball {
    public CraftLargeFireball(CraftServer server, net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/ entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/ getHandle() {
        return (net.minecraft.entity.projectile.EntityLargeFireball/*was:EntityLargeFireball*/) entity;
    }

    @Override
    public String toString() {
        return "CraftLargeFireball";
    }

    public EntityType getType() {
        return EntityType.FIREBALL;
    }
}
