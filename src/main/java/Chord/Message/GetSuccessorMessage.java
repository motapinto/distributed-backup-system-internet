package Chord.Message;

import Chord.Node;

import java.net.InetSocketAddress;

public class GetSuccessorMessage extends ChordMessage {
    public GetSuccessorMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public Node process(Node processor) {
        return processor.getSuccessor();
    }

    @Override
    public String toString() {
        return "GETSUCC " + this.sender;
    }
}
