package Chord;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChordIDMaker {
    /*
     * Generate identifier through an InetSocketAddress
     * @param address received address
     * @return hashed key
     */
    public static ChordID generateID(InetSocketAddress address) {
        String input = (address.getAddress().getHostAddress() + ":" + address.getPort()).toUpperCase();
        return generateID(input);
    }

    public static ChordID generateID(String input) {
        try {
            StringBuilder hexString = new StringBuilder();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < hash.length; i++)
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            return ChordID.fromHex(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}
