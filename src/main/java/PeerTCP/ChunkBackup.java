package PeerTCP;

import Chord.Node;
import Chord.ChordIDMaker;
import Chunks.Chunk;
import Messages.Message;
import Messages.PutChunkMessage;
import Messages.RawMessage;
import SslEngine.MySslEngineClient;
import StateLogging.PeerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChunkBackup {
    private final Chunk chunk;
    private final Node localNode;
    private final PeerState peerState;
    private final InetSocketAddress ownerAddress;

    public ChunkBackup(Chunk chunkToBackup, Node localNode, PeerState peerState, InetSocketAddress ownerAddress) {
        this.chunk = chunkToBackup;
        this.ownerAddress = ownerAddress;
        this.localNode = localNode;
        this.peerState = peerState;
    }

    private boolean backupFailed(String response) {
        return !response.equals(RawMessage.OK) && !response.equals(RawMessage.DUPLICATE);
    }

    public void backup(int repDeg) throws ChunkException {
        if (peerState.getBackupLog().numStorers(this.chunk.getChunkID()) >= repDeg) {
            Log("Chunk " + this.chunk.getChunkID() + " already replicated enough.");
            return;
        }
        List<Integer> failedReplicas = new ArrayList<>();

        Node firstNode = this.localNode.lookup(ChordIDMaker.generateID(chunk.getChunkID().toString()));
        Node nextNode = firstNode;
        for (int replicaNo = 0; replicaNo < repDeg; replicaNo++) {
            if (nextNode.getID().equals(this.localNode.getID()))
                replicaNo--;
            else {
                String response = backupOnNode(nextNode.getAddress(), replicaNo, false);
                if (backupFailed(response)) // Failed
                    failedReplicas.add(replicaNo);
                if (peerState.getBackupLog().numStorers(this.chunk.getChunkID()) >= repDeg)
                    return;
            }

            nextNode = nextNode.getSuccessor();
            if (nextNode.getID().equals(firstNode.getID()))
                throw new ChunkException("Couldn't back up chunk " + chunk.getChunkID() + ": Possibly not enough peers in the circle.");
        }

        for (int i = 0; i < failedReplicas.size(); i++) {
            Log("Backing up chunk " + chunk.getChunkID() + " again because of a failed replication (" + i + ")");
            String response = backupOnNode(nextNode.getAddress(), failedReplicas.get(i), false);
            if (backupFailed(response)) // Failed
                i--;

            if (peerState.getBackupLog().numStorers(this.chunk.getChunkID()) >= repDeg)
                return;

            nextNode = nextNode.getSuccessor();
            if (nextNode.getID().equals(firstNode.getID()))
                throw new ChunkException("Couldn't back up chunk " + chunk.getChunkID() + ": Possibly not enough peers in the circle.");

        }
    }

    public String backupOnNode(InetSocketAddress address, int replicaNo, boolean isRedirect) {
        Log("Trying to back up chunk to peer " + address + ", at address " + address);
        try (MySslEngineClient client = new MySslEngineClient(TCPGroups.DB(address))) {
            Log("Successful connection to peer " + address);
            String response = client.writeRead(new PutChunkMessage(this.chunk, this.ownerAddress, replicaNo, isRedirect).toString(), Message.MAX_SIZE);
            Log("Tried to back up chunk to peer " + address + " and got response: " + response);
            if (!isRedirect && (response.equals(RawMessage.OK) || response.equals(RawMessage.DUPLICATE)))
                this.peerState.getBackupLog().addChunkStorer(this.chunk.getChunkID(), address);
            return response;
        }
        catch (IOException e) {
            Log("Couldnt store chunk " + chunk + " in peer " + address);
            Log("(Exception: " + e.getMessage() + ")");
        }
        return RawMessage.NOT_OK;
    }

    private void Log(String message) {
        System.out.println("ChunkBackup (Peer " + this.localNode.getID() + "): " + message);
    }
}
