package Chunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Chunk {
    public static final ScheduledExecutorService chunkThreadPool = Executors.newScheduledThreadPool(10);
    public static final int MAX_CHUNK_SIZE = 64000;

    private final ChunkID chunkID;
    private final byte[] bytes;

    public Chunk(ChunkID chunkID, byte[] bytes) {
        this.chunkID = chunkID;
        this.bytes = bytes;
    }

    public Chunk(String fileID, int chunkNo, byte[] bytes) {
        this(new ChunkID(fileID, chunkNo), bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public int size() {
        return bytes.length;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "fileID=" + this.getFileID() +
                ", chunkNo=" + this.getChunkNo() +
                ", numBytes=" + this.bytes.length +
                '}';
    }

    public int getChunkNo() {
        return this.getChunkID().chunkNo;
    }

    public String getFileID() {
        return this.getChunkID().fileID;
    }

    public ChunkID getChunkID() {
        return this.chunkID;
    }

    public void save(String backupFolderPath) throws IOException {
        File fileFolder = new File(backupFolderPath + "/" + this.getFileID());
        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }

        File chunkFile = new File(fileFolder, String.valueOf(this.getChunkNo()));
        chunkFile.getParentFile().mkdirs();
        chunkFile.createNewFile();

        ByteBuffer buffer = ByteBuffer.wrap(this.bytes);
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(chunkFile.getPath()), Collections.singleton(StandardOpenOption.WRITE), chunkThreadPool);
        fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                try {
                    fileChannel.close();
                } catch (IOException ignored) { }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                System.out.println("Error writing chunk " + Chunk.this.chunkID + ": " + exc.getMessage());
                try {
                    fileChannel.close();
                } catch (IOException ignored) { }
            }
        });

    }
}
