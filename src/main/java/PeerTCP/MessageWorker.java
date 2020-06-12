package PeerTCP;

import Chord.LocalNode;
import Chunks.Chunk;
import Chunks.ChunkID;
import Messages.*;
import SslEngine.MySslServerConnection;
import StateLogging.FileBackupLog;
import StateLogging.PeerState;
import StateLogging.StoredChunkLog;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MessageWorker implements Runnable, MessageProcessor {
    private final PeerState peerState;
    private final StoredChunkLog storedLog;
    private final FileBackupLog backupLog;
    private final String backupFolderPath;

    private final Message message;
    private final LocalNode localNode;
    private final MySslServerConnection connection;

    public MessageWorker(Message message, LocalNode localNode, PeerState peerState, MySslServerConnection connection) {
        this.connection = connection;
        this.message = message;
        this.localNode = localNode;

        this.peerState = peerState;

        this.storedLog = peerState.getStoredLog();
        this.backupLog = peerState.getBackupLog();
        this.backupFolderPath = peerState.getBackupFolderPath();
    }

    @Override
    public void run() {
        Log("Processing message: " + message.toString().split(Message.HEADER_ENDING)[0]);
        Message response = this.message.process(this);
        try {
            connection.write(response.toString());
            connection.shutdownOutput();
        } catch (IOException e) {
            Log("Failed to reply to " + connection.getRemoteAddress() + " with message of type: " + response.getClass().getSimpleName());
        }
        connection.close();
    }

    private void Log(String message) {
        System.out.println("MessageWorker (Peer " + this.localNode.getID() + "): " + message);
    }

    @Override
    public Message processDeleteChunkMessage(DeleteChunkMessage message) {
        if (!this.storedLog.hasChunk(message.chunkID))
            return new RawMessage(RawMessage.NO_RESOURCE);
        this.storedLog.removeChunk(message.chunkID);

        File folder = new File(backupFolderPath, message.chunkID.fileID);
        if (folder.exists()) {
            File chunkFile = new File(folder, String.valueOf(message.chunkID.chunkNo));
            if (chunkFile.exists()) {
                chunkFile.delete();
                return new RawMessage(RawMessage.OK);
            }
        }
        return new RawMessage("I_THOUGHT_I_HAD_CHUNK_BUT_TURNS_OUT_I_DONT");
    }

    @Override
    public Message processRedirectMessage(RedirectMessage redirectMessage) {
        ChunkID chunkID = redirectMessage.chunkID;
        InetSocketAddress oldAddress = redirectMessage.oldAddress;
        InetSocketAddress newAddress = redirectMessage.newAddress;
        FileBackupLog backupLog = this.peerState.getBackupLog();
        if (backupLog.getChunkStorers(chunkID).contains(oldAddress)) {
            backupLog.removeChunkStorer(chunkID, oldAddress);
            if (newAddress != null)
                backupLog.addChunkStorer(chunkID, newAddress);
            return new RawMessage(RawMessage.OK);
        }
        else return new RawMessage(RawMessage.NOT_OK);
    }

    @Override
    public Message processDeleteFileMessage(DeleteFileMessage message) {
        if (!this.storedLog.hasFile(message.fileID))
            return new RawMessage(RawMessage.NO_RESOURCE);
        this.storedLog.removeFileRecords(message.fileID);

        File folder = new File(backupFolderPath, message.fileID);
        if (folder.exists()) {
            for (String chunkNo : folder.list()) {
                new File(folder.getPath(), chunkNo).delete();
            }
            folder.delete();
            return new RawMessage(RawMessage.OK);
        }
        return new RawMessage("I_THOUGHT_I_HAD_FILE_BUT_TURNS_OUT_I_DONT");
    }

    @Override
    public Message processGetChunkMessage(GetChunkMessage message) {
        if (!this.storedLog.hasChunk(message.chunkID))
            return new RawMessage(RawMessage.NO_RESOURCE);
        Chunk storedChunk = this.storedLog.getChunk(message.chunkID, this.backupFolderPath);
        return new ChunkMessage(storedChunk);
    }

    @Override
    public Message processPutChunkMessage(PutChunkMessage message) {

        if (this.backupLog.hasFile(message.chunkID.fileID)) {
            Log("Cannot store chunk for a file of which I am the original owner. FileID: " + message.chunkID.fileID);
            return new RawMessage(RawMessage.NOT_OK);
        }

        if (this.peerState.hasRedirect(message.chunkID)) {
            Log("Cannot store chunk for which I have a redirect entry to.");
            return new RawMessage(RawMessage.NOT_OK);
        }

        Chunk chunk = message.getChunk();
        if (this.storedLog.hasChunk(chunk.getChunkID())) {
            if (!message.isRedirect)
                this.storedLog.updateReplicaNo(chunk.getChunkID(), message.replicaNo);
            Log("Chunk is already stored: " + chunk);
            return new RawMessage(RawMessage.DUPLICATE);
        }

        if (this.peerState.availableCapacity() < chunk.size() || this.peerState.getCapacity() == 0) {
            // return "NOT OK"
            Log("Not enough storage space for chunk ( " + chunk.size() + " bytes). Available space: " + this.peerState.availableCapacity() + "/" + this.peerState.getCapacity());
            return new RawMessage(RawMessage.NOT_OK);
        }

        Log("Saving chunk: " + chunk);

        try {
            chunk.save(this.backupFolderPath);
        } catch (IOException e) {
            e.printStackTrace();
            Log("IO Exception: Could not save chunk: " + chunk);
            Log("Error: " + e.getMessage());
            return new RawMessage(RawMessage.IO_EXCEPTION);
        }
        this.storedLog.addChunk(chunk.getChunkID(), chunk.size(), message.owner, message.replicaNo);

        return new RawMessage(RawMessage.OK);
    }
}
