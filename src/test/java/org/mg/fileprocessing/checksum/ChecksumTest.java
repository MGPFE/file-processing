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

class ChecksumTest {
    private InputStream input;
    private ChecksumConfig checksumConfig;
    private Checksum checksum;

    @BeforeEach
    void setUp() {
        input = new ByteArrayInputStream("test-data".getBytes(StandardCharsets.UTF_8));

        checksumConfig = new ChecksumConfig("SHA-256");
        checksum = new Checksum(checksumConfig);
    }

    @Test
    public void shouldReturnValidHash() throws IOException {
        // Given
        // When
        String result = checksum.getChecksumAsString(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("05D84936CE1050C2B19D0618D342EA7403B96A46FBB73F86623AF1BD63384652");
    }

    @Test
    public void shouldThrowExceptionWhenAlgorithmUnsupported() {
        // Given
        String algorithm = "UNSUPPORTED";
        checksumConfig = new ChecksumConfig(algorithm);
        checksum = new Checksum(checksumConfig);

        // When
        // Then
        Throwable throwable = assertThatThrownBy(() -> checksum.getChecksumAsString(input)).actual();
        assertThat(throwable).isInstanceOf(RuntimeException.class);
        assertThat(throwable.getCause()).isInstanceOf(NoSuchAlgorithmException.class);
        assertThat(throwable.getMessage()).isEqualTo("Algorithm %s not supported".formatted(algorithm));
    }

    @Test
    public void shouldThrowExceptionWhenNoAlgorithmPassed() {
        // Given
        checksumConfig = new ChecksumConfig();
        checksum = new Checksum(checksumConfig);

        // When
        // Then
        Throwable throwable = assertThatThrownBy(() -> checksum.getChecksumAsString(input)).actual();
        assertThat(throwable).isInstanceOf(RuntimeException.class);
        assertThat(throwable.getMessage()).isEqualTo("No digest algorithm passed");
    }
}