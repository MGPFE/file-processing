package org.mg.fileprocessing.checksum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Checksum extends Checksum {
    MessageDigest getMessageDigest() {
        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM implementation does not support SHA-256 algorithm!");
        }

        return messageDigest;
    }
}
