package net.minecraft.server;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.NetworkListenThread;

public class ServerListenThread extends Thread
{
    private static Logger logger = Logger.getLogger("Minecraft");
    private final List pendingConnections = Collections.synchronizedList(new ArrayList());

    /**
     * This map stores a list of InetAddresses and the last time which they connected at
     */
    private final HashMap recentConnections = new HashMap();
    private int connectionCounter = 0;
    private final ServerSocket myServerSocket;
    private NetworkListenThread myNetworkListenThread;
    private final InetAddress myServerAddress;
    private final int myPort;

    long connectionThrottle; // CraftBukkit

    public ServerListenThread(NetworkListenThread par1NetworkListenThread, InetAddress par2InetAddress, int par3) throws IOException   // CraftBukkit - added throws
    {
        super("Listen thread");
        this.myNetworkListenThread = par1NetworkListenThread;
        this.myPort = par3;
        this.myServerSocket = new ServerSocket(par3, 0, par2InetAddress);
        this.myServerAddress = par2InetAddress == null ? this.myServerSocket.getInetAddress() : par2InetAddress;
        this.myServerSocket.setPerformancePreferences(0, 2, 1);
    }

    public void processPendingConnections()
    {
        List list = this.pendingConnections;

        synchronized (this.pendingConnections)
        {
            for (int i = 0; i < this.pendingConnections.size(); ++i)
            {
                NetLoginHandler netloginhandler = (NetLoginHandler)this.pendingConnections.get(i);

                try
                {
                    netloginhandler.tryLogin();
                }
                catch (Exception exception)
                {
                    netloginhandler.raiseErrorAndDisconnect("Internal server error");
                    FMLLog.log(Level.SEVERE, exception, "Error handling login related packet - connection from %s refused", netloginhandler.getUsernameAndAddress());
                    logger.log(Level.WARNING, "Failed to handle packet for " + netloginhandler.getUsernameAndAddress() + ": " + exception, exception);
                }

                if (netloginhandler.connectionComplete)
                {
                    this.pendingConnections.remove(i--);
                }

                netloginhandler.myTCPConnection.wakeThreads();
            }
        }
    }

    public void run()
    {
        while (this.myNetworkListenThread.isListening)
        {
            try
            {
                Socket socket = this.myServerSocket.accept();
                InetAddress inetaddress = socket.getInetAddress();
                long i = System.currentTimeMillis();
                HashMap hashmap = this.recentConnections;

                // CraftBukkit start
                if (((MinecraftServer) this.myNetworkListenThread.getServer()).server == null)
                {
                    socket.close();
                    continue;
                }

                connectionThrottle = ((MinecraftServer) this.myNetworkListenThread.getServer()).server.getConnectionThrottle();
                // CraftBukkit end

                synchronized (this.recentConnections)
                {
                    if (this.recentConnections.containsKey(inetaddress) && !isLocalHost(inetaddress) && i - ((Long) this.recentConnections.get(inetaddress)).longValue() < connectionThrottle)
                    {
                        this.recentConnections.put(inetaddress, Long.valueOf(i));
                        socket.close();
                        continue;
                    }

                    this.recentConnections.put(inetaddress, Long.valueOf(i));
                }

                NetLoginHandler netloginhandler = new NetLoginHandler(this.myNetworkListenThread.getServer(), socket, "Connection #" + this.connectionCounter++);
                this.addPendingConnection(netloginhandler);
            }
            catch (IOException ioexception)
            {
                logger.warning("DSCT: " + ioexception.getMessage()); // CraftBukkit
            }
        }

        System.out.println("Closing listening thread");
    }

    private void addPendingConnection(NetLoginHandler par1NetLoginHandler)
    {
        if (par1NetLoginHandler == null)
        {
            throw new IllegalArgumentException("Got null pendingconnection!");
        }
        else
        {
            List list = this.pendingConnections;

            synchronized (this.pendingConnections)
            {
                this.pendingConnections.add(par1NetLoginHandler);
            }
        }
    }

    private static boolean isLocalHost(InetAddress par0InetAddress)
    {
        return "127.0.0.1".equals(par0InetAddress.getHostAddress());
    }

    public void func_71769_a(InetAddress par1InetAddress)
    {
        if (par1InetAddress != null)
        {
            HashMap hashmap = this.recentConnections;

            synchronized (this.recentConnections)
            {
                this.recentConnections.remove(par1InetAddress);
            }
        }
    }

    public void func_71768_b()
    {
        try
        {
            this.myServerSocket.close();
        }
        catch (Throwable throwable)
        {
            ;
        }
    }

    @SideOnly(Side.CLIENT)
    public InetAddress getInetAddress()
    {
        return this.myServerAddress;
    }

    @SideOnly(Side.CLIENT)
    public int getMyPort()
    {
        return this.myPort;
    }
}
