package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Flying;

public class CraftFlying extends CraftLivingEntity implements Flying {

    public CraftFlying(CraftServer server, net.minecraft.entity.EntityFlying/*was:EntityFlying*/ entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.EntityFlying/*was:EntityFlying*/ getHandle() {
        return (net.minecraft.entity.EntityFlying/*was:EntityFlying*/) entity;
    }

    @Override
    public String toString() {
        return "CraftFlying";
    }
}
