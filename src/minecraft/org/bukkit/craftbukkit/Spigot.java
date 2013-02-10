package org.bukkit.craftbukkit;

import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class Spigot {
    public static void initialize(CraftServer server, SimpleCommandMap commandMap, YamlConfiguration configuration) {
        commandMap.register("bukkit", new org.bukkit.craftbukkit.command.RestartCommand("restart"));

        org.bukkit.craftbukkit.util.WatchdogThread.startThread(configuration.getInt("settings.timeout-time", 180), configuration.getBoolean("settings.restart-on-crash", false));

        server.whitelistMessage = configuration.getString("settings.whitelist-message", server.whitelistMessage);
        server.stopMessage = configuration.getString("settings.stop-message", server.stopMessage);
        server.logCommands = configuration.getBoolean("settings.log-commands", true);
        server.ipFilter = configuration.getBoolean("settings.filter-unsafe-ips", false);
        server.commandComplete = configuration.getBoolean("settings.command-complete", true);
        server.spamGuardExclusions = configuration.getStringList("settings.spam-exclusions");

        if (server.chunkGCPeriod == 0) {
            server.getLogger().severe("[Spigot] You should not disable chunk-gc. Resetting period-in-ticks to 600 ticks.");
            server.chunkGCPeriod = 600;
        }
    }
}
