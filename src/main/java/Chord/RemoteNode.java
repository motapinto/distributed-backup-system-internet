package Chord;

import Chord.Message.*;
import SslEngine.MySslEngineClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RemoteNode implements Node {
    private final ChordID id;
    private final InetSocketAddress address;
    private final InetSocketAddress ownerAddress;

    private final ChordID ownerID;

    public RemoteNode(InetSocketAddress address, InetSocketAddress ownerAddress) {
        this.id = ChordIDMaker.generateID(address);
        this.address = address;
        this.ownerID = ChordIDMaker.generateID(ownerAddress);
        this.ownerAddress = ownerAddress;
    }

    @Override
    public ChordID getID() {
        return id;
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public Node getPredecessor() {
        return sendMessage(new GetPredecessorMessage(ownerAddress));
    }

    @Override
    public Node setPredecessor(Node node) {
        return sendMessage(new SetPredecessorMessage(node.getAddress(), ownerAddress));
    }

    @Override
    public Node getSuccessor() {
        return sendMessage(new GetSuccessorMessage(ownerAddress));
    }

    @Override
    public void setSuccessor(Node node) { }

    @Override
    public Node lookup(ChordID key) {
        return sendMessage(new LookupMessage(key, ownerAddress));
    }

    @Override
    public boolean ping() {
        if (ownerID.equals(this.id))
            return true;
        try(MySslEngineClient ignored = new MySslEngineClient(address)) {} catch (IOException e) {
            return false;
        }
        return true;
    }

    private Node sendMessage(ChordMessage message) {
        try (MySslEngineClient client = new MySslEngineClient(address)) {
            client.write(message.toString());
            client.shutdownOutput();
            String response = client.read(ChordMessage.MAX_SIZE);
            return new RemoteNode(ChordMessageParser.parseAddress(response), this.ownerAddress);
        } catch (IOException e) {
            System.err.println("Error sending message on remote node " + this.getID() + ": " + e.getMessage());
            return this;
        }
    }

    @Override
    public String toString() {
        return "RemoteNode{" +
            "id=" + id +
            ", address=" + address +
            '}';
    }
}
