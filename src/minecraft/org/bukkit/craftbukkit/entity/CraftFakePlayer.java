package org.bukkit.craftbukkit.entity;

import cpw.mods.fml.common.FMLCommonHandler;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.world.World;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.PluginManager;

public class CraftFakePlayer extends CraftPlayer
{
    public CraftFakePlayer(CraftServer server, EntityPlayerMP entity)
    {
        super(server, entity);
    }

    private static Map<String, EntityPlayerMP> fakePlayers = new HashMap<String, EntityPlayerMP>();
    public static String defaultName = "[FakePlayer]";
    public static boolean doLogin = false;

    /**
     * Get a fake player, creating if necessary
     * @param world The world to login the player from. This is only used when the player is first created;
     *              fake players can cross worlds without issue.
     * @param modFakePlayer The mod's fake player instance, if available. Note this is an EntityPlayer, which
     *                      is insufficient for Bukkit's purposes, hence can be 'converted' to an EntityPlayerMP
     *                      through CraftFakePlayer. The class name is used for naming the new fake player.
     * @return
     */
    public static EntityPlayerMP get(World world, EntityPlayer modFakePlayer)
    {
        String name = defaultName;
        String className = null;

        if (modFakePlayer != null)
        {
            className = modFakePlayer.getClass().getName().replace('.', '/');
            String modDefaultName;

            // Default to either the mod's fake player username, or class name if unspecified
            if (modFakePlayer.username != null && !modFakePlayer.username.equals("")) {
                modDefaultName = "[" + modFakePlayer.username + "]";
            } else {
                modDefaultName = "[" + className + "]";
            }

            // Use custom name defined by administrator
            name = ((CraftServer)Bukkit.getServer()).configuration.getString("mcpc.fake-players." + className, modDefaultName);
        }

        if (!fakePlayers.containsKey(name))
        {
            System.out.println("[FakePlayer] Initializing fake player for " + className + ": " + name);
            EntityPlayerMP fakePlayer = new EntityPlayerMP(FMLCommonHandler.instance().getMinecraftServerInstance(), world,
                    name, new ItemInWorldManager(world));

            if (doLogin)
            {
                PlayerLoginEvent ple = new PlayerLoginEvent((CraftPlayer)fakePlayer.getBukkitEntity()); // TODO: update from deprecation
                world.getServer().getPluginManager().callEvent(ple);
                if (ple.getResult() != PlayerLoginEvent.Result.ALLOWED)
                {
                    System.err.println("[FakePlayer] Warning: Login event was disallowed for "+name+". Ignoring, but this may cause confused plugins.");
                }

                PlayerJoinEvent pje = new PlayerJoinEvent((CraftPlayer)fakePlayer.getBukkitEntity(), "");
                world.getServer().getPluginManager().callEvent(pje);
            }

            fakePlayers.put(name, fakePlayer);
        }

        return fakePlayers.get(name);
    }

    public static EntityPlayerMP get(World world)
    {
        return get(world, null);
    }

    public static CraftPlayer getBukkitEntity(World world, EntityPlayer modFakePlayer)
    {
        EntityPlayerMP player = get(world, modFakePlayer);
        if (player != null)
        {
            return (CraftPlayer)player.getBukkitEntity();
        }
        return null;
    }

    public static CraftPlayer getBukkitEntity(World world)
    {
        return getBukkitEntity(world, null);
    }
}
