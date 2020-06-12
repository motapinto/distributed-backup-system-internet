package StateLogging;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;

public class FileBackupEntry implements Serializable {
    private static final long serialVersionUID = 5L;

    private final String pathName;
    private int desiredRepDeg;
    private final int numChunks;
    private Boolean waitingForDeletion = false;

    private final List<Set<InetSocketAddress>> storers;

    public FileBackupEntry(String pathName, int desiredRepDeg, int numChunks) {
        this.pathName = pathName;
        this.desiredRepDeg = desiredRepDeg;
        this.numChunks = numChunks;
        this.storers = new ArrayList<>();
        for (int i = 0; i < numChunks; i++)
            this.storers.add(new HashSet<>());
    }

    public void addChunkStorer(int chunkNo, InetSocketAddress storer) {
        this.storers.get(chunkNo).add(storer);
    }

    public void removeChunkStorer(int chunkNo, InetSocketAddress storer) {
        this.storers.get(chunkNo).remove(storer);
    }

    public List<InetSocketAddress> getChunkStorers(int chunkNo) {
        return new ArrayList<>(this.storers.get(chunkNo));
    }

    public List<InetSocketAddress> getStorers() {
        Set<InetSocketAddress> allStorers = new HashSet<>();
        for (Set<InetSocketAddress> chunkStorers : storers) {
            allStorers.addAll(chunkStorers);
        }
        return new ArrayList<>(allStorers);
    }

    public void removeStorer(InetSocketAddress storer) {
        for(Set<InetSocketAddress> set : storers)
            set.remove(storer);
    }



    public int getMaxPerceivedRepDeg(){
        int maxPerceivedRepDeg = 0;
        for(Set<InetSocketAddress> set : storers)
            maxPerceivedRepDeg = Math.max(maxPerceivedRepDeg,set.size());
        return maxPerceivedRepDeg;
    }

    public int getDesiredRepDeg() {
        return this.desiredRepDeg;
    }

    public String getPathName() {
        return pathName;
    }

    public int getNumChunks() { return numChunks; }


    public void addToDeletion() {
        waitingForDeletion = true;
    }
    public void removeFromDeletion() {
        waitingForDeletion = false;
    }

    public boolean isWaitingForDeletion() {
        return waitingForDeletion;
    }

    public void setDesiredDegree(int desiredDegree) {
        this.desiredRepDeg = desiredDegree;
    }
}
