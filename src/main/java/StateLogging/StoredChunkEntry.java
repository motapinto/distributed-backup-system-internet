package StateLogging;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class StoredChunkEntry implements Serializable, Comparable<StoredChunkEntry> {
    private static final long serialVersionUID = 4L;

    private final int chunkSize;

    private final InetSocketAddress owner;
    private int replicaNo;

    public StoredChunkEntry(int chunkSize, InetSocketAddress owner, int replicaNo) {
        this.chunkSize = chunkSize;
        this.owner = owner;
        this.replicaNo = replicaNo;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int compareTo(StoredChunkEntry e2) {
        if (this.getChunkSize() == e2.getChunkSize())
            return this.hashCode() - e2.hashCode();
        return e2.getChunkSize() - this.getChunkSize();
    }

    public InetSocketAddress getOwner() {
        return owner;
    }

    public int getReplicaNo() {
        return this.replicaNo;
    }

    public void updateReplicaNo(int replicaNo) {
        this.replicaNo = replicaNo;
    }
}
