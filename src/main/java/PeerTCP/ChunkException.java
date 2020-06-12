package PeerTCP;

public class ChunkException extends Exception {
    private final String message;

    public ChunkException(String s) {
        this.message = s;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
