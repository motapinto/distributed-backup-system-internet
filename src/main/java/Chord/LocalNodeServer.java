package Chord;

import Chord.Message.ChordMessage;
import Chord.Message.ChordMessageParser;
import Chord.Message.NullChordMessage;
import SslEngine.MySslEngineServer;
import SslEngine.MySslServerConnection;
import SslEngine.MySslServerListener;

import java.io.IOException;

public class LocalNodeServer implements MySslServerListener, Runnable {
    private final LocalNode node;
    private final MySslEngineServer server;

    public LocalNodeServer(LocalNode node) {
        this.node = node;
        try {
            this.server = new MySslEngineServer(node.getAddress());
            server.addObserver(this);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't initiate SSLEngineServer at port " + node.getAddress().getPort());
        }
    }

    @Override
    public void onReadReceived(MySslServerConnection connection) {
        try {
            String message = connection.read(ChordMessage.MAX_SIZE);
            if (message == null)
                return;
            //Log("Received message: "  + message);
            ChordMessage chordMessage = ChordMessageParser.parseMessage(message);
            if (chordMessage instanceof NullChordMessage)
                throw new IOException("Invalid message: " + chordMessage);
            Node toReturn = chordMessage.process(node);
            connection.write(toReturn.getAddress().toString());
            connection.shutdownOutput();
        } catch (IOException e) {
            connection.close();
        }
    }

    @Override
    public void run() {
        server.initialize();
    }

    public void Log(String message) {
        System.out.println("Node " + node.getID() + ": " + message);
    }

}
