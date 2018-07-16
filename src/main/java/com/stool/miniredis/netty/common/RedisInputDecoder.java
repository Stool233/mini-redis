package com.stool.miniredis.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class InputState {
    public int index;
    public int mark;

    public InputState(int index, int mark) {
        this.index = index;
        this.mark = mark;
    }
}

public class RedisInputDecoder extends ReplayingDecoder<InputState> {

    private int length;
    private List<String> params;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        InputState state = this.state();    // 得到检查点
        if (state == null) {
            int mark = getMark(in);
            if (mark == PLUS || mark == DOLLAR) {
                this.params = new ArrayList<>(1);
                state = new InputState(0, mark);
                this.checkpoint(state);
            } else {
                length = readParamsLen(in);
                this.params = new ArrayList<>(length);
                state = new InputState(0, mark);
                this.checkpoint(state);
            }
        }
        if (state.mark == PLUS) {
            String strLine = readStrLine(in);
            this.params.add(strLine);
        } else if (state.mark == DOLLAR) {
            String param = readParam(in);
            this.params.add(param);
        } else {
            for (int i = state.index; i < length; i++) {    // 从检查点开始继续解码
                String param = readParam(in);
                this.params.add(param);
                state.index = state.index + 1;
                this.checkpoint(state);
            }
        }
        out.add(new RedisInput(params));
        this.checkpoint(null);      // 解码结束，清除检查点
    }

    private final static int CR = '\r';
    private final static int LF = '\n';
    private final static int DOLLAR = '$';
    private final static int ASTERISK = '*';
    private final static int PLUS = '+';

    private int getMark(ByteBuf in) {
        int mark = in.getByte(in.readerIndex());
        return mark;
    }

    private int readParamsLen(ByteBuf in) {
        int c = in.readByte();
        if (c != ASTERISK) {
            throw new DecoderException("expect character *");
        }
        int len = readLen(in, 3);
        if (len == 0) {
            throw new DecoderException("expect non-zero params");
        }
        return len;
    }

    private String readParam(ByteBuf in) {
        int len = readStrLen(in);
        return readStr(in, len);
    }

    private String readStrLine(ByteBuf in) {
        int c = in.readByte();
        if (c != PLUS) {
            throw new DecoderException("expect charactor +");
        }
        return readStrLine(in, 1024);
    }

    private String readStrLine(ByteBuf in, int maxBytes) {

        byte[] str = new byte[maxBytes];
        int len = 0;
        while (true) {
            byte c = in.getByte(in.readerIndex());  // getByte 不会移动readerIndex
            if (!Character.isLetter(c)) {
                break;
            }
            in.readByte();
            str[len] = c;
            len++;
            if (len > maxBytes) {
                throw new DecoderException("params length too large");
            }
        }
        skipCrlf(in);
        if (len == 0) {
            throw new DecoderException("expect letter");
        }
        return new String(str, 0, len);
    }

    private String readStr(ByteBuf in, int len) {
        if (len == 0) {
            return "";
        }
        byte[] cs = new byte[len];
        in.readBytes(cs);
        skipCrlf(in);
        return new String(cs, StandardCharsets.UTF_8);
    }

    private int readStrLen(ByteBuf in) {
        int c = in.readByte();
        if (c != DOLLAR) {
            throw new DecoderException("expect charactor $");
        }
        return readLen(in, 6);  // string maxlen 999999
    }

    private int readLen(ByteBuf in, int maxBytes) {
        byte[] digits = new byte[maxBytes];
        int len = 0;
        while (true) {
            byte d = in.getByte(in.readerIndex());  // getByte 不会移动readerIndex
            if (!Character.isDigit(d)) {
                break;
            }
            in.readByte();
            digits[len] = d;
            len++;
            if (len > maxBytes) {
                throw new DecoderException("params length too large");
            }
        }
        skipCrlf(in);
        if (len == 0) {
            throw new DecoderException("expect digit");
        }
        return Integer.parseInt(new String(digits, 0, len));
    }

    private void skipCrlf(ByteBuf in) {
        int c = in.readByte();
        if (c == CR){
            c = in.readByte();
            if (c == LF) {
                return ;
            }
        }
        throw new DecoderException("expect cr lf");
    }
}
