package net.minecraft.server.dedicated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.minecraft.server.MinecraftServer;
import static org.bukkit.craftbukkit.Main.*; // CraftBukkit

class DedicatedServerCommandThread extends Thread
{
    final DedicatedServer server;

    DedicatedServerCommandThread(DedicatedServer par1DedicatedServer)
    {
        this.server = par1DedicatedServer;
    }

    public void run()
    {
        // CraftBukkit start
        if (!useConsole)
        {
            return;
        }

        // CraftBukkit end
        jline.console.ConsoleReader var1 = this.server.reader; // CraftBukkit
        String var2;

        try
        {
            // CraftBukkit start - JLine disabling compatibility
            while (!this.server.isServerStopped() && this.server.isServerRunning())
            {
                if (useJline)
                {
                    var2 = var1.readLine(">", null);
                }
                else
                {
                    var2 = var1.readLine();
                }

                if (var2 != null)
                {
                    this.server.addPendingCommand(var2, this.server);
                }

                // CraftBukkit end
            }
        }
        catch (IOException var4)
        {
            // CraftBukkit
            MinecraftServer.logger.log(java.util.logging.Level.SEVERE, null, var4);
        }
    }
}
