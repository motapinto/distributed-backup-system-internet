package Messages;

import Chunks.ChunkID;

public class DeleteChunkMessage extends Message{
    public final ChunkID chunkID;

    public DeleteChunkMessage(ChunkID chunkID) {
        super();
        this.chunkID = chunkID;
    }

    @Override
    public String toString() {
        return "DELETECHUNK " + this.chunkID.fileID + " " + this.chunkID.chunkNo + " " + Message.HEADER_ENDING;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return processor.processDeleteChunkMessage(this);
    }
}
