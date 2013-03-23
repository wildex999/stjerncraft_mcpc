package net.minecraft.world;

import net.minecraft.logging.ILogAgent;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.ISaveHandler;
public class WorldServerMulti extends WorldServer
{
    // CraftBukkit start - Changed signature
    public WorldServerMulti(MinecraftServer minecraftserver, ISaveHandler isavehandler, String s, int i, WorldSettings worldsettings, WorldServer worldserver, Profiler profiler, ILogAgent ilogagent, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen)
    {
        super(minecraftserver, isavehandler, s, i, worldsettings, profiler, ilogagent, env, gen);
        // CraftBukkit end
        this.mapStorage = worldserver.mapStorage;
        this.worldScoreboard = worldserver.getScoreboard();
        // this.worldData = new SecondaryWorldData(worldserver.getWorldData()); // CraftBukkit - use unique worlddata
    }

    // protected void a() {} // CraftBukkit - save world data!

    // MCPC+ start - vanilla compatibility
    public WorldServerMulti(MinecraftServer minecraftserver, ISaveHandler isavehandler, String s, int i, WorldSettings worldsettings, WorldServer worldserver, Profiler profiler, ILogAgent ilogagent) {
        this(minecraftserver, isavehandler, s, i, worldsettings, worldserver, profiler, ilogagent, null, null);
    }
    // MCPC+ end
}
