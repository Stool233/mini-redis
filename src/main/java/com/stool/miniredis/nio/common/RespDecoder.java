package com.stool.miniredis.nio.common;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class RespDecoder {

    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    private SocketChannel channel;

    // 是否只拿到了CR，LF还没拿到
    private boolean isOnlyGetCR;

    // 字符串缓冲区
    private StringBuilder sb = new StringBuilder();

    // 目前的还没读取的定长字符串长度
    private int stringLength;

    // 目前读取到的字符串数组
    private List<String> wordList = new ArrayList<String>();

    // 目前读取到的mark
    private byte mark;

    // 目前还需读取到数组中的字符串数量
    private int size;

    public RespDecoder(SocketChannel channel) {
        this.channel = channel;
        init();
    }

    private void init() {
        byteBuffer.clear();
        byteBuffer.flip();
        isOnlyGetCR = false;
        sb.setLength(0);
        stringLength = 0;
        wordList.clear();
        mark = 0;
        size = 0;
    }

    public boolean decode() throws Exception {
        boolean isComplete = true;
        try {
            decode0();
        } catch (ReadEmptyException e) {
            // 捕获到异常，则返回false，提示调用者目前还未解码完成。
            isComplete = false;
        }
        return isComplete;
    }

    private void decode0() throws Exception {
        // mark默认为0，若不为0，则为中间状态
        mark = (mark != 0 ? mark : readOneByte());
        if (mark == '*') {
            // size默认为0，若不为0，则为中间状态
            size = (size != 0 ? size : readInteger());

            while ((size--) > 0) {
                decode0();   // 递归解析
            }
        } else if (mark == '$') {
            // stringLength默认为0，若不为0，则之前读取了一部分的字符串
            stringLength = (stringLength != 0 ? stringLength : readInteger() + 2);     // +2 : 加上CRLF
            readFixString();
            // 没有抛异常就保存字符串
            saveString();
        } else if (mark == '+') {
            readString();
            // 没有抛异常就保存字符串
            saveString();
        }
    }

    private void saveString() {
        wordList.add(sb.toString());
        sb.setLength(0);
        mark = 0;
    }

    public void clear() {
        isOnlyGetCR = false;
        sb.setLength(0);
        stringLength = 0;
        wordList.clear();
        mark = 0;
        size = 0;
    }


    /**
     * 读取字符串，直到遇到CRLF
     * @return
     */
    private void readString() throws Exception {
        readToBuffer();

        byte b;
        boolean getCR = isOnlyGetCR;
        while (byteBuffer.hasRemaining()) {
            b = byteBuffer.get();
            if (b == '\r') {
                getCR = true;
                continue;
            }
            if (getCR && b == '\n') {
                return ;   // 读取完成
            }
            sb.append((char)b);
        }

        if (getCR) {    // 只读取到了CR，还没读取到LF
            isOnlyGetCR = true;
            readString();
        } else {    // 读取一半
            readString();
        }

    }

    /**
     * 读一个byte
     * @return
     */
    private byte readOneByte() throws Exception{
        readToBuffer();

        byte b = byteBuffer.get();
        return b;
    }

    /**
     * 读一个整数
     * @return
     */
    private Integer readInteger() throws Exception{
        readString();

        Integer number = Integer.parseInt(sb.toString());
        sb.setLength(0);
        mark = 0;
        return number;
    }

    /**
     * 读一个定长字符串
     * @return
     */
    private void readFixString() throws Exception{

        readToBuffer();

        byte[] bytes;
        if (byteBuffer.remaining() < stringLength) {
            int currentSize = byteBuffer.remaining();
            bytes = new byte[currentSize];
            byteBuffer.get(bytes);
            String str = new String(bytes, "UTF-8");
            sb.append(str);
            stringLength -= currentSize;
            readFixString();     // 递归读取剩余的数据
        } else {
            bytes = new byte[stringLength];
            byteBuffer.get(bytes);
            String str = new String(bytes, "UTF-8");
            sb.append(str.replaceAll("\r|\n", ""));     // 去除CRLF
            stringLength = 0;
        }

    }

    /**
     * 若byteBuffer为空，读取数据到byteBuffer
     * @throws Exception
     */
    private void readToBuffer() throws Exception{
        if (!byteBuffer.hasRemaining()) {
            byteBuffer.clear();
            int count = channel.read(byteBuffer);
            if (count < 0) {
                throw new RuntimeException("connection error");
            } else if (count == 0) {
                throw new ReadEmptyException("read empty");
            }
            byteBuffer.flip();
        }
    }

    public List<String> getWordList() {
        return wordList;
    }


    public static class ReadEmptyException extends RuntimeException {
        public ReadEmptyException(String message) {
            super(message);
        }

        public ReadEmptyException() {
            super();
        }
    }


}
