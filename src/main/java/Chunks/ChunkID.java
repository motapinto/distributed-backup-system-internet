package Chunks;

import java.io.Serializable;
import java.util.Objects;

public class ChunkID implements Serializable {
    private static final long serialVersionUID = 6L;

    public final String fileID;
    public final int chunkNo;

    public ChunkID(String fileID, int chunkNo) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
    }

    @Override
    public String toString() {
        return "ChunkID{" +
                "fileID='" + fileID + '\'' +
                ", chunkNo=" + chunkNo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkID chunkID = (ChunkID) o;
        return chunkNo == chunkID.chunkNo &&
                fileID.equals(chunkID.fileID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileID, chunkNo);
    }
}
