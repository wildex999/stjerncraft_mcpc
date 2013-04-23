package org.spigotmc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;

public class MultiplexingServerConnection extends net.minecraft.network.NetworkListenThread {

    private static final boolean NETTY_DISABLED = Boolean.getBoolean("org.spigotmc.netty.disabled");
    private final Collection<net.minecraft.network.NetworkListenThread> children = new HashSet<net.minecraft.network.NetworkListenThread>();
    private final List<net.minecraft.network.NetLoginHandler> pending = Collections.synchronizedList(new ArrayList<net.minecraft.network.NetLoginHandler>());
    private final HashMap<InetAddress, Long> throttle = new HashMap<InetAddress, Long>();

    public MultiplexingServerConnection(net.minecraft.server.MinecraftServer ms) throws IOException { // MCPC+
        super(ms);

        // Add primary connection
        start(ms.server.getIp(), ms.server.getPort());
        // Add all other connections
        for (InetSocketAddress address : ms.server.getSecondaryHosts()) {
            start(address.getAddress().getHostAddress(), address.getPort());
        }
    }

    private void start(String ipAddress, int port) {
        try {
            // Calculate address, can't use isEmpty due to Java 5
            InetAddress socketAddress = (ipAddress.length() == 0) ? null : InetAddress.getByName(ipAddress);
            // Say hello to the log
            getServer().getLogAgent().logInfo("Starting listener #" + children.size() + " on " + (socketAddress == null ? "*" : ipAddress) + ":" + port);
            // Start connection: Netty / non Netty
            net.minecraft.network.NetworkListenThread listener = (NETTY_DISABLED) ? new net.minecraft.server.dedicated.DedicatedServerListenThread(getServer(), socketAddress, port) : new org.spigotmc.netty.NettyServerConnection(getServer(), socketAddress, port);
            // Register with other connections
            children.add(listener);
            // Gotta catch em all
        } catch (Throwable t) {
            // Just print some info to the log
            t.printStackTrace();
            getServer().getLogAgent().logWarning("**** FAILED TO BIND TO PORT!");
            getServer().getLogAgent().logWarningException("The exception was: {0}", t);
            getServer().getLogAgent().logWarning("Perhaps a server is already running on that port?");
        }
    }

    /**
     * close.
     */
    @Override
    public void stopListening() {
        for (net.minecraft.network.NetworkListenThread child : children) {
            child.stopListening();
        }
    }

    /**
     * Pulse. This method pulses all connections causing them to update. It is
     * called from the main server thread a few times a tick.
     */
    @Override

    /**
     * processes packets and pending connections
     */
    public void networkTick() {
        super.networkTick(); // pulse PlayerConnections
        for (int i = 0; i < pending.size(); ++i) {
            net.minecraft.network.NetLoginHandler connection = pending.get(i);

            try {
                connection.tryLogin();
            } catch (Exception ex) {
                connection.raiseErrorAndDisconnect("Internal server error");
                Bukkit.getServer().getLogger().log(Level.WARNING, "Failed to handle packet: " + ex, ex);
            }

            if (connection.connectionComplete) {
                pending.remove(i--);
            }
        }
    }

    /**
     * Remove the user from connection throttle. This should fix the server ping
     * bugs.
     *
     * @param address the address to remove
     */
    public void unThrottle(InetAddress address) {
        if (address != null) {
            synchronized (throttle) {
                throttle.remove(address);
            }
        }
    }

    /**
     * Add a connection to the throttle list.
     *
     * @param address
     * @return Whether they must be disconnected
     */
    public boolean throttle(InetAddress address) {
        long currentTime = System.currentTimeMillis();
        synchronized (throttle) {
            Long value = throttle.get(address);
            if (value != null && !address.isLoopbackAddress() && currentTime - value < getServer().server.getConnectionThrottle()) {
                throttle.put(address, currentTime);
                return true;
            }

            throttle.put(address, currentTime);
        }
        return false;
    }

    public void register(net.minecraft.network.NetLoginHandler conn) {
        pending.add(conn);
    }
}
