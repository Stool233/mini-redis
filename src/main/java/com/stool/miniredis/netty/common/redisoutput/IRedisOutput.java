package com.stool.miniredis.netty.common.redisoutput;

import io.netty.buffer.ByteBuf;

public interface IRedisOutput {
    public void encode(ByteBuf buf);
}
