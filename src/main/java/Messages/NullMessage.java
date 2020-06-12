package Messages;


public class NullMessage extends Message {
    private final String error;

    NullMessage(String error) {
        super();
        this.error = error;
    }

    @Override
    public String toString() {
        return "NullMessage: " + this.error;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return this;
    }
}
