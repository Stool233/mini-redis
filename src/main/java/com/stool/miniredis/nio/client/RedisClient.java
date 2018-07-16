package com.stool.miniredis.nio.client;

import com.stool.miniredis.nio.common.RespDecoder;
import com.stool.miniredis.nio.common.RespWrongDecoder;
import com.stool.miniredis.nio.common.RespEncoder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class RedisClient {

    public void start() throws Exception{
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 6379));

        while (true) {

            int n = selector.select();
            if (n == 0) {
                continue;
            }
            Iterator it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                SocketChannel channel = (SocketChannel) key.channel();
                if (key.isConnectable() && channel.finishConnect()) {
                    channel.register(selector, SelectionKey.OP_READ);

                    // 发送到服务器
                    sendToServer(channel);
                }
                if (key.isReadable()) {

                    // 读取服务器回应
                    if (key.attachment() == null) {
                        key.attach(new RespDecoder(channel));
                    }
                    RespDecoder decoder = (RespDecoder) key.attachment();

                    if (decoder.decode()) {
                        List<String> wordList = decoder.getWordList();
                        System.out.println(Arrays.toString(wordList.toArray()));
                        // 解码结束，清空decoder的状态
                        decoder.clear();

                        // 发送到服务器
                        sendToServer(channel);
                    }

                }

                it.remove();
            }
        }
    }

    private void sendToServer(SocketChannel channel) throws Exception{
        // 控制台输入指令
        Scanner scanner = new Scanner(System.in);
        String str = scanner.nextLine();

        String[] strArray = str.split("\\s+");
        List<String> wordList = Arrays.asList(strArray);
        ByteBuffer byteBuffer = RespEncoder.encode(wordList);

        // 发送到服务器
        while (byteBuffer.hasRemaining()) {
            channel.write(byteBuffer);
        }
    }
}
