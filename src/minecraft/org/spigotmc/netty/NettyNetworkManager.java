package org.spigotmc.netty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.AbstractList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * This class forms the basis of the Netty integration. It implements
 * {@link INetworkManager} and handles all events and inbound messages provided
 * by the upstream Netty process.
 */
public class NettyNetworkManager extends ChannelInboundMessageHandlerAdapter<net.minecraft.network.packet.Packet> implements net.minecraft.network.INetworkManager {

    private static final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Async Packet Handler - %1$d").build());
    private static final net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
    private static final PrivateKey key = server.getKeyPair().getPrivate();
    private static final NettyServerConnection serverConnection = (NettyServerConnection) server.getNetworkThread();
    /*========================================================================*/
    private final Queue<net.minecraft.network.packet.Packet> syncPackets = new ConcurrentLinkedQueue<net.minecraft.network.packet.Packet>();
    private final List<net.minecraft.network.packet.Packet> highPriorityQueue = new AbstractList<net.minecraft.network.packet.Packet>() {
        @Override
        public void add(int index, net.minecraft.network.packet.Packet element) {
            // NOP
        }

        @Override
        public net.minecraft.network.packet.Packet get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }
    };
    private volatile boolean connected;
    private Channel channel;
    private SocketAddress address;
    private net.minecraft.network.packet.NetHandler connection;
    private SecretKey secret;
    private String dcReason;
    private Object[] dcArgs;
    private Socket socketAdaptor;
    private long writtenBytes;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Channel and address groundwork first
        channel = ctx.channel();
        address = channel.remoteAddress();
        // Then the socket adaptor
        socketAdaptor = NettySocketAdaptor.adapt((SocketChannel) channel);
        // Followed by their first handler
        connection = new net.minecraft.network.NetLoginHandler(server, this);
        // Finally register the connection
        connected = true;
        serverConnection.pendingConnections.add((net.minecraft.network.NetLoginHandler) connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        networkShutdown("disconnect.endOfStream", new Object[0]);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO: Remove this once we are more stable
        // Bukkit.getServer().getLogger().severe("======================= Start Netty Debug Log =======================");
        // Bukkit.getServer().getLogger().log(Level.SEVERE, "Error caught whilst handling " + channel, cause);
        // Bukkit.getServer().getLogger().severe("======================= End Netty Debug Log =======================");
        // Disconnect with generic reason + exception
        networkShutdown("disconnect.genericReason", new Object[]{"Internal exception: " + cause});
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final net.minecraft.network.packet.Packet msg) throws Exception {
        if (connected) {
            if (msg instanceof net.minecraft.network.packet.Packet252SharedKey) {
                secret = ((net.minecraft.network.packet.Packet252SharedKey) msg).getSharedKey(key);
            }

            if (msg.canProcessAsync()) {
                threadPool.submit(new Runnable() {
                    public void run() {
                        net.minecraft.network.packet.Packet packet = PacketListener.callReceived(NettyNetworkManager.this, connection, msg);
                        if (packet != null) {
                            packet.processPacket(connection);
                        }
                    }
                });
            } else {
                syncPackets.add(msg);
            }
        }
    }

    public Socket getSocket() {
        return socketAdaptor;
    }

    /**
     * setHandler. Set the {@link NetHandler} used to process received packets.
     *
     * @param nh the new {@link NetHandler} instance
     */

    /**
     * Sets the NetHandler for this NetworkManager. Server-only.
     */
    public void setNetHandler(net.minecraft.network.packet.NetHandler nh) {
        connection = nh;
    }

    /**
     * queue. Queue a packet for sending, or in this case send it to be write it
     * straight to the channel.
     *
     * @param packet the packet to queue
     */

    /**
     * Adds the packet to the correct send queue (chunk data packets go to a separate queue).
     */
    public void addToSendQueue(net.minecraft.network.packet.Packet packet) {
        // Only send if channel is still connected
        if (connected) {
            // Process packet via handler
            packet = PacketListener.callQueued(this, connection, packet);
            // If handler indicates packet send
            if (packet != null) {
                highPriorityQueue.add(packet);

                // If needed, check and prepare encryption phase
                // We don't send the packet here as it is sent just before the cipher handler has been added to ensure we can safeguard from any race conditions
                // Which are caused by the slow first initialization of the cipher SPI
                if (packet instanceof net.minecraft.network.packet.Packet252SharedKey) {
                    Cipher encrypt = NettyServerConnection.getCipher(Cipher.ENCRYPT_MODE, secret);
                    Cipher decrypt = NettyServerConnection.getCipher(Cipher.DECRYPT_MODE, secret);
                    CipherCodec codec = new CipherCodec(encrypt, decrypt, (net.minecraft.network.packet.Packet252SharedKey) packet);
                    channel.pipeline().addBefore("decoder", "cipher", codec);
                } else {
                    channel.write(packet);
                }
            }
        }
    }

    /**
     * wakeThreads. In Vanilla this method will interrupt the network read and
     * write threads, thus waking them.
     */

    /**
     * Wakes reader and writer threads
     */
    public void wakeThreads() {
    }

    /**
     * processPackets. Remove up to 1000 packets from the queue and process
     * them. This method should only be called from the main server thread.
     */

    /**
     * Checks timeouts and processes all pending read packets.
     */
    public void processReadPackets() {
        for (int i = 1000; !syncPackets.isEmpty() && i >= 0; i--) {
            if (connection instanceof net.minecraft.network.NetLoginHandler ? ((net.minecraft.network.NetLoginHandler) connection).connectionComplete : ((net.minecraft.network.NetServerHandler) connection).connectionClosed) {
                syncPackets.clear();
                break;
            }

            net.minecraft.network.packet.Packet packet = PacketListener.callReceived(this, connection, syncPackets.poll());
            if (packet != null) {
                packet.processPacket(connection);
            }
        }

        // Disconnect via the handler - this performs all plugin related cleanup + logging
        if (!connected && (dcReason != null || dcArgs != null)) {
            connection.handleErrorMessage(dcReason, dcArgs);
        }
    }

    /**
     * getSocketAddress. Return the remote address of the connected user. It is
     * important that this method returns a value even after disconnect.
     *
     * @return the remote address of this connection
     */

    /**
     * Return the InetSocketAddress of the remote endpoint
     */
    public SocketAddress getSocketAddress() {
        return address;
    }

    public void setSocketAddress(SocketAddress address) {
        this.address = address;
    }

    /**
     * close. Close and release all resources associated with this connection.
     */

    /**
     * Shuts down the server. (Only actually used on the server)
     */
    public void serverShutdown() {
        if (connected) {
            connected = false;
            channel.close();
        }
    }

    /**
     * queueSize. Return the number of packets in the low priority queue. In a
     * NIO environment this will always be 0.
     *
     * @return the size of the packet send queue
     */

    /**
     * returns 0 for memoryConnections
     */
    public int packetSize() {
        return 0;
    }

    /**
     * networkShutdown. Shuts down this connection, storing the reason and
     * parameters, used to notify the current {@link Connection}.
     *
     * @param reason the main disconnect reason
     * @param arguments additional disconnect arguments, for example, the
     * exception which triggered the disconnect.
     */

    /**
     * Shuts down the network with the specified reason. Closes all streams and sockets, spawns NetworkMasterThread to
     * stop reading and writing threads.
     */
    public void networkShutdown(String reason, Object... arguments) {
        if (connected) {
            dcReason = reason;
            dcArgs = arguments;
            serverShutdown();
        }
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }

    public void addWrittenBytes(int written) {
        writtenBytes += written;
    }
}
