package Chord;

import Chord.Message.ChordMessageParser;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LocalNode implements Node {


    public static void main(String[] args) {
        InetSocketAddress localAddr = ChordMessageParser.parseAddress(args[0]);
        InetSocketAddress knownAddr = (args.length > 1) ? ChordMessageParser.parseAddress(args[1]) : null;

        LocalNode node = new LocalNode(localAddr);
        if (knownAddr == null) node.join(null);
        else node.join(new RemoteNode(knownAddr, node.getAddress()));
    }

    private final ChordID id;
    private final InetSocketAddress address;
    
    private transient boolean joined = false;

    private Node predecessor;
    private Node otherSuccessor;
    private Node otherPredecessor;

    private final AtomicReferenceArray<Node> fingerTable;

    public LocalNode(InetSocketAddress address) {
        this.id = ChordIDMaker.generateID(address);
        this.address = address;
        this.fingerTable = new AtomicReferenceArray<>(ChordID.SIZE);

        for(int i = 0; i < fingerTable.length(); i++)
            fingerTable.set(i, this);

        this.predecessor = this;
        this.otherSuccessor = this;
        this.otherPredecessor = this;
    }

    @Override
    public ChordID getID() {
        return this.id;
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.address;
    }

    @Override
    public Node getPredecessor() {
        return this.predecessor;
    }

    @Override
    public Node setPredecessor(Node predecessor) {
        Node oldPredecessor = this.predecessor;
        this.predecessor = predecessor;
        return oldPredecessor;
    }

    @Override
    public Node getSuccessor() {
        return this.fingerTable.get(0);
    }

    @Override
    public void setSuccessor(Node node) {
        fingerTable.set(0, node);
    }

    @Override
    public Node lookup(ChordID key) {
        if (this.getID().equals(key)) return this;

        Node successor = getSuccessor();
        if (key.isBetween(this.getID(), successor.getID()) || key.equals(successor.getID()))
            return successor;

        return forwardLookup(key);
    }

    private Node forwardLookup(ChordID key) {
        for(int i = fingerTable.length() - 1; i >= 0; i--)
            if (fingerTable.get(i).getID().isBetween(this.getID(), key))
                return fingerTable.get(i).lookup(key);

        throw new RuntimeException("No successor found... yikes");
    }

    /**
     * Join a Chord Ring containing node other
     * @param other
     */
    public void join(Node other) {
        if (this.joined)
            throw new RuntimeException("Node " + this.getID() + " is already in a ring.");
            
        this.joined = true;

        if (other != null) {
            Node successor = other.lookup(getID().add2Pow(0));
            this.setSuccessor(successor.setPredecessor(this));
        }
    
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::stabilize, 4000, 4000, TimeUnit.MILLISECONDS);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::fixFingers, 4000, 4000, TimeUnit.MILLISECONDS);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkPredecessor, 4000, 4000, TimeUnit.MILLISECONDS);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkSuccessor, 4000, 4000, TimeUnit.MILLISECONDS);
        Executors.newSingleThreadScheduledExecutor().execute(new LocalNodeServer(this));
    }

    /**
     * Called periodically, verifies node immediate successor, and tells the successor about this node
     */
    public void stabilize() {
        if (!this.joined) return;

        Node notify = this.getSuccessor().getPredecessor();
        if (notify.getID().isBetween(this.getID(), this.getSuccessor().getID())) {
            this.setSuccessor(notify);
        }
        if (!notify.getID().equals(this.id))
            this.getSuccessor().setPredecessor(this);
    }

    /**
     * Called periodically, refreshes finger table entries
     */
    private void fixFingers() {
        if (!this.joined) return;

        System.out.println("UPDATING node " + this);

        for(int i = 1; i < fingerTable.length(); i++) {
            ChordID toLookup = this.getID().add2Pow(i);

            Node successor = this.getSuccessor();
            if (toLookup.isBetween(this.getID(), successor.getID()))
                fingerTable.set(i, successor);
            else
                fingerTable.set(i, successor.lookup(toLookup));
        }
    }

    /**
     * Called periodically, checks if whether predecessor has failed
     */
    public void checkPredecessor() {
        if (!this.joined) return;

        if (!predecessor.ping()) {
            setPredecessor(this.otherPredecessor);
        } else this.otherPredecessor = predecessor.getPredecessor();
    }

    /**
     * Chord's fault tolerance
     * @return
     */
    public void checkSuccessor() {
        if (!this.joined) return;
        if(!getSuccessor().ping()) {
            setSuccessor(this.otherSuccessor);
        }
        else {
            this.otherSuccessor = getSuccessor().getSuccessor();
        }

}

    @Override
    public String toString() {
        StringBuilder fingerTableStr = new StringBuilder("(");
        for (int i = 0; i < fingerTable.length(); i++) {
            if (fingerTable.get(i) == null) fingerTableStr.append("?,");
            else fingerTableStr.append(fingerTable.get(i).getID()).append(',');
        }

        fingerTableStr.append(")");
        return "Node {" +
            "id=" + id +
            ", predecessor=" + predecessor.getID() +
            ", fingerTable=" + fingerTableStr +
            ", address=" + address +
            ", otherSucc=" + otherSuccessor.getID() +
            ", otherPred=" + otherPredecessor.getID() +
            '}';
    }

    public static Node cycleNode(Node currNode, Node firstNode) {
        Node nextNode = currNode.getSuccessor();
        if (nextNode.getID().equals(firstNode.getID())) {
            System.out.println("Warning: There may not be enough nodes in the circle.");
            return null;
        }
        return nextNode;
    }

    @Override
    public boolean ping() {
        return true;
    }

    public LocalNode(int ID) {
        this.id = new ChordID(ID);
        this.address = null;
        this.fingerTable = new AtomicReferenceArray<>(ChordID.SIZE);

        for(int i = 0; i < fingerTable.length(); i++) {
            fingerTable.set(i, this);
        }
        this.predecessor = this;
    }
}
