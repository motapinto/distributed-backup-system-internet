import java.net.InetSocketAddress;

public class PeerRunner {
    public static void main(String[] args) {
        InetSocketAddress address;
        InetSocketAddress knownAddress;
        try {
            if (args.length == 4) {
                knownAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            } else knownAddress = null;
            address = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            printUsage();
            e.printStackTrace();
            return;
        }
        new Peer(address, knownAddress).run();
    }

    private static void printUsage() {
        System.out.println("Usage: java PeerRunner <IP> <Port> [<KnownIP> <KnownPort>]");
    }
}
