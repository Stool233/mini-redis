package com.stool.miniredis.nio.demo;


import com.stool.miniredis.nio.server.RedisServer;

public class ServerMain {
    public static void main(String[] args) throws Exception{
        new RedisServer().start();
    }
}
