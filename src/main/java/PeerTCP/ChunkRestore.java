package PeerTCP;

import Chord.Node;
import Chunks.Chunk;
import Chunks.ChunkID;
import Messages.*;
import SslEngine.MySslEngineClient;
import StateLogging.PeerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ChunkRestore {
    private static final int MAX_TRIES = 5;

    private final ChunkID chunkID;
    private final Node localNode;
    private final PeerState peerState;

    public ChunkRestore(ChunkID chunkToRestore, Node localNode, PeerState peerState) {
        this.chunkID = chunkToRestore;
        this.localNode = localNode;
        this.peerState = peerState;
    }

    public Chunk restoreChunk() throws ChunkException {
        List<InetSocketAddress> storers = this.peerState.getBackupLog().getChunkStorers(chunkID);

        for (InetSocketAddress node : storers) {
            Log("Trying to restore chunk from peer " + node);
            try (MySslEngineClient client = new MySslEngineClient(TCPGroups.DR(node))) {
                Log("Successful connection to peer " + node);
                String response = client.writeRead(new GetChunkMessage(this.chunkID).toString(), Message.MAX_SIZE);
                Log("Tried to restore chunk from peer " + node + " and got response: " + response.substring(0, Math.min(100, response.length())));

                if (response.equals(RawMessage.NO_RESOURCE))
                    Log("Couldn't restore chunk from peer " + node + ": Claims he has no such chunk");
                else {
                    ChunkMessage chunkMessage = (ChunkMessage) MessageParser.parseMessage(response);
                    Log("Successfully retrieved chunk: " + chunkMessage.getChunk());
                    return chunkMessage.getChunk();
                }
            }
            catch (IOException e) {
                Log("Failed to retrieve chunk from peer " + node);
            }
        }
        throw new ChunkException("Failed to retrieve chunk. Aborting...");
    }

    private void Log(String message) {
        System.out.println("ChunkBackup (Peer " + this.localNode.getID() + "): " + message);
    }}
