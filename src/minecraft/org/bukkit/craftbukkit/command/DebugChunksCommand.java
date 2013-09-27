package org.bukkit.craftbukkit.command;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class DebugChunksCommand extends Command {

    public static boolean debugChunks = false;
    public static boolean showStackTrace = false;

    public DebugChunksCommand(String name) {
        super(name);
        this.description = "Toggle chunk debugging";
        this.usageMessage = "/debugchunks [on|off|toggletrace]";
        this.setPermission("bukkit.command.debugchunks");
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) return true;
        for(String arg: args) {
            if (arg.toLowerCase().equals("on")) {
                debugChunks = true;
            }
            if (arg.toLowerCase().equals("off")) {
                debugChunks = false;
            }
            if (arg.toLowerCase().equals("toggletrace")) {
                showStackTrace = !showStackTrace;
            }
            if (arg.toLowerCase().equals("dumppersistent")) {
                for(WorldServer world : MinecraftServer.getServer().worlds) {
                    sender.sendMessage(ChatColor.GOLD + "World: " + ChatColor.GREEN + world.getWorld().getName());
                    for(ChunkCoordIntPair chunk : world.getPersistentChunks().keySet()) {
                        sender.sendMessage(ChatColor.GOLD + "Chunk: " + ChatColor.AQUA + chunk.chunkXPos +", "+ chunk.chunkZPos);
                    }
                }
            }
        }
        String chunkMessage = debugChunks ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        String stackMessage = (debugChunks && showStackTrace) ? " with stack traces" : "";
        sender.sendMessage(ChatColor.GOLD + "Chunk debugging is " + chunkMessage + stackMessage);
        return true;
    }
}
