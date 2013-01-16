package org.bukkit.craftbukkit.block;

import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftInventoryBrewer;
import org.bukkit.inventory.BrewerInventory;

public class CraftBrewingStand extends CraftBlockState implements BrewingStand {
    private final CraftWorld world;
    private final net.minecraft.tileentity.TileEntityBrewingStand/*was:TileEntityBrewingStand*/ brewingStand;

    public CraftBrewingStand(Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        brewingStand = (net.minecraft.tileentity.TileEntityBrewingStand/*was:TileEntityBrewingStand*/) world.getTileEntityAt(getX(), getY(), getZ());
    }

    public BrewerInventory getInventory() {
        return new CraftInventoryBrewer(brewingStand);
    }

    @Override
    public boolean update(boolean force) {
        boolean result = super.update(force);

        if (result) {
            brewingStand.onInventoryChanged/*was:update*/();
        }

        return result;
    }

    public int getBrewingTime() {
        return brewingStand.brewTime/*was:brewTime*/;
    }

    public void setBrewingTime(int brewTime) {
        brewingStand.brewTime/*was:brewTime*/ = brewTime;
    }
}
