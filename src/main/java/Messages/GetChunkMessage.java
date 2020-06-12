package Messages;

import Chunks.ChunkID;

public class GetChunkMessage extends Message {
    public final ChunkID chunkID;

    public GetChunkMessage(ChunkID chunkID) {
        this.chunkID = chunkID;
    }

    @Override
    public String toString() {
        return "GETCHUNK " + this.chunkID.fileID + " " + this.chunkID.chunkNo + " " + Message.HEADER_ENDING;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return processor.processGetChunkMessage(this);
    }
}
