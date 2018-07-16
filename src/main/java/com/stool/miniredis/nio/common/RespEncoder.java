package com.stool.miniredis.nio.common;

import java.nio.ByteBuffer;
import java.util.List;

public class RespEncoder {

    public static ByteBuffer encode(List<String> wordList) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // 编码*
        int length = wordList.size();
        byteBuffer.put(("*"+length).getBytes()).put("\r\n".getBytes());

        for (int i = 0; i < length; i++) {
            // 编码$
            String word = wordList.get(i);
            int wordLength = word.length();
            byteBuffer.put(("$"+wordLength).getBytes()).put("\r\n".getBytes());
            byteBuffer.put(word.getBytes()).put("\r\n".getBytes());
        }

        byteBuffer.flip();
        return byteBuffer;
    }

    public static ByteBuffer encode(String word) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // 编码+
        int length = word.length();
        byteBuffer.put(("+"+word).getBytes()).put("\r\n".getBytes());

        byteBuffer.flip();
        return byteBuffer;
    }

}
