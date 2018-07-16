package com.stool.miniredis.netty.server;


import com.stool.miniredis.netty.common.RedisInput;
import com.stool.miniredis.netty.common.redisoutput.StateOutput;
import com.stool.miniredis.netty.common.redisoutput.StringOutput;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class RedisServerHandler extends ChannelInboundHandlerAdapter {

    private final static String GET = "get";
    private final static String SET = "set";

    private Map<String, String> redisMap = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("connection comes");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("connection leaves");
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RedisInput)) {
            return ;
        }
        RedisInput redisInput = (RedisInput) msg;
        List<String> params = redisInput.getParams();
        if (params.size() < 2) {
            throw new RuntimeException("params size error");
        }
        if (params.get(0).equals(GET)) {
            String res = redisMap.get(params.get(1));
            ctx.writeAndFlush(StringOutput.of(res));     // 发送到下一个处理器
        } else if (params.get(0).equals(SET)) {
            redisMap.put(params.get(1), params.get(2));
            ctx.writeAndFlush(StateOutput.OK);  // 发送到下一个处理器
        } else {
            throw new RuntimeException("not support operate:" + params.get(0));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 此处可能因为客户端机器突发重启
        // 也可能是因为客户端链接闲置时间超时，后面的ReadTimeoutHandler抛出来的异常
        // 也可能是消息协议错误，序列化异常
        // 不管他，链接统统关闭，反正客户端具有重连机制
        System.out.println("connection error");
        cause.printStackTrace();
        ctx.close();
    }
}
