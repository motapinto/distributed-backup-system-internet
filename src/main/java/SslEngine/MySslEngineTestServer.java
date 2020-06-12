package SslEngine;


import java.net.InetSocketAddress;

public class MySslEngineTestServer {
    public static void main(String[] args) throws Exception {
        InetSocketAddress address = new InetSocketAddress("localhost",9223);
        MySslEngineServer server = new MySslEngineServer(address);
        server.addObserver(new MySslServerListenerSimple());
        server.initialize();
    }
}
