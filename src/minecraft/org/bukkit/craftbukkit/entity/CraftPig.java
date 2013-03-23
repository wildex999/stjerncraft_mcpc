package org.bukkit.craftbukkit.entity;


import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;

public class CraftPig extends CraftAnimals implements Pig {
    public CraftPig(CraftServer server, net.minecraft.entity.passive.EntityPig/*was:EntityPig*/ entity) {
        super(server, entity);
    }

    public boolean hasSaddle() {
        return getHandle().getSaddled/*was:hasSaddle*/();
    }

    public void setSaddle(boolean saddled) {
        getHandle().setSaddled/*was:setSaddle*/(saddled);
    }

    public net.minecraft.entity.passive.EntityPig/*was:EntityPig*/ getHandle() {
        return (net.minecraft.entity.passive.EntityPig/*was:EntityPig*/) entity;
    }

    @Override
    public String toString() {
        return "CraftPig";
    }

    public EntityType getType() {
        return EntityType.PIG;
    }
}
