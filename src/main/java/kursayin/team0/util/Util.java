package kursayin.team0.util;

import aca.proto.ChatMsg;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;


public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static boolean write(ByteBuffer byteBuffer, ChatMsg chatMsg) {

        byte[] bytes = chatMsg.toByteArray();
        int totalBytesForMessage = 4 + bytes.length;

        if (byteBuffer.remaining() < totalBytesForMessage) {
            return false;
        }

        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        return true;


//        byte[] bytes = chatMsg.toByteArray();
//        byteBuffer.putInt(bytes.length);
//        byteBuffer.put(bytes);


    }


    public static ChatMsg read(ByteBuffer byteBuffer) {


        while (true) {
            if (byteBuffer.remaining() <= 4) {
                continue;

            }

            byteBuffer.mark();
            int length = byteBuffer.getInt();

            if (byteBuffer.remaining() < length) {
                byteBuffer.reset();
                continue;
            }

            byte[] bytes = new byte[length];
            byteBuffer.get(bytes);

            try {
                return ChatMsg.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Invalid message");
                return null;
            }

        }
    }


}
