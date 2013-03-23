package org.bukkit.craftbukkit.block;

import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftInventoryFurnace;
import org.bukkit.inventory.FurnaceInventory;

public class CraftFurnace extends CraftBlockState implements Furnace {
    private final CraftWorld world;
    private final net.minecraft.tileentity.TileEntityFurnace furnace;

    public CraftFurnace(final Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        furnace = (net.minecraft.tileentity.TileEntityFurnace) world.getTileEntityAt(getX(), getY(), getZ());
    }

    public FurnaceInventory getInventory() {
        return new CraftInventoryFurnace(furnace);
    }

    @Override
    public boolean update(boolean force) {
        boolean result = super.update(force);

        if (result) {
            furnace.onInventoryChanged();
        }

        return result;
    }

    public short getBurnTime() {
        return (short) furnace.furnaceBurnTime;
    }

    public void setBurnTime(short burnTime) {
        furnace.furnaceBurnTime = burnTime;
    }

    public short getCookTime() {
        return (short) furnace.furnaceCookTime;
    }

    public void setCookTime(short cookTime) {
        furnace.furnaceCookTime = cookTime;
    }
}
