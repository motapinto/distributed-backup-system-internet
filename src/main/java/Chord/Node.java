package Chord;

import java.io.Serializable;
import java.net.InetSocketAddress;

public interface Node extends Serializable {
    long serialVersionUID = 8L;

    ChordID getID();

    InetSocketAddress getAddress();

    Node getPredecessor();

    Node setPredecessor(Node node);

    Node getSuccessor();

    void setSuccessor(Node node);

    /**
     * DHT provides a single operation, lookup(key)
     * @param key Object's key
     * @return the address of the node responsible for he key
     */
    Node lookup(ChordID key);

    /**
     * Checks if successor is alive
     * @return
     */
    boolean ping();
}