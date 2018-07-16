package com.stool.miniredis.netty.client;

import com.stool.miniredis.netty.common.RedisInputDecoder;
import com.stool.miniredis.netty.common.RedisOutputEncoder;
import com.stool.miniredis.netty.common.redisoutput.ArrayOutput;
import com.stool.miniredis.netty.common.redisoutput.IRedisOutput;
import com.stool.miniredis.netty.common.redisoutput.StringOutput;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RedisClient {
    private final static Logger LOG = LoggerFactory.getLogger(RedisClient.class);

    private String ip;
    private int port;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private RedisClientHandler handler;

    private boolean started;
    private boolean stopped;

    public RedisClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        init();
    }

    private void init() {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup(1);
        bootstrap.group(group);
        handler = new RedisClientHandler(this);

        bootstrap.channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast(new ReadTimeoutHandler(60));
                        pipe.addLast(new RedisInputDecoder());
                        pipe.addLast(new RedisOutputEncoder());
                        pipe.addLast(handler);
                    }
                });

        bootstrap.option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }


    public Object send(String command) {
        Future<?> future = sendAsync(command);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Future<T> sendAsync(String command) {
        if (!started) {
            connect();
            started = true;
        }
        IRedisOutput output = convertToOutput(command);
        return handler.send(output);
    }

    private IRedisOutput convertToOutput(String command) {
        String[] strArray = command.split("\\s+");
        ArrayOutput output = ArrayOutput.newArray();
        for (String word : strArray) {
            IRedisOutput wordOutput = StringOutput.of(word);
            output.append(wordOutput);
        }
        return output;
    }

    public void connect() {
        bootstrap.connect(ip, port).syncUninterruptibly();
    }

    public void reconnect() {
        if (stopped) {
            return;
        }
        bootstrap.connect(ip, port).addListener(future -> {
            if (future.isSuccess()) {
                return ;
            }

            if (!stopped) {
                group.schedule(() -> {
                    reconnect();
                }, 1, TimeUnit.SECONDS);
            }
            LOG.error("connect {}:{} failure", ip, port, future.cause());
        });
    }

    public void close() {
        stopped = true;
        handler.close();
        // quietPeriod: 我们可以将静默时间看作为一段观察期，在此期间如果没有任务执行，说明可以跳出循环；如果此期间有任务执行，
        // 执行完后立即进入下一个观察期继续观察；如果连续多个观察期一直有任务执行，那么截止时间到则跳出循环。
        group.shutdownGracefully(0, 5000, TimeUnit.SECONDS);
    }
}
