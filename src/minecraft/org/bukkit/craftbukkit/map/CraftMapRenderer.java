package org.bukkit.craftbukkit.map;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class CraftMapRenderer extends MapRenderer {

    private final net.minecraft.world.storage.MapData/*was:WorldMap*/ worldMap;

    public CraftMapRenderer(CraftMapView mapView, net.minecraft.world.storage.MapData/*was:WorldMap*/ worldMap) {
        super(false);
        this.worldMap = worldMap;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        // Map
        for (int x = 0; x < 128; ++x) {
            for (int y = 0; y < 128; ++y) {
                canvas.setPixel(x, y, worldMap.colors/*was:colors*/[y * 128 + x]);
            }
        }

        // Cursors
        MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        for (Object key : worldMap.playersVisibleOnMap/*was:g*/.keySet()) {
            // If this cursor is for a player check visibility with vanish system
            Player other = Bukkit.getPlayerExact((String) key);
            if (other != null && !player.canSee(other)) {
                continue;
            }

            net.minecraft.world.storage.MapCoord/*was:WorldMapDecoration*/ decoration = (net.minecraft.world.storage.MapCoord/*was:WorldMapDecoration*/) worldMap.playersVisibleOnMap/*was:g*/.get(key);
            cursors.addCursor(decoration.centerX/*was:locX*/, decoration.centerZ/*was:locY*/, (byte) (decoration.iconRotation/*was:rotation*/ & 15), (byte) (decoration.iconSize/*was:type*/));
        }
    }

}
