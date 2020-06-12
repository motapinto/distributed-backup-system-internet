package Messages;

public class RawMessage extends Message {
    public static final String OK = "OK";
    public static final String NO_RESOURCE = "NO_RES";
    public static final String IO_EXCEPTION = "IO_EX";
    public static final String NOT_OK = "NOK";
    public static final String DUPLICATE = "DUPE";

    private final String content;

    public RawMessage(String content) {
        super();
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return new NullMessage("RawMessage is unprocessable");
    }

}
