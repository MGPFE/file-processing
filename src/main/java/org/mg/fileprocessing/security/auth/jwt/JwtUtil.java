package org.mg.fileprocessing.security.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Optional<String> parseJwt(String authHeader) {
        return Optional.ofNullable(authHeader)
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()).trim());
    }

    public String generateToken(String username) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getExpiration())))
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT Validation failed: {}", e.getMessage(), e);
        }
        return false;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(Instant.now(clock)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
