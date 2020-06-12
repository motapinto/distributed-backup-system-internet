package Messages;

import Chunks.Chunk;
import Chunks.ChunkID;

import java.net.InetSocketAddress;

public class MessageParser {
    public static InetSocketAddress parseAddress(String address) {
        String ipPort = address.split("/")[1];
        return new InetSocketAddress(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }

    public static Message parseMessage(String message){
        try {
            String[] headerTokens = message.split(Message.HEADER_ENDING, 2)[0].split(" ");
            String type = headerTokens[0], fileID, body, ownerAddress, oldAddress, newAddress;
            int chunkNo, replicaNo;
            boolean isRedirect;
            switch (type) {
                case "PUTCHUNK":
                    fileID = headerTokens[1];
                    chunkNo = Integer.parseInt(headerTokens[2]);
                    ownerAddress = headerTokens[3];
                    replicaNo = Integer.parseInt(headerTokens[4]);
                    isRedirect = Boolean.parseBoolean(headerTokens[5]);
                    body = message.split(Message.HEADER_ENDING, 2)[1];
                    return new PutChunkMessage(new Chunk(fileID, chunkNo, Message.toBytes(body)), parseAddress(ownerAddress), replicaNo, isRedirect);
                case "GETCHUNK":
                    fileID = headerTokens[1];
                    chunkNo = Integer.parseInt(headerTokens[2]);
                    return new GetChunkMessage(new ChunkID(fileID, chunkNo));
                case "CHUNK":
                    fileID = headerTokens[1];
                    chunkNo = Integer.parseInt(headerTokens[2]);
                    body = message.split(Message.HEADER_ENDING, 2)[1];
                    return new ChunkMessage(new Chunk(fileID, chunkNo, Message.toBytes(body)));
                case "DELETE":
                    fileID = headerTokens[1];
                    return new DeleteFileMessage(fileID);
                case "DELETECHUNK":
                    fileID = headerTokens[1];
                    chunkNo = Integer.parseInt(headerTokens[2]);
                    return new DeleteChunkMessage(new ChunkID(fileID, chunkNo));
                case "REDIRECT":
                    fileID = headerTokens[1];
                    chunkNo = Integer.parseInt(headerTokens[2]);
                    oldAddress = headerTokens[3];
                    newAddress = headerTokens[4];
                    if (newAddress.equals("null"))
                        return new RedirectMessage(new ChunkID(fileID, chunkNo), parseAddress(oldAddress), null);
                    return new RedirectMessage(new ChunkID(fileID, chunkNo), parseAddress(oldAddress), parseAddress(newAddress));
                default: return new NullMessage("This type of message is not yet supported: " + type);
            }

        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return new NullMessage("Invalid message: " + message);
        }
        /*
        String version, fileId, type, body;
        int chunkNo, repDeg, senderId;

        try {
            version = headerTokens[0];
            senderId = Integer.parseInt(headerTokens[2]);
            type = headerTokens[1];
            fileId = headerTokens[3];
        } catch (NumberFormatException e) {
            return new NullMessage("INVALID SENDER ID: " + headerTokens[2]);
        }

        switch (type){
            case "PUTCHUNK":
                chunkNo = Integer.parseInt(headerTokens[4]);
                repDeg = Integer.parseInt(headerTokens[5]);
                body = message.split(Message.HEADER_ENDING, 2)[1];
                return new PutChunkMessage(version, senderId, repDeg, new Chunk(fileId, chunkNo, Message.toBytes(body)));
            case "STORED":
                chunkNo = Integer.parseInt(headerTokens[4]);
                return new StoredMessage(version, senderId,fileId, chunkNo);
            case "GETCHUNK":
                chunkNo = Integer.parseInt(headerTokens[4]);
                if (version.equals(Message.VERSION_ENHANCED)) {
                    String[] address = message.split(Message.HEADER_ENDING, 2)[1].split(":");
                    return new GetChunkMessage(version, senderId, fileId, chunkNo, address[0], Integer.parseInt(address[1]));
                }
                return new GetChunkMessage(version, senderId,fileId,chunkNo);
            case "CHUNK":
                chunkNo = Integer.parseInt(headerTokens[4]);
                body = message.split(Message.HEADER_ENDING, 2)[1];
                return new ChunkMessage(version, senderId, new Chunk(fileId, chunkNo, Message.toBytes(body)));
            case "DELETE":
                return new DeleteMessage(version, senderId,fileId);
            case "CONFIRMDELETION":
                return new ConfirmDeletionMessage(version, senderId,fileId);
            case "REMOVED":
                chunkNo = Integer.parseInt(headerTokens[4]);
                return new RemovedMessage(version, senderId,fileId,chunkNo);
            case "START":
                return new StartMessage(version, senderId);
            default: return new NullMessage("INVALID MESSAGE TYPE: " + type);
        }

         */
    }
}
