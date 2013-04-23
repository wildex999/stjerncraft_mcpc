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
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

/**
 * This is the NettyServerConnection class. It implements
 * {@link ServerConnection} and is the main interface between the Minecraft
 * server and this NIO implementation. It handles starting, stopping and
 * processing the Netty backend.
 */
public class NettyServerConnection extends net.minecraft.network.NetworkListenThread {

    private final ChannelFuture socket;

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
                        .addLast("flusher", new OutboundManager())
                        .addLast("timer", new ReadTimeoutHandler(30))
                        .addLast("decoder", new PacketDecoder())
                        .addLast("encoder", new PacketEncoder(networkManager))
                        .addLast("manager", networkManager);
            }
        }).childOption(ChannelOption.TCP_NODELAY, false).group(new NioEventLoopGroup(threads, new ThreadFactoryBuilder().setNameFormat("Netty IO Thread - %1$d").build())).localAddress(host, port).bind();
        net.minecraft.server.MinecraftServer.getServer().getLogAgent().logInfo("Using Netty NIO with " + threads + " threads for network connections.");
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
