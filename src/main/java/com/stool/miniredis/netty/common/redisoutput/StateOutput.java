package com.stool.miniredis.netty.common.redisoutput;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class StateOutput implements IRedisOutput{

    private String state;

    public StateOutput(String state) {
        this.state = state;
    }

    public void encode(ByteBuf buf) {
        buf.writeByte('+');
        buf.writeBytes(state.getBytes(StandardCharsets.UTF_8));
        buf.writeByte('\r');
        buf.writeByte('\n');
    }

    public static StateOutput of (String state) {
        return new StateOutput(state);
    }

    public final static StateOutput OK = new StateOutput("OK");

    public final static StateOutput PONG = new StateOutput("PONG");
}
