package com.stool.miniredis.netty.demo;



import com.stool.miniredis.netty.client.RedisClient;

import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        RedisClient client = new RedisClient("localhost", 6379);

        while(true) {
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();
            String res = (String) client.send(command);
            System.out.println(res);
        }
    }
}
