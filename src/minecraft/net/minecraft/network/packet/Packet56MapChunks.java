package net.minecraft.network.packet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.world.chunk.Chunk;

public class Packet56MapChunks extends Packet
{
    private int[] chunkPostX;
    private int[] chunkPosZ;
    public int[] field_73590_a;
    public int[] field_73588_b;

    /** The compressed chunk data buffer */
    private byte[] chunkDataBuffer;
    private byte[][] field_73584_f;

    /** total size of the compressed data */
    private int dataLength;
    private boolean field_92024_h;
    private byte[] chunkDataNotCompressed = new byte[0]; // CraftBukkit - remove static
    // CraftBukkit start
    static final ThreadLocal<Deflater> localDeflater = new ThreadLocal<Deflater>()
    {
        @Override
        protected Deflater initialValue()
        {
            // Don't use higher compression level, slows things down too much
            return new Deflater(6);
        }
    };
    // CraftBukkit end

    public Packet56MapChunks() {}

    public Packet56MapChunks(List par1List)
    {
        int var2 = par1List.size();
        this.chunkPostX = new int[var2];
        this.chunkPosZ = new int[var2];
        this.field_73590_a = new int[var2];
        this.field_73588_b = new int[var2];
        this.field_73584_f = new byte[var2][];
        this.field_92024_h = !par1List.isEmpty() && !((Chunk)par1List.get(0)).worldObj.provider.hasNoSky;
        int var3 = 0;

        for (int var4 = 0; var4 < var2; ++var4)
        {
            Chunk var5 = (Chunk)par1List.get(var4);
            Packet51MapChunkData var6 = Packet51MapChunk.getMapChunkData(var5, true, '\uffff');

            if (chunkDataNotCompressed.length < var3 + var6.compressedData.length)
            {
                byte[] var7 = new byte[var3 + var6.compressedData.length];
                System.arraycopy(chunkDataNotCompressed, 0, var7, 0, chunkDataNotCompressed.length);
                chunkDataNotCompressed = var7;
            }

            System.arraycopy(var6.compressedData, 0, chunkDataNotCompressed, var3, var6.compressedData.length);
            var3 += var6.compressedData.length;
            this.chunkPostX[var4] = var5.xPosition;
            this.chunkPosZ[var4] = var5.zPosition;
            this.field_73590_a[var4] = var6.chunkExistFlag;
            this.field_73588_b[var4] = var6.chunkHasAddSectionFlag;
            this.field_73584_f[var4] = var6.compressedData;
        }

        /* CraftBukkit start - moved to compress()
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
    }

    // Add compression method
    public void compress()
    {
        if (this.chunkDataBuffer != null)
        {
            return;
        }

        Deflater deflater = localDeflater.get();
        deflater.reset();
        deflater.setInput(this.chunkDataNotCompressed);
        deflater.finish();
        this.chunkDataBuffer = new byte[this.chunkDataNotCompressed.length + 100];
        this.dataLength = deflater.deflate(this.chunkDataBuffer);
    }
    // CraftBukkit end

    /**
     * Abstract. Reads the raw packet data from the data stream.
     */
    public void readPacketData(DataInputStream par1DataInputStream) throws IOException   // CraftBukkit - throws IOException
    {
        short var2 = par1DataInputStream.readShort();
        this.dataLength = par1DataInputStream.readInt();
        this.field_92024_h = par1DataInputStream.readBoolean();
        this.chunkPostX = new int[var2];
        this.chunkPosZ = new int[var2];
        this.field_73590_a = new int[var2];
        this.field_73588_b = new int[var2];
        this.field_73584_f = new byte[var2][];

        if (chunkDataNotCompressed.length < this.dataLength)
        {
            chunkDataNotCompressed = new byte[this.dataLength];
        }

        par1DataInputStream.readFully(chunkDataNotCompressed, 0, this.dataLength);
        byte[] var3 = new byte[196864 * var2];
        Inflater var4 = new Inflater();
        var4.setInput(chunkDataNotCompressed, 0, this.dataLength);

        try
        {
            var4.inflate(var3);
        }
        catch (DataFormatException var12)
        {
            throw new IOException("Bad compressed data format");
        }
        finally
        {
            var4.end();
        }

        int var5 = 0;

        for (int var6 = 0; var6 < var2; ++var6)
        {
            this.chunkPostX[var6] = par1DataInputStream.readInt();
            this.chunkPosZ[var6] = par1DataInputStream.readInt();
            this.field_73590_a[var6] = par1DataInputStream.readShort();
            this.field_73588_b[var6] = par1DataInputStream.readShort();
            int var7 = 0;
            int var8 = 0;
            int var9;

            for (var9 = 0; var9 < 16; ++var9)
            {
                var7 += this.field_73590_a[var6] >> var9 & 1;
                var8 += this.field_73588_b[var6] >> var9 & 1;
            }

            var9 = 2048 * 4 * var7 + 256;
            var9 += 2048 * var8;

            if (this.field_92024_h)
            {
                var9 += 2048 * var7;
            }

            this.field_73584_f[var6] = new byte[var9];
            System.arraycopy(var3, var5, this.field_73584_f[var6], 0, var9);
            var5 += var9;
        }
    }

    /**
     * Abstract. Writes the raw packet data to the data stream.
     */
    public void writePacketData(DataOutputStream par1DataOutputStream) throws IOException   // CraftBukkit - throws IOException
    {
        compress(); // CraftBukkit
        par1DataOutputStream.writeShort(this.chunkPostX.length);
        par1DataOutputStream.writeInt(this.dataLength);
        par1DataOutputStream.writeBoolean(this.field_92024_h);
        par1DataOutputStream.write(this.chunkDataBuffer, 0, this.dataLength);

        for (int var2 = 0; var2 < this.chunkPostX.length; ++var2)
        {
            par1DataOutputStream.writeInt(this.chunkPostX[var2]);
            par1DataOutputStream.writeInt(this.chunkPosZ[var2]);
            par1DataOutputStream.writeShort((short)(this.field_73590_a[var2] & '\uffff'));
            par1DataOutputStream.writeShort((short)(this.field_73588_b[var2] & '\uffff'));
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
