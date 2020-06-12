package Chord.Message;

import Chord.Node;

import java.net.InetSocketAddress;

public class GetPredecessorMessage extends ChordMessage {
    public GetPredecessorMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public Node process(Node processor) {
        return processor.getPredecessor();
    }

    @Override
    public String toString() {
        return "GETPRED " + this.sender;
    }
}
