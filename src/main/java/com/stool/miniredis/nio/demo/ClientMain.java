package com.stool.miniredis.nio.demo;

import com.stool.miniredis.nio.client.RedisClient;

public class ClientMain {

    public static void main(String[] args) throws Exception{
        new RedisClient().start();
    }
}
