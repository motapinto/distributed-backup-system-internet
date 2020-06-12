package Chord.Message;

import Chord.Node;
import Chord.RemoteNode;

import java.net.InetSocketAddress;

public class SetPredecessorMessage extends ChordMessage {
    public final InetSocketAddress newPred;

    public SetPredecessorMessage(InetSocketAddress pred, InetSocketAddress sender) {
        super(sender);
        this.newPred = pred;
    }

    @Override
    public String toString() {
        return "SETPRED " + this.sender + " " + newPred;
    }

    @Override
    public Node process(Node processor) {
        return processor.setPredecessor(new RemoteNode(newPred, processor.getAddress()));
    }
}
