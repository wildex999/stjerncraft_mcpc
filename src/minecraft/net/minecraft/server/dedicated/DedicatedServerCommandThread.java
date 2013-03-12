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
        jline.console.ConsoleReader consolereader = this.server.reader; // CraftBukkit
        String s;

        try
        {
            // CraftBukkit start - JLine disabling compatibility
            while (!this.server.isServerStopped() && this.server.isServerRunning())
            {
                if (useJline)
                {
                    s = consolereader.readLine(">", null);
                }
                else
                {
                    s = consolereader.readLine();
                }

                if (s != null)
                {
                    this.server.addPendingCommand(s, this.server);
                }

                // CraftBukkit end
            }
        }
        catch (IOException ioexception)
        {
            // CraftBukkit
            MinecraftServer.logger.log(java.util.logging.Level.SEVERE, null, ioexception);
        }
    }
}
