package org.bukkit.craftbukkit.block;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.craftbukkit.CraftWorld;


public class CraftJukebox extends CraftBlockState implements Jukebox {
    private final CraftWorld world;
    private final net.minecraft.block.TileEntityRecordPlayer/*was:TileEntityRecordPlayer*/ jukebox;

    public CraftJukebox(final Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        jukebox = (net.minecraft.block.TileEntityRecordPlayer/*was:TileEntityRecordPlayer*/) world.getTileEntityAt(getX(), getY(), getZ());
    }

    public Material getPlaying() {
        return Material.getMaterial(jukebox.record/*was:record*/.itemID/*was:id*/);
    }

    public void setPlaying(Material record) {
        if (record == null) {
            record = Material.AIR;
        }
        jukebox.record/*was:record*/ = new net.minecraft.item.ItemStack/*was:ItemStack*/(net.minecraft.item.Item/*was:Item*/.itemsList/*was:byId*/[record.getId()], 1);
        jukebox.onInventoryChanged/*was:update*/();
        if (record == Material.AIR) {
            world.getHandle().setBlockMetadataWithNotify/*was:setData*/(getX(), getY(), getZ(), 0);
        } else {
            world.getHandle().setBlockMetadataWithNotify/*was:setData*/(getX(), getY(), getZ(), 1);
        }
        world.playEffect(getLocation(), Effect.RECORD_PLAY, record.getId());
    }

    public boolean isPlaying() {
        return getRawData() == 1;
    }

    public boolean eject() {
        boolean result = isPlaying();
        ((net.minecraft.block.BlockJukeBox/*was:BlockJukeBox*/) net.minecraft.block.Block/*was:Block*/.jukebox/*was:JUKEBOX*/).ejectRecord/*was:dropRecord*/(world.getHandle(), getX(), getY(), getZ());
        return result;
    }
}
