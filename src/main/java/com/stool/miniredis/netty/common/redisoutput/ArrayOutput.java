package com.stool.miniredis.netty.common.redisoutput;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ArrayOutput implements IRedisOutput {

    private List<IRedisOutput> outputs = new ArrayList<>();

    public static ArrayOutput newArray() {
        return new ArrayOutput();
    }

    public ArrayOutput append(IRedisOutput output) {
        outputs.add(output);
        return this;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte('*');
        buf.writeBytes(String.valueOf(outputs.size()).getBytes(StandardCharsets.UTF_8));
        buf.writeByte('\r');
        buf.writeByte('\n');
        for (IRedisOutput output : outputs) {
            output.encode(buf);
        }
    }
}
