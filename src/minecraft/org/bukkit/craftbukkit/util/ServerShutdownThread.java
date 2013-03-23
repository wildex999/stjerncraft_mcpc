package org.bukkit.craftbukkit.util;


public class ServerShutdownThread extends Thread {
    private final net.minecraft.server.MinecraftServer/*was:MinecraftServer*/ server;

    public ServerShutdownThread(net.minecraft.server.MinecraftServer/*was:MinecraftServer*/ server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            server.stopServer/*was:stop*/();
        } finally {
            try {
                server.reader.getTerminal().restore();
            } catch (Exception e) {
            }
        }
    }
}
