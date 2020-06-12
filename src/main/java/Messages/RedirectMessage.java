package Messages;

import Chunks.ChunkID;

import java.net.InetSocketAddress;

public class RedirectMessage extends Message {
    public final ChunkID chunkID;
    public final InetSocketAddress oldAddress;
    public final InetSocketAddress newAddress;

    public RedirectMessage(ChunkID chunkID, InetSocketAddress oldAddress, InetSocketAddress newAddress) {
        this.chunkID = chunkID;
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;
    }

    @Override
    public Message process(MessageProcessor processor) {
        return processor.processRedirectMessage(this);
    }

    @Override
    public String toString() {
        return "REDIRECT " + this.chunkID.fileID + " " + this.chunkID.chunkNo + " " + this.oldAddress + " " + this.newAddress + " " + Message.HEADER_ENDING;
    }
}
