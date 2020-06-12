package Chunks;

import Chord.LocalNode;
import PeerTCP.ChunkBackup;
import PeerTCP.ChunkException;
import StateLogging.PeerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;

public class CompletionHandlerSender implements CompletionHandler<Integer, ByteBuffer> {
    private final int repdeg;
    private final LocalNode localNode;
    private final InetSocketAddress address;
    private final PeerState peerState;


    private AsynchronousFileChannel fileChannel;
    private ChunkID chunkID;

    public CompletionHandlerSender(int repdeg, PeerState peerState){
        this.repdeg = repdeg;
        this.peerState = peerState;
        this.localNode = peerState.getLocalNode();
        this.address = localNode.getAddress();
    }

    public CompletionHandlerSender(CompletionHandlerSender completionHandlerSender, ChunkID chunkID, AsynchronousFileChannel fileChannel) {
        this.repdeg = completionHandlerSender.repdeg;
        this.localNode = completionHandlerSender.localNode;
        this.address = completionHandlerSender.address;
        this.peerState = completionHandlerSender.peerState;
        this.fileChannel = completionHandlerSender.fileChannel;
        this.chunkID = chunkID;
        this.fileChannel = fileChannel;
    }


    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        //System.out.println("result = " + result);
        attachment.flip();
        byte[] data = new byte[attachment.limit()];
        attachment.get(data);
        Chunk chunk = new Chunk(chunkID, data);
        try {
            new ChunkBackup(chunk, localNode, peerState, address).backup(repdeg);
            if (chunk.size() < Chunk.MAX_CHUNK_SIZE)
                fileChannel.close();

        } catch (ChunkException | IOException e) {
            System.out.println("Error backing up file: " + e.getMessage());
        }
        //System.out.println(new String(data));
        attachment.clear();
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        System.out.println("Failed to read file");
    }


}
