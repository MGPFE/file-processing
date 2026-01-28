package org.mg.fileprocessing.security.auth.jwt;

import org.junit.jupiter.api.Test;
import org.mg.fileprocessing.TestUtils;
import org.mg.fileprocessing.interceptors.IdempotencyInterceptorProperties;
import org.mg.fileprocessing.security.auth.AuthDto;
import org.mg.fileprocessing.security.SecurityConfig;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JwtAuthController.class)
@Import(SecurityConfig.class)
class JwtAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock clock() {
            final Instant fixedNow = Instant.parse("2026-05-20T12:00:00Z");
            return Clock.fixed(fixedNow, ZoneId.of("UTC"));
        }
    }

    @MockitoBean private JwtAuthService jwtAuthService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RedisTemplate<String, String> redisTemplate;
    @MockitoBean private IdempotencyInterceptorProperties idempotencyInterceptorProperties;

    @Test
    public void shouldReturn201OnUserRegister() throws Exception {
        // Given
        AuthDto authDto = new AuthDto("test@user.com", "helloworld");

        // When
        // Then
        mockMvc.perform(post("/auth/sign-up")
                        .content(objectMapper.writeValueAsString(authDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturn400WhenInvalidSignUpPassword() throws Exception {
        // Given
        AuthDto authDto = new AuthDto("test@user.com", "a");

        String expected = TestUtils.getResourceAsString(Path.of("security/auth/jwt/sign-up-invalid-password.json"));

        // When
        // Then
        mockMvc.perform(post("/auth/sign-up")
                        .content(objectMapper.writeValueAsString(authDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
    }

    @Test
    public void shouldReturn400WhenInvalidSignUpEmail() throws Exception {
        // Given
        AuthDto authDto = new AuthDto("dummy", "helloworld");

        String expected = TestUtils.getResourceAsString(Path.of("security/auth/jwt/sign-up-invalid-email.json"));

        // When
        // Then
        mockMvc.perform(post("/auth/sign-up")
                        .content(objectMapper.writeValueAsString(authDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
    }

    @Test
    public void shouldReturnJwtTokenOnSignIn() throws Exception {
        // Given
        AuthDto authDto = new AuthDto("test@mail.com", "helloworld");

        given(jwtAuthService.signIn(authDto)).willReturn(new JwtSignInResponseDto("JWT_TOKEN"));

        // When
        // Then
        mockMvc.perform(post("/auth/sign-in")
                        .content(objectMapper.writeValueAsString(authDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("JWT_TOKEN"));
    }
}