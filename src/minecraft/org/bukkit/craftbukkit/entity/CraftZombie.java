package org.bukkit.craftbukkit.entity;


import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;

public class CraftZombie extends CraftMonster implements Zombie {

    public CraftZombie(CraftServer server, net.minecraft.entity.monster.EntityZombie/*was:EntityZombie*/ entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.monster.EntityZombie/*was:EntityZombie*/ getHandle() {
        return (net.minecraft.entity.monster.EntityZombie/*was:EntityZombie*/) entity;
    }

    @Override
    public String toString() {
        return "CraftZombie";
    }

    public EntityType getType() {
        return EntityType.ZOMBIE;
    }

    public boolean isBaby() {
        return getHandle().isChild/*was:isBaby*/();
    }

    public void setBaby(boolean flag) {
        getHandle().setChild/*was:setBaby*/(flag);
    }

    public boolean isVillager() {
        return getHandle().isVillager/*was:isVillager*/();
    }

    public void setVillager(boolean flag) {
        getHandle().setVillager/*was:setVillager*/(flag);
    }
}
