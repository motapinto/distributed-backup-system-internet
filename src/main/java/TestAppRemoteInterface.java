import StateLogging.PeerState;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TestAppRemoteInterface extends Remote {
    void backupFile(String fileName, int repDeg) throws Exception;
    void restoreFile(String fileName) throws Exception;
    void deleteFile(String fileName)  throws Exception;
    void reclaimDiskSpace(int space)  throws Exception;
    PeerState getInternalState() throws RemoteException;
}