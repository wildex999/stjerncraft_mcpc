package org.spigotmc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.DataOutputStream;

/**
 * Netty encoder which takes a packet and encodes it, and adds a byte packet id
 * header.
 */
public class PacketEncoder extends MessageToByteEncoder<net.minecraft.network.packet.Packet>
{

    private ByteBuf outBuf;
    private DataOutputStream dataOut;
    private final NettyNetworkManager networkManager;

    public PacketEncoder(NettyNetworkManager networkManager)
    {
        this.networkManager = networkManager;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, net.minecraft.network.packet.Packet msg, ByteBuf out) throws Exception
    {
        if ( outBuf == null )
        {
            outBuf = ctx.alloc().directBuffer();
        }
        if ( dataOut == null )
        {
            dataOut = new DataOutputStream( new ByteBufOutputStream( outBuf ) );
        }

        out.writeByte( msg.getPacketId() );
        msg.writePacketData( dataOut );

        networkManager.addWrittenBytes( outBuf.readableBytes() );
        out.writeBytes( outBuf );
        out.discardSomeReadBytes();
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        super.freeOutboundBuffer(ctx);
        if (outBuf != null) {
            outBuf.release();
            outBuf = null;
        }
        dataOut = null;
    }
}
