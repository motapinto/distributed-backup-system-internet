package StateLogging;

import Chunks.ChunkID;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileBackupLog implements Serializable {
    private static final long serialVersionUID = 3L;
    private final ConcurrentHashMap<String, FileBackupEntry> fileBackupMap = new ConcurrentHashMap<>();

    /*
    public ConcurrentHashMap<String, FileBackupEntry> getHashMap() {
        return this.fileBackupMap;
    }
     */

    public HashMap<ChunkID, List<InetSocketAddress>> getAllStorers() {
        HashMap<ChunkID, List<InetSocketAddress>> storers = new HashMap<>();
        for (ConcurrentHashMap.Entry<String, FileBackupEntry> entry : fileBackupMap.entrySet()){
            String fileID = entry.getKey();
            for (int i = 0; i < entry.getValue().getNumChunks(); i++) {
                storers.put(new ChunkID(fileID, i), entry.getValue().getChunkStorers(i));
            }
        }
        return storers;
    }

    public void addFile(String fileID, String pathName, int desiredDegree, int numChunks) {
        if (hasFile(fileID)) {
            if (fileBackupMap.get(fileID).isWaitingForDeletion())
                fileBackupMap.get(fileID).removeFromDeletion();
            fileBackupMap.get(fileID).setDesiredDegree(desiredDegree);
        }
        else this.fileBackupMap.put(fileID, new FileBackupEntry(pathName, desiredDegree, numChunks));
    }

    public void removeFile(String fileID) {
        fileBackupMap.remove(fileID);
    }

    public boolean hasFile(String fileId) {
        return fileBackupMap.containsKey(fileId);
    }

    /* Storers */

    public void addChunkStorer(ChunkID chunkID, InetSocketAddress storer) {
        fileBackupMap.get(chunkID.fileID).addChunkStorer(chunkID.chunkNo, storer);
    }

    public List<InetSocketAddress> getChunkStorers(ChunkID chunkID) {
        return fileBackupMap.get(chunkID.fileID).getChunkStorers(chunkID.chunkNo);
    }

    public void removeChunkStorer(ChunkID chunkID, InetSocketAddress storer) {
        fileBackupMap.get(chunkID.fileID).removeChunkStorer(chunkID.chunkNo, storer);
    }

    public List<InetSocketAddress> getStorers(String fileID) {
        return fileBackupMap.get(fileID).getStorers();
    }

    public void removeStorer(String fileId, InetSocketAddress storer){
        FileBackupEntry entry = fileBackupMap.get(fileId);
        entry.removeStorer(storer);
        if(entry.isWaitingForDeletion() && entry.getMaxPerceivedRepDeg() == 0)
            removeFile(fileId);
    }

    /* Storers End */


    public void addToDeletion(String fileId){
        fileBackupMap.get(fileId).addToDeletion();
    }

    public ArrayList<WaitDeletionEntry> filesWaitingForDeletion(){
        ArrayList<WaitDeletionEntry> files = new ArrayList<>();
        for (ConcurrentHashMap.Entry<String, FileBackupEntry> entry : fileBackupMap.entrySet()){
            String fileID = entry.getKey();
            if (entry.getValue().isWaitingForDeletion()) {
                for (InetSocketAddress missing : entry.getValue().getStorers()) {
                    files.add(new WaitDeletionEntry(fileID, missing));
                }
            }
        }
        return files;
    }

    public String getFileID(String fileName) {
        for (Map.Entry<String, FileBackupEntry> entry : fileBackupMap.entrySet()) {
            if (entry.getValue().getPathName().equalsIgnoreCase(fileName))
                return entry.getKey();
        }
        return null;
    }

    /*
    public boolean withinDesiredRepDeg(ChunkID chunkID) {
        FileBackupEntry chunkEntry = this.fileBackupMap.get(chunkID.fileID);
        return chunkEntry.getChunkStorers(chunkID.chunkNo).size() >= chunkEntry.getDesiredRepDeg();
    }

     */

    public int numStorers(ChunkID chunkID) {
        return this.getChunkStorers(chunkID).size();
    }

    public ConcurrentHashMap<String, FileBackupEntry> getFileBackupMap(){
        return fileBackupMap;
    }


    public FileBackupEntry getEntry(String fileID) {
        return this.fileBackupMap.get(fileID);
    }

    public int getDesiredRepDeg(String fileID) {
        return this.fileBackupMap.get(fileID).getDesiredRepDeg();
    }
}
