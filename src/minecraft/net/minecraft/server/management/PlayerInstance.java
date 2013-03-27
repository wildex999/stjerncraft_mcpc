package net.minecraft.server.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet51MapChunk;
import net.minecraft.network.packet.Packet52MultiBlockChange;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeDummyContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

public class PlayerInstance
{
    private final List playersInChunk;

    /** note: this is final */
    private final ChunkCoordIntPair chunkLocation;
    private short[] locationOfBlockChange;
    private int numberOfTilesToUpdate;
    private int field_73260_f;
    private boolean loaded = false; // CraftBukkit

    final PlayerManager myManager;

    public PlayerInstance(PlayerManager par1PlayerManager, int par2, int par3)
    {
        this.myManager = par1PlayerManager;
        this.playersInChunk = new ArrayList();
        this.locationOfBlockChange = new short[64];
        this.numberOfTilesToUpdate = 0;
        this.chunkLocation = new ChunkCoordIntPair(par2, par3);
        // CraftBukkit start
        par1PlayerManager.getWorldServer().theChunkProviderServer.getChunkAt(par2, par3, new Runnable()
        {
            public void run()
            {
                PlayerInstance.this.loaded = true;
            }
        });
        // CraftBukkit end
    }

    /**
     * called for all chunks within the visible radius of the player
     */
    public void addPlayerToChunkWatchingList(final EntityPlayerMP par1EntityPlayerMP)   // CraftBukkit - added final to argument
    {
        if (this.playersInChunk.contains(par1EntityPlayerMP))
        {
            throw new IllegalStateException("Failed to add player. " + par1EntityPlayerMP + " already is in chunk " + this.chunkLocation.chunkXPos + ", " + this.chunkLocation.chunkZPos);
        }
        else
        {
            this.playersInChunk.add(par1EntityPlayerMP);

            // CraftBukkit start
            if (this.loaded)
            {
                par1EntityPlayerMP.loadedChunks.add(this.chunkLocation);
            }
            else
            {
                // Abuse getChunkAt to add another callback
                this.myManager.getWorldServer().theChunkProviderServer.getChunkAt(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, new Runnable()
                {
                    public void run()
                    {
                        par1EntityPlayerMP.loadedChunks.add(PlayerInstance.this.chunkLocation);
                    }
                });
            }

            // CraftBukkit end
        }
    }

