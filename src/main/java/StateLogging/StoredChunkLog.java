package StateLogging;

import Chunks.Chunk;
import Chunks.ChunkID;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class StoredChunkLog implements Serializable {
    private static final long serialVersionUID = 2L;

    private final ConcurrentHashMap<ChunkID, StoredChunkEntry> chunkStoredMap = new ConcurrentHashMap<>();

    public Collection<StoredChunkEntry> getEntries() {
        return this.chunkStoredMap.values();
    }

    public ConcurrentHashMap<ChunkID, StoredChunkEntry> getMap() {
        return new ConcurrentHashMap<>(chunkStoredMap);
    }

    public void addChunk(ChunkID chunkID, int chunkSize, InetSocketAddress owner, int replicaNo) {
        chunkStoredMap.put(chunkID, new StoredChunkEntry(chunkSize, owner, replicaNo));
    }

    public StoredChunkEntry getEntry(ChunkID chunkID) {
        return chunkStoredMap.get(chunkID);
    }

    public boolean hasChunk(ChunkID chunkID) {
        return chunkStoredMap.containsKey(chunkID);
    }

    public void removeChunk(ChunkID chunkID) {
        this.chunkStoredMap.remove(chunkID);
    }

    public void removeFileRecords(String fileID) {
        Set<ChunkID> foundKeys = new HashSet<>();
        for (ChunkID key : chunkStoredMap.keySet()) {
            if (key.fileID.equals(fileID))
                foundKeys.add(key);
        }
        chunkStoredMap.keySet().removeAll(foundKeys);
    }

    public boolean hasFile(String fileID) {
        for (ChunkID key : chunkStoredMap.keySet()) {
            if (key.fileID.equals(fileID))
                return true;
        }
        return false;
    }

    public Chunk getChunk(ChunkID chunkID, String backupFolderPath) {
        String chunkPath = backupFolderPath + '/' + chunkID.fileID + '/' + chunkID.chunkNo;
        try {
            return new Chunk(chunkID.fileID, chunkID.chunkNo, Files.readAllBytes(Paths.get(chunkPath)));
        } catch (IOException e) {
            return null;
        }
    }

    public void getChunk(ChunkID chunkID, String backupFolderPath, ScheduledExecutorService executor, ChunkReadyHandler handler) {
        try {
            String chunkPath = backupFolderPath + '/' + chunkID.fileID + '/' + chunkID.chunkNo;
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(chunkPath), Collections.singleton(StandardOpenOption.READ), executor);
            ByteBuffer buffer = ByteBuffer.allocate(Chunk.MAX_CHUNK_SIZE);
            fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    Chunk chunk = new Chunk(chunkID, attachment.get(attachment.array(), 0, result).array());
                    handler.completed(chunk);
                    try {
                        fileChannel.close();
                    } catch (IOException ignored) { }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    System.out.println("Failed to retrieve chunk " + chunkID + ": " + exc.getMessage());
                    try {
                        fileChannel.close();
                    } catch (IOException ignored) { }
                }
            });
        } catch (IOException e) {
            System.out.println("Failed to retrieve chunk " + chunkID + ": " + e.getMessage());
        }
    }

    public void updateReplicaNo(ChunkID chunkID, int replicaNo) {
        this.getEntry(chunkID).updateReplicaNo(replicaNo);
    }

    public void deleteStoredChunk(ChunkID chunkID, String backupFolderPath) {
        new File(backupFolderPath, chunkID.fileID + '/' + chunkID.chunkNo).delete();
        this.removeChunk(chunkID);
    }

    public List<ChunkID> freeEntireSpace(String backupFolderPath) {
        List<ChunkID> freedChunks = new ArrayList<>();
        for (Map.Entry<ChunkID, StoredChunkEntry> entry : this.chunkStoredMap.entrySet()) {
            ChunkID chunkID = entry.getKey();
            freedChunks.add(chunkID);
            // this.deleteStoredChunk(chunkID, backupFolderPath);
        }
        return freedChunks;
    }

    public List<ChunkID> freeChunkSpace(int toFree, String backupFolderPath) {
        if (toFree <= 0)
            return new ArrayList<>();
        Map<StoredChunkEntry, ChunkID> orderedStorage = new TreeMap<>();
        for (Map.Entry<ChunkID, StoredChunkEntry> entry : this.chunkStoredMap.entrySet()) {
            orderedStorage.put(entry.getValue(), entry.getKey());
        }
        int freedCapacity = 0;
        List<ChunkID> freedChunks = new ArrayList<>();
        for (Map.Entry<StoredChunkEntry, ChunkID> entry : orderedStorage.entrySet()) {
            ChunkID chunkID = entry.getValue();
            freedCapacity += entry.getKey().getChunkSize();
            freedChunks.add(chunkID);

            // this.deleteStoredChunk(chunkID, backupFolderPath);

            if (freedCapacity >= toFree)
                break;
        }

        return freedChunks;
    }
        
    public ConcurrentHashMap<ChunkID, StoredChunkEntry> getChunkStoredMap(){
        return chunkStoredMap;
    }
}
