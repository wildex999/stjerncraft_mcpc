package org.spigotmc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOperationHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundManager extends ChannelOperationHandlerAdapter {

    private static final int FLUSH_TIME = 3;
    private long lastFlush;
    public boolean flushNow = false;

    public void flush(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (flushNow || System.currentTimeMillis() - lastFlush > FLUSH_TIME) {
            lastFlush = System.currentTimeMillis();
            ctx.flush(promise);
        }
    }
}
