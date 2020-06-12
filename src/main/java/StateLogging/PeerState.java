package StateLogging;

import Chord.LocalNode;
import Chunks.ChunkID;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PeerState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<ChunkID, RedirectEntry> redirects;
    private final FileBackupLog backupState;
    private final StoredChunkLog storedState;
    private int capacity;

    private static final String PEER_STATE_NAME = "PEER_STATE";
    private static final int DEFAULT_CAPACITY = 50000000;

    private transient String backupFolderPath;
    private transient LocalNode localNode;

    private PeerState(String backupFolderPath, InetSocketAddress address) {
        this.capacity = DEFAULT_CAPACITY;
        this.storedState = new StoredChunkLog();
        this.backupState = new FileBackupLog();
        this.redirects = new ConcurrentHashMap<>();
        this.backupFolderPath = backupFolderPath;
        this.localNode = new LocalNode(address);
    }

    public static PeerState loadPeerState(String backupFolderPath, InetSocketAddress address) {
        try {
            FileInputStream fi = new FileInputStream(new File(backupFolderPath, PEER_STATE_NAME));
            ObjectInputStream oi = new ObjectInputStream(fi);
            PeerState peerState = (PeerState) oi.readObject();
            oi.close();
            fi.close();
            peerState.backupFolderPath = backupFolderPath;
            peerState.localNode = new LocalNode(address);
            return peerState;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading peer state from: " +  backupFolderPath);
        }

        return new PeerState(backupFolderPath, address);
    }

    public void save() {
        try {
            System.out.println("Saving peer state. Used capacity: " + (capacity -this.availableCapacity()) + "/" + capacity);

            FileOutputStream f = new FileOutputStream(new File(this.backupFolderPath, PEER_STATE_NAME));
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(this);
            o.close();
            f.close();
        } catch (IOException e) {
            System.out.println("Error writing peer state to: " +  backupFolderPath);
            e.printStackTrace();
        }
    }

    public StoredChunkLog getStoredLog() {
        return this.storedState;
    }

    public FileBackupLog getBackupLog() {
        return this.backupState;
    }

    public String getBackupFolderPath() {
        return backupFolderPath;
    }

    public List<ChunkID> setCapacity(int capacity, String backupFolderPath) {
        this.capacity = capacity;

        int availableCapacity = this.availableCapacity();
        if (capacity == 0)
            return this.storedState.freeEntireSpace(backupFolderPath);
        if (availableCapacity < 0)
            return this.storedState.freeChunkSpace(-availableCapacity, backupFolderPath);

        return new ArrayList<>();
    }

    public int getCapacity() {
        return capacity;
    }

    public int availableCapacity() {
        int usedCapacity = 0;
        for (StoredChunkEntry entry : this.storedState.getEntries()) {
            usedCapacity += entry.getChunkSize();
        }
        return this.capacity - usedCapacity;
    }

    // ** State Printing ** //

    public void print(PrintStream printStream) {
        ConcurrentHashMap<String, FileBackupEntry> fileBackupMap = backupState.getFileBackupMap();
        int lastElem = fileBackupMap.size();
        int elemNum = 1;
        printStream.println("Files Backed Up: (" + lastElem + ")");
        for (Map.Entry<String, FileBackupEntry> entry : fileBackupMap.entrySet()) {
            String fileId = entry.getKey();
            FileBackupEntry fileBackupEntry = entry.getValue();
            if(fileBackupEntry.isWaitingForDeletion())
                printStream.println("Note: This file is waiting for deletion");
            int numChunks = fileBackupEntry.getNumChunks();
            String branchElem1, branchElem2, subBranchElem1, subBranchElem2;
            if (elemNum != lastElem) {
                branchElem1 = "|";
                branchElem2 = "|";
            } else {
                branchElem1 = "+";
                branchElem2 = " ";
            }

            printStream.println(branchElem1 + "-Pathname:                   " + fileBackupEntry.getPathName());
            printStream.println(branchElem2 + " Backup service id:          " + fileId);
            printStream.println(branchElem2 + " Desired replication degree: " + fileBackupEntry.getDesiredRepDeg());
            printStream.println(branchElem2 + " Chunks: (" + numChunks + ")");
            for (int i = 0; i < numChunks; i++) {
                if (i != numChunks - 1) {
                    subBranchElem1 = "|";
                    subBranchElem2 = "|";
                } else {
                    subBranchElem1 = "+";
                    subBranchElem2 = " ";
                }
                printStream.println(branchElem2 + ' ' + subBranchElem1 + "-ChunkId:                      " + i);
                printStream.println(branchElem2 + ' ' + subBranchElem2 + " Perceived replication degree: " + fileBackupEntry.getChunkStorers(i).size());
                for (InetSocketAddress addr : fileBackupEntry.getChunkStorers(i)) {
                    printStream.println(branchElem2 + ' ' + subBranchElem2 + "  - " + addr);

                }
                printStream.println(branchElem2 + ' ' + subBranchElem2);
            }
            printStream.println(branchElem2);
            elemNum++;
        }

        ConcurrentHashMap<ChunkID, StoredChunkEntry> chunkStoredMap = storedState.getChunkStoredMap();
        int lastElemChunk = chunkStoredMap.size();
        int elemNumChunk = 1;

        printStream.println("Chunks Stored: (" + lastElemChunk + ")");
        for (Map.Entry<ChunkID, StoredChunkEntry> entry : chunkStoredMap.entrySet()) {
            ChunkID chunkId = entry.getKey();
            StoredChunkEntry chunkEntry = entry.getValue();
            String branchElem1, branchElem2;
            if (elemNumChunk != lastElemChunk) {
                branchElem1 = "|";
                branchElem2 = "|";
            } else {
                branchElem1 = "+";
                branchElem2 = " ";
            }
            printStream.println(branchElem1 + "-Chunk ID:                     " + chunkId.fileID + ":" + chunkId.chunkNo);
            printStream.println(branchElem2 + " Size(KB):                     " + chunkEntry.getChunkSize());
            //printStream.println(branchElem2 + " Perceived replication degree: " + chunkEntry.numStorers());
            printStream.println(branchElem2);
            elemNumChunk++;
        }

        printStream.println("Storage (KB):");
        printStream.println(" Total disk space: " + this.capacity / 1000);
        printStream.println(" Storage used:     " + (this.capacity - this.availableCapacity()) / 1000);
        printStream.println(" Storage left:     " + this.availableCapacity() / 1000);
        printStream.println(" " + storageBar());
    }

    public int getStorageUsedPercent() {
        if (this.capacity == 0)
            return 100;
        int capacityUsed = this.capacity - this.availableCapacity();
        return (int)Math.round(100.0 * capacityUsed/this.capacity);
    }

    private String storageBar(){
        int capacityUsed = this.capacity - this.availableCapacity();
        int percentageUsed = getStorageUsedPercent();
        StringBuilder sBar = new StringBuilder(percentageUsed + "% [");
        for (int i = 0 ; i < 50 ; i++) {
          if(i<percentageUsed/2)
              sBar.append("■");
          else sBar.append(" ");
        }
        sBar.append("] ").append(capacityUsed / 1000).append('/').append(this.capacity / 1000).append("KB");
        return sBar.toString();
    }

    public ConcurrentHashMap<ChunkID, RedirectEntry> getRedirects() {
        return new ConcurrentHashMap<>(this.redirects);
    }

    public void clearRedirect(ChunkID chunkID) {
        this.redirects.remove(chunkID);
    }

    public void addRedirect(ChunkID chunkID, InetSocketAddress address, InetSocketAddress owner) {
        this.redirects.put(chunkID, new RedirectEntry(address, owner));
    }

    public boolean hasRedirect(ChunkID chunkID) {
        return this.redirects.containsKey(chunkID);
    }

    public LocalNode getLocalNode() {
        return this.localNode;
    }
}
