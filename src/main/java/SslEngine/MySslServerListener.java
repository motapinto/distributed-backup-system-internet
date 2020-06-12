package SslEngine;

public interface MySslServerListener {
    void onReadReceived(MySslServerConnection connection);
}
