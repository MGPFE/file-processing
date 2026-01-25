package org.mg.fileprocessing.security.auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    private final Instant fixedNow = Instant.parse("2026-05-20T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties("THIS_IS_A_TEST_SECRET_KEY_FOR_JWT", Duration.ofSeconds(10L));

        jwtUtil = new JwtUtil(jwtProperties, clock);

        jwtUtil.init();
    }

    @Test
    public void shouldParseJwtFromAuthHeader() {
        // Given
        String authHeader = "Bearer testingJwt";

        // When
        Optional<String> result = jwtUtil.parseJwt(authHeader);

        // Then
        assertThat(result).isNotEmpty()
                .hasValueSatisfying(value -> assertThat(value).isEqualTo("testingJwt"));
    }

    @Test
    public void shouldParseJwtFromAuthHeaderWithAdditionalWhitespace() {
        // Given
        String authHeader = "Bearer testingJwt         ";

        // When
        Optional<String> result = jwtUtil.parseJwt(authHeader);

        // Then
        assertThat(result).isNotEmpty()
                .hasValueSatisfying(value -> assertThat(value).isEqualTo("testingJwt"));
    }

    @Test
    public void shouldReturnEmptyOptionalWhenInvalidAuthHeader() {
        // Given
        String authHeader = "testingJwt";

        // When
        Optional<String> result = jwtUtil.parseJwt(authHeader);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldGenerateJwtToken() {
        // Given
        String username = "test@test.com";

        // When
        String result = jwtUtil.generateToken(username);

        // Then
        assertThat(result).isNotNull()
                .isNotBlank();
        assertTrue(jwtUtil.isValid(result));
        assertThat(jwtUtil.getUsernameFromToken(result)).isEqualTo(username);

        Claims claims = jwtUtil.parseClaims(result);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(claims.getExpiration()).isEqualTo(clock.instant().plus(jwtProperties.getExpiration()));
    }

    @Test
    public void shouldGetUsernameFromToken() {
        // Given
        String username = "test@test.com";
        String token = jwtUtil.generateToken(username);

        // When
        String result = jwtUtil.getUsernameFromToken(token);

        // Then
        assertThat(result).isNotNull()
                .isNotBlank()
                .isEqualTo(username);
    }

    @Test
    public void shouldReturnTrueWhenValidTokenPassed() {
        // Given
        String username = "test@test.com";
        String token = jwtUtil.generateToken(username);

        // When
        boolean result = jwtUtil.isValid(token);

        // Then
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseWhenInvalidTokenPassed() {
        // Given
        String token = "abcdef";

        // When
        boolean result = jwtUtil.isValid(token);

        // Then
        assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsExpired() {
        // Given
        String username = "test@test.com";
        String token = jwtUtil.generateToken(username);

        Instant futureInstant = fixedNow.plus(Duration.ofHours(10L));
        Clock futureClock = Clock.fixed(futureInstant, ZoneId.of("UTC"));

        JwtUtil futureUtil = new JwtUtil(jwtProperties, futureClock);
        futureUtil.init();

        // When
        boolean result = futureUtil.isValid(token);

        // Then
        assertFalse(result);
    }

    @Test
    public void shouldParseTokenClaims() {
        // Given
        String username = "test@test.com";
        String token = jwtUtil.generateToken(username);

        // When
        Claims result = jwtUtil.parseClaims(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExpiration()).isAfter(result.getIssuedAt());
        assertThat(result.getSubject()).isEqualTo(username);
    }
}