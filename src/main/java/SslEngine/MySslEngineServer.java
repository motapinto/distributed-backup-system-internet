package SslEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import javax.net.ssl.SSLEngine;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;


public class MySslEngineServer extends MySslEnginePeer {
    private final Selector selector;

    private MySslServerListener serverListener;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public MySslEngineServer(InetSocketAddress address) throws IOException {
        super(address, "server.jks");
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(true);

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(hostname, port));

        selector = Selector.open();

        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void initialize() {
        System.out.println("Initialized server and waiting for connections");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try { selectKeys(); }
            catch (IOException e) {
                System.err.println("Error running SSLEngineServer: " + e.getMessage());
            }
        }, 25, 25, TimeUnit.MILLISECONDS);
    }

    public void selectKeys() throws IOException {
        selector.select();
        Set<SelectionKey> readyKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = readyKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (key.isValid() && key.isAcceptable()) {
                SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                client.configureBlocking(false);
                SSLEngine engine = sslContext.createSSLEngine();
                engine.setUseClientMode(false);
                doHandshake(engine, client);
                client.register(selector, SelectionKey.OP_READ, engine);
            } else if (key.isValid() && key.isReadable()) {
               new Thread(() -> serverListener.onReadReceived(new MySslServerConnection(key, MySslEngineServer.this))).start();
                //executor.execute(() -> serverListener.onReadReceived(new MySslServerConnection(key, MySslEngineServer.this)));
            }
        }
    }

    public void addObserver(MySslServerListener mySslServerListener) {
        this.serverListener = mySslServerListener;
    }

    public void close(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdownOutput(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}