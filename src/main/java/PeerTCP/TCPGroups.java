package PeerTCP;

import java.net.InetSocketAddress;

public class TCPGroups {
    public static InetSocketAddress CC(InetSocketAddress baseAddress) {
        return new InetSocketAddress(baseAddress.getAddress(), baseAddress.getPort() + 1);
    }

    public static InetSocketAddress DB(InetSocketAddress baseAddress) {
        return new InetSocketAddress(baseAddress.getAddress(), baseAddress.getPort() + 2);
    }

    public static InetSocketAddress DR(InetSocketAddress baseAddress) {
        return new InetSocketAddress(baseAddress.getAddress(), baseAddress.getPort() + 3);
    }
}
