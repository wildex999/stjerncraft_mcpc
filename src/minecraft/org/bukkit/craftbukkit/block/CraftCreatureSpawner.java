package org.bukkit.craftbukkit.block;


import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.EntityType;

public class CraftCreatureSpawner extends CraftBlockState implements CreatureSpawner {
    private final CraftWorld world;
    private final net.minecraft.tileentity.TileEntityMobSpawner spawner;

    public CraftCreatureSpawner(final Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        spawner = (net.minecraft.tileentity.TileEntityMobSpawner) world.getTileEntityAt(getX(), getY(), getZ());
    }

    @Deprecated
    public CreatureType getCreatureType() {
        return CreatureType.fromName(spawner.mobID);
    }

    public EntityType getSpawnedType() {
        return EntityType.fromName(spawner.mobID);
    }

    @Deprecated
    public void setCreatureType(CreatureType creatureType) {
        spawner.mobID = creatureType.getName();
    }

    public void setSpawnedType(EntityType entityType) {
        if (entityType == null || entityType.getName() == null) {
            throw new IllegalArgumentException("Can't spawn EntityType " + entityType + " from mobspawners!");
        }

        spawner.mobID = entityType.getName();
    }

    @Deprecated
    public String getCreatureTypeId() {
        return spawner.mobID;
    }

    @Deprecated
    public void setCreatureTypeId(String creatureName) {
        setCreatureTypeByName(creatureName);
    }

    public String getCreatureTypeName() {
        return spawner.mobID;
    }

    public void setCreatureTypeByName(String creatureType) {
        // Verify input
        EntityType type = EntityType.fromName(creatureType);
        if (type == null) {
            return;
        }
        setSpawnedType(type);
    }

    public int getDelay() {
        return spawner.delay;
    }

    public void setDelay(int delay) {
        spawner.delay = delay;
    }

}
