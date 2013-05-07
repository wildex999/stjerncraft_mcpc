package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;

public class CraftCustomEntity extends CraftEntity {

    public CraftCustomEntity(CraftServer server, net.minecraft.entity.Entity entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.Entity getHandle() {
        return (net.minecraft.entity.Entity) entity;
    }

    @Override
    public String toString() {
        return "CustomEntity";
    }

    public EntityType getType() {
        return EntityType.UNKNOWN;
    }
}