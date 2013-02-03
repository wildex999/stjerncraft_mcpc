package org.bukkit.craftbukkit;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import org.bukkit.Location;
import org.bukkit.TravelAgent;

public class CraftTravelAgent extends Teleporter implements TravelAgent {

    public static TravelAgent DEFAULT = null;

    private int searchRadius = 128;
    private int creationRadius = 16;
    private boolean canCreatePortal = true;

    public CraftTravelAgent(WorldServer worldserver) {
        super(worldserver);
        if (DEFAULT == null && worldserver.dimension == 0) {
            DEFAULT = (TravelAgent) worldserver.func_85176_s/*was:s*/();
        }
    }

    public Location findOrCreate(Location target) {
        WorldServer worldServer = ((CraftWorld) target.getWorld()).getHandle();
        boolean before = worldServer.theChunkProviderServer.loadChunkOnProvideRequest;
        worldServer.theChunkProviderServer.loadChunkOnProvideRequest = true;

        Location found = this.findPortal(target);
        if (found == null) {
            if (this.getCanCreatePortal() && this.createPortal(target)) {
                found = this.findPortal(target);
            } else {
                found = target; // fallback to original if unable to find or create
            }
        }

        worldServer.theChunkProviderServer.loadChunkOnProvideRequest = before;
        return found;
    }

    public Location findPortal(Location location) {
        Teleporter pta = ((CraftWorld) location.getWorld()).getHandle().func_85176_s();
        ChunkCoordinates found = pta.findPortal(location.getX(), location.getY(), location.getZ(), this.getSearchRadius());
        return found != null ? new Location(location.getWorld(), found.posX, found.posY, found.posZ, location.getYaw(), location.getPitch()) : null;
    }

    public boolean createPortal(Location location) {
        Teleporter pta = ((CraftWorld) location.getWorld()).getHandle().func_85176_s();
        return pta.createPortal(location.getX(), location.getY(), location.getZ(), this.getCreationRadius());
    }

    public TravelAgent setSearchRadius(int radius) {
        this.searchRadius = radius;
        return this;
    }

    public int getSearchRadius() {
        return this.searchRadius;
    }

    public TravelAgent setCreationRadius(int radius) {
        this.creationRadius = radius < 2 ? 0 : radius;
        return this;
    }

    public int getCreationRadius() {
        return this.creationRadius;
    }

    public boolean getCanCreatePortal() {
        return this.canCreatePortal;
    }

    public void setCanCreatePortal(boolean create) {
        this.canCreatePortal = create;
    }
}