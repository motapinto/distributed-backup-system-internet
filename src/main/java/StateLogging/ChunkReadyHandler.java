package StateLogging;

import Chunks.Chunk;

public interface ChunkReadyHandler {
    void completed(Chunk chunk);
}
