package SslEngine;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MySslServerConnection {
    private final SSLEngine engine;
    private final SocketChannel client;
    private final MySslEngineServer server;

    public MySslServerConnection(SelectionKey key, MySslEngineServer server){
        this.client = ((SocketChannel)key.channel());
        this.engine = ((SSLEngine)key.attachment());
        this.server = server;
    }

    public String read(int maxSize) throws IOException {
        return server.read(engine, client, maxSize);
    }

    public void write(String message) throws IOException {
        server.write(engine, client, message);
    }

    public void close() {
        server.close(client);
    }

    public void shutdownOutput() {
        server.shutdownOutput(client);
    }

    public SocketAddress getRemoteAddress() {
        try {
            return client.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    public SocketAddress getLocalAddress() {
        try {
            return client.getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }
}
