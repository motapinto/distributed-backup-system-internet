import Chord.*;
import Chunks.*;
import PeerTCP.*;
import SslEngine.MySslEngineClient;
import StateLogging.*;
import Messages.*;

import java.io.*;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peer implements TestAppRemoteInterface {

    private final String rootFolderPath;
    private final String peerFolderPath;
    private final String backupFolderPath;

    private final PeerState peerState;
    private final LocalNode localNode;
    private final RemoteNode knownNode;

    private final ScheduledExecutorService stateSaver = Executors.newSingleThreadScheduledExecutor();

    private final ScheduledExecutorService updaterDeletions = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService updaterRedirects = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService updaterExpectedOwners = Executors.newSingleThreadScheduledExecutor();

    public Peer(InetSocketAddress baseAddress, InetSocketAddress knownAddress) {
        if (knownAddress == null)
            this.knownNode = null;
        else this.knownNode = new RemoteNode(knownAddress, baseAddress);

        this.rootFolderPath = "peers";
        this.peerFolderPath = this.rootFolderPath + "/peer" + ChordIDMaker.generateID(baseAddress);
        this.backupFolderPath =  this.peerFolderPath + "/backup";

        this.peerState = PeerState.loadPeerState(this.backupFolderPath, baseAddress);
        this.localNode = this.peerState.getLocalNode();

        this.setupFileSystem();

        String rmiAccessPoint = this.localNode.getID().toString();
        try {
            TestAppRemoteInterface stub = (TestAppRemoteInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(rmiAccessPoint, stub);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this.peerState::save));
        Log("Armed and ready! Access point: " + rmiAccessPoint);
    }

    private void setupFileSystem() {
        File rootFolder = new File(this.rootFolderPath);
        File peerFolder = new File(this.peerFolderPath);
        File backupFolder = new File(this.backupFolderPath);
        File documentFolder = new File(this.peerFolderPath, "Documents");

        if (!rootFolder.exists())
            rootFolder.mkdir();
        if (!peerFolder.exists())
            peerFolder.mkdir();
        if (!backupFolder.exists())
            backupFolder.mkdir();
        if (!documentFolder.exists())
            documentFolder.mkdir();
    }
    
    public void run() {
        Log("Joining ring!");
        this.localNode.join(this.knownNode);
        Log("Joined!");
        new TCPServerThread("TCCThread", TCPGroups.CC(this.localNode.getAddress()).getPort(), this.peerState).start();
        new TCPServerThread("TDBThread", TCPGroups.DB(this.localNode.getAddress()).getPort(), this.peerState).start();
        new TCPServerThread("TDRThread", TCPGroups.DR(this.localNode.getAddress()).getPort(), this.peerState).start();

        stateSaver.scheduleAtFixedRate(peerState::save, 10000, 10000, TimeUnit.MILLISECONDS);
        updaterDeletions.scheduleAtFixedRate(this::updateDeletions, 5000, 10000, TimeUnit.MILLISECONDS);
        updaterExpectedOwners.scheduleAtFixedRate(this::updateOwners, 20000, 10000, TimeUnit.MILLISECONDS);
        updaterRedirects.scheduleAtFixedRate(this::notifyRedirects, 5000, 10000, TimeUnit.MILLISECONDS);
    }



    @Override
    public void backupFile(String filePath, int repDeg) throws Exception {
        if (repDeg > 9 || repDeg <= 0) {
            ThrowError("Replication degree must be at least 1 and lower than 10 (was " + repDeg + ")");
        }

        File file = new File(this.peerFolderPath, filePath);
        if (!file.exists()) {
            ThrowError("File does not exist: " + filePath);
        }

        String fileID = FileIDMaker.createID(file);
        if (fileID == null) {
            ThrowError("Could not create file ID for " + filePath);
        }

        String oldFileID = peerState.getBackupLog().getFileID(filePath);
        if (oldFileID != null && !oldFileID.equals(fileID)) {
            Log("Trying to backup new file version, deleting old one.");
            deleteFile(filePath);
        }

        CompletionHandlerSender chs = new CompletionHandlerSender(repDeg, peerState);
        FileSender fileSender = new FileSender(file,fileID, chs, Chunk.chunkThreadPool);
        peerState.getBackupLog().addFile(fileID, filePath, repDeg, fileSender.getNumChunks());
        fileSender.execute();

        Log("Successfully backed up file: " + filePath);
    }

    @Override
    public void restoreFile(String filePath) throws Exception {
        String fileId = peerState.getBackupLog().getFileID(filePath);
        int numChunks = peerState.getBackupLog().getEntry(fileId).getNumChunks();
        if (fileId == null) {
            ThrowError("Cannot find backed up instance of " + filePath + ", aborting...");
        }
        File restoredFile = new File(this.peerFolderPath, filePath);
        restoredFile.getParentFile().mkdirs();
        restoredFile.createNewFile();

        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(this.peerFolderPath, filePath), Collections.singleton(StandardOpenOption.WRITE), Chunk.chunkThreadPool);

        List<Boolean> done = new ArrayList<>();
        for (int chunkNo = 0; chunkNo < numChunks; chunkNo++) {
            try {
                Chunk chunk = new ChunkRestore(new ChunkID(fileId, chunkNo), this.localNode, this.peerState).restoreChunk();


                ByteBuffer buffer = ByteBuffer.wrap(chunk.getBytes());
                fileChannel.write(buffer, chunkNo * 64000, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        done.add(true);
                        if (done.size() == numChunks) {
                            try {
                                fileChannel.close();
                                Log("Successfully restored file.");
                            } catch (IOException ignored) { }
                        }
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        try {
                            fileChannel.close();
                            restoredFile.delete();
                        } catch (IOException ignored) { }
                        Log("Error restoring file: " + filePath + ". Error message: " + exc.getMessage());
                    }
                });
            } catch (ChunkException e) {
                fileChannel.close();
                restoredFile.delete();
                ThrowError("Error restoring file: " + filePath + ". Error message: " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteFile(String fileName) throws Exception {
        String fileID = peerState.getBackupLog().getFileID(fileName);

        if (fileID == null) {
            ThrowError("Did not find " + fileName + " in backup log!");
        }

        peerState.getBackupLog().addToDeletion(fileID);

        List<InetSocketAddress> storers = peerState.getBackupLog().getStorers(fileID);

        for (InetSocketAddress storer : storers) {
            new FileDeletion(fileID, this.localNode, this.peerState).deleteFile(storer);
        }
    }

    @Override
    public void reclaimDiskSpace(int spaceKB) {
        List<ChunkID> toFree = this.peerState.setCapacity(spaceKB * 1000, this.backupFolderPath);

        for (ChunkID chunkID : toFree) {
            Log("Freed chunk with ID: " + chunkID);

            this.peerState.getStoredLog().getChunk(chunkID, this.backupFolderPath, Chunk.chunkThreadPool, this::freeChunk);
        }
    }

    private void freeChunk(Chunk chunk) {
        Node firstNode = this.localNode.getSuccessor();
        Node nextNode = firstNode;

        for (int attempts = 0; attempts < 10 && nextNode != null; attempts++) {
            boolean success = redirectToNode(chunk, nextNode);
            if (success)
                return;
            nextNode = LocalNode.cycleNode(nextNode, firstNode);
        }
        Log("Error in reclaim: Couldn't keep replication degree of chunk " + chunk.getChunkID());
        redirectToNode(chunk, null);
        this.peerState.getStoredLog().deleteStoredChunk(chunk.getChunkID(), this.backupFolderPath);
    }

    private boolean redirectToNode(Chunk chunk, Node nextNode) {
        System.out.println("Redirecting chunk " + chunk);

        StoredChunkEntry chunkEntry = this.peerState.getStoredLog().getEntry(chunk.getChunkID());

        if (nextNode == null) {
            this.peerState.addRedirect(chunk.getChunkID(), null, chunkEntry.getOwner());
            return true;
        }

        String response = new ChunkBackup(chunk, this.localNode, this.peerState, chunkEntry.getOwner()).backupOnNode(nextNode.getAddress(), chunkEntry.getReplicaNo(), true);

        if (response.equals(RawMessage.OK)) {

            this.peerState.addRedirect(chunk.getChunkID(), nextNode.getAddress(), chunkEntry.getOwner());

            this.peerState.getStoredLog().deleteStoredChunk(chunk.getChunkID(), this.backupFolderPath);
            return true;
        }
        else return false;
    }

    private void updateOwners() {
        Log("Updating owners redirects");

        for (Map.Entry<ChunkID, StoredChunkEntry> entry : this.peerState.getStoredLog().getMap().entrySet()) {
            ChunkID chunkID = entry.getKey();
            StoredChunkEntry chunkEntry = entry.getValue();

            Node node = this.responsibleFor(chunkID, chunkEntry.getReplicaNo(), chunkEntry.getOwner());
            if (node.getID().equals(this.localNode.getID())) {
                Log("I am the correct owner of the chunk " + chunkID + " rep " + chunkEntry.getReplicaNo());
            }
            else {
                Log("I am not the correct owner of the chunk " + chunkID + " rep " + chunkEntry.getReplicaNo() + ", " + node.getID() + " is.");
                this.peerState.getStoredLog().getChunk(chunkID, this.backupFolderPath, Chunk.chunkThreadPool, (chunk) -> {
                    System.out.println("Redirecting chunk " + chunk);
                    redirectToNode(chunk, node);
                });
            }
        }
    }

    private Node responsibleFor(ChunkID chunkID, int replicaNo, InetSocketAddress ownerAddr) {
        Node firstNode = localNode.lookup(ChordIDMaker.generateID(chunkID.toString()));
        Node nextNode = firstNode;

        while (replicaNo > 0 || nextNode.getAddress().equals(ownerAddr)) {
            if (!nextNode.getAddress().equals(ownerAddr))
                replicaNo--;

            nextNode =  LocalNode.cycleNode(nextNode, firstNode);
            if (nextNode == null)
                return this.localNode;
        }
        return nextNode;
    }

    private void notifyRedirects() {
        Log("Notifying of redirects");
        for (Map.Entry<ChunkID, RedirectEntry> mapEntry : this.peerState.getRedirects().entrySet()) {
            ChunkID chunkID = mapEntry.getKey();
            InetSocketAddress ownerAddress = mapEntry.getValue().getOwner();
            InetSocketAddress newAddress = mapEntry.getValue().getAddress();
            Log("Notifying redirect of chunk " + chunkID + " to " + newAddress);

            try (MySslEngineClient client = new MySslEngineClient(TCPGroups.CC(ownerAddress))) {
                String response = client.writeRead(new RedirectMessage(chunkID, this.localNode.getAddress(), newAddress).toString(), Message.MAX_SIZE);
                if (response.equals(RawMessage.OK))
                    this.peerState.clearRedirect(chunkID);
            } catch (IOException e) {
                Log("Failed to send redirect to " + ownerAddress);
            }
        }
    }

    private void updateDeletions() {
        Log("Updating file deletions");
        for (WaitDeletionEntry deletionEntry : this.peerState.getBackupLog().filesWaitingForDeletion()) {
            new FileDeletion(deletionEntry.getFileID(), this.localNode, this.peerState).deleteFile(deletionEntry.getAddress());
        }
    }

    @Override
    public PeerState getInternalState() {
        return peerState;
    }

    public void Log(String message) {
        System.out.println("Peer " + this.localNode.getID() + ": " + message);
    }

    public void ThrowError(String message) throws Exception {
        Log(message);
        throw new Exception(message);
    }

}