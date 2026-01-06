package org.mg.fileprocessing.checksum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChecksumUtilTest {
    private InputStream input;
    private ChecksumUtil checksumUtil;

    @BeforeEach
    void setUp() {
        input = new ByteArrayInputStream("test-data".getBytes(StandardCharsets.UTF_8));

        ChecksumProperties checksumProperties = new ChecksumProperties("SHA-256");
        checksumUtil = new ChecksumUtil(checksumProperties);
    }

    @Test
    public void shouldReturnValidHash() throws IOException {
        // Given
        // When
        String result = checksumUtil.getChecksumAsString(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("05D84936CE1050C2B19D0618D342EA7403B96A46FBB73F86623AF1BD63384652");
    }

    @Test
    public void shouldThrowExceptionWhenAlgorithmUnsupported() {
        // Given
        String algorithm = "UNSUPPORTED";
        ChecksumProperties checksumProperties = new ChecksumProperties(algorithm);
        checksumUtil = new ChecksumUtil(checksumProperties);

        // When
        // Then
        assertThatThrownBy(() -> checksumUtil.getChecksumAsString(input))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(NoSuchAlgorithmException.class)
                .hasMessage("Algorithm %s not supported".formatted(algorithm));
    }

    @Test
    public void shouldThrowExceptionWhenNoAlgorithmPassed() {
        // Given
        ChecksumProperties checksumProperties = new ChecksumProperties();
        checksumUtil = new ChecksumUtil(checksumProperties);

        // When
        // Then
        assertThatThrownBy(() -> checksumUtil.getChecksumAsString(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No digest algorithm passed");
    }
}