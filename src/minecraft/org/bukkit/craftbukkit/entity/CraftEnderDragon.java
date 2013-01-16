package org.bukkit.craftbukkit.entity;

import guava10.com.google.common.collect.ImmutableSet;
import guava10.com.google.common.collect.ImmutableSet.Builder;

import java.util.Set;


import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;

public class CraftEnderDragon extends CraftComplexLivingEntity implements EnderDragon {
    public CraftEnderDragon(CraftServer server, net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/ entity) {
        super(server, entity);
    }

    public Set<ComplexEntityPart> getParts() {
        Builder<ComplexEntityPart> builder = ImmutableSet.builder();

        for (net.minecraft.entity.boss.EntityDragonPart/*was:EntityComplexPart*/ part : getHandle().dragonPartArray/*was:children*/) {
            builder.add((ComplexEntityPart) part.getBukkitEntity());
        }

        return builder.build();
    }

    @Override
    public net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/ getHandle() {
        return (net.minecraft.entity.boss.EntityDragon/*was:EntityEnderDragon*/) entity;
    }

    @Override
    public String toString() {
        return "CraftEnderDragon";
    }

    public EntityType getType() {
        return EntityType.ENDER_DRAGON;
    }
}
