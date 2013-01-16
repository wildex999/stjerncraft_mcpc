package org.bukkit.craftbukkit.entity;


import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.CraftServer;

public class CraftItem extends CraftEntity implements Item {
    private final net.minecraft.entity.item.EntityItem/*was:EntityItem*/ item;

    public CraftItem(CraftServer server, net.minecraft.entity.Entity/*was:Entity*/ entity, net.minecraft.entity.item.EntityItem/*was:EntityItem*/ item) {
        super(server, entity);
        this.item = item;
    }

    public CraftItem(CraftServer server, net.minecraft.entity.item.EntityItem/*was:EntityItem*/ entity) {
        this(server, entity, entity);
    }

    public ItemStack getItemStack() {
        return CraftItemStack.asCraftMirror(item.func_92014_d/*was:getItemStack*/());
    }

    public void setItemStack(ItemStack stack) {
        item.func_92013_a/*was:setItemStack*/(CraftItemStack.asNMSCopy(stack));
    }

    public int getPickupDelay() {
        return item.delayBeforeCanPickup/*was:pickupDelay*/;
    }

    public void setPickupDelay(int delay) {
        item.delayBeforeCanPickup/*was:pickupDelay*/ = delay;
    }

    @Override
    public String toString() {
        return "CraftItem";
    }

    public EntityType getType() {
        return EntityType.DROPPED_ITEM;
    }
}
