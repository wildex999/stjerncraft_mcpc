package net.minecraft.server.management;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
// CraftBukkit start
import java.util.Collections;
import java.util.Queue;
import java.util.Iterator;
import java.util.LinkedList;
// CraftBukkit end

public class PlayerManager
{
    private final WorldServer theWorldServer;

    /** players in the current instance */
    private final List players = new ArrayList();

    /**
     * A map of chunk position (two ints concatenated into a long) to PlayerInstance
     */
    private final LongHashMap playerInstances = new LongHashMap();

    /**
     * contains a PlayerInstance for every chunk they can see. the "player instance" cotains a list of all players who
     * can also that chunk
     */
    private final Queue chunkWatcherWithPlayers = new java.util.concurrent.ConcurrentLinkedQueue(); // CraftBukkit ArrayList -> ConcurrentLinkedQueue

    /**
     * Number of chunks the server sends to the client. Valid 3<=x<=15. In server.properties.
     */
    private final int playerViewRadius;

    /** x, z direction vectors: east, south, west, north */
    private final int[][] xzDirectionsConst = new int[][] {{1, 0}, {0, 1}, { -1, 0}, {0, -1}};
    private boolean wasNotEmpty; // CraftBukkit

    public PlayerManager(WorldServer par1WorldServer, int par2)
    {
        if (par2 > 15)
        {
            throw new IllegalArgumentException("Too big view radius!");
        }
        else if (par2 < 3)
        {
            throw new IllegalArgumentException("Too small view radius!");
        }
        else
        {
            this.playerViewRadius = par2;
            this.theWorldServer = par1WorldServer;
        }
    }

    public WorldServer getWorldServer()
    {
        return this.theWorldServer;
    }

    /**
     * updates all the player instances that need to be updated
     */
    public void updatePlayerInstances()
    {
        // CraftBukkit start - use iterator
        Iterator iterator = this.chunkWatcherWithPlayers.iterator();

        while (iterator.hasNext())
        {
            PlayerInstance playerinstance = (PlayerInstance) iterator.next();
            playerinstance.sendChunkUpdate();
            iterator.remove();
        }

        // CraftBukkit end

        // this.d.clear(); // CraftBukkit - removals are already covered
        if (this.players.isEmpty())
        {
            if (!wasNotEmpty)
            {
                return;    // CraftBukkit - only do unload when we go from non-empty to empty
            }

            WorldProvider var1 = this.theWorldServer.provider;

            if (!var1.canRespawnHere())
            {
                this.theWorldServer.theChunkProviderServer.unloadAllChunks();
            }

            // CraftBukkit start
            wasNotEmpty = false;
        }
        else
        {
            wasNotEmpty = true;
        }

        // CraftBukkit end
    }

    private PlayerInstance getOrCreateChunkWatcher(int par1, int par2, boolean par3)
    {
        long var4 = (long)par1 + 2147483647L | (long)par2 + 2147483647L << 32;
        PlayerInstance var6 = (PlayerInstance)this.playerInstances.getValueByKey(var4);

        if (var6 == null && par3)
        {
            var6 = new PlayerInstance(this, par1, par2);
            this.playerInstances.add(var4, var6);
        }

        return var6;
    }
    // CraftBukkit start
    public final boolean isChunkInUse(int x, int z)
    {
        PlayerInstance pi = getOrCreateChunkWatcher(x, z, false);

        if (pi != null)
        {
            return (PlayerInstance.getPlayersInChunk(pi).size() > 0);
        }

        return false;
    }
    // CraftBukkit end

    /**
     * the "PlayerInstance"/ chunkWatcher will send this chunk to all players who are in line of sight
     */
    public void flagChunkForUpdate(int par1, int par2, int par3)
    {
        int var4 = par1 >> 4;
        int var5 = par3 >> 4;
        PlayerInstance var6 = this.getOrCreateChunkWatcher(var4, var5, false);

        if (var6 != null)
        {
            var6.flagChunkForUpdate(par1 & 15, par2, par3 & 15);
        }
    }

    /**
     * Adds an EntityPlayerMP to the PlayerManager.
     */
    public void addPlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        int var2 = (int)par1EntityPlayerMP.posX >> 4;
        int var3 = (int)par1EntityPlayerMP.posZ >> 4;
        par1EntityPlayerMP.managedPosX = par1EntityPlayerMP.posX;
        par1EntityPlayerMP.managedPosZ = par1EntityPlayerMP.posZ;
        // CraftBukkit start - load nearby chunks first
        List<ChunkCoordIntPair> chunkList = new LinkedList<ChunkCoordIntPair>();

