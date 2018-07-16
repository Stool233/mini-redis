package com.stool.miniredis.nio.common;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * 这个解码器存在问题
 */
@Deprecated
public class RespWrongDecoder {

    /**
     * 解析Resp
     * @param channel
     * @return
     */
    public static List<String> decode(SocketChannel channel) throws Exception{
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        List<String> wordList = new ArrayList<String>();
        byte b;
        b = readOneByte(channel, byteBuffer);
        if (b == '*') {
            int number = readInt(channel);
            // 递归解析
            while ((number--) > 0) {
                wordList.addAll(decode(channel));
            }
        } else if (b == '$') {
            int number = readInt(channel);
            wordList.add(readFixString(channel, number));
        } else if (b == '+') {
            wordList.add(readString(channel));
        }
        return wordList;
    }

    /**
     * 读取指定长度的字符串
     * @param channel
     * @param length
     * @return
     */
    public static String readFixString(SocketChannel channel, int length) throws Exception {
        int trueLength = length + 2;  //加上CRLF
        ByteBuffer byteBuffer = ByteBuffer.allocate(trueLength);
        int count;
        String result = null;
        while ((count = channel.read(byteBuffer)) > 0) {
            // 保存bytebuffer状态
            int position = byteBuffer.position();
            int limit = byteBuffer.limit();
            byteBuffer.flip();
            // 长度不够
            if (byteBuffer.remaining() < trueLength) {
                byteBuffer.position(position);
                byteBuffer.limit(limit);
                continue;
            }

//            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            result = new String(bytes, "UTF-8");

            if (result.charAt(result.length()-2) != '\r' || result.charAt(result.length()-1) != '\n') {
                throw new RuntimeException("CRLF error");
            }

            // 去掉CRLF;
            result = result.replaceAll("\r|\n", "");
            break; // 读取完了，跳出循环
        }
        return result;
    }

    /**
     * 读取数字，直到遇到CRLF
     * @param channel
     * @return
     */
    public static Integer readInt(SocketChannel channel) throws Exception{
        return Integer.parseInt(readString(channel));
    }

    /**
     * 读取字符串，直到遇到CRLF
     * @param channel
     * @return
     */
    public static String readString(SocketChannel channel) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        StringBuilder sb = new StringBuilder();
        byte b;
        while (true) {
            b = readOneByte(channel, byteBuffer);
            if (b == '\r') {
                b = readOneByte(channel, byteBuffer);
                if (b == '\n') {
                    break;  // 遇到CRLF，退出循环
                } else {
                    throw new RuntimeException("CRLF error");
                }
            } else {
                sb.append((char)b);
            }
        }
        return sb.toString();
    }

    /**
     * 读取一个字节
     * @param channel
     * @param byteBuffer 容量必须为1
     * @return
     */
    private static byte readOneByte(SocketChannel channel, ByteBuffer byteBuffer) throws Exception{
        if (byteBuffer.capacity() != 1) {
            throw new RuntimeException("capacity of byteBuffer must be 1");
        }
        int count;
        byte b;

        byteBuffer.clear();
        count = channel.read(byteBuffer);

        if (count < 0) {
            throw new RuntimeException("connection is close");
        } else if (count == 0) {
            throw new RuntimeException("read empty");
        }

        byteBuffer.flip();
        b = byteBuffer.get();

        return b;
    }
}
