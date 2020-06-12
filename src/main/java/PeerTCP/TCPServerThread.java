package PeerTCP;

import Chord.LocalNode;
import Messages.Message;
import Messages.MessageParser;
import SslEngine.MySslEngineServer;
import SslEngine.MySslServerConnection;
import SslEngine.MySslServerListener;
import StateLogging.PeerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TCPServerThread extends Thread implements MySslServerListener  {
    private final String threadName;
    private final LocalNode localNode;
    private final PeerState peerState;
    private final MySslEngineServer server;

    private static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(15);

    public TCPServerThread(String threadName, int port, PeerState peerState) {
        this.threadName = threadName;
        this.peerState = peerState;
        this.localNode = peerState.getLocalNode();

        try {
            server = new MySslEngineServer(new InetSocketAddress("localhost", port));
            server.addObserver(this);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't initiate SSLEngineServer at port " + port);
        }
    }

    @Override
    public void run() {
        System.out.println("Starting thread: " + threadName);
        server.initialize();
    }

    public static void Log(String threadName, String message) {
        System.out.println(threadName + ": " + message);
    }

    @Override
    public void onReadReceived(MySslServerConnection connection) {
        try {
            //Log(threadName,"Got connection! -> " + connection.getRemoteAddress());

            String messageStr = connection.read(Message.MAX_SIZE);
            if (messageStr == null)
                return;
            Message message = MessageParser.parseMessage(messageStr);
            threadPool.execute(new MessageWorker(message, localNode, peerState, connection));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            Log(threadName,"Invalid message syntax. Ignoring... ");
            connection.close();
        } catch (IOException e) {
            //Log(threadName,"IO Exception: " + e.getMessage());
            connection.close();
        }

    }
}
