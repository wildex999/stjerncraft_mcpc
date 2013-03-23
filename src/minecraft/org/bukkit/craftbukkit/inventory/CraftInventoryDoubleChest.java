package org.bukkit.craftbukkit.inventory;

import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class CraftInventoryDoubleChest extends CraftInventory implements DoubleChestInventory {
    private final CraftInventory left;
    private final CraftInventory right;

    public CraftInventoryDoubleChest(CraftInventory left, CraftInventory right) {
        super(new net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/("Large chest", left.getInventory(), right.getInventory()));
        this.left = left;
        this.right = right;
    }

    public CraftInventoryDoubleChest(net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/ largeChest) {
        super(largeChest);
        if (largeChest.upperChest/*was:left*/ instanceof net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/) {
            left = new CraftInventoryDoubleChest((net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/) largeChest.upperChest/*was:left*/);
        } else {
            left = new CraftInventory(largeChest.upperChest/*was:left*/);
        }
        if (largeChest.lowerChest/*was:right*/ instanceof net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/) {
            right = new CraftInventoryDoubleChest((net.minecraft.inventory.InventoryLargeChest/*was:InventoryLargeChest*/) largeChest.lowerChest/*was:right*/);
        } else {
            right = new CraftInventory(largeChest.lowerChest/*was:right*/);
        }
    }

    public Inventory getLeftSide() {
        return left;
    }

    public Inventory getRightSide() {
        return right;
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (getInventory().getContents().length < items.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + getInventory().getContents().length + " or less");
        }
        ItemStack[] leftItems = new ItemStack[left.getSize()], rightItems = new ItemStack[right.getSize()];
        System.arraycopy(items, 0, leftItems, 0, Math.min(left.getSize(),items.length));
        left.setContents(leftItems);
        if (items.length >= left.getSize()) {
            System.arraycopy(items, left.getSize(), rightItems, 0, Math.min(right.getSize(), items.length - left.getSize()));
            right.setContents(rightItems);
        }
    }

    @Override
    public DoubleChest getHolder() {
        return new DoubleChest(this);
    }
}
