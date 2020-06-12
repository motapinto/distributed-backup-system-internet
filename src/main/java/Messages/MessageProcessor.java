package Messages;

public interface MessageProcessor {
    Message processGetChunkMessage(GetChunkMessage message);
    Message processPutChunkMessage(PutChunkMessage message);
    Message processDeleteFileMessage(DeleteFileMessage message);
    Message processDeleteChunkMessage(DeleteChunkMessage message);
    Message processRedirectMessage(RedirectMessage redirectMessage);
}
