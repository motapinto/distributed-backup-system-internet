package Chord.Message;

import Chord.Node;

import java.net.InetSocketAddress;

public abstract class ChordMessage {
    public static final int MAX_SIZE = 1024;

    public final InetSocketAddress sender;

    public ChordMessage(InetSocketAddress sender){
        this.sender = sender;
    }

    public abstract Node process(Node processor);
}
