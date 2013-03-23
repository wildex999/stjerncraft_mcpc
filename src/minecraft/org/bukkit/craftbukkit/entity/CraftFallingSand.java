package org.bukkit.craftbukkit.entity;


import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingSand;

public class CraftFallingSand extends CraftEntity implements FallingSand {

    public CraftFallingSand(CraftServer server, net.minecraft.entity.item.EntityFallingSand/*was:EntityFallingBlock*/ entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.item.EntityFallingSand/*was:EntityFallingBlock*/ getHandle() {
        return (net.minecraft.entity.item.EntityFallingSand/*was:EntityFallingBlock*/) entity;
    }

    @Override
    public String toString() {
        return "CraftFallingSand";
    }

    public EntityType getType() {
        return EntityType.FALLING_BLOCK;
    }

    public Material getMaterial() {
        return Material.getMaterial(getBlockId());
    }

    public int getBlockId() {
        return getHandle().blockID/*was:id*/;
    }

    public byte getBlockData() {
        return (byte) getHandle().metadata/*was:data*/;
    }

    public boolean getDropItem() {
        return getHandle().shouldDropItem/*was:dropItem*/;
    }

    public void setDropItem(boolean drop) {
        getHandle().shouldDropItem/*was:dropItem*/ = drop;
    }
}
