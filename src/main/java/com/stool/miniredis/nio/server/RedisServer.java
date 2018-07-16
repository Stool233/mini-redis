package com.stool.miniredis.nio.server;

import com.stool.miniredis.nio.common.RespDecoder;
import com.stool.miniredis.nio.common.RespEncoder;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {

    private Map<String, String> redisMap = new ConcurrentHashMap<String, String>();

    public void start() throws Exception{

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        Selector selector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 绑定6379
        serverSocket.bind(new InetSocketAddress(6379));
        System.out.println("Listen to " + 6379);

        while (true) {
            int n = selector.select();
            if (n == 0) {
                continue;
            }
            Iterator it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();

                    System.out.println(socketChannel.getLocalAddress() + " accepted");

                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);


                }

                if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    if (key.attachment() == null) {
                        key.attach(new RespDecoder(socketChannel));
                    }
                    RespDecoder decoder = (RespDecoder) key.attachment();

                    try {
                        boolean isComplete = decoder.decode();
                        if (isComplete) {
                            List<String> wordList = decoder.getWordList();
                            System.out.println(Arrays.toString(wordList.toArray()));
                            String message = operate(wordList);
                            // 解码结束，清空decoder的状态
                            decoder.clear();

                            // 发送到客户端
                            send(message, socketChannel);
                        }
                    } catch (Exception e) {
                        key.cancel();
                        socketChannel.socket().close();
                        socketChannel.close();
                    }



                }
                // 清除处理过的键
                it.remove();
            }
        }
    }

    private String operate(List<String> wordList) throws Exception{
        String result = null;
        if (wordList.get(0).equals("set")) {
            redisMap.put(wordList.get(1), wordList.get(2));
            result = "OK";
        } else if (wordList.get(0).equals("get")) {
            result = redisMap.get(wordList.get(1));
        }
        return result;
    }

    private void send(String message, SocketChannel channel) throws Exception{
        ByteBuffer writeBuffer = RespEncoder.encode(message);

        while (writeBuffer.hasRemaining()) {
            channel.write(writeBuffer);
        }
    }
}
