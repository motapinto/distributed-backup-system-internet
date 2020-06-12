package Chord;


import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ChordTest {
    public static void main(String[] args) {
        ConcurrentHashMap<String, String> a = new ConcurrentHashMap<>();
        a.put("a", "A");
        a.put("b", "B");
        a.put("c", "C");

        ConcurrentHashMap<String, String> b = new ConcurrentHashMap<>(a);
        System.out.println(b.get("a"));
        System.out.println(b.get("b"));
        a.remove("b");
        System.out.println(b.get("a"));
        System.out.println(b.get("b"));

        System.out.println(a.get("a"));
        System.out.println(a.get("b"));


        if (true)
            return;
        InetSocketAddress myAddress = new InetSocketAddress("localhost", 1200);
        InetSocketAddress otherAddress1 = new InetSocketAddress("localhost", 1500); // 27
        InetSocketAddress otherAddress2 = new InetSocketAddress("localhost", 2500); // 1
        InetSocketAddress otherAddress3 = new InetSocketAddress("localhost", 3500); // 27

        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(5))); // 6
        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(24))); // 24
        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(13))); // 24
        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(28))); // 1
        System.out.println(new RemoteNode(otherAddress2, myAddress).lookup(new ChordID(5))); // 6
        System.out.println(new RemoteNode(otherAddress2, myAddress).lookup(new ChordID(24))); // 24
        System.out.println(new RemoteNode(otherAddress2, myAddress).lookup(new ChordID(13))); // 24
        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(28))); // 1
        System.out.println(new RemoteNode(otherAddress3, myAddress).lookup(new ChordID(5))); // 6
        System.out.println(new RemoteNode(otherAddress3, myAddress).lookup(new ChordID(24))); // 24
        System.out.println(new RemoteNode(otherAddress3, myAddress).lookup(new ChordID(13))); // 24
        System.out.println(new RemoteNode(otherAddress1, myAddress).lookup(new ChordID(28))); // 1
    }

    public static void TEST(String[] args) {
        // M = 5
        LocalNode node12 = new LocalNode(new InetSocketAddress("localhost", 1500)); // 900f2e224ebcab10a439e9f4f4636f6f9fe70508496c0173392b1fc95fa2cda7
        LocalNode node1 = new LocalNode(new InetSocketAddress("localhost", 2500)); // 0d19b84e6513f8b88dbbd5f8bf4149c283119a659f70ff3f7c50bb3225cbe256
        LocalNode node27 = new LocalNode(new InetSocketAddress("localhost", 3500)); // 5525dcba859c7ae6d189b990f22c05d41fa08470b9e1c1037195b9fc5385ac39
        LocalNode node6 = new LocalNode(new InetSocketAddress("localhost", 4500)); // 55938fe360b409b3f0e2e20a1635104c3e9d30b8b5884afcb8d23c99ed5d3f93
        LocalNode node24 = new LocalNode(new InetSocketAddress("localhost", 5500)); // f8fad371e4997aa336217da64fdab566eab993dc03e10739884364b5a94ea889
        LocalNode node9 = new LocalNode(new InetSocketAddress("localhost", 6500)); // ea70356dbd563a7f9ed076c4add376809a3723e952a184b3169634219cd22b01
        LocalNode node19 = new LocalNode(new InetSocketAddress("localhost", 7500)); // 6af0b9adac94a2a45528733e053ebde5236872a4e019aac1c863c16e944a1fa7
        LocalNode node2 = new LocalNode(new InetSocketAddress("localhost", 8500)); // 3c1e6e51bcc4184e50f63638a731ae7bcc79b65485a631f8bde5675f9d55fbc0
        LocalNode node8 = new LocalNode(new InetSocketAddress("localhost", 9500)); // 43c6867bc83bf73049f469563c9e37dc3ff40177e56f9379a72ab7c1c6999e7f

        node8.join(null);
        node2.join(node8);
        node19.join(node2);
        node9.join(node19);
        node12.join(node9);
        node27.join(node9);
        node1.join(node27);
        node6.join(node9);
        node24.join(node9);
        printNodes(new Node[]{node8, node2, node19, node9, node24, node6, node27, node1, node12});

        try { Thread.sleep(1000); } catch (InterruptedException ignored) { }
        System.out.println("SLEPT");
        printNodes(new Node[]{node8, node2, node19, node9, node24, node6, node27, node1, node12});

        System.out.println("RESULT:" + node1.lookup(new ChordID(26)));
       // System.out.println("RESULT:" +node28.lookup(new ChordID(12)));
    }

    public static void TEST() {
        // testar com M = 5
        LocalNode node1 = new LocalNode(1);
        LocalNode node4 = new LocalNode(4);
        LocalNode node9 = new LocalNode(9);
        LocalNode node11 = new LocalNode(11);
        LocalNode node14 = new LocalNode(14);
        LocalNode node18 = new LocalNode(18);
        LocalNode node20 = new LocalNode(20);
        LocalNode node21 = new LocalNode(21);
        LocalNode node28 = new LocalNode(28);

        node28.join(null);
        printNodes(new Node[]{node28});

        node21.join(node28);
        printNodes(new Node[]{node28, node21});

        node20.join(node21);
        printNodes(new Node[]{node28, node21, node20});

        node18.join(node20);
        printNodes(new Node[]{node28, node21, node20, node18});


        node1.join(node18);
        node9.join(node1);
        node4.join(node9);
        node11.join(node4);
        node14.join(node11);
        printNodes(new Node[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});

        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        periodicQuery(new LocalNode[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});
        printNodes(new Node[]{node28, node21, node20, node18, node14, node11, node9, node4, node1});

        // System.out.println("RESULT:" + node1.lookup(new ChordID(26)));
        // System.out.println("RESULT:" +node28.lookup(new ChordID(12)));

    }


    public static void printNodes(Node[] nodes) {
        for (Node node : nodes) {
            System.out.println(node);
        }
    }

    public static void periodicQuery(LocalNode[] nodes) {
        for (LocalNode node : nodes) {
            node.stabilize();
        }
    }
}
