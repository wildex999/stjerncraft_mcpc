package org.bukkit.craftbukkit;

import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class Spigot {
    public static void initialize(CraftServer server, SimpleCommandMap commandMap, YamlConfiguration configuration) {
        if (configuration.getBoolean("settings.restart-command", true)) {
            commandMap.register("bukkit", new org.bukkit.craftbukkit.command.RestartCommand("restart"));
        }

        if (configuration.getBoolean("settings.tps-command", true)) {
            commandMap.register("bukkit", new org.bukkit.craftbukkit.command.TicksPerSecondCommand("tps"));
        }

        int timeout = configuration.getInt("settings.timeout-time", 300);
        if (timeout == 180) {
            timeout = 300;
            server.getLogger().info("Migrating to new timeout time of 300");
            configuration.set("settings.timeout-time", timeout);
            server.saveConfig();
        }
        org.bukkit.craftbukkit.util.WatchdogThread.startThread(timeout, configuration.getBoolean("settings.restart-on-crash", false));

        server.whitelistMessage = configuration.getString("settings.whitelist-message", server.whitelistMessage);
        server.stopMessage = configuration.getString("settings.stop-message", server.stopMessage);
        server.logCommands = configuration.getBoolean("settings.log-commands", true);
        server.ipFilter = configuration.getBoolean("settings.filter-unsafe-ips", false);
        server.commandComplete = configuration.getBoolean("settings.command-complete", true);
        server.spamGuardExclusions = configuration.getStringList("settings.spam-exclusions");

        server.orebfuscatorEnabled = configuration.getBoolean("orebfuscator.enable", false);
        server.orebfuscatorUpdateRadius = configuration.getInt("orebfuscator.update-radius", 2);
        server.orebfuscatorDisabledWorlds = configuration.getStringList("orebfuscator.disabled-worlds");

        if (server.chunkGCPeriod == 0) {
            server.getLogger().severe("[Spigot] You should not disable chunk-gc. Resetting period-in-ticks to 600 ticks.");
            server.chunkGCPeriod = 600;
        }
    }
}
