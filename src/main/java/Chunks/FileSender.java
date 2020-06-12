package Chunks;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;


public class FileSender {
    private static final long MAX_FILE_SIZE = 64000000000L;

    private final String fileID;
    private final File file;
    private final int numFullChunks;
    private final int lastChunkSize;
    private final ScheduledExecutorService executorService;
    private final CompletionHandlerSender completionHandlerSender;


    private int numChunksRead = 0;

    public FileSender(File file, String fileID, CompletionHandlerSender completionHandlerSender, ScheduledExecutorService executorService) throws IOException {
        this.file = file;
        this.fileID = fileID;
        this.executorService = executorService;
        this.completionHandlerSender = completionHandlerSender;

        if (this.file.length() > MAX_FILE_SIZE)
            throw new IOException("Maximum file size reached (" + file.getName() + ")");

        this.numFullChunks = (int)(this.file.length() / Chunk.MAX_CHUNK_SIZE);
        this.lastChunkSize = (int)(this.file.length() % Chunk.MAX_CHUNK_SIZE);
        //this.execute();
    }

    public void execute() throws IOException {
        while(!isDone()){
            sendNextChunk();
        }
    }

    private void sendNextChunk() throws IOException {
        if (numChunksRead >= numFullChunks) {
            this.sendChunk(this.lastChunkSize, numChunksRead);
        }
        else this.sendChunk(Chunk.MAX_CHUNK_SIZE, numChunksRead);
        numChunksRead++;
    }

    private void sendChunk(int size, int chunkNo) throws IOException {
        AsynchronousFileChannel fileChannel =  AsynchronousFileChannel.open(file.toPath(), Collections.singleton(StandardOpenOption.READ), executorService);
        ByteBuffer buffer = ByteBuffer.allocate(size);
        long position = chunkNo * Chunk.MAX_CHUNK_SIZE;
        CompletionHandlerSender chs = new CompletionHandlerSender(completionHandlerSender, new ChunkID(fileID, chunkNo), fileChannel);
        fileChannel.read(buffer, position, buffer, chs);
    }

    public int getNumChunks() {
        return this.numFullChunks + 1;
    }

    public boolean isDone() {
        return this.numChunksRead >= this.numFullChunks + 1;
    }

}
