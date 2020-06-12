package PeerTCP;

import Chord.Node;
import Messages.DeleteFileMessage;
import Messages.Message;
import Messages.RawMessage;
import SslEngine.MySslEngineClient;
import StateLogging.PeerState;

import java.io.IOException;
import java.net.InetSocketAddress;

public class FileDeletion {
    private final String fileID;
    private final Node localNode;
    private final PeerState peerState;

    public FileDeletion(String fileID, Node localNode, PeerState peerState) {
        this.fileID = fileID;
        this.localNode = localNode;
        this.peerState = peerState;
    }

    public void deleteFile(InetSocketAddress storer) {
        try (MySslEngineClient client = new MySslEngineClient(TCPGroups.CC(storer))){
            String response = client.writeRead(new DeleteFileMessage(fileID).toString(), Message.MAX_SIZE);
            Log("Tried to delete file " + this.fileID + " at " + storer + " and got response " + response);
            if (response.equals(RawMessage.OK) || response.equals(RawMessage.NO_RESOURCE))
                peerState.getBackupLog().removeStorer(fileID, storer);
            else Log("Error deleting file " + this.fileID + ": Peer " + storer + " responded with " + response);
        }
        catch (IOException e) {
            Log("Error deleting file from storer " + storer + " :" + e.getMessage());
        }
    }

    private void Log(String message) {
        System.out.println("FileDeletion (Peer " + this.localNode.getID() + "): " + message);
    }
}
