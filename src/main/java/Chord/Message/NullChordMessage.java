package Chord.Message;

import Chord.Node;

public class NullChordMessage extends ChordMessage {
    private final String message;

    public NullChordMessage(String message) {
        super(null);
        this.message = message;
    }

    @Override
    public String toString() {
        return "INVALID MESSAGE: " + this.message;
    }

    @Override
    public Node process(Node processor) {
        return null;
    }
}
