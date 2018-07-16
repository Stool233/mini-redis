package com.stool.miniredis.netty.common;


import com.stool.miniredis.netty.common.redisoutput.IRedisOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

// 解码器是无状态的，所以它可以被channel共享
@ChannelHandler.Sharable
public class RedisOutputEncoder extends MessageToMessageEncoder<IRedisOutput> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IRedisOutput msg, List<Object> out) throws Exception {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
        msg.encode(buf);
        out.add(buf);
    }
}
