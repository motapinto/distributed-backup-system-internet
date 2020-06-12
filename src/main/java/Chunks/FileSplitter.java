package Chunks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSplitter {
    private static final long MAX_FILE_SIZE = 64000000000L;

    private final String fileID;
    private final File file;
    private final FileInputStream inputStream;
    private final int numFullChunks;
    private final int lastChunkSize;

    private int numChunksRead = 0;

    public FileSplitter(File file, String fileID) throws IOException {
        this.file = file;
        this.fileID = fileID;
        this.inputStream = new FileInputStream(file);

        if (this.file.length() > MAX_FILE_SIZE)
            throw new IOException("Maximum file size reached (" + file.getName() + ")");

        this.numFullChunks = (int)(this.file.length() / Chunk.MAX_CHUNK_SIZE);
        this.lastChunkSize = (int)(this.file.length() % Chunk.MAX_CHUNK_SIZE);
    }

    public Chunk getNextChunk() throws IOException {
        Chunk chunk;
        if (numChunksRead >= numFullChunks) {
            chunk = this.readChunk(this.inputStream, this.lastChunkSize, numChunksRead);
            this.inputStream.close();
        }
        else chunk =  this.readChunk(this.inputStream, Chunk.MAX_CHUNK_SIZE, numChunksRead);
        numChunksRead++;
        return chunk;
    }

    public int getNumChunks() {
        return this.numFullChunks + 1;
    }

    public boolean isDone() {
        return this.numChunksRead >= this.numFullChunks + 1;
    }

    public void close() {
        try {
            this.inputStream.close();
        } catch (IOException ignored) { }
    }

    private Chunk readChunk(FileInputStream inputStream, int size, int chunkNo) throws IOException {
        byte[] data = new byte[size];
        inputStream.read(data, 0, size);
        return new Chunk(this.fileID, chunkNo, data);
    }
}
