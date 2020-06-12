package SslEngine;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MySslEngineClient extends MySslEnginePeer implements AutoCloseable {
    private final SocketChannel socketChannel;

    public MySslEngineClient(InetSocketAddress address) throws IOException {
        super(address,"client.jks");
        socketChannel = connectSocket();
        engine.setUseClientMode(true);

        // Do initial handshake
        doHandshake(engine, socketChannel);
    }

    public SocketChannel connectSocket() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(hostname, port));
        while (!socketChannel.finishConnect()) { }
        return socketChannel;
    }

    @Override
    public void close() {
        SSLSession session = engine.getSession();
        ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());

        engine.closeOutbound();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                if (engine.isOutboundDone()) {
                    executor.shutdown();
                    socketChannel.close();
                    return;
                }

                engine.wrap(myAppData, myNetData);

                myNetData.flip();

                // Send close message to peer
                while (myNetData.hasRemaining()) {
                    int num = socketChannel.write(myNetData);
                    if (num == 0) {
                        // no bytes written; try again later
                    }
                }
            } catch (IOException ignored) { }
        }, 10, 10, TimeUnit.MILLISECONDS);
      //  executor.schedule(() -> tryClose(myAppData, myNetData, executor), 10, TimeUnit.MILLISECONDS);
    }
/*
    private void tryClose(ByteBuffer myAppData, ByteBuffer myNetData, ScheduledExecutorService executor) {
        try {
            if (engine.isOutboundDone()) {
                socketChannel.close();
                return;
            }

            engine.wrap(myAppData, myNetData);

            myNetData.flip();

            // Send close message to peer
            while (myNetData.hasRemaining()) {
                int num = socketChannel.write(myNetData);
                if (num == 0) {
                    // no bytes written; try again later
                }
            }
        } catch (IOException ignored) { }

        executor.schedule(() -> tryClose(myAppData, myNetData, executor), 10, TimeUnit.MILLISECONDS);
    }

 */

    public void write(String message) throws IOException {
        this.write(engine, socketChannel, message);
    }

    public String read(int maxSize) throws IOException {
        String message;
        do {
            message = this.read(engine, socketChannel, maxSize);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(message == null);
        return message;
    }

    public String writeRead(String message, int maxSize) throws IOException {
        this.write(message);
        this.shutdownOutput();
        return this.read(maxSize);
    }

    public void shutdownOutput() {
        this.engine.closeOutbound();
    }
}
