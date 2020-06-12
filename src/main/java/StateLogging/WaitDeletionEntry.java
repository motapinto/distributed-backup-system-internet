package StateLogging;

import java.net.InetSocketAddress;

public class WaitDeletionEntry {
    private final String fileID;
    private final InetSocketAddress address;

    public WaitDeletionEntry(String fileID, InetSocketAddress address) {
        this.fileID = fileID;
        this.address = address;
    }

    public String getFileID() {
        return fileID;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
