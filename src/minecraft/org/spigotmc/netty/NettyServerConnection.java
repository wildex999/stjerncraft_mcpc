package org.spigotmc.netty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import org.bukkit.Bukkit;

/**
 * This is the NettyServerConnection class. It implements
 * {@link ServerConnection} and is the main interface between the Minecraft
 * server and this NIO implementation. It handles starting, stopping and
 * processing the Netty backend.
 */
public class NettyServerConnection extends net.minecraft.network.NetworkListenThread {

    private final ChannelFuture socket;
    final List<net.minecraft.network.NetLoginHandler> pendingConnections = Collections.synchronizedList(new ArrayList<net.minecraft.network.NetLoginHandler>());

    public NettyServerConnection(net.minecraft.server.MinecraftServer ms, InetAddress host, int port) throws java.io.IOException { // MCPC+ - throws IOException for MCP compatibility
        super(ms);
        int threads = Integer.getInteger("org.spigotmc.netty.threads", 3);
        socket = new ServerBootstrap().channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                try {
                    ch.config().setOption(ChannelOption.IP_TOS, 0x18);
                } catch (ChannelException ex) {
                    // IP_TOS is not supported (Windows XP / Windows Server 2003)
                }

                NettyNetworkManager networkManager = new NettyNetworkManager();
                ch.pipeline()
                        .addLast("timer", new ReadTimeoutHandler(30))
                        .addLast("decoder", new PacketDecoder())
                        .addLast("encoder", new PacketEncoder(networkManager))
                        .addLast("manager", networkManager);
            }
        }).group(new NioEventLoopGroup(threads, new ThreadFactoryBuilder().setNameFormat("Netty IO Thread - %1$d").build())).localAddress(host, port).bind();
        net.minecraft.server.MinecraftServer.getServer().getLogAgent().logInfo("Using Netty NIO with " + threads + " threads for network connections.");
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
        for (int i = 0; i < pendingConnections.size(); ++i) {
            net.minecraft.network.NetLoginHandler connection = pendingConnections.get(i);

            try {
                connection.tryLogin();
            } catch (Exception ex) {
                connection.raiseErrorAndDisconnect("Internal server error");
                Bukkit.getServer().getLogger().log(Level.WARNING, "Failed to handle packet: " + ex, ex);
            }

            if (connection.connectionComplete) {
                pendingConnections.remove(i--);
            }
        }
    }

    /**
     * Shutdown. This method is called when the server is shutting down and the
     * server socket and all clients should be terminated with no further
     * action.
     */
    @Override
    public void stopListening() {
        socket.channel().close().syncUninterruptibly();
    }

    /**
     * Return a Minecraft compatible cipher instance from the specified key.
     *
     * @param opMode the mode to initialize the cipher in
     * @param key to use as the initial vector
     * @return the initialized cipher
     */
    public static Cipher getCipher(int opMode, Key key) {
        try {
            Cipher cip = Cipher.getInstance("AES/CFB8/NoPadding");
            cip.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cip;
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
}
