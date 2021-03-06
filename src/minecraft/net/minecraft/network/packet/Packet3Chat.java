package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException; // CraftBukkit

public class Packet3Chat extends Packet
{
    /** Maximum number of characters allowed in chat string in each packet. */
    public static int maxChatLength = 119;

    /** The message being sent. */
    public String message;
    private boolean isServer;

    public Packet3Chat()
    {
        this.isServer = true;
    }

    public Packet3Chat(String par1Str)
    {
        this(par1Str, true);
    }

    public Packet3Chat(String par1Str, boolean par2)
    {
        this.isServer = true;
        /* CraftBukkit start - Handle this later
        if (s.length() > b) {
            s = s.substring(0, b);
        }
        // CraftBukkit end */
        this.message = par1Str;
        this.isServer = par2;
    }

    /**
     * Abstract. Reads the raw packet data from the data stream.
     */
    public void readPacketData(DataInputStream par1DataInputStream) throws IOException   // CraftBukkit
    {
        this.message = readString(par1DataInputStream, maxChatLength);
    }

    /**
     * Abstract. Writes the raw packet data to the data stream.
     */
    public void writePacketData(DataOutputStream par1DataOutputStream) throws IOException   // CraftBukkit
    {
        writeString(this.message, par1DataOutputStream);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(NetHandler par1NetHandler)
    {
        par1NetHandler.handleChat(this);
    }

    /**
     * Abstract. Return the size of the packet (not counting the header).
     */
    public int getPacketSize()
    {
        return 2 + this.message.length() * 2;
    }

    /**
     * Get whether this is a server
     */
    public boolean getIsServer()
    {
        return this.isServer;
    }

    /**
     * If this returns true, the packet may be processed on any thread; otherwise it is queued for the main thread to
     * handle.
     */
    public boolean canProcessAsync()
    {
        return !this.message.startsWith("/");
    }
}
