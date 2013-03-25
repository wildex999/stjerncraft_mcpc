package net.minecraft.network.packet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class Packet56MapChunks extends Packet
{
    private int[] chunkPostX;
    private int[] chunkPosZ;
    public int[] field_73590_a;
    public int[] field_73588_b;

    /** The compressed chunk data buffer */
    private byte[] chunkDataBuffer;
    private byte[][] field_73584_f; // 'inflatedBuffers' in CB

    /** total size of the compressed data */
    private int dataLength;
    private boolean field_92076_h;
    private byte[] chunkDataNotCompressed = new byte[0]; // CraftBukkit - remove static
    private int maxLen = 0;

    private Semaphore deflateGate;

    private World world; // Spigot (Orebfuscator) Keep track of world

    public Packet56MapChunks() {}

    public Packet56MapChunks(List par1List)
    {
        int i = par1List.size();
        this.chunkPostX = new int[i];
        this.chunkPosZ = new int[i];
        this.field_73590_a = new int[i];
        this.field_73588_b = new int[i];
        this.field_73584_f = new byte[i][];
        this.field_92076_h = !par1List.isEmpty() && !((Chunk)par1List.get(0)).worldObj.provider.hasNoSky;
        int j = 0;

        for (int k = 0; k < i; ++k)
        {
            Chunk chunk = (Chunk)par1List.get(k);
            Packet51MapChunkData packet51mapchunkdata = Packet51MapChunk.getMapChunkData(chunk, true, 65535);

            world = chunk.worldObj; // Spigot (Orebfuscator)
            /* Spigot (Orebfuscator) - Don't use the build buffer yet. Copy to it more efficiently once the chunk is obfuscated
            // Moved to deflate()
            if (chunkDataNotCompressed.length < j + packet51mapchunkdata.compressedData.length)
            {
                byte[] abyte = new byte[j + packet51mapchunkdata.compressedData.length];
                System.arraycopy(chunkDataNotCompressed, 0, abyte, 0, chunkDataNotCompressed.length);
                chunkDataNotCompressed = abyte;
            }

            System.arraycopy(packet51mapchunkdata.compressedData, 0, chunkDataNotCompressed, j, packet51mapchunkdata.compressedData.length);*/
            j += packet51mapchunkdata.compressedData.length;
            this.chunkPostX[k] = chunk.xPosition;
            this.chunkPosZ[k] = chunk.zPosition;
            this.field_73590_a[k] = packet51mapchunkdata.chunkExistFlag;
            this.field_73588_b[k] = packet51mapchunkdata.chunkHasAddSectionFlag;
            this.field_73584_f[k] = packet51mapchunkdata.compressedData;
        }
        /* CraftBukkit start - Moved to compress()
        Deflater deflater = new Deflater(-1);

        try {
            deflater.setInput(buildBuffer, 0, j);
            deflater.finish();
            this.buffer = new byte[j];
            this.size = deflater.deflate(this.buffer);
        } finally {
            deflater.end();
        }
        */
        // MCPC+ start - use Forge's chunk packet compression
        deflateGate = new Semaphore(1);
        maxLen = j;
        // MCPC+ end      
    }

    private void deflate()
    {
        // Spigot (Orebfuscator) start - Obfuscate chunks
        int finalBufferSize = 0;

        for (int i = 0; i < field_73590_a.length; i++)
        {
            org.bukkit.craftbukkit.OrebfuscatorManager.obfuscate(chunkPostX[i], chunkPosZ[i], field_73590_a[i], field_73584_f[i], world);
            finalBufferSize += field_73584_f[i].length;
        }

        // Now it's time to efficiently copy the chunk to the build buffer
        chunkDataNotCompressed = new byte[finalBufferSize];
        int bufferLocation = 0;

        for (int i = 0; i < field_73590_a.length; i++)
        {
            System.arraycopy(field_73584_f[i], 0, chunkDataNotCompressed, bufferLocation, field_73584_f[i].length);
            bufferLocation += field_73584_f[i].length;
        }

        // Spigot (Orebfuscator) end

        Deflater deflater = new Deflater(-1);

        try
        {
            deflater.setInput(chunkDataNotCompressed, 0, maxLen);
            deflater.finish();
            byte[] deflated = new byte[maxLen];
            this.dataLength = deflater.deflate(deflated);
            this.chunkDataBuffer = deflated;
        }
        finally
        {
            deflater.end();
        }
    }

    /**
     * Abstract. Reads the raw packet data from the data stream.
     */
    public void readPacketData(DataInputStream par1DataInputStream) throws IOException
    {
        short short1 = par1DataInputStream.readShort();
        this.dataLength = par1DataInputStream.readInt();
        this.field_92076_h = par1DataInputStream.readBoolean();
        this.chunkPostX = new int[short1];
        this.chunkPosZ = new int[short1];
        this.field_73590_a = new int[short1];
        this.field_73588_b = new int[short1];
        this.field_73584_f = new byte[short1][];

        if (chunkDataNotCompressed.length < this.dataLength)
        {
            chunkDataNotCompressed = new byte[this.dataLength];
        }

        par1DataInputStream.readFully(chunkDataNotCompressed, 0, this.dataLength);
        byte[] abyte = new byte[196864 * short1];
        Inflater inflater = new Inflater();
        inflater.setInput(chunkDataNotCompressed, 0, this.dataLength);

        try
        {
            inflater.inflate(abyte);
        }
        catch (DataFormatException dataformatexception)
        {
            throw new IOException("Bad compressed data format");
        }
        finally
        {
            inflater.end();
        }

        int i = 0;

        for (int j = 0; j < short1; ++j)
        {
            this.chunkPostX[j] = par1DataInputStream.readInt();
            this.chunkPosZ[j] = par1DataInputStream.readInt();
            this.field_73590_a[j] = par1DataInputStream.readShort();
            this.field_73588_b[j] = par1DataInputStream.readShort();
            int k = 0;
            int l = 0;
            int i1;

            for (i1 = 0; i1 < 16; ++i1)
            {
                k += this.field_73590_a[j] >> i1 & 1;
                l += this.field_73588_b[j] >> i1 & 1;
            }

            i1 = 2048 * 4 * k + 256;
            i1 += 2048 * l;

            if (this.field_92076_h)
            {
                i1 += 2048 * k;
            }

            this.field_73584_f[j] = new byte[i1];
            System.arraycopy(abyte, i, this.field_73584_f[j], 0, i1);
            i += i1;
        }
    }

    /**
     * Abstract. Writes the raw packet data to the data stream.
     */
    public void writePacketData(DataOutputStream par1DataOutputStream) throws IOException
    {
        if (this.chunkDataBuffer == null)
        {
            deflateGate.acquireUninterruptibly();
            if (this.chunkDataBuffer == null)
            {
                deflate();
            }
            deflateGate.release();
        }

        par1DataOutputStream.writeShort(this.chunkPostX.length);
        par1DataOutputStream.writeInt(this.dataLength);
        par1DataOutputStream.writeBoolean(this.field_92076_h);
        par1DataOutputStream.write(this.chunkDataBuffer, 0, this.dataLength);

        for (int i = 0; i < this.chunkPostX.length; ++i)
        {
            par1DataOutputStream.writeInt(this.chunkPostX[i]);
            par1DataOutputStream.writeInt(this.chunkPosZ[i]);
            par1DataOutputStream.writeShort((short)(this.field_73590_a[i] & 65535));
            par1DataOutputStream.writeShort((short)(this.field_73588_b[i] & 65535));
        }
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(NetHandler par1NetHandler)
    {
        par1NetHandler.handleMapChunks(this);
    }

    /**
     * Abstract. Return the size of the packet (not counting the header).
     */
    public int getPacketSize()
    {
        return 6 + this.dataLength + 12 * this.getNumberOfChunkInPacket();
    }

    @SideOnly(Side.CLIENT)
    public int getChunkPosX(int par1)
    {
        return this.chunkPostX[par1];
    }

    @SideOnly(Side.CLIENT)
    public int getChunkPosZ(int par1)
    {
        return this.chunkPosZ[par1];
    }

    public int getNumberOfChunkInPacket()
    {
        return this.chunkPostX.length;
    }

    @SideOnly(Side.CLIENT)
    public byte[] getChunkCompressedData(int par1)
    {
        return this.field_73584_f[par1];
    }
}