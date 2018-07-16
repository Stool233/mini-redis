package com.stool.miniredis.netty.server;

import com.stool.miniredis.netty.common.RedisInputDecoder;
import com.stool.miniredis.netty.common.RedisOutputEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class RedisServer {
    private String ip;
    private int port;
    private int ioThreads;

    private ServerBootstrap bootstrap;
    private EventLoopGroup group;
    private Channel serverChannel;

    public RedisServer(String ip, int port, int ioThreads) {
        this.ip = ip;
        this.port = port;
        this.ioThreads = ioThreads;
    }

    public void start() {
        bootstrap = new ServerBootstrap();
        group = new NioEventLoopGroup(ioThreads);
        bootstrap.group(group);
        bootstrap.channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast(new ReadTimeoutHandler(60));
                        pipe.addLast(new RedisInputDecoder());
                        pipe.addLast(new RedisOutputEncoder());
                        pipe.addLast(new RedisServerHandler());
                    }
                });

        bootstrap.option(ChannelOption.SO_BACKLOG, 100)         // 客户端套接字接受队列大小
                .option(ChannelOption.SO_REUSEADDR, true)      // reuse addr 避免端口冲突
                .option(ChannelOption.TCP_NODELAY, true)       // 关闭小流合并，保证消息的即时性
                .childOption(ChannelOption.SO_KEEPALIVE, true);     // 长时间没动静的连接自动关闭

        serverChannel = bootstrap.bind(this.ip, this.port).channel();
        System.out.printf("server started @ %s:%d\n", ip, port);
    }

    public void stop() {
        // 先关闭服务端套接字
        serverChannel.close();
        // 再斩断消息来源，停止io线程池
        group.shutdownGracefully();
    }


}
