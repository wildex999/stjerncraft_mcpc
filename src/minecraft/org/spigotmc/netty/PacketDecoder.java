package org.spigotmc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import net.minecraft.network.packet.Packet;

/**
 * Packet decoding class backed by a reusable {@link DataInputStream} which
 * backs the input {@link ByteBuf}. Reads an unsigned byte packet header and
 * then decodes the packet accordingly.
 */
public class PacketDecoder extends ReplayingDecoder<ReadState>
{

    private DataInputStream input;
    private net.minecraft.network.packet.Packet packet;

    public PacketDecoder()
    {
        super( ReadState.HEADER );
    }

    @Override
    protected Packet decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception
    {
        if ( input == null )
        {
            input = new DataInputStream( new ByteBufInputStream( in ) );
        }

        while ( true )
        {
            switch ( state() )
            {
                case HEADER:
                    short packetId = in.readUnsignedByte();
                    packet = net.minecraft.network.packet.Packet.getNewPacket( net.minecraft.server.MinecraftServer.getServer().getLogAgent(), packetId );
                    if ( packet == null )
                    {
                        throw new IOException( "Bad packet id " + packetId );
                    }
                    checkpoint( ReadState.DATA );
                case DATA:
                    try
                    {
                        packet.readPacketData( input );
                    } catch ( EOFException ex )
                    {
                        return null;
                    }

                    checkpoint( ReadState.HEADER );
                    net.minecraft.network.packet.Packet ret = packet;
                    packet = null;

                    return ret;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
