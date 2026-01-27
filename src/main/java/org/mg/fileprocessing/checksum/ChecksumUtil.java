package org.mg.fileprocessing.checksum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChecksumUtil {
    private final ChecksumProperties checksumProperties;

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public String getChecksumAsString(InputStream is) throws IOException {
        MessageDigest messageDigest = getMessageDigest();
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, bytesRead);
        }

        byte[] generatedHash = messageDigest.digest();

        return transformBytesToHex(generatedHash);
    }

     private MessageDigest getMessageDigest() {
        return Optional.ofNullable(checksumProperties.getAlgorithm())
                .map(algo -> {
                    try {
                        return MessageDigest.getInstance(algo);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("Algorithm %s not supported".formatted(algo), e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("No digest algorithm passed"));
    }

    private String transformBytesToHex(byte[] bytes) {
        byte[] hexBytes = new byte[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            hexBytes[i * 2] = HEX_ARRAY[b >>> 4];
            hexBytes[i * 2 + 1] = HEX_ARRAY[b & 0x0F];
        }

        return new String(hexBytes, StandardCharsets.UTF_8);
    }
}
