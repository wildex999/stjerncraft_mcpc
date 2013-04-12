package net.minecraft.network;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.crypto.SecretKey;

import cpw.mods.fml.common.network.FMLNetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet205ClientCommand;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet252SharedKey;
import net.minecraft.network.packet.Packet253ServerAuthData;
import net.minecraft.network.packet.Packet254ServerPing;
import net.minecraft.network.packet.Packet255KickDisconnect;
import net.minecraft.network.packet.Packet2ClientProtocol;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServerListenThread;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.StringUtils;

public class NetLoginHandler extends NetHandler
{
    /** The Random object used to generate serverId hex strings. */
    private static Random rand = new Random();

    /** The 4 byte verify token read from a Packet252SharedKey */
    private byte[] verifyToken;

    /** Reference to the MinecraftServer object. */
    private final MinecraftServer mcServer;
    public final INetworkManager myTCPConnection; // Spigot - TcpConnection -> INetworkManager
    public boolean connectionComplete = false;
    private int connectionTimer = 0;
    public String clientUsername = null;
    private volatile boolean field_72544_i = false;

    /** server ID that is randomly generated by this login handler. */
    private String loginServerId = Long.toString(rand.nextLong(), 16); // CraftBukkit - Security fix
    private boolean field_92079_k = false;

    /** Secret AES key obtained from the client's Packet252SharedKey */
    private SecretKey sharedKey = null;
    public String hostname = ""; // CraftBukkit - add field

    // Spigot start
    public NetLoginHandler(MinecraftServer minecraftserver, org.spigotmc.netty.NettyNetworkManager networkManager)
    {
        this.mcServer = minecraftserver;
        this.myTCPConnection = networkManager;
    }
    // Spigot end

    public NetLoginHandler(MinecraftServer par1MinecraftServer, Socket par2Socket, String par3Str) throws java.io.IOException   // CraftBukkit - throws IOException
    {
        this.mcServer = par1MinecraftServer;
        this.myTCPConnection = new TcpConnection(par1MinecraftServer.getLogAgent(), par2Socket, par3Str, this, par1MinecraftServer.getKeyPair().getPrivate());
        // this.myTCPConnection.field_74468_e = 0; // Spigot
    }

    // CraftBukkit start
    public Socket getSocket()
    {
        // MCPC+ start - bypass inheritance for runtime deobf, see #729
        //return this.myTCPConnection.getSocket();
        if (this.myTCPConnection instanceof TcpConnection)
        {
            return ((TcpConnection) this.myTCPConnection).getSocket();
        }
        else if (this.myTCPConnection instanceof org.spigotmc.netty.NettyNetworkManager)
        {
            return ((org.spigotmc.netty.NettyNetworkManager) this.myTCPConnection).getSocket();
        }
        else
        {
            throw new IllegalArgumentException("Unknown network manager implementation: " + this.myTCPConnection.getClass().getName());
        }
        // MCPC+ end
    }
    // CraftBukkit end

    /**
     * Logs the user in if a login packet is found, otherwise keeps processing network packets unless the timeout has
     * occurred.
     */
    public void tryLogin()
    {
        if (this.field_72544_i)
        {
            this.initializePlayerConnection();
        }

        if (this.connectionTimer++ == 6000)
        {
            this.raiseErrorAndDisconnect("Took too long to log in");
        }
        else
        {
            this.myTCPConnection.processReadPackets();
        }
    }

