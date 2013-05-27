package org.spigotmc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToByteCodec;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

/**
 * This class is a complete solution for encrypting and decoding bytes in a
 * Netty stream. It takes two {@link Cipher} instances, used for encryption and
 * decryption respectively.
 */
public class CipherCodec extends ByteToByteCodec {

    private Cipher encrypt;
    private Cipher decrypt;
    private net.minecraft.network.packet.Packet252SharedKey responsePacket;
    private ThreadLocal<byte[]> heapInLocal = new EmptyByteThreadLocal();
    private ThreadLocal<byte[]> heapOutLocal = new EmptyByteThreadLocal();

    private static class EmptyByteThreadLocal extends ThreadLocal<byte[]> {

        @Override
        protected byte[] initialValue() {
            return new byte[0];
        }
    }

    public CipherCodec(Cipher encrypt, Cipher decrypt, net.minecraft.network.packet.Packet252SharedKey responsePacket) {
        this.encrypt = encrypt;
        this.decrypt = decrypt;
        this.responsePacket = responsePacket;
    }

    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().write(responsePacket);
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        cipher(in, out, encrypt);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        cipher(in, out, decrypt);
    }

    private void cipher(ByteBuf in, ByteBuf out, Cipher cipher) throws ShortBufferException {
        byte[] heapIn = heapInLocal.get();
        int readableBytes = in.readableBytes();
        if (heapIn.length < readableBytes) {
            heapIn = new byte[readableBytes];
        }
        in.readBytes(heapIn, 0, readableBytes);

        byte[] heapOut = heapOutLocal.get();
        int outputSize = cipher.getOutputSize(readableBytes);
        if (heapOut.length < outputSize) {
            heapOut = new byte[outputSize];
        }
        out.writeBytes(heapOut, 0, cipher.update(heapIn, 0, readableBytes, heapOut));
    }
}