    public void sendThisChunkToPlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        if (this.playersInChunk.contains(par1EntityPlayerMP))
        {
            par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet51MapChunk(PlayerManager.getWorldServer(this.myManager).getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos), true, 0));
            this.playersInChunk.remove(par1EntityPlayerMP);
            par1EntityPlayerMP.loadedChunks.remove(this.chunkLocation);

            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(chunkLocation, par1EntityPlayerMP));

            if (this.playersInChunk.isEmpty())
            {
                long i = (long)this.chunkLocation.chunkXPos + 2147483647L | (long)this.chunkLocation.chunkZPos + 2147483647L << 32;
                PlayerManager.getChunkWatchers(this.myManager).remove(i);

                if (this.numberOfTilesToUpdate > 0)
                {
                    PlayerManager.c(this.myManager).remove(this);
                }

                this.myManager.getWorldServer().theChunkProviderServer.unloadChunksIfNotNearSpawn(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
            }
        }
    }

    public void flagChunkForUpdate(int par1, int par2, int par3)
    {
        if (this.numberOfTilesToUpdate == 0)
        {
            PlayerManager.c(this.myManager).add(this);
        }

        this.field_73260_f |= 1 << (par2 >> 4);

        //if (this.numberOfTilesToUpdate < 64) //Forge; Cache everything, so always run
        {
            short short1 = (short)(par1 << 12 | par3 << 8 | par2);

            for (int l = 0; l < this.numberOfTilesToUpdate; ++l)
            {
                if (this.locationOfBlockChange[l] == short1)
                {
                    return;
                }
            }

            if (numberOfTilesToUpdate == locationOfBlockChange.length)
            {
                locationOfBlockChange = Arrays.copyOf(locationOfBlockChange, locationOfBlockChange.length << 1);
            }
            this.locationOfBlockChange[this.numberOfTilesToUpdate++] = short1;
        }
    }

    public void sendToAllPlayersWatchingChunk(Packet par1Packet)
    {
        for (int i = 0; i < this.playersInChunk.size(); ++i)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playersInChunk.get(i);

            if (!entityplayermp.loadedChunks.contains(this.chunkLocation))
            {
                entityplayermp.playerNetServerHandler.sendPacketToPlayer(par1Packet);
            }
        }
    }

    public void sendChunkUpdate()
    {
        if (this.numberOfTilesToUpdate != 0)
        {
            int i;
            int j;
            int k;

            if (this.numberOfTilesToUpdate == 1)
            {
                i = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[0] >> 12 & 15);
                j = this.locationOfBlockChange[0] & 255;
                k = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[0] >> 8 & 15);
                this.sendToAllPlayersWatchingChunk(new Packet53BlockChange(i, j, k, PlayerManager.getWorldServer(this.myManager)));

                if (PlayerManager.getWorldServer(this.myManager).blockHasTileEntity(i, j, k))
                {
                    this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.myManager).getBlockTileEntity(i, j, k));
                }
            }
            else
            {
                int l;

                if (this.numberOfTilesToUpdate >= ForgeDummyContainer.clumpingThreshold)
                {
                    i = this.chunkLocation.chunkXPos * 16;
                    j = this.chunkLocation.chunkZPos * 16;
                    this.sendToAllPlayersWatchingChunk(new Packet51MapChunk(PlayerManager.getWorldServer(this.myManager).getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos), (this.field_73260_f == 0xFFFF), this.field_73260_f)); // CraftBukkit - send everything (including biome) if all sections flagged

                    /* Forge: Grabs ALL tile entities is costly on a modded server, only send needed ones
                    for (k = 0; k < 16; ++k)
                    {
                        if ((this.field_73260_f & 1 << k) != 0)
                        {
                            l = k << 4;
                            List list = PlayerManager.getWorldServer(this.myManager).getAllTileEntityInBox(i, l, j, i + 16, l + 16, j + 16);

                            for (int i1 = 0; i1 < list.size(); ++i1)
                            {
                                this.sendTileToAllPlayersWatchingChunk((TileEntity)list.get(i1));
                            }
                        }
                    }
                    */
                }
                else
                {
                    this.sendToAllPlayersWatchingChunk(new Packet52MultiBlockChange(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, this.locationOfBlockChange, this.numberOfTilesToUpdate, PlayerManager.getWorldServer(this.myManager)));
                }

                { //Forge: Send only the tile entities that are updated, Adding this brace lets us keep the indent and the patch small
                    for (i = 0; i < this.numberOfTilesToUpdate; ++i)
                    {
                        j = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[i] >> 12 & 15);
                        k = this.locationOfBlockChange[i] & 255;
                        l = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[i] >> 8 & 15);

                        if (PlayerManager.getWorldServer(this.myManager).blockHasTileEntity(j, k, l))
                        {
                            this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.myManager).getBlockTileEntity(j, k, l));
                        }
                    }
                }
            }

            this.numberOfTilesToUpdate = 0;
            this.field_73260_f = 0;
        }
    }

    private void sendTileToAllPlayersWatchingChunk(TileEntity par1TileEntity)
    {
        if (par1TileEntity != null)
        {
            Packet packet = par1TileEntity.getDescriptionPacket();

            if (packet != null)
            {
                this.sendToAllPlayersWatchingChunk(packet);
            }
        }
    }

    static ChunkCoordIntPair getChunkLocation(PlayerInstance par0PlayerInstance)
    {
        return par0PlayerInstance.chunkLocation;
    }

    static List getPlayersInChunk(PlayerInstance par0PlayerInstance)
    {
        return par0PlayerInstance.playersInChunk;
    }
}
