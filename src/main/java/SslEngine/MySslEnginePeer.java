package SslEngine;


import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySslEnginePeer {
    protected final int initial_buffer_size = 32000;
    protected final SSLContext sslContext;
    protected final SSLEngine engine;
    protected final int port;
    protected final String hostname;

    public MySslEnginePeer(InetSocketAddress address, String keystoreFilepath) {
        this.hostname = address.getHostName();
        this.port = address.getPort();
        sslContext = initializeSslContext(keystoreFilepath, "truststore.jks", "storepass","keypass");

        engine = sslContext.createSSLEngine(hostname, port);
    }

    public SSLContext initializeSslContext(String keystoreFilepath, String truststoreFilepath,String passphrasekeys ,String passphrasetrust) {
        SSLContext sslContext = null;

        // First initialize the key and trust material
        try {
            KeyStore ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(new FileInputStream(keystoreFilepath), passphrasekeys.toCharArray());
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(new FileInputStream(truststoreFilepath), passphrasekeys.toCharArray());


            // KeyManagers decide which key material to use
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            kmf.init(ksKeys, passphrasetrust.toCharArray());

            // TrustManagers decide whether to allow connections
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ksTrust);

            // Get an instance of SSLContext for TLS protocols
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        } catch (Exception e) {
            System.err.println("Error initializing SSLContext: " + e.toString());
        }

        return sslContext;
    }

    void doHandshake(SSLEngine engine, SocketChannel socketChannel) throws IOException {
        // Create byte buffers to use for holding application data
        SSLSession session = engine.getSession();

        ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

        // Begin handshake
        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();

        myNetData.clear();
        peerNetData.clear();

        // Process handshaking message
        while (hs != SSLEngineResult.HandshakeStatus.FINISHED &&
                hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (hs) {

                case NEED_UNWRAP:
                    // Receive handshaking data from peer
                    if (socketChannel.read(peerNetData) < 0) {
                        // The channel has reached end-of-stream
                        //engine.closeOutbound();
                        break;
                    }

                    // Process incoming handshaking data
                    peerNetData.flip();
                    SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();
                    hs = res.getHandshakeStatus();
                    // Check status
                    switch (res.getStatus()) {
                        case OK:
                            hs = engine.getHandshakeStatus();
                            break;
                        case BUFFER_UNDERFLOW:
                            // Maybe need to enlarge the peer network packet buffer
                            int netSize = engine.getSession().getPacketBufferSize();
                            // Resize buffer if needed.
                            if (netSize > peerNetData.capacity()) {
                                ByteBuffer b = ByteBuffer.allocate(netSize);
                                myAppData.flip();
                                b.put(myAppData);
                                myAppData = b;
                            } else {
                                // compact or clear the buffer
                            }
                            hs = engine.getHandshakeStatus();
                            // obtain more inbound network data and then retry the operation
                            break;

                        // Handle other status: BUFFER_UNDERFLOW, BUFFER_OVERFLOW, CLOSED
                        // ...
                    }
                    break;

                case NEED_WRAP:
                    // Empty the local network packet buffer.
                    myNetData.clear();

                    // Generate handshaking data
                    res = engine.wrap(myAppData, myNetData);
                    hs = res.getHandshakeStatus();

                    // Check status
                    switch (res.getStatus()) {
                        case OK:
                            myNetData.flip();

                            // Send the handshaking data to peer
                            while (myNetData.hasRemaining()) {
                                socketChannel.write(myNetData);
                            }
                            hs = engine.getHandshakeStatus();
                            break;

                        // Handle other status:  BUFFER_OVERFLOW, BUFFER_UNDERFLOW, CLOSED
                        // ...
                    }
                    break;

                case NEED_TASK:
                    // Handle blocking tasks
                    Runnable task = engine.getDelegatedTask();
                    if (task != null) {
                        task.run();
                    }
                    hs = engine.getHandshakeStatus();
                    break;
            }
        }

        //System.out.println("Handshake done.");
    }

    public void write(SSLEngine engine, SocketChannel socketChannel, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer myAppData = ByteBuffer.allocate(bytes.length);
        ByteBuffer myNetData = ByteBuffer.allocate(bytes.length);
        ByteBuffer peerNetData = ByteBuffer.allocate(bytes.length);

        myAppData.put(bytes);
        myAppData.flip();

        while (myAppData.hasRemaining()) {
            // Generate TLS/DTLS encoded data (handshake or application data)

            SSLEngineResult res = engine.wrap(myAppData, myNetData);

            switch (res.getStatus()) {
                case OK:
                    break;


                case BUFFER_OVERFLOW:
                    // Maybe need to enlarge the peer application data buffer.
                    ByteBuffer newNetData = ByteBuffer.allocate(myNetData.capacity() * 2); // enlarge the peer application data buffer
                    myNetData.flip();
                    newNetData.put(myNetData);
                    myNetData = newNetData;
                    break;

                case BUFFER_UNDERFLOW:
                    // Maybe need to enlarge the peer network packet buffer
                    if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                        // enlarge the peer network packet buffer
                    } else {
                        // compact or clear the buffer
                    }
                    // obtain more inbound network data and then retry the operation
                    break;

                // Handle other status: CLOSED, OK
                // ...
            }
        }

        myNetData.flip();

        while (myNetData.hasRemaining()) {
            int num = socketChannel.write(myNetData);
            //System.out.println("Sending data size: " + num);
            if (num == 0) {
                //System.out.println("Didn't write :(");
                // no bytes written; try again later
            }
        }

        myNetData.clear();
    }

    public String read(SSLEngine engine, SocketChannel socketChannel, int maxSize) throws IOException {
        ByteBuffer peerAppData = ByteBuffer.allocate(maxSize);
        ByteBuffer peerNetData = ByteBuffer.allocate(maxSize);

        // Read TLS/DTLS encoded data from peer
        String message = "";
        SSLEngineResult res;
        int num = socketChannel.read(peerNetData);

        if (num == -1) {
            return null;
            // The channel has reached end-of-stream
        } else if (num == 0) {
            // No bytes read; try again ...
        } else {
            peerNetData.flip();
            while (peerNetData.hasRemaining()) {
                // Process incoming data
                peerAppData.clear();

                res = engine.unwrap(peerNetData, peerAppData);
                switch (res.getStatus()) {
                    case OK:
                        message += new String(Arrays.copyOfRange(peerAppData.array(), 0, res.bytesProduced()), StandardCharsets.ISO_8859_1);
                        break;
                    case CLOSED:
                        engine.closeOutbound();
                        socketChannel.close();
                        break;
                    // Handle other status:  BUFFER_OVERFLOW, BUFFER_UNDERFLOW, CLOSED
                    case BUFFER_UNDERFLOW:
                        // Maybe need to enlarge the peer network packet buffer
                        if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
                            // enlarge the peer network packet buffer
                        } else {
                            peerAppData.compact();
                            peerNetData.compact();
                            // compact or clear the buffer
                        }
                        // obtain more inbound network data and then retry the operation
                        break;
                }
            }
        }

        return message.isEmpty() ? null : message;
    }
}
