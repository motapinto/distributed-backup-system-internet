package Chunks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileIDMaker {
    public static String createID(File file) {
        try {
            StringBuilder hexString = new StringBuilder();
            String input = (file.getAbsolutePath() + file.lastModified()).toUpperCase();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < hash.length; i++)
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
