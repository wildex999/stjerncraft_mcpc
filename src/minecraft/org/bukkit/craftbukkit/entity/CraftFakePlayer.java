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
        String key;

        if (modFakePlayer != null)
        {
            // Fake player is configured via its class name
            key = modFakePlayer.getClass().getName().replace('.', '/');
        }
        else
        {
            key = "default";
        }


        if (!fakePlayers.containsKey(key))
        {
            String defaultName;

            if (modFakePlayer == null)
            {
                // Global fake player name, if mod cannot be identified
                defaultName = "[FakePlayer]";
            } else {
                // Default to either the mod's fake player username, or class name if unspecified
                if (modFakePlayer.username != null && !modFakePlayer.username.equals("")) {
                    defaultName = "[" + modFakePlayer.username + "]";
                } else {
                    defaultName = "[" + key + "]";
                }
            }

            // Use custom name defined by administrator, if any
            String username = ((CraftServer)Bukkit.getServer()).configuration.getString("mcpc.fake-players." + key + ".username", defaultName);

            System.out.println("[FakePlayer] Initializing fake player for " + key + ": " + username);
            EntityPlayerMP fakePlayer = new EntityPlayerMP(FMLCommonHandler.instance().getMinecraftServerInstance(), world,
                    username, new ItemInWorldManager(world));

            boolean doLogin = ((CraftServer)Bukkit.getServer()).configuration.getBoolean("mcpc.fake-players." + key + ".do-login", false);
            if (doLogin)
            {
                PlayerLoginEvent ple = new PlayerLoginEvent(fakePlayer.getBukkitEntity(), "", null);
                world.getServer().getPluginManager().callEvent(ple);
                if (ple.getResult() != PlayerLoginEvent.Result.ALLOWED)
                {
                    System.err.println("[FakePlayer] Warning: Login event was disallowed for "+key+" for "+username+". Ignoring, but this may cause confused plugins.");
                }

                PlayerJoinEvent pje = new PlayerJoinEvent(fakePlayer.getBukkitEntity(), "");
                world.getServer().getPluginManager().callEvent(pje);
            }

            fakePlayers.put(key, fakePlayer);
        }

        return fakePlayers.get(key);
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
            return player.getBukkitEntity();
        }
        return null;
    }

    public static CraftPlayer getBukkitEntity(World world)
    {
        return getBukkitEntity(world, null);
    }
}
