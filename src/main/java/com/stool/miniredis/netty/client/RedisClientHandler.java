package com.stool.miniredis.netty.client;

import com.stool.miniredis.netty.common.RedisInput;
import com.stool.miniredis.netty.common.redisoutput.IRedisOutput;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RedisClientHandler extends ChannelInboundHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(RedisClientHandler.class);

    private RedisClient client;
    private ChannelHandlerContext context;
    private RPCFuture<?> future;

    private Throwable connectionClosed = new Exception("rpc connection not active error");

    public RedisClientHandler(RedisClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.context = null;
        future.fail(connectionClosed);
        future = null;
        // 尝试重连
        ctx.channel().eventLoop().schedule(() -> {
            client.reconnect();
        }, 1, TimeUnit.SECONDS);
    }

    public <T> Future<T> send(IRedisOutput output) {
        ChannelHandlerContext ctx = context;
        RPCFuture<T> future = new RPCFuture<>();
        if (ctx != null) {
            ctx.channel().eventLoop().execute(() -> {
                this.future = future;
                ctx.writeAndFlush(output);
            });
        } else {
            future.fail(connectionClosed);
        }
        return future;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RedisInput)) {
            return ;
        }

        RedisInput input = (RedisInput) msg;
        List<String> params = input.getParams();

        if (params.size() == 1) {
            String value = params.get(0);
            @SuppressWarnings("unchecked")
            RPCFuture<Object> future = (RPCFuture<Object>) this.future;
            future.success(value);
        } else {
            throw new RuntimeException("no support");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    public void close() {
        ChannelHandlerContext ctx = context;
        if (ctx != null) {
            ctx.close();
        }
    }
}
