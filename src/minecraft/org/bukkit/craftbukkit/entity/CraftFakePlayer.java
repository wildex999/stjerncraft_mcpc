package org.bukkit.craftbukkit.entity;

import cpw.mods.fml.common.FMLCommonHandler;
import java.io.PrintStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.world.World;

import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.PluginManager;

public class CraftFakePlayer extends CraftPlayer
{
  public CraftFakePlayer(CraftServer server, EntityPlayerMP entity) {
        super(server, entity);
        // TODO Auto-generated constructor stub
    }

private static Method method = Method.FAKEPLAYER;
  private static EntityPlayerMP fakePlayer = null;
  public static String name = "[FakePlayer]";
  public static boolean doLogin = false;

  public static void setMethod(String value) {
    if (value.equalsIgnoreCase("null"))
      method = Method.NULL;
    else if (value.equalsIgnoreCase("fakeplayer"))
      method = Method.FAKEPLAYER;
    else
      System.err.println("Unknown blocks.placedby type '" + value + "'");
  }

  public static CraftFakePlayer get(World world)
  {
    switch (method.ordinal() + 1) {
    case 1:
      return null;
    case 2:
      if (fakePlayer == null) {
        fakePlayer = new EntityPlayerMP(FMLCommonHandler.instance().getMinecraftServerInstance(), world, 
          name, new ItemInWorldManager(world));

        if (doLogin) {
          PlayerLoginEvent ple = new PlayerLoginEvent(fakePlayer.getBukkitEntity());
          world.getServer().getPluginManager().callEvent(ple);
          if (ple.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            System.err.println("[IndustrialCraft] FakePlayer login event was disallowed. Ignoring, but this may cause confused plugins.");
          }

          PlayerJoinEvent pje = new PlayerJoinEvent(fakePlayer.getBukkitEntity(), "");
          world.getServer().getPluginManager().callEvent(pje);
        }
      }
      return new CraftFakePlayer(FMLCommonHandler.instance().getMinecraftServerInstance().server, fakePlayer);
    }
    return null;
  }

  static enum Method
  {
    NULL, FAKEPLAYER;
  }
}