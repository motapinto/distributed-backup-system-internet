package Messages;

import Chunks.Chunk;
import Chunks.ChunkID;

import java.net.InetSocketAddress;

public class PutChunkMessage extends Message {
    private final byte[] body;

    public final ChunkID chunkID;
    public final InetSocketAddress owner;
    public final int replicaNo;
    public final boolean isRedirect;

    public PutChunkMessage(Chunk chunk, InetSocketAddress owner, int replicaNo, boolean isRedirect) {
        this.owner = owner;
        this.chunkID = chunk.getChunkID();
        this.body = chunk.getBytes();
        this.replicaNo = replicaNo;
        this.isRedirect = isRedirect;
    }

    @Override
    public String toString() {
        return "PUTCHUNK " + this.chunkID.fileID + " " + this.chunkID.chunkNo + " " + owner + " " + replicaNo + " " + isRedirect + " " + Message.HEADER_ENDING + Message.toString(body);
    }

    public Chunk getChunk() {
        return new Chunk(this.chunkID, this.body);
    }

    @Override
    public Message process(MessageProcessor processor) {
        return processor.processPutChunkMessage(this);
    }
}