        for (int k = var2 - this.playerViewRadius; k <= var2 + this.playerViewRadius; ++k)
        {
            for (int l = var3 - this.playerViewRadius; l <= var3 + this.playerViewRadius; ++l)
            {
                chunkList.add(new ChunkCoordIntPair(k, l));
            }
        }

        Collections.sort(chunkList, new ChunkCoordComparator(par1EntityPlayerMP));

        for (ChunkCoordIntPair pair : chunkList)
        {
            this.getOrCreateChunkWatcher(pair.chunkXPos, pair.chunkZPos, true).addPlayerToChunkWatchingList(par1EntityPlayerMP);
        }

        // CraftBukkit end
        this.players.add(par1EntityPlayerMP);
        this.filterChunkLoadQueue(par1EntityPlayerMP);
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    public void filterChunkLoadQueue(EntityPlayerMP par1EntityPlayerMP)
    {
        ArrayList var2 = new ArrayList(par1EntityPlayerMP.loadedChunks);
        int var3 = 0;
        int var4 = this.playerViewRadius;
        int var5 = (int)par1EntityPlayerMP.posX >> 4;
        int var6 = (int)par1EntityPlayerMP.posZ >> 4;
        int var7 = 0;
        int var8 = 0;
        ChunkCoordIntPair var9 = PlayerInstance.getChunkLocation(this.getOrCreateChunkWatcher(var5, var6, true));
        par1EntityPlayerMP.loadedChunks.clear();

        if (var2.contains(var9))
        {
            par1EntityPlayerMP.loadedChunks.add(var9);
        }

        int var10;

        for (var10 = 1; var10 <= var4 * 2; ++var10)
        {
            for (int var11 = 0; var11 < 2; ++var11)
            {
                int[] var12 = this.xzDirectionsConst[var3++ % 4];

                for (int var13 = 0; var13 < var10; ++var13)
                {
                    var7 += var12[0];
                    var8 += var12[1];
                    var9 = PlayerInstance.getChunkLocation(this.getOrCreateChunkWatcher(var5 + var7, var6 + var8, true));

                    if (var2.contains(var9))
                    {
                        par1EntityPlayerMP.loadedChunks.add(var9);
                    }
                }
            }
        }

        var3 %= 4;

        for (var10 = 0; var10 < var4 * 2; ++var10)
        {
            var7 += this.xzDirectionsConst[var3][0];
            var8 += this.xzDirectionsConst[var3][1];
            var9 = PlayerInstance.getChunkLocation(this.getOrCreateChunkWatcher(var5 + var7, var6 + var8, true));

            if (var2.contains(var9))
            {
                par1EntityPlayerMP.loadedChunks.add(var9);
            }
        }
    }

    /**
     * Removes an EntityPlayerMP from the PlayerManager.
     */
    public void removePlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        int var2 = (int)par1EntityPlayerMP.managedPosX >> 4;
        int var3 = (int)par1EntityPlayerMP.managedPosZ >> 4;

        for (int var4 = var2 - this.playerViewRadius; var4 <= var2 + this.playerViewRadius; ++var4)
        {
            for (int var5 = var3 - this.playerViewRadius; var5 <= var3 + this.playerViewRadius; ++var5)
            {
                PlayerInstance var6 = this.getOrCreateChunkWatcher(var4, var5, false);

                if (var6 != null)
                {
                    var6.sendThisChunkToPlayer(par1EntityPlayerMP);
                }
            }
        }

