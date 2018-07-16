package com.stool.miniredis.netty.demo;


import com.stool.miniredis.netty.server.RedisServer;

public class ServerMain {

    public static void main(String[] args) {
        RedisServer redisServer = new RedisServer("localhost", 6379, 1);
        redisServer.start();
    }
}
