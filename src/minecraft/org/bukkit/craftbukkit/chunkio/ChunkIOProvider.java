package org.bukkit.craftbukkit.chunkio;


import org.bukkit.Server;
import org.bukkit.craftbukkit.util.AsynchronousExecutor;
import org.bukkit.craftbukkit.util.LongHash;

import java.util.concurrent.atomic.AtomicInteger;

class ChunkIOProvider implements AsynchronousExecutor.CallBackProvider<QueuedChunk, net.minecraft.world.chunk.Chunk/*was:Chunk*/, Runnable, RuntimeException> {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    // async stuff
    public net.minecraft.world.chunk.Chunk/*was:Chunk*/ callStage1(QueuedChunk queuedChunk) throws RuntimeException {
        net.minecraft.world.chunk.storage.AnvilChunkLoader/*was:ChunkRegionLoader*/ loader = queuedChunk.loader;
        Object[] data = loader.loadChunk__Async_CB/*was:loadChunk*/(queuedChunk.world, LongHash.msw(queuedChunk.coords), LongHash.lsw(queuedChunk.coords));

        if (data != null) {
            queuedChunk.compound = (net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/) data[1];
            return (net.minecraft.world.chunk.Chunk/*was:Chunk*/) data[0];
        }

        return null;
    }

    // sync stuff
    public void callStage2(QueuedChunk queuedChunk, net.minecraft.world.chunk.Chunk/*was:Chunk*/ chunk) throws RuntimeException {
        if(chunk == null) {
            // If the chunk loading failed just do it synchronously (may generate)
            queuedChunk.provider.loadChunk/*was:getChunkAt*/(LongHash.msw(queuedChunk.coords), LongHash.lsw(queuedChunk.coords));
            return;
        }

        int x = LongHash.msw(queuedChunk.coords);
        int z = LongHash.lsw(queuedChunk.coords);

        // See if someone already loaded this chunk while we were working on it (API, etc)
        if (queuedChunk.provider.loadedChunkHashMap/*was:chunks*/.containsKey(queuedChunk.coords)) {
            // Make sure it isn't queued for unload, we need it
            queuedChunk.provider.chunksToUnload/*was:unloadQueue*/.remove(x, z);
            return;
        }

        queuedChunk.loader.loadEntities(chunk, queuedChunk.compound.getCompoundTag/*was:getCompound*/("Level"), queuedChunk.world);
        chunk.lastSaveTime/*was:n*/ = queuedChunk.provider.worldObj/*was:world*/.getTotalWorldTime/*was:getTime*/();
        queuedChunk.provider.loadedChunkHashMap/*was:chunks*/.put(queuedChunk.coords, chunk);
        queuedChunk.provider.loadedChunks.add(chunk); // MCPC+  vanilla compatibility
        chunk.onChunkLoad/*was:addEntities*/();

        if (queuedChunk.provider.currentChunkProvider/*was:chunkProvider*/ != null) {
            queuedChunk.provider.currentChunkProvider/*was:chunkProvider*/.recreateStructures/*was:recreateStructures*/(x, z);
        }

        Server server = queuedChunk.provider.worldObj/*was:world*/.getServer();
        if (server != null) {
            server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, false));
        }
        
        chunk.populateChunk/*was:a*/(queuedChunk.provider, queuedChunk.provider, x, z);
    }

    public void callStage3(QueuedChunk queuedChunk, net.minecraft.world.chunk.Chunk/*was:Chunk*/ chunk, Runnable runnable) throws RuntimeException {
        runnable.run();
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "Chunk I/O Executor Thread-" + threadNumber.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
