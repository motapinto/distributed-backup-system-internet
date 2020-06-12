package StateLogging;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class RedirectEntry implements Serializable {
    private static final long serialVersionUID = 7L;

    private final InetSocketAddress address;
    private final InetSocketAddress owner;

    public RedirectEntry(InetSocketAddress address, InetSocketAddress owner) {
        this.address = address;
        this.owner = owner;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public InetSocketAddress getOwner() {
        return owner;
    }
}
