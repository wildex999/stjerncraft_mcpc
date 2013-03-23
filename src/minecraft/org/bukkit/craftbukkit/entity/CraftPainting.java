package org.bukkit.craftbukkit.entity;


import org.bukkit.Art;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftArt;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;

public class CraftPainting extends CraftHanging implements Painting {

    public CraftPainting(CraftServer server, net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/ entity) {
        super(server, entity);
    }

    public Art getArt() {
        net.minecraft.util.EnumArt/*was:EnumArt*/ art = getHandle().art/*was:art*/;
        return CraftArt.NotchToBukkit(art);
    }

    public boolean setArt(Art art) {
        return setArt(art, false);
    }

    public boolean setArt(Art art, boolean force) {
        net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/ painting = this.getHandle();
        net.minecraft.util.EnumArt/*was:EnumArt*/ oldArt = painting.art/*was:art*/;
        painting.art/*was:art*/ = CraftArt.BukkitToNotch(art);
        painting.setDirection/*was:setDirection*/(painting.hangingDirection/*was:direction*/);
        if (!force && !painting.onValidSurface/*was:survives*/()) {
            // Revert painting since it doesn't fit
            painting.art/*was:art*/ = oldArt;
            painting.setDirection/*was:setDirection*/(painting.hangingDirection/*was:direction*/);
            return false;
        }
        this.update();
        return true;
    }

    public boolean setFacingDirection(BlockFace face, boolean force) {
        if (super.setFacingDirection(face, force)) {
            update();
            return true;
        }

        return false;
    }

    private void update() {
        net.minecraft.world.WorldServer/*was:WorldServer*/ world = ((CraftWorld) getWorld()).getHandle();
        net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/ painting = new net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/(world);
        painting.xPosition/*was:x*/ = getHandle().xPosition/*was:x*/;
        painting.yPosition/*was:y*/ = getHandle().yPosition/*was:y*/;
        painting.zPosition/*was:z*/ = getHandle().zPosition/*was:z*/;
        painting.art/*was:art*/ = getHandle().art/*was:art*/;
        painting.setDirection/*was:setDirection*/(getHandle().hangingDirection/*was:direction*/);
        getHandle().setDead/*was:die*/();
        getHandle().velocityChanged/*was:velocityChanged*/ = true; // because this occurs when the painting is broken, so it might be important
        world.spawnEntityInWorld/*was:addEntity*/(painting);
        this.entity = painting;
    }

    @Override
    public net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/ getHandle() {
        return (net.minecraft.entity.item.EntityPainting/*was:EntityPainting*/) entity;
    }

    @Override
    public String toString() {
        return "CraftPainting{art=" + getArt() + "}";
    }

    public EntityType getType() {
        return EntityType.PAINTING;
    }
}
