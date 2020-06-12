package Messages;

public class DeleteFileMessage extends Message{
    public final String fileID;

    public DeleteFileMessage(String fileId) {
        this.fileID = fileId;
    }

    @Override
    public String toString() {
        return "DELETE " + this.fileID + " " + Message.HEADER_ENDING;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return processor.processDeleteFileMessage(this);
    }
}
