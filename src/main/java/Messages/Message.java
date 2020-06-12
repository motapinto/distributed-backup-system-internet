package Messages;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public abstract class Message {
    public static final int MAX_SIZE = 65536;


    private final static char CR  = (char) 0x0D;
    private final static char LF  = (char) 0x0A;
    private final static String CRLF  = "" + CR + LF;
    public final static String HEADER_ENDING  = CRLF + CRLF;

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    public static byte[] toBytes(String string) {
        return string.getBytes(StandardCharsets.ISO_8859_1);
    }

    public abstract Message process(MessageProcessor processor);
}
