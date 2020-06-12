package Messages;

import Chunks.Chunk;
import Chunks.ChunkID;

public class ChunkMessage extends Message{
    public final ChunkID chunkID;
    public final byte[] body;

    public ChunkMessage(Chunk chunk) {
        this.chunkID = chunk.getChunkID();
        this.body = chunk.getBytes();
    }

    @Override
    public String toString() {
        return "CHUNK " + this.chunkID.fileID + " " + this.chunkID.chunkNo  + " " + Message.HEADER_ENDING + Message.toString(body);
    }

    @Override
    public Message process(MessageProcessor processor) {
        return new NullMessage("ChunkMessage is unprocessable");
    }

    public Chunk getChunk() {
        return new Chunk(this.chunkID, this.body);
    }
}

