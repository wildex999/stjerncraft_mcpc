package net.minecraft.world;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.ISaveHandler;

public class WorldServerMulti extends WorldServer
{
    // CraftBukkit start - Changed signature
    public WorldServerMulti(MinecraftServer minecraftserver, ISaveHandler idatamanager, String s, int i, WorldSettings worldsettings, WorldServer worldserver, Profiler methodprofiler, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen)
    {
        super(minecraftserver, idatamanager, s, i, worldsettings, methodprofiler, env, gen);
        // CraftBukkit end
        this.mapStorage = worldserver.mapStorage;
        // this.worldData = new SecondaryWorldData(worldserver.getWorldData()); // CraftBukkit - use unique worlddata
    }

    // MCPC+ start - vanilla incompatibility
    public WorldServerMulti(MinecraftServer par1MinecraftServer, ISaveHandler par2ISaveHandler, String par3Str, int par4, WorldSettings par5WorldSettings, WorldServer par6WorldServer, Profiler par7Profiler)
    {
        super(par1MinecraftServer, par2ISaveHandler, par3Str, par4, par5WorldSettings, par7Profiler);
        this.mapStorage = par6WorldServer.mapStorage;
        this.worldInfo = new DerivedWorldInfo(par6WorldServer.getWorldInfo());
    }
    // MCPC+ end
}
