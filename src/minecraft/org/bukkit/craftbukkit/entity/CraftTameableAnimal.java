package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Tameable;

public class CraftTameableAnimal extends CraftAnimals implements Tameable, Creature {
    public CraftTameableAnimal(CraftServer server, net.minecraft.entity.passive.EntityTameable/*was:EntityTameableAnimal*/ entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.entity.passive.EntityTameable/*was:EntityTameableAnimal*/ getHandle() {
        return (net.minecraft.entity.passive.EntityTameable/*was:EntityTameableAnimal*/)super.getHandle();
    }

    public AnimalTamer getOwner() {
        if (("").equals(getOwnerName())) return null;

        AnimalTamer owner = getServer().getPlayerExact(getOwnerName());
        if (owner == null) {
            owner = getServer().getOfflinePlayer(getOwnerName());
        }

        return owner;
    }

    public String getOwnerName() {
        return getHandle().getOwnerName/*was:getOwnerName*/();
    }

    public boolean isTamed() {
        return getHandle().isTamed/*was:isTamed*/();
    }

    public void setOwner(AnimalTamer tamer) {
        if (tamer != null) {
            setTamed(true);
            getHandle().setPathToEntity/*was:setPathEntity*/(null);
            setOwnerName(tamer.getName());
        } else {
            setTamed(false);
            setOwnerName("");
        }
    }

    public void setOwnerName(String ownerName) {
        getHandle().setOwner/*was:setOwnerName*/(ownerName == null ? "" : ownerName);
    }

    public void setTamed(boolean tame) {
        getHandle().setTamed/*was:setTamed*/(tame);
        if (!tame) {
            setOwnerName("");
        }
    }

    public boolean isSitting() {
        return getHandle().isSitting/*was:isSitting*/();
    }

    public void setSitting(boolean sitting) {
        getHandle().func_70907_r/*was:getGoalSit*/().setSitting/*was:setSitting*/(sitting);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{owner=" + getOwner() + ",tamed=" + isTamed() + "}";
    }
}
