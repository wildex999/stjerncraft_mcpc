package net.minecraft.network;

import java.io.IOException;
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
    /** The 4 byte verify token read from a Packet252SharedKey */
    private byte[] verifyToken;

    /** The Minecraft logger. */
    public static Logger logger = Logger.getLogger("Minecraft");

    /** The Random object used to generate serverId hex strings. */
    private static Random rand = new Random();
    public TcpConnection myTCPConnection;
    public boolean connectionComplete = false;

    /** Reference to the MinecraftServer object. */
    private MinecraftServer mcServer;
    private int connectionTimer = 0;
    public String clientUsername = null;
    private volatile boolean field_72544_i = false;
    private String loginServerId = Long.toString(rand.nextLong(), 16); // CraftBukkit - Security fix
    private boolean field_92028_k = false;

    /** Secret AES key obtained from the client's Packet252SharedKey */
    private SecretKey sharedKey = null;
    public String hostname = ""; // CraftBukkit - add field

    public NetLoginHandler(MinecraftServer par1MinecraftServer, Socket par2Socket, String par3Str) throws IOException
    {
        this.mcServer = par1MinecraftServer;
        this.myTCPConnection = new TcpConnection(par2Socket, par3Str, this, par1MinecraftServer.getKeyPair().getPrivate());
        this.myTCPConnection.field_74468_e = 0;
    }

    // CraftBukkit start
    public Socket getSocket()
    {
        return this.myTCPConnection.getSocket();
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
            logger.info("Disconnecting " + this.getUsernameAndAddress() + ": " + par1Str);
            this.myTCPConnection.addToSendQueue(new Packet255KickDisconnect(par1Str));
            this.myTCPConnection.serverShutdown();
            this.connectionComplete = true;
        }
        catch (Exception var3)
        {
            var3.printStackTrace();
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
            PublicKey var2 = this.mcServer.getKeyPair().getPublic();

            if (par1Packet2ClientProtocol.getProtocolVersion() != 51)
            {
                if (par1Packet2ClientProtocol.getProtocolVersion() > 51)
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
                this.myTCPConnection.addToSendQueue(new Packet253ServerAuthData(this.loginServerId, var2, this.verifyToken));
            }
        }
    }

    public void handleSharedKey(Packet252SharedKey par1Packet252SharedKey)
    {
        PrivateKey var2 = this.mcServer.getKeyPair().getPrivate();
        this.sharedKey = par1Packet252SharedKey.getSharedKey(var2);

        if (!Arrays.equals(this.verifyToken, par1Packet252SharedKey.getVerifyToken(var2)))
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
                if (this.field_92028_k)
                {
                    this.raiseErrorAndDisconnect("Duplicate login");
                    return;
                }

                this.field_92028_k = true;
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

    public void completeConnection(String var1)
    {
        if (var1 != null)
        {
            this.raiseErrorAndDisconnect(var1);
        }

        // CraftBukkit start
        EntityPlayerMP s = this.mcServer.getConfigurationManager().attemptLogin(this, this.clientUsername, this.hostname);

        if (s == null)
        {
            return;
            // CraftBukkit end
        }
        else
        {
            EntityPlayerMP entityplayer = this.mcServer.getConfigurationManager().processLogin(s); // CraftBukkit - this.h -> s

            if (entityplayer != null)
            {
                this.mcServer.getConfigurationManager().initializeConnectionToPlayer((INetworkManager) this.myTCPConnection, entityplayer);
            }
        }

        this.connectionComplete = true;
    }

    public void handleErrorMessage(String par1Str, Object[] par2ArrayOfObj)
    {
        logger.info(this.getUsernameAndAddress() + " lost connection");
        this.connectionComplete = true;
    }

    /**
     * Handle a server ping packet.
     */
    public void handleServerPing(Packet254ServerPing par1Packet254ServerPing)
    {
        if (this.myTCPConnection.getSocket() == null)
        {
            return;    // CraftBukkit - fix NPE when a client queries a server that is unable to handle it.
        }

        try
        {
            ServerConfigurationManager var2 = this.mcServer.getConfigurationManager();
            String var3 = null;
            // CraftBukkit
            org.bukkit.event.server.ServerListPingEvent var4 = org.bukkit.craftbukkit.event.CraftEventFactory.callServerListPingEvent(this.mcServer.server, getSocket().getInetAddress(), this.mcServer.getMOTD(), var2.getCurrentPlayerCount(), var2.getMaxPlayers());

            if (par1Packet254ServerPing.field_82559_a == 1)
            {
                // CraftBukkit start - fix decompile issues, don't create a list from an array
                Object[] list = new Object[] { 1, 51, this.mcServer.getMinecraftVersion(), var4.getMotd(), var2.getCurrentPlayerCount(), var4.getMaxPlayers() };

                for (Object object : list)
                {
                    if (var3 == null)
                    {
                        var3 = "\u00A7";
                    }
                    else
                    {
                        var3 = var3 + "\0";
                    }

                    var3 += org.apache.commons.lang.StringUtils.replace(object.toString(), "\0", "");
                }

                // CraftBukkit end
            }
            else
            {
                // CraftBukkit
                var3 = var4.getMotd() + "\u00A7" + var2.getCurrentPlayerCount() + "\u00A7" + var4.getMaxPlayers();
            }

            InetAddress var6 = null;

            if (this.myTCPConnection.getSocket() != null)
            {
                var6 = this.myTCPConnection.getSocket().getInetAddress();
            }

            this.myTCPConnection.addToSendQueue(new Packet255KickDisconnect(var3));
            this.myTCPConnection.serverShutdown();

            if (var6 != null && this.mcServer.getNetworkThread() instanceof DedicatedServerListenThread)
            {
                ((DedicatedServerListenThread) this.mcServer.getNetworkThread()).func_71761_a(var6);
            }

            this.connectionComplete = true;
        }
        catch (Exception var5)
        {
            var5.printStackTrace();
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


    public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
    {
        FMLNetworkHandler.handlePacket250Packet(par1Packet250CustomPayload, myTCPConnection, this);
    }

    @Override
    public void handleVanilla250Packet(Packet250CustomPayload payload)
    {
        // NOOP for login
    }

    public EntityPlayer getPlayer()
    {
        return null;
    }

    public EntityPlayer getPlayerH()
    {
        return null;
    }
}