    public void raiseErrorAndDisconnect(String par1Str)
    {
        try
        {
            this.mcServer.getLogAgent().logInfo("Disconnecting " + this.getUsernameAndAddress() + ": " + par1Str);
            this.myTCPConnection.addToSendQueue(new Packet255KickDisconnect(par1Str));
            this.myTCPConnection.serverShutdown();
            this.connectionComplete = true;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public void handleClientProtocol(Packet2ClientProtocol par1Packet2ClientProtocol)
    {
        // CraftBukkit start
        this.hostname = par1Packet2ClientProtocol.serverHost == null ? "" : par1Packet2ClientProtocol.serverHost + ':' + par1Packet2ClientProtocol.serverPort;
        // CraftBukkit end
        this.clientUsername = par1Packet2ClientProtocol.getUsername();

        if (!this.clientUsername.equals(StringUtils.stripControlCodes(this.clientUsername)))
        {
            this.raiseErrorAndDisconnect("Invalid username!");
        }
        else
        {
            PublicKey publickey = this.mcServer.getKeyPair().getPublic();

            if (par1Packet2ClientProtocol.getProtocolVersion() != 60)
            {
                if (par1Packet2ClientProtocol.getProtocolVersion() > 60)
                {
                    this.raiseErrorAndDisconnect("Outdated server!");
                }
                else
                {
                    this.raiseErrorAndDisconnect("Outdated client!");
                }
            }
            else
            {
                this.loginServerId = this.mcServer.isServerInOnlineMode() ? Long.toString(rand.nextLong(), 16) : "-";
                this.verifyToken = new byte[4];
                rand.nextBytes(this.verifyToken);
                this.myTCPConnection.addToSendQueue(new Packet253ServerAuthData(this.loginServerId, publickey, this.verifyToken));
            }
        }
    }

    public void handleSharedKey(Packet252SharedKey par1Packet252SharedKey)
    {
        PrivateKey privatekey = this.mcServer.getKeyPair().getPrivate();
        this.sharedKey = par1Packet252SharedKey.getSharedKey(privatekey);

        if (!Arrays.equals(this.verifyToken, par1Packet252SharedKey.getVerifyToken(privatekey)))
        {
            this.raiseErrorAndDisconnect("Invalid client reply");
        }

        this.myTCPConnection.addToSendQueue(new Packet252SharedKey());
    }

    public void handleClientCommand(Packet205ClientCommand par1Packet205ClientCommand)
    {
        if (par1Packet205ClientCommand.forceRespawn == 0)
        {
            if (this.mcServer.isServerInOnlineMode())
            {
                if (this.field_92079_k)
                {
                    this.raiseErrorAndDisconnect("Duplicate login");
                    return;
                }

                this.field_92079_k = true;
                (new ThreadLoginVerifier(this, mcServer.server)).start(); // CraftBukkit - add CraftServer
            }
            else
            {
                this.field_72544_i = true;
            }
        }
    }

    public void handleLogin(Packet1Login par1Packet1Login)
    {
        FMLNetworkHandler.handleLoginPacketOnServer(this, par1Packet1Login);
    }

    /**
     * on success the specified username is connected to the minecraftInstance, otherwise they are packet255'd
     */
    public void initializePlayerConnection()
    {
        FMLNetworkHandler.onConnectionReceivedFromClient(this, this.mcServer, this.myTCPConnection.getSocketAddress(), this.clientUsername);
    }

    public void completeConnection(String s)
    {    
        // CraftBukkit start
        EntityPlayerMP entityplayermp = this.mcServer.getConfigurationManager().attemptLogin(this, this.clientUsername, this.hostname);

        if (entityplayermp == null)
        {
            return;
            // CraftBukkit end
        }
        else
        {
            entityplayermp = this.mcServer.getConfigurationManager().processLogin(entityplayermp); // CraftBukkit - this.h -> s // MCPC+ - reuse variable

            if (entityplayermp != null)
            {
                this.mcServer.getConfigurationManager().initializeConnectionToPlayer((INetworkManager) this.myTCPConnection, entityplayermp);
            }
        }

        this.connectionComplete = true;
    }

    public void handleErrorMessage(String par1Str, Object[] par2ArrayOfObj)
    {
        this.mcServer.getLogAgent().logInfo(this.getUsernameAndAddress() + " lost connection");
        this.connectionComplete = true;
    }

    /**
     * Handle a server ping packet.
     */
    public void handleServerPing(Packet254ServerPing par1Packet254ServerPing)
    {
        if (this.getSocket() == null) // MCPC+ - remove myTCPConnection
        {
            return;    // CraftBukkit - fix NPE when a client queries a server that is unable to handle it.
        }

        try
        {
            ServerConfigurationManager serverconfigurationmanager = this.mcServer.getConfigurationManager();
            String s = null;
            // CraftBukkit
            org.bukkit.event.server.ServerListPingEvent pingEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callServerListPingEvent(this.mcServer.server, getSocket().getInetAddress(), this.mcServer.getMOTD(), serverconfigurationmanager.getCurrentPlayerCount(), serverconfigurationmanager.getMaxPlayers());

            if (true || par1Packet254ServerPing.field_82559_a == 1) // Spigot
            {
                // CraftBukkit start - Fix decompile issues, don't create a list from an array
                Object[] list = new Object[] { 1, 60, this.mcServer.getMinecraftVersion(), pingEvent.getMotd(), serverconfigurationmanager.getCurrentPlayerCount(), pingEvent.getMaxPlayers() };

                for (Object object : list)
                {
                    if (s == null)
                    {
                        s = "\u00A7";
                    }
                    else
                    {
                        s = s + "\0";
                    }

                    s += org.apache.commons.lang.StringUtils.replace(object.toString(), "\0", "");
                }

                // CraftBukkit end
            }
            else
            {
                // CraftBukkit
                s = pingEvent.getMotd() + "\u00A7" + serverconfigurationmanager.getCurrentPlayerCount() + "\u00A7" + pingEvent.getMaxPlayers();
            }

            InetAddress inetaddress = null;

            if (this.getSocket() != null) // MCPC+ - remove myTCPConnection
            {
                inetaddress = this.getSocket().getInetAddress(); // MCPC+ - remove myTCPConnection
            }

            this.myTCPConnection.addToSendQueue(new Packet255KickDisconnect(s));
            this.myTCPConnection.serverShutdown();

            if (inetaddress != null && this.mcServer.getNetworkThread() instanceof DedicatedServerListenThread)
            {
                ((DedicatedServerListenThread)this.mcServer.getNetworkThread()).func_71761_a(inetaddress);
            }

            this.connectionComplete = true;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Default handler called for packets that don't have their own handlers in NetClientHandler; currentlly does
     * nothing.
     */
    public void unexpectedPacket(Packet par1Packet)
    {
        this.raiseErrorAndDisconnect("Protocol error");
    }

    public String getUsernameAndAddress()
    {
        return this.clientUsername != null ? this.clientUsername + " [" + this.myTCPConnection.getSocketAddress().toString() + "]" : this.myTCPConnection.getSocketAddress().toString();
    }

    /**
     * determine if it is a server handler
     */
    public boolean isServerHandler()
    {
        return true;
    }

    /**
     * Returns the server Id randomly generated by this login handler.
     */
    static String getServerId(NetLoginHandler par0NetLoginHandler)
    {
        return par0NetLoginHandler.loginServerId;
    }

    /**
     * Returns the reference to Minecraft Server.
     */
    static MinecraftServer getLoginMinecraftServer(NetLoginHandler par0NetLoginHandler)
    {
        return par0NetLoginHandler.mcServer;
    }

    /**
     * Return the secret AES sharedKey
     */
    static SecretKey getSharedKey(NetLoginHandler par0NetLoginHandler)
    {
        return par0NetLoginHandler.sharedKey;
    }

    /**
     * Returns the connecting client username.
     */
    static String getClientUsername(NetLoginHandler par0NetLoginHandler)
    {
        return par0NetLoginHandler.clientUsername;
    }

    public static boolean func_72531_a(NetLoginHandler par0NetLoginHandler, boolean par1)
    {
        return par0NetLoginHandler.field_72544_i = par1;
    }
    
    // Spigot start
    @Override
    public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
    {
        if (par1Packet250CustomPayload.channel.equals("BungeeCord") && org.bukkit.craftbukkit.Spigot.bungeeIPs.contains(getSocket().getInetAddress().getHostAddress()))
        {
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(par1Packet250CustomPayload.data);
            String subTag = in.readUTF();

            if (subTag.equals("Login"))
            {
                myTCPConnection.setSocketAddress(new java.net.InetSocketAddress(in.readUTF(), in.readInt()));
            }
        }
            
        FMLNetworkHandler.handlePacket250Packet(par1Packet250CustomPayload, myTCPConnection, this);  // MCPC+
    }
    // Spigot end    

    @Override
    public void handleVanilla250Packet(Packet250CustomPayload payload)
    {
        // NOOP for login
    }

    @Override
    public EntityPlayer getPlayer()
    {
        return null;
    }
}
