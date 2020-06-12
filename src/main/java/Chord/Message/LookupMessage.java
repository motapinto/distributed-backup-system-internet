package Chord.Message;

import Chord.ChordID;
import Chord.Node;

import java.net.InetSocketAddress;

public class LookupMessage extends ChordMessage {
    public final ChordID key;

    public LookupMessage(ChordID key, InetSocketAddress sender) {
        super(sender);
        this.key = key;
    }

    @Override
    public String toString() {
        return "LOOKUP " + this.sender + " " + this.key;
    }

    @Override
    public Node process(Node processor) {
        return processor.lookup(key);
    }
}