        this.players.remove(par1EntityPlayerMP);
    }

    private boolean func_72684_a(int par1, int par2, int par3, int par4, int par5)
    {
        int var6 = par1 - par3;
        int var7 = par2 - par4;
        return var6 >= -par5 && var6 <= par5 ? var7 >= -par5 && var7 <= par5 : false;
    }

    /**
     * update chunks around a player being moved by server logic (e.g. cart, boat)
     */
    public void updateMountedMovingPlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        int var2 = (int)par1EntityPlayerMP.posX >> 4;
        int var3 = (int)par1EntityPlayerMP.posZ >> 4;
        double var4 = par1EntityPlayerMP.managedPosX - par1EntityPlayerMP.posX;
        double var6 = par1EntityPlayerMP.managedPosZ - par1EntityPlayerMP.posZ;
        double var8 = var4 * var4 + var6 * var6;

        if (var8 >= 64.0D)
        {
            int var10 = (int)par1EntityPlayerMP.managedPosX >> 4;
            int var11 = (int)par1EntityPlayerMP.managedPosZ >> 4;
            int var12 = this.playerViewRadius;
            int var13 = var2 - var10;
            int var14 = var3 - var11;
            List<ChunkCoordIntPair> var15 = new LinkedList<ChunkCoordIntPair>(); // CraftBukkit

            if (var13 != 0 || var14 != 0)
            {
                for (int var16 = var2 - var12; var16 <= var2 + var12; ++var16)
                {
                    for (int var17 = var3 - var12; var17 <= var3 + var12; ++var17)
                    {
                        if (!this.func_72684_a(var16, var17, var10, var11, var12))
                        {
                            var15.add(new ChunkCoordIntPair(var16, var17)); // CraftBukkit
                        }

                        if (!this.func_72684_a(var16 - var13, var17 - var14, var2, var3, var12))
                        {
                            PlayerInstance playerchunk = this.getOrCreateChunkWatcher(var16 - var13, var17 - var14, false);

                            if (playerchunk != null)
                            {
                                playerchunk.sendThisChunkToPlayer(par1EntityPlayerMP);
                            }
                        }
                    }
                }

                this.filterChunkLoadQueue(par1EntityPlayerMP);
                par1EntityPlayerMP.managedPosX = par1EntityPlayerMP.posX;
                par1EntityPlayerMP.managedPosZ = par1EntityPlayerMP.posZ;
                // CraftBukkit start - send nearest chunks first
                Collections.sort(var15, new ChunkCoordComparator(par1EntityPlayerMP));

                for (ChunkCoordIntPair pair : var15)
                {
                    this.getOrCreateChunkWatcher(pair.chunkXPos, pair.chunkZPos, true).addPlayerToChunkWatchingList(par1EntityPlayerMP);
                }

                if (var12 > 1 || var12 < -1 || var13 > 1 || var13 < -1)
                {
                    Collections.sort(par1EntityPlayerMP.loadedChunks, new ChunkCoordComparator(par1EntityPlayerMP));
                }

                // CraftBukkit end
            }
        }
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP par1EntityPlayerMP, int par2, int par3)
    {
        PlayerInstance var4 = this.getOrCreateChunkWatcher(par2, par3, false);
        return var4 == null ? false : PlayerInstance.getPlayersInChunk(var4).contains(par1EntityPlayerMP) && !par1EntityPlayerMP.loadedChunks.contains(PlayerInstance.getChunkLocation(var4));
    }

    /**
     * Get the furthest viewable block given player's view distance
     */
    public static int getFurthestViewableBlock(int par0)
    {
        return par0 * 16 - 16;
    }

    static WorldServer getWorldServer(PlayerManager par0PlayerManager)
    {
        return par0PlayerManager.theWorldServer;
    }

    static LongHashMap getChunkWatchers(PlayerManager par0PlayerManager)
    {
        return par0PlayerManager.playerInstances;
    }

    static Queue getChunkWatchersWithPlayers(PlayerManager playermanager)   // CraftBukkit List -> Queue
    {
        return playermanager.chunkWatcherWithPlayers;
    }

    // CraftBukkit start - sorter to load nearby chunks first
    private static class ChunkCoordComparator implements java.util.Comparator<ChunkCoordIntPair>
    {
        private int x;
        private int z;

        public ChunkCoordComparator(EntityPlayerMP entityplayer)
        {
            x = (int) entityplayer.posX >> 4;
            z = (int) entityplayer.posZ >> 4;
        }

        public int compare(ChunkCoordIntPair a, ChunkCoordIntPair b)
        {
            if (a.equals(b))
            {
                return 0;
            }

            // Subtract current position to set center point
            int ax = a.chunkXPos - this.x;
            int az = a.chunkZPos - this.z;
            int bx = b.chunkXPos - this.x;
            int bz = b.chunkZPos - this.z;
            int result = ((ax - bx) * (ax + bx)) + ((az - bz) * (az + bz));

            if (result != 0)
            {
                return result;
            }

            if (ax < 0)
            {
                if (bx < 0)
                {
                    return bz - az;
                }
                else
                {
                    return -1;
                }
            }
            else
            {
                if (bx < 0)
                {
                    return 1;
                }
                else
                {
                    return az - bz;
                }
            }
        }
    }
    // CraftBukkit end
}
